package download;

import config.NettyServerConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.StringUtils;
import utils.FileSizeUtil;
import utils.HttpResponseUtils;
import utils.UriUtils;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 *
 * 文件下载处理器
 * @author chenhaijian
 * @date 2020-05-17 10:06
 */
public class HttpDownloadFileHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT+8";
    public static final int HTTP_CACHE_SECONDS = 60;

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");

    private static final String PASSWD_PARAM = "passwd";
    private static final String TOKEN_PARAM = "token";

    private static final String TYPE_PARAM = "type";

    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            HttpResponseUtils.sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }
        if (request.method() != HttpMethod.GET) {
            HttpResponseUtils.sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        // 获取请求的uri
        final URI uri = UriUtils.getURI(request);
        final String uriPath = uri.getPath();
        String path = sanitizeUri(uriPath);
        if (path == null) {
            HttpResponseUtils.sendError(ctx, FORBIDDEN);
            return;
        }

        // 获取request的参数
        Map<String, String> params = UriUtils.getQueryParams(uri);

        File file = new File(path);
        if (file.isHidden() || !file.exists()) {
            // 文件不存在
            HttpResponseUtils.sendError(ctx, NOT_FOUND);
            return;
        }

        // 认证访问链接
        boolean authFlag = doAuth(params, file, uriPath);
        if (!authFlag) {
            // 认证失败
            HttpResponseUtils.sendError(ctx, HttpResponseStatus.UNAUTHORIZED);
            return;
        }
        if ("1".equals(params.get(TYPE_PARAM))) {
            // 如果是获取下载链接的请求
            processGetLink(ctx, uriPath);
            return;
        }

        if (file.isDirectory()) {
            // 如果是目录，则发送该目录下的文件列表
            if (uriPath.endsWith("/")) {
                sendListing(ctx, file, params);
            } else {
                sendRedirect(ctx, uriPath + "/", params);
            }
            return;
        }

        if (!file.isFile()) {
            HttpResponseUtils.sendError(ctx, FORBIDDEN);
            return;
        }

        // 缓存验证
        String ifModifiedSince = request.headers().get(IF_MODIFIED_SINCE);
        if (StringUtils.isNotBlank(ifModifiedSince)) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.SIMPLIFIED_CHINESE);
            Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                sendNotModified(ctx);
                return;
            }
        }

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException fnfe) {
            HttpResponseUtils.sendError(ctx, NOT_FOUND);
            return;
        }
        long fileLength = raf.length();

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_LENGTH, fileLength);

        setContentTypeHeader(response, file);
        setDateAndCacheHeaders(response, file);

        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }

        ctx.write(response);

        // 写入文件内容
        ChannelFuture sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());

        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) {
                    System.err.println("Transfer progress: " + progress);
                } else {
                    System.err.println("Transfer progress: " + progress + " / " + total);
                }
            }

            public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                System.err.println("Transfer complete.");
            }
        });

        // 写入结束标志
        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        if (!HttpUtil.isKeepAlive(request)) {
            // 写完后关闭该连接
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            HttpResponseUtils.sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * 获取文件的决定路径
     * @param uri
     * @return
     */
    private static String sanitizeUri(String uri) {
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            try {
                uri = URLDecoder.decode(uri, "ISO-8859-1");
            } catch (UnsupportedEncodingException e1) {
                throw new Error();
            }
        }

        if (!uri.startsWith("/")) {
            return null;
        }

        // 转换为系统的分隔符
        uri = uri.replace('/', File.separatorChar);

        // 文件uri的校验
        if (uri.contains(File.separator + '.') ||
                uri.contains('.' + File.separator) ||
                uri.startsWith(".") || uri.endsWith(".") ||
                INSECURE_URI.matcher(uri).matches()) {
            return null;
        }

        // 转换为文件的决定路径地址
        return NettyServerConfig.ROOT_PATH + File.separator + uri;
    }

    /**
     * 返回文件夹下所有文件
     * @param ctx
     * @param dir
     */
    private void sendListing(ChannelHandlerContext ctx, File dir, Map<String, String> params) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");

        StringBuilder buf = new StringBuilder();
        String dirPath = dir.getPath();

        buf.append("<!DOCTYPE html>\r\n");
        buf.append("<html><head><title>");
        buf.append("Listing of: ");
        buf.append(dirPath);
        buf.append("</title></head><body>\r\n");

        buf.append("<h3>Listing of: ");
        buf.append(dirPath);
        buf.append("</h3>\r\n");

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        buf.append("<table style=\"width: 100%;max-width: 100%;margin-bottom: 20px;\">");

        buf.append("<thead><tr>");
        buf.append("<th style=\"width:55%;text-align: left;\">File Name</th>");
        buf.append("<th style=\"width:15%;text-align: left;\">File Size</th>");
        buf.append("<th style=\"width:20%;text-align: left;\">File Date</th>");
        buf.append("<th style=\"width:10%;text-align: left;\">Download Link</th>");
        buf.append("</tr></thead>");

        buf.append("<tbody>");
        File[] listFiles = dir.listFiles();
        for (int i =0 ; i < listFiles.length; i++) {
            File f = listFiles[i];
            if (i == 0 && !NettyServerConfig.ROOT_PATH.equals(dir.getPath())) {
                String parent_dir = dir.getParent().replaceAll(NettyServerConfig.ROOT_PATH, "");
                parent_dir = parent_dir.equals("") ? "/" : parent_dir;
                buf.append("<tr>");
                buf.append("<td><a href=\"");
                buf.append(fillParams(parent_dir, params));
                buf.append("\" style=\"color:#FFD700\">");
                buf.append("Parent directory/");
                buf.append("</a></td>");
                buf.append("<td>-</td>");
                buf.append("<td>-</td>");
                buf.append("<td>-</td>");
                buf.append("</tr>");
            }
            if (f.isHidden() || !f.canRead()) {
                continue;
            }

            String name = f.getName();
//            if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
//                continue;
//            }
            // 获取文件大小
            String filesSize = "";
            if (f.isFile()) {
                filesSize = FileSizeUtil.getAutoFileOrFilesSize(f);
            }
            long lastModified = f.lastModified();
            String date = format.format(new Date(lastModified));

            String targetUri = fillParams(name, params);
            buf.append("<tr>");
            buf.append("<td><a href=\"");
            buf.append(targetUri);
            if (f.isDirectory()) {
                buf.append("\" style=\"color:#006400\">");
            } else {
                buf.append("\" style=\"color:#088ACB\">");
            }
            buf.append(name);
            buf.append("</a></td>");

            buf.append("<td>");
            buf.append(filesSize);
            buf.append("</td>");

            buf.append("<td>");
            buf.append(date);
            buf.append("</td>");

            // 获取下载链接
            if (f.isFile()) {
                buf.append("<td><a target=\"_blank\" href=\"");
                buf.append(targetUri + "&type=1");
                buf.append("\">");
                buf.append("获取链接");
                buf.append("</a></td>");
            } else {
                buf.append("<td>-</td>");
            }
            buf.append("</tr>");
        }
        buf.append("</tbody>");



        buf.append("</table></body></html>\r\n");
        ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 重定向
     * @param ctx
     * @param newUri
     */
    private void sendRedirect(ChannelHandlerContext ctx, String newUri, Map<String, String> params) {
        newUri = fillParams(newUri, params);

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
        response.headers().set(LOCATION, newUri);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 为response设置时间和缓存的响应头
     *
     * @param response
     *            HTTP response
     * @param fileToCache
     *            file to extract content type
     */
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(
                LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    /**
     * 为response header 设置content-type
     *
     * @param response
     *            HTTP response
     * @param file
     *            file to extract content type
     */
    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
//        response.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
        response.headers().set(CONTENT_TYPE, "application/octet-stream");
    }

    /**
     * 为response header 设置date
     *
     * @param response
     *            HTTP response
     */
    private static void setDateHeader(FullHttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));
    }

    /**
     * 当文件时间戳和浏览器发送一致时, send a "304 Not Modified"
     *
     * @param ctx
     *            Context
     */
    private static void sendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);
        setDateHeader(response);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 校验权限
     * @param params
     * @param file
     */
    private boolean doAuth(Map<String, String> params, File file, String uriNoParams) {
        String passwd = params.get(PASSWD_PARAM);
        String token = params.get(TOKEN_PARAM);

        if (NettyServerConfig.passwd.equals(passwd)) {
            return true;
        }
        if (file.isDirectory() || ("1".equals(params.get(TYPE_PARAM)) && !NettyServerConfig.passwd.equals(passwd))) {
            // 访问的是文件夹，却没有密码，则提示无权访问   || 访问的是获取下载地址的链接， 如果没有密码，则无权访问
            return false;
        }
        if (file.isFile()) {
            if (StringUtils.isNotBlank(token)) {
                String value = DownloadTokenCache.cache.getIfPresent(token);
                if (StringUtils.isNotBlank(value)) {
                    // 清除该缓存，
                    DownloadTokenCache.cache.invalidate(token);
                    if (value.equals(uriNoParams)) {
                        // 校验访问的文件的地址是否一致
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 填充参数
     * @param uri
     * @param params
     * @return
     */
    private String fillParams(String uri, Map<String, String> params) {

        if (!params.isEmpty()) {
            uri = uri + "?";
            for (Map.Entry<String, String> entry : params.entrySet()) {
                uri = uri + entry.getKey() + "=" + entry.getValue() + "&";
            }
            uri = uri.substring(0, uri.length() - 1);
        }

        return uri;
    }

    /**
     * 获取下载链接
     * @param ctx
     * @param uriPath
     */
    private void processGetLink(ChannelHandlerContext ctx, String uriPath) {
        String uuid = UUID.randomUUID().toString();
        // 放入guava缓存中
        DownloadTokenCache.cache.put(uuid, uriPath);

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");

        StringBuffer buf = new StringBuffer();
        buf.append("http://");
        buf.append(NettyServerConfig.LOCAL_IP + ":");
        buf.append(NettyServerConfig.DOWNLOAD_PORT);
        buf.append(uriPath);
        buf.append("?token=" + uuid);

        ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}

package upload;

import config.NettyServerConfig;
import config.UploadHtml;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;
import utils.HttpResponseUtils;
import utils.UriUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 文件上传处理类
 * @author chenhaijian
 * @date 2020-05-21 14:45
 */
public class HttpUploadFileHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger logger = Logger.getLogger(HttpUploadFileHandler.class.getName());

    private HttpRequest request;

    private static final String uploadUrl = "/up";

    private static final String fromFileUrl = "/post_multipart";

    private static final HttpDataFactory factory =
            new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk if size exceed

    private HttpPostRequestDecoder decoder;

    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
        // on exit (in normal
        // exit)
        DiskFileUpload.baseDirectory = null; // system temp directory
        DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on
        // exit (in normal exit)
        DiskAttribute.baseDirectory = null; // system temp directory
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (decoder != null) {
            decoder.cleanFiles();
        }
    }

    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

        if (msg instanceof HttpRequest) {
            this.request = (HttpRequest) msg;
            URI uri = UriUtils.getURI(request);

            // 密码校验
            boolean authFlag = auth(ctx, uri);
            if (!authFlag) {
                // 校验不通过
                return;
            }
            logger.info(uri.getPath());
            urlRoute(ctx, uri.getPath());
        }

        if (decoder != null) {
            if (msg instanceof HttpContent) {
                // 接收一个新的请求体
                decoder.offer((HttpContent) msg);
                // 将内存中的数据序列化本地
                readHttpDataChunkByChunk();
            }
            if (msg instanceof LastHttpContent) {
                logger.info("LastHttpContent");
                reset();
                HttpResponseUtils.sendResponseJson(ctx, HttpResponseStatus.OK, HttpResponseStatus.OK.code(), "上传成功");
            }

        }


    }

    // url路由
    private void urlRoute(ChannelHandlerContext ctx, String uri) {
        StringBuilder urlResponse = new StringBuilder();
        // 访问文件上传页面
        if (uri.startsWith(uploadUrl)) {
            urlResponse.append(getUploadResponse());
        } else if (uri.startsWith(fromFileUrl)) {
            decoder = new HttpPostRequestDecoder(factory, request);
            return;
        } else {
            urlResponse.append(getHomeResponse());
        }
        writeResponse(ctx, urlResponse.toString());
    }

    private void writeResponse(ChannelHandlerContext ctx, String context) {
        ByteBuf buf = Unpooled.copiedBuffer(context, CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=utf-8");
        //设置短连接 addListener 写完马上关闭连接
        ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private String getHomeResponse() {
        return " <h1> welcome home </h1> ";
    }

    private String getUploadResponse() {
//        return "<!DOCTYPE html>\n" +
//                "<html lang=\"en\">\n" +
//                "<head>\n" +
//                "    <meta charset=\"UTF-8\">\n" +
//                "    <title>Title</title>\n" +
//                "</head>\n" +
//                "<body>\n" +
//                "\n" +
//                "<form action=\"http://"+NettyServerConfig.LOCAL_IP+":"+NettyServerConfig.UPLOAD_PORT +"/post_multipart?passwd="+NettyServerConfig.passwd+"\" enctype=\"multipart/form-data\" method=\"POST\">\n" +
//                "\n" +
//                "\n" +
//                "    <input type=\"file\" name=" +
//                " " +
//                "" +
//                "\"YOU_KEY\">\n" +
//                "\n" +
//                "    <input type=\"submit\" name=\"send\">\n" +
//                "\n" +
//                "</form>\n" +
//                "\n" +
//                "</body>\n" +
//                "</html>";

        return UploadHtml.HTML.replace("{#ip}", NettyServerConfig.LOCAL_IP).replace("{#port}", NettyServerConfig.UPLOAD_PORT + "")
                .replace("{#passwd}", NettyServerConfig.passwd);

    }

    /**
     * 将字节流写入本地磁盘
     * @throws IOException
     */
    private void readHttpDataChunkByChunk() throws IOException {
        while (decoder.hasNext()) {
            InterfaceHttpData data = decoder.next();
            if (data != null) {
                if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                    FileUpload fileUpload = (FileUpload) data;
                    if (fileUpload.isCompleted()) {
                        fileUpload.isInMemory();// tells if the file is in Memory
                        // or on File
                        fileUpload.renameTo(new File(NettyServerConfig.ROOT_PATH + File.separator + fileUpload.getFilename())); // enable to move into another
                        // File dest
                        decoder.removeHttpDataFromClean(fileUpload); //remove
                    }
                }
            }
        }
    }

    private void reset() {
        request = null;
        // destroy the decoder to release all resources
        decoder.destroy();
        decoder = null;
    }

    /**
     * 进行请求的密码校验
     * @param ctx
     * @param uri
     * @return
     */
    private boolean auth(ChannelHandlerContext ctx, URI uri) {
        Map<String, String> queryParams = UriUtils.getQueryParams(uri);
        String passwd = queryParams.get("passwd");
        if (NettyServerConfig.passwd.equals(passwd)) {
            return true;
        }

        // 校验不通过
        HttpResponseUtils.sendResponseJson(ctx, HttpResponseStatus.UNAUTHORIZED, HttpResponseStatus.UNAUTHORIZED.code(),
                HttpResponseStatus.UNAUTHORIZED.toString());
        return false;
    }
}

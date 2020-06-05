import config.NettyServerConfig;
import download.HttpDownloadFileServer;
import org.apache.commons.lang3.StringUtils;
import upload.HttpUploadFileServer;

/**
 * @author chenhaijian
 * @date 2020-05-21 18:11
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        String dPortStr = System.getProperty("dPort");
        if (StringUtils.isNotBlank(dPortStr)) {
            int port = Integer.valueOf(dPortStr);
            NettyServerConfig.DOWNLOAD_PORT = port;
        }
        String uPortStr = System.getProperty("uPort");
        if (StringUtils.isNotBlank(uPortStr)) {
            int port = Integer.valueOf(uPortStr);
            NettyServerConfig.UPLOAD_PORT = port;
        }

        String ip = System.getProperty("ip");
        if (StringUtils.isNotBlank(ip)) {
            NettyServerConfig.LOCAL_IP = ip;
        }
        String path = System.getProperty("path");
        if (StringUtils.isNotBlank(path)) {
            NettyServerConfig.ROOT_PATH = path;
        }
        // 启动下载服务
        new Thread(() -> {
            HttpDownloadFileServer download = new HttpDownloadFileServer();
            try {
                download.run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        // 启动上传服务
        HttpUploadFileServer upload = new HttpUploadFileServer();
        upload.run();
    }
}

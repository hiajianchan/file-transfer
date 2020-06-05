package config;

/**
 * 下载器的 相关配置文件
 * @author chenhaijian
 * @date 2020-05-17 09:56
 */
public class NettyServerConfig {

    // 下载监听端口号
    public static int DOWNLOAD_PORT = 7101;

    // 上传监听端口号
    public static int UPLOAD_PORT = 7100;

    // 文件根目录
    public static String ROOT_PATH = "/Users/admin/cloudfile";

    public static String passwd = "fireinthehole";

//    public final static String LOCAL_IP = "39.96.183.210";
    public static String LOCAL_IP = "127.0.0.1";
}

package download;

import config.NettyServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author chenhaijian
 * @date 2020-05-17 09:55
 */
public class HttpDownloadFileServer {

    public void run() throws InterruptedException {

        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        try {
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpDownloadFileInitialzer());

            ChannelFuture channelFuture = bootstrap.bind(NettyServerConfig.DOWNLOAD_PORT).sync();
            System.out.println("download server started...");

            channelFuture.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }

    }

//    public static void main(String[] args) throws InterruptedException {
//        String portStr = System.getProperty("port");
//        if (StringUtils.isNotBlank(portStr)) {
//            int port = Integer.valueOf(portStr);
//            NettyServerConfig.DOWNLOAD_PORT = port;
//        }
//        String ip = System.getProperty("ip");
//        if (StringUtils.isNotBlank(ip)) {
//            NettyServerConfig.LOCAL_IP = ip;
//        }
//        String path = System.getProperty("path");
//        if (StringUtils.isNotBlank(path)) {
//            NettyServerConfig.ROOT_PATH = path;
//        }
//        HttpDownloadFileServer server = new HttpDownloadFileServer();
//        server.run();
//    }
}

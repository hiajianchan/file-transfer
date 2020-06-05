package upload;

import config.NettyServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * netty实现http协议上传文件 服务端
 * @author chenhaijian
 * @date 2020-05-21 14:33
 */
public class HttpUploadFileServer {

    public void run() throws InterruptedException {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new HttpUploadFileInitialzer());

            Channel channel = bootstrap.bind(NettyServerConfig.UPLOAD_PORT).sync().channel();

            System.out.println("upload server started ....");

            channel.closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

//    public static void main(String[] args) throws InterruptedException {
//        HttpUploadFileServer server = new HttpUploadFileServer();
//        server.run();
//    }
}

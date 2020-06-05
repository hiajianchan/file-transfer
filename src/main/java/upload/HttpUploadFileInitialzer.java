package upload;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 *
 * @author chenhaijian
 * @date 2020-05-21 14:42
 */
public class HttpUploadFileInitialzer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast(new HttpContentCompressor());
//        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
//        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());

        pipeline.addLast("processHandler", new HttpUploadFileHandler());
    }
}

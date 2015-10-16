package issuer.handler.dispatch.upstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import util.Config;


public class DispatchChannelInitializer extends ChannelInitializer<SocketChannel> {

private Channel inboundChannel;
	
	public DispatchChannelInitializer(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		
		// Logging on?
		if (Config.getInstance().getValue("LOGGING").equals("on")) {
			//pipeline.addLast(new LoggingHandler(LogLevel.INFO));
		}

		pipeline.addLast("httpdecoder", new HttpRequestDecoder()); // upstream 1
        pipeline.addLast("httpencoder", new HttpResponseEncoder()); // downstream 1
        pipeline.addLast("aggregator", new HttpObjectAggregator(1048576));
        
        pipeline.addLast("returner", new ReturnHandler(inboundChannel)); // upstream 3
	}
}

package receiver.handler.upstream;


import receiver.server.ReceiverServer;
import util.Config;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
public class ReceiverChannelInitializer extends ChannelInitializer<SocketChannel> {
	
	private ReceiverServer server;
	
	public ReceiverChannelInitializer(ReceiverServer server) {
		this.server = server;
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
        
        pipeline.addLast("forwarder", new ForwardHandler(server)); // upstream 2
	}

}

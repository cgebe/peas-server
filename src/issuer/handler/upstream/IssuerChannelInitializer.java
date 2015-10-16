package issuer.handler.upstream;


import util.Config;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import issuer.server.IssuerServer;

public class IssuerChannelInitializer extends ChannelInitializer<SocketChannel> {
	

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
        
        //pipeline.addLast("keyhandler", new KeyHandler()); // upstream 3
        pipeline.addLast("queryhandler", new QueryHandler()); // upstream 2

        
	}

}

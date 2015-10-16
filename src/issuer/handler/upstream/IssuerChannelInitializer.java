package issuer.handler.upstream;

import util.Config;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

public class IssuerChannelInitializer extends ChannelInitializer<SocketChannel> {
	

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
	
		// Logging on?
		if (Config.getInstance().getValue("LOGGING").equals("on")) {
			//pipeline.addLast(new LoggingHandler(LogLevel.INFO));
		}

		pipeline.addLast("httpcodec", new HttpServerCodec()); // upstream/downstream 1
        

        //pipeline.addLast("keyhandler", new KeyHandler()); // upstream 3
        pipeline.addLast("queryhandler", new QueryHandler()); // upstream 4

        
        // TODO
        //pipeline.addLast("dispatcher", new DispatchHandler());
	}

}

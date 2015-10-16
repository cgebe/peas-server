package receiver.handler.forward.upstream;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import receiver.server.ReceiverServer;
import util.Config;

public class ForwardChannelInitializer extends ChannelInitializer<SocketChannel> {

	private ReceiverServer server;
	
	public ForwardChannelInitializer(ReceiverServer server) {
        this.server = server;
    }

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		
		// Logging on?
		if (Config.getInstance().getValue("LOGGING").equals("on")) {
			//pipeline.addLast(new LoggingHandler(LogLevel.INFO));
		}

		pipeline.addLast("httpcodec", new HttpServerCodec()); // upstream/downstream 1
        pipeline.addLast("returner", new ReturnHandler(server)); // upstream 3
	}

}

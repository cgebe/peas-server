ackage issuer.handler.upstream;

import org.json.simple.parser.JSONParser;

import protocol.PEASMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
public class DispatchHandler extends SimpleChannelInboundHandler<PEASMessage> {
	
	
	private JSONParser parser = new JSONParser();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }
    
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, PEASMessage obj) throws Exception {
	}

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        
    }
    

	


}

package issuer.handler.dispatch.upstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;


public class ReturnHandler extends SimpleChannelInboundHandler<HttpObject> {

	private final Channel inboundChannel;

    public ReturnHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject toReturn) throws Exception {
        ctx.close();
        ChannelFuture f = inboundChannel.writeAndFlush(toReturn);
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                	//System.out.println("successful return");
                } else {
                	//System.out.println("failed return");
                }
                inboundChannel.close();
            }
        });
	}


}

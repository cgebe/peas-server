package receiver.handler.forward.upstream;

import receiver.server.ReceiverServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;

public class ReturnHandler extends SimpleChannelInboundHandler<HttpObject> {

	private ReceiverServer server;

    public ReturnHandler(ReceiverServer server) {
        this.server = server;
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject toReturn) throws Exception {
		HttpResponse res = (HttpResponse) toReturn;
		Channel ret = server.getClients().remove(res.headers().get("PEAS-Cluster-ID"));
		
        ChannelFuture f = ret.writeAndFlush(res);
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
            	//server.getClients().get(toReturn.getHeader().getReceiverID()).close();
            	//Channel ch = server.getClients().remove(toReturn.getHeader().getReceiverID());
                if (future.isSuccess()) {
                	//System.out.println("successful return");
                	ret.close();
                } else {
                	//System.out.println("failed return");
                	ret.close();
                }
                
            }
        });
	}



}

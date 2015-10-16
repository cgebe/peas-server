package receiver.handler.upstream;

import java.util.Random;


import receiver.handler.forward.upstream.ForwardChannelInitializer;
import receiver.server.ReceiverServer;
import util.IpValidator;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

public class ForwardHandler extends SimpleChannelInboundHandler<HttpObject> {

	private ReceiverServer server;

	public ForwardHandler(ReceiverServer server) {
		this.server = server;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject obj) throws Exception {
		HttpRequest req = (HttpRequest) obj;
		Channel inboundChannel = ctx.channel();
		
		
		// validate request
		if (IpValidator.validate(req.headers().get("PEAS-Issuer"))) {
	        String ip = req.headers().get("PEAS-Issuer").substring(0, req.headers().get("PEAS-Issuer").indexOf(':'));
	        int port = Integer.parseInt(req.headers().get("PEAS-Issuer").substring(req.headers().get("PEAS-Issuer").indexOf(':') + 1));
	        
			// Generate id for this client/peas request
			String id;
			do {
				// TODO: replace with primal number generation
				id = randomID(32);
			} while (server.getClients().get(id) != null);
			
			server.getClients().put(id, inboundChannel);
			req.headers().set("PEAS-Cluster-ID", id);
	        
			if (server.getIssuers().get(req.headers().get("PEAS-Issuer")) == null) {
		        // Start the connection attempt.
		        Bootstrap b = new Bootstrap();
		        b.group(inboundChannel.eventLoop())
		         .channel(ctx.channel().getClass())
		         .handler(new ForwardChannelInitializer(server));
		        
		        ChannelFuture f = b.connect(ip, port);

		        Channel ch = f.channel();
		        server.getIssuers().put(req.headers().get("PEAS-Issuer"), ch);
		        
		        f.addListener(new ChannelFutureListener() {
		            @Override
		            public void operationComplete(ChannelFuture future) {
		                if (future.isSuccess()) {
		                	System.out.println("Connection To Issuer Established");
		                	ChannelFuture f = ch.writeAndFlush(req);
		                	
		                	f.addListener(new ChannelFutureListener() {
		    		            @Override
		    		            public void operationComplete(ChannelFuture future) {
		    		                if (future.isSuccess()) {

		    		                } else {
		    		                	future.cause().printStackTrace();
		    		                	inboundChannel.close();
		    		                }
		    		            }
		    		        });
		                } else {
		                	// TODO: send response not okay issuer not available

		                    // Close the connection if the connection attempt has failed.
		                	System.out.println("Connection To Issuer Failed");
		                    inboundChannel.close();
		                }
		            }
		        });
			} else {
				server.getIssuers().get(req.headers().get("PEAS-Issuer")).writeAndFlush(req);
			}
	    } else {
	    	// send response not ok
	        System.out.println("invalid format");
	    }
		
	}
	
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	super.channelInactive(ctx);
    	//System.out.println("inactive");
    }

	static final String AB = "0123456789abcdefghijklmnopqrstuvwxyz";							
	static Random rnd = new Random();
	
	private String randomID(int length) 
	{
	   StringBuilder sb = new StringBuilder(length);
	   for( int i = 0; i < length; i++ ) 
	      sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
	   return sb.toString();
	}



}

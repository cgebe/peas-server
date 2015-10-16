package issuer.server;

import util.Config;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import issuer.handler.upstream.IssuerChannelInitializer;

/**
 * Discards any incoming data.
 */
public class IssuerServer {

    private int port;

    public IssuerServer(int port) {
        this.port = port;

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
    }

    public void run() throws Exception {
    	//System.out.println(Runtime.getRuntime().availableProcessors());
        EventLoopGroup bossGroup = new NioEventLoopGroup(Integer.parseInt(Config.getInstance().getValue("BOSS_CORES"))); 
        EventLoopGroup workerGroup = new NioEventLoopGroup(Integer.parseInt(Config.getInstance().getValue("WORKER_CORES")));
        
        try {
            ServerBootstrap b = new ServerBootstrap(); 
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class) 
             .childHandler(new IssuerChannelInitializer());
            
    		// Logging on?
    		if (Config.getInstance().getValue("LOGGING").equals("on")) {
    			//b.handler(new LoggingHandler(LogLevel.INFO));
    		}

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync(); 

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        //new IssuerServer(Integer.parseInt(Config.getInstance().getValue("port"))).run();
    	new IssuerServer(11779).run();
    }
    
}
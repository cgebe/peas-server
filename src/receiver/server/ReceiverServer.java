package receiver.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import receiver.handler.upstream.ReceiverChannelInitializer;
import util.Config;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
/**
 * Discards any incoming data.
 */
public class ReceiverServer {

    private int port;
    private Map<String, Channel> issuers;
    private Map<String, Channel> clients;

    public ReceiverServer(int port) {
        this.port = port;
    	this.setIssuers(new ConcurrentHashMap<String, Channel>());
    	this.setClients(new ConcurrentHashMap<String, Channel>());
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(Integer.parseInt(Config.getInstance().getValue("BOSS_CORES")));
        EventLoopGroup workerGroup = new NioEventLoopGroup(Integer.parseInt(Config.getInstance().getValue("WORKER_CORES")));
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ReceiverChannelInitializer(this))
             .option(ChannelOption.SO_BACKLOG, 2000);

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
        //new RecieverServer(Integer.parseInt(Config.getInstance().getValue("port"))).run();
    	new ReceiverServer(11777).run();
    }

	public Map<String, Channel> getIssuers() {
		return issuers;
	}

	public void setIssuers(Map<String, Channel> issuers) {
		this.issuers = issuers;
	}

	public Map<String, Channel> getClients() {
		return clients;
	}

	public void setClients(Map<String, Channel> clients) {
		this.clients = clients;
	}
	
	
}
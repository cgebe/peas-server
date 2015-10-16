package issuer.handler.upstream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import util.Config;
import util.Encryption;
import util.HttpFormatException;
import util.HttpRequestParser;
import util.Pair;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import issuer.handler.dispatch.upstream.DispatchChannelInitializer;
import issuer.server.IssuerServer;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;


public class QueryHandler extends SimpleChannelInboundHandler<HttpObject> {


	private static final int KEY_SIZE = 16;
	private PKCS1Encoding RSAdecipher;
	private IvParameterSpec iv;
	private ExecutorService executor;

	public QueryHandler() throws IOException, URISyntaxException {
        byte[] ivBytes = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        iv = new IvParameterSpec(ivBytes);
        
        String jarPath = new File(QueryHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath();
        InputStream inputStream = new FileInputStream(new File(jarPath + "/resources/privKey2.der"));
        byte[] keyBytes = IOUtils.toByteArray(inputStream);
        AsymmetricKeyParameter privateKey = PrivateKeyFactory.createKey(keyBytes);
        
        RSAdecipher = new PKCS1Encoding(new RSAEngine());
        RSAdecipher.init(false, privateKey);
        
        executor = Executors.newFixedThreadPool(Integer.parseInt(Config.getInstance().getValue("WORKER_CORES")));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext arg0, Throwable arg1) throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject obj) throws Exception {
		HttpRequest req = (HttpRequest) obj;
		if (req.headers().get("PEAS-Command").equals("QUERY")) {
			Runnable queryHandler = new QueryHandlerThread(ctx, req);
			executor.execute(queryHandler);
		}
	}
	
	
	private Pair<SecretKey, String> getSecretKeyAndQueryFromQueryField(String field) throws InvalidCipherTextException {
		SecretKey sKey;
		String query;
		
		byte[] decoded = Base64.decodeBase64(field);
		ByteBuf keyAndQuery = Unpooled.wrappedBuffer(decoded);
		
	    if (keyAndQuery.capacity() <= 128) {
	        // CASE 1: {K | Q}_RSA
	    	// E(K + Q1)
			keyAndQuery = RSAdecrypt(keyAndQuery);
			// Extract the Symmetric Key
			sKey = extractSecretKey(keyAndQuery);
	    	
	        query = new String(keyAndQuery.readerIndex(KEY_SIZE).readBytes(keyAndQuery.capacity() - KEY_SIZE).array());
	    } else {
	    	// CASE 2: {K | Q_1}_RSA | {Q_2}_AES
	        // Decipher the first part
	    	// K + Q1
			ByteBuf firstPartDecrypted = RSAdecrypt(keyAndQuery.readBytes(128));
			// Extract the Symmetric Key
			sKey = extractSecretKey(firstPartDecrypted);

	    	// Q1
	        ByteBuf partOne = firstPartDecrypted.readerIndex(KEY_SIZE).readBytes(117 - KEY_SIZE); // 128 because RSA output when using 1024bit key is 128 bytes long

	        // Q2
	        ByteBuf partTwo = Unpooled.wrappedBuffer(Encryption.AESdecrypt(keyAndQuery.readerIndex(128).readBytes(keyAndQuery.capacity() - 128).array(), sKey, iv)); // read the rest

	        ByteBuf queryConcat = Unpooled.buffer(partOne.capacity() + partTwo.capacity());
	        queryConcat.writeBytes(partOne);
	        queryConcat.writeBytes(partTwo);

	        query = new String(queryConcat.array());
	    }
	    
	    return Pair.createPair(sKey, query);
	}
	
	private ByteBuf RSAdecrypt(ByteBuf encrypted) throws InvalidCipherTextException {
      	byte[] decrypted = RSAdecipher.processBlock(encrypted.array(), 0, encrypted.array().length);
      	return Unpooled.wrappedBuffer(decrypted);
	}
	
    private SecretKey extractSecretKey(ByteBuf concat) {  
    	ByteBuf key = concat.readerIndex(0).readBytes(KEY_SIZE);
        return new SecretKeySpec(key.array(), "AES");         
    }
	
    public class QueryHandlerThread implements Runnable {
        
        private ChannelHandlerContext ctx;
		private FullHttpRequest req;

		public QueryHandlerThread(ChannelHandlerContext ctx, HttpRequest req){
            this.ctx = ctx;
            this.req = (FullHttpRequest) req;
        }
     
        @Override
        public void run() {
        	Pair<SecretKey, String> keyAndQery;
			try {
				keyAndQery = getSecretKeyAndQueryFromQueryField(req.headers().get("PEAS-Query"));
				System.out.println("query:");
				System.out.println(keyAndQery.getElement1());
				System.out.println();
				
				String content = new String(Encryption.AESdecrypt(req.duplicate().content().array(), keyAndQery.getElement0(), iv));
				System.out.println("content:");
				System.out.println(content);
				
				// create search engine request out of decrypted content
				FullHttpRequest searchReq = null;
				try {
					searchReq = createHttpRequest(content);
				} catch (HttpFormatException | IOException e) {
					// return invalid reponse
				}
				
				// send request to search engine
				// Start the connection attempt.
		        Bootstrap b = new Bootstrap();
		        b.group(ctx.channel().eventLoop())
		         .channel(ctx.channel().getClass())
		         .handler(new DispatchChannelInitializer(ctx.channel()));
		        
		        ChannelFuture c = b.connect(searchReq.headers().get("Host"), 80);
		        
				// send response back
	            ChannelFuture f = ctx.writeAndFlush(searchReq);
	            
	            f.addListener(new ChannelFutureListener() {
	                @Override
	                public void operationComplete(ChannelFuture future) {
	                    if (future.isSuccess()) {
	                    	//System.out.println("return query successful");
	                    } else {
	                        //System.out.println("return query failed");
	                    }
	                }
	            });
			} catch (InvalidCipherTextException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }


     
    }
    
    private FullHttpRequest createHttpRequest(String requestString) throws IOException, HttpFormatException {
		HttpRequestParser parser = new HttpRequestParser();
		
		// parse request String
		parser.parseRequest(requestString);
		
		// create the request to the search engine
		HttpVersion version = convertRequestVersion(parser.getRequestHttpVersion());
		HttpMethod method = convertRequestMethod(parser.getRequestMethod());
		
		// construct request with request line
		FullHttpRequest req = new DefaultFullHttpRequest(version, method, parser.getRequestPath());

		// add all header fields
    	for (Map.Entry<String, String> entry : parser.headers().entrySet()) {
    		req.headers().set(entry.getKey(), entry.getValue());
    	}
    	
    	// add message content if available
    	if (parser.getMessageBody() != null) {
    		req.content().writeBytes(parser.getMessageBody().getBytes());
    	}
    	
    	return req;
    }
    
	private HttpVersion convertRequestVersion(String requestHttpVersion) {
		HttpVersion version;
		switch(requestHttpVersion) {
		case "HTTP/1.0":
			version = HttpVersion.HTTP_1_0;
			break;
		case "HTTP/1.1":
			version = HttpVersion.HTTP_1_1;
			break;
		default:
			version = null;
		}
		return version;
	}
    
    private HttpMethod convertRequestMethod(String requestMethod) {
    	HttpMethod method;
    	switch(requestMethod) {
		case "CONNECT":
			method = HttpMethod.CONNECT;
			break;
		case "DELETE":
			method = HttpMethod.DELETE;
			break;
		case "HEAD":
			method = HttpMethod.HEAD;
			break;
		case "GET":
			method = HttpMethod.GET;
			break;
		case "OPTIONS":
			method = HttpMethod.OPTIONS;
			break;
		case "PATCH":
			method = HttpMethod.PATCH;
			break;
		case "POST":
			method = HttpMethod.POST;
			break;
		case "PUT":
			method = HttpMethod.PUT;
			break;
		case "TRACE":
			method = HttpMethod.TRACE;
			break;
		default:
			method = null;
		}
    	return method;
    }

}

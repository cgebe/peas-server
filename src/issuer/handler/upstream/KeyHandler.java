package issuer.handler.upstream;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import org.apache.commons.io.IOUtils;

public class KeyHandler extends SimpleChannelInboundHandler<HttpObject> {

	@Override
	public void exceptionCaught(ChannelHandlerContext arg0, Throwable arg1) throws Exception {
		arg1.printStackTrace();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject obj) throws Exception {
		HttpRequest req = (HttpRequest) obj;
		if (req.headers().get("PEAS-Command").equals("KEY")) {

            //byte[] keyBytes = Files.readAllBytes(Paths.get("./resources/").resolve("pubKey2.der"));
			String jarPath = new File(KeyHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath();
			InputStream inputStream = new FileInputStream(new File(jarPath + "/resources/pubKey2.der"));
            //InputStream inputStream = KeyHandler.class.getClassLoader().getResourceAsStream("pubKey2.der");
            byte[] keyBytes = IOUtils.toByteArray(inputStream);
            
            // construct key response
            FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            res.headers().set("PEAS-Issuer", req.headers().get("PEAS-Issuer"));
            res.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/octet-stream");
            res.headers().set(HttpHeaders.Names.CONTENT_LENGTH, keyBytes.length);
            
            res.content().writeBytes(keyBytes);

            // send reponse back
            ChannelFuture f = ctx.writeAndFlush(res);
            
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                    	System.out.println("return key successful");
                    } else {
                        System.out.println("return key failed");
                        future.channel().close();
                    }
                }
            });

		} else {
			ctx.fireChannelRead(obj);
		}
			
	}


}

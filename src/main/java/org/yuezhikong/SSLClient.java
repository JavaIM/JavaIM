package org.yuezhikong;
 
import java.io.BufferedInputStream;  
import java.io.BufferedOutputStream;  
import java.io.FileInputStream;  
import java.io.IOException;  
import java.io.InputStream;  
import java.io.OutputStream;  
import java.security.KeyStore;  
 
import javax.net.ssl.KeyManagerFactory;  
import javax.net.ssl.SSLContext;  
import javax.net.ssl.SSLSocket;  
import javax.net.ssl.TrustManagerFactory;  
 
/** 
 * SSL Client 
 *  
 */  
public class SSLClient{  
 
	private SSLSocket sslSocket;
 
	/** 
	 * 启动客户端程序 
	 * @param args 
	 */  
	public static void main(String[] args) {  
		SSLClient client = new SSLClient();  
		client.init();  
		client.process();  
	}  
 
	/** 
	 * 通过ssl socket与服务端进行连接,并且发送一个消息 
	 */  
	public void process() {  
		if (sslSocket == null) {  
			System.out.println("socket初始化错误");  
			return;  
		}  
		try {  
			InputStream input = sslSocket.getInputStream();  
			OutputStream output = sslSocket.getOutputStream();  
 
			BufferedInputStream bis = new BufferedInputStream(input);  
			BufferedOutputStream bos = new BufferedOutputStream(output);  
 
			bos.write("hello server".getBytes());  
			bos.flush();  
 
			byte[] buffer = new byte[20];  
			bis.read(buffer);  
			System.out.println(new String(buffer));  
 
			sslSocket.close();  
		} catch (IOException e) {  
			System.out.println(e);  
		}  
	}  
 
	/** 
	 * 初始化SSLSocket
	 * 导入客户端私钥KeyStore，导入客户端受信任的KeyStore(服务端的证书)
	 */  
	public void init() {  
		try {  
			SSLContext ctx = SSLContext.getInstance("SSL");  
 
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");  
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");  
 
			KeyStore ks = KeyStore.getInstance("JKS");  
			KeyStore tks = KeyStore.getInstance("JKS");  
 
			ks.load(new FileInputStream("src/clientkeys"), "clientkeys".toCharArray());  
			tks.load(new FileInputStream("src/clienttrust "), "clienttrust".toCharArray());  
 
			kmf.init(ks, "clientkeys".toCharArray());  
			tmf.init(tks);  
 
			ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);  
 
			sslSocket = (SSLSocket) ctx.getSocketFactory().createSocket("127.0.0.1", 8888);  
		} catch (Exception e) {  
			System.out.println(e);  
		}  
	}  
 
}

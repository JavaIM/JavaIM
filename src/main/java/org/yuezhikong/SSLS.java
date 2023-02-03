package demo2;
 
import java.io.BufferedInputStream;  
import java.io.BufferedOutputStream;  
import java.io.FileInputStream;  
import java.io.InputStream;  
import java.io.OutputStream;  
import java.net.Socket;  
import java.security.KeyStore;  
 
import javax.net.ssl.KeyManagerFactory;  
import javax.net.ssl.SSLContext;  
import javax.net.ssl.SSLServerSocket;  
import javax.net.ssl.TrustManagerFactory;  
 
 
public class SSLServer {  
 
	private SSLServerSocket serverSocket;  
 
	/** 
	 * 启动程序 
	 *  
	 * @param args 
	 */  
	public static void main(String[] args) {  
		SSLServer server = new SSLServer();  
		server.init();  
		server.start();  
	}  
 
	/** 
	 * SSL Server Socket 
	 */  
	public void start() {  
		if (serverSocket == null) {  
			System.out.println("socket初始化错误");  
			return;  
		}  
		System.out.println("服务端已启动");
		while (true) {  
			try {  
				Socket s = serverSocket.accept();  
				System.out.println("连接成功...");
				InputStream input = s.getInputStream();  
				OutputStream output = s.getOutputStream();  
 
				BufferedInputStream bis = new BufferedInputStream(input);  
				BufferedOutputStream bos = new BufferedOutputStream(output);  
 
				byte[] buffer = new byte[20];  
				bis.read(buffer);  
				System.out.println(new String(buffer));  
 
				bos.write("hello client".getBytes());  
				bos.flush();  
 
				s.close();  
			} catch (Exception e) {  
				System.out.println(e);  
			}  
		}  
	}  
 
	/** 
	 *  
	 * 初始化SSLServerSocket 
	 * 导入服务端私钥KeyStore，导入服务端受信任的KeyStore(客户端的证书)
	 *  
	 */  
	public void init() {  
		try {  
			SSLContext ctx = SSLContext.getInstance("SSL");  
 
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");  
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");  
 
			KeyStore ks = KeyStore.getInstance("JKS");  
			ks.load(new FileInputStream("src/serverkeys"), "serverkeys".toCharArray());  
 
			KeyStore tks = KeyStore.getInstance("JKS");  
			tks.load(new FileInputStream("src/servertrust"), "servertrust".toCharArray());  
 
			kmf.init(ks, "serverkeys".toCharArray());  
			tmf.init(tks);  
 
			ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);  
 
			serverSocket = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket(8888);  
			serverSocket.setNeedClientAuth(true);   
		} catch (Exception e) {  
			e.printStackTrace();  
		}  
	}  
}

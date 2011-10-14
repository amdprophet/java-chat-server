import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;


public class SocketListenerThread extends Thread {
	
	private ServerSocket listenSocket;
	private int port;
	private boolean isRunning;
	
	public SocketListenerThread(int port) {
		this.port = port;
	}
	
	public boolean getIsRunning() {
		return isRunning;
	}
	
	public void setIsRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}
	
	public void run() {
		try {
			isRunning = true;
			int threadId = 1;
			listenSocket = new ServerSocket(port);
			
			ChatServer.outputQueue.add("Server started on port " + port);
			
			while(isRunning) {
				// wait for a connection
				ChatServer.outputQueue.add("Waiting for connections...");
				Socket clientSocket = listenSocket.accept();
				ChatServer.outputQueue.add("Client connected from " + clientSocket.getInetAddress().toString());
				
				// start a client connection thread for the new connection
				ClientConnectionThread c = new ClientConnectionThread(threadId++, clientSocket);
				c.start();
				
				// add the ClientConnectionThread to a list of threads
				ChatServer.clients.add(c);
			}
		} catch(BindException e) {
			ChatServer.outputQueue.add("ERROR: Unable to bind to port" + port);
		} catch(IOException e) {
			ChatServer.outputQueue.add(ChatServer.stackTraceToString(e));
		} finally {
			ChatServer.outputQueue.add("Listener thread shutting down...");
			ChatServer.appIsRunning = false;
		}
	}
	
}

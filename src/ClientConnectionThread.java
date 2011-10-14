import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ClientConnectionThread extends Thread {
	
	public ConcurrentLinkedQueue<ChatMessage> outgoingMessageQueue = new ConcurrentLinkedQueue<ChatMessage>();
	
	private int threadId;
	private Socket clientSocket;
	private boolean isRunning;
	
	public ClientConnectionThread(int threadId, Socket clientSocket) {
		this.threadId = threadId;
		this.clientSocket = clientSocket;
		isRunning = true;
	}
	
	public int getThreadId() {
		return threadId;
	}
	
	public boolean getIsRunning() {
		return isRunning;
	}
	
	public void setIsRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}
	
	public void run() {
		try {
			DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());
			
			while(isRunning) {
				if(clientSocket.isOutputShutdown()) {
					isRunning = false;
				} else {
					InputStream clientInputStream = clientSocket.getInputStream();
					
					// check to see if bytes are available for reading from the socket
					// TODO: implement a better solution for non-blocking reading from sockets.
					// see http://stackoverflow.com/questions/3551337/java-sockets-nonblocking-read
					if(clientInputStream.available() > 0) {
						// read the bytes in from the socket's input stream into a string
						String msg = new BufferedReader(new InputStreamReader(clientInputStream)).readLine();
						
						// create a chat message object and set its properties
						ChatMessage chatMessage = new ChatMessage();
						chatMessage.setThreadId(threadId);
						chatMessage.setMsg(msg);
						
						// send the message to the main thread's incoming message queue
						// to redistribute to the other clients
						ChatServer.incomingMessageQueue.add(chatMessage);
					} else if(!outgoingMessageQueue.isEmpty()) { // check to see if there are messages to be sent to the client
						// pull the message off of the queue
						ChatMessage chatMessage = outgoingMessageQueue.remove();
						
						// send the message to the client
						outToClient.writeBytes("Message received from thread #"
								+ chatMessage.getThreadId() + ": " + chatMessage.getMsg() + '\n');
					} else { // sleep the thread for a bit
						Thread.sleep(1);
					}
				}
			}
		} catch (InterruptedException e) {
			ChatServer.outputQueue.add(ChatServer.stackTraceToString(e));
		} catch(IOException e) {
			ChatServer.outputQueue.add(ChatServer.stackTraceToString(e));
		}
	}
	
}

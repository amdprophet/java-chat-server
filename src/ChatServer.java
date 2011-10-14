import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ChatServer {
	
	public static ConcurrentLinkedQueue<ChatMessage> incomingMessageQueue = new ConcurrentLinkedQueue<ChatMessage>();
	public static ConcurrentLinkedQueue<String> outputQueue = new ConcurrentLinkedQueue<String>();
	public static Vector<ClientConnectionThread> clients = new Vector<ClientConnectionThread>();
	public static boolean appIsRunning;
	
	private SocketListenerThread socketListenerThread;
	
	public ChatServer(int port) {
		appIsRunning = true;
		
		try {
			// start listener thread
			socketListenerThread = new SocketListenerThread(port);
			socketListenerThread.start();
			
			// continuously poll output queue for messages, if there are
			// no messages then make the thread sleep for 1 ms
			while(appIsRunning) {
				if(!incomingMessageQueue.isEmpty()) {
					// pull the message off of the queue
					ChatMessage chatMessage = incomingMessageQueue.remove();
					int threadId = chatMessage.getThreadId();
					
					// send the message out to all of the other clients
					for(ClientConnectionThread client : clients) {
						if(client.getThreadId() != threadId) {
							client.outgoingMessageQueue.add(chatMessage);
						}
					}
				} else if(!outputQueue.isEmpty()) {
					// pull the message off of the queue
					String msg = outputQueue.remove();
					
					// TODO: add logging to a log file support here
					
					// print out the message from the queue
					System.out.println(msg);
				} else {
					Thread.sleep(1);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			// shut down the ClientConnectionThreads
			for(ClientConnectionThread client : clients) {
				if(client.isAlive()) {
					client.setIsRunning(false);
					
					// wait until the thread has ended before continuing on
					while(client.isAlive()) {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			// shut down the SocketListenerThread
			if(socketListenerThread.isAlive()) {
				socketListenerThread.setIsRunning(false);
				
				// wait until the thread has ended before continuing on
				while(socketListenerThread.isAlive()) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
 		}
	}
	
	public static String stackTraceToString(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
	
	public static void main(String[] args) {
		// default port
		int port = 7000;
		
		// check to see if there are any arguments, if there are
		// try to parse the first argument as an Integer, if successful,
		// use the override the default port with whatever the first
		// argument is equal to
		if (args.length > 0) {
		    try {
		        port = Integer.parseInt(args[0]);
		    } catch (NumberFormatException e) {
		        System.err.println("Argument must be an integer");
		        System.exit(1);
		    }
		}
		
		new ChatServer(port);
	}

}

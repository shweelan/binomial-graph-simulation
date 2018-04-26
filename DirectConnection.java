package bn;

import java.net.Socket;
import java.io.PrintStream;

public class DirectConnection {
  private int remoteNodeNumber;
  private String remoteHost;
  private int remotePort;
  private Socket socket;
  private PrintStream outputStream;
  // TODO shall we use threaded connections
  // TODO ASK shall I make the senders concurrent
  //private BlockingQueue<Task> blockingQueue = new LinkedBlockingDeque<Task>();

  public DirectConnection(int nodeNum, String host, int port) throws Exception {
    remoteNodeNumber = nodeNum;
    remoteHost = host;
    remotePort = port;
    //socket = new Socket(host, port);
    System.out.println("Connected to Node#" + remoteNodeNumber);
    //outputStream = new PrintStream(this.socket.getOutputStream());
  }

  public void sendMessage(byte[] msg) throws Exception {
    outputStream.println(msg);
    outputStream.flush();
  }

  public void close() throws Exception {
    outputStream.close();
    socket.close();
    System.out.println("Disconnected from Node#" + remoteNodeNumber);
  }
}

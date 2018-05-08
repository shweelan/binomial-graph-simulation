package bn;

import java.net.Socket;
import java.net.SocketException;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import bn.Message;
import bn.Controller;

class OutConnectionWorker implements Runnable {
  private static Controller controller = Controller.init();
  private Socket socket;
  private LinkedBlockingDeque<Message> queue;

  public OutConnectionWorker(Socket socket, LinkedBlockingDeque<Message> queue) throws Exception {
    this.socket = socket;
    this.queue = queue;
  }

  public void run() {
    try {
      OutputStream outputStream = socket.getOutputStream();
      try {
        while (true) {
          Message message = queue.take(); // Blocking
          if (message.getNumHops() == 0) {
            message.setTimestamp(controller.getTimestamp());
          }
          System.out.println("OUTGOING MESSAGE: " + message);
          outputStream.write(message.serialize());
          outputStream.flush();
          if (message.isGoodBye()) {
            break;
          }
        }
      } catch(Exception e) {
        if (!(e instanceof SocketException)) {
          e.printStackTrace();
        }
      }
      finally {
        outputStream.close();
        socket.close();
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}

public class DirectConnection {
  private static final int QUEUE_SIZE = 1024; // Messages before blocking
  private static final long QUEUE_OVERFLOW_BLOCK_TIMEOUT = 10; // Milliseconds
  private int remoteNodeNumber;
  private String remoteHost;
  private int remotePort;
  private Socket socket;
  private OutConnectionWorker worker;
  private LinkedBlockingDeque<Message> queue;

  public DirectConnection(int nodeNum, String host, int port) throws Exception {
    remoteNodeNumber = nodeNum;
    remoteHost = host;
    remotePort = port;
    socket = new Socket(host, port);
    System.out.println("Connected to Node#" + remoteNodeNumber);
    queue = new LinkedBlockingDeque<Message>(QUEUE_SIZE);
    worker = new OutConnectionWorker(socket, queue);
    Thread thread = new Thread(worker);
    thread.start();
  }

  public boolean sendMessage(Message message) {
    return queue.offer(message);
  }

  public boolean sendBlockingMessage(Message message) {
    try {
      return queue.offer(message, QUEUE_OVERFLOW_BLOCK_TIMEOUT, TimeUnit.MILLISECONDS); // Blocking
    } catch(InterruptedException e) {
      return false;
    }
  }

  public int getRemoteNodeNumber() {
    return remoteNodeNumber;
  }

  public void close() throws Exception {
    Message poison = new Message();
    queue.offer(poison);
    System.out.println("Disconnected from Node#" + remoteNodeNumber);
  }
}

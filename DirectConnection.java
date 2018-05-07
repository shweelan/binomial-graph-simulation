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
          message.setTimestamp(controller.getTimestamp());
          outputStream.write(message.serialize());
          outputStream.flush();
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

  public void stop() throws Exception {
    socket.close();
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
    queue = new LinkedBlockingDeque<Message>();
    worker = new OutConnectionWorker(socket, queue);
    Thread thread = new Thread(worker);
    thread.start();
  }

  public boolean sendMessage(Message message) {
    return queue.offer(message);
  }

  public boolean sendMessage(Message message, Long timeout) throws Exception {
    if (timeout == null) {
      timeout = QUEUE_OVERFLOW_BLOCK_TIMEOUT;
    }
    return queue.offer(message, timeout, TimeUnit.MILLISECONDS); // Blocking
  }

  public void close() throws Exception {
    worker.stop();
    System.out.println("Disconnected from Node#" + remoteNodeNumber);
  }
}

package bn;

import java.net.Socket;
import java.net.SocketException;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import bn.Message;
import bn.Controller;

class OutConnectionWorker implements Runnable {
  private static Controller controller;
  private Socket socket;
  private LinkedBlockingDeque<Message> queue;
  private int remoteNodeNumber;
  private volatile boolean terminated = false;

  public OutConnectionWorker(int nodeNum, Socket socket, LinkedBlockingDeque<Message> queue) throws Exception {
    controller = Controller.getInstance();
    remoteNodeNumber = nodeNum;
    this.socket = socket;
    this.queue = queue;
  }

  public boolean isDead() {
    return terminated;
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
        terminated = true;
        System.out.println("Disconnected from Node#" + remoteNodeNumber);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}

public class DirectConnection {
  private static final int BUFFER_SIZE = 1024 * 1024 * 1024;
  private static final int QUEUE_SIZE = 1024; // Messages before blocking
  private static final long QUEUE_OVERFLOW_BLOCK_TIMEOUT = 10; // Milliseconds
  private int remoteNodeNumber;
  private String remoteHost;
  private int remotePort;
  private Socket socket;
  private volatile boolean deactivated = false;
  private OutConnectionWorker worker;
  private LinkedBlockingDeque<Message> queue;

  public DirectConnection(int nodeNum, String host, int port) throws Exception {
    remoteNodeNumber = nodeNum;
    remoteHost = host;
    remotePort = port;
    socket = new Socket(host, port);
    socket.setSendBufferSize(BUFFER_SIZE);
    System.out.println("Connected to Node#" + remoteNodeNumber);
    queue = new LinkedBlockingDeque<Message>(QUEUE_SIZE);
    worker = new OutConnectionWorker(remoteNodeNumber, socket, queue);
    Thread thread = new Thread(worker);
    thread.start();
  }

  public boolean sendMessage(Message message) {
    return deactivated || queue.offer(message);
  }

  public boolean sendBlockingMessage(Message message) {
    try {
      return deactivated || queue.offer(message, QUEUE_OVERFLOW_BLOCK_TIMEOUT, TimeUnit.MILLISECONDS); // Blocking
    } catch(InterruptedException e) {
      return false;
    }
  }

  public int getRemoteNodeNumber() {
    return remoteNodeNumber;
  }

  public boolean isDeactivated() {
    return deactivated;
  }

  public void close() throws Exception {
    deactivated = true;
    Message poison = new Message();
    while(!queue.offer(poison, QUEUE_OVERFLOW_BLOCK_TIMEOUT, TimeUnit.MILLISECONDS));
    while(!worker.isDead()) {
      Thread.sleep(100);
    }
  }
}

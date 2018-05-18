package bn;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;
import bn.MessageRouter;
import bn.StatsUpdater;

class InConnectionWorker implements Runnable {
  private Socket socket;
  private String id;
  private MessageRouter router;
  private StatsUpdater updater;
  private long received;
  private long forwaded;
  private ArrayList<Long> latencies;
  private Controller controller;

  public InConnectionWorker(Socket socket, MessageRouter router, StatsUpdater updater) throws Exception {
    this.controller = Controller.getInstance();
    this.socket = socket;
    this.router = router;
    this.updater = updater;
    this.received = 0;
    this.forwaded = 0;
    this.latencies = new ArrayList<Long>();
    this.id = this.socket.getInetAddress() + ":" + String.valueOf(this.socket.getPort());
    System.out.println("Connection `" + this.id + "` connected!");
  }

  public void run() {
    try {
      BufferedInputStream inputStream = new BufferedInputStream(this.socket.getInputStream());
      try {
        while (true) {
          Message message = new Message(inputStream);
          message.incNumHops();
          System.out.println("INCOMING MESSAGE: " + message);
          if (message.isGoodBye()) {
            break;
          }
          else {
            received++;
            latencies.add(controller.getTimestamp() - message.getTimestamp());
            boolean routed = router.route(message);
            if (routed) {
              forwaded++;
            }
            if (received >= 1000) {
              updater.updateStats(received, forwaded, latencies);
              received = 0;
              forwaded = 0;
              latencies.clear();
            }
          }
        }
      }
      catch (Exception e) {
        if (!(e instanceof SocketException) && !(e instanceof EOFException)) {
          e.printStackTrace();
        }
      }
      finally {
        updater.updateStats(received, forwaded, latencies);
        inputStream.close();
        this.socket.close();
        System.out.println("Connection `" + this.id + "` disconnected!");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void stop() throws Exception {
    socket.close();
  }
}

class ServerWorker implements Runnable {
  private ServerSocket server = null;
  private MessageRouter router = null;
  private StatsUpdater updater = null;
  private ArrayList<InConnectionWorker> workers;

  public ServerWorker(int port, MessageRouter router, StatsUpdater updater) throws Exception {
    System.out.println("Trying to start server on port " + port);
    server = new ServerSocket(port);
    System.out.println("Server listening on port " + server.getLocalPort());
    workers = new ArrayList<InConnectionWorker>();
    this.router = router;
    this.updater = updater;
  }

  public void run() {
    try {
      while (true) {
        Socket socket = server.accept();
        InConnectionWorker worker = new InConnectionWorker(socket, router, updater);
        Thread thread = new Thread(worker);
        thread.start();
        workers.add(worker);
      }
    }
    catch (Exception e) {
      // Closing serverSocket will throw Exception then stop listener.
      if (!(e instanceof SocketException)) {
        e.printStackTrace();
      }
    }
  }

  public void stop() throws Exception {
    if (server != null) {
      server.close();
      System.out.println("Server Closed");
      for (InConnectionWorker worker : workers) {
        worker.stop();
      }
    }
  }
}

public class Server {
  private static ServerWorker worker;

  public static void startServer(int port, MessageRouter router, StatsUpdater updater) throws Exception {
    worker = new ServerWorker(port, router, updater);
    Thread thread = new Thread(worker);
    thread.start();
  }

  public static void stopServer() throws Exception {
    if (worker != null) worker.stop();
  }
}

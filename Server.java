package bn;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import bn.Route;

class ConnectionWorker implements Runnable {
  private Socket socket;
  private String id;
  private HashMap<Integer, Route> routes;

  public ConnectionWorker(Socket socket, HashMap<Integer, Route> routes) {
    this.socket = socket;
    this.routes = routes;
    this.id = this.socket.getInetAddress() + ":" + String.valueOf(this.socket.getPort());
    System.out.println("Connection `" + this.id + "` connected!");
  }

  public void run() {
    try {
      BufferedInputStream inputStream = new BufferedInputStream(this.socket.getInputStream());
      try {
        while (true) {
          Message msg = new Message(inputStream);
          // TODO statistics, routing
          // TODO USE TS for latencies
          Message.printInHex(msg.serialize());
          if (msg.isDataEnd()) {
            break;
          }
        }
      }
      catch (Exception e) {
        if (!(e instanceof SocketException)) {
          e.printStackTrace();
        }
      }
      finally {
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
  HashMap<Integer, Route> routes = null;
  private ArrayList<ConnectionWorker> workers;

  public ServerWorker(int port) throws Exception {
    server = new ServerSocket(port);
    System.out.println("Server listening on port " + server.getLocalPort());
    workers = new ArrayList<ConnectionWorker>();
  }

  public void run() {
    try {
      while (true) {
        Socket socket = server.accept();
        ConnectionWorker worker = new ConnectionWorker(socket, routes);
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

  public void setRoutes(HashMap<Integer, Route> routes) {
    this.routes = routes;
  }

  public void stop() throws Exception {
    if (server != null) {
      server.close();
      for (ConnectionWorker worker : workers) {
        worker.stop();
      }
    }
  }
}

public class Server {
  private static Thread thread;
  private static ServerWorker worker;

  public static void startServer(int port) throws Exception {
    worker = new ServerWorker(port);
    thread = new Thread(worker);
    thread.start();
  }

  public static void stopServer() throws Exception {
    worker.stop();
  }

  public static void setRoutes(HashMap<Integer, Route> routes) {
    worker.setRoutes(routes);
  }
}

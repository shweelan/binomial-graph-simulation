package bn;

import java.net.*;
import java.io.*;
import java.util.*;


class ConnectionWorker implements Runnable {
  private Socket socket;
  private String id;

  public ConnectionWorker(Socket socket) {
    this.socket = socket;
    this.id = this.socket.getInetAddress() + ":" + String.valueOf(this.socket.getPort());
    System.out.println("Connection `" + this.id + "` connected!");
  }

  public void run() {
    try {
      BufferedReader inputStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
      try {
        String inputLine;
        while ((inputLine = inputStream.readLine()) != null) {
          // TODO statistics, routing
          System.out.println(inputLine);
          if (inputLine.equals("bye")) {
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
        ConnectionWorker worker = new ConnectionWorker(socket);
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
}

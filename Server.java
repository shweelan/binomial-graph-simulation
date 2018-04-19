package bn;

import java.net.*;
import java.io.*;


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
          if (inputLine.equals("bye")) {
            break;
          }
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
}

class ServerWorker implements Runnable {
  private int port;

  public ServerWorker(int port) {
    this.port = port;
  }

  public void run() {
    try {
      ServerSocket server = new ServerSocket(port);
      System.out.println("Server listening on port " + server.getLocalPort());
      while (true) {
        Socket socket = server.accept();
        new Thread(new ConnectionWorker(socket)).start();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}

public class Server {
  private static Thread thread;
  public static void startServer(int port) throws Exception {
    thread = new Thread(new ServerWorker(port));
    thread.start();
    // TODO implement a way to kill server!
  }
}

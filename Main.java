package bn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.io.ByteArrayInputStream;
import java.util.Random;
import bn.Controller;
import bn.Server;
import bn.DirectConnection;
import bn.Message;
import bn.MessageRouter;

class Main {
  private static Controller controller = Controller.init();
  private static String host;
  private static int port;
  private static String selfId;
  private static int selfIndex;
  private static int nMax;
  private static ArrayList<String> nodes;
  private static HashMap<Integer, HashSet<Integer>> binomialGraph;
  private static HashMap<Integer, Route> routes;
  private static HashMap<Integer, DirectConnection> connections;

  private static void buildBinomialGraphNetwork() throws Exception {
    if (nodes.size() < 2) {
      throw new Exception("ERROR! Number of nodes in network < 2");
    }
    int numNodes = nodes.size();
    final double powerBase = Math.pow(numNodes, 1.0 / nMax);
    int i = 0;
    binomialGraph = new HashMap<Integer, HashSet<Integer>>();
    while (i < numNodes) {
      HashSet<Integer> links = binomialGraph.get(i);
      if (links == null) {
        links = new HashSet<Integer>();
        binomialGraph.put(i, links);
      }
      int j = 0;
      double offsetDouble;
      while ((offsetDouble = Math.pow(powerBase, j)) <= numNodes) {
        int offset = (int) Math.round(offsetDouble);
        offset = offset % numNodes;
        int target = i + offset;
        target = target % numNodes;
        if (target != i) {
          links.add(target);
        }
        j++;
      }
      i++;
    }
  }

  private static void calculateRoutes() {
    routes = new HashMap<Integer, Route>();
    routes.put(selfIndex, new Route(selfIndex, selfIndex, 0, selfIndex));
    HashMap<Integer, ArrayList<Integer>> paths = new HashMap<Integer, ArrayList<Integer>>();
    paths.put(selfIndex, new ArrayList<Integer>());
    HashSet<Integer> visited = new HashSet<Integer>();
    Queue<Integer> queue = new LinkedList<Integer>();
    queue.add(selfIndex);
    while (!queue.isEmpty()) {
      Integer parent = queue.remove(); // parent
      if (!visited.contains(parent)) {
        HashSet<Integer> children = binomialGraph.get(parent);
        ArrayList<Integer> parentPath = paths.get(parent);
        for (Integer child : children) {
          // TODO move inside if statement
          ArrayList<Integer> path = new ArrayList<Integer>();
          for (Integer node : parentPath) {
            path.add(node);
          }
          path.add(child);
          System.out.println(selfIndex + " " + child + " " + path);
          if (!routes.containsKey(child)) {
            paths.put(child, path);
            routes.put(child, new Route(selfIndex, child, path.size(), path.get(0)));
          }
          else {
            // Already found route, try to add new possible route
            Route route = routes.get(child);
            if (route.getRouteLength() == parentPath.size() + 1) {
              // Same length path
              route.addViaNode(parentPath.get(0));
            }
          }
          System.out.println(routes.get(child));
          queue.add(child);
        }
        visited.add(parent);
      }
    }
  }

  private static void openConnections() throws Exception {
    connections = new HashMap<Integer, DirectConnection>();

    for (int nodeNum : binomialGraph.get(selfIndex)) {
      String[] split = nodes.get(nodeNum).split(":");
      String host = split[0];
      int port = Integer.parseInt(split[1]);
      connections.put(nodeNum, new DirectConnection(nodeNum, host, port));
    }
  }

  private static boolean sendMessage(Message message) {
    final int destination = message.getDestination();
    final int nextHop = routes.get(destination).getRandomViaNode();
    DirectConnection connection = connections.get(nextHop);
    // TODO throw Exception if connection == null
    // try non-blocking then blocking send
    return connection.sendMessage(message) || connection.sendBlockingMessage(message);
  }

  private static void route(Message message) {
    // reportIncomming(message);
    if (message.getDestination() != selfIndex) {
      while (!sendMessage(message));
    }
  }

  private static void startSimulation() throws Exception {
    System.out.println("Simulation Started");
    System.out.println(selfIndex);
    System.out.println(binomialGraph);
    System.out.println(nodes);
    System.out.println(routes);

    // TODO read the config (message size, num messages | duration, ..) from redis
    int messageSize = 1024;
    long numMessages = 200;

    Random random = new Random();
    long messagesSent = 0;

    while (messagesSent < numMessages) {
      int destination = random.nextInt(nodes.size());
      if (destination == selfIndex) continue;
      long ts = controller.getTimestamp();
      Message message = new Message(selfIndex, destination, ts, messageSize);
      if (sendMessage(message)) messagesSent++;
    }

    for (DirectConnection connection : connections.values()) {
      connection.close();
    }
    System.out.println("Simulation Ended");
  }

  public static void main(String args[]) throws Exception {
    try {
      host = args[0];
      port = Integer.parseInt(args[1]);
      nMax = Integer.parseInt(args[2]);
      selfId = host + ":" + port;
      MessageRouter router = Main::route;
      Server.startServer(port, router);
      controller.announceNode(selfId);
      int nodesCount = controller.getNodesCount();
      if (nodesCount < 2) {
        throw new Exception("ERROR! Number of nodes declared < 2");
      }
      while (controller.getAnnouncedNodesCount() < nodesCount) {
        Thread.sleep(1000);
      }
      nodes = controller.getAnnouncedNodes();
      selfIndex = nodes.indexOf(selfId);
      buildBinomialGraphNetwork();
      calculateRoutes();
      openConnections();
      controller.incReadyCount();
      while (controller.getReadyCount() < nodesCount) {
        Thread.sleep(1000);
      }
      startSimulation();
      controller.delAnnouncedNode(selfId);
      while (controller.getAnnouncedNodesCount() > 0) {
        Thread.sleep(1000);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      Server.stopServer();
    }
  }
}

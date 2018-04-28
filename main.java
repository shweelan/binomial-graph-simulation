package bn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import bn.Controller;
import bn.Server;
import bn.DirectConnection;
import bn.Helper;
import bn.Route;

class Main {
  private static String host;
  private static int port;
  private static String selfId;
  private static int selfIndex;
  private static int nMax;
  private static ArrayList<String> nodes;
  private static HashMap<Integer, HashSet<Integer>> binomialGraph;
  private static HashMap<Integer, Route> routes;

  private static void buildBinomialGraphNetwork() throws Exception {
    if (nodes.size() < 2) {
      throw new Exception("ERROR! Number of nodes in network < 2");
    }
    int numNodes = nodes.size();
    // TODO Enable if duplex, final double powerBase = Math.pow(numNodes * 1.0, (2.0 / (nMax * 1.0)));
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
          // TODO Ask if we need to add it as duplex channel.
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

  public static void startSimulation() throws Exception {
    System.out.println("Simulation Started");
    System.out.println(selfIndex);
    System.out.println(binomialGraph);
    System.out.println(nodes);
    System.out.println(routes);

    // TODO read the config (message size, num messages | duration, ..) from redis

    HashMap<Integer, DirectConnection> connections = new HashMap<Integer, DirectConnection>();

    for (int nodeNum : binomialGraph.get(selfIndex)) {
      String[] split = nodes.get(nodeNum).split(":");
      String host = split[0];
      int port = Integer.parseInt(split[1]);
      connections.put(nodeNum, new DirectConnection(nodeNum, host, port));
    }

    int dest = routes.get(0).getRandomViaNode();
    int messageSize = 1024;
    byte[] msg = Helper.buildMessage(selfIndex, dest, messageSize);
    Helper.bytesToHex(msg);
    // TODO start a loop
    // TODO hash random data, send the data through the route
    Thread.sleep(5000);
    System.out.println("Simulation Ended");
  }

  private static void startBootBash() {
    // TODO implement or block bash script till java exits then loop again.
  }

  public static void main(String args[]) throws Exception {
    try {
      Controller controller = Controller.init();
      host = args[0];
      port = Integer.parseInt(args[1]);
      nMax = Integer.parseInt(args[2]);
      selfId = host + ":" + port;
      Server.startServer(port);
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
      Server.setRoutes(routes);
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
    finally {
      Server.stopServer();
      startBootBash();
    }
  }
}

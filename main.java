package bn;

import java.io.*;
import java.util.*;
import bn.Controller;
import bn.Server;
import bn.DirectConnection;
import bn.Helper;

class Main {
  private static String host;
  private static int port;
  private static String selfId;
  private static int selfIndex;
  private static int nMax;
  private static ArrayList<String> nodes;
  private static HashMap<Integer, HashSet<Integer>> binomialGraph;
  private static HashMap<Integer, ArrayList<Integer>> routes;

  private static void startServer() throws Exception {
    Server.startServer(port);
  }

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
    routes = new HashMap<Integer, ArrayList<Integer>>();
    routes.put(selfIndex, new ArrayList<Integer>());
    //routes.get(selfIndex).add(selfIndex);
    HashSet<Integer> visited = new HashSet<Integer>();
    Queue<Integer> queue = new LinkedList<Integer>();
    queue.add(selfIndex);
    while (!queue.isEmpty()) {
      // TODO calculate routes for all parents, the first parent will be overused, i.e. calculate all possible routes, maybe different algo
      Integer currentNode = queue.remove(); // parent
      if (!visited.contains(currentNode)) {
        HashSet<Integer> children = binomialGraph.get(currentNode);
        ArrayList<Integer> currentRoute = routes.get(currentNode);
        for (Integer child : children) {
          if (!routes.containsKey(child)) {
            ArrayList<Integer> route = new ArrayList<Integer>();
            for (Integer node : currentRoute) {
              route.add(node);
            }
            route.add(child);
            routes.put(child, route);
          }
          queue.add(child);
        }
        visited.add(currentNode);
      }
    }
  }

  public static void startSimulation() throws Exception {
    System.out.println("Simulation Started");
    System.out.println(selfIndex);
    System.out.println(binomialGraph);
    System.out.println(nodes);
    System.out.println(routes);

    // TODO read the config (message size, num messages | duration, ..)

    HashMap<Integer, DirectConnection> connections = new HashMap<Integer, DirectConnection>();

    for (int nodeNum : binomialGraph.get(selfIndex)) {
      String[] split = nodes.get(nodeNum).split(":");
      String host = split[0];
      int port = Integer.parseInt(split[1]);
      connections.put(nodeNum, new DirectConnection(nodeNum, host, port));
    }

    byte[] serializedSelfIndex = Helper.serialize(selfIndex);

    HashMap<Integer, byte[]> serializedRoutes = new HashMap<Integer, byte[]>();
    for (int i = 0; i < nodes.size(); i++) {
      if (i != selfIndex) {
        serializedRoutes.put(i, Helper.serialize(routes.get(i)));
        //System.out.println(routes.get(i));
        //Helper.bytesToHex(serializedRoutes.get(i));
      }
    }

    int messageSize = 1024;
    byte[] msg = Helper.buildMessage(serializedSelfIndex, serializedRoutes.get(25), messageSize);
    Helper.bytesToHex(msg);
    // TODO start a loop
    // TODO hash random data, send the data through the route
    Thread.sleep(5000);
    System.out.println("Simulation Ended");
  }

  private static void stopServer() throws Exception {
    Server.stopServer();
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
      startServer();
      controller.announceNode(selfId);
      int nodesCount = controller.getNodesCount();
      if (nodesCount < 2) {
        throw new Exception("ERROR! Number of nodes declared < 2");
      }
      while (controller.getAnnouncedNodesCount() < nodesCount) {
        Thread.sleep(1000);
      }
      //controller.resetNodesCount();
      nodes = controller.getAnnouncedNodes();
      selfIndex = nodes.indexOf(selfId);
      buildBinomialGraphNetwork();
      calculateRoutes();
      startSimulation();
      controller.delAnnouncedNode(selfId);
      while (controller.getAnnouncedNodesCount() > 0) {
        Thread.sleep(1000);
      }
    }
    finally {
      stopServer();
      startBootBash();
    }
  }
}

package bn;

import java.io.*;
import java.util.*;
import bn.Controller;
import bn.Server;

class Main {
  private static void startServer(int port) throws Exception {
    Server.startServer(port);
  }

  private static HashMap<Integer, HashSet<Integer>> buildBinomialGraphNetwork(ArrayList<String> nodes, int nMax) throws Exception {
    int numNodes = nodes.size();
    // TODO Enable if duplex, final double powerBase = Math.pow(numNodes * 1.0, (2.0 / (nMax * 1.0)));
    final double powerBase = Math.pow(numNodes, 1.0 / nMax);
    int i = 0;
    HashMap<Integer, HashSet<Integer>> binomialGraph = new HashMap<Integer, HashSet<Integer>>();
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
    return binomialGraph;
  }

  private static HashMap<Integer, ArrayList<Integer>> calculateRoutes(int selfIndex, HashMap<Integer, HashSet<Integer>> binomialGraph) {
    return null;
  }

  public static void startSimulation(HashMap<Integer, ArrayList<Integer>> routes) throws Exception {
    Thread.sleep(10000);
  }

  private static void stopServer() {

  }

  private static void startBootBash() {
    // TODO implement or block till exit then loop again.
  }

  public static void main(String args[]) throws Exception {
    Controller controller = Controller.init();
    final String myIp = args[0];
    final int myPort = Integer.parseInt(args[1]);
    final int nMax = Integer.parseInt(args[2]);
    final String selfId = myIp + ":" + myPort;
    startServer(myPort);
    controller.announceNode(selfId);
    int nodesCount = controller.getNodesCount();
    while(controller.getAnnouncedNodesCount() < nodesCount) {
      Thread.sleep(1);
    }
    controller.resetNodesCount();
    ArrayList<String> nodes = controller.getAnnouncedNodes();
    int selfIndex = nodes.indexOf(selfId);
    HashMap<Integer, HashSet<Integer>> binomialGraph = buildBinomialGraphNetwork(nodes, nMax);
    HashMap<Integer, ArrayList<Integer>> routes = calculateRoutes(selfIndex, binomialGraph);
    startSimulation(routes);
    controller.delAnnouncedNode(selfId);
    while(controller.getAnnouncedNodesCount() > 0) {
      Thread.sleep(1);
    }
    stopServer();
    startBootBash();
  }
}

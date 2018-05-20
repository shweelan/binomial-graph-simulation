package bn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.io.ByteArrayInputStream;
import java.util.Random;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentLinkedQueue;
import bn.Controller;
import bn.Server;
import bn.DirectConnection;
import bn.Message;
import bn.MessageRouter;
import bn.StatsUpdater;

class Main {
  private static Controller controller;
  private static String testId = "default";
  private static String host;
  private static int port;
  private static String selfId;
  private static int selfIndex;
  private static int nMax = 3;
  private static int numMessages = 1000;
  private static int messageSize = 1024;
  private static boolean useDirectConnections = false;
  private static ArrayList<String> nodes;
  private static HashMap<Integer, HashSet<Integer>> binomialGraph;
  private static HashMap<Integer, Route> routes;
  private static HashMap<Integer, DirectConnection> connections;

  private static LongAdder messagesSent;
  private static LongAdder messagesReceived;
  private static LongAdder messagesForwarded;
  private static ConcurrentLinkedQueue<Long> latencies;

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
    Route route = routes.get(destination);
    final int nextHop = route.getRandomViaNode();
    DirectConnection connection = connections.get(nextHop);
    // TODO throw Exception if connection == null
    // try non-blocking then blocking send
    boolean sent = connection.sendMessage(message) || connection.sendBlockingMessage(message);
    if (sent) {
      route.used();
      messagesSent.increment();
    }
    return sent;
  }

  private static void updateStats(ArrayList<Long> lats) {
    latencies.addAll(lats);
  }

  private static void route(Message message) {
    messagesReceived.increment();
    if (message.getDestination() != selfIndex) {
      while (!sendMessage(message));
      messagesForwarded.increment();
      System.out.println("FORWARD MESSAGE:" + message);
    }
  }

  private static void startSimulation() throws Exception {
    System.out.println("Simulation Started");
    System.out.println(selfIndex);
    System.out.println(binomialGraph);
    System.out.println(nodes);
    System.out.println(routes);

    Random random = new Random();
    int messagesCount = 0;

    while (messagesCount < numMessages) {
      int destination = random.nextInt(nodes.size());
      if (destination == selfIndex) continue;
      long ts = controller.getTimestamp();
      Message message = new Message(selfIndex, destination, ts, messageSize);
      if (sendMessage(message)) {
        messagesCount++;
        if (messagesCount % 101 == 0) Thread.sleep(100);
      }
    }

    for (DirectConnection connection : connections.values()) {
      connection.close();
    }
    System.out.println("Simulation Ended");
  }

  private static void recordResults(long simulationTime) throws Exception {
    ArrayList<String> csv = new ArrayList<String>();
    csv.add(selfId);
    csv.add(String.valueOf(selfIndex));
    csv.add(String.valueOf(simulationTime));
    try {
      if (simulationTime <= 0) throw new Exception();
      Long[] latenciesArray = new Long[latencies.size()];
      latenciesArray = latencies.toArray(latenciesArray);
      Arrays.sort(latenciesArray);
      long sum = 0;
      for(Long latency : latenciesArray) {
        sum += latency;
      }
      float averageLatency = sum / (float) latenciesArray.length;
      long minLatency = latenciesArray[0];
      long maxLatency = latenciesArray[latenciesArray.length - 1];
      int mid = latenciesArray.length / 2;
      long medianLatency = latenciesArray[mid];
      if (latenciesArray.length % 2 == 0) {
        medianLatency += latenciesArray[mid - 1];
        medianLatency /= 2;
      }
      long percentile1Latency = latenciesArray[latenciesArray.length / 100];
      long percentile25Latency = latenciesArray[latenciesArray.length / 4];
      long percentile75Latency = latenciesArray[latenciesArray.length * 3 / 4];
      long percentile99Latency = latenciesArray[latenciesArray.length * 99 / 100];
      csv.add("OK");
      csv.add(messagesSent.toString());
      csv.add(messagesReceived.toString());
      csv.add(messagesForwarded.toString());
      csv.add(String.valueOf(averageLatency));
      csv.add(String.valueOf(minLatency));
      csv.add(String.valueOf(percentile1Latency));
      csv.add(String.valueOf(percentile25Latency));
      csv.add(String.valueOf(medianLatency));
      csv.add(String.valueOf(percentile75Latency));
      csv.add(String.valueOf(percentile99Latency));
      csv.add(String.valueOf(maxLatency));
      csv.add(String.valueOf(controller.getTimeDiff()));
    } catch(Exception e) {
      e.printStackTrace();
      csv.add("ERROR");
    }
    String results = String.join(",", csv);
    System.out.println("RESULTS: " + results);
    String[] header = {
      "InstanceIndex",
      "InstanceId",
      "SimulationTime", // TODO Pre and post simulation time
      "TestStatus",
      "MessagesSent",
      "MessagesReceived",
      "MessagesForwarded",
      "AverageLatency",
      "MinLatency",
      "1PercentileLatency",
      "25PercentileLatency",
      "MedianLatency",
      "75PercentileLatency",
      "99PercentileLatency",
      "MaxLatency",
      "RemoteTimestampDiff"
    };
    controller.recordResults(testId, header, selfId, results);
  }

  public static void main(String args[]) throws Exception {
    long simulationTime = -1;
    try {
      host = args[0];
      port = Integer.parseInt(args[1]);
      selfId = host + ":" + port;
      messagesSent = new LongAdder();
      messagesReceived = new LongAdder();
      messagesForwarded = new LongAdder();
      latencies = new ConcurrentLinkedQueue<Long>();
      if (args.length > 2) Controller.setUrl(args[2]);
      controller = Controller.getInstance();
      testId = controller.getTestId();
      HashMap<String, String> config = controller.getConfig();
      if (config.containsKey("nmax")) nMax = Integer.parseInt(config.get("nmax"));
      if (config.containsKey("msgcount")) numMessages = Integer.parseInt(config.get("msgcount"));
      if (config.containsKey("msgsize")) messageSize = Integer.parseInt(config.get("msgsize"));
      if (config.containsKey("usedirect")) useDirectConnections = Boolean.parseBoolean(config.get("usedirect"));
      MessageRouter router = Main::route;
      StatsUpdater updater = Main::updateStats;
      Server.startServer(port, router, updater);
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
      long ts = System.currentTimeMillis();
      startSimulation();
      simulationTime = System.currentTimeMillis() - ts;
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
      recordResults(simulationTime);
    }
  }
}

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
import java.util.Collections;
import java.util.Comparator;
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
  private static int messageSize = 4096;
  private static int extraConnectionsCount = 0;
  private static long reRoutingFrequency = 1000; // milliseconds
  private static int[] dataDistribution = null;
  private static ArrayList<String> nodes;
  private static HashMap<Integer, HashSet<Integer>> binomialGraph;
  private static HashMap<Integer, Route> routes;
  private static HashMap<Integer, DirectConnection> connections;
  private static HashMap<Integer, DirectConnection> extraConnections;
  private static volatile boolean dirtyExtraConnections = false;
  private static volatile boolean simulationStarted = false;
  private static volatile boolean simulationEnded = false;
  private static LongAdder messagesSentDuringSimulation;
  private static LongAdder messagesReceivedDuringSimulation;
  private static LongAdder messagesForwardedDuringSimulation;
  private static LongAdder messagesSentBeforeSimulation;
  private static LongAdder messagesReceivedBeforeSimulation;
  private static LongAdder messagesForwardedBeforeSimulation;
  private static LongAdder messagesSentAfterSimulation;
  private static LongAdder messagesReceivedAfterSimulation;
  private static LongAdder messagesForwardedAfterSimulation;
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
          if (!routes.containsKey(child)) {
            ArrayList<Integer> path = new ArrayList<Integer>();
            path.addAll(parentPath);
            path.add(child);
            paths.put(child, path);
            routes.put(child, new Route(selfIndex, child, path.size(), path.get(0)));
          }
          else {
            // Already found route, try to add new possible route
            Route route = routes.get(child);
            // Same length path
            if (route.getRouteLength() == parentPath.size() + 1) {
              route.addViaNode(parentPath.get(0));
            }
          }
          queue.add(child);
        }
        visited.add(parent);
      }
    }
  }

  private static DirectConnection connectTo(int destination) throws Exception {
    String[] split = nodes.get(destination).split(":");
    String host = split[0];
    int port = Integer.parseInt(split[1]);
    return new DirectConnection(destination, host, port);
  }

  private static void relieveWorstRoutes(Comparator<Route> comparator) throws Exception {
    if (extraConnectionsCount <= 0) {
      return;
    }
    dirtyExtraConnections = true;
    HashMap<Integer, DirectConnection> clone =  new HashMap<Integer, DirectConnection>();
    clone.putAll(extraConnections);
    extraConnections.clear();
    ArrayList<Route> worstRoutes = new ArrayList<Route>(routes.values());
    HashSet<Integer> routesToRelieve = new HashSet<Integer>();
    Collections.sort(worstRoutes, comparator);
    System.out.println("###################### " + worstRoutes);
    int i = 0;
    for (Route route : worstRoutes) {
      if (route.getRouteLength() > 1) {
        routesToRelieve.add(route.getDestination());
        if (++i >= extraConnectionsCount) {
          break;
        }
      }
    }
    System.out.println("###################### " + routesToRelieve);
    for (int destination : routesToRelieve) {
      if (clone.containsKey(destination)) {
        // Connection already exists
        extraConnections.put(destination, clone.remove(destination));
      }
      else {
        extraConnections.put(destination, connectTo(destination));
      }
    }
    for (int destination : clone.keySet()) {
      clone.get(destination).close();
    }
    System.out.println("###################### " + extraConnections);
    dirtyExtraConnections = false;
  }

  private static void relieveLongestRoutes() throws Exception {
    relieveWorstRoutes(Route.getDescendingLengthComparator());
  }

  private static void relieveConjestedRoutes() throws Exception {
    relieveWorstRoutes(Route.getDescendingReductionComparator());
    for (Route route : routes.values()) {
      route.resetUsage();
    }
  }

  private static void openConnections() throws Exception {
    connections = new HashMap<Integer, DirectConnection>();
    extraConnections = new HashMap<Integer, DirectConnection>();
    for (int nodeNum : binomialGraph.get(selfIndex)) {
      connections.put(nodeNum, connectTo(nodeNum));
    }
    relieveLongestRoutes();
  }

  private static void increment(LongAdder counterDuring, LongAdder counterBefore, LongAdder counterAfter) {
    if (simulationEnded) {
      counterAfter.increment();
    }
    else if (simulationStarted) {
      counterDuring.increment();
    }
    else {
      counterBefore.increment();
    }
  }

  private static boolean sendMessage(Message message) {
    final int destination = message.getDestination();
    Route route = routes.get(destination);
    DirectConnection connection = null;
    if (!dirtyExtraConnections) connection = extraConnections.get(destination);
    if (connection != null) {
      System.out.println("###################### SENDING DIRECT" + destination);
    }
    if (connection == null || connection.isDeactivated()) {
      final int nextHop = route.getRandomViaNode();
      connection = connections.get(nextHop);
    }
    // try non-blocking then blocking send
    boolean sent = connection.sendMessage(message) || connection.sendBlockingMessage(message);
    if (sent) {
      route.used();
      increment(messagesSentDuringSimulation, messagesSentBeforeSimulation, messagesSentAfterSimulation);
    }
    return sent;
  }

  private static void updateStats(ArrayList<Long> lats) {
    latencies.addAll(lats);
  }

  private static void route(Message message) {
    increment(messagesReceivedDuringSimulation, messagesReceivedBeforeSimulation, messagesReceivedAfterSimulation);
    if (message.getDestination() != selfIndex) {
      while (!sendMessage(message));
      increment(messagesForwardedDuringSimulation, messagesForwardedBeforeSimulation, messagesForwardedAfterSimulation);
      System.out.println("FORWARD MESSAGE:" + message);
    }
  }

  private static void startSimulation() throws Exception {
    simulationStarted = true;
    System.out.println("Simulation Started");
    int numNodes = nodes.size();
    long[] toNodes = new long[numNodes];
    Arrays.fill(toNodes, 0L);
    Random random = new Random();
    int messagesCount = 0;

    long lastReRoute = System.currentTimeMillis();
    while (messagesCount < numMessages) {
      long now = System.currentTimeMillis();
      if (now - lastReRoute >= reRoutingFrequency) {
        relieveConjestedRoutes();
        lastReRoute = now;
      }
      int destination = -1;
      String debug = "###################";
      if (dataDistribution != null) {
        int randomDist = random.nextInt(100) + 1; // 1 - 100
        int ret = Arrays.binarySearch(dataDistribution, randomDist);
        if (ret < 0) ret = ++ret * -1;
        if (ret < dataDistribution.length && ret < numNodes) {
          destination = ret;
        }
        debug = debug + " " + dataDistribution + " " + randomDist + " " + ret;
      }
      if (destination < 0) {
        destination = random.nextInt(numNodes);
      }
      debug = debug + " " + destination;
      System.out.println(debug);
      if (destination == selfIndex) continue;
      toNodes[destination]++;
      long ts = controller.getTimestamp();
      Message message = new Message(selfIndex, destination, ts, messageSize);
      if (sendMessage(message)) {
        messagesCount++;
        if (messagesCount % 101 == 0) Thread.sleep(50);
      }
    }
    simulationEnded = true;
    System.out.println("Simulation Ended");
    System.out.println("Messages sent to each Node " + Arrays.toString(toNodes));
    relieveLongestRoutes();
  }

  private static void recordResults(long duration, long simulationTime) throws Exception {
    ArrayList<String> csv = new ArrayList<String>();
    csv.add(selfId);
    csv.add(String.valueOf(selfIndex));
    csv.add(String.valueOf(duration));
    csv.add(String.valueOf(nMax));
    csv.add(String.valueOf(numMessages));
    csv.add(String.valueOf(messageSize));
    csv.add(String.valueOf(extraConnectionsCount));
    csv.add(String.valueOf(reRoutingFrequency));
    if (dataDistribution != null) {
      csv.add("\"" + Arrays.toString(dataDistribution).replace(" ", "") + "\"");
    }
    else {
      csv.add("Random");
    }
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
      csv.add(String.valueOf(simulationTime));
      csv.add(messagesSentDuringSimulation.toString());
      csv.add(messagesReceivedDuringSimulation.toString());
      csv.add(messagesForwardedDuringSimulation.toString());
      csv.add(messagesSentBeforeSimulation.toString());
      csv.add(messagesReceivedBeforeSimulation.toString());
      csv.add(messagesForwardedBeforeSimulation.toString());
      csv.add(messagesSentAfterSimulation.toString());
      csv.add(messagesReceivedAfterSimulation.toString());
      csv.add(messagesForwardedAfterSimulation.toString());
      long totalSent = messagesSentDuringSimulation.longValue() + messagesSentBeforeSimulation.longValue() + messagesSentAfterSimulation.longValue();
      long totalReceived = messagesReceivedDuringSimulation.longValue() + messagesReceivedBeforeSimulation.longValue() + messagesReceivedAfterSimulation.longValue();
      long totalForwarded = messagesForwardedDuringSimulation.longValue() + messagesForwardedBeforeSimulation.longValue() + messagesForwardedAfterSimulation.longValue();
      csv.add(String.valueOf(totalSent));
      csv.add(String.valueOf(totalReceived));
      csv.add(String.valueOf(totalForwarded));
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
      "InstanceId",
      "InstanceIndex",
      "TestDuration",
      "NMax",
      "NumMessages",
      "MessageSize",
      "ExtraConnectionsCount",
      "ReRoutingFrequency",
      "DataDistribution",
      "TestStatus",
      "SimulationTime",
      "MessagesSentDuringSimulation",
      "MessagesReceivedDuringSimulation",
      "MessagesForwardedDuringSimulation",
      "MessagesSentBeforeSimulation",
      "MessagesReceivedBeforeSimulation",
      "MessagesForwardedBeforeSimulation",
      "MessagesSentAfterSimulation",
      "MessagesReceivedAfterSimulation",
      "MessagesForwardedAfterSimulation",
      "TotalMessagesSent",
      "TotalMessagesReceived",
      "TotalMessagesForwarded",
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
    long startTs = System.currentTimeMillis();
    long simulationTime = -1;
    try {
      host = args[0];
      port = Integer.parseInt(args[1]);
      selfId = host + ":" + port;
      messagesSentDuringSimulation = new LongAdder();
      messagesReceivedDuringSimulation = new LongAdder();
      messagesForwardedDuringSimulation = new LongAdder();
      messagesSentBeforeSimulation = new LongAdder();
      messagesReceivedBeforeSimulation = new LongAdder();
      messagesForwardedBeforeSimulation = new LongAdder();
      messagesSentAfterSimulation = new LongAdder();
      messagesReceivedAfterSimulation = new LongAdder();
      messagesForwardedAfterSimulation = new LongAdder();
      latencies = new ConcurrentLinkedQueue<Long>();
      if (args.length > 2) Controller.setUrl(args[2]);
      controller = Controller.getInstance();
      testId = controller.getTestId();
      HashMap<String, String> config = controller.getConfig();
      if (config.containsKey("nmax")) nMax = Integer.parseInt(config.get("nmax"));
      if (config.containsKey("msgcount")) numMessages = Integer.parseInt(config.get("msgcount"));
      if (config.containsKey("msgsize")) messageSize = Integer.parseInt(config.get("msgsize"));
      if (config.containsKey("extracons")) extraConnectionsCount = Integer.parseInt(config.get("extracons"));
      if (config.containsKey("reroutefreq")) reRoutingFrequency = Long.parseLong(config.get("reroutefreq"));
      if (config.containsKey("datadist")) {
        int sum = 0;
        String[] split = config.get("datadist").split(",");
        dataDistribution = new int[split.length];
        for (int i = 0; i < split.length; i++) {
          int distPercentage = Integer.parseInt(split[i].trim());
          sum += distPercentage;
          if (distPercentage <= 0 || sum >= 100) throw new Exception("ERROR! invalid data distribution percentages");
          dataDistribution[i] = sum;
        }
      }
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
      for (DirectConnection connection : connections.values()) {
        connection.close();
      }
      for (DirectConnection connection : extraConnections.values()) {
        connection.close();
      }
      Server.stopServer();
      long duration = System.currentTimeMillis() - startTs;
      recordResults(duration, simulationTime);
    }
  }
}

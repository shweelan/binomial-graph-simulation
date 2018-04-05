package bn.main;

import java.io.*;
import java.net.InetAddress;
import java.util.*;

class Main {

  private static final String REMOTE_USERNAME = "ssamson";
  private static final String BOOT_COMMAND = "BOOT";
  private static final String START_NODE_COMMAND = "START_NODE";
  private static final String WORKING_DIR = "~/binomial_network";

  public static ArrayList<String> getNodes (String nodesFile) throws Exception {
    File file = new File(nodesFile);
    BufferedReader fileStream = new BufferedReader(new FileReader(file));
    String configLine;
    ArrayList<String> nodes = new ArrayList<String>();
    while((configLine = fileStream.readLine()) != null) {
      nodes.add(configLine.trim());
    }
    fileStream.close();
    return nodes;
  }

  public static HashMap<String, List<String>> bootNodes(ArrayList<String> nodes) throws Exception {
    HashMap<String, List<String>> remoteNodesPIDs = new HashMap<String, List<String>>();
    for (String node : nodes) {
      int port = 0;
      String[] split = node.split(":");
      String host = split[0].trim();
      if (split.length > 1) {
        port = Integer.parseInt(split[1].trim());
      }
      final InetAddress addr = InetAddress.getByName(host);
      Process serverProcess = null;

      // run remote server
      List<String> command = new ArrayList<String>();
      // NOTE you need to have jvm installed, project cloned /root, and the project must be compiled using make command
      // NOTE you need to have ssh on port 22 (forced because testing on virtual machine)
      command.add("ssh");
      command.add(REMOTE_USERNAME + "@" + host);
      command.add("-p");
      command.add("22");
      command.add("cd");
      command.add(WORKING_DIR);
      command.add(";");
      command.add("nohup");
      command.add("java");
      command.add("-classpath");
      command.add("build/");
      command.add("bn.main.Main");
      command.add(START_NODE_COMMAND);
      if (port != 0) {
        command.add(String.valueOf(port));
      }
      command.add(">>");
      command.add("/tmp/" + port + "_out.log");
      command.add("2>>");
      command.add("/tmp/" + port + "_err.log");
      command.add("<");
      command.add("/dev/null");
      command.add("&");
      command.add("echo");
      command.add("$!");
      System.out.println(command);
      ProcessBuilder builder = new ProcessBuilder(command);
      serverProcess = builder.start();
      BufferedReader inputStream = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()));
      String inputLine;
      while ((inputLine = inputStream.readLine()) != null) {
        try {
          if (!remoteNodesPIDs.containsKey(host)) {
            remoteNodesPIDs.put(host, new ArrayList<String>());
          }
          // parseInt to validate pid
          remoteNodesPIDs.get(host).add(String.valueOf(Integer.parseInt(inputLine)));
          break;
        }
        catch (Exception e) {
          System.out.println(inputLine);
          //e.printStackTrace();
        }
      }
      inputStream.close();
    }
    return remoteNodesPIDs;
  }

  public static void killNodes(HashMap<String, List<String>> remoteNodesPIDs) throws Exception {
    for (String key : remoteNodesPIDs.keySet()) {
      List<String> command = new ArrayList<String>();
      // NOTE you need to have ssh on port 22 (forced because testing on virtual machine)
      command.add("ssh");
      command.add(REMOTE_USERNAME + "@" + key);
      command.add("-p");
      command.add("22");
      command.add("kill");
      command.add("-9");
      command.addAll(remoteNodesPIDs.get(key));
      System.out.println(command);
      (new ProcessBuilder(command)).start();
    }
  }


  public static byte[] buildBinomialGraphNetwork(ArrayList<String> nodes, int limit) {
    byte[] plan = new byte[0];
    // TODO
    return plan;
  }

  public static void startSimulation(ArrayList<String> nodes, byte[] plan) throws Exception {
    Thread.sleep(10000);
  }

  public static void main(String args[]) throws Exception {
    String command = args[0];
    if (command.equals(BOOT_COMMAND)) {
      ArrayList<String> nodes = getNodes(args[1]);
      HashMap<String, List<String>> remoteNodesPIDs = bootNodes(nodes);
      int nMax = Integer.parseInt(args[2]);
      byte[] networkPlan = buildBinomialGraphNetwork(nodes, nMax);
      startSimulation(nodes, networkPlan);
      killNodes(remoteNodesPIDs);
    }
    else if (command.equals(START_NODE_COMMAND)) {
      System.out.println("I am starting!");
      Thread.sleep(5000);
      System.out.println("I am ending!");
    }
    else {
      throw new Exception("UNKNOWN START COMMAND!");
    }
  }
}

package bn;

import java.io.*;
import java.util.*;

public class Controller {
  private static Controller self;
  private int nodesCount;
  private ArrayList<String> listeners;
  private HashSet<String> ready;
  private HashSet<String> starting;
  private HashSet<String> started;
  private boolean boot;


  private Controller () {
    nodesCount = 0;
    listeners = new ArrayList<String>();
    ready = new HashSet<String>();
    starting = new HashSet<String>();
    started = new HashSet<String>();
    boot = false;
  }

  public static Controller init() {
    if (self == null) {
      self = new Controller();
    }
    return self;
  }

  public void announceNode(String id) {
    listeners.add(id);
  }

  public int getNodesCount() {
    return nodesCount;
  }

  public int getAnnouncedNodesCount() {
    return listeners.size();
  }

  public void resetNodesCount() {
    nodesCount = 0;
  }

  public ArrayList<String> getAnnouncedNodes() {
    return listeners;
  }

  public void delAnnouncedNode(String id) {

  }
}

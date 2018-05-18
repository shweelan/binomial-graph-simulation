package bn;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.LongAdder;

public class Route {
  private static Random random = new Random();
  private int source;
  private int destination;
  private int routeLength;
  private LongAdder usage;
  private ArrayList<Integer> viaNodes = new ArrayList<Integer>();

  public Route(int src, int dest, int length, int viaNode) {
    // shortest found Route
    source = src;
    destination = dest;
    routeLength = length;
    usage = new LongAdder();
    viaNodes.add(viaNode);
  }

  public void used() {
    usage.increment();
  }

  public long getUsage() {
    return usage.longValue();
  }

  public long getUsageAndReset() {
    return usage.sumThenReset();
  }

  public int getRouteLength() {
    return routeLength;
  }

  public void addViaNode(int viaNode) {
    // Same-length route found
    if (viaNodes.indexOf(viaNode) < 0) {
      viaNodes.add(viaNode);
    }
  }

  public Integer getRandomViaNode() {
    int i = random.nextInt(viaNodes.size());
    return viaNodes.get(i);
  }

  public String toString() {
    return ("Route< Node#" + source + " *--|" + routeLength + "|--> Node#" + destination + " via Nodes " + viaNodes.toString() + " >");
  }
}

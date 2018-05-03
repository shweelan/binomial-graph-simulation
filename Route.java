package bn;

import java.util.ArrayList;
import java.util.Random;

public class Route {
  private static Random random = new Random();
  private int source;
  private int destination;
  private int routeLength;
  private ArrayList<Integer> viaNodes = new ArrayList<Integer>();

  public Route(int src, int dest, int length, int viaNode) {
    // shortest found Route
    source = src;
    destination = dest;
    routeLength = length;
    viaNodes.add(viaNode);
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

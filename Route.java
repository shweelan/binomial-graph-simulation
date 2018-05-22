package bn;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.LongAdder;
import java.util.Comparator;

public class Route {
  private static Random random = new Random();
  private int source;
  private int destination;
  private int routeLength;
  private LongAdder usage;
  private ArrayList<Integer> viaNodes = new ArrayList<Integer>();

  private static Comparator<Route> descendingLengthComparator = new Comparator<Route>() {
    @Override
    public int compare(Route rt1, Route rt2) {
      return rt2.getRouteLength() - rt1.getRouteLength();
    }
  };

  private static Comparator<Route> descendingReductionComparator = new Comparator<Route>() {
    @Override
    public int compare(Route rt1, Route rt2) {
      return rt2.getRouteReductionRate() - rt1.getRouteReductionRate();
    }
  };

  public Route(int src, int dest, int length, int viaNode) {
    // shortest found Route
    source = src;
    destination = dest;
    routeLength = length;
    usage = new LongAdder();
    viaNodes.add(viaNode);
  }

  public int getDestination() {
    return destination;
  }

  public static Comparator<Route> getDescendingLengthComparator() {
    return descendingLengthComparator;
  }

  public static Comparator<Route> getDescendingReductionComparator() {
    return descendingReductionComparator;
  }

  public int getRouteReductionRate() {
    return (routeLength - 1) * usage.intValue();
  }

  public void used() {
    usage.increment();
  }

  public void resetUsage() {
    usage.reset();
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
    return ("Route#" + destination + " < Node#" + source + " *--|" + routeLength + "|--> Node#" + destination + " via Nodes " + viaNodes.toString() + " >");
  }
}

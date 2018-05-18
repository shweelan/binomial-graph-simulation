package bn;

import java.util.ArrayList;

@FunctionalInterface
public interface StatsUpdater {
  void updateStats(long received, long forwaded, ArrayList<Long> lats);
}

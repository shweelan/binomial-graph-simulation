package bn;

import java.util.ArrayList;

@FunctionalInterface
public interface StatsUpdater {
  void updateStats(ArrayList<Long> lats);
}

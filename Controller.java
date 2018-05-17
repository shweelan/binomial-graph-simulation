package bn;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class Controller {
  private static Controller self = null;
  private static final String REDIS_URL = "http://127.0.0.1:7379";
  private static final String SEPARATOR = "-_-";
  private static final String LISTENERS_ID = "LISTENERS";
  private static final String NODES_COUNT_ID = "NUM_TOTAL_NODES";
  private static final String READY_COUNT_ID = "NUM_READY_NODES";
  private static final String RESULTS_ID = "RESULTS";
  private static Long timestampDiff = null;

  private static synchronized void init() {
    if (self == null) {
      self = new Controller();
    }
  }

  public static Controller getInstance() throws Exception {
    init();
    if (timestampDiff == null) {
      // synchronize timestamp with redis
      String[] command = {
        "TIME"
      };
      long ts = System.currentTimeMillis();
      String[] time = self.redisAPI(command).split(SEPARATOR);
      long rtt = System.currentTimeMillis() - ts;
      ts += rtt / 2;
      long remoteTs = (new Long(time[0]) * 1000 + new Long(time[1]) / 1000);
      timestampDiff = remoteTs - ts;
    }
    return self;
  }

  private String redisAPI(String[] command) throws Exception {
    StringBuffer urlBuilder = new StringBuffer();
    urlBuilder.append(REDIS_URL);
    for (String elem : command) {
      urlBuilder.append("/" + elem);
    }
    urlBuilder.append(".txt?sep=" + SEPARATOR);
    System.out.println("REDIS REQUEST : " + urlBuilder.toString());
    HttpURLConnection httpConnection = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
    httpConnection.setRequestMethod("GET");
    int responseCode = httpConnection.getResponseCode();
    if (responseCode == 404) {
      return "";
    }
    if (responseCode / 100 != 2) {
      throw new Exception("ERROR! Redis API failed! response code: " + responseCode);
    }
    BufferedReader inputStream = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));

    String inputLine;
    StringBuffer responseBuilder = new StringBuffer();
		while ((inputLine = inputStream.readLine()) != null) {
      responseBuilder.append(inputLine);
		}
		inputStream.close();
    System.out.println("REDIS RESPONSE : " + responseBuilder.toString());
		return responseBuilder.toString();
  }

  public void announceNode(String id) throws Exception {
    String[] command = {
      "RPUSH",
      LISTENERS_ID,
      id
    };
    redisAPI(command);
  }

  public int getNodesCount() throws Exception {
    String[] command = {
      "GET",
      NODES_COUNT_ID
    };
    String nodesCountStr = redisAPI(command);
    if (nodesCountStr.equals("")) {
      return 0;
    }
    return Integer.parseInt(nodesCountStr);
  }

  public int getAnnouncedNodesCount() throws Exception {
    String[] command = {
      "LLEN",
      LISTENERS_ID
    };
    String nodesCountStr = redisAPI(command);
    if (nodesCountStr.equals("")) {
      return 0;
    }
    return Integer.parseInt(nodesCountStr);
  }

  public ArrayList<String> getAnnouncedNodes() throws Exception {
    String[] command = {
      "LRANGE",
      LISTENERS_ID,
      "0",
      "-1"
    };
    String[] listeners = redisAPI(command).split(SEPARATOR);
    return new ArrayList<String>(Arrays.asList(listeners));
  }

  public void delAnnouncedNode(String id) throws Exception {
    String[] command = {
      "LREM",
      LISTENERS_ID,
      "0",
      id
    };
    redisAPI(command);
  }

  public void incReadyCount() throws Exception {
    String[] command = {
      "INCR",
      READY_COUNT_ID
    };
    redisAPI(command);
  }

  public int getReadyCount() throws Exception {
    String[] command = {
      "GET",
      READY_COUNT_ID
    };
    String readyCountStr = redisAPI(command);
    if (readyCountStr.equals("")) {
      return 0;
    }
    return Integer.parseInt(readyCountStr);
  }

  public void recordResults(String id, String result) throws Exception {
    String[] command = {
      "HSET",
      RESULTS_ID,
      id,
      result
    };
    redisAPI(command);
  }

  public long getTimestamp() {
    return System.currentTimeMillis() + timestampDiff;
  }
}

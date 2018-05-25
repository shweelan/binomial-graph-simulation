package bn;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

public class Controller {
  private static Controller self = null;
  private static String REDIS_URL = "http://127.0.0.1:7379";
  private static final String SEPARATOR = "-_-";
  private static final String LISTENERS_KEY = "LISTENERS";
  private static final String NODES_COUNT_KEY = "NUM_TOTAL_NODES";
  private static final String READY_COUNT_KEY = "NUM_READY_NODES";
  private static final String CONFIG_KEY = "CONFIG";
  private static final String RESULTS_KEY = "RESULTS_DATA";
  private static final String RESULTS_HEADER_KEY = "RESULTS_HEADER";
  private static final String TEST_ID_KEY = "TEST_NAME";
  private static Long timestampDiff = null;

  private static synchronized void init() {
    if (self == null) {
      self = new Controller();
    }
  }

  public static Controller getInstance() throws Exception {
    init();
    timestampDiff = new Long(0);
    if (timestampDiff == null) {
      // synchronize timestamp with redis
      String[] command = {
        "TIME"
      };
      long ts = System.currentTimeMillis();
      String[] time = self.redisAPI(command).split(SEPARATOR);
      ts = System.currentTimeMillis();
      long remoteTs = (new Long(time[0]) * 1000 + new Long(time[1]) / 1000);
      timestampDiff = remoteTs - ts;
      System.out.println("REDIS TIME: " + remoteTs);
      System.out.println("REDIS TIME DIFF: " + timestampDiff);
    }
    return self;
  }

  public static void setUrl(String url) {
    REDIS_URL = url;
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
      LISTENERS_KEY,
      id
    };
    redisAPI(command);
  }

  public int getNodesCount() throws Exception {
    String[] command = {
      "GET",
      NODES_COUNT_KEY
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
      LISTENERS_KEY
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
      LISTENERS_KEY,
      "0",
      "-1"
    };
    String[] listeners = redisAPI(command).split(SEPARATOR);
    return new ArrayList<String>(Arrays.asList(listeners));
  }

  public void delAnnouncedNode(String id) throws Exception {
    String[] command = {
      "LREM",
      LISTENERS_KEY,
      "0",
      id
    };
    redisAPI(command);
  }

  public void incReadyCount() throws Exception {
    String[] command = {
      "INCR",
      READY_COUNT_KEY
    };
    redisAPI(command);
  }

  public int getReadyCount() throws Exception {
    String[] command = {
      "GET",
      READY_COUNT_KEY
    };
    String readyCountStr = redisAPI(command);
    if (readyCountStr.equals("")) {
      return 0;
    }
    return Integer.parseInt(readyCountStr);
  }

  public void recordResults(String testId, String[] header, String key, String result) throws Exception {
    String[] command = {
      "HSET",
      RESULTS_KEY + "_" + testId,
      key,
      result
    };
    redisAPI(command);
    String[] headerCommand = {
      "SETNX",
      RESULTS_HEADER_KEY + "_" + testId,
      String.join(",", header)
    };
    redisAPI(headerCommand);
  }

  public HashMap<String, String> getConfig() throws Exception {
    String[] command = {
      "HGETALL",
      CONFIG_KEY
    };
    HashMap<String, String> config = new HashMap<String, String>();
    String response = redisAPI(command);
    if (!response.equals("")) {
      String[] elems = response.split(SEPARATOR);
      int i = 0;
      while (i < elems.length) {
        String key = elems[i];
        String value = elems[i + 1];
        config.put(key, value);
        i += 2;
      }
    }
    return config;
  }

  public String getTestId() throws Exception {
    String[] command = {
      "GET",
      TEST_ID_KEY
    };
    return redisAPI(command);
  }

  public long getTimestamp() {
    return System.currentTimeMillis() + timestampDiff;
  }

  public long getTimeDiff() {
    return timestampDiff;
  }

  public String[] getResultsKeys(String pattern) throws Exception {
    String[] command = {
      "KEYS",
      RESULTS_KEY + "_" + pattern
    };
    String ret = redisAPI(command);
    if (ret.equals("")) return new String[0];
    String[] keys = ret.split(SEPARATOR);
    for (int i = 0; i < keys.length; i++) {
      keys[i] = keys[i].replace(RESULTS_KEY + "_", "");
    }
    return keys;
  }

  public String getResultsHeader(String key) throws Exception {
    String[] command = {
      "GET",
      RESULTS_HEADER_KEY + "_" + key
    };
    return redisAPI(command);
  }

  public String[] getResults(String key) throws Exception {
    String[] command = {
      "HGETALL",
      RESULTS_KEY + "_" + key
    };
    String ret = redisAPI(command);
    if (ret.equals("")) return new String[0];
    String[] mapped = ret.split(SEPARATOR);
    System.out.println();
    System.out.println();
    String[] results = new String[mapped.length / 2];
    for (int i = 0; i < results.length; i++) {
      results[i] = mapped[2 * i + 1];
    }
    return results;
  }
}

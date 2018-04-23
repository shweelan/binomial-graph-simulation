package bn;

import java.net.*;
import java.io.*;
import java.util.*;

public class Controller {
  private static Controller self;
  private static final String REDIS_URL = "http://127.0.0.1:7379";
  private static final String SEPARATOR = "-_-";
  private static final String LISTENERS_ID = "LISTENERS";
  private static final String NODES_COUNT_ID = "NUM_NODES";

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

  public static Controller init() {
    if (self == null) {
      self = new Controller();
    }
    return self;
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

  public void resetNodesCount() throws Exception {
    String[] command = {
      "DEL",
      NODES_COUNT_ID
    };
    redisAPI(command);
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
      "1",
      id
    };
    redisAPI(command);
  }
}

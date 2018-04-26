package bn;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Random;
import java.lang.System;

public class Helper {
  // TODO remove
  private static Random random = new Random();
  private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
  public static void bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 3];
    for ( int j = 0; j < bytes.length; j++ ) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 3] = hexArray[v >>> 4];
      hexChars[j * 3 + 1] = hexArray[v & 0x0F];
      hexChars[j * 3 + 2] = ' ';
    }
    System.out.println(new String(hexChars));
  }

  public static byte[] serialize(ArrayList<Integer> data) {
    ByteBuffer bb = ByteBuffer.allocate(data.size() * 4).order(ByteOrder.BIG_ENDIAN);
    for (int d : data) {
      bb.putInt(d);
    }
    return bb.array();
  }

  public static byte[] serialize(int data) {
    ArrayList<Integer> arr = new ArrayList<Integer>();
    arr.add(data);
    return serialize(arr);
  }

  public static byte[] buildMessage(byte[] src, byte[] route, int chunkSize) {
    // [total message size, src, route size, route, chunk]
    int totalLength = 4 + src.length + 4 + route.length + chunkSize;
    System.out.println(totalLength + " " + route.length);
    byte[] msg = new byte[totalLength];
    byte[] serializedTotalLength = serialize(totalLength);
    byte[] serializedRouteSize = serialize(route.length);
    byte[] serializedChunk = new byte[chunkSize];
    random.nextBytes(serializedChunk);
    byte[][] parts = {
      serializedTotalLength,
      src,
      serializedRouteSize,
      route,
      serializedChunk
    };
    int bytesWritten = 0;
    for (byte[] part : parts) {
      System.arraycopy(part, 0, msg, bytesWritten, part.length);
      bytesWritten += part.length;
    }
    return msg;
  }
}

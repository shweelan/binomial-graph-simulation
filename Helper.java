package bn;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Random;
import java.lang.System;

public class Helper {
  private static Random random = new Random();
  // TODO remove
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

  // TODO build message class to serialize and deserialize
  public static byte[] buildMessage(int src, int dest, int chunkSize) {
    // [total message size, src, dest, data]
    int totalLength = 12 + chunkSize;
    byte[] msg = new byte[totalLength];
    byte[] serializedTotalLength = serialize(totalLength);
    byte[] serializedSrc = serialize(src);
    byte[] serializedDest = serialize(dest);
    byte[] serializedChunk = new byte[chunkSize];
    random.nextBytes(serializedChunk);
    byte[][] parts = {
      serializedTotalLength,
      serializedSrc,
      serializedDest,
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

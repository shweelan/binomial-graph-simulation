package bn;

import java.util.Random;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.lang.System;

public class Message {
  private static Random random = new Random();
  private int source;
  private int destination;
  private byte[] data;
  private long timestamp;

  public Message(int src, int dest, long ts, int size) {
    data = new byte[size];
    random.nextBytes(data);
    source = src;
    destination = dest;
    timestamp = ts;
  }

  public void setTimestamp(long ts) {
    timestamp = ts;
  }

  public byte[] serialize() {
    // [totalLength(int), source(int), destination(int), timestamp(long)]
    int headerLength = 3 * Integer.BYTES + Long.BYTES;
    int totalLength = headerLength + data.length;
    ByteBuffer bb = ByteBuffer.allocate(headerLength).order(ByteOrder.BIG_ENDIAN);
    bb.putInt(totalLength);
    bb.putInt(source);
    bb.putInt(destination);
    bb.putLong(timestamp);
    byte[] header = bb.array();
    byte[] msg = new byte[totalLength];
    System.arraycopy(header, 0, msg, 0, header.length);
    System.arraycopy(data, 0, msg, header.length, data.length);
    return msg;
  }

  // TODO remove
  private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
  public static void printInHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 3];
    for ( int j = 0; j < bytes.length; j++ ) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 3] = hexArray[v >>> 4];
      hexChars[j * 3 + 1] = hexArray[v & 0x0F];
      hexChars[j * 3 + 2] = ' ';
    }
    System.out.println(new String(hexChars));
  }

}

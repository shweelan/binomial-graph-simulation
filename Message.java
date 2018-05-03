package bn;

import java.util.Random;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.lang.System;
import java.io.InputStream;

public class Message {
  private static Random random = new Random();
  private static final int HEADER_LENGTH = Long.BYTES + 3 * Integer.BYTES;
  private long timestamp;
  private int source;
  private int destination;
  private byte[] data;

  public Message(int src, int dest, long ts, int size) {
    data = new byte[size];
    random.nextBytes(data);
    source = src;
    destination = dest;
    timestamp = ts;
  }

  public Message(InputStream inputStream) throws Exception {
    byte[] header = new byte[HEADER_LENGTH];
    int read = inputStream.read(header, 0, header.length);
    // TODO check read == HEADER_LENGTH
    ByteBuffer bb = ByteBuffer.allocate(header.length).order(ByteOrder.BIG_ENDIAN);
    bb.put(header);
    bb.rewind();
    timestamp = bb.getLong();
    source = bb.getInt();
    destination = bb.getInt();
    int dataSize = bb.getInt();
    // TODO if dataSize == -1 then its dataEnd
    data = new byte[dataSize];
    read = inputStream.read(data, 0, data.length);
    // TODO check read == dataSize
  }

  public void setTimestamp(long ts) {
    timestamp = ts;
  }

  public byte[] serialize() {
    // [source(int), destination(int), timestamp(long), dataSize(int), data(bytes)]
    int totalLength = HEADER_LENGTH + data.length;
    ByteBuffer bb = ByteBuffer.allocate(totalLength).order(ByteOrder.BIG_ENDIAN);
    bb.putLong(timestamp);
    bb.putInt(source);
    bb.putInt(destination);
    bb.putInt(data.length);
    bb.put(data);
    return bb.array();
  }

  public boolean isDataEnd() {
    return false;
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

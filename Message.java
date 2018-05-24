package bn;

import java.util.Random;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.lang.System;
import java.io.InputStream;
import java.io.EOFException;

public class Message {
  private static Random random = new Random();
  private static final int HEADER_LENGTH = Long.BYTES + 5 * Integer.BYTES;
  public static enum Type {
    DATA,
    BYE // to disconnect
  };
  private Type type;
  private long timestamp;
  private int source;
  private int destination;
  private int numHops;
  private byte[] data;

  public Message(int src, int dest, long ts, int size) {
    type = Type.DATA;
    data = new byte[size];
    random.nextBytes(data);
    source = src;
    destination = dest;
    numHops = 0;
    timestamp = ts;
  }

  public Message() {
    // Empty message means BYE
    type = Type.BYE;
    data = new byte[0];
  }

  // TODO add error messages

  public Message(InputStream inputStream) throws Exception {
    byte[] header = new byte[HEADER_LENGTH];
    int read = inputStream.read(header, 0, header.length);
    if (read < 0) {
      throw new EOFException();
    }
    else if (read != HEADER_LENGTH) {
      throw new Exception("ERROR! Bad message header");
    }
    ByteBuffer bb = ByteBuffer.allocate(header.length).order(ByteOrder.BIG_ENDIAN);
    bb.clear();
    bb.put(header);
    bb.flip(); // rewind
    timestamp = bb.getLong();
    type = Type.values()[bb.getInt()];
    source = bb.getInt();
    destination = bb.getInt();
    numHops = bb.getInt();
    int dataSize = bb.getInt();
    data = new byte[dataSize];
    read = inputStream.read(data, 0, data.length);
    if (read != data.length) {
      throw new Exception("ERROR! Bad message, Expected: " + data.length + " Got: " + read);
    }
  }

  public void setTimestamp(long ts) {
    timestamp = ts;
  }

  public void incNumHops() {
    numHops++;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public int getNumHops() {
    return numHops;
  }

  public int getDestination() {
    return destination;
  }

  public int getSource() {
    return source;
  }

  public byte[] serialize() {
    // [source(int), destination(int), timestamp(long), dataSize(int), data(bytes)]
    int totalLength = HEADER_LENGTH + data.length;
    ByteBuffer bb = ByteBuffer.allocate(totalLength).order(ByteOrder.BIG_ENDIAN);
    bb.putLong(timestamp);
    bb.putInt(type.ordinal());
    bb.putInt(source);
    bb.putInt(destination);
    bb.putInt(numHops);
    bb.putInt(data.length);
    if (data.length > 0) {
      bb.put(data);
    }
    return bb.array();
  }

  public boolean isGoodBye() {
    return type == Type.BYE;
  }

  public String toString() {
    String str = "{type: " + type + ", timestamp: " + timestamp;
    if (type != Type.BYE) {
      str += ", source: " + source + ", destination: " + destination + ", num-hops: " + numHops + ", data-size: " + data.length;
    }
    str += "}";
    return str;
  }
}

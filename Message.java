package bn;

import java.util.Random;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.lang.System;
import java.io.InputStream;
import java.io.EOFException;
import java.lang.IllegalArgumentException;

public class Message {
  private static final int DATA_READ_RETRIES = 5;
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

  private byte[] read(InputStream inputStream, int len) throws Exception {
    if (len < 0) throw new IllegalArgumentException();
    byte[] data = new byte[len];
    int read = 0;
    int retries = 0;
    while (read < len) {
      int _read = inputStream.read(data, read, len - read);
      if (_read < 0) {
        if (++retries == DATA_READ_RETRIES) throw new EOFException("ERROR! Bad message, Expected:" + len + " Got:" + read);
      }
      else {
        read += _read;
        retries = 0;
      }
    }
    return data;
  }

  public Message(InputStream inputStream) throws Exception {
    byte[] header = read(inputStream, HEADER_LENGTH);
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
    data = read(inputStream, dataSize);
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

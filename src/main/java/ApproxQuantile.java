import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Nikos R. Katsipoulakis (nick.katsip@gmail.com)
 */
public class ApproxQuantile {
  
  private final int b;
  
  private final int k;
  
  public ApproxQuantile(final int b, final int k) {
    if (b <= 3)
      throw new IllegalArgumentException("number of buffers need to be at least 3");
    if (k <= 3)
      throw new IllegalArgumentException("at least three elements per buffer");
    this.b = b;
    this.k = k;
  }
  
  static class Buffer {
    
    static enum State {
      EMPTY,
      FULL
    }
    
    State state;
  
    final int k;
    
    List<Integer> data;
    
    int weight;
    
    Buffer(final int k) {
      this.k = k;
      this.state = State.EMPTY;
      this.data = new ArrayList<>(k);
      this.weight = 0;
    }
    
    public int augment() {
      while (this.data.size() < k) {
        this.data.add(Integer.MIN_VALUE);
        this.data.add(Integer.MAX_VALUE);
      }
      Collections.sort(this.data);
      return data.size();
    }
    
    public boolean ingest(int element) {
      this.data.add(element);
      if (this.data.size() >= k)
        return true;
      return false;
    }
    
    public void setState(State state) {
      this.state = state;
    }
    
    public void init() {
      this.data.clear();
      this.state = State.EMPTY;
      this.weight = 0;
    }
    
  }
  
  public int getPercentile(final float phi, int[] data) {
    int index = 0;
    List<Buffer> buffers = new ArrayList<>(this.b);
    int totalSize = 0;
    while (index < data.length) {
      if (buffers.size() < this.b) {
        Buffer newBuffer = new Buffer(this.k);
        while (index < data.length && !newBuffer.ingest(data[index]))
          index++;
        totalSize += newBuffer.augment();
        newBuffer.setState(Buffer.State.FULL);
        buffers.add(newBuffer);
      } else {
        /**
         * Look for an empty buffer
         */
        int emptyBufferIndex = -1;
        for (int i = 0; i < buffers.size(); ++i) {
          if (buffers.get(i).state == Buffer.State.EMPTY) {
            emptyBufferIndex = i;
            break;
          }
        }
        if (emptyBufferIndex >= 0) {
          while (index < data.length && !buffers.get(emptyBufferIndex).ingest(data[index]))
            index++;
        } else {
          /**
           * Need to collapse and create new buffer
           */
        }
      }
    }
  }
  
  public Buffer collapse(Collection<Buffer> buffers) {
    List<Integer> tmp = new ArrayList<>();
    Buffer outputBuffer = new Buffer(k);
    outputBuffer.weight = 0;
    for (Buffer b : buffers) {
      outputBuffer.weight += b.weight;
      for (Integer d : b.data) {
        for (int i = 0; i < b.weight; ++i)
          tmp.add(d);
      }
    }
    Collections.sort(tmp);
    if (outputBuffer.weight % 2 != 0) {
      int constant = (outputBuffer.weight + 1) / 2;
      for (int j = 0; j < k; ++j)
        outputBuffer.data.add(tmp.get(j*outputBuffer.weight + constant));
    } else {
      int tik = outputBuffer.weight / 2;
      for (int j = 0; j < k; ++j) {
        if (j % 2 == 0)
          outputBuffer.data.add(tmp.get(j*outputBuffer.weight + tik));
        else
          outputBuffer.data.add(tmp.get(j*outputBuffer.weight + tik + 1));
      }
    }
    return outputBuffer;
  }
  
  public int output(final float phiPrime, Collection<Buffer> finalBuffers) {
    int totalWeight = 0;
    List<Integer> tmp = new ArrayList<>();
    for (Buffer buffer : finalBuffers) {
      totalWeight += buffer.weight;
      for (Integer d : buffer.data)
        tmp.add(d);
    }
    Collections.sort(tmp);
    int finalIndex = (int) Math.ceil(phiPrime * this.k * totalWeight);
    return tmp.get(finalIndex);
  }
}

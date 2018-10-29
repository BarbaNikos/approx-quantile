import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Nikos R. Katsipoulakis (nick.katsip@gmail.com)
 */
public class ApproxQuantile {
  
  private final int b;
  
  private final int k;
  
  private final float phi;
  
  private Buffer[] buffers;
  
  private int count;
  
  private int streamLength;
  
  public ApproxQuantile(final int b, final int k, final float phi) {
    if (b <= 3)
      throw new IllegalArgumentException("number of buffers need to be at least 3");
    if (k <= 3)
      throw new IllegalArgumentException("at least three elements per buffer");
    if (phi < 0.0f || phi > 1.f)
      throw new IllegalArgumentException("invalid value for phi");
    this.b = b;
    this.k = k;
    this.phi = phi;
    this.buffers = new Buffer[this.b];
    for (int i = 0; i < buffers.length; ++i)
      this.buffers[i] = new Buffer(this.k);
    this.count = 0;
    this.streamLength = 0;
  }
  
  /**
   * This receives chunks of up to k elements
   * @param data an array of up to k elements
   */
  public void load(int[] data) {
    int emptyIndex = -1;
    int emptyCount = 0;
    int minLevel = Integer.MAX_VALUE;
    for (int i = 0; i < buffers.length; ++i) {
      if (buffers[i].getState() == Buffer.State.EMPTY) {
        emptyIndex = i;
        emptyCount++;
        minLevel = minLevel > buffers[i].getLevel() ? buffers[i].getLevel() : minLevel;
      }
    }
    if (emptyCount >= 2) {
      /**
       * invoke new on the empty buffer and assign its level as 0
       */
      buffers[emptyIndex].init();
      int actualElementsAdded = buffers[emptyIndex].newOperation(data);
      buffers[emptyIndex].setLevel(0);
      count += actualElementsAdded;
    } else if (emptyCount == 1) {
      /**
       * invoke new on the empty buffer and assign its level as minLevel
       */
      buffers[emptyIndex].init();
      int elementsAdded = buffers[emptyIndex].newOperation(data);
      buffers[emptyIndex].setLevel(minLevel);
      count += elementsAdded;
    } else {
      /**
       * Collapse all the buffers with level = minLevel and assign the output buffer level
       * minLevel + 1
       */
      List<Integer> bufferIndices = new ArrayList<>();
      for (int i = 0; i < buffers.length; ++i) {
        if (buffers[i].getLevel() == minLevel)
          bufferIndices.add(i);
      }
      collapse(bufferIndices, minLevel);
      /**
       * The 2nd element in the bufferIndices should be an empty buffer
       */
      if (buffers[bufferIndices.get(1)].getState() != Buffer.State.EMPTY)
        throw new IllegalStateException("non empty buffer");
      emptyIndex = bufferIndices.get(1);
      buffers[emptyIndex].init();
      int elementsAdded = buffers[emptyIndex].newOperation(data);
      // TODO: Figure out if the following is correct
      buffers[emptyIndex].setLevel(minLevel + 1);
      count += elementsAdded;
    }
    streamLength += data.length;
  }
  
  public void collapse(List<Integer> bufferIndices, int minLevel) {
    List<Integer> tmp = new ArrayList<>();
    Buffer outputBuffer = new Buffer(k);
    int outputBufferWeight = 0;
    for (int index : bufferIndices) {
      outputBufferWeight += this.buffers[index].getWeight();
      for (Integer d : this.buffers[index].data) {
        for (int i = 0; i < this.buffers[index].getWeight(); ++i)
          tmp.add(d);
      }
      this.buffers[index].init();
    }
    Collections.sort(tmp);
    int outputIndex = bufferIndices.get(0);
    this.buffers[outputIndex].init();
    this.buffers[outputIndex].setLevel(minLevel + 1);
    this.buffers[outputIndex].setState(Buffer.State.FULL);
    this.buffers[outputIndex].setWeight(outputBufferWeight);
    if (outputBufferWeight % 2 != 0) {
      int constant = (outputBufferWeight + 1) / 2;
      for (int j = 0; j < k; ++j)
        outputBuffer.data.add(tmp.get(j*outputBufferWeight + constant));
    } else {
      int tik = outputBufferWeight / 2;
      for (int j = 0; j < k; ++j) {
        if (j % 2 == 0)
          outputBuffer.data.add(tmp.get(j*outputBufferWeight + tik));
        else
          outputBuffer.data.add(tmp.get(j*outputBufferWeight + tik + 1));
      }
    }
  }
  
  public int output(final float phi) {
    final float beta = ((float) count) / ((float) streamLength);
    final float phiPrime = (2.0f * phi + beta - 1.f) / (2.0f * beta);
    int totalWeight = 0;
    List<Integer> tmp = new ArrayList<>();
    for (int i = 0; i < buffers.length; ++i) {
      if (buffers[i].getState() == Buffer.State.FULL) {
        totalWeight += buffers[i].getWeight();
        for (Integer d : this.buffers[i].data) {
          for (int j = 0; j < this.buffers[i].getWeight(); ++j)
            tmp.add(d);
        }
      }
    }
    Collections.sort(tmp);
    int finalIndex = (int) Math.ceil(phiPrime * this.k * totalWeight);
    return tmp.get(finalIndex);
  }
  
}

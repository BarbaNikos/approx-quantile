import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Nikos R. Katsipoulakis (nick.katsip@gmail.com)
 */
public class Main {
  
  private static int[] randomData(final int N) {
    int[] data = new int[N];
    for (int i = 0; i < N; ++i)
      data[i] = ThreadLocalRandom.current().nextInt();
    return data;
  }
  
  static int lowerBound(float phi, float epsilon, int N) {
    return (int) Math.ceil((phi - epsilon) * (float) N);
  }
  
  static int upperBound(float phi, float epsilon, int N) {
    return (int) Math.ceil((phi + epsilon) * (float) N);
  }
  
  public static void main(String[] args) {
    int N = (int) 1e+7;
    float epsilon = 0.001f;
    float phi = 0.99f;
    int[] data = randomData(N);
    ApproxQuantile approxQuantile = new ApproxQuantile(epsilon, N);
    approxQuantile.load(data);
    Arrays.sort(data);
    int median = data[(int) Math.ceil(phi * (float) N)];
    int approxMedian = approxQuantile.output(phi);
    boolean success = false;
    for (int i = lowerBound(phi, epsilon, N); i <= upperBound(phi, epsilon, N); ++i) {
      if (data[i] == approxMedian) {
        success = true;
        break;
      }
    }
    System.out.println("median: " + median + ", approx-median: " + approxMedian + ", success: " +
        success);
  }
}

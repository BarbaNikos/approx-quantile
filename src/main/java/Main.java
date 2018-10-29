import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
  
  public static void main(String[] args) {
    int N = 10;
    float phi = 0.5f;
    float epsilon = 0.1f;
    int[] data = randomData(N);
  }
}

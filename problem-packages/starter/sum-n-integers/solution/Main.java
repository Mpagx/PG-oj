import java.io.BufferedInputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        FastScanner scanner = new FastScanner();
        int n = (int) scanner.nextLong();
        long sum = 0;
        for (int i = 0; i < n; i++) sum += scanner.nextLong();
        System.out.println(sum);
    }

    static class FastScanner {
        private final BufferedInputStream in = new BufferedInputStream(System.in);
        private final byte[] buffer = new byte[1 << 16];
        private int length, index;
        long nextLong() throws Exception {
            int c;
            do c = read(); while (c <= 32);
            int sign = 1;
            if (c == '-') { sign = -1; c = read(); }
            long value = 0;
            while (c > 32) { value = value * 10 + c - '0'; c = read(); }
            return value * sign;
        }
        int read() throws Exception {
            if (index >= length) { length = in.read(buffer); index = 0; }
            return length < 0 ? -1 : buffer[index++];
        }
    }
}

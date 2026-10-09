import java.io.BufferedInputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        FastScanner scanner = new FastScanner();
        int n = scanner.nextInt();
        int target = scanner.nextInt();
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = scanner.nextInt();
        int left = 0, right = n - 1, answer = -1;
        while (left <= right) {
            int middle = left + (right - left) / 2;
            if (values[middle] == target) { answer = middle; break; }
            if (values[middle] < target) left = middle + 1;
            else right = middle - 1;
        }
        System.out.println(answer);
    }

    static class FastScanner {
        private final BufferedInputStream in = new BufferedInputStream(System.in);
        private final byte[] buffer = new byte[1 << 16];
        private int length, index;
        int nextInt() throws Exception {
            int c;
            do c = read(); while (c <= 32);
            int sign = 1;
            if (c == '-') { sign = -1; c = read(); }
            int value = 0;
            while (c > 32) { value = value * 10 + c - '0'; c = read(); }
            return value * sign;
        }
        int read() throws Exception {
            if (index >= length) { length = in.read(buffer); index = 0; }
            return length < 0 ? -1 : buffer[index++];
        }
    }
}

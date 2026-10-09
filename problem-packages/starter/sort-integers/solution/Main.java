import java.util.Arrays;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = scanner.nextInt();
        Arrays.sort(values);
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) output.append(' ');
            output.append(values[i]);
        }
        System.out.println(output);
    }
}

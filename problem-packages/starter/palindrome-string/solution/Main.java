import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws Exception {
        String text = new BufferedReader(new InputStreamReader(System.in)).readLine();
        int left = 0, right = text.length() - 1;
        while (left < right && text.charAt(left) == text.charAt(right)) {
            left++;
            right--;
        }
        System.out.println(left >= right ? "YES" : "NO");
    }
}

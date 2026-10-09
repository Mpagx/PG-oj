export function codeTemplate(language: string): string {
  return language === "java"
    ? `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // 在这里读取标准输入、实现解题逻辑，并输出答案。

    }
}
`
    : "";
}

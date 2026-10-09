/** Fast checks only; Java parsing and final compilation remain server-side. */
export function javaSubmissionError(code: string): string | null {
  if (!code.trim()) return "请先填写代码";
  // Ignore comments and string/character literals so they cannot fake an entry point.
  const source = code.replace(
    /\/\/[^\r\n]*|\/\*[\s\S]*?\*\/|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'/g,
    " "
  );
  if (!/\bclass\s+Main\b/.test(source)) {
    return "请提交完整的 Java 程序，必须包含 public class Main，不能只提交数字或文字";
  }
  if (!/\bmain\s*\(/.test(source)) {
    return "Java 程序必须包含 public static void main(String[] args) 入口";
  }
  if (/\bpackage\s+[\w.]+\s*;/.test(source)) {
    return "提交代码不能声明 package，请删除包名后再提交";
  }
  return null;
}

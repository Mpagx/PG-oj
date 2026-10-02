/**
 * 根据代码内容粗略判断所属语言（仅用于前端提示，不参与判题）
 *
 * 注意：这里是启发式判断，只挑每种语言里非常明确的特征，
 * 判断不出来时返回 null，避免误报。真正判定代码对不对由后端判题机负责。
 *
 * @param code 用户输入的代码
 * @returns 识别出的语言值（java / cpp / go / html），无法判断时返回 null
 */
export function detectCodeLanguage(code: string): string | null {
  if (!code || !code.trim()) {
    return null;
  }
  const c = code.trim();

  // html：标签特征非常明显，优先判断
  if (
    /<\s*!DOCTYPE\s+html/i.test(c) ||
    /<\s*html[\s>]/i.test(c) ||
    /<\/\s*[a-z]+\s*>/i.test(c)
  ) {
    return "html";
  }

  // cpp：预处理指令 / 头文件 / 命名空间 / 标准输入输出
  if (
    /#include\b/.test(c) ||
    /\busing\s+namespace\s+std\b/.test(c) ||
    /std::/.test(c) ||
    /\bcout\s*<</.test(c) ||
    /\bcin\s*>>/.test(c)
  ) {
    return "cpp";
  }

  // java：包导入 / 类声明 / 标准输出 / main 签名
  if (
    /\bimport\s+java\./.test(c) ||
    /\bpublic\s+(static\s+)?(final\s+)?class\b/.test(c) ||
    /\bSystem\.out\b/.test(c) ||
    /\bpublic\s+static\s+void\s+main\s*\(/.test(c)
  ) {
    return "java";
  }

  // go：package main / func main / 短变量声明 / fmt 标准库
  if (
    /\bpackage\s+main\b/.test(c) ||
    /\bfunc\s+main\s*\(/.test(c) ||
    /:=/.test(c) ||
    /\bfmt\./.test(c)
  ) {
    return "go";
  }

  return null;
}

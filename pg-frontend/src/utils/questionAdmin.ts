import type { Question } from "@/generated";

export interface AdminCase {
  input: string;
  output: string;
}
export interface AdminConfig {
  timeLimit: number;
  memoryLimit: number;
  stackLimit: number;
}
export const DEFAULT_JUDGE_CONFIG: AdminConfig = {
  timeLimit: 1000,
  memoryLimit: 262144,
  stackLimit: 65536,
};

function parse(raw: unknown): unknown {
  if (typeof raw !== "string") return raw;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}
export function parseTags(raw: unknown): string[] {
  const tags = parse(raw);
  return Array.isArray(tags)
    ? tags.filter((tag): tag is string => typeof tag === "string")
    : [];
}
export function parseCases(raw: unknown): AdminCase[] | null {
  const cases = parse(raw);
  return Array.isArray(cases) &&
    cases.every(
      (item) =>
        item &&
        typeof item.input === "string" &&
        typeof item.output === "string"
    )
    ? cases
    : null;
}
export function parseConfig(raw: unknown): AdminConfig | null {
  const config = parse(raw) as AdminConfig | null;
  return config &&
    typeof config === "object" &&
    [config.timeLimit, config.memoryLimit, config.stackLimit].every(
      Number.isFinite
    )
    ? config
    : null;
}
export function judgeDataError(
  cases: AdminCase[] | null,
  config: AdminConfig | null
): string {
  if (!cases || cases.length < 1 || cases.length > 50)
    return "需要 1–50 个有效判题用例";
  if (
    !config ||
    !Number.isInteger(config.timeLimit) ||
    config.timeLimit < 100 ||
    config.timeLimit > 10000
  )
    return "时间限制需要为 100–10000 ms 的整数";
  if (
    !Number.isInteger(config.memoryLimit) ||
    config.memoryLimit < 16384 ||
    config.memoryLimit > 524288
  )
    return "内存限制需要为 16384–524288 KB 的整数";
  if (
    !Number.isInteger(config.stackLimit) ||
    config.stackLimit < 256 ||
    config.stackLimit > 65536
  )
    return "堆栈限制需要为 256–65536 KB 的整数";
  let total = 0;
  for (let i = 0; i < cases.length; i++) {
    const item = cases[i];
    if (
      !item ||
      typeof item.input !== "string" ||
      typeof item.output !== "string"
    )
      return `用例 ${i + 1} 的输入输出格式无效`;
    const bytes = new TextEncoder().encode(item.input).length;
    total += bytes;
    if (bytes > 16384) return `用例 ${i + 1} 的输入超过 16 KB`;
    if (new TextEncoder().encode(item.output).length > 1048576)
      return `用例 ${i + 1} 的期望输出超过 1 MB`;
  }
  return total > 262144 ? "所有用例的输入总量超过 256 KB" : "";
}
export function questionSummary(question: Question) {
  const cases = parseCases(question.judgeCase);
  const config = parseConfig(question.judgeConfig);
  const submitted = Math.max(0, question.submitNum ?? 0);
  const accepted = Math.max(0, question.acceptedNum ?? 0);
  return {
    tags: parseTags(question.tags),
    cases,
    config,
    caseCount: cases?.length ?? 0,
    issue:
      !question.title?.trim() || !question.content?.trim()
        ? "缺少题目标题或题面"
        : judgeDataError(cases, config),
    passRate: submitted
      ? `${Math.min(100, (accepted / submitted) * 100).toFixed(1)}%`
      : "—",
    submitted,
    accepted,
  };
}

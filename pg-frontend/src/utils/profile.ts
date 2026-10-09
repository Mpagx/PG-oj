import type { DailySubmissionStatVO, QuestionSubmitVO } from "@/generated";

export function submissionVerdict(submission: QuestionSubmitVO) {
  if (submission.status === 0) return { text: "等待判题", color: "gray" };
  if (submission.status === 1) return { text: "判题中", color: "arcoblue" };
  if (submission.status === 3) return { text: "系统异常", color: "orange" };
  const message = submission.judgeInfo?.message;
  const labels: Record<string, string> = {
    Accepted: "已通过",
    "Wrong Answer": "答案错误",
    编译错误: "编译错误",
    超时: "超出时间限制",
    内存溢出: "超出内存限制",
    运行错误: "运行错误",
    输出溢出: "输出超限",
    展示错误: "格式错误",
  };
  return {
    text: (message && labels[message]) || message || "已完成",
    color: message === "Accepted" ? "green" : "red",
  };
}

function localDay(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function buildActivity(
  stats: DailySubmissionStatVO[] = [],
  today = new Date()
) {
  const countByDay = new Map(
    stats.map((item) => [item.day, Number(item.submissionCount ?? 0)])
  );
  const start = new Date(
    today.getFullYear(),
    today.getMonth(),
    today.getDate()
  );
  start.setDate(start.getDate() - 83);
  return Array.from({ length: 84 }, (_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    const day = localDay(date);
    const count = Number(countByDay.get(day) ?? 0);
    return {
      day,
      count,
      level:
        count === 0 ? 0 : count === 1 ? 1 : count <= 3 ? 2 : count <= 6 ? 3 : 4,
    };
  });
}

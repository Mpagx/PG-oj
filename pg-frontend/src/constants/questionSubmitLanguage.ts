export const QUESTION_SUBMIT_LANGUAGES = [
  { label: "Java", value: "java" },
  { label: "C++", value: "cpp" },
  { label: "Go", value: "go" },
] as const;

export type QuestionSubmitLanguage =
  (typeof QUESTION_SUBMIT_LANGUAGES)[number]["value"];

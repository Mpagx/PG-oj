export const QUESTION_SUBMIT_LANGUAGES = [
  { label: "Java", value: "java" },
] as const;

export type QuestionSubmitLanguage =
  (typeof QUESTION_SUBMIT_LANGUAGES)[number]["value"];

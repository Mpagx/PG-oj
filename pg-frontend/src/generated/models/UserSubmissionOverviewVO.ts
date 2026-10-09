/* generated using openapi-typescript-codegen -- do not edit */
import type { DailySubmissionStatVO } from "./DailySubmissionStatVO";
export type UserSubmissionOverviewVO = {
  totalSubmissions?: number;
  acceptedSubmissions?: number;
  attemptedQuestions?: number;
  solvedQuestions?: number;
  activity?: Array<DailySubmissionStatVO>;
};

import axios from "axios";

export type ImportResult = {
  questionId: number;
  title: string;
  packageType: string;
  caseCount: number;
  generatedCaseCount: number;
  status: string;
  checks: string[];
};

type Response<T> = { code: number; data: T; message?: string };

export async function importProblemZip(file: File) {
  const body = new FormData();
  body.append("file", file);
  return (
    await axios.post<Response<ImportResult>>(
      "/api/admin/problem-import/zip",
      body
    )
  ).data;
}

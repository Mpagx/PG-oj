import axios from "axios";

export type QuestionListItem = {
  id: number;
  name: string;
  questionCount: number;
  containsQuestion: boolean;
  createTime?: string;
  updateTime?: string;
};

type ApiResponse<T> = { code: number; data: T; message?: string };

export async function getMyQuestionLists(questionId: number) {
  const response = await axios.get<ApiResponse<QuestionListItem[]>>(
    "/api/question-list/my",
    { params: { questionId } }
  );
  return response.data;
}

export async function createQuestionList(name: string) {
  const response = await axios.post<ApiResponse<number>>(
    "/api/question-list/create",
    { name }
  );
  return response.data;
}

export async function addQuestionToList(
  questionListId: number,
  questionId: number
) {
  const response = await axios.post<ApiResponse<boolean>>(
    "/api/question-list/question/add",
    { questionListId, questionId }
  );
  return response.data;
}

export async function removeQuestionFromList(
  questionListId: number,
  questionId: number
) {
  const response = await axios.post<ApiResponse<boolean>>(
    "/api/question-list/question/remove",
    { questionListId, questionId }
  );
  return response.data;
}

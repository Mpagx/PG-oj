import axios from "axios";

export type QuestionSolution = {
  id: number;
  questionId: number;
  userId: number;
  title: string;
  content: string;
  userName?: string;
  userAvatar?: string;
  createTime?: string;
  updateTime?: string;
  own?: boolean;
  deletable?: boolean;
};

export type SolutionAccess = {
  unlocked: boolean;
  canPublish: boolean;
  revealedWithoutAccepted: boolean;
  reason?: string;
  mine?: QuestionSolution;
  page?: {
    records: QuestionSolution[];
    total: number;
    current: number;
    size: number;
  };
};

type Response<T> = { code: number; data: T; message?: string };

export async function getQuestionSolutions(
  questionId: number,
  current = 1,
  pageSize = 10
) {
  return (
    await axios.get<Response<SolutionAccess>>("/api/question-solution/page", {
      params: { questionId, current, pageSize },
    })
  ).data;
}

export async function saveQuestionSolution(body: {
  questionId: number;
  title: string;
  content: string;
}) {
  return (
    await axios.post<Response<number>>("/api/question-solution/save", body)
  ).data;
}

export async function revealQuestionSolutions(questionId: number) {
  return (
    await axios.post<Response<boolean>>("/api/question-solution/reveal", {
      id: questionId,
    })
  ).data;
}

export async function deleteQuestionSolution(id: number) {
  return (
    await axios.post<Response<boolean>>("/api/question-solution/delete", {
      id,
    })
  ).data;
}

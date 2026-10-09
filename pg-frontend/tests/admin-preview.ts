// Isolated visual test entry. Never imported by src/main.ts; no backend requests.
import { createApp, defineComponent, h } from "vue";
import { createRouter, createWebHistory, RouterView } from "vue-router";
import { createStore } from "vuex";
import axios from "axios";
import { registerArco } from "../src/plugins/arco";
import {
  QuestionControllerService,
  QuestionSubmitControllerService,
  UserControllerService,
  Question,
} from "../src/generated";
import { CancelablePromise } from "../src/generated/core/CancelablePromise";
import { DEFAULT_JUDGE_CONFIG } from "../src/utils/questionAdmin";
import ManageQuestionView from "../src/views/question/ManageQuestionView.vue";
import AddQuestionView from "../src/views/question/AddQuestionView.vue";
import MyProfileView from "../src/views/user/MyProfileView.vue";
import GlobalaHeader from "../src/components/GlobalaHeader.vue";

const questions: Question[] = [
  {
    id: 15,
    title: "A + B",
    difficulty: "EASY",
    status: "PUBLISHED",
    tags: '["入门","数学"]',
    content:
      "## 题目描述\n\n给定两个整数 a 和 b，请计算它们的和。\n\n## 输入格式\n\n一行包含两个整数，以空格分隔。\n\n## 输出格式\n\n输出它们的和。\n\n## 数据范围\n\n-10⁹ ≤ a, b ≤ 10⁹\n\n## 输入样例\n\n```text\n1 2\n```\n\n## 输出样例\n\n```text\n3\n```",
    answer:
      "## 解题思路\n\n读取两个整数，输出两数之和。\n\n```java\nlong sum = a + b;\n```",
    judgeCase: JSON.stringify([
      { input: "1 2", output: "3" },
      { input: "0 0", output: "0" },
      { input: "-1 2", output: "1" },
      { input: "-3 -5", output: "-8" },
      { input: "100 200", output: "300" },
      { input: "1000000000 1000000000", output: "2000000000" },
      { input: "-1000000000 -1000000000", output: "-2000000000" },
      { input: "1000000000 -1000000000", output: "0" },
    ]),
    judgeConfig: JSON.stringify(DEFAULT_JUDGE_CONFIG),
    submitNum: 16,
    acceptedNum: 2,
  },
  {
    id: 17,
    title: "二叉树的最大深度",
    difficulty: "MEDIUM",
    status: "PUBLISHED",
    tags: '["二叉树","递归","简单"]',
    content: "## 题目描述\n\n求二叉树的最大深度。",
    judgeCase: '[{"input":"1 2 3","output":"2"}]',
    judgeConfig: JSON.stringify(DEFAULT_JUDGE_CONFIG),
    submitNum: 24,
    acceptedNum: 18,
  },
  {
    id: 18,
    title: "图的遍历：广度优先搜索与最短路径",
    difficulty: "HARD",
    status: "DRAFT",
    tags: '["图","搜索","队列","进阶"]',
    content: "## 题目描述\n\n请计算无权图中两点之间的最短路径。",
    judgeCase: "invalid",
    judgeConfig: '{"timeLimit":1000,"memoryLimit":1000,"stackLimit":1000}',
    submitNum: 0,
    acceptedNum: 0,
  },
].map((question) => ({
  ...question,
  userId: 1,
  createTime: "2026-10-06T10:00:00",
  updateTime: "2026-10-06T18:00:00",
}));
QuestionControllerService.listQuestionByPageUsingPost = (query) => {
  const filtered = questions.filter(
    (question) =>
      (!query.id || query.id === question.id) &&
      (!query.title || question.title?.includes(query.title)) &&
      (!query.tags ||
        query.tags.every((tag) => question.tags?.includes(tag))) &&
      (!query.status || query.status === question.status) &&
      (!query.difficulty || query.difficulty === question.difficulty)
  );
  const start = ((query.current ?? 1) - 1) * (query.pageSize ?? 10);
  return new CancelablePromise((resolve) =>
    resolve({
      code: 0,
      data: {
        records: filtered.slice(start, start + (query.pageSize ?? 10)),
        total: filtered.length,
      },
    })
  );
};
// Fail closed for every write; visual tests cannot mutate the real database.
const denyWrite = () =>
  new CancelablePromise<{ code: number; message: string }>((resolve) =>
    resolve({ code: 50000, message: "预览模式禁止写入" })
  );
QuestionControllerService.addQuestionUsingPost = denyWrite;
QuestionControllerService.updateQuestionUsingPost = denyWrite;
QuestionControllerService.deleteQuestionUsingPost = denyWrite;
const activity = Array.from({ length: 30 }, (_, index) => ({
  day: `2026-09-${String(index + 1).padStart(2, "0")}`,
  submissionCount: index % 4,
  acceptedCount: index % 3 === 0 ? 1 : 0,
}));
QuestionSubmitControllerService.getMySubmissionOverviewUsingGet = () =>
  new CancelablePromise((resolve) =>
    resolve({
      code: 0,
      data: {
        totalSubmissions: 42,
        acceptedSubmissions: 26,
        attemptedQuestions: 18,
        solvedQuestions: 12,
        activity,
      },
    })
  );
QuestionSubmitControllerService.listMyQuestionSubmissionsUsingPost = () =>
  new CancelablePromise((resolve) =>
    resolve({
      code: 0,
      data: {
        total: 3,
        records: [
          {
            id: 31,
            questionId: 15,
            questionVO: { id: 15, title: "A + B" },
            language: "java",
            status: 2,
            judgeInfo: { message: "Accepted", time: 126, memory: 18200 },
            createTime: "2026-10-07T18:30:00",
          },
          {
            id: 30,
            questionId: 17,
            questionVO: { id: 17, title: "二叉树的最大深度" },
            language: "java",
            status: 2,
            judgeInfo: { message: "Wrong Answer", time: 98, memory: 17600 },
            createTime: "2026-10-06T16:20:00",
          },
          {
            id: 29,
            questionId: 18,
            questionVO: { id: 18, title: "图的遍历" },
            language: "java",
            status: 1,
            createTime: "2026-10-05T09:15:00",
          },
        ],
      },
    })
  );
UserControllerService.updateMyUserUsingPost = denyWrite;
axios.get = ((url: string, config: { params: { id: string } }) => {
  if (url !== "/api/question/get/admin")
    return Promise.reject(Error("预览模式禁止网络请求"));
  return Promise.resolve({
    data: {
      code: 0,
      data: questions.find(
        (question) => question.id === Number(config.params.id)
      ),
    },
  });
}) as unknown as typeof axios.get;

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/manage/question/" },
    { path: "/manage/question/", component: ManageQuestionView },
    { path: "/add/question", component: AddQuestionView },
    { path: "/update/question", component: AddQuestionView },
    { path: "/profile", component: MyProfileView },
  ],
});
const app = createApp(
  defineComponent({
    setup: () => () =>
      h("div", [
        h("div", { style: "height:64px;background:#fff" }, [h(GlobalaHeader)]),
        h("div", { style: "max-width:1280px;margin:auto;padding:24px;" }, [
          h(
            "div",
            { style: "font-size:12px;color:#86909c;margin-bottom:20px" },
            "Cookie OJ · 管理员页面预览（测试数据，不连接数据库）"
          ),
          h(RouterView),
        ]),
      ]),
  })
);
const store = createStore({
  modules: {
    user: {
      namespaced: true,
      state: () => ({
        initialized: true,
        loginUser: {
          id: 2,
          userName: "AAAA",
          userProfile: "正在学习 Java 和数据结构，目标是每天完成一道题。",
          userRole: "user",
          createTime: "2026-08-01T10:00:00",
        },
      }),
    },
  },
});
registerArco(app);
app.use(store).use(router).mount("#app");

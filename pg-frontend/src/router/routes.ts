import { RouteRecordRaw } from "vue-router";
const AdminView = () => import("@/views/AdminView.vue");
const NoAuthView = () => import("@/views/NoAuthView.vue");
import ACCESS_ENUM from "@/access/accessEnum";
const UserLayout = () => import("@/layouts/UserLayout.vue");
const UserLoginView = () => import("@/views/user/UserLoginView.vue");
const UserRegisterView = () => import("@/views/user/UserRegisterView.vue");
const ForgotPasswordView = () => import("@/views/user/ForgotPasswordView.vue");
const MyProfileView = () => import("@/views/user/MyProfileView.vue");
const AddQuestionView = () => import("@/views/question/AddQuestionView.vue");
const ManageQuestionView = () =>
  import("@/views/question/ManageQuestionView.vue");
const QuestionsView = () => import("@/views/question/QuestionsView.vue");
const ViewQuestionView = () => import("@/views/question/ViewQuestionView.vue");
const QuestionSubmitDetailView = () =>
  import("@/views/question/QuestionSubmitDetailView.vue");

export const routes: Array<RouteRecordRaw> = [
  {
    path: "/user",
    name: "用户",
    component: UserLayout,
    children: [
      {
        path: "/user/login",
        name: "用户登录",
        component: UserLoginView,
      },
      {
        path: "/user/register",
        name: "用户注册",
        component: UserRegisterView,
      },
      {
        path: "/user/forgot-password",
        name: "找回密码",
        component: ForgotPasswordView,
      },
    ],
    meta: {
      hideInMenu: true,
    },
  },
  {
    path: "/questions",
    redirect: "/",
    meta: { hideInMenu: true },
  },
  {
    path: "/view/question/:id",
    name: "在线题目",
    component: ViewQuestionView,
    props: true,
    meta: {
      access: ACCESS_ENUM.USER,
      hideInMenu: true,
    },
  },
  {
    path: "/submission/:id",
    name: "提交详情",
    component: QuestionSubmitDetailView,
    props: true,
    meta: {
      access: ACCESS_ENUM.USER,
      hideInMenu: true,
    },
  },
  {
    path: "/update/question",
    name: "更新题目",
    component: AddQuestionView,
    meta: {
      hideInMenu: true,
      access: ACCESS_ENUM.ADMIN,
    },
  },
  {
    path: "/add/question",
    name: "新建题目",
    component: AddQuestionView,
    meta: { hideInMenu: true, access: ACCESS_ENUM.ADMIN },
  },
  {
    path: "/manage/question/",
    name: "管理题目",
    component: ManageQuestionView,
    meta: { access: ACCESS_ENUM.ADMIN },
  },
  {
    path: "/",
    name: "题库",
    component: QuestionsView,
  },
  {
    path: "/profile",
    name: "我的",
    component: MyProfileView,
    meta: { access: ACCESS_ENUM.USER },
  },

  // {
  //   path: "/hide",
  //   name: "隐藏页面",
  //   component: ExampleView,
  //   meta: {
  //     hideInMenu: true,
  //   },
  // },
  {
    path: "/noAuth",
    name: "无权限",
    component: NoAuthView,
    meta: {
      hideInMenu: true,
    },
  },
  {
    path: "/admin",
    name: "用户管理",
    component: AdminView,
    meta: {
      access: ACCESS_ENUM.ADMIN,
    },
  },
  {
    path: "/about",
    redirect: "/profile",
    meta: { hideInMenu: true },
  },
];

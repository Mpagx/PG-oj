// initial state
import { StoreOptions } from "vuex";
import ACCESS_ENUM from "@/access/accessEnum";
import { UserControllerService } from "@/generated";

export default {
  namespaced: true,

  // user.ts。登录后刷新状态丢失问题
  state: () => ({
    loginUser: (() => {
      const raw = localStorage.getItem("loginUser");
      if (!raw) {
        return {
          userAccount: "未登录",
          userRole: ACCESS_ENUM.NOT_LOGIN,
        };
      }
      try {
        return JSON.parse(raw);
      } catch {
        // 脏数据兜底
        return {
          userAccount: "未登录",
          userRole: ACCESS_ENUM.NOT_LOGIN,
        };
      }
    })(),
  }),

  getters: {},
  actions: {
    async getLoginUser({ commit, state }) {
      const res = await UserControllerService.getLoginUserUsingGet();

      console.log("【前端】getLoginUser 接口返回:", res);

      if (res.code === 0 && res.data) {
        commit("updateUser", res.data);
        console.log("【前端】commit updateUser:", res.data);
      } else {
        commit("updateUser", {
          userAccount: "未登录",
          userRole: ACCESS_ENUM.NOT_LOGIN,
        });
      }
    },
  },

  // actions: {
  //   async getLoginUser({ commit, state }, payload) {
  //     const res = await UserControllerService.getLoginUserUsingGet();
  //
  //     if (res.code === 0) {
  //       commit("updateUser", res.data);
  //     } else {
  //       commit("updateUser", {
  //         ...state.loginUser,
  //         userRole: ACCESS_ENUM.NOT_LOGIN,
  //       });
  //     }
  //
  //     // todo 改为从远程请求获取登录信息
  //   },
  // },

  mutations: {
    updateUser(state, payload) {
      state.loginUser = payload;
    },
  },
} as StoreOptions<any>;

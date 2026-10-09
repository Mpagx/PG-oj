import { Module } from "vuex";
import ACCESS_ENUM from "@/access/accessEnum";
import { UserControllerService, LoginUserVO } from "@/generated";

export interface UserState {
  loginUser: LoginUserVO;
  initialized: boolean;
}
const anonymous = (): LoginUserVO => ({
  userName: "未登录",
  userRole: ACCESS_ENUM.NOT_LOGIN,
});
let pending: Promise<LoginUserVO> | undefined;

export default {
  namespaced: true,
  state: (): UserState => ({ loginUser: anonymous(), initialized: false }),
  getters: {},
  actions: {
    async getLoginUser({ commit }) {
      if (!pending) {
        pending = UserControllerService.getLoginUserUsingGet()
          .then((res) => (res.code === 0 && res.data ? res.data : anonymous()))
          .catch(() => anonymous())
          .finally(() => {
            pending = undefined;
          });
      }
      commit("updateUser", await pending);
    },
  },
  mutations: {
    updateUser(state, payload: LoginUserVO) {
      state.loginUser = payload;
      state.initialized = true;
    },
  },
} as Module<UserState, { user: UserState }>;

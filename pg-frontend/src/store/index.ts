import { createStore } from "vuex";
import user, { UserState } from "./user";
export default createStore<{ user: UserState }>({
  actions: {},
  modules: {
    user,
  },
});

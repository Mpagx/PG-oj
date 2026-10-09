// main.ts
import { createApp } from "vue";
import App from "./App.vue";
import { registerArco } from "@/plugins/arco";
import router from "./router";
import store from "./store";
import { OpenAPI } from "./generated/core/OpenAPI";
import "../src/plugins/axios";
import "@/access";
// 使用代理时，BASE 留空或根路径，让请求走当前前端域名的 /api
OpenAPI.BASE = "";

const app = createApp(App);
registerArco(app);
app.use(store).use(router).mount("#app");

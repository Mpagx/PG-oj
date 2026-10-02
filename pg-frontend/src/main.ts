// main.ts
import { createApp } from "vue";
import App from "./App.vue";
import ArcoVue from "@arco-design/web-vue";
import "@arco-design/web-vue/dist/arco.css";
import router from "./router";
import store from "./store";
import { OpenAPI } from "./generated/core/OpenAPI";
import "../src/plugins/axios";
import "@/access";
//markdown插件
import "bytemd/dist/index.css";
// 使用代理时，BASE 留空或根路径，让请求走当前前端域名的 /api
OpenAPI.BASE = "";

createApp(App).use(ArcoVue).use(store).use(router).mount("#app");

<template>
  <div id="app">
    <template v-if="route.path.startsWith('/user')">
      <router-view />
    </template>
    <template v-else>
      <BasicLayout />
    </template>
  </div>
</template>

<style>
#app {
  min-height: 100vh;
}

body {
  margin: 0;
  background: #f5f7fa;
}
</style>
<script setup lang="ts">
import BasicLayout from "@/layouts/BasicLayout.vue";
import store from "@/store";
import { onMounted } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
/**
 * 全局初始化函数，有全局单词调用的代码，都可以写这里
 */
const doInit = () => {
  console.log("hello，欢迎来到我的项目");
};

onMounted(async () => {
  await store.dispatch("user/getLoginUser");
  doInit();
});
</script>

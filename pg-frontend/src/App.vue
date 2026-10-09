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
import store from "@/store";
import { defineAsyncComponent, onMounted } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const BasicLayout = defineAsyncComponent(
  () => import("@/layouts/BasicLayout.vue")
);
onMounted(async () => {
  if (!store.state.user.initialized) await store.dispatch("user/getLoginUser");
});
</script>

<template>
  <div id="basicLayout">
    <a-layout style="height: 400px">
      <a-layout-header class="header">
        <GlobalaHeader></GlobalaHeader>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view></router-view>
      </a-layout-content>
      <a-layout-footer class="footer">彭彭彭OJ判题系统</a-layout-footer>
    </a-layout>
  </div>
</template>

<style scoped>
#basicLayout {
}
.header {
  margin-bottom: 16px;
  box-shadow: #eee 1px 1px 5px;
}
.content {
  background: linear-gradient(to right, #8ff1f1, #ffffff);

  padding: 20px;
}
.footer {
  background: #6ed2d2;
}
</style>
<script setup lang="ts">
import GlobalaHeader from "@/components/GlobalaHeader.vue";
</script>

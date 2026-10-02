<template>
  <div id="basicLayout">
    <a-layout class="layout">
      <a-layout-header class="header">
        <GlobalaHeader></GlobalaHeader>
      </a-layout-header>
      <a-layout-content class="content">
        <div class="content-container">
          <router-view></router-view>
        </div>
      </a-layout-content>
      <a-layout-footer class="footer">
        Cookie OJ 判题系统 · 在线判题平台
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<style scoped>
#basicLayout {
  min-height: 100vh;
}

.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.header {
  height: 64px;
  line-height: normal;
  padding: 0;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;
}

.content {
  flex: 1;
  padding: 24px;
  background: #f5f7fa;
}

/* 所有内容页共用同一套居中容器，保证版式统一 */
.content-container {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
}

.footer {
  flex-shrink: 0;
  padding: 16px 24px;
  text-align: center;
  background: #2c2c3a;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}
</style>

<script setup lang="ts">
import GlobalaHeader from "@/components/GlobalaHeader.vue";
</script>

<template>
  <div id="userLayout">
    <a-layout class="layout-container">
      <!-- 顶部导航 -->
      <a-layout-header class="header">
        <div class="header-inner">
          <div class="logo-area" @click="goHome">
            <img class="logo" :src="logoImg" alt="logo" />
            <span class="system-title">Cookie OJ 判题系统</span>
          </div>
          <a-space class="header-actions">
            <a-button type="text" class="header-btn" @click="openGithub">
              <template #icon><github-outlined /></template>
              GitHub
            </a-button>
            <a-button type="primary" ghost class="header-btn" @click="goLogin">
              登录
            </a-button>
          </a-space>
        </div>
      </a-layout-header>

      <!-- 主体内容 -->
      <a-layout-content class="content">
        <div class="content-wrapper">
          <router-view v-slot="{ Component }">
            <transition name="fade-slide" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </a-layout-content>

      <!-- 底部 -->
      <a-layout-footer class="footer">
        <div class="footer-inner">
          <span>© 2025 Cookie OJ 判题系统</span>
          <a-divider type="vertical" />
          <a href="https://github.com" target="_blank" rel="noopener"
            >关于我们</a
          >
          <a-divider type="vertical" />
          <a href="mailto:contact@example.com">联系我们</a>
        </div>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import logoImg from "@/assets/cz.png";
import GithubOutlined from "@ant-design/icons-vue/es/icons/GithubOutlined";

const router = useRouter();

const goHome = () => {
  router.push("/");
};

const goLogin = () => {
  router.push("/user/login");
};

const openGithub = () => {
  window.open("https://github.com", "_blank", "noopener");
};
</script>

<style scoped>
/* ========== 全局容器 ========== */
#userLayout {
  min-height: 100vh;
}

.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ========== 顶部 Header ========== */
.header {
  padding: 0 32px;
  height: 64px;
  line-height: 64px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

/* Logo 区域 */
.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.logo {
  width: 100px;
  height: 100px;
  object-fit: contain;
  border-radius: 8px;
  transition: transform 0.3s ease;
}

.logo:hover {
  transform: scale(1.08);
}

.system-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

/* Header 右侧操作区 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* ========== 主体 Content ========== */
.content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background: linear-gradient(135deg, #e8f5e9 0%, #e3f2fd 50%, #f3e5f5 100%);
  background-size: 200% 200%;
  animation: gradientShift 12s ease infinite;
}

@keyframes gradientShift {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.content-wrapper {
  position: relative; /* ✅ 绝对定位基准 */
  box-sizing: border-box;

  width: 100%;
  max-width: 420px;
  background: #ffffff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1), 0 4px 12px rgba(0, 0, 0, 0.05);
}
/* ========== 底部 Footer ========== */
.footer {
  padding: 10px 24px;
  text-align: center;
  background: #2c2c3a;
  color: rgba(255, 255, 255, 0.4);
  font-size: 11px;
  flex-shrink: 0;
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
}

.footer a {
  color: rgba(255, 255, 255, 0.4);
  transition: color 0.2s;
  font-size: 11px;
}

.footer a:hover {
  color: #42b983;
}

/* ========== 路由切换动画 ========== */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .header {
    padding: 0 16px;
  }

  .system-title {
    font-size: 15px;
  }

  .content {
    padding: 24px 16px;
  }

  .content-wrapper {
    padding: 28px 20px;
    border-radius: 12px;
  }

  .footer {
    font-size: 10px;
    padding: 8px 16px;
  }
}

@media (max-width: 480px) {
  .system-title {
    display: none;
  }

  .logo {
    width: 36px;
    height: 36px;
  }
}
</style>

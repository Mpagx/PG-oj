<template>
  <div id="globalHeader">
    <div class="header-inner">
      <!-- 左侧：Logo + 导航菜单 -->
      <div class="header-left">
        <div class="logo-area" @click="goHome">
          <img class="logo" :src="logoImg" alt="logo" />
          <span class="system-title">Cookie OJ</span>
        </div>
        <a-menu
          mode="horizontal"
          class="menu"
          :selected-keys="selectedKeys"
          @menu-item-click="doMenuClick"
        >
          <a-menu-item v-for="item in visibleRoutes" :key="item.path">
            {{ item.name }}
          </a-menu-item>
        </a-menu>
      </div>

      <!-- 右侧：用户信息 -->
      <div class="header-right">
        <template v-if="isLogin">
          <a-dropdown @select="handleUserMenuClick">
            <div class="user-info">
              <a-avatar :size="30" class="user-avatar">{{
                avatarText
              }}</a-avatar>
              <span class="user-name">{{ userName }}</span>
            </div>
            <template #content>
              <a-doption value="logout">退出登录</a-doption>
            </template>
          </a-dropdown>
        </template>
        <template v-else>
          <a-button type="primary" size="small" @click="goLogin">登录</a-button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { useStore } from "vuex";
import logoImg from "../assets/cz.png";
import { routes } from "@/router/routes";
import checkAccess from "@/access/checkAccess";
import ACCESS_ENUM from "@/access/accessEnum";
import { UserControllerService } from "@/generated";
import message from "@arco-design/web-vue/es/message";

const router = useRouter();
const store = useStore();

// 默认选中的菜单项
const selectedKeys = ref<string[]>(["/"]);

// 路由跳转后，更新选中的菜单项
router.afterEach((to) => {
  selectedKeys.value = [to.path];
});

const goHome = () => router.push("/");
const goLogin = () => router.push("/user/login");

const doMenuClick = (key: string) => {
  router.push(key);
};

// 根据权限过滤后，在菜单里展示的路由
const visibleRoutes = computed(() => {
  return routes.filter((item) => {
    if (item.meta?.hideInMenu) {
      return false;
    }
    if (!checkAccess(store.state.user.loginUser, item.meta?.access)) {
      return false;
    }
    return true;
  });
});

const loginUser = computed(() => store.state.user.loginUser);
const isLogin = computed(
  () =>
    loginUser.value?.userRole &&
    loginUser.value.userRole !== ACCESS_ENUM.NOT_LOGIN
);
const userName = computed(
  () => loginUser.value?.userName || loginUser.value?.userAccount || "未登录"
);
const avatarText = computed(() => userName.value.charAt(0));

const handleUserMenuClick = (value: string) => {
  if (value === "logout") {
    doLogout();
  }
};

const doLogout = async () => {
  try {
    await UserControllerService.userLogoutUsingPost();
  } catch (e) {
    // 忽略登出接口异常，本地状态仍然要清理
  }
  store.commit("user/updateUser", {
    userAccount: "未登录",
    userRole: ACCESS_ENUM.NOT_LOGIN,
  });
  message.success("已退出登录");
  router.push("/");
};
</script>

<style scoped>
#globalHeader {
  height: 100%;
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
  min-width: 0;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo {
  height: 36px;
  width: 36px;
  object-fit: contain;
}

.system-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
}

.menu {
  background: transparent;
}

.menu :deep(.arco-menu-item) {
  font-size: 15px;
  line-height: 32px;
  padding: 0 14px;
}

.header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f7fa;
}

.user-avatar {
  background: #165dff;
  color: #fff;
}

.user-name {
  font-size: 14px;
  color: #333;
}
</style>

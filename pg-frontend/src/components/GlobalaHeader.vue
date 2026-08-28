<script setup lang="ts">
import logoImg from "../assets/cz.png";
import { routes } from "@/router/routes";
import { useRoute, useRouter } from "vue-router";
const router = useRouter();
import { useStore } from "vuex";
import { compile, computed, ref } from "vue";
import checkAccess from "@/access/checkAccess";
import ACCEXX_ENUM from "@/access/accessEnum";
//默认主页
const seleredKeys = ref(["/"]);

//路由跳转后，更新选中的菜单项
router.afterEach((to, from, falure) => {
  seleredKeys.value = [to.path];
});

const doMenuClick = (key: string) => {
  //原本router.push(push: key,});就不好使，换成如下好使了
  router.push(key);
};
const store = useStore();
console.log(store.state.user.loginUser);

//在菜单显示的路由数量
const visibleRoutes = computed(() => {
  return routes.filter((item, index) => {
    if (item.meta?.hideInMenu) {
      return false;
    }
    const loginUser = store.state.user.loginUser;
    //根据权限过滤菜单 TODO 鱼皮在这里强制类型为String
    if (!checkAccess(loginUser, item.meta?.access)) {
      return false;
    }
    return true;
  });
});
</script>

<template>
  <a-row id="globalHeader" align="center">
    <a-col flex="auto">
      <a-menu
        mode="horizontal"
        class="menu"
        :selected-keys="seleredKeys"
        @menu-item-click="doMenuClick"
      >
        <a-menu-item
          key="0"
          :style="{ padding: 0, marginRight: '38px' }"
          disabled
        >
          <div class="title-bar">
            <img class="logo" :src="logoImg" alt="logo" />
            <div>彭OJ</div>
          </div>
        </a-menu-item>

        <a-menu-item v-for="item in visibleRoutes" :key="item.path">
          {{ item.name }}
        </a-menu-item>
      </a-menu>
    </a-col>
    <a-col flex="100px">
      <div>
        <!--        //状态变量-->
        {{ store.state.user?.loginUser?.userName ?? "未登录" }}
      </div>
    </a-col>
  </a-row>
</template>

<style scoped>
.menu {
  font-size: 16px;
}

.menu :deep(.arco-menu-item) {
  font-size: 16px;
  line-height: 32px;
  padding: 0 18px;
}

.title-bar {
  display: flex;
  align-items: center;
}

.logo {
  height: 32px;
  width: 32px;
  object-fit: contain;
  margin-right: 8px;
}
</style>

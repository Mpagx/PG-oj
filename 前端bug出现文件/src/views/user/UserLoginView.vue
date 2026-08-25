<template>
  <div id="userLoginView">
    <a-form :model="form" class="login-form" @submit="handleSubmit">
      欢迎你来到彭OJ系统！
      <br />
      <br />
      <br />
      <br />
      <br />
      <br />
      <a-form-item field="userAccount" tooltip="请输入账号" label="账号">
        <a-input v-model="form.userAccount" placeholder="请输入账号" />
      </a-form-item>

      <a-form-item field="userPassword" tooltip="密码不少于六位" label="密码">
        <a-input-password
          v-model="form.userPassword"
          placeholder="请输入密码"
        />
      </a-form-item>

      <a-form-item>
        <a-button type="primary" html-type="submit" class="submit-btn">
          登录
        </a-button>
      </a-form-item>
    </a-form>
    <a-button type="outline" class="register-btn" @click="goRegister">
      还没有账户？立即注册
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { reactive } from "vue";
import { UserControllerService, UserLoginRequest } from "@/generated";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import { useStore } from "vuex";

const form = reactive({
  userAccount: "",
  userPassword: "",
} as UserLoginRequest);

const router = useRouter();
const store = useStore();

const handleSubmit = async () => {
  const res = await UserControllerService.userLoginUsingPost(form);
  if (res.code === 0) {
    await store.dispatch("user/getLoginUser");
    await router.push({ path: "/", replace: true });
  } else {
    message.error("登陆失败，" + res.message);
  }
};
const goRegister = () => {
  router.push("/user/register");
};
</script>

<style scoped>
#userLoginView {
  position: relative; /* ✅ 绝对定位的真正锚点 */
  min-height: 340px; /* ✅ 给按钮一个稳定的“地盘” */
}
/* ✅ 表单整体不要写死宽度 */
.login-form {
  width: 100%;
}

/* ✅ 标签往左 */
:deep(.arco-form-item-label-col) {
  width: 70px;
  text-align: right;
  padding-right: 10px;
}

/* ✅ 输入框变短，绝不超过白块 */
:deep(.arco-input-wrapper),
:deep(.arco-input-password-wrapper) {
  width: 240px;
  max-width: 100%;
}

/* ✅ 按钮左对齐 + 变短 */
.submit-btn {
  width: 100px;
  margin-left: 80px; /* 和 label 对齐 */
}
.register-btn {
  position: absolute;
  right: 36px;
  bottom: 24px;
  font-size: 13px;
}
</style>

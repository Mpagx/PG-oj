<template>
  <div id="userLoginView">
    <h1 class="title">欢迎登录 Cookie OJ</h1>
    <a-form
      :model="form"
      layout="vertical"
      class="login-form"
      @submit="handleSubmit"
    >
      <a-form-item field="userName" tooltip="唯一用户名用于登录" label="用户名">
        <a-input v-model="form.userName" placeholder="请输入用户名" />
      </a-form-item>
      <div class="password-actions">
        <a-button type="text" size="small" @click="goForgotPassword"
          >忘记密码？</a-button
        >
      </div>

      <a-form-item field="userPassword" tooltip="密码不少于八位" label="密码">
        <a-input-password
          v-model="form.userPassword"
          placeholder="请输入密码"
        />
      </a-form-item>

      <CaptchaChallenge
        ref="captcha"
        v-model="form.captchaAnswer"
        @ready="captchaReady = $event"
      />
      <a-form-item>
        <a-button
          type="primary"
          html-type="submit"
          class="submit-btn"
          :loading="submitting"
          :disabled="!captchaReady || submitting"
        >
          登录
        </a-button>
      </a-form-item>
    </a-form>
    <a-button type="outline" class="register-btn" @click="goRegister">
      还没有用户名？立即注册
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import CaptchaChallenge from "@/components/CaptchaChallenge.vue";
import { UserControllerService } from "@/generated";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import { useStore } from "vuex";

const form = reactive({
  userName: "",
  userPassword: "",
  captchaAnswer: "",
});
const captcha = ref<InstanceType<typeof CaptchaChallenge>>();
const captchaReady = ref(false);
const submitting = ref(false);

const router = useRouter();
const store = useStore();

const handleSubmit = async () => {
  if (submitting.value || !captchaReady.value) return;
  if (
    !form.userName.trim() ||
    !form.userPassword ||
    !/^\d{6}$/.test(form.captchaAnswer)
  ) {
    message.warning("请填写用户名、密码和 6 位验证码");
    return;
  }
  submitting.value = true;
  try {
    const res = await UserControllerService.userLoginUsingPost({
      ...form,
      userName: form.userName.trim(),
    });
    if (res.code === 0) {
      await store.dispatch("user/getLoginUser");
      await router.push({ path: "/", replace: true });
    } else {
      message.error("登陆失败，" + res.message);
    }
  } catch (error) {
    message.error("登录失败，请稍后再试");
  } finally {
    submitting.value = false;
    captcha.value?.refresh();
    form.captchaAnswer = "";
  }
};
const goRegister = () => {
  router.push("/user/register");
};
const goForgotPassword = () => router.push("/user/forgot-password");
</script>

<style scoped>
.login-form {
  width: 100%;
}
.title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 28px;
  text-align: center;
}
.submit-btn,
.register-btn {
  width: 100%;
  min-height: 40px;
}
.password-actions {
  display: flex;
  justify-content: flex-end;
  margin: -12px 0 12px;
}
:deep(.arco-form-item-content) {
  min-width: 0;
}
:deep(.arco-form-item:last-child) {
  margin-bottom: 12px;
}
</style>

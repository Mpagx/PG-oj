<template>
  <div id="forgotPasswordView">
    <h1>找回密码</h1>
    <p class="subtitle">验证码会发送到账号已验证的邮箱。</p>
    <a-alert v-if="emailAvailable === false" type="warning" class="notice">
      当前环境尚未配置邮件服务，请联系管理员。
    </a-alert>
    <a-form :model="form" layout="vertical" @submit="resetPassword">
      <a-form-item label="邮箱" required>
        <a-input v-model="form.email" placeholder="请输入已绑定邮箱" />
      </a-form-item>
      <a-form-item label="邮箱验证码" required>
        <a-input v-model="form.code" :max-length="6" placeholder="6 位验证码">
          <template #append>
            <a-button
              :loading="sending"
              :disabled="emailAvailable !== true || !captchaReady"
              @click="sendCode"
              >发送验证码</a-button
            >
          </template>
        </a-input>
      </a-form-item>
      <CaptchaChallenge
        ref="captcha"
        v-model="form.captchaAnswer"
        @ready="captchaReady = $event"
      />
      <a-form-item label="新密码" required>
        <a-input-password
          v-model="form.newPassword"
          placeholder="不少于 8 位"
        />
      </a-form-item>
      <a-form-item label="确认新密码" required>
        <a-input-password
          v-model="form.checkPassword"
          placeholder="再次输入新密码"
        />
      </a-form-item>
      <a-button
        type="primary"
        html-type="submit"
        long
        :loading="resetting"
        :disabled="emailAvailable !== true || resetting"
        >重置密码</a-button
      >
    </a-form>
    <a-button
      type="text"
      long
      class="back-button"
      @click="router.push('/user/login')"
    >
      返回登录
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import message from "@arco-design/web-vue/es/message";
import CaptchaChallenge from "@/components/CaptchaChallenge.vue";
import { UserControllerService } from "@/generated";

const router = useRouter();
const form = reactive({
  email: "",
  code: "",
  captchaAnswer: "",
  newPassword: "",
  checkPassword: "",
});
const captcha = ref<InstanceType<typeof CaptchaChallenge>>();
const captchaReady = ref(false);
const emailAvailable = ref<boolean>();
const sending = ref(false);
const resetting = ref(false);

onMounted(async () => {
  try {
    const res = await UserControllerService.emailStatusUsingGet();
    emailAvailable.value = res.code === 0 && res.data === true;
  } catch {
    emailAvailable.value = false;
  }
});

const sendCode = async () => {
  if (
    !/^\S+@\S+\.\S+$/.test(form.email.trim()) ||
    !/^\d{6}$/.test(form.captchaAnswer)
  ) {
    message.warning("请填写正确邮箱和图片验证码");
    return;
  }
  sending.value = true;
  try {
    const res = await UserControllerService.sendEmailCodeUsingPost({
      purpose: "RESET",
      email: form.email.trim(),
      captchaAnswer: form.captchaAnswer,
    });
    if (res.code !== 0) throw Error(res.message || "发送失败");
    message.success("如果该邮箱已绑定账号，验证码将在稍后送达");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "发送失败");
  } finally {
    sending.value = false;
    form.captchaAnswer = "";
    captcha.value?.refresh();
  }
};

const resetPassword = async () => {
  if (!/^\d{6}$/.test(form.code) || form.newPassword.length < 8) {
    message.warning("请输入 6 位邮箱验证码和不少于 8 位的新密码");
    return;
  }
  if (form.newPassword !== form.checkPassword) {
    message.warning("两次输入的新密码不一致");
    return;
  }
  resetting.value = true;
  try {
    const res = await UserControllerService.resetPasswordUsingPost({
      email: form.email.trim(),
      code: form.code,
      newPassword: form.newPassword,
      checkPassword: form.checkPassword,
    });
    if (res.code !== 0) throw Error(res.message || "重置失败");
    message.success("密码已重置，请重新登录");
    await router.replace("/user/login");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "重置失败");
  } finally {
    resetting.value = false;
  }
};
</script>

<style scoped>
h1 {
  margin: 0 0 8px;
  text-align: center;
  font-size: 22px;
}
.subtitle {
  margin: 0 0 24px;
  text-align: center;
  color: var(--color-text-3);
}
.notice {
  margin-bottom: 18px;
}
.back-button {
  margin-top: 12px;
}
</style>

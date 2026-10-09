<template>
  <div id="userRegisterView">
    <a-form
      :model="form"
      layout="vertical"
      class="register-form"
      @submit="handleSubmit"
    >
      <div class="title">欢迎注册 Cookie OJ 系统</div>

      <a-form-item field="userName" label="用户名">
        <a-input
          v-model="form.userName"
          placeholder="请输入唯一用户名"
          :max-length="32"
        />
      </a-form-item>

      <template v-if="emailEnabled">
        <a-form-item field="userEmail" label="邮箱">
          <a-input
            v-model="form.userEmail"
            placeholder="用于验证身份和找回密码"
          />
        </a-form-item>
        <a-form-item field="emailCode" label="邮箱验证码">
          <a-input
            v-model="form.emailCode"
            :max-length="6"
            placeholder="请输入邮件中的 6 位验证码"
          >
            <template #append>
              <a-button :loading="emailSending" @click="sendEmailCode"
                >发送验证码</a-button
              >
            </template>
          </a-input>
        </a-form-item>
      </template>

      <a-form-item field="userPassword" label="密码">
        <a-input-password
          v-model="form.userPassword"
          placeholder="请输入密码"
        />
      </a-form-item>

      <a-form-item field="checkPassword" label="确认密码">
        <a-input-password
          v-model="form.checkPassword"
          placeholder="请再次输入密码"
        />
      </a-form-item>

      <a-alert v-if="emailEnabled" type="info" class="email-tip">
        注册只验证邮箱，不再需要图片验证码。
      </a-alert>
      <a-alert v-else type="error" class="email-tip">
        邮箱服务尚未启用，请先配置后端邮箱环境变量再注册。
      </a-alert>
      <a-form-item>
        <a-button
          type="primary"
          html-type="submit"
          class="register-submit-btn"
          :loading="submitting"
          :disabled="submitting || !emailEnabled"
          >注册</a-button
        >
      </a-form-item>
    </a-form>

    <!-- ✅ 返回登录：左下角（可选） -->
    <a-button type="outline" class="back-login-btn" @click="goLogin">
      已经注册？返回登录
    </a-button>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { UserControllerService } from "@/generated";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";

const router = useRouter();

const form = reactive({
  userName: "",
  userPassword: "",
  checkPassword: "",
  userEmail: "",
  emailCode: "",
});
const submitting = ref(false);
const emailEnabled = ref(false);
const emailSending = ref(false);

onMounted(async () => {
  try {
    const res = await UserControllerService.emailStatusUsingGet();
    emailEnabled.value = res.code === 0 && res.data === true;
  } catch {
    emailEnabled.value = false;
  }
});

const sendEmailCode = async () => {
  if (!/^\S+@\S+\.\S+$/.test(form.userEmail.trim())) {
    message.warning("请输入正确的邮箱地址");
    return;
  }
  emailSending.value = true;
  try {
    const res = await UserControllerService.sendEmailCodeUsingPost({
      purpose: "REGISTER",
      email: form.userEmail.trim(),
    });
    if (res.code !== 0) throw Error(res.message || "发送失败");
    message.success("验证码已发送，10 分钟内有效");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "验证码发送失败");
  } finally {
    emailSending.value = false;
  }
};

const handleSubmit = async () => {
  if (submitting.value || !emailEnabled.value) return;
  // 前端校验
  if (!form.userName.trim()) {
    message.warning("请输入用户名");
    return;
  }
  if (
    form.userName.trim().length > 32 ||
    !/^[\p{L}\p{N}_-]+$/u.test(form.userName.trim())
  ) {
    message.warning("用户名需为 1 到 32 位文字、数字、下划线或短横线");
    return;
  }

  if (form.userPassword.length < 8) {
    message.warning("密码不少于 8 位");
    return;
  }

  if (form.userPassword !== form.checkPassword) {
    message.error("两次输入的密码不一致");
    return;
  }

  if (
    emailEnabled.value &&
    (!/^\S+@\S+\.\S+$/.test(form.userEmail.trim()) ||
      !/^\d{6}$/.test(form.emailCode))
  ) {
    message.warning("请输入邮箱和邮件中的 6 位验证码");
    return;
  }
  submitting.value = true;
  // 调用注册接口
  try {
    const res = await UserControllerService.userRegisterUsingPost({
      userName: form.userName.trim(),
      userPassword: form.userPassword,
      checkPassword: form.checkPassword,
      userEmail: form.userEmail.trim(),
      emailCode: form.emailCode,
    });

    if (res?.code === 0) {
      message.success("注册成功，请登录");
      await router.push("/user/login");
    } else {
      message.error("注册失败：" + (res?.message || "未知错误"));
    }
  } catch (error) {
    message.error("注册失败，请稍后再试");
  } finally {
    submitting.value = false;
  }
};

const goLogin = () => {
  router.push("/user/login");
};
</script>
<style scoped>
.register-form {
  width: 100%;
}
.title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 28px;
  text-align: center;
}
.register-submit-btn,
.back-login-btn {
  width: 100%;
  min-height: 40px;
}
.email-tip {
  margin-bottom: 18px;
}
:deep(.arco-form-item-content) {
  min-width: 0;
}
:deep(.arco-form-item:last-child) {
  margin-bottom: 12px;
}
</style>

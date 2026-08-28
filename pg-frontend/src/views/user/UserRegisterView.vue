<template>
  <div id="userRegisterView">
    <a-form :model="form" class="register-form" @submit="handleSubmit">
      <div class="title">欢迎注册 彭彭彭 OJ 系统</div>

      <a-form-item field="userAccount" label="昵称">
        <a-input v-model="form.userAccount" placeholder="请输入昵称" />
      </a-form-item>

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

      <a-form-item>
        <!-- 不写按钮，放右下角 -->
      </a-form-item>
    </a-form>

    <!-- ✅ 注册按钮：右下角 -->
    <a-button type="primary" class="register-submit-btn" @click="handleSubmit">
      注册
    </a-button>

    <!-- ✅ 返回登录：左下角（可选） -->
    <a-button type="outline" class="back-login-btn" @click="goLogin">
      已有账号？返回登录
    </a-button>
  </div>
</template>
<script setup lang="ts">
import { reactive } from "vue";
import { UserControllerService } from "@/generated";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";

const router = useRouter();

const form = reactive({
  userAccount: "",
  userPassword: "",
  checkPassword: "",
});

const handleSubmit = async () => {
  // 前端校验
  if (!form.userAccount.trim()) {
    message.warning("请输入账号");
    return;
  }

  if (form.userPassword.length < 6) {
    message.warning("密码不少于 6 位");
    return;
  }

  if (form.userPassword !== form.checkPassword) {
    message.error("两次输入的密码不一致");
    return;
  }

  // 调用注册接口
  const res = await UserControllerService.userRegisterUsingPost({
    userAccount: form.userAccount,
    userPassword: form.userPassword,
    checkPassword: form.checkPassword,
  });

  console.log("后端完整响应：", res);

  if (res?.code === 0) {
    message.success("注册成功，请登录");
    await router.push("/user/login");
  } else {
    message.error("注册失败：" + (res?.message || "未知错误"));
  }
};

const goLogin = () => {
  router.push("/user/login");
};
</script>
<style scoped>
#userRegisterView {
  position: relative;
  min-height: 420px;
}

.register-form {
  width: 100%;
}

.title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 24px;
  text-align: center;
}

/* 标签左对齐 */
:deep(.arco-form-item-label-col) {
  width: 80px;
  text-align: right;
  padding-right: 10px;
}

/* 输入框长度统一 */
:deep(.arco-input-wrapper),
:deep(.arco-input-password-wrapper) {
  width: 240px;
  max-width: 100%;
}

/* ✅ 注册按钮：右下角 */
.register-submit-btn {
  position: absolute;
  right: 36px;
  bottom: 24px;
}

/* ✅ 返回登录：左下角 */
.back-login-btn {
  position: absolute;
  left: 36px;
  bottom: 24px;
  font-size: 13px;
}
</style>

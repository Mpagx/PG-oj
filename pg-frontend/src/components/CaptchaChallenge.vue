<template>
  <a-form-item field="captchaAnswer" label="验证码">
    <div class="captcha-field">
      <div class="captcha-row">
        <a-input
          :model-value="modelValue"
          class="captcha-input"
          maxlength="6"
          inputmode="numeric"
          autocomplete="off"
          placeholder="输入 6 位数字"
          @update:model-value="$emit('update:modelValue', $event)"
        />
        <button
          type="button"
          class="captcha-image"
          aria-label="刷新验证码"
          :disabled="loading"
          @click="refresh"
        >
          <img
            v-if="url"
            :src="url"
            alt="验证码，点击刷新"
            @load="emit('ready', true)"
            @error="imageError"
          />
          <span v-else>{{ loading ? "正在加载…" : "加载失败，点击重试" }}</span>
        </button>
      </div>
      <div v-if="error" class="captcha-error" role="alert">{{ error }}</div>
      <div v-else class="captcha-hint">看不清？点击图片换一张</div>
    </div>
  </a-form-item>
</template>
<script setup lang="ts">
import {
  ref,
  onBeforeUnmount,
  defineProps,
  defineEmits,
  defineExpose,
} from "vue";
import axios from "axios";
defineProps<{ modelValue: string }>();
const emit = defineEmits(["update:modelValue", "ready"]);
const url = ref("");
const loading = ref(false);
const error = ref("");
let disposed = false;
let request: AbortController | undefined;
const clearImage = () => {
  if (url.value) URL.revokeObjectURL(url.value);
  url.value = "";
};
const imageError = () => {
  clearImage();
  error.value = "验证码图片无法显示，请点击重试";
  emit("ready", false);
};
const refresh = async () => {
  if (loading.value) return;
  loading.value = true;
  error.value = "";
  clearImage();
  emit("update:modelValue", "");
  emit("ready", false);
  request = new AbortController();
  try {
    const response = await axios.get<Blob>("/api/security/captcha", {
      params: { t: Date.now() },
      responseType: "blob",
      withCredentials: true,
      timeout: 8000,
      signal: request.signal,
    });
    if (disposed) return;
    if (!response.data.type.startsWith("image/") || !response.data.size) {
      throw new Error("验证码接口未返回图片，请确认后端已更新并启动");
    }
    url.value = URL.createObjectURL(response.data);
  } catch (cause) {
    if (disposed) return;
    error.value = axios.isAxiosError(cause)
      ? cause.response?.status === 429
        ? "刷新过于频繁，请一分钟后重试"
        : cause.response?.status === 404
        ? "验证码接口不存在，请更新并重启后端"
        : "验证码服务无法连接，请确认后端 8121 已正常启动"
      : cause instanceof Error
      ? cause.message
      : "验证码加载失败，请点击重试";
  } finally {
    loading.value = false;
  }
};
void refresh();
onBeforeUnmount(() => {
  disposed = true;
  request?.abort();
  clearImage();
});
defineExpose({ refresh });
</script>
<style scoped>
.captcha-field {
  width: 100%;
  min-width: 0;
}
.captcha-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.captcha-input {
  flex: 1 1 120px;
  min-width: 0;
}
.captcha-image {
  flex: 0 0 180px;
  max-width: 100%;
  height: 50px;
  overflow: hidden;
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  padding: 0;
  background: var(--color-fill-2);
  color: var(--color-text-2);
  cursor: pointer;
}
.captcha-image:disabled {
  cursor: wait;
}
.captcha-image img {
  display: block;
  width: 100%;
  height: 50px;
  object-fit: contain;
}
.captcha-hint,
.captcha-error {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
}
.captcha-hint {
  color: var(--color-text-3);
}
.captcha-error {
  color: rgb(var(--danger-6));
}
</style>

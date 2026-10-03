<template>
  <div id="questionSubmitDetailView">
    <a-card :bordered="false" class="detail-card">
      <template #title>
        <a-space>
          <span>提交详情 #{{ submission?.id || id }}</span>
          <a-tag :color="statusMeta.color">{{ statusMeta.label }}</a-tag>
          <a-spin v-if="isPending" :size="16" />
        </a-space>
      </template>
      <template #extra>
        <a-space>
          <a-button v-if="submission?.questionId" @click="backToQuestion">
            返回题目
          </a-button>
          <a-button :loading="loading" @click="refreshManually">
            刷新结果
          </a-button>
        </a-space>
      </template>

      <a-skeleton v-if="loading && !submission" animation>
        <a-space direction="vertical" fill>
          <a-skeleton-line :rows="6" />
        </a-space>
      </a-skeleton>

      <template v-else-if="submission">
        <div class="verdict-panel" :class="`verdict-${verdictTone}`">
          <div class="verdict-title">{{ verdictText }}</div>
          <div class="verdict-description">{{ verdictDescription }}</div>
        </div>

        <a-alert v-if="pollingPaused && isPending" type="warning" show-icon>
          自动刷新已暂停，请点击“刷新结果”继续查询。
        </a-alert>
        <a-alert
          v-if="submission.judgeInfo?.detail"
          :type="verdictTone === 'success' ? 'success' : 'warning'"
          show-icon
        >
          {{ submission.judgeInfo.detail }}
        </a-alert>

        <a-descriptions
          class="submission-info"
          title="运行信息"
          bordered
          :column="{ xs: 1, sm: 2, md: 3 }"
        >
          <a-descriptions-item label="提交 ID">
            {{ submission.id }}
          </a-descriptions-item>
          <a-descriptions-item label="题目 ID">
            {{ submission.questionId }}
          </a-descriptions-item>
          <a-descriptions-item label="语言">
            {{ languageLabel }}
          </a-descriptions-item>
          <a-descriptions-item label="通过用例">
            {{ passedCases }}
          </a-descriptions-item>
          <a-descriptions-item label="运行时间">
            {{ formatTime(submission.judgeInfo?.time) }}
          </a-descriptions-item>
          <a-descriptions-item label="内存占用">
            {{ formatMemory(submission.judgeInfo?.memory) }}
          </a-descriptions-item>
          <a-descriptions-item label="提交时间">
            {{ formatDate(submission.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="更新时间">
            {{ formatDate(submission.updateTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="自动刷新">
            {{ isPolling ? "每 1.5 秒刷新" : "已停止" }}
          </a-descriptions-item>
        </a-descriptions>

        <a-progress
          v-if="totalCaseCount > 0"
          :percent="caseProgress"
          :status="verdictTone === 'danger' ? 'danger' : 'normal'"
          :show-text="true"
          class="case-progress"
        />

        <a-card title="提交代码" class="code-card" :bordered="true">
          <template #extra>
            <a-button size="small" @click="copyCode">复制代码</a-button>
          </template>
          <pre
            class="code-content"
          ><code>{{ submission.code || "暂无代码" }}</code></pre>
        </a-card>
      </template>

      <a-result v-else status="error" title="无法加载提交结果">
        <template #subtitle>{{ loadError }}</template>
        <template #extra>
          <a-button type="primary" @click="refreshManually">重新加载</a-button>
        </template>
      </a-result>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import {
  computed,
  defineProps,
  onBeforeUnmount,
  onMounted,
  ref,
  withDefaults,
} from "vue";
import { QuestionSubmitControllerService, QuestionSubmitVO } from "@/generated";
import message from "@arco-design/web-vue/es/message";
import moment from "moment";
import { useRouter } from "vue-router";

interface Props {
  id: string;
}

const props = withDefaults(defineProps<Props>(), {
  id: () => "",
});

const router = useRouter();
const submission = ref<QuestionSubmitVO>();
const loading = ref(false);
const loadError = ref("");
const isPolling = ref(false);
const pollingPaused = ref(false);
let pollTimer: number | undefined;
let pollCount = 0;
const MAX_POLL_COUNT = 120;
const POLL_INTERVAL_MS = 1500;

const numericId = computed(() => Number(props.id));
const isPending = computed(
  () => submission.value?.status === 0 || submission.value?.status === 1
);

const statusMeta = computed(() => {
  switch (submission.value?.status) {
    case 0:
      return { label: "等待判题", color: "orange" };
    case 1:
      return { label: "判题中", color: "blue" };
    case 2:
      return { label: "判题完成", color: "green" };
    case 3:
      return { label: "系统失败", color: "red" };
    default:
      return { label: "加载中", color: "gray" };
  }
});

const verdictText = computed(() => {
  if (submission.value?.status === 0) return "Waiting";
  if (submission.value?.status === 1) return "Judging";
  if (submission.value?.status === 3) return "System Error";
  return submission.value?.judgeInfo?.message || "Finished";
});

const verdictTone = computed(() => {
  if (verdictText.value === "Accepted") return "success";
  if (isPending.value) return "pending";
  return "danger";
});

const verdictDescription = computed(() => {
  if (submission.value?.status === 0)
    return "提交已进入队列，正在等待可用判题线程。";
  if (submission.value?.status === 1) return "代码正在沙箱中编译和运行。";
  if (submission.value?.status === 3)
    return "判题基础设施执行失败，请查看详情或稍后重新提交。";
  if (verdictText.value === "Accepted") return "所有测试用例均已通过。";
  return "判题已经完成，请根据结果检查代码。";
});

const totalCaseCount = computed(
  () => submission.value?.judgeInfo?.totalCaseCount || 0
);
const passedCaseCount = computed(
  () => submission.value?.judgeInfo?.passedCaseCount || 0
);
const passedCases = computed(() =>
  totalCaseCount.value > 0
    ? `${passedCaseCount.value} / ${totalCaseCount.value}`
    : "暂无"
);
const caseProgress = computed(() =>
  totalCaseCount.value > 0 ? passedCaseCount.value / totalCaseCount.value : 0
);
const languageLabel = computed(() =>
  submission.value?.language === "java"
    ? "Java"
    : submission.value?.language || "未知"
);

const stopPolling = () => {
  if (pollTimer !== undefined) {
    window.clearTimeout(pollTimer);
    pollTimer = undefined;
  }
  isPolling.value = false;
};

const scheduleNextPoll = () => {
  stopPolling();
  if (!isPending.value) return;
  if (pollCount >= MAX_POLL_COUNT) {
    pollingPaused.value = true;
    return;
  }
  isPolling.value = true;
  pollTimer = window.setTimeout(() => loadSubmission(true), POLL_INTERVAL_MS);
};

const loadSubmission = async (silent = false) => {
  if (!Number.isSafeInteger(numericId.value) || numericId.value <= 0) {
    loadError.value = "提交 ID 不合法";
    stopPolling();
    return;
  }
  if (loading.value) return;

  loading.value = true;
  if (!silent) loadError.value = "";
  try {
    const res =
      await QuestionSubmitControllerService.getQuestionSubmitByIdUsingGet(
        numericId.value
      );
    if (res.code === 0 && res.data) {
      submission.value = res.data;
      loadError.value = "";
      if (isPending.value) {
        pollCount += 1;
        scheduleNextPoll();
      } else {
        stopPolling();
      }
    } else {
      loadError.value = res.message || "提交不存在或无权查看";
      stopPolling();
    }
  } catch (error) {
    loadError.value = "请求失败，请确认后端服务和登录状态";
    if (submission.value && isPending.value) {
      pollCount += 1;
      scheduleNextPoll();
    } else {
      stopPolling();
    }
  } finally {
    loading.value = false;
  }
};

const refreshManually = () => {
  pollCount = 0;
  pollingPaused.value = false;
  loadSubmission(false);
};

const backToQuestion = () => {
  if (submission.value?.questionId) {
    router.push(`/view/question/${submission.value.questionId}`);
  }
};

const copyCode = async () => {
  const code = submission.value?.code || "";
  try {
    await navigator.clipboard.writeText(code);
    message.success("代码已复制");
  } catch (error) {
    message.error("复制失败，请手动选择代码");
  }
};

const formatTime = (time?: number) => (time == null ? "暂无" : `${time} ms`);
const formatMemory = (memory?: number) =>
  memory == null ? "暂无" : `${memory} KB`;
const formatDate = (date?: string) =>
  date ? moment(date).format("YYYY-MM-DD HH:mm:ss") : "暂无";

onMounted(() => loadSubmission(false));
onBeforeUnmount(stopPolling);
</script>

<style scoped>
#questionSubmitDetailView {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
}

.detail-card {
  min-height: 520px;
}

.verdict-panel {
  margin-bottom: 20px;
  padding: 24px;
  border-radius: 8px;
  border-left: 5px solid;
  background: var(--color-fill-1);
}

.verdict-title {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.3;
}

.verdict-description {
  margin-top: 8px;
  color: var(--color-text-2);
}

.verdict-success {
  border-color: rgb(var(--green-6));
}

.verdict-success .verdict-title {
  color: rgb(var(--green-6));
}

.verdict-pending {
  border-color: rgb(var(--arcoblue-6));
}

.verdict-pending .verdict-title {
  color: rgb(var(--arcoblue-6));
}

.verdict-danger {
  border-color: rgb(var(--red-6));
}

.verdict-danger .verdict-title {
  color: rgb(var(--red-6));
}

.submission-info,
.case-progress,
.code-card {
  margin-top: 20px;
}

.code-content {
  max-height: 520px;
  margin: 0;
  padding: 16px;
  overflow: auto;
  border-radius: 6px;
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: Consolas, "Courier New", monospace;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>

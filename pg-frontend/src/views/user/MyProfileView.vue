<template>
  <div id="myProfileView">
    <a-spin :loading="loading" style="width: 100%">
      <div class="profile-layout">
        <aside class="profile-sidebar">
          <div class="avatar-shell">
            <img
              v-if="loginUser.userAvatar && !avatarFailed"
              :src="avatarSrc"
              alt="用户头像"
              @error="avatarFailed = true"
            />
            <span v-else>{{ initials }}</span>
          </div>
          <input
            ref="avatarInput"
            class="hidden-file-input"
            type="file"
            accept="image/jpeg,image/png"
            @change="uploadAvatar"
          />
          <a-button
            class="avatar-button"
            long
            :loading="avatarUploading"
            @click="avatarInput?.click()"
            >修改头像</a-button
          >
          <h1>{{ displayName }}</h1>
          <p class="bio">
            {{ loginUser.userProfile || "这个人还没有填写自我介绍。" }}
          </p>
          <a-space direction="vertical" fill>
            <a-button long @click="openEditor">编辑个人资料</a-button>
            <a-button long @click="openEmailEditor">{{
              loginUser.userEmail ? "更换验证邮箱" : "绑定验证邮箱"
            }}</a-button>
          </a-space>
          <div class="profile-meta">
            <div>● Cookie OJ 用户</div>
            <div v-if="loginUser.userEmail">✉ {{ maskedEmail }}</div>
            <div v-else>✉ 尚未绑定找回邮箱</div>
            <div v-if="loginUser.createTime">
              ◷ {{ formatDate(loginUser.createTime) }} 加入
            </div>
          </div>
        </aside>

        <main class="profile-content">
          <section class="welcome-card">
            <div>
              <span class="eyebrow">MY OJ PROFILE</span>
              <h2>{{ displayName }} 的做题主页</h2>
              <p>记录每一次提交，也看见自己一点点变强。</p>
            </div>
            <a-button type="primary" @click="router.push('/')"
              >继续做题</a-button
            >
          </section>

          <section class="stats-grid">
            <div class="stat-card">
              <span>已解决题目</span
              ><strong>{{ numberValue(overview.solvedQuestions) }}</strong
              ><small>通过的不同题目</small>
            </div>
            <div class="stat-card">
              <span>尝试题目</span
              ><strong>{{ numberValue(overview.attemptedQuestions) }}</strong
              ><small>提交过的不同题目</small>
            </div>
            <div class="stat-card">
              <span>累计提交</span
              ><strong>{{ numberValue(overview.totalSubmissions) }}</strong
              ><small>所有判题记录</small>
            </div>
            <div class="stat-card">
              <span>提交通过率</span><strong>{{ acceptanceRate }}</strong
              ><small
                >{{
                  numberValue(overview.acceptedSubmissions)
                }}
                次提交通过</small
              >
            </div>
          </section>

          <section class="panel">
            <div class="panel-heading">
              <div>
                <h2>最近 12 周做题活动</h2>
                <p>{{ recentSubmissionCount }} 次提交记录</p>
              </div>
              <div class="legend">
                <span>少</span
                ><i
                  v-for="level in [0, 1, 2, 3, 4]"
                  :key="level"
                  :class="'level-' + level"
                /><span>多</span>
              </div>
            </div>
            <div class="activity-scroll">
              <div class="activity-grid" aria-label="最近 12 周做题活动">
                <span
                  v-for="item in activity"
                  :key="item.day"
                  :class="'level-' + item.level"
                  :title="`${item.day}：${item.count} 次提交`"
                />
              </div>
            </div>
          </section>

          <section class="panel submissions-panel">
            <div class="panel-heading">
              <div>
                <h2>最近提交</h2>
                <p>点击记录可以查看代码与完整判题结果。</p>
              </div>
              <a-button :loading="submissionsLoading" @click="loadSubmissions"
                >刷新</a-button
              >
            </div>
            <a-alert v-if="loadError" type="error" class="load-error">{{
              loadError
            }}</a-alert>
            <a-table
              :columns="columns"
              :data="submissions"
              row-key="id"
              :loading="submissionsLoading"
              :scroll="{ x: 760 }"
              :pagination="{ current: page, pageSize, total, showTotal: true }"
              @page-change="changePage"
            >
              <template #question="{ record }">
                <button class="question-link" @click="goQuestion(record)">
                  {{ record.questionVO?.title || `题目 #${record.questionId}` }}
                </button>
                <div class="cell-note">#{{ record.questionId }}</div>
              </template>
              <template #verdict="{ record }">
                <a-tag :color="submissionVerdict(record).color">
                  {{ submissionVerdict(record).text }}
                </a-tag>
              </template>
              <template #resource="{ record }">
                <span>{{ record.judgeInfo?.time ?? 0 }} ms</span>
                <div class="cell-note">
                  {{ memoryLabel(record.judgeInfo?.memory) }}
                </div>
              </template>
              <template #created="{ record }">{{
                formatDateTime(record.createTime)
              }}</template>
              <template #action="{ record }">
                <a-button
                  type="text"
                  @click="router.push('/submission/' + record.id)"
                  >查看详情</a-button
                >
              </template>
              <template #empty>
                <div class="empty-state">
                  <h3>还没有提交记录</h3>
                  <p>从一道感兴趣的题目开始吧。</p>
                  <a-button type="outline" @click="router.push('/')"
                    >浏览题目</a-button
                  >
                </div>
              </template>
            </a-table>
          </section>
        </main>
      </div>
    </a-spin>

    <a-modal
      v-model:visible="editorVisible"
      title="编辑个人资料"
      :ok-loading="saving"
      ok-text="保存资料"
      cancel-text="取消"
      :on-before-ok="saveProfile"
    >
      <a-form :model="profileForm" layout="vertical">
        <a-form-item label="唯一用户名" required>
          <a-input
            v-model="profileForm.userName"
            :disabled="!canChangeUserName"
            :max-length="32"
            placeholder="1 到 32 位文字、数字、下划线或短横线"
          />
          <template #extra>{{ userNameChangeHint }}</template>
        </a-form-item>
        <a-form-item label="自我介绍">
          <a-textarea
            v-model="profileForm.userProfile"
            :max-length="512"
            show-word-limit
            :auto-size="{ minRows: 4, maxRows: 8 }"
            placeholder="介绍一下自己、正在学习的方向或做题目标…"
          />
        </a-form-item>
      </a-form>
    </a-modal>
    <a-modal
      v-model:visible="emailEditorVisible"
      :title="loginUser.userEmail ? '更换验证邮箱' : '绑定验证邮箱'"
      :ok-loading="emailSaving"
      ok-text="确认绑定"
      cancel-text="取消"
      :on-before-ok="bindEmail"
    >
      <a-alert type="info" class="email-notice">
        验证邮箱仅用于账号找回，不会展示给其他用户。
      </a-alert>
      <a-form :model="emailForm" layout="vertical">
        <a-form-item label="邮箱" required>
          <a-input v-model="emailForm.email" placeholder="请输入邮箱地址" />
        </a-form-item>
        <a-form-item label="邮箱验证码" required>
          <a-input
            v-model="emailForm.code"
            :max-length="6"
            placeholder="6 位验证码"
          >
            <template #append>
              <a-button :loading="emailSending" @click="sendBindCode"
                >发送验证码</a-button
              >
            </template>
          </a-input>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import axios from "axios";
import { useRouter } from "vue-router";
import { useStore } from "vuex";
import message from "@arco-design/web-vue/es/message";
import {
  QuestionSubmitControllerService,
  QuestionSubmitVO,
  UserControllerService,
  UserSubmissionOverviewVO,
} from "@/generated";
import { formatDate } from "@/utils/date";
import { buildActivity, submissionVerdict } from "@/utils/profile";

const router = useRouter();
const store = useStore();
const loginUser = computed(() => store.state.user.loginUser);
const displayName = computed(() => loginUser.value.userName || "用户");
const initials = computed(() => displayName.value.slice(0, 2).toUpperCase());
const avatarFailed = ref(false);
const avatarVersion = ref(Date.now());
const avatarSrc = computed(() => {
  const url = loginUser.value.userAvatar || "";
  if (!url) return "";
  return `${url}${url.includes("?") ? "&" : "?"}v=${avatarVersion.value}`;
});
watch(
  () => loginUser.value.userAvatar,
  () => {
    avatarFailed.value = false;
    avatarVersion.value = Date.now();
  }
);
const avatarInput = ref<HTMLInputElement>();
const avatarUploading = ref(false);
const overview = ref<UserSubmissionOverviewVO>({});
const loading = ref(true);
const submissionsLoading = ref(false);
const loadError = ref("");
const submissions = ref<QuestionSubmitVO[]>([]);
const page = ref(1);
const pageSize = 10;
const total = ref(0);
const editorVisible = ref(false);
const saving = ref(false);
const emailEditorVisible = ref(false);
const emailSending = ref(false);
const emailSaving = ref(false);
const emailForm = ref({ email: "", code: "" });
const maskedEmail = computed(() => {
  const email = loginUser.value.userEmail ?? "";
  const [name, domain] = email.split("@");
  if (!domain) return email;
  return `${name.slice(0, 2)}***@${domain}`;
});
const profileForm = ref({ userName: "", userProfile: "" });
const nextUserNameChange = computed(() => {
  const changedAt = loginUser.value.userNameUpdateTime;
  if (!changedAt) return undefined;
  const date = new Date(changedAt);
  if (Number.isNaN(date.getTime())) return undefined;
  date.setDate(date.getDate() + 30);
  return date;
});
const canChangeUserName = computed(
  () =>
    !nextUserNameChange.value ||
    nextUserNameChange.value.getTime() <= Date.now()
);
const userNameChangeHint = computed(() =>
  canChangeUserName.value
    ? "用户名全站唯一，修改成功后 30 天内不能再次修改。"
    : `下次可修改时间：${nextUserNameChange.value?.toLocaleString("zh-CN")}`
);
const numberValue = (value?: number | string) => {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
};
const activity = computed(() => buildActivity(overview.value.activity));
const recentSubmissionCount = computed(() =>
  (overview.value.activity ?? []).reduce(
    (sum, item) => sum + numberValue(item.submissionCount),
    0
  )
);
const acceptanceRate = computed(() => {
  const count = numberValue(overview.value.totalSubmissions);
  const accepted = numberValue(overview.value.acceptedSubmissions);
  return count ? `${((accepted / count) * 100).toFixed(1)}%` : "0.0%";
});
const memoryLabel = (kb?: number) =>
  kb ? `${Number((kb / 1024).toFixed(1))} MB` : "0 MB";
const formatDateTime = (value?: string) => {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : new Intl.DateTimeFormat("zh-CN", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      }).format(date);
};
const loadOverview = async () => {
  const res =
    await QuestionSubmitControllerService.getMySubmissionOverviewUsingGet();
  if (res.code !== 0) throw Error(res.message || "统计数据加载失败");
  overview.value = res.data ?? {};
};
const loadSubmissions = async () => {
  submissionsLoading.value = true;
  loadError.value = "";
  try {
    const res =
      await QuestionSubmitControllerService.listMyQuestionSubmissionsUsingPost({
        current: page.value,
        pageSize,
      });
    if (res.code !== 0) throw Error(res.message || "提交记录加载失败");
    submissions.value = res.data?.records ?? [];
    total.value = res.data?.total ?? 0;
  } catch (error) {
    loadError.value =
      error instanceof Error ? error.message : "提交记录加载失败";
  } finally {
    submissionsLoading.value = false;
  }
};
const loadPage = async () => {
  loading.value = true;
  try {
    await Promise.all([loadOverview(), loadSubmissions()]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "个人主页加载失败");
  } finally {
    loading.value = false;
  }
};
const changePage = (value: number) => {
  page.value = value;
  loadSubmissions();
};
const goQuestion = (submission: QuestionSubmitVO) =>
  router.push(`/view/question/${submission.questionId}`);
const openEditor = () => {
  profileForm.value = {
    userName: displayName.value,
    userProfile: loginUser.value.userProfile ?? "",
  };
  editorVisible.value = true;
};
const openEmailEditor = () => {
  emailForm.value = { email: loginUser.value.userEmail ?? "", code: "" };
  emailEditorVisible.value = true;
};
const sendBindCode = async () => {
  if (!/^\S+@\S+\.\S+$/.test(emailForm.value.email.trim())) {
    message.warning("请输入正确的邮箱地址");
    return;
  }
  emailSending.value = true;
  try {
    const res = await UserControllerService.sendEmailCodeUsingPost({
      purpose: "BIND",
      email: emailForm.value.email.trim(),
    });
    if (res.code !== 0) throw Error(res.message || "发送失败");
    message.success("验证码已发送，10 分钟内有效");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "发送失败");
  } finally {
    emailSending.value = false;
  }
};
const bindEmail = async () => {
  if (!/^\d{6}$/.test(emailForm.value.code)) {
    message.warning("请输入邮件中的 6 位验证码");
    return false;
  }
  emailSaving.value = true;
  try {
    const res = await UserControllerService.bindEmailUsingPost({
      email: emailForm.value.email.trim(),
      code: emailForm.value.code,
    });
    if (res.code !== 0) throw Error(res.message || "绑定失败");
    await store.dispatch("user/getLoginUser");
    message.success("验证邮箱已更新");
    return true;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "绑定失败");
    return false;
  } finally {
    emailSaving.value = false;
  }
};
const saveProfile = async () => {
  const userName = profileForm.value.userName.trim();
  if (!/^[\p{L}\p{N}_-]{1,32}$/u.test(userName)) {
    message.warning("用户名需为 1 到 32 位文字、数字、下划线或短横线");
    return false;
  }
  saving.value = true;
  try {
    const res = await UserControllerService.updateMyUserUsingPost({
      userName,
      userProfile: profileForm.value.userProfile.trim(),
    });
    if (res.code !== 0) {
      message.error(res.message || "保存失败");
      return false;
    }
    await store.dispatch("user/getLoginUser");
    message.success("个人资料已保存");
    return true;
  } catch {
    message.error("保存失败，请稍后重试");
    return false;
  } finally {
    saving.value = false;
  }
};
const uploadAvatar = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file) return;
  if (!["image/jpeg", "image/png"].includes(file.type)) {
    message.warning("头像仅支持 JPG、JPEG、PNG");
    return;
  }
  if (file.size > 2 * 1024 * 1024) {
    message.warning("头像不能超过 2MB");
    return;
  }
  avatarUploading.value = true;
  try {
    const body = new FormData();
    body.append("file", file);
    const response = await axios.post("/api/user/avatar", body);
    if (response.data?.code !== 0) {
      throw new Error(response.data?.message || "上传失败");
    }
    const avatarUrl = response.data.data as string;
    store.commit("user/updateUser", {
      ...loginUser.value,
      userAvatar: avatarUrl,
    });
    avatarVersion.value = Date.now();
    avatarFailed.value = false;
    message.success("头像已更新");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "头像上传失败");
  } finally {
    avatarUploading.value = false;
  }
};
const columns = [
  { title: "题目", slotName: "question", width: 230 },
  { title: "结果", slotName: "verdict", width: 120 },
  { title: "语言", dataIndex: "language", width: 90 },
  { title: "运行资源", slotName: "resource", width: 120 },
  { title: "提交时间", slotName: "created", width: 130 },
  { title: "", slotName: "action", width: 100 },
];
onMounted(loadPage);
</script>

<style scoped>
#myProfileView {
  width: 100%;
}
.profile-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 32px;
  align-items: start;
}
.profile-sidebar {
  position: sticky;
  top: 88px;
}
.avatar-shell {
  width: 240px;
  height: 240px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(145deg, #e8f3ff, #bedaff);
  border: 1px solid var(--color-border-2);
  color: #165dff;
  font-size: 64px;
  font-weight: 700;
}
.avatar-shell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.hidden-file-input {
  display: none;
}
.avatar-button {
  margin-top: 12px;
}
.profile-sidebar h1 {
  margin: 20px 0 2px;
  font-size: 25px;
  overflow-wrap: anywhere;
}
.bio {
  min-height: 48px;
  margin: 20px 0;
  color: var(--color-text-2);
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.profile-meta {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--color-border-2);
  color: var(--color-text-3);
  font-size: 13px;
  line-height: 2;
}
.profile-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.welcome-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 24px;
  border: 1px solid var(--color-border-2);
  border-radius: 10px;
  background: var(--color-bg-2);
}
.eyebrow {
  color: #165dff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.5px;
}
.welcome-card h2 {
  margin: 7px 0 5px;
  font-size: 20px;
}
.welcome-card p,
.panel-heading p {
  margin: 0;
  color: var(--color-text-3);
  font-size: 13px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.stat-card {
  padding: 18px;
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  background: var(--color-bg-2);
}
.stat-card span,
.stat-card small {
  display: block;
  color: var(--color-text-3);
  font-size: 12px;
}
.stat-card strong {
  display: block;
  margin: 10px 0 6px;
  font-size: 26px;
}
.panel {
  padding: 20px;
  border: 1px solid var(--color-border-2);
  border-radius: 10px;
  background: var(--color-bg-2);
}
.panel-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}
.panel-heading h2 {
  margin: 0 0 5px;
  font-size: 16px;
}
.legend {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text-3);
  font-size: 11px;
}
.legend i,
.activity-grid span {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}
.activity-scroll {
  overflow-x: auto;
  padding-bottom: 3px;
}
.activity-grid {
  display: grid;
  grid-template-rows: repeat(7, 12px);
  grid-auto-flow: column;
  grid-auto-columns: 12px;
  gap: 4px;
  width: max-content;
}
.level-0 {
  background: #ebedf0;
}
.level-1 {
  background: #9be9a8;
}
.level-2 {
  background: #40c463;
}
.level-3 {
  background: #30a14e;
}
.level-4 {
  background: #216e39;
}
.question-link {
  max-width: 100%;
  padding: 0;
  border: 0;
  background: none;
  color: #0969da;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.question-link:hover {
  text-decoration: underline;
}
.cell-note {
  margin-top: 4px;
  color: var(--color-text-3);
  font-size: 12px;
}
.username-notice {
  margin-bottom: 18px;
}
.load-error {
  margin-bottom: 16px;
}
.email-notice {
  margin-bottom: 18px;
}
.empty-state {
  padding: 28px;
  text-align: center;
}
.empty-state h3 {
  margin: 0 0 6px;
}
.empty-state p {
  margin: 0 0 16px;
  color: var(--color-text-3);
}
@media (max-width: 900px) {
  .profile-layout {
    grid-template-columns: 200px minmax(0, 1fr);
    gap: 20px;
  }
  .avatar-shell {
    width: 180px;
    height: 180px;
  }
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 650px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
  .profile-sidebar {
    position: static;
    display: grid;
    grid-template-columns: 84px 1fr;
    column-gap: 16px;
  }
  .avatar-shell {
    grid-row: span 1;
    width: 84px;
    height: 84px;
    font-size: 28px;
  }
  .profile-sidebar h1 {
    margin: 5px 0 0;
    font-size: 21px;
  }
  .bio,
  .profile-sidebar > button,
  .profile-meta {
    grid-column: 1 / -1;
  }
  .welcome-card {
    align-items: flex-start;
  }
  .stats-grid {
    gap: 8px;
  }
  .stat-card {
    padding: 14px 12px;
  }
  .panel {
    padding: 15px;
  }
  .submissions-panel {
    padding: 12px;
  }
}
</style>

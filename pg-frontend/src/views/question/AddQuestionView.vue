<template>
  <div id="addQuestionView">
    <header class="editor-heading">
      <div>
        <a-button
          type="text"
          class="back-link"
          @click="router.push('/manage/question/')"
          >← 返回题目管理</a-button
        >
        <h1>
          {{ updatePage ? "编辑题目" : "新建题目"
          }}<span v-if="updatePage && route.query.id"
            >#{{ route.query.id }}</span
          >
        </h1>
        <p>
          先写清题面，再配置资源限制和评测数据。公开样例与隐藏用例请分别维护。
        </p>
      </div>
      <a-space>
        <a-button
          v-if="updatePage"
          status="danger"
          :loading="deleting"
          :disabled="loading || loadFailed || saving"
          @click="confirmDeleteQuestion"
          >删除这道题</a-button
        >
        <a-button
          :disabled="loading || loadFailed || deleting"
          @click="previewVisible = true"
          >预览题面</a-button
        >
      </a-space>
    </header>
    <a-alert v-if="loadFailed" type="error" class="notice"
      >题目加载失败，为避免覆盖原数据，已禁用保存。<a-button
        type="text"
        @click="loadData"
        >重新加载</a-button
      ></a-alert
    >
    <a-spin :loading="loading" style="width: 100%">
      <a-card :bordered="false" class="editor-card">
        <a-tabs v-model:active-key="activeTab">
          <a-tab-pane key="statement" title="01 题目资料">
            <a-form :model="form" layout="vertical">
              <div class="basic-grid">
                <a-form-item label="题目标题" field="title" required
                  ><a-input
                    v-model="form.title"
                    :max-length="80"
                    show-word-limit
                    placeholder="例如：A + B、两数之和"
                /></a-form-item>
                <a-form-item label="题目标签" field="tags"
                  ><a-input-tag
                    v-model="form.tags"
                    placeholder="输入标签后按 Enter，如：入门、数组"
                    allow-clear
                /></a-form-item>
                <a-form-item label="题目难度" field="difficulty" required
                  ><a-select v-model="form.difficulty">
                    <a-option value="EASY">简单</a-option>
                    <a-option value="MEDIUM">中等</a-option>
                    <a-option value="HARD">困难</a-option>
                  </a-select></a-form-item
                >
                <a-form-item label="发布状态" field="status" required
                  ><a-select v-model="form.status">
                    <a-option value="DRAFT">草稿（用户不可见）</a-option>
                    <a-option value="PUBLISHED">已发布（用户可做题）</a-option>
                  </a-select>
                  <template #extra
                    >新题默认保存为草稿，确认题面和用例后再发布。</template
                  >
                </a-form-item>
              </div>
              <div class="section-heading">
                <div>
                  <h2>题目描述 <span class="required">*</span></h2>
                  <p>
                    支持
                    Markdown，请包含输入格式、输出格式、数据范围与公开样例。
                  </p>
                </div>
                <a-button size="small" @click="insertStatementTemplate"
                  >插入题面模板</a-button
                >
              </div>
              <MdEditor
                :value="form.content"
                :handle-change="onContentChange"
              />
              <div class="section-footer">
                <span>{{ form.content.length }} / 8192 字符</span
                ><a-button type="outline" @click="activeTab = 'judge'"
                  >下一步：判题与用例 →</a-button
                >
              </div>
            </a-form>
          </a-tab-pane>
          <a-tab-pane
            key="judge"
            :title="'02 判题与用例 · ' + form.judgeCase.length"
          >
            <div class="section-heading">
              <div>
                <h2>运行资源限制</h2>
                <p>
                  每次执行的最大资源用量。内存以 MB 填写，保存时自动换算为 KB。
                </p>
              </div>
            </div>
            <a-form :model="form" layout="vertical" class="limits-grid">
              <a-form-item label="时间限制（ms）" required
                ><a-input-number
                  v-model="form.judgeConfig.timeLimit"
                  :min="100"
                  :max="10000"
                  :precision="0"
                  :step="100"
                  mode="button"
                /><template #extra
                  >100–10000 ms，默认 1000 ms</template
                ></a-form-item
              >
              <a-form-item label="内存限制（MB）" required
                ><a-input-number
                  v-model="memoryMb"
                  :min="16"
                  :max="512"
                  :precision="0"
                  :step="16"
                  mode="button"
                /><template #extra
                  >16–512 MB，Java 建议预留足够内存</template
                ></a-form-item
              >
              <a-form-item label="堆栈限制（KB）" required
                ><a-input-number
                  v-model="form.judgeConfig.stackLimit"
                  :min="256"
                  :max="65536"
                  :precision="0"
                  :step="256"
                  mode="button"
                /><template #extra
                  >256–65536 KB，递归题需留意栈空间</template
                ></a-form-item
              >
            </a-form>
            <div class="section-heading">
              <div>
                <h2>
                  判题用例
                  <a-tag color="arcoblue"
                    >{{ form.judgeCase.length }} / 50</a-tag
                  >
                </h2>
                <p>
                  每个用例是一组标准输入和期望输出，独立运行。请覆盖普通情况与边界情况。
                </p>
              </div>
              <a-button
                type="outline"
                :disabled="form.judgeCase.length >= 50"
                @click="handleAdd"
                >＋ 添加用例</a-button
              >
            </div>
            <a-alert type="info" class="notice"
              >直接填写文本，不需要输入
              JSON。换行和空格会保留；空输入、空输出也是合法用例。</a-alert
            >
            <section
              v-for="(item, index) in form.judgeCase"
              :key="caseKeys[index]"
              class="case-card"
            >
              <div class="case-heading">
                <h3>
                  <span>{{ String(index + 1).padStart(2, "0") }}</span> 测试用例
                  {{ index + 1 }}
                </h3>
                <a-space
                  ><a-button
                    type="text"
                    size="small"
                    :disabled="form.judgeCase.length >= 50"
                    @click="duplicateCase(index)"
                    >复制</a-button
                  ><a-button
                    type="text"
                    size="small"
                    status="danger"
                    :disabled="form.judgeCase.length <= 1"
                    @click="handleDelete(index)"
                    >删除</a-button
                  ></a-space
                >
              </div>
              <div class="io-grid">
                <div>
                  <label :for="'case-input-' + caseKeys[index]">标准输入</label
                  ><a-textarea
                    :id="'case-input-' + caseKeys[index]"
                    v-model="item.input"
                    :auto-size="{ minRows: 4, maxRows: 12 }"
                    placeholder="例如：1 2"
                  />
                </div>
                <div>
                  <label :for="'case-output-' + caseKeys[index]">期望输出</label
                  ><a-textarea
                    :id="'case-output-' + caseKeys[index]"
                    v-model="item.output"
                    :auto-size="{ minRows: 4, maxRows: 12 }"
                    placeholder="例如：3"
                  />
                </div>
              </div>
            </section>
            <a-button
              long
              type="dashed"
              :disabled="form.judgeCase.length >= 50"
              @click="handleAdd"
              >＋ 添加一个测试用例</a-button
            >
          </a-tab-pane>
          <a-tab-pane key="answer" title="03 参考题解">
            <div class="section-heading">
              <div>
                <h2>参考题解 <span class="optional">选填</span></h2>
                <p>记录解题思路、复杂度和参考代码，便于管理员后续维护。</p>
              </div>
            </div>
            <MdEditor :value="form.answer" :handle-change="onAnswerChange" />
            <div class="section-footer">
              <span>{{ form.answer.length }} / 8192 字符</span>
            </div>
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </a-spin>
    <footer class="save-bar">
      <div>
        <strong>{{ form.title.trim() || "未命名题目" }}</strong
        ><span
          >{{ isDirty ? "有未保存的修改" : "暂无未保存修改" }} ·
          {{ form.judgeCase.length }} 个测试用例</span
        >
      </div>
      <a-space
        ><a-button :disabled="saving" @click="router.push('/manage/question/')"
          >取消</a-button
        ><a-button
          type="primary"
          :loading="saving"
          :disabled="loading || loadFailed"
          @click="doSubmit"
          >{{ saveButtonText }}</a-button
        ></a-space
      >
    </footer>
    <a-modal
      v-model:visible="previewVisible"
      title="题面预览 · 学生视角"
      :width="960"
      :footer="false"
      modal-class="question-preview-modal"
      ><div class="preview-body">
        <h1>{{ form.title || "未命名题目" }}</h1>
        <a-space wrap
          ><a-tag :color="difficultyMeta(form.difficulty).color">{{
            difficultyMeta(form.difficulty).label
          }}</a-tag
          ><a-tag :color="form.status === 'PUBLISHED' ? 'green' : 'gray'">{{
            form.status === "PUBLISHED" ? "已发布" : "草稿"
          }}</a-tag
          ><a-tag v-for="tag in form.tags" :key="tag">{{ tag }}</a-tag
          ><span class="preview-limits"
            >{{ form.judgeConfig.timeLimit }} ms / {{ memoryMb }} MB</span
          ></a-space
        ><MdViewer
          v-if="previewVisible"
          :value="form.content || '请先填写题目描述'"
        /></div
    ></a-modal>
  </div>
</template>

<script setup lang="ts">
import {
  computed,
  defineAsyncComponent,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import MdEditor from "@/components/MdEditor.vue";
import { QuestionControllerService } from "@/generated/services/QuestionControllerService";
import message from "@arco-design/web-vue/es/message";
import Modal from "@arco-design/web-vue/es/modal";
import {
  onBeforeRouteLeave,
  onBeforeRouteUpdate,
  useRoute,
  useRouter,
} from "vue-router";
import axios from "axios";
import type { Question } from "@/generated";
import {
  AdminCase,
  DEFAULT_JUDGE_CONFIG,
  judgeDataError,
  parseCases,
  parseConfig,
  parseTags,
} from "@/utils/questionAdmin";

const MdViewer = defineAsyncComponent(
  () => import("@/components/MdViewer.vue")
);
const route = useRoute();
const router = useRouter();
const updatePage = computed(() => route.path.includes("update"));
const saving = ref(false);
const deleting = ref(false);
const loading = ref(false);
const loadFailed = ref(false);
const activeTab = ref("statement");
const previewVisible = ref(false);
const initialForm = () => ({
  title: "",
  difficulty: "MEDIUM",
  status: "DRAFT",
  tags: [] as string[],
  answer: "",
  content: "",
  judgeConfig: { ...DEFAULT_JUDGE_CONFIG },
  judgeCase: [{ input: "", output: "" }] as AdminCase[],
});
const form = ref(initialForm());
const baseline = ref(JSON.stringify(form.value));
const isDirty = computed(() => JSON.stringify(form.value) !== baseline.value);
const saveButtonText = computed(() => {
  const action = form.value.status === "PUBLISHED" ? "发布" : "保存草稿";
  return updatePage.value && action === "发布" ? "保存并发布" : action;
});
const difficultyMeta = (difficulty?: string) =>
  difficulty === "EASY"
    ? { label: "简单", color: "green" }
    : difficulty === "HARD"
    ? { label: "困难", color: "red" }
    : { label: "中等", color: "orange" };
const memoryMb = computed({
  get: () => form.value.judgeConfig.memoryLimit / 1024,
  set: (value: number) => {
    form.value.judgeConfig.memoryLimit = Math.round(value * 1024);
  },
});
let nextCaseKey = 0;
const caseKeys = ref<number[]>([nextCaseKey++]);
let loadVersion = 0;

const loadData = async () => {
  const version = ++loadVersion;
  activeTab.value = "statement";
  loadFailed.value = false;
  form.value = initialForm();
  baseline.value = JSON.stringify(form.value);
  caseKeys.value = [nextCaseKey++];
  if (!updatePage.value) {
    loading.value = false;
    return;
  }
  const id = String(route.query.id ?? "");
  if (!/^[1-9]\d*$/.test(id)) {
    loadFailed.value = true;
    message.error("缺少有效题目 ID");
    return;
  }
  loading.value = true;
  try {
    const response = await axios.get("/api/question/get/admin", {
      params: { id },
    });
    if (version !== loadVersion) return;
    const res = response.data as {
      code: number;
      message: string;
      data: Question;
    };
    if (res.code !== 0 || !res.data) throw Error(res.message || "题目不存在");
    const data = res.data;
    const cases = parseCases(data.judgeCase);
    const config = parseConfig(data.judgeConfig);
    form.value = {
      title: data.title ?? "",
      difficulty: data.difficulty ?? "MEDIUM",
      status: data.status ?? "DRAFT",
      tags: parseTags(data.tags),
      answer: data.answer ?? "",
      content: data.content ?? "",
      judgeConfig: config ?? { ...DEFAULT_JUDGE_CONFIG },
      judgeCase: cases?.length ? cases : [{ input: "", output: "" }],
    };
    caseKeys.value = form.value.judgeCase.map(() => nextCaseKey++);
    baseline.value = JSON.stringify(form.value);
    if (!cases || !config)
      message.warning(
        "原题目的判题数据格式有误，请在“判题与用例”中重新检查配置"
      );
  } catch (error) {
    if (version === loadVersion) {
      loadFailed.value = true;
      message.error(
        error instanceof Error ? error.message : "加载题目失败，请检查服务"
      );
    }
  } finally {
    if (version === loadVersion) loading.value = false;
  }
};
watch(() => [route.path, route.query.id], loadData, { immediate: true });

const doSubmit = async () => {
  if (saving.value || loading.value || loadFailed.value) return;
  if (!form.value.title.trim() || !form.value.content.trim()) {
    activeTab.value = "statement";
    message.warning("请填写题目标题与题目描述");
    return;
  }
  if (
    form.value.title.length > 80 ||
    form.value.content.length > 8192 ||
    form.value.answer.length > 8192
  ) {
    message.warning("标题最多 80 字符，题面和题解各最多 8192 字符");
    return;
  }
  const issue = judgeDataError(form.value.judgeCase, form.value.judgeConfig);
  if (issue) {
    activeTab.value = "judge";
    message.warning(issue);
    return;
  }
  saving.value = true;
  try {
    const payload = {
      ...form.value,
      title: form.value.title.trim(),
      tags: [
        ...new Set(form.value.tags.map((tag) => tag.trim()).filter(Boolean)),
      ],
    };
    const res = updatePage.value
      ? await QuestionControllerService.updateQuestionUsingPost({
          ...payload,
          id: Number(route.query.id),
        })
      : await QuestionControllerService.addQuestionUsingPost(payload);
    if (res.code !== 0) {
      message.error("保存失败，" + (res.message || "请稍后重试"));
      return;
    }
    baseline.value = JSON.stringify(form.value);
    message.success(updatePage.value ? "题目修改已保存" : "题目已创建");
    saving.value = false;
    await router.push("/manage/question/");
  } catch {
    message.error("保存失败，请检查服务后重试，当前输入仍保留");
  } finally {
    saving.value = false;
  }
};
const confirmDeleteQuestion = () => {
  const id = Number(route.query.id);
  if (!updatePage.value || !Number.isSafeInteger(id) || id <= 0) return;
  Modal.confirm({
    title: "确定删除这道题？",
    content: `即将删除 #${id}「${
      form.value.title || "未命名题目"
    }」。删除后用户无法继续查看或提交，请确认不是只需要保存修改。`,
    okText: "确认删除",
    cancelText: "取消",
    okButtonProps: { status: "danger" },
    onOk: async () => {
      if (deleting.value) return false;
      deleting.value = true;
      try {
        const res = await QuestionControllerService.deleteQuestionUsingPost({
          id,
        });
        if (res.code !== 0) {
          message.error(res.message || "删除失败");
          return false;
        }
        baseline.value = JSON.stringify(form.value);
        message.success("题目已删除");
        await router.replace("/manage/question/");
        return true;
      } catch {
        message.error("删除失败，请检查服务后重试");
        return false;
      } finally {
        deleting.value = false;
      }
    },
  });
};
const handleAdd = () => {
  if (form.value.judgeCase.length >= 50) return;
  form.value.judgeCase.push({ input: "", output: "" });
  caseKeys.value.push(nextCaseKey++);
};
const duplicateCase = (index: number) => {
  if (form.value.judgeCase.length >= 50) return;
  form.value.judgeCase.splice(index + 1, 0, { ...form.value.judgeCase[index] });
  caseKeys.value.splice(index + 1, 0, nextCaseKey++);
};
const handleDelete = (index: number) => {
  if (form.value.judgeCase.length <= 1) return;
  const remove = () => {
    form.value.judgeCase.splice(index, 1);
    caseKeys.value.splice(index, 1);
  };
  if (!form.value.judgeCase[index].input && !form.value.judgeCase[index].output)
    remove();
  else
    Modal.confirm({
      title: "删除这个测试用例？",
      content: `将删除用例 ${index + 1} 的输入和输出。保存题目后才会生效。`,
      onOk: remove,
    });
};
const onContentChange = (value: string) => {
  form.value.content = value;
};
const onAnswerChange = (value: string) => {
  form.value.answer = value;
};
const statementTemplate =
  "## 题目描述\n\n请描述需要解决的问题。\n\n## 输入格式\n\n请描述输入数据。\n\n## 输出格式\n\n请描述输出要求。\n\n## 数据范围\n\n请填写数据范围和边界条件。\n\n## 输入样例\n\n```text\n\n```\n\n## 输出样例\n\n```text\n\n```\n\n## 样例说明\n\n请解释样例。\n";
const insertStatementTemplate = () => {
  const insert = () => {
    form.value.content = statementTemplate;
  };
  if (!form.value.content.trim()) insert();
  else
    Modal.confirm({
      title: "覆盖当前题面？",
      content: "插入模板将覆盖已输入的题目描述，请先备份需要保留的内容。",
      onOk: insert,
    });
};
const confirmLeave = () => {
  if (saving.value) return false;
  if (!isDirty.value) return true;
  return new Promise<boolean>((resolve) =>
    Modal.confirm({
      title: "离开编辑页面？",
      content: "还有未保存的修改，离开后这些修改将丢失。",
      okText: "放弃修改并离开",
      cancelText: "继续编辑",
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  );
};
onBeforeRouteLeave(confirmLeave);
onBeforeRouteUpdate(confirmLeave);
const beforeUnload = (event: BeforeUnloadEvent) => {
  if (isDirty.value) {
    event.preventDefault();
    event.returnValue = "";
  }
};
onMounted(() => window.addEventListener("beforeunload", beforeUnload));
onBeforeUnmount(() => {
  loadVersion++;
  window.removeEventListener("beforeunload", beforeUnload);
});
</script>

<style scoped>
#addQuestionView {
  width: 100%;
  padding-bottom: 24px;
  color: var(--color-text-1);
}
.editor-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}
.back-link {
  padding-left: 0;
}
h1 {
  font-size: 28px;
  margin: 8px 0;
}
h1 span {
  margin-left: 12px;
  font-size: 16px;
  color: var(--color-text-3);
  font-weight: 400;
}
.editor-heading p {
  color: var(--color-text-3);
  margin: 0;
  line-height: 1.7;
}
.editor-card {
  border-radius: 12px;
}
.editor-card :deep(.arco-tabs-content) {
  padding-top: 24px;
}
.basic-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin: 0 0 20px;
}
.section-heading h2 {
  font-size: 17px;
  margin: 0 0 8px;
}
.section-heading p {
  font-size: 13px;
  color: var(--color-text-3);
  margin: 0;
  line-height: 1.6;
}
.required {
  color: #f53f3f;
}
.optional {
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-3);
}
.limits-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  padding: 20px;
  margin-bottom: 24px;
  background: var(--color-fill-1);
  border-radius: 8px;
}
.limits-grid :deep(.arco-input-number) {
  width: 100%;
}
.limits-grid :deep(.arco-form-item) {
  margin-bottom: 0;
}
.notice {
  margin-bottom: 20px;
}
.case-card {
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}
.case-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.case-heading h3 {
  margin: 0;
  font-size: 14px;
}
.case-heading h3 span {
  color: #165dff;
  margin-right: 8px;
}
.io-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.io-grid > div {
  min-width: 0;
}
.io-grid label {
  display: block;
  font-size: 13px;
  margin-bottom: 10px;
}
.io-grid :deep(textarea) {
  font-family: Consolas, monospace;
  font-size: 13px;
  tab-size: 4;
}
.section-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--color-text-3);
  font-size: 12px;
  margin-top: 16px;
}
.save-bar {
  position: sticky;
  bottom: 16px;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 20px;
  padding: 18px 24px;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.07);
}
.save-bar strong {
  display: block;
  font-size: 14px;
  max-width: 400px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.save-bar span {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-text-3);
}
.preview-body {
  max-height: 65vh;
  overflow-y: auto;
}
.preview-limits {
  color: var(--color-text-3);
  font-size: 12px;
}
.preview-body :deep(.markdown-body) {
  margin-top: 24px;
  overflow-wrap: anywhere;
}
:deep(.bytemd) {
  width: 100%;
  height: 480px;
}
:deep(.bytemd-toolbar) {
  z-index: 1;
}
@media (max-width: 760px) {
  .editor-heading {
    align-items: flex-start;
    flex-direction: column;
  }
  .basic-grid,
  .limits-grid,
  .io-grid {
    grid-template-columns: 1fr;
  }
  .limits-grid {
    gap: 18px;
  }
  .section-heading {
    align-items: flex-start;
  }
  .save-bar {
    padding: 14px;
  }
  .save-bar strong {
    max-width: 170px;
  }
  .case-card {
    padding: 14px;
  }
}
@media (max-width: 480px) {
  .section-heading {
    flex-direction: column;
  }
  .save-bar {
    align-items: flex-start;
    flex-direction: column;
  }
  .save-bar strong {
    max-width: 100%;
  }
}
</style>
<style>
.question-preview-modal {
  max-width: calc(100vw - 32px);
}
</style>

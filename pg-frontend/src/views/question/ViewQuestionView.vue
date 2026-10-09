<template>
  <div id="viewQuestionView">
    <a-row :gutter="[24, 24]">
      <a-col :md="12" :xs="24">
        <a-tabs v-model:active-key="pageTab" @change="changePageTab">
          <a-tab-pane key="question" title="题目">
            <a-card v-if="question" :title="question.title">
              <MdViewer :value="question.content || ''" />
              <div class="judge-limit-tags" aria-label="判题限制">
                <span>判题限制</span>
                <a-tag size="small">
                  时间 {{ question.judgeConfig?.timeLimit ?? 0 }} ms
                </a-tag>
                <a-tag size="small">
                  内存
                  {{ resourceLimit(question.judgeConfig?.memoryLimit) }}
                </a-tag>
                <a-tag size="small">
                  堆栈 {{ resourceLimit(question.judgeConfig?.stackLimit) }}
                </a-tag>
              </div>
              <template #extra>
                <a-space wrap>
                  <div v-if="question.userVO" class="question-author">
                    <img
                      v-if="question.userVO.userAvatar"
                      :src="question.userVO.userAvatar"
                      alt="出题人头像"
                    />
                    <span v-else>{{
                      (question.userVO.userName || "用户").slice(0, 1)
                    }}</span>
                    <small>{{ question.userVO.userName || "用户" }}</small>
                  </div>
                  <a-tag :color="difficultyMeta(question.difficulty).color">{{
                    difficultyMeta(question.difficulty).label
                  }}</a-tag>
                  <a-tag
                    v-for="(tag, index) of question.tags"
                    :key="index"
                    color="green"
                    >{{ tag }}
                  </a-tag>
                  <a-button
                    size="small"
                    type="outline"
                    @click="openQuestionLists"
                  >
                    加入题单
                  </a-button>
                </a-space>
              </template>
            </a-card>
          </a-tab-pane>
          <a-tab-pane key="solutions" title="题解">
            <a-spin :loading="solutionsLoading" style="width: 100%">
              <a-alert
                v-if="solutionAccess && !solutionAccess.unlocked"
                type="info"
                :show-icon="true"
                class="solution-lock"
              >
                <template #title>题解暂未解锁</template>
                {{ solutionAccess.reason || "通过这道题后即可查看题解" }}
                <template v-if="store.state.user.loginUser?.id" #action>
                  <a-button size="small" @click="confirmRevealSolutions">
                    实在不会，查看题解
                  </a-button>
                </template>
              </a-alert>
              <template v-else-if="solutionAccess?.unlocked">
                <div class="solution-list-heading">
                  <div>
                    <h3>大家的题解</h3>
                    <span>{{ solutionTotal }} 篇</span>
                  </div>
                  <a-button
                    v-if="solutionAccess.canPublish"
                    type="primary"
                    @click="openSolutionEditor"
                  >
                    {{ solutionAccess.mine ? "编辑我的题解" : "发布题解" }}
                  </a-button>
                </div>
                <a-alert
                  v-if="solutionAccess.revealedWithoutAccepted"
                  type="warning"
                  class="solution-revealed-notice"
                >
                  你已选择提前查看题解；通过本题后才能发布自己的题解。
                </a-alert>
                <a-empty v-if="!solutions.length">暂时还没有题解</a-empty>
                <article
                  v-for="solution in solutions"
                  :key="solution.id"
                  class="solution-card"
                >
                  <header>
                    <div class="solution-author">
                      <img
                        v-if="solution.userAvatar"
                        :src="solution.userAvatar"
                        alt="题解作者头像"
                      />
                      <span v-else>{{
                        (solution.userName || "用户").slice(0, 1)
                      }}</span>
                      <div>
                        <strong>{{ solution.title }}</strong>
                        <small>
                          {{ solution.userName || "用户" }} ·
                          {{
                            formatDate(
                              solution.updateTime || solution.createTime
                            )
                          }}
                        </small>
                      </div>
                    </div>
                    <a-space v-if="solution.deletable">
                      <a-button
                        v-if="solution.own"
                        type="text"
                        size="small"
                        @click="openSolutionEditor"
                        >编辑</a-button
                      >
                      <a-button
                        type="text"
                        status="danger"
                        size="small"
                        @click="confirmDeleteSolution(solution)"
                        >删除</a-button
                      >
                    </a-space>
                  </header>
                  <MdViewer :value="solution.content" />
                </article>
                <a-pagination
                  v-if="solutionTotal > solutionPageSize"
                  v-model:current="solutionPage"
                  :page-size="solutionPageSize"
                  :total="solutionTotal"
                  @change="loadSolutions"
                />
              </template>
            </a-spin>
          </a-tab-pane>
        </a-tabs>
      </a-col>
      <a-col :md="12" :xs="24">
        <a-form :model="form" layout="inline">
          <a-form-item
            field="language"
            label="编程语言"
            style="min-width: 240px"
          >
            <a-select
              v-model="form.language"
              :style="{ width: '320px' }"
              placeholder="选择编程语言"
              @change="handleLanguageChange"
            >
              <a-option
                v-for="language in QUESTION_SUBMIT_LANGUAGES"
                :key="language.value"
                :value="language.value"
              >
                {{ language.label }}
              </a-option>
            </a-select>
          </a-form-item>
        </a-form>
        <CodeEditor
          class="code-editor-panel"
          :value="form.code as string"
          :language="form.language"
          :handle-change="changeCode"
        />
        <a-divider :size="0" />
        <a-space class="code-toolbar" wrap>
          <a-button size="small" @click="resetTemplate">恢复默认模板</a-button>
        </a-space>
        <a-collapse class="custom-test-panel">
          <a-collapse-item key="custom" header="自定义测试（不计入提交记录）">
            <a-textarea
              v-model="customInput"
              :auto-size="{ minRows: 4, maxRows: 10 }"
              :max-length="65536"
              placeholder="输入一组标准输入，例如：1 2"
            />
            <a-button
              class="custom-run-button"
              :loading="customTesting"
              :disabled="submitting || customTesting"
              @click="runCustomTest"
              >运行自测</a-button
            >
            <div v-if="customResult" class="custom-result">
              <div class="custom-result-heading">
                <strong>运行结果</strong>
                <a-tag :color="customResult.verdict ? 'orange' : 'green'">{{
                  customResult.verdict || "运行完成"
                }}</a-tag>
                <span>{{ customResult.time ?? 0 }} ms</span>
                <span>{{ customMemory }}</span>
              </div>
              <pre>{{ customResult.output || "（程序没有输出）" }}</pre>
              <p v-if="customResult.message">{{ customResult.message }}</p>
            </div>
          </a-collapse-item>
        </a-collapse>
        <a-button
          type="primary"
          style="min-width: 200px"
          :loading="submitting"
          :disabled="submitting || customTesting"
          @click="doSubmit"
          >提交代码</a-button
        >
      </a-col>
    </a-row>
    <a-modal
      v-model:visible="solutionEditorVisible"
      :title="solutionAccess?.mine ? '编辑我的题解' : '发布题解'"
      :width="900"
      :mask-closable="false"
      :footer="false"
      modal-class="solution-editor-modal"
    >
      <a-input
        v-model="solutionForm.title"
        :max-length="100"
        placeholder="题解标题"
      />
      <div class="solution-editor">
        <MdEditor
          :value="solutionForm.content"
          :handle-change="changeSolutionContent"
        />
      </div>
      <div class="solution-editor-actions">
        <a-button @click="solutionEditorVisible = false">取消</a-button>
        <a-button
          type="primary"
          :loading="solutionSaving"
          @click="saveSolution"
        >
          发布题解
        </a-button>
      </div>
    </a-modal>
    <a-modal
      v-model:visible="questionListVisible"
      title="保存到我的题单"
      :footer="false"
      :width="520"
    >
      <div class="create-list-row">
        <a-input
          v-model="newQuestionListName"
          :max-length="64"
          placeholder="新题单名称"
          @press-enter="createAndAddQuestion"
        />
        <a-button
          type="primary"
          :loading="creatingList"
          @click="createAndAddQuestion"
        >
          新建并加入
        </a-button>
      </div>
      <a-spin :loading="questionListsLoading" style="width: 100%">
        <a-empty v-if="!questionListsLoading && questionLists.length === 0">
          还没有题单，请在上方新建
        </a-empty>
        <div v-else class="question-list-options">
          <div
            v-for="item in questionLists"
            :key="item.id"
            class="question-list-option"
          >
            <div>
              <strong>{{ item.name }}</strong>
              <small>{{ item.questionCount }} 道题</small>
            </div>
            <a-button
              :status="item.containsQuestion ? 'danger' : 'normal'"
              :loading="changingListId === item.id"
              @click="toggleQuestionList(item)"
              >{{ item.containsQuestion ? "移出题单" : "加入" }}</a-button
            >
          </div>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import {
  computed,
  defineAsyncComponent,
  defineComponent,
  h,
  defineProps,
  withDefaults,
  ref,
  watch,
} from "vue";
import {
  QuestionControllerService,
  QuestionSubmitAddRequest,
  QuestionSubmitControllerService,
  QuestionVO,
  CustomTestResultVO,
} from "@/generated";
import message from "@arco-design/web-vue/es/message";
import Modal from "@arco-design/web-vue/es/modal";
import store from "@/store";
import { useCodeDraft } from "@/composables/useCodeDraft";
import { codeTemplate } from "@/utils/codeTemplate";
const CodeEditor = defineAsyncComponent({
  loader: () => import("@/components/CodeEditor.vue"),
  delay: 200,
  loadingComponent: defineComponent({
    setup: () => () =>
      h(
        "div",
        { style: "min-height: 400px; padding-top: 24px" },
        "代码编辑器加载中…"
      ),
  }),
  errorComponent: defineComponent({
    setup: () => () =>
      h(
        "div",
        { role: "alert" },
        "代码编辑器加载失败，请刷新页面重试。本机草稿不会因此删除。"
      ),
  }),
});
import MdViewer from "@/components/MdViewer.vue";
const MdEditor = defineAsyncComponent(
  () => import("@/components/MdEditor.vue")
);
import { formatDate } from "@/utils/date";
import { detectCodeLanguage } from "@/utils/codeLanguage";
import { javaSubmissionError } from "@/utils/javaSubmission";
import { QUESTION_SUBMIT_LANGUAGES } from "@/constants/questionSubmitLanguage";
import { useRouter } from "vue-router";
import {
  addQuestionToList,
  createQuestionList,
  getMyQuestionLists,
  removeQuestionFromList,
  QuestionListItem as UserQuestionList,
} from "@/api/questionList";
import {
  deleteQuestionSolution,
  getQuestionSolutions,
  QuestionSolution,
  revealQuestionSolutions,
  saveQuestionSolution,
  SolutionAccess,
} from "@/api/questionSolution";

interface Props {
  id: string;
}

const props = withDefaults(defineProps<Props>(), {
  id: () => "",
});
const question = ref<QuestionVO>();
const pageTab = ref("question");
const difficultyMeta = (difficulty?: string) =>
  difficulty === "EASY"
    ? { label: "简单", color: "green" }
    : difficulty === "HARD"
    ? { label: "困难", color: "red" }
    : { label: "中等", color: "orange" };
const resourceLimit = (kilobytes?: number) => {
  const value = Number(kilobytes ?? 0);
  if (value >= 1024 && value % 1024 === 0) return `${value / 1024} MB`;
  return `${value} KB`;
};
const submitting = ref(false);
const customTesting = ref(false);
const customInput = ref("");
const customResult = ref<CustomTestResultVO>();
const customMemory = computed(() =>
  customResult.value?.memory
    ? `${Number((customResult.value.memory / 1024).toFixed(1))} MB`
    : "0 MB"
);
const router = useRouter();
const questionListVisible = ref(false);
const questionListsLoading = ref(false);
const creatingList = ref(false);
const changingListId = ref<number>();
const newQuestionListName = ref("");
const questionLists = ref<UserQuestionList[]>([]);
const solutionsLoading = ref(false);
const solutionSaving = ref(false);
const solutionEditorVisible = ref(false);
const solutionAccess = ref<SolutionAccess>();
const solutions = ref<QuestionSolution[]>([]);
const solutionPage = ref(1);
const solutionPageSize = 10;
const solutionTotal = ref(0);
const solutionForm = ref({ title: "", content: "" });
const solutionFormLoaded = ref(false);

const loadSolutions = async (page = solutionPage.value) => {
  if (!question.value?.id) return;
  solutionPage.value = page;
  solutionsLoading.value = true;
  try {
    const res = await getQuestionSolutions(
      question.value.id,
      solutionPage.value,
      solutionPageSize
    );
    if (res.code === 40100) {
      solutionAccess.value = {
        unlocked: false,
        canPublish: false,
        revealedWithoutAccepted: false,
        reason: "请先登录，通过这道题后即可查看和发布题解",
      };
      solutions.value = [];
      solutionTotal.value = 0;
      return;
    }
    if (res.code !== 0 || !res.data)
      throw new Error(res.message || "题解加载失败");
    solutionAccess.value = res.data;
    solutions.value = res.data.page?.records ?? [];
    solutionTotal.value = Number(res.data.page?.total ?? 0);
    // Paging and refreshing the public list must not overwrite an unfinished edit.
    if (!solutionFormLoaded.value) {
      solutionForm.value = res.data.mine
        ? { title: res.data.mine.title, content: res.data.mine.content }
        : { title: "", content: "" };
      solutionFormLoaded.value = true;
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : "题解加载失败");
  } finally {
    solutionsLoading.value = false;
  }
};
const changePageTab = (key: string | number) => {
  if (key === "solutions") loadSolutions();
};
const changeSolutionContent = (value: string) => {
  solutionForm.value.content = value;
};
const openSolutionEditor = () => {
  if (!solutionAccess.value?.canPublish) return;
  solutionEditorVisible.value = true;
};
const confirmRevealSolutions = () => {
  Modal.confirm({
    title: "确认提前查看题解？",
    content: "建议先独立完成。确认后将立即显示本题题解，并记录你的选择。",
    okText: "确认查看",
    cancelText: "继续做题",
    onOk: async () => {
      if (!question.value?.id) return false;
      try {
        const res = await revealQuestionSolutions(question.value.id);
        if (res.code !== 0) throw new Error(res.message || "题解解锁失败");
        await loadSolutions(1);
        return true;
      } catch (error) {
        message.error(error instanceof Error ? error.message : "题解解锁失败");
        return false;
      }
    },
  });
};
const saveSolution = async () => {
  if (!question.value?.id || solutionSaving.value) return;
  const title = solutionForm.value.title.trim();
  const content = solutionForm.value.content.trim();
  if (!title || !content) {
    message.warning("请填写题解标题和正文");
    return;
  }
  solutionSaving.value = true;
  try {
    const res = await saveQuestionSolution({
      questionId: question.value.id,
      title,
      content,
    });
    if (res.code !== 0) throw new Error(res.message || "题解保存失败");
    message.success(
      solutionAccess.value?.mine ? "题解修改成功" : "题解发布成功"
    );
    solutionEditorVisible.value = false;
    await loadSolutions(1);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "题解保存失败");
  } finally {
    solutionSaving.value = false;
  }
};
const confirmDeleteSolution = (solution: QuestionSolution) => {
  Modal.confirm({
    title: "删除题解？",
    content: `即将删除「${solution.title}」，删除后不再向其他用户展示。`,
    okText: "确认删除",
    cancelText: "取消",
    okButtonProps: { status: "danger" },
    onOk: async () => {
      try {
        const res = await deleteQuestionSolution(solution.id);
        if (res.code !== 0) throw new Error(res.message || "删除失败");
        message.success("题解已删除");
        if (solution.own) {
          solutionForm.value = { title: "", content: "" };
          solutionFormLoaded.value = false;
        }
        await loadSolutions(solutionPage.value);
        return true;
      } catch (error) {
        message.error(error instanceof Error ? error.message : "删除失败");
        return false;
      }
    },
  });
};

const loadQuestionLists = async () => {
  if (!question.value?.id) return;
  questionListsLoading.value = true;
  try {
    const res = await getMyQuestionLists(question.value.id);
    if (res.code !== 0) throw new Error(res.message || "题单加载失败");
    questionLists.value = res.data ?? [];
  } catch (error) {
    message.error(error instanceof Error ? error.message : "题单加载失败");
  } finally {
    questionListsLoading.value = false;
  }
};
const openQuestionLists = async () => {
  if (!store.state.user.loginUser.id) {
    Modal.confirm({
      title: "登录后保存题目",
      content: "登录后可以创建自己的题单并保存题目。",
      okText: "去登录",
      onOk: () => router.push("/user/login"),
    });
    return;
  }
  questionListVisible.value = true;
  await loadQuestionLists();
};
const createAndAddQuestion = async () => {
  const name = newQuestionListName.value.trim();
  if (!name || !question.value?.id) {
    message.warning("请输入题单名称");
    return;
  }
  creatingList.value = true;
  try {
    const created = await createQuestionList(name);
    if (created.code !== 0 || !created.data)
      throw new Error(created.message || "创建失败");
    const added = await addQuestionToList(created.data, question.value.id);
    if (added.code !== 0) throw new Error(added.message || "加入失败");
    newQuestionListName.value = "";
    message.success("已新建题单并加入题目");
    await loadQuestionLists();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "操作失败");
  } finally {
    creatingList.value = false;
  }
};
const toggleQuestionList = async (item: UserQuestionList) => {
  if (!question.value?.id) return;
  changingListId.value = item.id;
  try {
    const res = item.containsQuestion
      ? await removeQuestionFromList(item.id, question.value.id)
      : await addQuestionToList(item.id, question.value.id);
    if (res.code !== 0) throw new Error(res.message || "操作失败");
    message.success(item.containsQuestion ? "已移出题单" : "已加入题单");
    await loadQuestionLists();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "操作失败");
  } finally {
    changingListId.value = undefined;
  }
};

const loadData = async () => {
  const id = props.id;
  question.value = undefined;
  pageTab.value = "question";
  solutionAccess.value = undefined;
  solutions.value = [];
  solutionEditorVisible.value = false;
  solutionForm.value = { title: "", content: "" };
  solutionFormLoaded.value = false;
  try {
    const res = await QuestionControllerService.getQuestionVoByIdUsingGet(
      Number(id)
    );
    if (id !== props.id) return;
    if (res.code === 0) question.value = res.data;
    else message.error("加载失败，" + res.message);
  } catch {
    if (id === props.id) message.error("题目加载失败，请稍后重试");
  }
};

const form = ref<QuestionSubmitAddRequest>({
  language: "java",
  code: "",
});
const code = computed({
  get: () => form.value.code ?? "",
  set: (value: string) => {
    form.value.code = value;
  },
});
const { flushDraft } = useCodeDraft(
  () => [
    String(store.state.user.loginUser.id ?? ""),
    props.id,
    form.value.language ?? "java",
  ],
  code
);
const resetTemplate = () => {
  Modal.confirm({
    title: "恢复默认模板？",
    content: "将覆盖这道题当前语言的本机草稿，请先复制需要保留的代码。",
    onOk: () => {
      code.value = codeTemplate(form.value.language ?? "java");
      flushDraft();
    },
  });
};

/**
 * 提交代码
 */
const doSubmit = async () => {
  if (!question.value?.id) {
    return;
  }
  if (!form.value.code?.trim()) {
    message.warning("请先填写代码");
    return;
  }

  if (form.value.language === "java") {
    const error = javaSubmissionError(form.value.code);
    if (error) {
      message.warning(error);
      return;
    }
  }

  submitting.value = true;
  flushDraft();
  try {
    const res = await QuestionSubmitControllerService.doQuestionSubmitUsingPost(
      {
        ...form.value,
        questionId: question.value.id,
      }
    );
    if (res.code === 0 && res.data) {
      message.success("提交成功，正在判题");
      await router.push(`/submission/${res.data}`);
    } else {
      message.error("提交失败，" + (res.message || "请稍后重试"));
    }
  } catch (error) {
    message.error("提交失败，请检查后端服务");
  } finally {
    submitting.value = false;
  }
};

const runCustomTest = async () => {
  if (customTesting.value || !question.value?.id) return;
  if (form.value.language === "java") {
    const error = javaSubmissionError(form.value.code);
    if (error) {
      message.warning(error);
      return;
    }
  }
  customTesting.value = true;
  customResult.value = undefined;
  flushDraft();
  try {
    const res = await QuestionSubmitControllerService.runCustomTestUsingPost({
      questionId: question.value.id,
      language: form.value.language,
      code: form.value.code,
      input: customInput.value,
    });
    if (res.code !== 0) {
      message.error(res.message || "自定义测试失败");
      return;
    }
    customResult.value = res.data;
  } catch {
    message.error("自定义测试失败，请检查判题服务后重试");
  } finally {
    customTesting.value = false;
  }
};

/**
 * 页面加载时，请求数据
 */
watch(() => props.id, loadData, { immediate: true });

const changeCode = (value: string) => {
  form.value.code = value;
};

/**
 * 切换编程语言：对当前已输入的代码做一次粗略判断，
 * 若代码看起来与所选语言不符，给出非阻断的提示（不阻止提交）
 */
const handleLanguageChange = (language: string) => {
  const detected = detectCodeLanguage(form.value.code ?? "");
  if (detected && detected !== language) {
    message.warning(`当前代码疑似 ${detected}，但你选择了 ${language}，请确认`);
  }
};
</script>

<style scoped>
#viewQuestionView {
  width: 100%;
}
.code-toolbar {
  margin-bottom: 16px;
}
.code-editor-panel {
  width: 100%;
  min-height: 520px !important;
}
.judge-limit-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 22px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border-2);
  color: var(--color-text-3);
  font-size: 12px;
}
.judge-limit-tags > span:first-child {
  margin-right: 2px;
}
.solution-lock {
  margin-top: 12px;
}
.solution-list-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.solution-list-heading h3 {
  margin: 0;
}
.solution-list-heading span {
  margin: 4px 0 0;
  color: var(--color-text-3);
  font-size: 12px;
}
.solution-editor {
  margin: 12px 0;
}
.solution-editor :deep(.bytemd) {
  height: min(520px, 58vh);
}
.solution-editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.solution-list-heading {
  margin: 12px 0;
}
.solution-list-heading > div > span {
  display: block;
}
.solution-revealed-notice {
  margin-bottom: 12px;
}
.solution-card {
  margin-bottom: 14px;
  padding: 18px;
  border: 1px solid var(--color-border-2);
  border-radius: 10px;
  background: var(--color-bg-2);
}
.solution-card > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.solution-author {
  display: flex;
  align-items: center;
  gap: 10px;
}
.solution-author img,
.solution-author > span {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: 50%;
  object-fit: cover;
  display: inline-grid;
  place-items: center;
  background: #e8f3ff;
  color: #165dff;
}
.solution-author small {
  display: block;
  margin-top: 3px;
  color: var(--color-text-3);
  font-size: 12px;
}
.custom-test-panel {
  margin-bottom: 16px;
}
.custom-run-button {
  margin-top: 12px;
}
.custom-result {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  background: var(--color-fill-1);
}
.custom-result-heading {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--color-text-3);
  font-size: 12px;
}
.custom-result-heading strong {
  color: var(--color-text-1);
  font-size: 14px;
}
.custom-result pre {
  min-height: 60px;
  margin: 12px 0 0;
  padding: 12px;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: #1f1f1f;
  color: #f2f3f5;
  border-radius: 6px;
  font-family: Consolas, monospace;
}
.custom-result p {
  margin: 10px 0 0;
  color: var(--color-text-3);
  font-size: 12px;
}
.question-author {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.question-author img,
.question-author > span {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  display: inline-grid;
  place-items: center;
  background: #e8f3ff;
  color: #165dff;
}
.question-author small {
  color: var(--color-text-2);
}
.create-list-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  margin-bottom: 18px;
}
.question-list-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.question-list-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
}
.question-list-option small {
  display: block;
  margin-top: 4px;
  color: var(--color-text-3);
}

#viewQuestionView .arco-space-horizontal .arco-space-item {
  margin-bottom: 0 !important;
}
</style>

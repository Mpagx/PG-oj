import { onBeforeUnmount, onMounted, ref, watch, Ref } from "vue";
import { codeTemplate } from "@/utils/codeTemplate";
import { draftKey, readDraft, writeDraft } from "@/utils/codeDraft";

export function useCodeDraft(
  context: () => [string, string, string],
  code: Ref<string>
) {
  const draftStatus = ref("使用默认模板");
  let activeKey = "";
  let dirty = false;
  let restoring = false;
  let timer: ReturnType<typeof setTimeout> | undefined;
  const flushDraft = () => {
    if (timer !== undefined) clearTimeout(timer);
    if (!dirty || !activeKey) return;
    try {
      writeDraft(window.localStorage, activeKey, code.value);
      dirty = false;
      draftStatus.value = "草稿已保存到本机";
    } catch {
      draftStatus.value = "本地保存失败，请复制代码备份";
    }
  };
  watch(
    context,
    ([user, question, language]) => {
      flushDraft();
      activeKey = user && question ? draftKey(user, question, language) : "";
      dirty = false;
      let value = codeTemplate(language);
      draftStatus.value = "使用默认模板";
      if (activeKey) {
        try {
          const draft = readDraft(window.localStorage, activeKey);
          if (draft) {
            value = draft.code;
            draftStatus.value = "已恢复本机草稿";
          }
        } catch {
          draftStatus.value = "本地保存不可用，请复制代码备份";
        }
      }
      restoring = true;
      code.value = value;
      restoring = false;
    },
    { immediate: true, flush: "sync" }
  );
  watch(
    code,
    () => {
      if (!activeKey || restoring) return;
      dirty = true;
      draftStatus.value = "草稿待保存…";
      if (timer !== undefined) clearTimeout(timer);
      timer = setTimeout(flushDraft, 500);
    },
    { flush: "sync" }
  );
  const onVisibilityChange = () => {
    if (document.visibilityState === "hidden") flushDraft();
  };
  onMounted(() => {
    window.addEventListener("pagehide", flushDraft);
    document.addEventListener("visibilitychange", onVisibilityChange);
  });
  onBeforeUnmount(() => {
    flushDraft();
    window.removeEventListener("pagehide", flushDraft);
    document.removeEventListener("visibilitychange", onVisibilityChange);
  });
  return { draftStatus, flushDraft };
}

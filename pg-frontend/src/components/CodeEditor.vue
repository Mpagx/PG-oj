<template>
  <div id="code-editor" ref="codeEditorRef" style="min-height: 400px" />
</template>

<script setup lang="ts">
import * as monaco from "monaco-editor/esm/vs/editor/editor.api";
import {
  onMounted,
  onBeforeUnmount,
  shallowRef,
  ref,
  withDefaults,
  defineProps,
  watch,
} from "vue";

/**
 * 定义组件属性类型
 */
interface Props {
  value: string;
  language?: string;
  handleChange: (v: string) => void;
}

/**
 * 给组件指定初始值
 */
const props = withDefaults(defineProps<Props>(), {
  value: () => "",
  language: () => "java",
  handleChange: () => undefined,
});

const codeEditorRef = ref();
const codeEditor = shallowRef<monaco.editor.IStandaloneCodeEditor>();

onMounted(() => {
  if (!codeEditorRef.value) {
    return;
  }
  // 创建一次编辑器实例，之后切换语言只改模型语言，不重建编辑器
  codeEditor.value = monaco.editor.create(codeEditorRef.value, {
    value: props.value,
    language: props.language,
    automaticLayout: true,
    colorDecorators: true,
    minimap: {
      enabled: true,
    },
    readOnly: false,
    theme: "vs-dark",
  });

  // 编辑 监听内容变化
  codeEditor.value.onDidChangeModelContent(() => {
    props.handleChange(codeEditor.value?.getValue() || "");
  });
});
onBeforeUnmount(() => {
  const model = codeEditor.value?.getModel();
  codeEditor.value?.dispose();
  model?.dispose();
});
watch(
  () => props.value,
  (value) => {
    if (codeEditor.value && codeEditor.value.getValue() !== value)
      codeEditor.value.setValue(value);
  }
);

// 切换编程语言：只切换当前模型的语法高亮语言，保留用户已输入的代码和内容监听
watch(
  () => props.language,
  (language) => {
    const editor = codeEditor.value;
    if (!editor) {
      return;
    }
    const model = editor.getModel();
    if (model) {
      monaco.editor.setModelLanguage(model, language);
    }
  }
);
</script>

<style scoped></style>

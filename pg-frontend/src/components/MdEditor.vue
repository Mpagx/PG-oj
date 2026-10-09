<template>
  <Editor
    :value="value"
    :mode="mode"
    :plugins="plugins"
    @change="handleChange"
  />
</template>

<script setup lang="ts">
import gfm from "@bytemd/plugin-gfm";
import { markdownHighlight } from "@/utils/markdownHighlight";
import "bytemd/dist/index.css";
import { Editor } from "@bytemd/vue-next";
import { withDefaults, defineProps } from "vue";

/**
 * 定义组件属性类型
 */
interface Props {
  value: string;
  mode?: string;
  handleChange: (v: string) => void;
}

const plugins = [
  gfm(),
  markdownHighlight(),
  // Add more plugins here
];

/**
 * 给组件指定初始值
 */
withDefaults(defineProps<Props>(), {
  value: () => "",
  mode: () => "split",
  handleChange: () => undefined,
});
</script>

<style>
.bytemd-toolbar-icon.bytemd-tippy.bytemd-tippy-right:last-child {
  display: none;
}
</style>

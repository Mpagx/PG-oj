<template>
  <Viewer :value="value" :plugins="plugins" />
</template>

<script setup lang="ts">
import gfm from "@bytemd/plugin-gfm";
import { markdownHighlight } from "@/utils/markdownHighlight";
import "bytemd/dist/index.css";
import { Viewer } from "@bytemd/vue-next";
import { withDefaults, defineProps } from "vue";

/**
 * 定义组件属性类型
 */
interface Props {
  value: string;
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
});
</script>

<style></style>

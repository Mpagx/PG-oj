import type { BytemdPlugin } from "bytemd";

// Load just the languages used in OJ problem statements, and only when code exists.
const loadHighlighter = () => import("./syntaxHighlight");
export function markdownHighlight(): BytemdPlugin {
  return {
    viewerEffect({ markdownBody }) {
      let disposed = false;
      const blocks = markdownBody.querySelectorAll<HTMLElement>("pre > code");
      if (blocks.length) {
        loadHighlighter()
          .then(({ highlightBlock }) => {
            if (!disposed) blocks.forEach(highlightBlock);
          })
          .catch(() => {
            /* Keep readable plain text if the chunk cannot load. */
          });
      }
      return () => {
        disposed = true;
      };
    },
  };
}

import hljs from "highlight.js/lib/core";
import java from "highlight.js/lib/languages/java";
import cpp from "highlight.js/lib/languages/cpp";
import python from "highlight.js/lib/languages/python";
import javascript from "highlight.js/lib/languages/javascript";
import json from "highlight.js/lib/languages/json";
import sql from "highlight.js/lib/languages/sql";
import plaintext from "highlight.js/lib/languages/plaintext";

hljs.registerLanguage("java", java);
hljs.registerLanguage("cpp", cpp);
hljs.registerLanguage("python", python);
hljs.registerLanguage("javascript", javascript);
hljs.registerLanguage("json", json);
hljs.registerLanguage("sql", sql);
hljs.registerLanguage("plaintext", plaintext);

export function highlightBlock(element: HTMLElement) {
  const requested = /(?:^|\s)lang(?:uage)?-([\w+-]+)/.exec(
    element.className
  )?.[1];
  const language =
    requested && hljs.getLanguage(requested) ? requested : "plaintext";
  // highlight.js escapes code text; do not interpolate source into HTML directly.
  element.innerHTML = hljs.highlight(element.textContent ?? "", {
    language,
  }).value;
  element.classList.add("hljs");
}

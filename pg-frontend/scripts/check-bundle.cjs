const fs = require("node:fs");
const path = require("node:path");
const zlib = require("node:zlib");
const dist = path.resolve(__dirname, "../dist");
const html = fs.readFileSync(path.join(dist, "index.html"), "utf8");
const assets = new Set(
  [...html.matchAll(/(?:src|href)="([^"]+\.(?:js|css))"/g)].map(
    (match) => match[1]
  )
);
if (!assets.size) throw Error("No initial assets found in built HTML");
let raw = 0;
let gzip = 0;
for (const asset of assets) {
  const file = path.resolve(dist, asset.replace(/^\//, ""));
  if (!file.startsWith(dist + path.sep)) throw Error("Asset outside dist");
  const data = fs.readFileSync(file);
  raw += data.length;
  gzip += zlib.gzipSync(data).length;
}
console.log(
  `Initial JS + CSS: ${(raw / 1024).toFixed(1)} KiB; gzip ${(
    gzip / 1024
  ).toFixed(1)} KiB`
);
// Keep the editor/Markdown chunks off the initial page; fail CI on a regression.
if (raw > 1024 * 1024 || gzip > 300 * 1024) {
  throw Error("Initial resource budget exceeded (1 MiB raw / 300 KiB gzip)");
}

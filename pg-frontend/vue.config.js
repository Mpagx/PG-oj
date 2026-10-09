const MonacoWebpackPlugin = require("monaco-editor-webpack-plugin");

module.exports = {
  // Monaco and the Markdown editor make parallel production compilation very
  // memory hungry on Windows. A single worker is slower but avoids random OOMs.
  parallel: false,
  chainWebpack: (config) => {
    // ESLint/Prettier caches can become stale on Windows when IDEA restores files
    // with their original timestamp. Disable the small lint cache so `serve` and
    // `build` always validate the source currently stored on disk.
    if (config.plugins.has("eslint")) {
      config.plugin("eslint").tap((options) => {
        options[0].cache = false;
        delete options[0].cacheStrategy;
        return options;
      });
    }
  },
  devServer: {
    port: 8082,
    historyApiFallback: true,
    proxy: {
      "/api": {
        // Backend binds IPv4; Node may resolve localhost to IPv6 ::1.
        target: "http://127.0.0.1:8121",
        changeOrigin: true,
      },
    },
    client: {
      // 关闭 webpack-dev-server 在浏览器里拦截 console.warn/error 后弹出的覆盖层
      // errors: true 保留真正的报错弹层，warnings: false 屏蔽烦人的警告弹层
      overlay: {
        errors: true,
        warnings: false,
      },
      // 控制台/终端只显示 error 级别，屏蔽 info/warn 的啰嗦输出
      logging: "error",
    },
  },
  configureWebpack: {
    // Monaco 的 html/css/json/ts 等语言需要 web worker 提供智能提示，
    // 不配置会报 “You must define a function MonacoEnvironment.getWorkerUrl...”
    plugins: [
      new MonacoWebpackPlugin({
        languages: ["java"],
        features: ["bracketMatching", "find", "hover", "suggest", "folding"],
      }),
    ],
  },
};

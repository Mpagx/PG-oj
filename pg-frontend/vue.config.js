const MonacoWebpackPlugin = require("monaco-editor-webpack-plugin");

module.exports = {
  devServer: {
    port: 8080,
    proxy: {
      "/api": {
        target: "http://localhost:8121",
        changeOrigin: true,
      },
    },
  },
  configureWebpack: {
    // Monaco 的 html/css/json/ts 等语言需要 web worker 提供智能提示，
    // 不配置会报 “You must define a function MonacoEnvironment.getWorkerUrl...”
    plugins: [new MonacoWebpackPlugin()],
  },
};

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
};

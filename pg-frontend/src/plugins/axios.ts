// Add a request interceptor
import axios from "axios";

axios.defaults.withCredentials = true;
let csrfRequest: Promise<string> | undefined;
const csrf = () => {
  if (!csrfRequest) {
    csrfRequest = axios
      .get("/api/security/csrf")
      .then((response) => response.data.data as string)
      .finally(() => {
        csrfRequest = undefined;
      });
  }
  return csrfRequest;
};

axios.interceptors.request.use(
  async function (config) {
    if (
      !["get", "head", "options"].includes(
        (config.method || "get").toLowerCase()
      )
    ) {
      const token = await csrf();
      config.headers = config.headers || {};
      config.headers["X-XSRF-TOKEN"] = token;
    }
    // Do something before request is sent
    return config;
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error);
  }
);

// Add a response interceptor
axios.interceptors.response.use(
  function (response) {
    // Any status code that lie within the range of 2xx cause this function to trigger
    // Do something with response data
    return response;
  },
  function (error) {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with response error
    return Promise.reject(error);
  }
);

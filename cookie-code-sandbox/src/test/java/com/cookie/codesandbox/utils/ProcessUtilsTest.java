package com.cookie.codesandbox.utils;

import com.cookie.codesandbox.model.ExecuteMessage;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessUtilsTest {

    @Test
    void terminatesProcessAfterTimeout() throws Exception {
        Process process = javaProcess(SleepProcess.class);

        ExecuteMessage result = ProcessUtils.runProcessAndGetMessage(
                process, "测试进程", 200L, 1024);

        assertNull(result.getExitValue());
        assertTrue(result.getErrorMessage().contains("超时"));
        assertTrue(!process.isAlive());
    }

    @Test
    void truncatesUnboundedProcessOutput() throws Exception {
        Process process = javaProcess(OutputFloodProcess.class);

        ExecuteMessage result = ProcessUtils.runProcessAndGetMessage(
                process, "测试进程", 5000L, 1024);

        assertTrue(result.getErrorMessage().contains("输出超过限制"));
    }

    private Process javaProcess(Class<?> mainClass) throws Exception {
        String executable = System.getProperty("java.home") + File.separator
                + "bin" + File.separator + (isWindows() ? "java.exe" : "java");
        return new ProcessBuilder(
                executable, "-cp", System.getProperty("java.class.path"), mainClass.getName())
                .redirectErrorStream(true)
                .start();
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static class SleepProcess {
        public static void main(String[] args) throws Exception {
            Thread.sleep(30_000L);
        }
    }

    public static class OutputFloodProcess {
        public static void main(String[] args) {
            for (int i = 0; i < 20_000; i++) {
                System.out.print('x');
            }
        }
    }
}

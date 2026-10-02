package com.cookie.codesandbox.security;

import java.security.Permission;

/**
 * 自定义安全管理器
 * 说明：JDK 17 起 SecurityManager 已被标记为废弃（JEP 411），JDK 21 仍可用但会告警，
 * JDK 24 起将彻底移除，届时请改用容器（Docker）沙箱方案。
 */
public class MySecurityManager extends SecurityManager {

    /**
     * 项目根目录。子进程继承沙箱服务的 user.dir，因此用它作为"允许读取"的边界。
     */
    private static final String PROJECT_DIR = System.getProperty("user.dir");

    /**
     * 检查所有权限：此处不做整体拦截，具体由下面的 checkXxx 方法逐个控制。
     */
    @Override
    public void checkPermission(Permission perm) {
        // 不调用 super，避免默认放行策略干扰下面的 checkXxx
        //super.checkPermission(perm);
    }

    /**
     * 检查是否允许执行外部程序（如 Runtime.exec / ProcessBuilder）
     */
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("checkExec 权限异常：" + cmd);
    }

    /**
     * 检查是否允许读文件
     */
    @Override
    public void checkRead(String file) {
        if (file != null && file.contains(PROJECT_DIR) || file.contains("jdk-8")) {
            return;
        }
        throw new SecurityException("checkRead 权限异常：" + file);
    }

    /**
     * 检查是否允许写文件
     */
    @Override
    public void checkWrite(String file) {
        throw new SecurityException("checkWrite 权限异常：" + file);
    }

    /**
     * 检查是否允许删除文件
     */
    @Override
    public void checkDelete(String file) {
        throw new SecurityException("checkDelete 权限异常：" + file);
    }

    /**
     * 检查是否允许连接网络
     */
    @Override
    public void checkConnect(String host, int port) {
        throw new SecurityException("checkConnect 权限异常：" + host + ":" + port);
    }
}

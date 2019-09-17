package org.ifaster.rocketmq.spring.cycle;

/**
 * @author yangnan
 * 生命周期管理
 */
public interface LifeCycle {
    /**
     * 启动
     */
    void start();

    /**
     * 是否启动 true表示启动 false表示未启动
     * @return
     */
    boolean isStart();

    /**
     * 关闭资源
     */
    void shutdown();
}

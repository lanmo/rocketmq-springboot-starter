package org.ifaster.rocketmq.spring.context;

import java.util.Map;

/**
 * @author yangnan
 * mq上下文
 */
public interface Context {

    /**
     * 获取topic
     *
     * @return
     */
    String getTopic();

    /**
     * 获取数据内容
     *
     * @return
     */
    Object getBody();

    /**
     * 获取扩展值
     *
     * @return
     */
    Map<String, Object> getExt();

    /**
     * 添加扩展值
     *
     * @param key
     * @param value
     */
    void addValue(String key, Object value);
}

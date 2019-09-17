package org.ifaster.rocketmq.spring.cycle;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author yangnan
 */
public abstract class AbstractLifeCycle implements LifeCycle, EnvironmentAware {

    /**
     * 从spring配置中读取value值
     *
     * @param env
     * @param key
     * @return
     */
    protected String getValue(ConfigurableEnvironment env, String key) {
        return env.resolvePlaceholders(key);
    }

    /**
     * 判断字符串是否为空
     *
     * @param value
     * @return
     */
    protected boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}

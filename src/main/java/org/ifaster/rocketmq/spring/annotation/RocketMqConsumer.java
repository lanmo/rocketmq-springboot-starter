package org.ifaster.rocketmq.spring.annotation;

import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author yangnan
 * 注解定义该注解的都表示是消费者
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Component
public @interface RocketMqConsumer {

    /**
     * namesrvAddr地址
     * 从spring环境变量中读取 ${namesrvAddr:} :后面为默认值
     * @return
     */
    String namesrvAddr();

    /**
     * topic名字
     * 从spring环境变量中读取 ${topic:} :后面为默认值
     *
     * @return
     */
    String topic();

    /**
     * 消费组名称
     * 从spring环境变量中读取 ${groupName:} :后面为默认值
     * 不能为空
     * @return
     */
    String groupName();

    /**
     * instanceName 从spring中读取配置
     * 为空时用当前时间戳作为instanceName ${instanceName:} :后面为默认值
     * @return
     */
    String instanceName() default "";

    /**
     * batchMaxSize 批量消费最大值
     * @return
     */
    int batchMaxSize() default 16;

    /**
     * 从spring读取配置
     * tag过滤 默认不过滤 ${tag:*}
     * @return
     */
    String tag() default "*";

    /**
     * 是否开启记录跟踪功能 true表示开启
     * @return
     */
    boolean enableMsgTrace() default true;

    /**
     * 是否忽略日志 true表示忽略 false表示不忽略
     * @return
     */
    boolean ignoreLog() default false;

    /**
     * 默认从末尾消费
     * @return
     */
    ConsumeFromWhere from() default ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;

    /**
     * 消费顺序 默认无序
     * @return
     */
    Class<? extends MessageListener> listener() default MessageListenerConcurrently.class;

}

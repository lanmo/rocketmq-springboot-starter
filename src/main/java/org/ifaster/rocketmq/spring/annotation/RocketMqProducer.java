package org.ifaster.rocketmq.spring.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author yangnan
 * 注解定义该注解的都表示是生产者
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Component
public @interface RocketMqProducer {

    /***
     * 默认发送超时时间
     */
    int SEND_MSG_TIMEOUT = 3_1000;
    /**
     * 默认发送失败次数
     */
    int RETRY_TIMES_WHEN_SEND_FAILED = 2;
    /**
     * 默认队列长度
     */
    int TOPIC_QUEUE_NUMS = 4;

    /**
     * namesrvAddr地址 ${namesrvAddr:} :后面为默认值
     * 从spring环境变量中读取
     * @return
     */
    String namesrvAddr();

    /**
     * topic名字 ${topic:} :后面为默认值
     * 从spring环境变量中读取
     *
     * @return
     */
    String topic() default "";

    /**
     * 生产者名称 ${groupName:} :后面为默认值
     * 从spring环境变量中读取
     * 不能为空
     * @return
     */
    String groupName();

    /**
     * instanceName ${instanceName:} :后面为默认值
     * 为空时用当前时间戳作为instanceName
     * @return
     */
    String instanceName() default "";

    /**
     * 是否开启记录跟踪功能 true表示开启
     * @return
     */
    boolean enableMsgTrace() default true;

    /**
     * 追踪日志topic 默认 RMQ_SYS_TRACE_TOPIC
     * ${customizedTraceTopic:} :后面为默认值
     * @return
     */
    String customizedTraceTopic();

    /**
     * 发送超时时间
     * @return
     */
    int sendMsgTimeout() default SEND_MSG_TIMEOUT;

    /**
     * 发送失败重试次数
     *
     * @return
     */
    int retryTimesWhenSendFailed() default RETRY_TIMES_WHEN_SEND_FAILED;

    /**
     * 默认队列长度
     *
     * @return
     */
    int defaultTopicQueueNums() default TOPIC_QUEUE_NUMS;

    /**
     * 是否忽略日志 false表示不忽略日志 true表示忽略日志
     *
     * @return
     */
    boolean ignoreLog() default false;
}

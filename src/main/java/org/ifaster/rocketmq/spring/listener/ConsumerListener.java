package org.ifaster.rocketmq.spring.listener;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.ifaster.rocketmq.spring.context.Context;

/**
 * @author yangnan
 */
public interface ConsumerListener {

    /**
     * 前置处理
     *
     * @param context 上下文
     * @param consumer
     */
    void before(DefaultMQPushConsumer consumer, Context context);

    /**
     * 错误处理
     *
     * @param context 上下文
     * @param consumer
     * @param e 异常栈
     */
    void error(DefaultMQPushConsumer consumer, Context context, Throwable e);

    /**
     * 完成处理
     *
     * @param context 上下文
     * @param orderlyStatus 消费结果
     * @param consumer
     */
    void complete(DefaultMQPushConsumer consumer, Context context, ConsumeOrderlyStatus orderlyStatus);

    /**
     * 完成处理
     *
     * @param context 上下文
     * @param concurrentlyStatus 消费结果
     * @param consumer
     */
    void complete(DefaultMQPushConsumer consumer, Context context, ConsumeConcurrentlyStatus concurrentlyStatus);
}

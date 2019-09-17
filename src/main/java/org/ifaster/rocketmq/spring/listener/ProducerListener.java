package org.ifaster.rocketmq.spring.listener;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.ifaster.rocketmq.spring.context.Context;

/**
 * @author yangnan
 */
public interface ProducerListener {

    /**
     * 前置处理
     *
     * @param context 上下文
     * @param producer
     */
    void before(DefaultMQProducer producer, Context context);

    /**
     * 错误处理
     *
     * @param context 上下文
     * @param producer
     * @param e 异常栈
     */
    void error(DefaultMQProducer producer, Context context, Throwable e);

    /**
     * 完成处理
     *
     * @param context 上下文
     * @param result 发送结果
     * @param producer
     */
    void complete(DefaultMQProducer producer, Context context, SendResult result);
}

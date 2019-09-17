package org.ifaster.rocketmq.spring.consumer;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.ifaster.rocketmq.spring.annotation.RocketMqConsumer;
import org.ifaster.rocketmq.spring.context.ConsumerContext;
import org.ifaster.rocketmq.spring.cycle.AbstractLifeCycle;
import org.ifaster.rocketmq.spring.listener.ConsumerListener;
import org.ifaster.rocketmq.spring.listener.ExtendClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yangnan
 * 抽象rocket-mq消费者
 */
public abstract class AbstractRocketMqConsumer extends AbstractLifeCycle implements EnvironmentAware {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected DefaultMQPushConsumer consumer = null;
    private static AtomicInteger number = new AtomicInteger(0);
    private ConfigurableEnvironment env;
    private boolean ignoreLog;

    private List<ConsumerListener> listeners = null;

    /**
     * 是否开启
     * */
    private volatile boolean start = false;

    @Override
    public void start() {
        synchronized (this.getClass()) {
            init();
            listeners = ExtendClassLoader.getListener(ConsumerListener.class);
        }
    }

    /**
     * 初始化
     */
    private void init() {
        if (start) {
            logger.warn("the consumer [{}] is started.", this);
            return;
        }
        RocketMqConsumer rocketMqConsumer = this.getClass().getAnnotation(RocketMqConsumer.class);
        if (rocketMqConsumer == null) {
            throw new IllegalStateException("RocketMqConsumer annotation is required");
        }
        String namesrvAddr = getValue(env, rocketMqConsumer.namesrvAddr());
        if (isEmpty(namesrvAddr)) {
            throw new IllegalStateException("RocketMqConsumer.namesrvAddr is required");
        }
        String groupName = getValue(env, rocketMqConsumer.groupName());
        if (isEmpty(groupName)) {
            throw new IllegalStateException("RocketMqConsumer.groupName is required");
        }
        String topic = getValue(env, rocketMqConsumer.topic());
        if (isEmpty(topic)) {
            throw new IllegalStateException("RocketMqConsumer.topic is required");
        }

        String instanceName = getValue(env, rocketMqConsumer.instanceName());
        if (isEmpty(instanceName)) {
            instanceName = "consumer-" + number.incrementAndGet() + "-" + System.currentTimeMillis();
        }
        int batchSize = rocketMqConsumer.batchMaxSize();
        if (batchSize <= 0) {
            batchSize = 16;
        }
        String tag = getValue(env, rocketMqConsumer.tag());
        if (isEmpty(tag)) {
            tag = "*";
        }
        String finalTag = tag;
        ignoreLog = rocketMqConsumer.ignoreLog();
        consumer = new DefaultMQPushConsumer(groupName, rocketMqConsumer.enableMsgTrace());
        consumer.setConsumeFromWhere(rocketMqConsumer.from());
        consumer.setInstanceName(instanceName);
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.setConsumeMessageBatchMaxSize(batchSize);
        try {
            consumer.subscribe(topic, finalTag);
            // 注册监听
            if (rocketMqConsumer.listener() == MessageListenerConcurrently.class) {
                consumer.registerMessageListener((MessageListenerConcurrently) (msg, context) -> {
                    ConsumeConcurrentlyStatus status = null;
                    ConsumerContext consumerContext = null;
                    try {
                        if (!CollectionUtils.isEmpty(listeners)) {
                            consumerContext = ConsumerContext.builder().body(msg).topic(topic).ext(getExt()).build();
                            onBefore(consumerContext);
                        }
                        status = handleMessage(msg, context);
                    } catch (Exception e) {
                        if (!ignoreLog) {
                            logger.error("消费失败topic:[{}] tag:[{}] msg:[{}]", topic, finalTag, JSON.toJSONString(msg), e);
                        }
                        onError(consumerContext, e);
                        throw e;
                    } finally {
                        onComplete(consumerContext, status);
                    }
                    return status;
                });
            } else if (rocketMqConsumer.listener() == MessageListenerOrderly.class) {
                consumer.registerMessageListener((MessageListenerOrderly) (msg, context) -> {
                    ConsumerContext consumerContext = null;
                    ConsumeOrderlyStatus status = null;
                    try {
                        if (!CollectionUtils.isEmpty(listeners)) {
                            consumerContext = ConsumerContext.builder().body(msg).topic(topic).ext(getExt()).build();
                            onBefore(consumerContext);
                        }
                        status = handleOrderlyMessage(msg, context);
                    } catch (Exception e) {
                        if (!ignoreLog) {
                            logger.error("消费失败topic:[{}] tag:[{}] msg:[{}]", topic, finalTag, JSON.toJSONString(msg), e);
                        }
                        onError(consumerContext, e);
                       throw e;
                    } finally {
                        onComplete(consumerContext, status);
                    }
                    return status;
                });
            } else {
                throw new UnsupportedOperationException();
            }
            consumer.start();
            start = true;
            logger.info("rocketMq consumer [namesrvAddr={} groupName={} topic={} tag={} instanceName={}] 启动完成",
                    namesrvAddr, groupName, topic, rocketMqConsumer.tag(), instanceName);
        } catch (MQClientException e) {
            logger.warn("rocketMq consumer [namesrvAddr={} groupName={} topic={} tag={} instanceName={}] 启动失败",
                    namesrvAddr, groupName, topic, rocketMqConsumer.tag(), instanceName);
            throw new IllegalStateException("mq consumer init fail", e);
        }

    }

    /**
     * 前置处理
     *
     * @param consumerContext
     */
    protected void onBefore(ConsumerContext consumerContext) {
        if (consumerContext == null || CollectionUtils.isEmpty(listeners)) {
            return;
        }
        listeners.forEach(c -> c.before(consumer, consumerContext));
    }

    /**
     * 完成处理
     *
     * @param orderlyStatus
     * @param consumerContext
     */
    protected void onComplete(ConsumerContext consumerContext, ConsumeOrderlyStatus orderlyStatus) {
        if (consumerContext == null || CollectionUtils.isEmpty(listeners)) {
            return;
        }
        listeners.forEach(c -> c.complete(consumer, consumerContext, orderlyStatus));
    }

    /**
     * 完成处理
     *
     * @param consumerContext
     * @param concurrentlyStatus
     */
    protected void onComplete(ConsumerContext consumerContext, ConsumeConcurrentlyStatus concurrentlyStatus) {
        if (consumerContext == null || CollectionUtils.isEmpty(listeners)) {
            return;
        }
        listeners.forEach(c -> c.complete(consumer, consumerContext, concurrentlyStatus));
    }

    /**
     * 异常处理
     *
     * @param consumerContext
     * @param e
     */
    protected void onError(ConsumerContext consumerContext, Throwable e) {
        if (consumerContext == null || CollectionUtils.isEmpty(listeners)) {
            return;
        }
        listeners.forEach(c -> c.error(consumer, consumerContext, e));
    }

    /**
     * 获取扩展信息
     *
     * @return
     */
    protected final Map<String, Object> getExt() {
        return null;
    }

    /**
     * 顺序消费消息
     *
     * @param msgs
     * @param context
     * @return
     */
    protected ConsumeOrderlyStatus handleOrderlyMessage(final List<MessageExt> msgs, final ConsumeOrderlyContext context) {
        throw new UnsupportedOperationException();
    }

    /**
     * 非顺序消费消息
     *
     * @param msgs
     * @param context
     * @return
     */
    protected ConsumeConcurrentlyStatus handleMessage(final List<MessageExt> msgs, final ConsumeConcurrentlyContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStart() {
        return start;
    }

    @Override
    public void shutdown() {
        if (consumer != null) {
            consumer.shutdown();
            start = false;
            logger.info("the consumer [{}] shutdown OK", this);
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.env = (ConfigurableEnvironment) environment;
    }
}

package org.ifaster.rocketmq.spring.producer;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.ifaster.rocketmq.spring.annotation.RocketMqProducer;
import org.ifaster.rocketmq.spring.context.ProducerContext;
import org.ifaster.rocketmq.spring.cycle.AbstractLifeCycle;
import org.ifaster.rocketmq.spring.listener.ExtendClassLoader;
import org.ifaster.rocketmq.spring.listener.ProducerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.currentTimeMillis;

/**
 * 抽象rocket-mq生产者
 *
 * @author yangnan
 */
public abstract class AbstractRocketMqProducer extends AbstractLifeCycle implements Producer {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected DefaultMQProducer producer = null;
    private static AtomicInteger number = new AtomicInteger(0);
    protected ConfigurableEnvironment env;
    protected String topic;
    private String namesrvAddr;
    private String groupName;
    private List<ProducerListener> listeners = null;

    /**是否忽略日志*/
    private boolean ignoreLog = false;

    /**
     * 是否开启
     * */
    private volatile boolean start = false;

    @Override
    public void start() {
        synchronized (AbstractRocketMqProducer.class) {
            init();
            listeners = ExtendClassLoader.getListener(ProducerListener.class);
        }
    }

    /**
     * 初始化
     */
    private void init() {
        if (start) {
            logger.warn("the producer [{}] is started.", this);
            return;
        }
        RocketMqProducer rocketMqProducer = this.getClass().getAnnotation(RocketMqProducer.class);
        if (rocketMqProducer == null) {
            throw new IllegalStateException("RocketMqProducer annotation is required");
        }
        namesrvAddr = getValue(env, rocketMqProducer.namesrvAddr());
        if (isEmpty(namesrvAddr)) {
            throw new IllegalStateException("RocketMqProducer.namesrvAddr is required");
        }
        groupName = getValue(env, rocketMqProducer.groupName());
        if (isEmpty(groupName)) {
            throw new IllegalStateException("RocketMqProducer.groupName is required");
        }
        this.topic = getValue(env, rocketMqProducer.topic());
        String instanceName = getValue(env, rocketMqProducer.instanceName());
        if (isEmpty(instanceName)) {
            instanceName = "producer-" + number.incrementAndGet() + "-" + currentTimeMillis();
        }
        String customizedTraceTopic = getValue(env, rocketMqProducer.customizedTraceTopic());
        if (isEmpty(customizedTraceTopic)) {
            customizedTraceTopic = null;
        }
        int sendMsgTimeout = rocketMqProducer.sendMsgTimeout();
        if (rocketMqProducer.sendMsgTimeout() <= 0) {
            sendMsgTimeout = RocketMqProducer.SEND_MSG_TIMEOUT;
        }
        int retryTimesWhenSendFailed = rocketMqProducer.retryTimesWhenSendFailed();
        if (retryTimesWhenSendFailed < 0) {
            retryTimesWhenSendFailed = RocketMqProducer.RETRY_TIMES_WHEN_SEND_FAILED;
        }
        int defaultTopicQueueNums = rocketMqProducer.defaultTopicQueueNums();
        if (defaultTopicQueueNums <= 0) {
            defaultTopicQueueNums = RocketMqProducer.TOPIC_QUEUE_NUMS;
        }
        ignoreLog = rocketMqProducer.ignoreLog();
        producer = new DefaultMQProducer(groupName, rocketMqProducer.enableMsgTrace(), customizedTraceTopic);
        producer.setSendMsgTimeout(sendMsgTimeout);
        producer.setInstanceName(instanceName);
        producer.setNamesrvAddr(namesrvAddr);
        producer.setRetryTimesWhenSendFailed(retryTimesWhenSendFailed);
        producer.setDefaultTopicQueueNums(defaultTopicQueueNums);
        try {
            producer.start();
            start = true;
            logger.info("rocketMq producer [namesrvAddr={} groupName={} topic={} instanceName={}] 启动完成",
                    namesrvAddr, groupName, topic, instanceName);
        } catch (Exception e) {
            logger.warn("rocketMq producer [namesrvAddr={} groupName={} topic={} instanceName={}] 启动失败",
                    namesrvAddr, groupName, topic, instanceName);
            throw new IllegalStateException("mq producer init fail", e);
        }
    }


    @Override
    public boolean sendMsg(String topic, String tags, String keys, String body, Map<String, Object> ext) throws Exception {
        checkState(topic, "topic");
        Message message = new Message(topic, tags, keys, body.getBytes(UTF_8));
        ProducerContext context = null;
        SendResult result = null;
        if (!CollectionUtils.isEmpty(listeners)) {
            context = ProducerContext.builder().topic(topic).body(body).keys(keys).ext(ext).tags(tags).build();
            onBefore(context);
        }
        long start = currentTimeMillis();
        try {
            result = producer.send(message);
        } catch (Exception e) {
            if (!ignoreLog) {
                logger.error("发送异常topic:[{}] tags:[{}] keys:[{}] body:[{}] ext:[{}] 耗时:[{}]ms", topic,
                        tags, keys, body, ext, currentTimeMillis() - start, e);
            }
            onError(context, e);
            throw e;
        } finally {
            onComplete(context, result);
        }
        if (!ignoreLog) {
            logger.info("发送完成topic:[{}] tags:[{}] keys:[{}] result:[{}] body:[{}] ext:[{}] 耗时:[{}]ms", topic,
                    tags, keys, result, body, ext, currentTimeMillis() - start);
        }
        if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
            return false;
        }
        return true;
    }

    @Override
    public boolean sendMsg(String topic, String tags, String keys, String body) throws Exception {
        return sendMsg(topic, tags, keys, body, null);
    }

    @Override
    public boolean sendMsg(String tags, String keys, String body) throws Exception {
        return sendMsg(this.topic, tags, keys, body);
    }

    @Override
    public boolean sendMsg(String keys, String body) throws Exception {
        return sendMsg("", keys, body);
    }

    @Override
    public boolean sendMsg(String body) throws Exception {
        return sendMsg("", body);
    }

    @Override
    public boolean sendOrderMsg(String topic, String tags, String keys, String body, Map<String, Object> ext) throws Exception {
        checkState(topic, "topic");
        checkState(keys, "keys");
        ProducerContext context = null;
        Message message = new Message(topic, tags, keys, body.getBytes(UTF_8));
        if (!CollectionUtils.isEmpty(listeners)) {
            context = ProducerContext.builder().topic(topic).body(body).keys(keys).ext(ext).tags(tags).build();
            onBefore(context);
        }
        long start = currentTimeMillis();
        SendResult result = null;
        try {
            result = producer.send(message, (mqs, msg, arg) -> {
                int index = (mqs.size() - 1) & hash(arg);
                return mqs.get(index);
            }, keys);
        } catch (Exception e) {
            if (!ignoreLog) {
                logger.error("发送异常topic:[{}] tags:[{}] keys:[{}] body:[{}] ext:[{}] 耗时:[{}]ms", topic,
                        tags, keys, body, ext, currentTimeMillis() - start, e);
            }
            onError(context, e);
            throw e;
        } finally {
            onComplete(context, result);
        }
        if (!ignoreLog) {
            logger.info("发送完成topic:[{}] tags:[{}] keys:[{}] result:[{}] body:[{}] ext:[{}] 耗时:[{}]ms", topic,
                    tags, keys, result, body, ext, currentTimeMillis() - start);
        }
        if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
            return false;
        }
        return true;
    }

    @Override
    public boolean sendOrderMsg(String topic, String tags, String keys, String body) throws Exception {
        return sendOrderMsg(topic, tags, keys, body, null);
    }

    @Override
    public boolean sendOrderMsg(String tags, String keys, String body) throws Exception {
        return sendOrderMsg(this.topic, tags, keys, body);
    }

    @Override
    public boolean sendOrderMsg(String keys, String body) throws Exception {
        return sendOrderMsg("", keys, body);
    }

    @Override
    public boolean sendOrderMsg(String body) throws Exception {
        return sendOrderMsg("", body);
    }

    protected void checkState(String state, String field) {
        if (!start) {
            throw new IllegalStateException("producer not start");
        }
        if (isEmpty(state)) {
            throw new IllegalStateException(String.format("%s is required", field));
        }
    }

    @Override
    public boolean isStart() {
        return start;
    }

    @Override
    public void shutdown() {
        if (producer != null) {
            producer.shutdown();
            start = false;
            logger.info("the producer [{}] shutdown OK", this);
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.env = (ConfigurableEnvironment) environment;
    }

    private int hash(Object key) {
        int h;
        return key == null ? 0 : (h = key.hashCode()) ^ h >>> 16;
    }

    /**
     * 前置处理
     *
     * @param context
     */
    protected void onBefore(final ProducerContext context) {
        if (context == null || CollectionUtils.isEmpty(listeners)) {
            return;
        }
        listeners.forEach(c -> c.before(producer, context));
    }

    /**
     * 异常处理
     *
     * @param context
     */
    protected void onError(final ProducerContext context, Throwable e) {
        if (context == null || CollectionUtils.isEmpty(listeners)) {
            return;
        }
        listeners.forEach(c -> c.error(producer, context, e));
    }

    /**
     * 完成时调用
     *
     * @param context
     * @param result
     */
    protected void onComplete(ProducerContext context, SendResult result) {
        if (context == null || CollectionUtils.isEmpty(listeners)) {
            return;
        }
        listeners.forEach(c -> c.complete(producer, context, result));
    }
}

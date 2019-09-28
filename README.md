## 1、修改maven仓库地址url
    <repositories>
         <repository>
             <id>nexus-snapshots</id>
             <name>im20 Snapshots Repository</name>
             <url>http://localhost:8081/nexus/content/groups/public</url>
         </repository>
     </repositories>
     <distributionManagement>
         <repository>
             <id>nexus-releases</id>
             <name>Nexus Release Repository</name>
             <url>http://localhost:8081/nexus/content/repositories/releases</url>
         </repository>
         <snapshotRepository>
             <id>nexus-snapshots</id>
             <name>Nexus Snapshots Repository</name>
             <url>http://localhost:8081/nexus/content/repositories/snapshots</url>
         </snapshotRepository>
     </distributionManagement>
## 2、deploy到maven中心仓库
    mvn clean deploy -Dmaven.test.skip=true -U
## 3、在项目中引用jar
    <dependency>
     <groupId>org.ifaster.rocketmq.spring</groupId>
     <artifactId>rocketmq-springboot-starter</artifactId>
     <version>1.0.0</version>
    </dependency>

## 使用案例
    在配置文件application.yml配置
    test:
      mq:
        topic: test
        namesrvAddr: 127.0.0.1:9876
      consumer:
        topic: test
        namesrvAddr: 127.0.0.1:9876
    
    @RocketMqProducer(topic = "${test.mq.topic}", namesrvAddr = "${test.mq.namesrvAddr}",
            groupName = "${test.mq.groupName:testGroup}")
    public class TestMqProducer extends AbstractRocketMqProducer {
    }
    
    @RocketMqConsumer(topic = "${test.consumer.topic}", namesrvAddr = "${test.consumer.namesrvAddr}",
            groupName = "${test.consumer.groupName:testGroup}")
    public class TestMqConsumer extends AbstractRocketMqConsumer {
        @Override
        protected ConsumeOrderlyStatus handleOrderlyMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
            System.out.println("ConsumeOrderlyStatus:" + context);
            return ConsumeOrderlyStatus.SUCCESS;
        }
    
        @Override
        protected ConsumeConcurrentlyStatus handleMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
            System.out.println("ConsumeConcurrentlyStatus:" + context);
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
    }
    
    如果使用listener功能
    需要在resources下面增加文件
    META-INF/services/org.ifaster.rocketmq.spring.listener.ConsumerListener
    META-INF/services/org.ifaster.rocketmq.spring.listener.ProducerListener
    文件内容写具体的实现类
    

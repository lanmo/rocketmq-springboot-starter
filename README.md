#如何使用
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

#使用案例
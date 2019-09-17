package org.ifaster.rocketmq.spring.context;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yangnan
 */
@Builder
@Data
public class ProducerContext implements Context {

    private String topic;
    private String tags;
    private String keys;
    private Object body;
    /**扩展字段*/
    private Map<String, Object> ext;


    @Override
    public void addValue(String key, Object value) {
        if (ext == null) {
            ext = new HashMap<>(16);
        }
        ext.put(key, value);
    }
}

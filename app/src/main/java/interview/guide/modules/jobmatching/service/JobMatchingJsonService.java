package interview.guide.modules.jobmatching.service;

import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * 岗位匹配模块 JSON 工具服务。
 * 统一处理常见的对象与列表序列化/反序列化容错逻辑。
 */
@Service
public class JobMatchingJsonService {

    private final ObjectMapper objectMapper;

    /**
     * 构造 JSON 工具服务。
     *
     * @param objectMapper JSON 处理器
     */
    public JobMatchingJsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将字符串列表序列化为 JSON。
     *
     * @param values 字符串列表
     * @return JSON 字符串
     */
    public String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 将任意对象序列化为 JSON。
     *
     * @param value 任意对象
     * @param <T> 对象类型
     * @return JSON 字符串
     */
    public <T> String toJsonObject(T value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 将 JSON 反序列化为字符串列表。
     *
     * @param json JSON 字符串
     * @return 字符串列表
     */
    public List<String> toStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 将 JSON 反序列化为目标类型对象。
     *
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @param <T> 对象类型
     * @return 反序列化对象
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }
}

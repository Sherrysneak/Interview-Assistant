package interview.guide.modules.jobmatching.service;

import interview.guide.modules.jobmatching.model.LearningResourceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 网关调用服务。
 * 对外暴露技能资源聚合能力，统一封装内部 MCP 工具调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpGatewayService {

    private final ObjectMapper objectMapper;

    @Value("${app.mcp.enabled:true}")
    private boolean enabled;

    @Value("${app.mcp.base-url:http://localhost:8086}")
    private String baseUrl;

    @Value("${app.mcp.internal-token:dev-internal-token}")
    private String internalToken;

    /**
     * 根据技能列表聚合学习资源。
     *
     * @param skills 技能关键词列表
     * @return 聚合后的学习资源列表
     */
    public List<LearningResourceDTO> fetchResources(List<String> skills) {
        if (!enabled || skills == null || skills.isEmpty()) {
            return List.of();
        }

        List<LearningResourceDTO> all = new ArrayList<>();
        for (String skill : skills) {
            all.addAll(callTool("repo-search", Map.of("query", skill + " java interview")));
            all.addAll(callTool("doc-search", Map.of("query", skill + " best practices")));
            all.addAll(callTool("problem-search", Map.of("query", skill + " algorithm")));
            all.addAll(callTool("article-search", Map.of("query", skill + " 实战")));
        }
        return deduplicate(all);
    }

    /**
     * 调用单个 MCP 工具并返回资源结果。
     */
    private List<LearningResourceDTO> callTool(String tool, Map<String, Object> params) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-Internal-Token", internalToken);

            Map<String, Object> body = new HashMap<>();
            body.put("tool", tool);
            body.put("params", params);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(baseUrl + "/tools/invoke", entity, String.class);
            return parseResources(tool, response);
        } catch (RestClientException e) {
            log.warn("MCP 调用失败, tool={}, error={}", tool, e.getMessage());
            return List.of();
        }
    }

    /**
     * 将 MCP 返回的 JSON 转换为统一资源结构。
     */
    private List<LearningResourceDTO> parseResources(String tool, String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return List.of();
            }
            List<LearningResourceDTO> resources = new ArrayList<>();
            for (JsonNode item : data) {
                String title = item.path("title").asText(item.path("name").asText("未命名资源"));
                String url = item.path("url").asText(item.path("html_url").asText(""));
                if (url.isBlank()) {
                    continue;
                }
                resources.add(new LearningResourceDTO(
                    tool,
                    title,
                    url,
                    item.path("platform").asText("external"),
                    item.path("reason").asText("来自 MCP 聚合推荐")
                ));
            }
            return resources;
        } catch (Exception e) {
            log.warn("解析 MCP 响应失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 按 URL 去重，避免同一资源重复推荐。
     */
    private List<LearningResourceDTO> deduplicate(List<LearningResourceDTO> resources) {
        Map<String, LearningResourceDTO> map = new HashMap<>();
        for (LearningResourceDTO resource : resources) {
            map.putIfAbsent(resource.url(), resource);
        }
        return new ArrayList<>(map.values());
    }
}

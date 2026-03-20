package interview.guide.modules.jobmatching.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 从岗位匹配结果生成联动面试请求。
 */
public record GenerateInterviewFromMatchingRequest(
    @Min(value = 3, message = "题目数量最少 3")
    @Max(value = 20, message = "题目数量最多 20")
    Integer questionCount,
    Boolean forceCreate
) {
}

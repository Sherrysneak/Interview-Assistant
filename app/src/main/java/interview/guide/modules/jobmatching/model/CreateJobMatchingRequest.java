package interview.guide.modules.jobmatching.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 创建岗位匹配分析请求。
 */
public record CreateJobMatchingRequest(
    @NotNull(message = "简历 ID 不能为空")
    Long resumeId,
    @NotNull(message = "JD ID 不能为空")
    Long jdId,
    @Min(value = 3, message = "学习周数最少 3 周")
    @Max(value = 6, message = "学习周数最多 6 周")
    Integer durationWeeks,
    Integer questionCountForInterview
) {
}

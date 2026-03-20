package interview.guide.modules.interview.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 创建面试会话请求
 */
public record CreateInterviewRequest(
    String resumeText,      // 简历文本内容
    
    @Min(value = 3, message = "题目数量最少3题")
    @Max(value = 20, message = "题目数量最多20题")
    int questionCount,      // 面试题目数量 (3-20)
    
    Long resumeId,          // 简历ID（可选）
    
    Boolean forceCreate,    // 是否强制创建新会话（忽略未完成的会话），默认为 false

    InterviewJobContext jobContext // 岗位上下文（可选）
) {}

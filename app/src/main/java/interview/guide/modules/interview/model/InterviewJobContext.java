package interview.guide.modules.interview.model;

import java.util.List;

/**
 * 岗位驱动面试上下文。
 */
public record InterviewJobContext(
    Long jdId,
    String jobTitle,
    String jdSummary,
    List<String> requiredSkills,
    List<String> focusSkills
) {
}

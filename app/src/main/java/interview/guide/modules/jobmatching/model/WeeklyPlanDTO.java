package interview.guide.modules.jobmatching.model;

import java.util.List;

/**
 * 周学习计划传输对象。
 */
public record WeeklyPlanDTO(
    Long planId,
    Integer weekNumber,
    String title,
    List<String> objectives,
    List<String> tasks,
    List<String> deliverables,
    List<String> acceptanceCriteria,
    List<LearningResourceDTO> resources,
    Integer completionPercentage,
    Boolean completed
) {
}

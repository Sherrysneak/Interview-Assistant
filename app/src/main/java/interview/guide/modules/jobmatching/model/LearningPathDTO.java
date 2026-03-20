package interview.guide.modules.jobmatching.model;

import java.util.List;

/**
 * 学习路径传输对象。
 */
public record LearningPathDTO(
    Long id,
    String targetGoal,
    Integer durationWeeks,
    Integer estimatedHoursPerWeek,
    Integer progressPercentage,
    LearningPathEntity.LearningPathStatus status,
    List<WeeklyPlanDTO> weeklyPlans
) {
}

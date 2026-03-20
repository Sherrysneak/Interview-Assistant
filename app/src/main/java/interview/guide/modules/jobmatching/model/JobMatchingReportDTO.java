package interview.guide.modules.jobmatching.model;

import interview.guide.common.model.AsyncTaskStatus;

import java.util.List;

/**
 * 岗位匹配报告传输对象。
 */
public record JobMatchingReportDTO(
    Long matchingId,
    Long resumeId,
    Long jdId,
    Integer overallScore,
    Integer skillMatchScore,
    Integer experienceMatchScore,
    Integer projectMatchScore,
    Integer educationMatchScore,
    String matchSummary,
    List<String> evidenceChains,
    List<String> strengths,
    List<String> improvementSuggestions,
    List<SkillGapDTO> skillGaps,
    LearningPathDTO learningPath,
    AsyncTaskStatus status,
    String error
) {
}

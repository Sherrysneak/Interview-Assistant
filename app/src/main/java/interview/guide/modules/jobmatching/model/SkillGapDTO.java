package interview.guide.modules.jobmatching.model;

/**
 * 技能差距传输对象。
 */
public record SkillGapDTO(
    String skillName,
    String requiredLevel,
    String currentLevel,
    SkillGapEntity.GapSeverity severity,
    String jdEvidence,
    String resumeEvidence,
    String actionSuggestion
) {
}

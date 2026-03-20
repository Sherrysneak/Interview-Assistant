package interview.guide.modules.jobmatching.model;

/**
 * 学习资源传输对象。
 */
public record LearningResourceDTO(
    String type,
    String title,
    String url,
    String platform,
    String reason
) {
}

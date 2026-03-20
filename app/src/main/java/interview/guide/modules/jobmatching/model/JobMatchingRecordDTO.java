package interview.guide.modules.jobmatching.model;

import interview.guide.common.model.AsyncTaskStatus;

import java.time.LocalDateTime;

/**
 * JD 维度的匹配记录列表项。
 */
public record JobMatchingRecordDTO(
    Long matchingId,
    Long resumeId,
    String resumeFilename,
    Long jdId,
    String jobTitle,
    Integer overallScore,
    AsyncTaskStatus status,
    String error,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {
}

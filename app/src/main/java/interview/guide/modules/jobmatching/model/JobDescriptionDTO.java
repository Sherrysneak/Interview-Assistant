package interview.guide.modules.jobmatching.model;

import interview.guide.common.model.AsyncTaskStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JD 详情传输对象。
 */
public record JobDescriptionDTO(
    Long id,
    String jobTitle,
    String companyName,
    String salaryRange,
    String workLocation,
    String experienceYears,
    List<String> technicalSkills,
    String coreRequirements,
    String responsibilities,
    String bonusPoints,
    JobDescriptionEntity.SourceType sourceType,
    String sourceUrl,
    AsyncTaskStatus parseStatus,
    String parseError,
    LocalDateTime parseStartedAt,
    LocalDateTime parseFinishedAt,
    Integer parseRetryCount,
    LocalDateTime createdAt
) {
}

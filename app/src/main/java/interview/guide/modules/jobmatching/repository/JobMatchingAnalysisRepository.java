package interview.guide.modules.jobmatching.repository;

import interview.guide.modules.jobmatching.model.JobMatchingAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 岗位匹配分析仓储。
 */
public interface JobMatchingAnalysisRepository extends JpaRepository<JobMatchingAnalysisEntity, Long> {

    /**
     * 查询指定简历的匹配分析历史，按创建时间倒序。
     *
     * @param resumeId 简历 ID
     * @return 匹配分析列表
     */
    List<JobMatchingAnalysisEntity> findByResumeIdOrderByCreatedAtDesc(Long resumeId);

    /**
     * 查询指定 JD 的匹配分析历史，按创建时间倒序。
     *
     * @param jdId JD ID
     * @return 匹配分析列表
     */
    List<JobMatchingAnalysisEntity> findByJobDescriptionIdOrderByCreatedAtDesc(Long jdId);
}

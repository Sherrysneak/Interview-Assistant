package interview.guide.modules.jobmatching.repository;

import interview.guide.modules.jobmatching.model.LearningTaskCompletionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 学习任务完成记录仓储。
 */
public interface LearningTaskCompletionRepository extends JpaRepository<LearningTaskCompletionEntity, Long> {

    /**
     * 查询某周计划下的所有任务完成记录。
     *
     * @param planId 周计划 ID
     * @return 任务完成记录列表
     */
    List<LearningTaskCompletionEntity> findByPlanId(Long planId);

    /**
     * 查询某周计划下指定任务索引的完成记录。
     *
     * @param planId 周计划 ID
     * @param taskIndex 任务索引
     * @return 任务完成记录（可选）
     */
    Optional<LearningTaskCompletionEntity> findByPlanIdAndTaskIndex(Long planId, Integer taskIndex);
}

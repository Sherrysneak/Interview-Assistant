package interview.guide.modules.jobmatching.repository;

import interview.guide.modules.jobmatching.model.WeeklyLearningPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 周学习计划仓储。
 */
public interface WeeklyLearningPlanRepository extends JpaRepository<WeeklyLearningPlanEntity, Long> {

    /**
     * 查询学习路径下的所有周计划。
     *
     * @param pathId 学习路径 ID
     * @return 周计划列表
     */
    List<WeeklyLearningPlanEntity> findByPathIdOrderByWeekNumberAsc(Long pathId);
}

package interview.guide.modules.jobmatching.repository;

import interview.guide.modules.jobmatching.model.LearningPathEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 学习路径仓储。
 */
public interface LearningPathRepository extends JpaRepository<LearningPathEntity, Long> {

    /**
     * 根据匹配分析 ID 查询学习路径。
     *
     * @param matchingId 匹配分析 ID
     * @return 学习路径实体（可选）
     */
    Optional<LearningPathEntity> findByMatchingId(Long matchingId);
}

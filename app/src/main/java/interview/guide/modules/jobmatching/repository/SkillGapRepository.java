package interview.guide.modules.jobmatching.repository;

import interview.guide.modules.jobmatching.model.SkillGapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 技能差距仓储。
 */
public interface SkillGapRepository extends JpaRepository<SkillGapEntity, Long> {

    /**
     * 查询某次匹配分析的技能差距列表。
     *
     * @param matchingId 匹配分析 ID
     * @return 技能差距列表
     */
    List<SkillGapEntity> findByMatchingIdOrderByGapSeverityAscIdAsc(Long matchingId);

    /**
     * 删除某次匹配分析下的技能差距数据。
     *
     * @param matchingId 匹配分析 ID
     */
    void deleteByMatchingId(Long matchingId);
}

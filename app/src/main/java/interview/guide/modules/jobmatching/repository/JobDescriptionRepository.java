package interview.guide.modules.jobmatching.repository;

import interview.guide.modules.jobmatching.model.JobDescriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 岗位描述仓储。
 */
public interface JobDescriptionRepository extends JpaRepository<JobDescriptionEntity, Long> {

    /**
     * 根据 JD 内容哈希查询已存在记录。
     *
     * @param jdHash JD 哈希值
     * @return JD 实体（可选）
     */
    Optional<JobDescriptionEntity> findByJdHash(String jdHash);

    /**
     * 查询所有未软删除的 JD。
     */
    List<JobDescriptionEntity> findByDeletedAtIsNullOrderByCreatedAtDesc();

    /**
     * 根据 ID 查询未软删除的 JD。
     */
    Optional<JobDescriptionEntity> findByIdAndDeletedAtIsNull(Long id);
}

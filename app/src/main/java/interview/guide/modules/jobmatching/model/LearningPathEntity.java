package interview.guide.modules.jobmatching.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 学习路径实体。
 * 表示某次岗位匹配对应的整体学习目标与进度状态。
 */
@Entity
@Table(name = "learning_paths", indexes = {
    @Index(name = "idx_learning_path_matching", columnList = "matching_id"),
    @Index(name = "idx_learning_path_resume", columnList = "resume_id")
})
public class LearningPathEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_id", nullable = false)
    private JobMatchingAnalysisEntity matching;

    @Column(nullable = false)
    private Long resumeId;

    @Column(nullable = false)
    private Long jdId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String targetGoal;

    @Column(nullable = false)
    private Integer durationWeeks;

    @Column(nullable = false)
    private Integer estimatedHoursPerWeek;

    @Column(nullable = false)
    private Integer progressPercentage = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearningPathStatus status = LearningPathStatus.NOT_STARTED;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    /**
     * 学习路径状态。
     */
    public enum LearningPathStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        PAUSED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 获取Id。
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取Matching。
     */
    public JobMatchingAnalysisEntity getMatching() {
        return matching;
    }

    /**
     * 设置Matching。
     */
    public void setMatching(JobMatchingAnalysisEntity matching) {
        this.matching = matching;
    }

    /**
     * 获取ResumeId。
     */
    public Long getResumeId() {
        return resumeId;
    }

    /**
     * 设置ResumeId。
     */
    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    /**
     * 获取JdId。
     */
    public Long getJdId() {
        return jdId;
    }

    /**
     * 设置JdId。
     */
    public void setJdId(Long jdId) {
        this.jdId = jdId;
    }

    /**
     * 获取TargetGoal。
     */
    public String getTargetGoal() {
        return targetGoal;
    }

    /**
     * 设置TargetGoal。
     */
    public void setTargetGoal(String targetGoal) {
        this.targetGoal = targetGoal;
    }

    /**
     * 获取DurationWeeks。
     */
    public Integer getDurationWeeks() {
        return durationWeeks;
    }

    /**
     * 设置DurationWeeks。
     */
    public void setDurationWeeks(Integer durationWeeks) {
        this.durationWeeks = durationWeeks;
    }

    /**
     * 获取EstimatedHoursPerWeek。
     */
    public Integer getEstimatedHoursPerWeek() {
        return estimatedHoursPerWeek;
    }

    /**
     * 设置EstimatedHoursPerWeek。
     */
    public void setEstimatedHoursPerWeek(Integer estimatedHoursPerWeek) {
        this.estimatedHoursPerWeek = estimatedHoursPerWeek;
    }

    /**
     * 获取ProgressPercentage。
     */
    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    /**
     * 设置ProgressPercentage。
     */
    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    /**
     * 获取Status。
     */
    public LearningPathStatus getStatus() {
        return status;
    }

    /**
     * 设置Status。
     */
    public void setStatus(LearningPathStatus status) {
        this.status = status;
    }

    /**
     * 获取CreatedAt。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取StartedAt。
     */
    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    /**
     * 设置StartedAt。
     */
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * 获取CompletedAt。
     */
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    /**
     * 设置CompletedAt。
     */
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}

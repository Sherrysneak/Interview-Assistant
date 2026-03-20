package interview.guide.modules.jobmatching.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 周学习计划实体。
 * 记录学习路径中每周目标、任务、产出和验收标准。
 */
@Entity
@Table(name = "weekly_learning_plans", indexes = {
    @Index(name = "idx_weekly_plan_path", columnList = "path_id")
})
public class WeeklyLearningPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "path_id", nullable = false)
    private LearningPathEntity path;

    @Column(nullable = false)
    private Integer weekNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String objectivesJson;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String tasksJson;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String deliverablesJson;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String acceptanceCriteriaJson;

    @Column(columnDefinition = "TEXT")
    private String resourcesJson;

    @Column(nullable = false)
    private Integer completionPercentage = 0;

    @Column(nullable = false)
    private Boolean completed = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

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
     * 获取Path。
     */
    public LearningPathEntity getPath() {
        return path;
    }

    /**
     * 设置Path。
     */
    public void setPath(LearningPathEntity path) {
        this.path = path;
    }

    /**
     * 获取WeekNumber。
     */
    public Integer getWeekNumber() {
        return weekNumber;
    }

    /**
     * 设置WeekNumber。
     */
    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    /**
     * 获取Title。
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置Title。
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取ObjectivesJson。
     */
    public String getObjectivesJson() {
        return objectivesJson;
    }

    /**
     * 设置ObjectivesJson。
     */
    public void setObjectivesJson(String objectivesJson) {
        this.objectivesJson = objectivesJson;
    }

    /**
     * 获取TasksJson。
     */
    public String getTasksJson() {
        return tasksJson;
    }

    /**
     * 设置TasksJson。
     */
    public void setTasksJson(String tasksJson) {
        this.tasksJson = tasksJson;
    }

    /**
     * 获取DeliverablesJson。
     */
    public String getDeliverablesJson() {
        return deliverablesJson;
    }

    /**
     * 设置DeliverablesJson。
     */
    public void setDeliverablesJson(String deliverablesJson) {
        this.deliverablesJson = deliverablesJson;
    }

    /**
     * 获取AcceptanceCriteriaJson。
     */
    public String getAcceptanceCriteriaJson() {
        return acceptanceCriteriaJson;
    }

    /**
     * 设置AcceptanceCriteriaJson。
     */
    public void setAcceptanceCriteriaJson(String acceptanceCriteriaJson) {
        this.acceptanceCriteriaJson = acceptanceCriteriaJson;
    }

    /**
     * 获取ResourcesJson。
     */
    public String getResourcesJson() {
        return resourcesJson;
    }

    /**
     * 设置ResourcesJson。
     */
    public void setResourcesJson(String resourcesJson) {
        this.resourcesJson = resourcesJson;
    }

    /**
     * 获取CompletionPercentage。
     */
    public Integer getCompletionPercentage() {
        return completionPercentage;
    }

    /**
     * 设置CompletionPercentage。
     */
    public void setCompletionPercentage(Integer completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    /**
     * 获取Completed。
     */
    public Boolean getCompleted() {
        return completed;
    }

    /**
     * 设置Completed。
     */
    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    /**
     * 获取CreatedAt。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
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

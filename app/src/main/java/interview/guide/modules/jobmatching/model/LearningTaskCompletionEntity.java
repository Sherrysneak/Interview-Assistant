package interview.guide.modules.jobmatching.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 学习任务完成记录实体。
 * 用于记录周计划中每个任务的打卡状态。
 */
@Entity
@Table(name = "learning_task_completions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_learning_task_completion", columnNames = {"plan_id", "taskIndex"})
}, indexes = {
    @Index(name = "idx_learning_task_completion_plan", columnList = "plan_id")
})
public class LearningTaskCompletionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private WeeklyLearningPlanEntity plan;

    @Column(nullable = false)
    private Integer taskIndex;

    @Column(nullable = false, length = 500)
    private String taskTitle;

    @Column(nullable = false)
    private Boolean completed = false;

    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

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
     * 获取Plan。
     */
    public WeeklyLearningPlanEntity getPlan() {
        return plan;
    }

    /**
     * 设置Plan。
     */
    public void setPlan(WeeklyLearningPlanEntity plan) {
        this.plan = plan;
    }

    /**
     * 获取TaskIndex。
     */
    public Integer getTaskIndex() {
        return taskIndex;
    }

    /**
     * 设置TaskIndex。
     */
    public void setTaskIndex(Integer taskIndex) {
        this.taskIndex = taskIndex;
    }

    /**
     * 获取TaskTitle。
     */
    public String getTaskTitle() {
        return taskTitle;
    }

    /**
     * 设置TaskTitle。
     */
    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
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

    /**
     * 获取Notes。
     */
    public String getNotes() {
        return notes;
    }

    /**
     * 设置Notes。
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * 获取CreatedAt。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

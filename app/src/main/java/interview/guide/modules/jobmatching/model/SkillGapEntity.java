package interview.guide.modules.jobmatching.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 技能差距实体。
 * 表示岗位要求与简历能力之间的差距明细。
 */
@Entity
@Table(name = "skill_gaps", indexes = {
    @Index(name = "idx_skill_gap_matching", columnList = "matching_id"),
    @Index(name = "idx_skill_gap_matching_severity", columnList = "matching_id,gapSeverity")
})
public class SkillGapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_id", nullable = false)
    private JobMatchingAnalysisEntity matching;

    @Column(nullable = false, length = 100)
    private String skillName;

    @Column(length = 50)
    private String requiredLevel;

    @Column(length = 50)
    private String currentLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GapSeverity gapSeverity;

    @Column(columnDefinition = "TEXT")
    private String jdEvidence;

    @Column(columnDefinition = "TEXT")
    private String resumeEvidence;

    @Column(columnDefinition = "TEXT")
    private String actionSuggestion;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 差距严重度。
     */
    public enum GapSeverity {
        CRITICAL,
        IMPORTANT,
        NICE_TO_HAVE
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
     * 获取SkillName。
     */
    public String getSkillName() {
        return skillName;
    }

    /**
     * 设置SkillName。
     */
    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    /**
     * 获取RequiredLevel。
     */
    public String getRequiredLevel() {
        return requiredLevel;
    }

    /**
     * 设置RequiredLevel。
     */
    public void setRequiredLevel(String requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    /**
     * 获取CurrentLevel。
     */
    public String getCurrentLevel() {
        return currentLevel;
    }

    /**
     * 设置CurrentLevel。
     */
    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    /**
     * 获取GapSeverity。
     */
    public GapSeverity getGapSeverity() {
        return gapSeverity;
    }

    /**
     * 设置GapSeverity。
     */
    public void setGapSeverity(GapSeverity gapSeverity) {
        this.gapSeverity = gapSeverity;
    }

    /**
     * 获取JdEvidence。
     */
    public String getJdEvidence() {
        return jdEvidence;
    }

    /**
     * 设置JdEvidence。
     */
    public void setJdEvidence(String jdEvidence) {
        this.jdEvidence = jdEvidence;
    }

    /**
     * 获取ResumeEvidence。
     */
    public String getResumeEvidence() {
        return resumeEvidence;
    }

    /**
     * 设置ResumeEvidence。
     */
    public void setResumeEvidence(String resumeEvidence) {
        this.resumeEvidence = resumeEvidence;
    }

    /**
     * 获取ActionSuggestion。
     */
    public String getActionSuggestion() {
        return actionSuggestion;
    }

    /**
     * 设置ActionSuggestion。
     */
    public void setActionSuggestion(String actionSuggestion) {
        this.actionSuggestion = actionSuggestion;
    }

    /**
     * 获取CreatedAt。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

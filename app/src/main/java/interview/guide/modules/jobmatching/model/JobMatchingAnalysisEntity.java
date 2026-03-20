package interview.guide.modules.jobmatching.model;

import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.modules.resume.model.ResumeEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 岗位匹配分析实体。
 * 记录 JD 与简历的评分、摘要和分析状态。
 */
@Entity
@Table(name = "job_matching_analyses", indexes = {
    @Index(name = "idx_job_matching_resume_created", columnList = "resume_id,createdAt")
})
public class JobMatchingAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private ResumeEntity resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jd_id", nullable = false)
    private JobDescriptionEntity jobDescription;

    private Integer overallScore;
    private Integer skillMatchScore;
    private Integer experienceMatchScore;
    private Integer projectMatchScore;
    private Integer educationMatchScore;

    @Column(columnDefinition = "TEXT")
    private String matchSummary;

    @Column(columnDefinition = "TEXT")
    private String evidenceChainsJson;

    @Column(columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(columnDefinition = "TEXT")
    private String improvementSuggestionsJson;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AsyncTaskStatus analyzeStatus = AsyncTaskStatus.PENDING;

    @Column(length = 500)
    private String analyzeError;

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
     * 获取Resume。
     */
    public ResumeEntity getResume() {
        return resume;
    }

    /**
     * 设置Resume。
     */
    public void setResume(ResumeEntity resume) {
        this.resume = resume;
    }

    /**
     * 获取JobDescription。
     */
    public JobDescriptionEntity getJobDescription() {
        return jobDescription;
    }

    /**
     * 设置JobDescription。
     */
    public void setJobDescription(JobDescriptionEntity jobDescription) {
        this.jobDescription = jobDescription;
    }

    /**
     * 获取OverallScore。
     */
    public Integer getOverallScore() {
        return overallScore;
    }

    /**
     * 设置OverallScore。
     */
    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    /**
     * 获取SkillMatchScore。
     */
    public Integer getSkillMatchScore() {
        return skillMatchScore;
    }

    /**
     * 设置SkillMatchScore。
     */
    public void setSkillMatchScore(Integer skillMatchScore) {
        this.skillMatchScore = skillMatchScore;
    }

    /**
     * 获取ExperienceMatchScore。
     */
    public Integer getExperienceMatchScore() {
        return experienceMatchScore;
    }

    /**
     * 设置ExperienceMatchScore。
     */
    public void setExperienceMatchScore(Integer experienceMatchScore) {
        this.experienceMatchScore = experienceMatchScore;
    }

    /**
     * 获取ProjectMatchScore。
     */
    public Integer getProjectMatchScore() {
        return projectMatchScore;
    }

    /**
     * 设置ProjectMatchScore。
     */
    public void setProjectMatchScore(Integer projectMatchScore) {
        this.projectMatchScore = projectMatchScore;
    }

    /**
     * 获取EducationMatchScore。
     */
    public Integer getEducationMatchScore() {
        return educationMatchScore;
    }

    /**
     * 设置EducationMatchScore。
     */
    public void setEducationMatchScore(Integer educationMatchScore) {
        this.educationMatchScore = educationMatchScore;
    }

    /**
     * 获取MatchSummary。
     */
    public String getMatchSummary() {
        return matchSummary;
    }

    /**
     * 设置MatchSummary。
     */
    public void setMatchSummary(String matchSummary) {
        this.matchSummary = matchSummary;
    }

    /**
     * 获取EvidenceChainsJson。
     */
    public String getEvidenceChainsJson() {
        return evidenceChainsJson;
    }

    /**
     * 设置EvidenceChainsJson。
     */
    public void setEvidenceChainsJson(String evidenceChainsJson) {
        this.evidenceChainsJson = evidenceChainsJson;
    }

    /**
     * 获取StrengthsJson。
     */
    public String getStrengthsJson() {
        return strengthsJson;
    }

    /**
     * 设置StrengthsJson。
     */
    public void setStrengthsJson(String strengthsJson) {
        this.strengthsJson = strengthsJson;
    }

    /**
     * 获取ImprovementSuggestionsJson。
     */
    public String getImprovementSuggestionsJson() {
        return improvementSuggestionsJson;
    }

    /**
     * 设置ImprovementSuggestionsJson。
     */
    public void setImprovementSuggestionsJson(String improvementSuggestionsJson) {
        this.improvementSuggestionsJson = improvementSuggestionsJson;
    }

    /**
     * 获取AnalyzeStatus。
     */
    public AsyncTaskStatus getAnalyzeStatus() {
        return analyzeStatus;
    }

    /**
     * 设置AnalyzeStatus。
     */
    public void setAnalyzeStatus(AsyncTaskStatus analyzeStatus) {
        this.analyzeStatus = analyzeStatus;
    }

    /**
     * 获取AnalyzeError。
     */
    public String getAnalyzeError() {
        return analyzeError;
    }

    /**
     * 设置AnalyzeError。
     */
    public void setAnalyzeError(String analyzeError) {
        this.analyzeError = analyzeError;
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

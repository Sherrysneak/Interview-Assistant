package interview.guide.modules.jobmatching.model;

import interview.guide.common.model.AsyncTaskStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 岗位描述实体。
 * 存储 JD 原文、结构化字段及解析状态。
 */
@Entity
@Table(name = "job_descriptions", indexes = {
    @Index(name = "idx_job_description_hash", columnList = "jdHash", unique = true),
    @Index(name = "idx_job_description_created", columnList = "createdAt"),
    @Index(name = "idx_job_description_deleted_created", columnList = "deletedAt,createdAt")
})
public class JobDescriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String jdHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceType sourceType;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(length = 255)
    private String originalFilename;

    @Column(length = 500)
    private String storageKey;

    @Column(nullable = false, length = 200)
    private String jobTitle;

    @Column(length = 200)
    private String companyName;

    @Column(length = 100)
    private String salaryRange;

    @Column(length = 200)
    private String workLocation;

    @Column(length = 50)
    private String educationRequirement;

    @Column(length = 50)
    private String experienceYears;

    @Column(columnDefinition = "TEXT")
    private String technicalSkillsJson;

    @Column(columnDefinition = "TEXT")
    private String coreRequirements;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Column(columnDefinition = "TEXT")
    private String bonusPoints;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rawJdText;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AsyncTaskStatus parseStatus = AsyncTaskStatus.PENDING;

    @Column(length = 500)
    private String parseError;

    private LocalDateTime parseStartedAt;

    private LocalDateTime parseFinishedAt;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer parseRetryCount = 0;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JD 来源类型。
     */
    public enum SourceType {
        URL,
        TEXT,
        FILE
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 获取Id。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置Id。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取JdHash。
     */
    public String getJdHash() {
        return jdHash;
    }

    /**
     * 设置JdHash。
     */
    public void setJdHash(String jdHash) {
        this.jdHash = jdHash;
    }

    /**
     * 获取SourceType。
     */
    public SourceType getSourceType() {
        return sourceType;
    }

    /**
     * 设置SourceType。
     */
    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * 获取SourceUrl。
     */
    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
     * 设置SourceUrl。
     */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    /**
     * 获取OriginalFilename。
     */
    public String getOriginalFilename() {
        return originalFilename;
    }

    /**
     * 设置OriginalFilename。
     */
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    /**
     * 获取StorageKey。
     */
    public String getStorageKey() {
        return storageKey;
    }

    /**
     * 设置StorageKey。
     */
    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    /**
     * 获取JobTitle。
     */
    public String getJobTitle() {
        return jobTitle;
    }

    /**
     * 设置JobTitle。
     */
    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    /**
     * 获取CompanyName。
     */
    public String getCompanyName() {
        return companyName;
    }

    /**
     * 设置CompanyName。
     */
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    /**
     * 获取SalaryRange。
     */
    public String getSalaryRange() {
        return salaryRange;
    }

    /**
     * 设置SalaryRange。
     */
    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    /**
     * 获取WorkLocation。
     */
    public String getWorkLocation() {
        return workLocation;
    }

    /**
     * 设置WorkLocation。
     */
    public void setWorkLocation(String workLocation) {
        this.workLocation = workLocation;
    }

    /**
     * 获取EducationRequirement。
     */
    public String getEducationRequirement() {
        return educationRequirement;
    }

    /**
     * 设置EducationRequirement。
     */
    public void setEducationRequirement(String educationRequirement) {
        this.educationRequirement = educationRequirement;
    }

    /**
     * 获取ExperienceYears。
     */
    public String getExperienceYears() {
        return experienceYears;
    }

    /**
     * 设置ExperienceYears。
     */
    public void setExperienceYears(String experienceYears) {
        this.experienceYears = experienceYears;
    }

    /**
     * 获取TechnicalSkillsJson。
     */
    public String getTechnicalSkillsJson() {
        return technicalSkillsJson;
    }

    /**
     * 设置TechnicalSkillsJson。
     */
    public void setTechnicalSkillsJson(String technicalSkillsJson) {
        this.technicalSkillsJson = technicalSkillsJson;
    }

    /**
     * 获取CoreRequirements。
     */
    public String getCoreRequirements() {
        return coreRequirements;
    }

    /**
     * 设置CoreRequirements。
     */
    public void setCoreRequirements(String coreRequirements) {
        this.coreRequirements = coreRequirements;
    }

    /**
     * 获取Responsibilities。
     */
    public String getResponsibilities() {
        return responsibilities;
    }

    /**
     * 设置Responsibilities。
     */
    public void setResponsibilities(String responsibilities) {
        this.responsibilities = responsibilities;
    }

    /**
     * 获取BonusPoints。
     */
    public String getBonusPoints() {
        return bonusPoints;
    }

    /**
     * 设置BonusPoints。
     */
    public void setBonusPoints(String bonusPoints) {
        this.bonusPoints = bonusPoints;
    }

    /**
     * 获取RawJdText。
     */
    public String getRawJdText() {
        return rawJdText;
    }

    /**
     * 设置RawJdText。
     */
    public void setRawJdText(String rawJdText) {
        this.rawJdText = rawJdText;
    }

    /**
     * 获取ParseStatus。
     */
    public AsyncTaskStatus getParseStatus() {
        return parseStatus;
    }

    /**
     * 设置ParseStatus。
     */
    public void setParseStatus(AsyncTaskStatus parseStatus) {
        this.parseStatus = parseStatus;
    }

    /**
     * 获取ParseError。
     */
    public String getParseError() {
        return parseError;
    }

    /**
     * 设置ParseError。
     */
    public void setParseError(String parseError) {
        this.parseError = parseError;
    }

    /**
     * 获取ParseStartedAt。
     */
    public LocalDateTime getParseStartedAt() {
        return parseStartedAt;
    }

    /**
     * 设置ParseStartedAt。
     */
    public void setParseStartedAt(LocalDateTime parseStartedAt) {
        this.parseStartedAt = parseStartedAt;
    }

    /**
     * 获取ParseFinishedAt。
     */
    public LocalDateTime getParseFinishedAt() {
        return parseFinishedAt;
    }

    /**
     * 设置ParseFinishedAt。
     */
    public void setParseFinishedAt(LocalDateTime parseFinishedAt) {
        this.parseFinishedAt = parseFinishedAt;
    }

    /**
     * 获取ParseRetryCount。
     */
    public Integer getParseRetryCount() {
        return parseRetryCount;
    }

    /**
     * 设置ParseRetryCount。
     */
    public void setParseRetryCount(Integer parseRetryCount) {
        this.parseRetryCount = parseRetryCount;
    }

    /**
     * 获取DeletedAt。
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 设置DeletedAt。
     */
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 获取CreatedAt。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取UpdatedAt。
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

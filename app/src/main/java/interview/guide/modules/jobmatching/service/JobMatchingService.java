package interview.guide.modules.jobmatching.service;

import interview.guide.common.ai.StructuredOutputInvoker;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.modules.interview.model.CreateInterviewRequest;
import interview.guide.modules.interview.model.InterviewJobContext;
import interview.guide.modules.interview.model.InterviewSessionDTO;
import interview.guide.modules.interview.service.InterviewSessionService;
import interview.guide.modules.jobmatching.listener.JobMatchingAnalyzeStreamProducer;
import interview.guide.modules.jobmatching.model.*;
import interview.guide.modules.jobmatching.repository.*;
import interview.guide.modules.resume.model.ResumeEntity;
import interview.guide.modules.resume.repository.ResumeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 岗位匹配主服务。
 * 负责匹配分析、技能差距落库、学习路径生成和 JD 联动面试创建。
 */
@Slf4j
@Service
public class JobMatchingService {

    private final JobDescriptionService jobDescriptionService;
    private final JobMatchingAnalysisRepository analysisRepository;
    private final SkillGapRepository skillGapRepository;
    private final LearningPathRepository learningPathRepository;
    private final WeeklyLearningPlanRepository weeklyLearningPlanRepository;
    private final LearningTaskCompletionRepository taskCompletionRepository;
    private final ResumeRepository resumeRepository;
    private final McpGatewayService mcpGatewayService;
    private final StructuredOutputInvoker structuredOutputInvoker;
    private final InterviewSessionService interviewSessionService;
    private final ObjectMapper objectMapper;
    private final JobMatchingAnalyzeStreamProducer jobMatchingAnalyzeStreamProducer;

    private final ChatClient chatClient;
    private final PromptTemplate matchingSystemTemplate;
    private final PromptTemplate matchingUserTemplate;
    private final PromptTemplate learningSystemTemplate;
    private final PromptTemplate learningUserTemplate;

    @Value("${app.jobmatching.default-duration-weeks:4}")
    private int defaultDurationWeeks;

    /**
     * 构造岗位匹配服务。
     *
     * @param jobDescriptionService JD 服务
     * @param analysisRepository 匹配分析仓储
     * @param skillGapRepository 技能差距仓储
     * @param learningPathRepository 学习路径仓储
     * @param weeklyLearningPlanRepository 周计划仓储
     * @param taskCompletionRepository 任务完成仓储
     * @param resumeRepository 简历仓储
     * @param mcpGatewayService MCP 网关服务
     * @param structuredOutputInvoker 结构化输出调用器
     * @param interviewSessionService 面试会话服务
     * @param objectMapper JSON 工具
    * @param jobMatchingAnalyzeStreamProducer 匹配分析异步任务生产者
     * @param chatClientBuilder AI 客户端构建器
     * @param matchingSystemPrompt 匹配系统提示词
     * @param matchingUserPrompt 匹配用户提示词
     * @param learningSystemPrompt 学习路径系统提示词
     * @param learningUserPrompt 学习路径用户提示词
     * @throws IOException 读取提示词失败时抛出
     */
    @Autowired
    public JobMatchingService(
        JobDescriptionService jobDescriptionService,
        JobMatchingAnalysisRepository analysisRepository,
        SkillGapRepository skillGapRepository,
        LearningPathRepository learningPathRepository,
        WeeklyLearningPlanRepository weeklyLearningPlanRepository,
        LearningTaskCompletionRepository taskCompletionRepository,
        ResumeRepository resumeRepository,
        McpGatewayService mcpGatewayService,
        StructuredOutputInvoker structuredOutputInvoker,
        InterviewSessionService interviewSessionService,
        ObjectMapper objectMapper,
        JobMatchingAnalyzeStreamProducer jobMatchingAnalyzeStreamProducer,
        ChatClient.Builder chatClientBuilder,
        @Value("classpath:prompts/job-matching-system.st") Resource matchingSystemPrompt,
        @Value("classpath:prompts/job-matching-user.st") Resource matchingUserPrompt,
        @Value("classpath:prompts/learning-path-system.st") Resource learningSystemPrompt,
        @Value("classpath:prompts/learning-path-user.st") Resource learningUserPrompt
    ) throws IOException {
        this.jobDescriptionService = jobDescriptionService;
        this.analysisRepository = analysisRepository;
        this.skillGapRepository = skillGapRepository;
        this.learningPathRepository = learningPathRepository;
        this.weeklyLearningPlanRepository = weeklyLearningPlanRepository;
        this.taskCompletionRepository = taskCompletionRepository;
        this.resumeRepository = resumeRepository;
        this.mcpGatewayService = mcpGatewayService;
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.interviewSessionService = interviewSessionService;
        this.objectMapper = objectMapper;
        this.jobMatchingAnalyzeStreamProducer = jobMatchingAnalyzeStreamProducer;
        this.chatClient = chatClientBuilder.build();
        this.matchingSystemTemplate = new PromptTemplate(matchingSystemPrompt.getContentAsString(StandardCharsets.UTF_8));
        this.matchingUserTemplate = new PromptTemplate(matchingUserPrompt.getContentAsString(StandardCharsets.UTF_8));
        this.learningSystemTemplate = new PromptTemplate(learningSystemPrompt.getContentAsString(StandardCharsets.UTF_8));
        this.learningUserTemplate = new PromptTemplate(learningUserPrompt.getContentAsString(StandardCharsets.UTF_8));
    }

    /**
     * 创建岗位匹配分析任务并入队异步执行。
     *
     * @param request 岗位匹配请求
     * @return 新建匹配记录
     */
    @Transactional
    public JobMatchingRecordDTO createAndEnqueue(CreateJobMatchingRequest request) {
        ResumeEntity resume = resumeRepository.findById(request.resumeId())
            .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
        JobDescriptionEntity jd = jobDescriptionService.getEntity(request.jdId());

        JobMatchingAnalysisEntity analysis = new JobMatchingAnalysisEntity();
        analysis.setResume(resume);
        analysis.setJobDescription(jd);
        analysis.setAnalyzeStatus(AsyncTaskStatus.PENDING);
        analysis.setAnalyzeError(null);
        analysis.setCompletedAt(null);
        analysis = analysisRepository.save(analysis);
        jobMatchingAnalyzeStreamProducer.sendAnalyzeTask(analysis.getId(), request.durationWeeks());

        return toMatchingRecord(analysis);
    }

    /**
     * 异步消费者执行匹配分析主流程。
     *
     * @param matchingId 匹配分析 ID
     */
    @Transactional
    public void processPendingMatching(Long matchingId, Integer durationWeeks) {
        JobMatchingAnalysisEntity analysis = analysisRepository.findById(matchingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.JOB_MATCHING_NOT_FOUND));

        if (analysis.getAnalyzeStatus() == AsyncTaskStatus.COMPLETED) {
            return;
        }

        try {
            ResumeEntity resume = analysis.getResume();
            JobDescriptionEntity jd = analysis.getJobDescription();

            MatchingAiResult result = runMatchingAi(resume.getResumeText(), jd);
            applyMatchingResult(analysis, result);
            skillGapRepository.deleteByMatchingId(analysis.getId());
            saveSkillGaps(analysis, result.skillGaps());

            if (learningPathRepository.findByMatchingId(analysis.getId()).isEmpty()) {
                generateLearningPath(analysis, durationWeeks, result.skillGaps());
            }

            analysis.setAnalyzeStatus(AsyncTaskStatus.COMPLETED);
            analysis.setAnalyzeError(null);
            analysis.setCompletedAt(LocalDateTime.now());
            analysisRepository.save(analysis);
        } catch (Exception e) {
            analysis.setAnalyzeStatus(AsyncTaskStatus.FAILED);
            analysis.setAnalyzeError(truncate(e.getMessage()));
            analysis.setCompletedAt(LocalDateTime.now());
            analysisRepository.save(analysis);
            log.error("岗位匹配分析失败: matchingId={}, error={}", analysis.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询某个 JD 的匹配记录列表。
     *
     * @param jdId JD ID
     * @return 匹配记录列表
     */
    @Transactional(readOnly = true)
    public List<JobMatchingRecordDTO> listMatchingsByJdId(Long jdId) {
        JobDescriptionEntity jd = jobDescriptionService.getEntity(jdId);
        return analysisRepository.findByJobDescriptionIdOrderByCreatedAtDesc(jd.getId()).stream()
            .map(this::toMatchingRecord)
            .toList();
    }

    private JobMatchingRecordDTO toMatchingRecord(JobMatchingAnalysisEntity analysis) {
        return new JobMatchingRecordDTO(
            analysis.getId(),
            analysis.getResume().getId(),
            analysis.getResume().getOriginalFilename(),
            analysis.getJobDescription().getId(),
            analysis.getJobDescription().getJobTitle(),
            analysis.getOverallScore(),
            analysis.getAnalyzeStatus(),
            analysis.getAnalyzeError(),
            analysis.getCreatedAt(),
            analysis.getCompletedAt()
        );
    }

    /**
     * 获取岗位匹配报告。
     *
     * @param matchingId 匹配分析 ID
     * @return 岗位匹配报告
     */
    @Transactional(readOnly = true)
    public JobMatchingReportDTO getReport(Long matchingId) {
        JobMatchingAnalysisEntity analysis = analysisRepository.findById(matchingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.JOB_MATCHING_NOT_FOUND));

        List<SkillGapEntity> gaps = skillGapRepository.findByMatchingIdOrderByGapSeverityAscIdAsc(matchingId);
        LearningPathDTO path = learningPathRepository.findByMatchingId(matchingId)
            .map(this::toLearningPath)
            .orElse(null);

        return new JobMatchingReportDTO(
            analysis.getId(),
            analysis.getResume().getId(),
            analysis.getJobDescription().getId(),
            analysis.getOverallScore(),
            analysis.getSkillMatchScore(),
            analysis.getExperienceMatchScore(),
            analysis.getProjectMatchScore(),
            analysis.getEducationMatchScore(),
            analysis.getMatchSummary(),
            readStringList(analysis.getEvidenceChainsJson()),
            readStringList(analysis.getStrengthsJson()),
            readStringList(analysis.getImprovementSuggestionsJson()),
            gaps.stream().map(this::toSkillGapDTO).toList(),
            path,
            analysis.getAnalyzeStatus(),
            analysis.getAnalyzeError()
        );
    }

    /**
     * 完成学习任务并刷新学习路径进度。
     *
     * @param planId 周计划 ID
     * @param taskIndex 任务索引
     * @param request 打卡请求
     * @return 更新后的学习路径
     */
    @Transactional
    public LearningPathDTO completeTask(Long planId, Integer taskIndex, TaskCompletionRequest request) {
        WeeklyLearningPlanEntity plan = weeklyLearningPlanRepository.findById(planId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "周计划不存在"));

        List<String> tasks = readStringList(plan.getTasksJson());
        if (taskIndex < 0 || taskIndex >= tasks.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务索引无效");
        }

        LearningTaskCompletionEntity completion = taskCompletionRepository.findByPlanIdAndTaskIndex(planId, taskIndex)
            .orElseGet(() -> {
                LearningTaskCompletionEntity entity = new LearningTaskCompletionEntity();
                entity.setPlan(plan);
                entity.setTaskIndex(taskIndex);
                entity.setTaskTitle(tasks.get(taskIndex));
                return entity;
            });

        completion.setCompleted(Boolean.TRUE);
        completion.setCompletedAt(LocalDateTime.now());
        completion.setNotes(request == null ? null : request.notes());
        taskCompletionRepository.save(completion);

        updateProgress(plan);
        return toLearningPath(plan.getPath());
    }

    /**
     * 基于岗位匹配报告生成面试会话。
     *
     * @param matchingId 匹配分析 ID
     * @param request 面试生成请求
     * @return 面试会话信息
     */
    @Transactional
    public InterviewSessionDTO generateInterviewFromMatching(
        Long matchingId,
        GenerateInterviewFromMatchingRequest request
    ) {
        JobMatchingAnalysisEntity analysis = analysisRepository.findById(matchingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.JOB_MATCHING_NOT_FOUND));
        ResumeEntity resume = analysis.getResume();
        JobDescriptionEntity jd = analysis.getJobDescription();

        List<SkillGapEntity> gaps = skillGapRepository.findByMatchingIdOrderByGapSeverityAscIdAsc(matchingId);
        List<String> focusSkills = gaps.stream()
            .filter(g -> g.getGapSeverity() != SkillGapEntity.GapSeverity.NICE_TO_HAVE)
            .map(SkillGapEntity::getSkillName)
            .distinct()
            .toList();

        InterviewJobContext context = new InterviewJobContext(
            jd.getId(),
            jd.getJobTitle(),
            jd.getCoreRequirements(),
            readStringList(jd.getTechnicalSkillsJson()),
            focusSkills
        );

        int questionCount = request == null || request.questionCount() == null ? 8 : request.questionCount();
        Boolean forceCreate = request != null && Boolean.TRUE.equals(request.forceCreate());

        CreateInterviewRequest createRequest = new CreateInterviewRequest(
            resume.getResumeText(),
            questionCount,
            resume.getId(),
            forceCreate,
            context
        );

        return interviewSessionService.createSession(createRequest);
    }

    private MatchingAiResult runMatchingAi(String resumeText, JobDescriptionEntity jd) {
        BeanOutputConverter<MatchingAiResult> converter = new BeanOutputConverter<>(MatchingAiResult.class);
        String systemPrompt = matchingSystemTemplate.render() + "\n\n" + converter.getFormat();
        String userPrompt = matchingUserTemplate.render(Map.of(
            "resumeText", resumeText,
            "jdText", jd.getRawJdText(),
            "technicalSkills", readStringList(jd.getTechnicalSkillsJson()),
            "jobTitle", jd.getJobTitle(),
            "experienceYears", safe(jd.getExperienceYears())
        ));

        return structuredOutputInvoker.invoke(
            chatClient,
            systemPrompt,
            userPrompt,
            converter,
            ErrorCode.JOB_MATCHING_ANALYSIS_FAILED,
            "岗位匹配分析失败: ",
            "岗位匹配",
            log
        );
    }

    private void applyMatchingResult(JobMatchingAnalysisEntity analysis, MatchingAiResult result) {
        analysis.setOverallScore(clamp(result.overallScore()));
        analysis.setSkillMatchScore(clamp(result.skillMatchScore()));
        analysis.setExperienceMatchScore(clamp(result.experienceMatchScore()));
        analysis.setProjectMatchScore(clamp(result.projectMatchScore()));
        analysis.setEducationMatchScore(clamp(result.educationMatchScore()));
        analysis.setMatchSummary(result.matchSummary());
        analysis.setEvidenceChainsJson(writeJson(result.evidenceChains()));
        analysis.setStrengthsJson(writeJson(result.strengths()));
        analysis.setImprovementSuggestionsJson(writeJson(result.improvementSuggestions()));
    }

    private void saveSkillGaps(JobMatchingAnalysisEntity analysis, List<SkillGapAi> gaps) {
        List<SkillGapEntity> entities = new ArrayList<>();
        if (gaps != null) {
            for (SkillGapAi gap : gaps) {
                SkillGapEntity entity = new SkillGapEntity();
                entity.setMatching(analysis);
                entity.setSkillName(limit(gap.skillName(), 100));
                entity.setRequiredLevel(limit(gap.requiredLevel(), 50));
                entity.setCurrentLevel(limit(gap.currentLevel(), 50));
                entity.setGapSeverity(parseSeverity(gap.severity()));
                entity.setJdEvidence(gap.jdEvidence());
                entity.setResumeEvidence(gap.resumeEvidence());
                entity.setActionSuggestion(gap.actionSuggestion());
                entities.add(entity);
            }
        }
        skillGapRepository.saveAll(entities);
    }

    private void generateLearningPath(
        JobMatchingAnalysisEntity analysis,
        Integer durationWeeks,
        List<SkillGapAi> gaps
    ) {
        int weeks = durationWeeks == null ? defaultDurationWeeks : durationWeeks;
        List<String> keySkills = gaps == null ? List.of() : gaps.stream()
            .filter(g -> parseSeverity(g.severity()) != SkillGapEntity.GapSeverity.NICE_TO_HAVE)
            .map(SkillGapAi::skillName)
            .distinct()
            .toList();

        List<LearningResourceDTO> resources = mcpGatewayService.fetchResources(keySkills);
        LearningPathAiResult aiResult = runLearningPathAi(analysis, weeks, keySkills, resources);

        LearningPathEntity path = new LearningPathEntity();
        path.setMatching(analysis);
        path.setResumeId(analysis.getResume().getId());
        path.setJdId(analysis.getJobDescription().getId());
        path.setTargetGoal(aiResult.targetGoal());
        path.setDurationWeeks(aiResult.durationWeeks() == null ? weeks : aiResult.durationWeeks());
        path.setEstimatedHoursPerWeek(aiResult.estimatedHoursPerWeek() == null ? 12 : aiResult.estimatedHoursPerWeek());
        path = learningPathRepository.save(path);

        List<WeeklyLearningPlanEntity> weeklyPlans = new ArrayList<>();
        List<WeeklyPlanAi> plans = aiResult.weeklyPlans() == null ? List.of() : aiResult.weeklyPlans();
        if (plans.isEmpty()) {
            plans = buildFallbackPlans(weeks, keySkills);
        }

        int resourceIndex = 0;
        for (WeeklyPlanAi ai : plans) {
            WeeklyLearningPlanEntity entity = new WeeklyLearningPlanEntity();
            entity.setPath(path);
            entity.setWeekNumber(ai.weekNumber());
            entity.setTitle(ai.title());
            entity.setObjectivesJson(writeJson(nonNull(ai.objectives())));
            entity.setTasksJson(writeJson(nonNull(ai.tasks())));
            entity.setDeliverablesJson(writeJson(nonNull(ai.deliverables())));
            entity.setAcceptanceCriteriaJson(writeJson(nonNull(ai.acceptanceCriteria())));

            List<LearningResourceDTO> weeklyResources = new ArrayList<>();
            for (int i = 0; i < 4 && resourceIndex < resources.size(); i++) {
                weeklyResources.add(resources.get(resourceIndex++));
            }
            entity.setResourcesJson(writeJson(weeklyResources));
            weeklyPlans.add(entity);
        }

        weeklyLearningPlanRepository.saveAll(weeklyPlans);
    }

    private LearningPathAiResult runLearningPathAi(
        JobMatchingAnalysisEntity analysis,
        int weeks,
        List<String> keySkills,
        List<LearningResourceDTO> resources
    ) {
        BeanOutputConverter<LearningPathAiResult> converter = new BeanOutputConverter<>(LearningPathAiResult.class);
        String systemPrompt = learningSystemTemplate.render() + "\n\n" + converter.getFormat();
        String userPrompt = learningUserTemplate.render(Map.of(
            "jobTitle", analysis.getJobDescription().getJobTitle(),
            "durationWeeks", weeks,
            "keySkills", keySkills,
            "resources", resources,
            "resumeSummary", safe(analysis.getMatchSummary())
        ));

        try {
            return structuredOutputInvoker.invoke(
                chatClient,
                systemPrompt,
                userPrompt,
                converter,
                ErrorCode.LEARNING_PATH_GENERATION_FAILED,
                "学习路径生成失败: ",
                "学习路径",
                log
            );
        } catch (Exception e) {
            log.warn("学习路径 AI 生成失败，使用规则兜底: {}", e.getMessage());
            return new LearningPathAiResult(
                "围绕岗位关键能力完成补强",
                weeks,
                12,
                buildFallbackPlans(weeks, keySkills)
            );
        }
    }

    private void updateProgress(WeeklyLearningPlanEntity plan) {
        List<String> tasks = readStringList(plan.getTasksJson());
        int total = tasks.isEmpty() ? 1 : tasks.size();
        int done = taskCompletionRepository.findByPlanId(plan.getId()).stream()
            .filter(LearningTaskCompletionEntity::getCompleted)
            .map(LearningTaskCompletionEntity::getTaskIndex)
            .collect(Collectors.toSet())
            .size();

        int completion = Math.min(100, (int) ((done * 100.0) / total));
        plan.setCompletionPercentage(completion);
        plan.setCompleted(completion >= 100);
        if (completion >= 100) {
            plan.setCompletedAt(LocalDateTime.now());
        }
        weeklyLearningPlanRepository.save(plan);

        LearningPathEntity path = plan.getPath();
        List<WeeklyLearningPlanEntity> plans = weeklyLearningPlanRepository.findByPathIdOrderByWeekNumberAsc(path.getId());
        int sum = plans.stream().mapToInt(WeeklyLearningPlanEntity::getCompletionPercentage).sum();
        int progress = plans.isEmpty() ? 0 : sum / plans.size();
        path.setProgressPercentage(progress);
        if (progress > 0 && path.getStatus() == LearningPathEntity.LearningPathStatus.NOT_STARTED) {
            path.setStatus(LearningPathEntity.LearningPathStatus.IN_PROGRESS);
            path.setStartedAt(LocalDateTime.now());
        }
        if (progress >= 100) {
            path.setStatus(LearningPathEntity.LearningPathStatus.COMPLETED);
            path.setCompletedAt(LocalDateTime.now());
        }
        learningPathRepository.save(path);
    }

    private LearningPathDTO toLearningPath(LearningPathEntity path) {
        List<WeeklyPlanDTO> plans = weeklyLearningPlanRepository.findByPathIdOrderByWeekNumberAsc(path.getId()).stream()
            .map(this::toWeeklyPlan)
            .toList();

        return new LearningPathDTO(
            path.getId(),
            path.getTargetGoal(),
            path.getDurationWeeks(),
            path.getEstimatedHoursPerWeek(),
            path.getProgressPercentage(),
            path.getStatus(),
            plans
        );
    }

    private WeeklyPlanDTO toWeeklyPlan(WeeklyLearningPlanEntity entity) {
        return new WeeklyPlanDTO(
            entity.getId(),
            entity.getWeekNumber(),
            entity.getTitle(),
            readStringList(entity.getObjectivesJson()),
            readStringList(entity.getTasksJson()),
            readStringList(entity.getDeliverablesJson()),
            readStringList(entity.getAcceptanceCriteriaJson()),
            readResourceList(entity.getResourcesJson()),
            entity.getCompletionPercentage(),
            entity.getCompleted()
        );
    }

    private SkillGapDTO toSkillGapDTO(SkillGapEntity entity) {
        return new SkillGapDTO(
            entity.getSkillName(),
            entity.getRequiredLevel(),
            entity.getCurrentLevel(),
            entity.getGapSeverity(),
            entity.getJdEvidence(),
            entity.getResumeEvidence(),
            entity.getActionSuggestion()
        );
    }

    private SkillGapEntity.GapSeverity parseSeverity(String severity) {
        if (severity == null) {
            return SkillGapEntity.GapSeverity.IMPORTANT;
        }
        try {
            return SkillGapEntity.GapSeverity.valueOf(severity.trim().toUpperCase());
        } catch (Exception e) {
            return SkillGapEntity.GapSeverity.IMPORTANT;
        }
    }

    private List<WeeklyPlanAi> buildFallbackPlans(int weeks, List<String> keySkills) {
        List<WeeklyPlanAi> plans = new ArrayList<>();
        List<String> skills = keySkills == null || keySkills.isEmpty() ? List.of("Java", "Spring", "数据库") : keySkills;
        for (int i = 1; i <= weeks; i++) {
            String skill = skills.get((i - 1) % skills.size());
            plans.add(new WeeklyPlanAi(
                i,
                "第 " + i + " 周 - " + skill + " 专项强化",
                List.of("掌握 " + skill + " 核心知识点", "形成可讲解的实践经验"),
                List.of("阅读官方文档并整理笔记", "完成一个最小可运行 Demo", "录制一次 10 分钟讲解"),
                List.of("一篇技术总结", "一个可运行仓库"),
                List.of("能够解释原理并给出落地方案", "可以回答 3 个追问")
            ));
        }
        return plans;
    }

    private List<String> nonNull(List<String> list) {
        return list == null ? List.of() : list;
    }

    private int clamp(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String writeJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<LearningResourceDTO> readResourceList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record MatchingAiResult(
        Integer overallScore,
        Integer skillMatchScore,
        Integer experienceMatchScore,
        Integer projectMatchScore,
        Integer educationMatchScore,
        String matchSummary,
        List<String> evidenceChains,
        List<String> strengths,
        List<String> improvementSuggestions,
        List<SkillGapAi> skillGaps
    ) {
    }

    private record SkillGapAi(
        String skillName,
        String requiredLevel,
        String currentLevel,
        String severity,
        String jdEvidence,
        String resumeEvidence,
        String actionSuggestion
    ) {
    }

    private record LearningPathAiResult(
        String targetGoal,
        Integer durationWeeks,
        Integer estimatedHoursPerWeek,
        List<WeeklyPlanAi> weeklyPlans
    ) {
    }

    private record WeeklyPlanAi(
        Integer weekNumber,
        String title,
        List<String> objectives,
        List<String> tasks,
        List<String> deliverables,
        List<String> acceptanceCriteria
    ) {
    }
}

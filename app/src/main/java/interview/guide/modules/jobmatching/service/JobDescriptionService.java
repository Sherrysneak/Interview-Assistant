package interview.guide.modules.jobmatching.service;

import interview.guide.common.ai.StructuredOutputInvoker;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.infrastructure.file.DocumentParseService;
import interview.guide.infrastructure.file.FileHashService;
import interview.guide.infrastructure.file.FileStorageService;
import interview.guide.modules.jobmatching.listener.JdParseStreamProducer;
import interview.guide.modules.jobmatching.model.JobDescriptionDTO;
import interview.guide.modules.jobmatching.model.JobDescriptionEntity;
import interview.guide.modules.jobmatching.model.JdTextUploadRequest;
import interview.guide.modules.jobmatching.model.JdUrlUploadRequest;
import interview.guide.modules.jobmatching.repository.JobDescriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 岗位描述服务。
 * 提供 JD 文本/文件/URL 三种接入方式，并完成结构化解析与持久化。
 */
@Slf4j
@Service
public class JobDescriptionService {

    private static final List<String> SKILL_HINTS = List.of(
        "Java", "Spring", "Spring Boot", "MySQL", "Redis", "Kafka", "RabbitMQ", "Docker", "Kubernetes",
        "Elasticsearch", "JVM", "Linux", "Netty", "Nginx", "微服务", "分布式", "高并发"
    );

    private final JobDescriptionRepository jobDescriptionRepository;
    private final FileHashService fileHashService;
    private final DocumentParseService documentParseService;
    private final FileStorageService fileStorageService;
    private final ChatClient chatClient;
    private final StructuredOutputInvoker structuredOutputInvoker;
    private final PromptTemplate parseSystemTemplate;
    private final PromptTemplate parseUserTemplate;
    private final ObjectMapper objectMapper;
    private final JdParseStreamProducer jdParseStreamProducer;

    @Value("${app.jobmatching.url-enabled:true}")
    private boolean urlEnabled;

    @Value("${app.jobmatching.min-url-extracted-length:180}")
    private int minExtractedLength;

    @Value("${app.jobmatching.max-jd-text-length:30000}")
    private int maxJdTextLength;

    /**
     * 构造岗位描述服务。
     *
     * @param jobDescriptionRepository JD 仓储
     * @param fileHashService 文件哈希服务
     * @param documentParseService 文档解析服务
     * @param fileStorageService 文件存储服务
     * @param chatClientBuilder AI 客户端构建器
     * @param structuredOutputInvoker 结构化输出调用器
     * @param parseSystemPrompt JD 解析系统提示词
     * @param parseUserPrompt JD 解析用户提示词
     * @throws IOException 读取提示词失败时抛出
     */
    public JobDescriptionService(
        JobDescriptionRepository jobDescriptionRepository,
        FileHashService fileHashService,
        DocumentParseService documentParseService,
        FileStorageService fileStorageService,
        ChatClient.Builder chatClientBuilder,
        StructuredOutputInvoker structuredOutputInvoker,
        ObjectMapper objectMapper,
        JdParseStreamProducer jdParseStreamProducer,
        @Value("classpath:prompts/jd-parse-system.st") Resource parseSystemPrompt,
        @Value("classpath:prompts/jd-parse-user.st") Resource parseUserPrompt
    ) throws IOException {
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.fileHashService = fileHashService;
        this.documentParseService = documentParseService;
        this.fileStorageService = fileStorageService;
        this.chatClient = chatClientBuilder.build();
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.objectMapper = objectMapper;
        this.jdParseStreamProducer = jdParseStreamProducer;
        this.parseSystemTemplate = new PromptTemplate(parseSystemPrompt.getContentAsString(StandardCharsets.UTF_8));
        this.parseUserTemplate = new PromptTemplate(parseUserPrompt.getContentAsString(StandardCharsets.UTF_8));
    }

    /**
     * 通过文本创建 JD。
     *
     * @param request JD 文本请求
     * @return 结构化 JD 信息
     */
    public JobDescriptionDTO createFromText(JdTextUploadRequest request) {
        String jdText = request.jdText().trim();
        if (jdText.length() < 80) {
            throw new BusinessException(ErrorCode.JOB_DESCRIPTION_PARSE_FAILED, "JD 文本过短，无法有效解析");
        }
        String hash = fileHashService.calculateHash(jdText.getBytes(StandardCharsets.UTF_8));
        return jobDescriptionRepository.findByJdHash(hash)
            .map(this::toDto)
            .orElseGet(() -> enqueueAndSave(jdText, hash, JobDescriptionEntity.SourceType.TEXT, null, null, null));
    }

    /**
     * 通过文件创建 JD。
     *
     * @param file JD 文件
     * @return 结构化 JD 信息
     */
    public JobDescriptionDTO createFromFile(MultipartFile file) {
        try {
            String hash = fileHashService.calculateHash(file);
            return jobDescriptionRepository.findByJdHash(hash)
                .map(this::toDto)
                .orElseGet(() -> {
                    String storageKey = fileStorageService.uploadKnowledgeBase(file);
                    String jdText = documentParseService.parseContent(file);
                    return enqueueAndSave(jdText, hash, JobDescriptionEntity.SourceType.FILE, null, storageKey, file.getOriginalFilename());
                });
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.JOB_DESCRIPTION_PARSE_FAILED, "JD 文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 通过 URL 创建 JD。
     *
     * @param request JD URL 请求
     * @return 结构化 JD 信息
     */
    public JobDescriptionDTO createFromUrl(JdUrlUploadRequest request) {
        if (!urlEnabled) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 抓取功能已关闭");
        }
        String text = fetchAndExtractText(request.url());
        String hash = fileHashService.calculateHash(text.getBytes(StandardCharsets.UTF_8));
        return jobDescriptionRepository.findByJdHash(hash)
            .map(this::toDto)
            .orElseGet(() -> enqueueAndSave(text, hash, JobDescriptionEntity.SourceType.URL, request.url(), null, null));
    }

    /**
     * 获取 JD 库列表。
     */
    public List<JobDescriptionDTO> listActive() {
        return jobDescriptionRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
            .map(this::toDto)
            .toList();
    }

    /**
     * 软删除 JD。
     */
    @Transactional
    public void softDelete(Long id) {
        JobDescriptionEntity entity = jobDescriptionRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.JOB_DESCRIPTION_NOT_FOUND));
        entity.setDeletedAt(LocalDateTime.now());
        jobDescriptionRepository.save(entity);
    }

    /**
     * 重试解析指定 JD。
     */
    @Transactional
    public JobDescriptionDTO retryParse(Long id) {
        JobDescriptionEntity entity = jobDescriptionRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.JOB_DESCRIPTION_NOT_FOUND));
        entity.setParseStatus(AsyncTaskStatus.PENDING);
        entity.setParseError(null);
        entity.setParseFinishedAt(null);
        entity.setParseStartedAt(null);
        jobDescriptionRepository.save(entity);
        jdParseStreamProducer.sendParseTask(entity.getId());
        return toDto(entity);
    }

    /**
     * 获取 JD 实体。
     *
     * @param id JD ID
     * @return JD 实体
     */
    public JobDescriptionEntity getEntity(Long id) {
        return jobDescriptionRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.JOB_DESCRIPTION_NOT_FOUND));
    }

    /**
     * 获取 JD DTO。
     *
     * @param id JD ID
     * @return JD DTO
     */
    public JobDescriptionDTO getById(Long id) {
        return toDto(getEntity(id));
    }

    /**
     * 消费端执行解析。
     */
    @Transactional
    public void parsePending(Long id) {
        JobDescriptionEntity entity = jobDescriptionRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.JOB_DESCRIPTION_NOT_FOUND));
        if (entity.getDeletedAt() != null) {
            return;
        }

        try {
            StructuredJd structured = parseStructured(entity.getRawJdText());
            entity.setJobTitle(defaultValue(structured.jobTitle(), defaultValue(entity.getJobTitle(), "未命名岗位")));
            entity.setCompanyName(structured.companyName());
            entity.setSalaryRange(structured.salaryRange());
            entity.setWorkLocation(structured.workLocation());
            entity.setEducationRequirement(structured.educationRequirement());
            entity.setExperienceYears(structured.experienceYears());
            entity.setTechnicalSkillsJson(toJsonSkills(structured.technicalSkills()));
            entity.setCoreRequirements(structured.coreRequirements());
            entity.setResponsibilities(structured.responsibilities());
            entity.setBonusPoints(structured.bonusPoints());
            entity.setParseStatus(AsyncTaskStatus.COMPLETED);
            entity.setParseError(null);
            entity.setParseFinishedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("JD AI 解析失败，使用规则兜底: {}", e.getMessage());
            entity.setJobTitle(defaultValue(entity.getJobTitle(), inferTitle(entity.getRawJdText())));
            entity.setTechnicalSkillsJson(toJsonSkills(inferSkills(entity.getRawJdText())));
            entity.setCoreRequirements(extractBlock(entity.getRawJdText(), "任职要求", "岗位职责"));
            entity.setResponsibilities(extractBlock(entity.getRawJdText(), "岗位职责", "任职要求"));
            entity.setParseStatus(AsyncTaskStatus.FAILED);
            entity.setParseError(truncate(e.getMessage()));
            entity.setParseFinishedAt(LocalDateTime.now());
            throw e;
        } finally {
            jobDescriptionRepository.save(entity);
        }
    }

    private JobDescriptionDTO enqueueAndSave(
        String rawText,
        String hash,
        JobDescriptionEntity.SourceType sourceType,
        String sourceUrl,
        String storageKey
    ) {
        return enqueueAndSave(rawText, hash, sourceType, sourceUrl, storageKey, null);
    }

    private JobDescriptionDTO enqueueAndSave(
        String rawText,
        String hash,
        JobDescriptionEntity.SourceType sourceType,
        String sourceUrl,
        String storageKey,
        String originalFilename
    ) {
        JobDescriptionEntity entity = new JobDescriptionEntity();
        entity.setJdHash(hash);
        entity.setSourceType(sourceType);
        entity.setSourceUrl(sourceUrl);
        entity.setStorageKey(storageKey);
        entity.setOriginalFilename(originalFilename);
        entity.setRawJdText(rawText);
        entity.setJobTitle(inferTitle(rawText));
        entity.setTechnicalSkillsJson("[]");
        entity.setParseStatus(AsyncTaskStatus.PENDING);
        entity.setParseError(null);

        JobDescriptionEntity saved = jobDescriptionRepository.save(entity);
        jdParseStreamProducer.sendParseTask(saved.getId());
        return toDto(saved);
    }

    private StructuredJd parseStructured(String rawText) {
        BeanOutputConverter<StructuredJd> converter = new BeanOutputConverter<>(StructuredJd.class);
        String systemPrompt = parseSystemTemplate.render() + "\n\n" + converter.getFormat();
        String userPrompt = parseUserTemplate.render(Map.of("jdText", rawText));

        return structuredOutputInvoker.invoke(
            chatClient,
            systemPrompt,
            userPrompt,
            converter,
            ErrorCode.JOB_DESCRIPTION_PARSE_FAILED,
            "JD 结构化解析失败: ",
            "JD 解析",
            log
        );
    }

    private String fetchAndExtractText(String url) {
        try {
            Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                .timeout((int) Duration.ofSeconds(6).toMillis())
                .get();

            // HTML 去噪，避免无关内容进入 LLM。
            document.select("script,style,noscript,svg,canvas,nav,footer,header,aside,form").remove();
            document.select("[class*=ad],[id*=ad],[class*=banner],[class*=recommend]").remove();

            Element main = selectMainContent(document, url);
            String text = main != null ? main.text() : document.body().text();
            text = sanitizeExtractedText(text);
            if (text.length() < minExtractedLength) {
                throw new BusinessException(ErrorCode.JOB_URL_FETCH_FAILED, "URL 抽取内容过短，建议切换文本或文件输入");
            }
            return text;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.JOB_URL_FETCH_FAILED, "URL 抓取失败: " + e.getMessage());
        }
    }

    private Element selectMainContent(Document document, String url) {
        String host = extractHost(url);
        if (host.contains("jobs.bytedance.com")) {
            Element el = pickBestBySelectors(document, List.of(
                ".job-detail-content", ".job-description", ".job-posting-content", "main", "article"
            ));
            if (el != null) {
                return el;
            }
        }
        if (host.contains("zhipin.com")) {
            Element el = pickBestBySelectors(document, List.of(
                ".job-sec", ".job-detail", ".job-box", ".job-detail-wrap", "main"
            ));
            if (el != null) {
                return el;
            }
        }
        if (host.contains("jobs.meituan.com") || host.contains("zhaopin.meituan.com")) {
            Element el = pickBestBySelectors(document, List.of(
                ".job-content", ".job-description", ".position-content", "main", "article"
            ));
            if (el != null) {
                return el;
            }
        }

        return pickBestBySelectors(document, List.of(
            "main", "article", ".job-detail", ".job-description", ".job-content", ".position-content", ".content"
        ));
    }

    private Element pickBestBySelectors(Document document, List<String> selectors) {
        Element best = null;
        int bestLen = 0;
        for (String selector : selectors) {
            for (Element candidate : document.select(selector)) {
                int len = sanitizeExtractedText(candidate.text()).length();
                if (len > bestLen) {
                    bestLen = len;
                    best = candidate;
                }
            }
        }
        return best;
    }

    private String sanitizeExtractedText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text
            .replace('\u00A0', ' ')
            .replaceAll("\\s+", " ")
            .trim();
        if (normalized.length() <= maxJdTextLength) {
            return normalized;
        }
        return normalized.substring(0, maxJdTextLength);
    }

    private String extractHost(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    private String inferTitle(String text) {
        if (text == null || text.isBlank()) {
            return "未命名岗位";
        }
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String candidate = line.trim();
            if (candidate.length() >= 4 && candidate.length() <= 40) {
                return candidate;
            }
        }
        return "未命名岗位";
    }

    private List<String> inferSkills(String text) {
        List<String> result = new ArrayList<>();
        for (String hint : SKILL_HINTS) {
            if (text.contains(hint)) {
                result.add(hint);
            }
        }
        return result;
    }

    private String extractBlock(String text, String keyword, String fallbackKeyword) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int start = text.indexOf(keyword);
        if (start < 0) {
            return "";
        }
        int end = fallbackKeyword == null ? -1 : text.indexOf(fallbackKeyword, start + keyword.length());
        if (end > start) {
            return text.substring(start, Math.min(end, start + 1200));
        }
        return text.substring(start, Math.min(text.length(), start + 1200));
    }

    private String toJsonSkills(List<String> skills) {
        try {
            return objectMapper.writeValueAsString(skills == null ? List.of() : skills);
        } catch (Exception e) {
            return "[]";
        }
    }

    private JobDescriptionDTO toDto(JobDescriptionEntity entity) {
        List<String> skills = List.of();
        try {
            skills = objectMapper.readValue(entity.getTechnicalSkillsJson(), new TypeReference<>() {
                });
        } catch (Exception ignored) {
        }

        return new JobDescriptionDTO(
            entity.getId(),
            entity.getJobTitle(),
            entity.getCompanyName(),
            entity.getSalaryRange(),
            entity.getWorkLocation(),
            entity.getExperienceYears(),
            skills,
            entity.getCoreRequirements(),
            entity.getResponsibilities(),
            entity.getBonusPoints(),
            entity.getSourceType(),
            entity.getSourceUrl(),
            entity.getParseStatus(),
            entity.getParseError(),
            entity.getParseStartedAt(),
            entity.getParseFinishedAt(),
            entity.getParseRetryCount(),
            entity.getCreatedAt()
        );
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private String defaultValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private record StructuredJd(
        String jobTitle,
        String companyName,
        String salaryRange,
        String workLocation,
        String educationRequirement,
        String experienceYears,
        List<String> technicalSkills,
        String coreRequirements,
        String responsibilities,
        String bonusPoints
    ) {
    }
}

package interview.guide.modules.jobmatching.listener;

import interview.guide.common.async.AbstractStreamConsumer;
import interview.guide.common.constant.AsyncTaskStreamConstants;
import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.infrastructure.redis.RedisService;
import interview.guide.modules.jobmatching.repository.JobMatchingAnalysisRepository;
import interview.guide.modules.jobmatching.service.JobMatchingService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.stream.StreamMessageId;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 岗位匹配分析任务消费者。
 */
@Slf4j
@Component
public class JobMatchingAnalyzeStreamConsumer extends AbstractStreamConsumer<JobMatchingAnalyzeStreamConsumer.JobMatchingPayload> {

    private final JobMatchingService jobMatchingService;
    private final JobMatchingAnalysisRepository analysisRepository;
    private final JobMatchingAnalyzeStreamProducer producer;

    record JobMatchingPayload(Long matchingId, Integer durationWeeks, int retryCount) {
    }

    public JobMatchingAnalyzeStreamConsumer(
        RedisService redisService,
        JobMatchingService jobMatchingService,
        JobMatchingAnalysisRepository analysisRepository,
        JobMatchingAnalyzeStreamProducer producer
    ) {
        super(redisService);
        this.jobMatchingService = jobMatchingService;
        this.analysisRepository = analysisRepository;
        this.producer = producer;
    }

    @Override
    protected String taskDisplayName() {
        return "岗位匹配分析";
    }

    @Override
    protected String streamKey() {
        return AsyncTaskStreamConstants.JOB_MATCHING_ANALYZE_STREAM_KEY;
    }

    @Override
    protected String groupName() {
        return AsyncTaskStreamConstants.JOB_MATCHING_ANALYZE_GROUP_NAME;
    }

    @Override
    protected String consumerPrefix() {
        return AsyncTaskStreamConstants.JOB_MATCHING_ANALYZE_CONSUMER_PREFIX;
    }

    @Override
    protected String threadName() {
        return "job-matching-analyze-consumer";
    }

    @Override
    protected JobMatchingPayload parsePayload(StreamMessageId messageId, Map<String, String> data) {
        String matchingId = data.get(AsyncTaskStreamConstants.FIELD_MATCHING_ID);
        if (matchingId == null) {
            log.warn("岗位匹配消息格式错误，跳过: messageId={}", messageId);
            return null;
        }
        Integer durationWeeks = parseDurationWeeks(data.get(AsyncTaskStreamConstants.FIELD_DURATION_WEEKS));
        int retryCount = parseRetryCount(data);
        return new JobMatchingPayload(Long.parseLong(matchingId), durationWeeks, retryCount);
    }

    @Override
    protected String payloadIdentifier(JobMatchingPayload payload) {
        return "matchingId=" + payload.matchingId();
    }

    @Override
    protected void markProcessing(JobMatchingPayload payload) {
        analysisRepository.findById(payload.matchingId()).ifPresent(analysis -> {
            analysis.setAnalyzeStatus(AsyncTaskStatus.PROCESSING);
            analysis.setAnalyzeError(null);
            analysis.setCompletedAt(null);
            analysisRepository.save(analysis);
        });
    }

    @Override
    protected void processBusiness(JobMatchingPayload payload) {
        jobMatchingService.processPendingMatching(payload.matchingId(), payload.durationWeeks());
    }

    @Override
    protected void markCompleted(JobMatchingPayload payload) {
        // 完成状态由 service.processPendingMatching 统一写入。
    }

    @Override
    protected void markFailed(JobMatchingPayload payload, String error) {
        analysisRepository.findById(payload.matchingId()).ifPresent(analysis -> {
            analysis.setAnalyzeStatus(AsyncTaskStatus.FAILED);
            analysis.setAnalyzeError(error);
            analysis.setCompletedAt(LocalDateTime.now());
            analysisRepository.save(analysis);
        });
    }

    @Override
    protected void retryMessage(JobMatchingPayload payload, int retryCount) {
        producer.sendRetryTask(payload.matchingId(), payload.durationWeeks(), retryCount);
    }

    private Integer parseDurationWeeks(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

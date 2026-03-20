package interview.guide.modules.jobmatching.listener;

import interview.guide.common.async.AbstractStreamProducer;
import interview.guide.common.constant.AsyncTaskStreamConstants;
import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.infrastructure.redis.RedisService;
import interview.guide.modules.jobmatching.repository.JobMatchingAnalysisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 岗位匹配分析任务生产者。
 */
@Slf4j
@Component
public class JobMatchingAnalyzeStreamProducer extends AbstractStreamProducer<JobMatchingAnalyzeStreamProducer.JobMatchingPayload> {

    private final JobMatchingAnalysisRepository analysisRepository;

    public record JobMatchingPayload(Long matchingId, Integer durationWeeks, int retryCount) {
    }

    public JobMatchingAnalyzeStreamProducer(
        RedisService redisService,
        JobMatchingAnalysisRepository analysisRepository
    ) {
        super(redisService);
        this.analysisRepository = analysisRepository;
    }

    /**
     * 发送匹配分析任务。
     */
    public void sendAnalyzeTask(Long matchingId, Integer durationWeeks) {
        sendTask(new JobMatchingPayload(matchingId, durationWeeks, 0));
    }

    /**
     * 发送重试任务。
     */
    public void sendRetryTask(Long matchingId, Integer durationWeeks, int retryCount) {
        sendTask(new JobMatchingPayload(matchingId, durationWeeks, retryCount));
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
    protected Map<String, String> buildMessage(JobMatchingPayload payload) {
        return Map.of(
            AsyncTaskStreamConstants.FIELD_MATCHING_ID, payload.matchingId().toString(),
            AsyncTaskStreamConstants.FIELD_DURATION_WEEKS, payload.durationWeeks() == null ? "" : payload.durationWeeks().toString(),
            AsyncTaskStreamConstants.FIELD_RETRY_COUNT, String.valueOf(payload.retryCount())
        );
    }

    @Override
    protected String payloadIdentifier(JobMatchingPayload payload) {
        return "matchingId=" + payload.matchingId();
    }

    @Override
    protected void onSendFailed(JobMatchingPayload payload, String error) {
        analysisRepository.findById(payload.matchingId()).ifPresent(analysis -> {
            analysis.setAnalyzeStatus(AsyncTaskStatus.FAILED);
            analysis.setAnalyzeError(truncateError(error));
            analysis.setCompletedAt(LocalDateTime.now());
            analysisRepository.save(analysis);
        });
    }
}

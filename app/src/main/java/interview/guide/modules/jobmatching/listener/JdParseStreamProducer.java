package interview.guide.modules.jobmatching.listener;

import interview.guide.common.async.AbstractStreamProducer;
import interview.guide.common.constant.AsyncTaskStreamConstants;
import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.infrastructure.redis.RedisService;
import interview.guide.modules.jobmatching.repository.JobDescriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JD 解析任务生产者。
 */
@Slf4j
@Component
public class JdParseStreamProducer extends AbstractStreamProducer<JdParseStreamProducer.JdParsePayload> {

    private final JobDescriptionRepository jobDescriptionRepository;

    public record JdParsePayload(Long jdId, int retryCount) {
    }

    public JdParseStreamProducer(RedisService redisService, JobDescriptionRepository jobDescriptionRepository) {
        super(redisService);
        this.jobDescriptionRepository = jobDescriptionRepository;
    }

    /**
     * 发送 JD 解析任务。
     */
    public void sendParseTask(Long jdId) {
        sendTask(new JdParsePayload(jdId, 0));
    }

    /**
     * 发送重试任务。
     */
    public void sendRetryTask(Long jdId, int retryCount) {
        sendTask(new JdParsePayload(jdId, retryCount));
    }

    @Override
    protected String taskDisplayName() {
        return "JD解析";
    }

    @Override
    protected String streamKey() {
        return AsyncTaskStreamConstants.JD_PARSE_STREAM_KEY;
    }

    @Override
    protected Map<String, String> buildMessage(JdParsePayload payload) {
        return Map.of(
            AsyncTaskStreamConstants.FIELD_JD_ID, payload.jdId().toString(),
            AsyncTaskStreamConstants.FIELD_RETRY_COUNT, String.valueOf(payload.retryCount())
        );
    }

    @Override
    protected String payloadIdentifier(JdParsePayload payload) {
        return "jdId=" + payload.jdId();
    }

    @Override
    protected void onSendFailed(JdParsePayload payload, String error) {
        jobDescriptionRepository.findById(payload.jdId()).ifPresent(jd -> {
            jd.setParseStatus(AsyncTaskStatus.FAILED);
            jd.setParseError(truncateError(error));
            jd.setParseFinishedAt(LocalDateTime.now());
            jd.setParseRetryCount(payload.retryCount());
            jobDescriptionRepository.save(jd);
        });
    }
}

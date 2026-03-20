package interview.guide.modules.jobmatching.listener;

import interview.guide.common.async.AbstractStreamConsumer;
import interview.guide.common.constant.AsyncTaskStreamConstants;
import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.infrastructure.redis.RedisService;
import interview.guide.modules.jobmatching.repository.JobDescriptionRepository;
import interview.guide.modules.jobmatching.service.JobDescriptionService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.stream.StreamMessageId;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JD 解析任务消费者。
 */
@Slf4j
@Component
public class JdParseStreamConsumer extends AbstractStreamConsumer<JdParseStreamConsumer.JdParsePayload> {

    private final JobDescriptionService jobDescriptionService;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final JdParseStreamProducer producer;

    record JdParsePayload(Long jdId, int retryCount) {
    }

    public JdParseStreamConsumer(
        RedisService redisService,
        JobDescriptionService jobDescriptionService,
        JobDescriptionRepository jobDescriptionRepository,
        JdParseStreamProducer producer
    ) {
        super(redisService);
        this.jobDescriptionService = jobDescriptionService;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.producer = producer;
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
    protected String groupName() {
        return AsyncTaskStreamConstants.JD_PARSE_GROUP_NAME;
    }

    @Override
    protected String consumerPrefix() {
        return AsyncTaskStreamConstants.JD_PARSE_CONSUMER_PREFIX;
    }

    @Override
    protected String threadName() {
        return "jd-parse-consumer";
    }

    @Override
    protected JdParsePayload parsePayload(StreamMessageId messageId, Map<String, String> data) {
        String jdId = data.get(AsyncTaskStreamConstants.FIELD_JD_ID);
        if (jdId == null) {
            log.warn("JD消息格式错误，跳过: messageId={}", messageId);
            return null;
        }
        int retryCount = parseRetryCount(data);
        return new JdParsePayload(Long.parseLong(jdId), retryCount);
    }

    @Override
    protected String payloadIdentifier(JdParsePayload payload) {
        return "jdId=" + payload.jdId();
    }

    @Override
    protected void markProcessing(JdParsePayload payload) {
        jobDescriptionRepository.findById(payload.jdId()).ifPresent(jd -> {
            if (jd.getDeletedAt() != null) {
                return;
            }
            jd.setParseStatus(AsyncTaskStatus.PROCESSING);
            jd.setParseStartedAt(LocalDateTime.now());
            jd.setParseError(null);
            jd.setParseRetryCount(payload.retryCount());
            jobDescriptionRepository.save(jd);
        });
    }

    @Override
    protected void processBusiness(JdParsePayload payload) {
        jobDescriptionService.parsePending(payload.jdId());
    }

    @Override
    protected void markCompleted(JdParsePayload payload) {
        // 成功状态由 service.parsePending 统一写入，这里无需重复更新。
    }

    @Override
    protected void markFailed(JdParsePayload payload, String error) {
        jobDescriptionRepository.findById(payload.jdId()).ifPresent(jd -> {
            if (jd.getDeletedAt() != null) {
                return;
            }
            jd.setParseStatus(AsyncTaskStatus.FAILED);
            jd.setParseError(error);
            jd.setParseFinishedAt(LocalDateTime.now());
            jd.setParseRetryCount(payload.retryCount());
            jobDescriptionRepository.save(jd);
        });
    }

    @Override
    protected void retryMessage(JdParsePayload payload, int retryCount) {
        producer.sendRetryTask(payload.jdId(), retryCount);
    }
}

package interview.guide.modules.jobmatching;

import interview.guide.common.result.Result;
import interview.guide.modules.interview.model.InterviewSessionDTO;
import interview.guide.modules.jobmatching.model.*;
import interview.guide.modules.jobmatching.service.JobDescriptionService;
import interview.guide.modules.jobmatching.service.JobMatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 岗位匹配模块控制器。
 * 提供 JD 录入、岗位匹配、学习路径打卡以及面试联动接口。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class JobMatchingController {

    private final JobDescriptionService jobDescriptionService;
    private final JobMatchingService jobMatchingService;

    /**
     * 通过纯文本创建 JD。
     *
     * @param request JD 文本请求
     * @return 结构化 JD 信息
     */
    @PostMapping("/api/job-descriptions/from-text")
    public Result<JobDescriptionDTO> createJdFromText(@Valid @RequestBody JdTextUploadRequest request) {
        return Result.success(jobDescriptionService.createFromText(request));
    }

    /**
     * 通过 URL 抓取并创建 JD。
     *
     * @param request JD URL 请求
     * @return 结构化 JD 信息
     */
    @PostMapping("/api/job-descriptions/from-url")
    public Result<JobDescriptionDTO> createJdFromUrl(@Valid @RequestBody JdUrlUploadRequest request) {
        return Result.success(jobDescriptionService.createFromUrl(request));
    }

    /**
     * 通过文件上传创建 JD。
     *
     * @param file JD 文件
     * @return 结构化 JD 信息
     */
    @PostMapping(value = "/api/job-descriptions/from-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<JobDescriptionDTO> createJdFromFile(@RequestParam("file") MultipartFile file) {
        return Result.success(jobDescriptionService.createFromFile(file));
    }

    /**
     * 查询单个 JD 详情。
     *
     * @param id JD ID
     * @return JD 详情
     */
    @GetMapping("/api/job-descriptions/{id}")
    public Result<JobDescriptionDTO> getJobDescription(@PathVariable Long id) {
        return Result.success(jobDescriptionService.getById(id));
    }

    /**
     * 查询 JD 库列表（未软删除）。
     */
    @GetMapping("/api/job-descriptions")
    public Result<List<JobDescriptionDTO>> listJobDescriptions() {
        return Result.success(jobDescriptionService.listActive());
    }

    /**
     * 软删除 JD。
     */
    @DeleteMapping("/api/job-descriptions/{id}")
    public Result<Void> deleteJobDescription(@PathVariable Long id) {
        jobDescriptionService.softDelete(id);
        return Result.success(null);
    }

    /**
     * 重试 JD 解析。
     */
    @PostMapping("/api/job-descriptions/{id}/retry")
    public Result<JobDescriptionDTO> retryJobDescriptionParse(@PathVariable Long id) {
        return Result.success(jobDescriptionService.retryParse(id));
    }

    /**
     * 创建岗位匹配分析任务并异步执行。
     *
     * @param request 岗位匹配请求
     * @return 新建匹配记录
     */
    @PostMapping("/api/job-matchings")
    public Result<JobMatchingRecordDTO> createMatching(@Valid @RequestBody CreateJobMatchingRequest request) {
        log.info("开始岗位匹配分析: resumeId={}, jdId={}", request.resumeId(), request.jdId());
        return Result.success(jobMatchingService.createAndEnqueue(request));
    }

    /**
     * 查询岗位匹配报告。
     *
     * @param id 匹配分析 ID
     * @return 岗位匹配报告
     */
    @GetMapping("/api/job-matchings/{id}")
    public Result<JobMatchingReportDTO> getMatchingReport(@PathVariable Long id) {
        return Result.success(jobMatchingService.getReport(id));
    }

    /**
     * 查询指定 JD 的匹配记录列表。
     *
     * @param jdId JD ID
     * @return 匹配记录列表
     */
    @GetMapping("/api/job-descriptions/{jdId}/matchings")
    public Result<List<JobMatchingRecordDTO>> listMatchingsByJd(@PathVariable Long jdId) {
        return Result.success(jobMatchingService.listMatchingsByJdId(jdId));
    }

    /**
     * 标记周计划任务完成并回传学习路径最新进度。
     *
     * @param planId 周计划 ID
     * @param taskIndex 任务索引
     * @param request 打卡请求
     * @return 更新后的学习路径
     */
    @PostMapping("/api/learning-paths/plans/{planId}/tasks/{taskIndex}/complete")
    public Result<LearningPathDTO> completeTask(
        @PathVariable Long planId,
        @PathVariable Integer taskIndex,
        @RequestBody(required = false) TaskCompletionRequest request
    ) {
        return Result.success(jobMatchingService.completeTask(planId, taskIndex, request));
    }

    /**
     * 基于岗位匹配结果创建联动模拟面试。
     *
     * @param id 匹配分析 ID
     * @param request 面试生成请求
     * @return 面试会话信息
     */
    @PostMapping("/api/job-matchings/{id}/generate-interview")
    public Result<InterviewSessionDTO> generateInterview(
        @PathVariable Long id,
        @RequestBody(required = false) GenerateInterviewFromMatchingRequest request
    ) {
        return Result.success(jobMatchingService.generateInterviewFromMatching(id, request));
    }
}

package interview.guide.modules.jobmatching.model;

import jakarta.validation.constraints.NotBlank;

/**
 * URL 方式上传 JD 请求。
 */
public record JdUrlUploadRequest(
    @NotBlank(message = "URL 不能为空")
    String url
) {
}

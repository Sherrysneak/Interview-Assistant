package interview.guide.modules.jobmatching.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 文本方式上传 JD 请求。
 */
public record JdTextUploadRequest(
    @NotBlank(message = "JD 文本不能为空")
    String jdText,
    String jobTitleHint,
    String companyNameHint
) {
}

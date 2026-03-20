package interview.guide.modules.system;

import interview.guide.common.result.Result;
import interview.guide.modules.system.model.RuntimeInfoDTO;
import interview.guide.modules.system.service.SystemInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统信息控制器。
 */
@RestController
@RequiredArgsConstructor
public class SystemInfoController {

    private final SystemInfoService systemInfoService;

    @GetMapping("/api/system/runtime-info")
    public Result<RuntimeInfoDTO> getRuntimeInfo() {
        return Result.success(systemInfoService.getRuntimeInfo());
    }
}

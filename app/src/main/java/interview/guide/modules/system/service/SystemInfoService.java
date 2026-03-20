package interview.guide.modules.system.service;

import interview.guide.modules.system.model.RuntimeInfoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 系统运行时信息服务。
 */
@Service
public class SystemInfoService {

    private final String chatModel;

    public SystemInfoService(@Value("${spring.ai.openai.chat.options.model:${AI_MODEL:qwen-plus}}") String chatModel) {
        this.chatModel = chatModel;
    }

    public RuntimeInfoDTO getRuntimeInfo() {
        return new RuntimeInfoDTO(chatModel);
    }
}

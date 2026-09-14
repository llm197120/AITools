package org.jeecg.modules.homeai.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 回收站批量恢复 / 彻底删除请求体。
 * 使用普通 DTO 而非 JsonNode，避免 Spring Boot 4 / Jackson 无法定义 JsonNode 类型。
 */
@Data
@Schema(description = "资料回收站批量操作")
public class StorageRecycleBatchRequest {
    @Schema(description = "文件ID列表")
    private List<String> fileIds;

    @Schema(description = "文件夹ID列表")
    private List<String> folderIds;
}

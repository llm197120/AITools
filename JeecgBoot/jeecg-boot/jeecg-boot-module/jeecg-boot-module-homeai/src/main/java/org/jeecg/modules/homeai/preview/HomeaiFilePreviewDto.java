package org.jeecg.modules.homeai.preview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 资料 / 学习资料统一预览描述
 */
@Data
@Schema(description = "文件预览")
public class HomeaiFilePreviewDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "image/video/audio/pdf/office/text/archive/link/unknown")
    private String kind;

    @Schema(description = "源文件访问 URL")
    private String fileUrl;

    @Schema(description = "Office 转 PDF 后的预览 URL")
    private String previewPdfUrl;

    @Schema(description = "展示文件名")
    private String fileName;

    @Schema(description = "扩展名")
    private String extension;

    @Schema(description = "转换任务 ID（office 且尚未有缓存时）")
    private String convertTaskId;

    @Schema(description = "PENDING/PROCESSING/COMPLETED/FAILED")
    private String convertStatus;

    @Schema(description = "转换失败原因")
    private String errorMessage;
}

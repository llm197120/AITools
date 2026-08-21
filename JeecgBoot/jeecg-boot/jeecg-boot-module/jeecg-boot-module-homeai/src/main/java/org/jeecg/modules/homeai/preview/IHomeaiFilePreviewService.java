package org.jeecg.modules.homeai.preview;

import org.jeecg.modules.homeai.recipe.entity.LearnMaterial;
import org.jeecg.modules.homeai.storage.entity.StorageConvertTask;
import org.jeecg.modules.homeai.storage.entity.StorageFile;

/**
 * 资料 / 学习资料预览
 */
public interface IHomeaiFilePreviewService {

    String CONVERT_PREVIEW_PDF = "preview_pdf";
    String CONVERT_PREVIEW_PDF_LEARN = "preview_pdf_learn";

    HomeaiFilePreviewDto previewStorage(StorageFile file);

    HomeaiFilePreviewDto previewLearn(LearnMaterial material);

    /** 提交或复用 Office→PDF 任务，并回填 preview 描述 */
    HomeaiFilePreviewDto ensureStoragePreviewPdf(String userId, StorageFile file);

    HomeaiFilePreviewDto ensureLearnPreviewPdf(String userId, LearnMaterial material);

    HomeaiFilePreviewDto refreshByTask(HomeaiFilePreviewDto preview, StorageConvertTask task);
}

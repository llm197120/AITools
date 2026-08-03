package org.jeecg.modules.homeai.storage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.query.QueryGenerator;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.modules.homeai.storage.entity.OfficeTemplate;
import org.jeecg.modules.homeai.storage.service.IOfficeTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 文档模板管理（管理端）
 */
@Slf4j
@RestController
@RequestMapping("/homeai/storage/template")
public class OfficeTemplateController {

    @Autowired
    private IOfficeTemplateService templateService;

    @Value("${jeecg.path.upload:./upload}")
    private String uploadPath;

    /** 模板列表 */
    @GetMapping("/list")
    @Operation(summary="文档模板-分页列表查询")
    @RequiresPermissions("homeai:storage:template:list")
    public Result<?> list(OfficeTemplate template,
                          @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                          HttpServletRequest req) {
        QueryWrapper<OfficeTemplate> queryWrapper = QueryGenerator.initQueryWrapper(template, req.getParameterMap());
        Page<OfficeTemplate> page = new Page<>(pageNo, pageSize);
        IPage<OfficeTemplate> pageList = templateService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /** 新增模板 */
    @PostMapping
    @AutoLog(value="文档模板-新增")
    @Operation(summary="文档模板-新增")
    @RequiresPermissions("homeai:storage:template:add")
    public Result<?> add(@RequestBody OfficeTemplate template) {
        templateService.save(template);
        return Result.OK("新增成功");
    }

    /** 编辑模板 */
    @PutMapping
    @AutoLog(value="文档模板-编辑")
    @Operation(summary="文档模板-编辑")
    @RequiresPermissions("homeai:storage:template:edit")
    public Result<?> edit(@RequestBody OfficeTemplate template) {
        templateService.updateById(template);
        return Result.OK("编辑成功");
    }

    /** 删除模板 */
    @DeleteMapping("/{id}")
    @AutoLog(value="文档模板-删除")
    @Operation(summary="文档模板-删除")
    @RequiresPermissions("homeai:storage:template:delete")
    public Result<?> delete(@PathVariable String id) {
        templateService.removeById(id);
        return Result.OK("删除成功");
    }

    /** 设为默认 */
    @PutMapping("/{id}/default")
    @AutoLog(value="文档模板-设为默认")
    @Operation(summary="文档模板-设为默认")
    @RequiresPermissions("homeai:storage:template:edit")
    public Result<?> setDefault(@PathVariable String id) {
        templateService.setDefault(id);
        return Result.OK("设置成功");
    }

    /** 上传模板文件 */
    @PostMapping("/{id}/upload")
    @AutoLog(value="文档模板-上传文件")
    @Operation(summary="文档模板-上传文件")
    @RequiresPermissions("homeai:storage:template:edit")
    public Result<?> uploadFile(@PathVariable String id, @RequestParam MultipartFile file) {
        try {
            String dir = uploadPath + "/homeai/template/";
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(dir));
            String original = file.getOriginalFilename();
            String ext = original != null && original.contains(".")
                    ? original.substring(original.lastIndexOf(".")) : "";
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            file.transferTo(java.nio.file.Path.of(dir + fileName).toFile());
            String fileUrl = org.jeecg.modules.homeai.config.HomeaiFileUrlUtil.toAbsoluteUrl(
                    "/upload/homeai/template/" + fileName);
            OfficeTemplate template = templateService.getById(id);
            if (template != null) {
                template.setFileUrl(fileUrl);
                templateService.updateById(template);
            }
            return Result.OK(fileUrl);
        } catch (Exception e) {
            log.error("模板文件上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /** 获取启用的模板（小程序端） */
    @GetMapping("/enabled")
    public Result<?> getEnabled(@RequestParam(required = false) String type) {
        return Result.OK(templateService.getEnabledTemplates(type));
    }
}

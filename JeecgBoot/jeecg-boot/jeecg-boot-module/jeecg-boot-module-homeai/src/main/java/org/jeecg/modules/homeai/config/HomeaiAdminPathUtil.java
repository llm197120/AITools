package org.jeecg.modules.homeai.config;

import org.jeecg.common.util.oConvertUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 管理端路径判定（须控制台 JWT）。
 * <p>
 * 从 {@link HomeaiAuthInterceptor} 抽出以便单测；新增管理端接口须同步登记。
 * </p>
 */
public final class HomeaiAdminPathUtil {

    private HomeaiAdminPathUtil() {
    }

    /** 精确前缀：path 等于该项或以其 + "/" 开头则视为管理端 */
    static final List<String> ADMIN_PREFIXES = Arrays.asList(
            "/homeai/user/list",
            "/homeai/user/exportXls",
            "/homeai/user/exportTemplate",
            "/homeai/user/importExcel",
            "/homeai/user/recycleBin",
            "/homeai/user/moveToRecycleBin",
            "/homeai/user/restore",
            "/homeai/user/deletePermanently",
            "/homeai/family/list",
            "/homeai/family/admin",
            "/homeai/family/add",
            "/homeai/family/exportXls",
            "/homeai/family/exportTemplate",
            "/homeai/family/importExcel",
            "/homeai/family/recycleBin",
            "/homeai/family/moveToRecycleBin",
            "/homeai/family/restore",
            "/homeai/family/deletePermanently",
            "/homeai/bill/list",
            "/homeai/bill/add",
            "/homeai/bill/exportXls",
            "/homeai/bill/importExcel",
            "/homeai/bill/recycleBin",
            "/homeai/bill/moveToRecycleBin",
            "/homeai/bill/restore",
            "/homeai/bill/deletePermanently",
            //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R24】管理端账单统计须控制台 JWT-----------
            "/homeai/bill/admin",
            //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R24】管理端账单统计须控制台 JWT-----------
            "/homeai/plan/list",
            "/homeai/plan/add",
            "/homeai/plan/exportXls",
            "/homeai/plan/importExcel",
            "/homeai/plan/recycleBin",
            "/homeai/plan/moveToRecycleBin",
            "/homeai/plan/restore",
            "/homeai/plan/deletePermanently",
            "/homeai/plan/category-list",
            "/homeai/plan/category",
            "/homeai/plan/admin/completion",
            "/homeai/plan/admin/calendar",
            "/homeai/plan/admin/date",
            //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R24】补跑接口须控制台 JWT-----------
            "/homeai/plan/admin/repeat",
            //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R24】补跑接口须控制台 JWT-----------
            "/homeai/dashboard/plan-learn",
            "/homeai/recipe/add",
            "/homeai/recipe/exportXls",
            "/homeai/recipe/exportTemplate",
            "/homeai/recipe/importExcel",
            //update-begin---author:cursor ---date:2026-08-13 for：【菜谱导入】批量导入封面（管理端）-----------
            "/homeai/recipe/import-covers",
            //update-end---author:cursor ---date:2026-08-13 for：【菜谱导入】批量导入封面（管理端）-----------
            "/homeai/recipe/recycleBin",
            "/homeai/recipe/moveToRecycleBin",
            "/homeai/recipe/restore",
            "/homeai/recipe/deletePermanently",
            "/homeai/recipe/category/list",
            "/homeai/learn/addMaterial",
            "/homeai/learn/exportXls",
            "/homeai/learn/importExcel",
            "/homeai/learn/recycleBin",
            "/homeai/learn/moveToRecycleBin",
            "/homeai/learn/restore",
            "/homeai/learn/deletePermanently",
            "/homeai/learn/category/list",
            //update-begin---author:copilot ---date:2026-08-12 for：【第15轮】学习按分类统计-----------
            "/homeai/learn/admin",
            //update-end---author:copilot ---date:2026-08-12 for：【第15轮】学习按分类统计-----------
            "/homeai/ai/key-config",
            "/homeai/ai/conversations/list",
            "/homeai/storage/folder-list",
            "/homeai/storage/file-list",
            "/homeai/storage/office/list",
            //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】资料回收站与审计管理端入口-----------
            "/homeai/storage/recycleBin",
            "/homeai/storage/restore",
            "/homeai/storage/deletePermanently",
            "/homeai/audit",
            //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】资料回收站与审计管理端入口-----------
            //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R69】APP 版本管理端-----------
            "/homeai/app/version/admin",
            "/homeai/app/version/upload"
            //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R69】APP 版本管理端-----------
    );

    /**
     * 是否必须走控制台 JWT。
     *
     * @param path   去掉 contextPath 后的 URI
     * @param method HTTP 方法，可为 null
     */
    public static boolean isAdminPath(String path, String method) {
        if (oConvertUtils.isEmpty(path)) {
            return false;
        }
        String m = method == null ? "" : method.toUpperCase();
        for (String prefix : ADMIN_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }

        if (path.startsWith("/homeai/user/")) {
            String rest = path.substring("/homeai/user/".length());
            if (oConvertUtils.isNotEmpty(rest)) {
                String first = rest.split("/")[0];
                if (!first.equals("info") && !first.equals("login") && !first.equals("refresh-token")) {
                    return true;
                }
            }
        }
        if (path.equals("/homeai/user") && "POST".equals(m)) {
            return true;
        }

        if ("PUT".equals(m)) {
            if (path.matches("/homeai/family/[^/]+")) return true;
            if (path.matches("/homeai/bill/[^/]+")
                    && !path.equals("/homeai/bill/entry") && !path.equals("/homeai/bill/category")) return true;
            if (path.matches("/homeai/plan/[^/]+") && !path.startsWith("/homeai/plan/instance/")) return true;
            if (path.matches("/homeai/recipe/[^/]+") && !path.matches("/homeai/recipe/[^/]+/video")) return true;
            if (path.matches("/homeai/learn/material/[^/]+")) return true;
        }

        if (path.startsWith("/homeai/storage/rule/")) {
            String rest = path.substring("/homeai/storage/rule/".length());
            if (!rest.equals("targets")) return true;
        }
        if (path.equals("/homeai/storage/rule") && ("POST".equals(m) || "PUT".equals(m))) return true;
        if (path.startsWith("/homeai/storage/template/")) {
            String rest = path.substring("/homeai/storage/template/".length());
            if (!rest.equals("enabled")) return true;
        }
        if (path.equals("/homeai/storage/template") && ("POST".equals(m) || "PUT".equals(m))) return true;
        if (path.startsWith("/homeai/recipe/category/")) {
            String rest = path.substring("/homeai/recipe/category/".length());
            if (!rest.equals("all")) return true;
        }
        if (path.equals("/homeai/recipe/category") && ("POST".equals(m) || "PUT".equals(m))) return true;
        if (path.startsWith("/homeai/learn/category/")) {
            String rest = path.substring("/homeai/learn/category/".length());
            if (!rest.equals("all")) return true;
        }
        if (path.equals("/homeai/learn/category") && ("POST".equals(m) || "PUT".equals(m))) return true;
        if (path.equals("/homeai/config/file-whitelist") && "PUT".equals(m)) return true;
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】存储配额配置管理端-----------
        if (path.equals("/homeai/config/storage") && ("PUT".equals(m) || "GET".equals(m))) return true;
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R30】家庭级配额覆盖管理端-----------
        if (path.startsWith("/homeai/config/storage/family/")) return true;
        //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R32】家庭配额看板管理端-----------
        if (path.startsWith("/homeai/config/storage/families")) return true;
        //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R32】家庭配额看板管理端-----------
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R30】家庭级配额覆盖管理端-----------
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R31】学习提醒模板联调管理端-----------
        if (path.equals("/homeai/config/wechat-learn-remind") && "GET".equals(m)) return true;
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R31】学习提醒模板联调管理端-----------
        if (path.equals("/homeai/config/plan") && ("PUT".equals(m) || "GET".equals(m))) return true;
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】存储配额配置管理端-----------
        return false;
    }
}

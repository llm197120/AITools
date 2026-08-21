# -*- coding: utf-8 -*-
# 清空现有菜谱业务数据（物理删除），分类表保留，便于重新 Excel 导入。
# 用法（在仓库根或本目录均可）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File docs/data/purge_homeai_recipes.ps1
# 可选参数：-Host 127.0.0.1 -Port 3306 -User root -Database jeecg -Password 123456

param(
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Database = "jeecg",
    [string]$Password = ""
)

$ErrorActionPreference = "Stop"
$SqlFile = Join-Path $PSScriptRoot "purge_homeai_recipes.sql"

if (-not (Test-Path -LiteralPath $SqlFile)) {
    Write-Error "找不到 SQL 文件: $SqlFile"
    exit 1
}

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
    Write-Error "mysql 未加入 PATH，请先安装 MySQL 客户端。"
    exit 1
}

if ([string]::IsNullOrEmpty($Password)) {
    $secure = Read-Host "MySQL 密码（无密码直接回车）" -AsSecureString
    $BSTR = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)
    }
}

$env:MYSQL_PWD = $Password
$mysqlArgs = @(
    "--default-character-set=utf8mb4",
    "-h$DbHost",
    "-P$Port",
    "-u$User",
    $Database
)

function Invoke-MysqlQuery([string]$Query) {
    & mysql @mysqlArgs -e $Query
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL 执行失败（exit $LASTEXITCODE）"
    }
}

Write-Host ""
Write-Host "目标库: ${DbHost}:${Port}/${Database}  用户: $User"
Write-Host "将物理删除：菜谱主表 / 食材 / 步骤 / 收藏"
Write-Host "将断开：计划表 recipe_id（计划本身保留）"
Write-Host "将保留：菜谱分类"
Write-Host ""

Write-Host "===== 删除前数量 ====="
Invoke-MysqlQuery @"
SELECT 'homeai_recipe' AS tbl, COUNT(*) AS cnt FROM homeai_recipe
UNION ALL SELECT 'homeai_recipe_ingredient', COUNT(*) FROM homeai_recipe_ingredient
UNION ALL SELECT 'homeai_recipe_step', COUNT(*) FROM homeai_recipe_step
UNION ALL SELECT 'homeai_recipe_favorite', COUNT(*) FROM homeai_recipe_favorite
UNION ALL SELECT 'plan_linked_recipe', COUNT(*) FROM homeai_plan_master
 WHERE recipe_id IS NOT NULL AND recipe_id <> ''
UNION ALL SELECT 'homeai_recipe_category', COUNT(*) FROM homeai_recipe_category;
"@

$confirm = Read-Host "确认清空全部菜谱数据？输入 YES 继续"
if ($confirm -ne "YES") {
    Write-Host "已取消。"
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    exit 0
}

Write-Host ""
Write-Host "正在执行 $SqlFile ..."
Get-Content -LiteralPath $SqlFile -Raw -Encoding UTF8 | & mysql @mysqlArgs
if ($LASTEXITCODE -ne 0) {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    Write-Error "清空失败，请检查上方 MySQL 输出。"
    exit 1
}

Write-Host ""
Write-Host "===== 删除后数量 ====="
Invoke-MysqlQuery @"
SELECT 'homeai_recipe' AS tbl, COUNT(*) AS cnt FROM homeai_recipe
UNION ALL SELECT 'homeai_recipe_ingredient', COUNT(*) FROM homeai_recipe_ingredient
UNION ALL SELECT 'homeai_recipe_step', COUNT(*) FROM homeai_recipe_step
UNION ALL SELECT 'homeai_recipe_favorite', COUNT(*) FROM homeai_recipe_favorite
UNION ALL SELECT 'plan_linked_recipe', COUNT(*) FROM homeai_plan_master
 WHERE recipe_id IS NOT NULL AND recipe_id <> ''
UNION ALL SELECT 'homeai_recipe_category', COUNT(*) FROM homeai_recipe_category;
"@

Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
Write-Host ""
Write-Host "完成。下一步：管理端 → 菜谱列表 → 导入 Excel（docs/data/homeai-recipes-import.xlsx）"
Write-Host "若分类为空，先执行 init_homeai_recipe_category.sql"

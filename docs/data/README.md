# -*- coding: utf-8 -*-
# 菜谱种子数据

## 文件

| 文件 | 说明 |
|------|------|
| `homeai-recipes-import.xlsx` | 约 300 道中餐家常菜，可直接在管理端「菜谱列表」导入 |
| `fetch_homeai_recipes.py` | 从开源项目拉取并转换的脚本 |
| `purge_homeai_recipes.sql` | 物理清空菜谱主表/食材/步骤/收藏（保留分类） |
| `purge_homeai_recipes.ps1` | 带确认的清空脚本，默认连 `127.0.0.1:3306/jeecg` |

## 数据来源

按优先级去重合并：

1. [Anduin2017/HowToCook](https://github.com/Anduin2017/HowToCook)（社区开源中文菜谱，Markdown，Unlicense）
2. [Gar-b-age/CookLikeHOC](https://github.com/Gar-b-age/CookLikeHOC)（老乡鸡菜品溯源整理）
3. [XiaChuFang Recipe Corpus](https://huggingface.co/datasets/xzm1999/XiaChuFang_Recipe_Corpus)（HuggingFace，MIT，按菜名去重补齐）

按系统 Excel 导入格式转换：菜谱名称、分类 ID、难度、用时、份数、食材、步骤、小贴士、可见性。

当前导入表已从约 1000 条中筛选为 **约 300 道中餐家常菜**（优先炒/蒸/炖/煮、家里好做；去掉西餐烘焙、鸡尾酒、猎奇/过难菜，并合并明显重复）。

## 重新生成

```bash
# 默认拉取并导出 1000 条（本地已有 HowToCook 则不重复下载）
python docs/data/fetch_homeai_recipes.py --limit 1000

# 强制重新下载 GitHub 源码
python docs/data/fetch_homeai_recipes.py --download --limit 1000
```

## 清空后重新导入

会物理删除全部菜谱及子表，并断开计划上的 `recipe_id`；**不删** `homeai_recipe_category`。执行前请自行备份。

```powershell
# 仓库根目录；提示输入密码后，必须输入 YES 才会删除
powershell -NoProfile -ExecutionPolicy Bypass -File docs/data/purge_homeai_recipes.ps1

# 或显式传连接参数（密码请按本机 MySQL 填写，勿提交真实密码）
powershell -NoProfile -ExecutionPolicy Bypass -File docs/data/purge_homeai_recipes.ps1 -Database jeecg -User root
```

也可在 Navicat / mysql 客户端对目标库直接执行 `docs/data/purge_homeai_recipes.sql`。

SQL 文件末尾附有「只删管理端导入、保留用户自建」的可选语句（默认关闭）。

## 导入步骤

1. 确认库中已有默认分类（`rc_hot` / `rc_cold` / `rc_soup` / `rc_staple` / `rc_bake` / `rc_drink` 等）
2. 管理端 → 菜谱列表 → 导入 Excel
3. 上传 `homeai-recipes-import.xlsx`
4. 导入后菜谱可见性为 `public`，可按需在列表中调整

格式说明见 [recipe-excel-import.md](../guide/recipe-excel-import.md)。

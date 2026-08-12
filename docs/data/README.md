# -*- coding: utf-8 -*-
# 菜谱种子数据

## 文件

| 文件 | 说明 |
|------|------|
| `homeai-recipes-import.xlsx` | 约 100 条常用菜谱，可直接在管理端「菜谱列表」导入 |
| `fetch_homeai_recipes.py` | 从开源项目 HowToCook 拉取并转换的脚本 |

## 数据来源

[Anduin2017/HowToCook](https://github.com/Anduin2017/HowToCook)（社区开源中文菜谱，Markdown）

按系统 Excel 导入格式转换：菜谱名称、分类 ID、难度、用时、份数、食材、步骤、小贴士、可见性。

## 重新生成

```bash
# 若本地尚无源码，会自动下载 zip 到项目 tmp/
python docs/data/fetch_homeai_recipes.py --limit 100

# 强制重新下载
python docs/data/fetch_homeai_recipes.py --download --limit 100
```

## 导入步骤

1. 确认库中已有默认分类（`rc_hot` / `rc_cold` / `rc_soup` / `rc_staple` / `rc_bake` / `rc_drink` 等）
2. 管理端 → 菜谱列表 → 导入 Excel
3. 上传 `homeai-recipes-import.xlsx`
4. 导入后菜谱可见性为 `public`，可按需在列表中调整

格式说明见 [recipe-excel-import.md](../guide/recipe-excel-import.md)。

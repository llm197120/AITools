# -*- coding: utf-8 -*-
# 菜谱种子数据

## 文件

| 文件 | 说明 |
|------|------|
| `homeai-recipes-import.xlsx` | 约 1000 条常用菜谱，可直接在管理端「菜谱列表」导入 |
| `fetch_homeai_recipes.py` | 从开源项目拉取并转换的脚本 |

## 数据来源

按优先级去重合并：

1. [Anduin2017/HowToCook](https://github.com/Anduin2017/HowToCook)（社区开源中文菜谱，Markdown，Unlicense）
2. [Gar-b-age/CookLikeHOC](https://github.com/Gar-b-age/CookLikeHOC)（老乡鸡菜品溯源整理）
3. [XiaChuFang Recipe Corpus](https://huggingface.co/datasets/xzm1999/XiaChuFang_Recipe_Corpus)（HuggingFace，MIT，按菜名去重补齐）

按系统 Excel 导入格式转换：菜谱名称、分类 ID、难度、用时、份数、食材、步骤、小贴士、可见性。

## 重新生成

```bash
# 默认拉取并导出 1000 条（本地已有 HowToCook 则不重复下载）
python docs/data/fetch_homeai_recipes.py --limit 1000

# 强制重新下载 GitHub 源码
python docs/data/fetch_homeai_recipes.py --download --limit 1000
```

## 导入步骤

1. 确认库中已有默认分类（`rc_hot` / `rc_cold` / `rc_soup` / `rc_staple` / `rc_bake` / `rc_drink` 等）
2. 管理端 → 菜谱列表 → 导入 Excel
3. 上传 `homeai-recipes-import.xlsx`
4. 导入后菜谱可见性为 `public`，可按需在列表中调整

格式说明见 [recipe-excel-import.md](../guide/recipe-excel-import.md)。

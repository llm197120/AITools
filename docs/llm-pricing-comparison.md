# 主流大模型 API Token 价格与能力对比（2026-08）

> 数据来源：各平台官方定价页（见文末来源清单），已于 **2026-08-17** 逐一核实。
> 汇率为 **$1 ≈ ¥7.15**，价格为官方按量计费正价（元/百万 token，国际平台为 USD/百万 token）。
> ⚠️ 各家促销价/限时价/阶梯价变化频繁，正式接入前请以官网为准。

---

## 一、国际平台（USD/百万 token）

| 平台 | 模型 | 输入 | 输出 | 缓存命中输入 | 上下文 | 能力特点 |
|---|---|---|---|---|---|---|
| **OpenAI** | GPT-5.6 Sol（旗舰） | $5 | $30 | $0.5 | 1.05M | 自适应推理、工具调用、结构化输出、视觉；长上下文加价（$10/$45） |
| | GPT-5.6 Terra（生产主力） | $2 | $12 | $0.2 | 1.05M | 7/30 降价 20%，推理+编码均衡 |
| | GPT-5.6 Luna（轻量） | $0.20 | $1.20 | $0.02 | 1.05M | 7/30 降价 80%，分类/抽取/路由 |
| | GPT-5.5 | $5 | $30 | $0.5 | 1M | 上一代旗舰 |
| | GPT-5.4 / 5.4-mini | $2.5 / $0.75 | $15 / $4.5 | 10% 标价 | 1.05M / 400K | 存量主力 |
| | GPT-5.1 | $1.25 | $10 | $0.125 | 400K | 性价比旧款 |
| **Anthropic** | Claude Fable 5 | $10 | $50 | $1 | — | 最强但带安全分类器（敏感请求回退 Opus） |
| | Claude Opus 5 / 4.8 | $5 | $25 | $0.5 | 200K~1M | 最强推理/Agent，代码与 MCP 生态 |
| | Claude Sonnet 5 | $2（限时至 8/31） | $10 | $0.2 | 1M | 标准价 $3/$15，生产默认档 |
| | Claude Haiku 4.5 | $1 | $5 | $0.1 | 200K | 高速低价档 |
| **Google** | Gemini 3.1 Pro | $2（>200K 后 $4） | $12（>200K $18） | $0.2 | **2M** | 全行业最大上下文，原生多模态 |
| | Gemini 2.5 Pro | $1.25 | $10 | 90% 折扣 | 1M | 长上下文性价比之王 |
| | Gemini 3.6 Flash | $0.75（推广价至 12/31） | $3.75 | — | 1M | 之后恢复 $1.5/$7.5 |
| | Gemini 2.5 Flash | $0.30 | $2.50 | — | 1M | 便宜中端档 |
| | Gemini 2.5 Flash-Lite | $0.10 | $0.40 | — | 1M | 最便宜 Tier-1 模型 |
| **DeepSeek** | V4 Pro | 高峰 ¥9 / 空闲 ¥4.5 | 高峰 ¥27 / 空闲 ¥13.5 | ¥0.15~0.3 | 1M | 开源 MIT，输出最大 384K |
| | V4 Flash | 高峰 ¥3 / 空闲 ¥1.5 | 高峰 ¥9 / 空闲 ¥4.5 | ¥0.05~0.1 | 1M | 峰谷定价，兼容 OpenAI/Anthropic 双协议 |

> **DeepSeek 峰谷定价**：高峰时段为北京时间 9:00-12:00、14:00-18:00（价格为空闲时段 2 倍），其余为空闲时段（半价）。

## 二、国内平台（CNY/百万 token）

| 平台 | 模型 | 输入 | 输出 | 缓存命中输入 | 上下文 | 能力特点 |
|---|---|---|---|---|---|---|
| **阿里百炼** | Qwen3.8-Max | ¥12（限时 5 折→¥6） | ¥36（¥18） | 有折扣 | 1M | 千问旗舰，视觉+视频 |
| | Qwen3.7-Plus | ¥2（限时 8 折） | ¥8 | — | 1M | 性价比主力（≤256K 档） |
| | Qwen3.7-Flash | ¥0.2 | ¥0.8 | — | 1M | 分段计费（32K/256K 阶梯） |
| | Qwen3.5-Plus | ¥0.8 | ¥4.8 | — | 1M | 开源 397B/激活 17B |
| | Qwen-Turbo / Qwen-Long | ¥0.3 / ¥0.5 | ¥0.6 / ¥2 | — | — | 最低价入门 / 长文本专用 |
| **Kimi（月之暗面）** | K3（旗舰） | $3（国内 ¥20） | $15（¥100） | $0.30（¥2） | **1M** | 2.8 万亿参数，开放权重(MIT)，始终推理，对标 Claude Sonnet 5 定价 |
| | K2.6 | $0.95（¥6.5） | $4（¥27） | $0.16（¥1.1） | 262K | 多模态（图/视频），思考/非思考双模 |
| | K2.5 | ¥4 | ¥21 | ¥0.7 | 262K | 上代多模态 |
| **智谱 GLM** | GLM-5.2 | ¥8 | ¥28 | ¥2 | **1M** | 744B MoE，MIT 开源 |
| | GLM-5.1 / GLM-5 | ¥6 / ¥4（≤32K） | ¥24 / ¥18 | ¥1.3 / ¥1 | 200K | 分段计价，≥32K 涨价 |
| | GLM-5-Turbo | ¥5 | ¥22 | ¥1.2 | 200K | 高速档 |
| | GLM-4.7 / 4.7-Flash | ¥2 / **免费** | ¥8 / 免费 | ¥0.4 | 200K | 免费档可跑通链路 |
| **火山方舟（豆包）** | Seed 2.1 Pro / Evolving | ¥6 | ¥30 | ¥1.2 | 256K / 1M | Coding & Agent 旗舰，缓存=标价 20% |
| | Seed 2.1 Turbo | ¥3 | ¥15 | ¥0.6 | 256K | Pro 精确半价 |
| | Seed 2.0 Pro / Lite / Mini | ¥3.2 / ¥0.6 / ¥0.2 | ¥16 / ¥3.6 / ¥2 | 20% 标价 | 256K | 分段计费，Mini 极便宜 |
| | Seed-Code | ¥1.2 | ¥8 | ¥0.24（不跳档） | 256K | 编程专用 |

## 三、能力速览与选型建议

| 维度 | 结论 |
|---|---|
| **上下文最大** | Gemini 3.1 Pro（2M）> OpenAI 5.6 / GLM-5.2 / Kimi K3 / Qwen3.8 / DeepSeek V4（≈1M）> 豆包 Seed 2.1（256K） |
| **最便宜旗舰** | DeepSeek V4 Pro 空闲时段 ¥4.5/¥13.5（≈$0.6/$1.9），远低于国际旗舰 |
| **国际旗舰价格带** | 已收敛到 $5/$25~30（Opus 5 / GPT-5.6 Sol / GPT-5.5）；Fable 5 独贵（$10/$50） |
| **性价比推荐（国内）** | Qwen3.7-Flash（¥0.2/¥0.8）、DeepSeek V4 Flash（¥1.5/¥3）、豆包 Seed 2.0 Mini（¥0.2/¥2） |
| **开源可自托管** | DeepSeek V4（MIT）、GLM-5.2（MIT，需 >1TB 显存）、Kimi K3（Modified MIT）、Qwen（Apache 2.0） |
| **免费额度** | 阿里百炼每模型 100 万 token（合计 7000 万+）、智谱 2000 万、豆包 50 万、Gemini AI Studio 免费层 |
| **省钱机制** | 缓存命中普遍 10%~20% 标价（Kimi K3 / GLM 折扣 90%）；Batch 半价；DeepSeek 峰谷定价可跨时区套利；国内平台普遍分段计费（长输入跳档） |

## 四、接入注意事项（踩坑提醒）

1. **DeepSeek 官方公告近期将整体上调 API 定价**，当前价格随时可能变化。
2. Claude Sonnet 5 的 $2/$10 为**限时优惠价**，2026-08-31 后恢复 $3/$15。
3. Gemini 3.6 Flash、Qwen3.7-Max/Plus 均为**限时促销价**，到期回调。
4. 豆包/Qwen/GLM 均采用**按单次请求输入长度整单跳档**计费（非"超出部分加价"），长输入成本会成倍上升。
5. **火山方舟转售的 DeepSeek V4 Pro（¥12/¥24）比官方（¥3~9）贵约 4 倍**，勿在方舟上购买 DeepSeek。
6. 国际平台（OpenAI/Anthropic）另有长上下文加价、区域处理费（约 +10%）等附加费率。

## 五、数据来源（官方）

| 平台 | 官方定价页 |
|---|---|
| OpenAI | https://openai.com/api/pricing |
| Anthropic | https://platform.claude.com/docs/en/about-claude/pricing |
| Google Gemini | https://ai.google.dev/gemini-api/docs/pricing |
| DeepSeek | https://api-docs.deepseek.com/zh-cn/quick_start/pricing |
| 阿里云百炼（通义千问） | https://help.aliyun.com/zh/model-studio/model-pricing |
| Kimi（月之暗面） | https://platform.kimi.com/docs/pricing |
| 智谱 GLM | https://bigmodel.cn/pricing |
| 火山方舟（豆包） | https://www.volcengine.com/product/doubao |

## 相关文档

- 图片精修开源工具总览：[ai-image-editing-tools.md](./ai-image-editing-tools.md)
- 本机（i7-14650HX + RTX 4060 8G + 16G）照片精修与视频剪辑方案：[local-photo-video-ai.md](./local-photo-video-ai.md)
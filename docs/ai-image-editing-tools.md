# AI 图片精修开源工具调研（2026-08）

> 调研时间：2026-08-17，数据来自 GitHub 实测（star 数为当天值）。
> 背景：为"家庭AI小工具"项目选型——寻找可结合 AI 进行图片精修的开源工具，兼顾个人/家庭部署与现有技术栈（JeecgBoot 管理端 + UniApp 小程序）的集成可能性。
> 相关文档：LLM API 价格对比见 [llm-pricing-comparison.md](./llm-pricing-comparison.md)；本机（RTX 4060）落地方案见 [local-photo-video-ai.md](./local-photo-video-ai.md)；ComfyUI 见 [../ComfyUI/README.md](../ComfyUI/README.md)；百炼 API 接入见 [qwen-image-edit-api.md](./qwen-image-edit-api.md)。

---

## 一、人像/老照片修复（专门赛道）

| 项目 | Star | 许可证 | 能力 | 运行要求 |
|---|---|---|---|---|
| [GFPGAN](https://github.com/TencentARC/GFPGAN)（腾讯 ARC） | 37.7k | BSD-3 | **人脸修复事实标准**，修复模糊/破损人脸、补细节 | Python+PyTorch，建议 GPU |
| [CodeFormer](https://github.com/sczhou/CodeFormer) | ~20k | S-Lab 1.0（**非商用**） | 保真度可调（w 参数），处理极端退化更好 | Python，建议 GPU |
| [Bringing-Old-Photos-Back-to-Life](https://github.com/microsoft/Bringing-Old-Photos-Back-to-Life)（微软亚洲研究院） | 15k+ | MIT | **专治划痕/折痕/破损** + 上色 + 人脸增强三段式 | Python |
| [GPEN](https://github.com/yangxy/GPEN) | 5.6k | 研究 | 盲人脸修复（GFPGAN 架构变体） | Python |
| [SUPIR](https://github.com/Fanghua-Yu/SUPIR) | 6.5k | 开源 | 基于 SDXL 扩散的**通用修复**，效果最细腻但吃显存（≥12G） | GPU |

> ⚠️ 社区实测反馈（V2EX 2026-04）：GFPGAN/CodeFormer 这类 GAN 方案"人脸还行但涂抹感重、大多久没维护"。2026 年新趋势是用 **Nano Banana 2 / FLUX Kontext / Qwen-Image-Edit** 这类指令式编辑模型做修复，效果更自然。

## 二、通用增强：超分/去噪/上色/抠图（面向普通用户）

| 项目 | Star | 许可证 | 能力 | 适合谁 |
|---|---|---|---|---|
| [Upscayl](https://github.com/upscayl/upscayl) | **48.3k** | AGPL-3.0 | 桌面应用（Win/Mac/Linux），Real-ESRGAN 引擎，最高 16x，批量处理，**本地离线** | 家庭用户"傻瓜式"放大 |
| [IOPaint](https://github.com/Sanster/IOPaint) | 23.4k | Apache-2.0 | WebUI，**去水印/去路人/补全**（inpainting/outpainting），本地部署，有 REST API | 集成进自家系统的首选 |
| [Real-ESRGAN](https://github.com/xinntao/Real-ESRGAN) | 26.7k | BSD-3 | 超分引擎（Upscayl 的内核） | 开发者 |
| [DeOldify](https://github.com/jantic/deoldify) | 18.5k | MIT | 黑白照片/**视频**上色，社区最成熟 | 老照片上色 |
| [DDColor](https://github.com/piddnad/DDColor)（阿里达摩院） | 1.6k | 开源 | 高保真上色，效果更真实 | 上色进阶 |
| [waifu2x](https://github.com/nagadomi/waifu2x) | 27.3k | MIT | 动漫图放大，支持照片 | 二次元/插画 |

## 三、"结合 AI"的新玩法：LLM/指令驱动的图像编辑

| 项目 | Star | 说明 |
|---|---|---|
| [Qwen-Image-Edit-2511](https://huggingface.co/Qwen/Qwen-Image-Edit-2511)（阿里） | 开源模型 | 指令式编辑（"把划痕去掉""给照片上色"），中文友好，可走**阿里百炼 API** 免部署 |
| [LongCat-Image-Edit](https://huggingface.co/meituan-longcat/LongCat-Image-Edit)（美团） | 开源模型 | **中英双语**指令编辑，SOTA 级保真，支持多轮编辑 |
| [Step1X-Edit-v1p2](https://huggingface.co/stepfun-ai/Step1X-Edit-v1p2)（阶跃星辰） | 开源模型 | 带思考/反思机制的编辑，理解复杂指令 |
| [GenArtist](https://github.com/zhenyuw16/GenArtist) | 169 | NeurIPS 2024：多模态 LLM 当 Agent 编排多个模型做编辑（学术项目，配置重） |
| [ImageAgent](https://github.com/josefdc/ImageAgent) | 16 | LangGraph 对话式修图（"调亮点""旋转90度"），小型 demo，可参考架构 |

## 四、中文生态（与项目最相关）

| 项目 | Star | 亮点 |
|---|---|---|
| [HivisionIDPhotos](https://github.com/Zeyi-Lin/HivisionIDPhotos) | **21.4k** | 证件照制作：抠图/换底色/美颜/排版打印，**纯 CPU 可跑**，Gradio WebUI + API + Docker。**社区版有 uniapp 小程序和网页版**（`HivisionIDPhotos-uniapp`、`HivisionIDPhotos-web`），与现有 JeecgBoot 小程序栈契合 |

## 五、针对"家庭AI小工具"的落地建议

1. **最快落地（给家人用）**：直接装 **Upscayl**（桌面放大）+ **HivisionIDPhotos**（证件照）——零开发、本地离线、免费。
2. **集成进现有项目（推荐）**：接 **阿里百炼 Qwen-Image-Edit API**（中文指令修图，不用自己扛 GPU），或自部署 **IOPaint**（提供 inpainting REST API）做"去水印/去杂物"功能。
3. **小程序端**：复用 `HivisionIDPhotos-uniapp` 社区实现思路，把证件照功能搬进家庭小程序。
4. **老照片修复**：不建议再上 GFPGAN/CodeFormer（效果与维护均不佳），直接调 Qwen-Image-Edit 或 Nano Banana 2 类指令式模型。

## 附：主要数据来源

- GitHub 仓库主页（star/许可证/更新时间实测）
- V2EX 老照片修复实测反馈（2026-04）：https://www.v2ex.com/t/1203327
- KDnuggets 2026-02 开源图像编辑模型盘点
- aifreeforever.com 2026-07 AI 照片修复工具盘点
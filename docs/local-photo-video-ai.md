# 本机（i7-14650HX + RTX 4060 8G + 16G 内存）照片精修与视频剪辑方案（2026-08）

> 调研时间：2026-08-17，针对笔记本 **CPU i7-14650HX / GPU RTX 4060 Laptop 8GB VRAM / 内存 16GB**。
> 结论先行：这台机器是"高性价比本地 AI 入门平台"——**照片精修完全胜任，常规视频剪辑流畅，AI 视频生成可尝鲜（480p 短片）**。
> 相关文档：图片精修工具总览见 [ai-image-editing-tools.md](./ai-image-editing-tools.md)；LLM API 价格对比见 [llm-pricing-comparison.md](./llm-pricing-comparison.md)；ComfyUI 见 [../ComfyUI/README.md](../ComfyUI/README.md)；百炼 API 接入见 [qwen-image-edit-api.md](./qwen-image-edit-api.md)。

---

## 〇、硬件定位（为什么 8GB 是分水岭）

- Windows 桌面/浏览器/驱动会先占用部分显存，**实际可用 AI 显存约 6.5~7.2GB**，不是整 8GB。
- 16GB 系统内存是剪映的舒适区、DaVinci 的入门线；跑生图/视频生成时建议关闭浏览器（省显存+内存）。
- 判断标准不是"模型能不能启动"，而是"**显存是否溢出到系统内存**"——一旦溢出，速度会崩到不可用。

| 用途 | 结论 |
|---|---|
| 本地 LLM（聊天/编码） | 可跑 3B~8B 量化模型（Qwen 3 8B Q4、DeepSeek R1 Distill 7B 等），约 30 token/s |
| 图像生成/精修 | **推荐主力场景**：SDXL / SD 1.5 / Z-Image-Turbo 流畅；FLUX 需量化 |
| 常规视频剪辑 | **推荐主力场景**：剪映流畅；DaVinci 入门可用 |
| AI 视频生成 | 可跑 Wan 1.3B / LTX 2B / FramePack，480p 短片 4~6 分钟/条 |
| 不建议 | 14B+ 大模型、未量化 FLUX 高清批量、大分辨率视频生成、多模型常驻 |

## 一、照片精修方案（本地部署）

### 主力推荐：ComfyUI（一站式本地 AI 修图/生成平台）

| 模型 | 显存 | 效果 | 用途 |
|---|---|---|---|
| **SDXL**（如 WAI-Illustrious 等微调版） | ~8GB 内流畅 | 1024×1024 约 15s/张，可挂 LoRA | 通用生成、图生图、ControlNet 精修 |
| **SD 1.5** | 轻松 | 最快，生态最全 | 快速出图、老插件 |
| **Z-Image-Turbo**（阿里 6B，INT8 量化） | 8GB | **亚秒级**出图，**中文理解强**（古诗词/水墨/国风） | 中文提示词场景首选 |
| **FLUX.1 schnell GGUF Q4/Q5** | 6~8GB | 画质/提示词理解最强 | 追求画质时用，需 `ComfyUI-GGUF` 节点 + `--lowvram` |
| **FLUX.1 dev FP8 + FluxExt-MZ 插件** | ~6GB 可跑 | 国内开发者 MinusZone 的分层加载插件，显存占用降 60%+ | FLUX 进阶方案 |

落地建议：
- 前端用 **ComfyUI**（显存效率最高、社区工作流可复用）；新手也可用 **Fooocus / SD WebUI Forge**（开箱即用）。
- 场景化工作流（可导出 JSON 复用）：
  - 老照片修复：`img2img` 低重绘幅度 + 人脸修复节点（GFPGAN/CodeFormer 作为 ComfyUI 内置节点）
  - 去水印/去路人：IOPaint（独立 WebUI，REST API 可集成进系统）
  - 放大：Upscayl（桌面端）或 Real-ESRGAN 节点
  - 上色：DeOldify / DDColor 节点
- **注意**：不要照搬 16GB/24GB 显卡的网上工作流；先 768px 或单张 1024px 起步，跑模型时关浏览器。

### 轻量/免安装方案（适合给家人用）

| 工具 | 用途 | 说明 |
|---|---|---|
| [Upscayl](https://github.com/upscayl/upscayl)（桌面应用） | 照片放大 4~16x | 一键、免费、离线；**需 Vulkan 兼容 GPU（4060 满足）**；不能修复失焦/运动模糊 |
| [HivisionIDPhotos](https://github.com/Zeyi-Lin/HivisionIDPhotos) | 证件照 | 纯 CPU 可跑，本地 WebUI，一键抠图/换底色/美颜/排版 |
| [IOPaint](https://github.com/Sanster/IOPaint) | 去水印/去杂物 | WebUI + API，显存占用低，8GB 轻松 |

### 云端 API 补充（不占本机资源，中文指令修图）

- 阿里百炼 **Qwen-Image-Edit**（指令式编辑："把划痕去掉""给照片上色"），价格与接入见 [llm-pricing-comparison.md](./llm-pricing-comparison.md)。
- 适合集成进家庭小工具的后端，本机只做开发调试。

## 二、视频剪辑方案

### 主力推荐：剪映专业版（家庭/短视频场景首选）

- **免费**、中文、模板丰富、上手零门槛。
- RTX 加速：NVDEC 硬件解码（4K 流畅拖动）+ NVENC 硬件编码（导出快）+ AV1 编码（体积小 30%）。
- AI 功能全（自动字幕/配音/抠像/图文成片），16GB 内存舒适运行。
- 适合：家庭相册视频、短视频、Vlog。

### 进阶推荐：DaVinci Resolve（免费版）

- **免费版无阉割**（无水印、可商用），调色是行业标准，剪辑/音频/Fusion 特效一体。
- 硬件门槛：基础剪辑最低 16GB 内存（本机达标但紧），4K + AI 特效建议 32GB；RTX 4060 8GB 显存可用（免费版单 GPU 加速）。
- AI 功能（Studio 版 $295 买断）：AI 字幕、场景剪切、物体追踪、智能多机位切换；免费版含基础 AI 调色。
- 适合：想认真学剪辑/调色、需要专业出片时。

### 其他

| 工具 | 门槛 | 说明 |
|---|---|---|
| 万兴喵影 | 8GB 内存即可 | AI 功能多、模板多，买断/订阅，介于剪映与达芬奇之间 |
| Premiere Pro | 订阅制 | 不建议家庭用户（贵、学习曲线陡） |
| Topaz Video AI（商业） | TensorRT 加速 | 视频超分/增强/修复天花板，4K/8K 提升，按需购买 |
| RTX VSR（显卡自带） | 免费 | 浏览器看视频时 AI 超分提升画质，零成本 |

## 三、AI 视频生成（尝鲜，480p 短片）

| 模型 | 显存 | 出片 | 说明 |
|---|---|---|---|
| **Wan 2.1 1.3B**（GGUF） | 4~6GB | 480p，5s 约 4~6 分钟 | 8GB 下性价比最高，LightX2V 框架支持 4060 8GB |
| **LTX Video 2B**（FP8 + tiling） | 6~8GB | 最高 720p | 生成快，8GB 较舒适 |
| **FramePack** | ~6GB | 短片 | 显存友好，适合长镜头 |
| Wan 2.2 5B（量化） | 8GB 可跑 480p | 5s 约 4~6 分钟 | 比 1.3B 质量明显更好，推荐替代 1.3B |
| Wan 14B / HunyuanVideo | 需量化+CPU offload | 20~30 分钟/条 | **不建议**在 8GB 上跑，体验差 |

工具链：ComfyUI（官方 Wan/LTX 工作流）或独立的 Wan2.1/2.2 WebUI 一键包（B 站社区有 6GB 显存青春版）。

## 四、推荐落地组合（针对此笔记本）

```
日常照片精修   → ComfyUI + SDXL + Z-Image-Turbo（中文）+ IOPaint（去杂物）
家人用简单工具 → Upscayl（放大）+ HivisionIDPhotos（证件照）
视频剪辑       → 剪映专业版（主）+ DaVinci Resolve 免费版（进阶调色）
AI 视频尝鲜    → ComfyUI + Wan 2.2 5B 量化（480p 短片）
系统集成       → 后端接阿里百炼 Qwen-Image-Edit API（不占本机算力）
```

## 五、注意事项

1. **16GB 内存偏紧**：DaVinci 4K + 浏览器 + AI 模型同时开容易卡；跑模型时关闭其他应用，长期用建议升级 32GB。
2. **散热**：笔记本生图/视频生成/批量转写请开性能模式，注意散热（4060 满载 115W TDP）。
3. **驱动**：保持 NVIDIA Studio 驱动较新，RTX 4060 有 233 TOPS AI 算力，需要驱动配合。
4. **显存监控**：用任务管理器或 `nvidia-smi` 观察，长期超 7.5GB 就要降模型/量化/分辨率。
5. **免费额度**：剪映、Upscayl、DaVinci 免费版、HivisionIDPhotos 全部免费，先零成本试用再决定是否付费升级。

## 附：主要数据来源

- KnightLi 博客《笔记本 RTX 4060 8GB 适合跑哪些本地 AI 模型》（2026-05）
- WillItRunAI《Video Generation VRAM Requirements 2026》（2026-04）
- RunAIHome《Wan 2.1/2.2/2.7 for Local AI Video Generation》（2026-06）
- NVIDIA 中国博客《RTX 加速剪映 AI 视频剪辑》《RTX Blackwell GPU 为专业级视频剪辑提供加速》
- DaVinci Resolve 20 评测（万兴/SSKOO，2026）
- ComfyUI 8GB 显存实战指南（lilting.ch、Cursor IDE 博客、aifreeapi 等，2025-2026）
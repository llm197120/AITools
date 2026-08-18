---
name: ComfyUI 本地路线图
version: v1
status: 已落地（第 41～42 轮）
updated: 2026-08-18
---

# ComfyUI 本地路线图

> 本文档为 ComfyUI 本地部署与照片精修能力建设的专项迭代路线，自总路线文档（`homeai-optimization-roadmap.md`）拆分独立维护。
> 承接调研：`docs/local-photo-video-ai.md`（本机方案）、`docs/comfyui-local-setup.md`（安装与实测指南）。

## 第 41 轮：ComfyUI 本地落地 + 双模型验证（SDXL + Z-Image-Turbo）（2026-08-18）

> 背景：承接本地 AI 方案调研（`docs/local-photo-video-ai.md`、`docs/comfyui-local-setup.md`），在本机（i7-14650HX + RTX 4060 Laptop 8GB + 16GB 内存）安装 ComfyUI 便携版，下载并验证 SDXL 与 Z-Image-Turbo 双模型文生图，为照片精修能力铺路。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 环境 | ComfyUI 便携版 | 0.33.1 便携包（内置 Python 3.13.14）；torch 2.13.0+cu126（驱动 576.28 不支持 CUDA 13，故弃 cu130 用 cu126） | `C:\Users\57089\ComfyUI-portable\` |
| 环境 | 长路径坑 | Windows 长路径 WinError 206 → `subst X:` 映射解决（重启后失效，需重新 subst） | — |
| 模型 | SD 1.5（既有） | `v1-5-pruned-emaonly.safetensors`（3.97GB）→ checkpoints | `models/checkpoints/` |
| 模型 | SDXL | `sd_xl_base_1.0.safetensors`（6.6GB，ModelScope）→ checkpoints | `models/checkpoints/` |
| 模型 | Z-Image-Turbo 三件套 | int8 扩散 `z_image_turbo_int8_convrot`（5.8GB）+ Qwen3-4B fp8 文本编码器（5.2GB）+ ae VAE（320MB），均 ModelScope 官方 `Comfy-Org/z_image_turbo` | `models/diffusion_models/`、`text_encoders/`、`vae/` |
| 验证 | 三模型 API 出图 | SD1.5 512px 8s / SDXL 1024px 46s / Z-Image-Turbo 1024px 68s（10 步 CFG=1.0），全部 `status: success`，PNG 头校验合法 | `wf.json` / `wf-sdxl.json` / `wf-zimage.json` |

**关键结论：** 8GB 显存 + `--lowvram` 可跑三模型共存；Z-Image-Turbo 蒸馏模型 **CFG 必须 =1.0**（euler + simple，10 步）；bf16 版扩散模型 12.3GB 超显存，故选 int8/fp8 量化版。

**无新增 SQL。**

**状态：** 已落地。

---

## 第 42 轮：ComfyUI 常用模型下载 + 启停脚本（2026-08-18）

> 承接第 41 轮，补齐照片精修常用模型（放大 / 人脸修复 / 局部重绘 ControlNet），并新增 ComfyUI 一键启动/停止脚本。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 模型 | 放大 | `RealESRGAN_x4plus.pth`（67MB）+ `4x-UltraSharp.pth`（67MB）→ `models/upscale_models/` | ModelScope `muse/RealESRGAN_x4plus`、`XiangZL0/4x-UltraSharp` |
| 模型 | 人脸修复 | `GFPGANv1.4.pth`（348MB）+ `codeformer.pth`（376MB）→ `models/facerestore_models/`（新建目录） | ModelScope `muse/GFPGANv1.4`、`a694193787/CodeFormer` |
| 模型 | ControlNet | `control_v11p_sd15_inpaint.pth`（1.45GB）→ `models/controlnet/` | ModelScope `lllyasviel/ControlNet-v1-1` |
| 脚本 | 启动 | `start-comfyui.bat`：端口 8188 已监听则跳过，否则 `--lowvram --port 8188` 后台启动并写日志 | `C:\Users\57089\ComfyUI-portable\` |
| 脚本 | 停止 | `stop-comfyui.bat`：按 8188 端口找 PID 强杀 + 轮询等端口释放（最长 15s） | 同上 |
| 验证 | 模型识别 | 重启后 `/object_info`：UpscaleModelLoader 2 个、ControlNetLoader 1 个均识别 | — |
| 插件 | 人脸修复节点 | `comfyorg/comfyui-facerestore`（**重命名目录为 `facerestore_cf`** 以匹配代码 import 路径）→ `custom_nodes/facerestore_cf/`；注册 3 节点 `FaceRestoreModelLoader` / `FaceRestoreCFWithModel` / `CropFace` | `models/facedetection/`：`detection_Resnet50_Final.pth`（109MB）+ `parsing_parsenet.pth`（85MB）+ `parsing_bisenet.pth`（53MB）预下载避免运行时 GitHub 拉取 |
| 插件 | 依赖 | 嵌入式 Python 补装 `opencv-python` / `scikit-image` / `addict` / `lmdb` / `lpips` / `yapf` / `gdown` / `future`（清华源，未动 torch 2.13.0+cu126） | `python_embeded` |
| 验证 | 人脸修复端到端 | SD1.5 生成人像 → `FaceRestoreModelLoader`(GFPGANv1.4) → `FaceRestoreCFWithModel`(retinaface_resnet50) → 输出 `test_facerestore_00001_.png`，`status: success` | `wf-portrait.json` / `wf-facerestore.json` |

**关键结论：** ① `.bat` 脚本在中文 Windows 必须 **ASCII 编码 + CRLF**（UTF-8 中文会被 cmd 拆词报错，`chcp 65001` 无法救）；② 人脸修复链路已打通：**`comfyorg/comfyui-facerestore` 是官方 fork，自带 vendored `basicsr/`+`facelib/`**（避开老库编译问题，兼容 Python 3.13），但目录名必须改为 `facerestore_cf`；检测/解析模型会先查 `models/facedetection/`，预下载可避免首次运行从 GitHub 慢速拉取；③ 所有 5 个模型字节数与 ModelScope API 精确一致。

**无新增 SQL。**

**状态：** 已落地。

---

## 关联文档

| 文档 | 说明 |
|------|------|
| `docs/comfyui-local-setup.md` | ComfyUI 安装与实测指南（含 6.6 人脸修复插件用法） |
| `docs/local-photo-video-ai.md` | 本机本地 AI 方案调研（RTX 4060 8G） |
| `docs/ai-image-editing-tools.md` | AI 图片精修开源工具调研 |
| `docs/plan/homeai-optimization-roadmap.md` | 总迭代路线文档（本专项已拆分） |
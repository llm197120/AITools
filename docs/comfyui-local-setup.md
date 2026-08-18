# ComfyUI 本地安装与落地指南（RTX 4060 8G / Windows / 2026-08）

> 面向 **i7-14650HX + RTX 4060 Laptop 8GB + 16GB 内存** 的 Windows 笔记本。
> 目标：跑通「照片精修 + 图像生成」主力工作流（SDXL / Z-Image-Turbo / FLUX GGUF）。
> 相关文档：本机整体方案见 [local-photo-video-ai.md](./local-photo-video-ai.md)；GitHub 工具总览见 [ai-image-editing-tools.md](./ai-image-editing-tools.md)。

---

## 一、安装步骤（约 30 分钟）

### 1. 前置准备

- 安装 [Python 3.10.11](https://www.python.org/downloads/)（ComfyUI 兼容性最稳；3.11/3.12 也可）。安装时勾选 **Add Python to PATH**。
- 确认 NVIDIA 驱动较新（建议 Studio 驱动），用 `nvidia-smi` 查看 CUDA 版本（本机 4060 支持 CUDA 12.x）。
- 磁盘预留 **至少 20GB**（模型占大头）。

### 2. 下载 ComfyUI

**方式 A（推荐，独立环境不污染系统）**：GitHub 官方便携版
```
https://github.com/comfyanonymous/ComfyUI/releases
```
下载 `ComfyUI_windows_portable_nvidia.7z`，解压即用（内置 Python + PyTorch CUDA + Git）。

**方式 B（手动搭建，可控性强）**：
```powershell
# 安装 PyTorch（CUDA 12.x）
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu124

# 克隆 ComfyUI 主仓库
git clone https://github.com/comfyanonymous/ComfyUI.git
cd ComfyUI
pip install -r requirements.txt
```

> 国内网络建议：便携版走镜像加速；PyTorch 用清华源 `https://pypi.tuna.tsinghua.edu.cn/simple`。

### 3. 首次启动与显存参数（8GB 关键）

便携版直接运行 `run_nvidia_gpu.bat`；手动版：
```powershell
python main.py --lowvram --auto-launch
```
- `--lowvram`：**8GB 显存必开**，防止显存溢出到内存。
- `--auto-launch`：启动后自动打开浏览器（默认 http://127.0.0.1:8188）。
- 备用参数：`--cpu`（仅调试）、`--force-fp16`、`--disable-smart-memory`（内存紧张时关）。

**启动后验证**：浏览器打开 8188 端口看到默认工作流即成功。用任务管理器观察，显存占用应在 7.5GB 以内。

### 4. 安装必要插件（在 ComfyUI 根目录）

```powershell
# 1) 管理器（装模型/插件的神器）
git clone https://github.com/ltdrdata/ComfyUI-Manager.git custom_nodes/ComfyUI-Manager

# 2) GGUF 支持（跑 FLUX GGUF 必需）
git clone https://github.com/city96/ComfyUI-GGUF.git custom_nodes/ComfyUI-GGUF

# 3) FLUX 分层加载（MinusZone，显存占用降 60%+，国内开发者）
git clone https://github.com/MinusZoneAI/ComfyUI-FluxExt-MZ.git custom_nodes/ComfyUI-FluxExt-MZ
```
重启 ComfyUI 后，管理器界面可一键搜索安装更多插件。

### 5. 下载模型（放对路径最关键）

| 模型 | 存放路径 | 说明 |
|---|---|---|
| SDXL 大模型（如 `wai-realistic`、`dreamshaper`） | `models/checkpoints/` | 主力模型，约 7GB |
| SD 1.5 大模型（可选） | `models/checkpoints/` | 轻量备用 |
| Z-Image-Turbo（INT8，阿里开源） | `models/checkpoints/` | 亚秒级中文生图 |
| FLUX.1 schnell GGUF（Q4/Q5） | `models/unet/`（GGUF 节点专用） | 画质天花板 |
| VAE | `models/vae/` | SDXL 用 `sdxl_vae` |
| LoRA | `models/loras/` | 风格微调 |
| ControlNet | `models/controlnet/` | 精修控制 |

下载渠道：HuggingFace（`Comfy-Org` / `stabilityai` 官方仓库）、LiblibAI（国内快）、Civitai。SDXL 建议先下 **`dreamshaper-xl` 或 `juggernaut-xl`** 这类通用模型。

## 二、8GB 显存的 4 个落地工作流（照片精修）

> 所有工作流都可在 ComfyUI 界面「Load」加载 JSON 后用，或用管理器模板。核心原则：**不照搬 16GB/24GB 显卡的图**，先 768px 起步。

### 工作流 1：老照片修复
```
加载图片 → 高清放大（Upscale）→ img2img（重绘幅度 0.3~0.4）→ 人脸修复节点（GFPGAN/CodeFormer，ComfyUI 内置）→ VAE 解码 → 保存
```

### 工作流 2：去水印 / 去路人
```
IOPaint 独立程序处理（REST API 可接入后端）→ 或用 ControlNet inpaint 节点 → 局部重绘
```

### 工作流 3：中文提示词生图（Z-Image-Turbo）
```
TextEncode（中文直出）→ KSampler（步数 4~8，CFG 1.0~2.0，速度快）→ 1024×1024
```
Z-Image-Turbo 对中文/古风/水墨理解强，INT8 量化 8GB 可跑，亚秒级。

### 工作流 4：FLUX 高质量出图
```
FLUX.1 schnell GGUF Q4 加载（ComfyUI-GGUF 节点）→ 1024×1024 → --lowvram 运行
```
画质要求高时用，出图时间 1~3 分钟。**千万不要直接跑 FLUX dev 全精度**（会 OOM）。

## 三、性能与稳定性 Checklist

1. **显存监控**：任务管理器「GPU」面板或 `nvidia-smi`。长期 >7.5GB → 降分辨率 / 换 Q4 量化 / 关浏览器。
2. **16GB 内存**：跑模型时关闭浏览器、微信等大内存应用。
3. **散热**：笔记本开性能模式，注意风扇/温度（4060 满载 115W）。
4. **预览**：工作流中 `PreviewImage` 节点保留，但**生成完成后点掉浏览器预览**可释放显存。
5. **模型一致性**：同款模型不同来源（HuggingFace/Civitai/Liblib）sha256 可能不同，混用会出奇怪结果；固定一个来源。
6. **错误排查**：报错先看控制台日志；`--lowvram` 下 OOM 概率极低，若仍 OOM 换 `--novram`。

## 四、常见坑

| 坑 | 现象 | 解决 |
|---|---|---|
| 显存溢出 | 生成时卡死/报 `CUDA out of memory` | 开 `--lowvram`；降分辨率；换 Q4 GGUF |
| 中文乱码 | 提示词中文显示 `??` | 检查文件编码为 UTF-8；用 Z-Image-Turbo 中文友好 |
| 模型不显示 | 下拉框没有模型 | 确认文件名/路径在 `models/` 对应子目录，重启 ComfyUI |
| 插件报错 | 缺依赖 | ComfyUI-Manager 里点「Install Missing Custom Nodes」 |
| 出图模糊 | 放大后糊 | 加 Real-ESRGAN / 4x-UltraSharp 放大节点 |

## 五、常用链接

- ComfyUI 官方：https://github.com/comfyanonymous/ComfyUI
- 官方示例工作流：https://comfyanonymous.github.io/ComfyUI_examples/
- ComfyUI-Manager：https://github.com/ltdrdata/ComfyUI-Manager
- ComfyUI-GGUF：https://github.com/city96/ComfyUI-GGUF
- FluxExt-MZ：https://github.com/MinusZoneAI/ComfyUI-FluxExt-MZ
- SDXL 模型（HuggingFace）：https://huggingface.co/stabilityai/stable-diffusion-xl-base-1.0
- LiblibAI（国内模型下载）：https://www.liblib.art/

---

## 六、本机实际落地记录（2026-08-18 实测，与上文通用指南互补）

> 本机：i7-14650HX + RTX 4060 Laptop **8GB** + 16GB 内存，驱动 576.28（**不支持 CUDA 13**）。ComfyUI **0.33.1 便携版**，Python 3.13.14（内置），torch 2.13.0+cu126。

### 6.1 关键差异点（对照通用指南）

| 项 | 通用指南 | 本机实际 |
|---|---|---|
| Python | 建议 3.10.11 | 便携版内置 3.13.14 即可；系统 Python 3.14.6 过新勿用 |
| PyTorch CUDA | cu124 | **cu126**（驱动 576.28 不支持 CUDA 13，cu130 装不上） |
| 下载渠道 | HF / LiblibAI / Civitai | **ModelScope 最快**（实测 ~7.5MB/s；hf-mirror 仅 ~0.4MB/s 弃用） |
| 长路径 | — | WinError 206 → `subst X: "C:\Users\57089\ComfyUI-portable\ComfyUI_windows_portable"`（重启后失效） |
| 启动参数 | `--lowvram` | 实测必须 `--lowvram --port 8188`，三模型共存不冲突 |

### 6.2 已装模型（ModelScope 下载，字节数与 API 精确一致）

| 模型 | 文件 | 大小 | 目录 |
|---|---|---|---|
| SD 1.5 | `v1-5-pruned-emaonly.safetensors` | 3.97GB | `models/checkpoints/` |
| SDXL | `sd_xl_base_1.0.safetensors` | 6.6GB | `models/checkpoints/` |
| Z-Image-Turbo 扩散 | `z_image_turbo_int8_convrot.safetensors` | 5.8GB | `models/diffusion_models/` |
| Z-Image-Turbo 文本编码器 | `qwen_3_4b_fp8_mixed.safetensors` | 5.2GB | `models/text_encoders/`（CLIPLoader type=qwen_image） |
| Z-Image-Turbo VAE | `ae.safetensors` | 320MB | `models/vae/` |
| 放大（ESRGAN） | `RealESRGAN_x4plus.pth` | 67MB | `models/upscale_models/` |
| 放大（锐化） | `4x-UltraSharp.pth` | 67MB | `models/upscale_models/` |
| 人脸修复 | `GFPGANv1.4.pth` | 348MB | `models/facerestore_models/`（新建目录） |
| 人脸修复 | `codeformer.pth` | 376MB | `models/facerestore_models/` |
| ControlNet 局部重绘 | `control_v11p_sd15_inpaint.pth` | 1.45GB | `models/controlnet/` |

> Z-Image 来源：ModelScope `Comfy-Org/z_image_turbo`（官方拆分文件）；SDXL 来源：`AI-ModelScope/stable-diffusion-xl-base-1.0`（官方单文件）。常用模型来源：`muse/RealESRGAN_x4plus`、`XiangZL0/4x-UltraSharp`、`muse/GFPGANv1.4`、`a694193787/CodeFormer`、`lllyasviel/ControlNet-v1-1`（均 ModelScope）。人脸修复模型需另装 FaceRestore 类插件（如 ComfyUI-FaceRestore / Impact Pack）才有对应节点。

### 6.3 实测出图（API 提交工作流，同一提示词，全部 success）

| 模型 | 分辨率 | 配置 | 耗时 | 输出 |
|---|---|---|---|---|
| SD 1.5 | 512×512 | 20 步 / euler / CFG 7 | ~8s | `test_cat_00001_.png` |
| SDXL | 1024×1024 | 25 步 / dpmpp_2m / karras / CFG 7 | ~46s | `test_sdxl_00001_.png` |
| Z-Image-Turbo | 1024×1024 | **10 步 / euler / simple / CFG 必须 =1.0** | ~68s | `test_zimage_00001_.png` |

> 工作流 JSON 已留存：`wf.json`（SD1.5）/ `wf-sdxl.json` / `wf-zimage.json`（均在 `C:\Users\57089\ComfyUI-portable\`），可在 Web UI Load 导入复用。验证 API：`POST /prompt` + `GET /history/{prompt_id}`。

### 6.4 8GB 显存经验（实测）

1. **Z-Image 量化选择**：int8 扩散（5.8GB）+ fp8 文本编码器（5.2GB）；**bf16 版扩散模型 12.3GB 超显存不可用**。
2. **CFG=1.0 是硬约束**：Z-Image 为蒸馏模型，CFG>1 会出图异常。
3. **模型共存**：`--lowvram` 动态换载，SD1.5/SDXL/Z-Image 三个模型共存无压力。
4. **下载加速**：ModelScope `https://modelscope.cn/models/{org}/{repo}/resolve/master/{path}` 直链 curl 并行下载最快。

### 6.5 启停脚本（第 42 轮，已实测闭环）

| 脚本 | 作用 | 说明 |
|---|---|---|
| `start-comfyui.bat` | 启动 ComfyUI | 端口 8188 已监听则跳过；否则 `python_embeded\python.exe -s ComfyUI\main.py --windows-standalone-build --lowvram --port 8188` 后台启动，日志写 `comfyui.log` / `comfyui.log.err` |
| `stop-comfyui.bat` | 停止 ComfyUI | 按 8188 端口找 PID 强杀，轮询等端口释放（最长 15s，避免 socket 释放延迟误判） |

> 位置：`C:\Users\57089\ComfyUI-portable\`。**编码硬规则**：`.bat` 必须 **ASCII 编码 + CRLF 换行**——中文 Windows 下 UTF-8 中文或 LF 换行会被 cmd 拆词报错（`'cho' 不是内部或外部命令` 等），`chcp 65001` 无法修复。闭环实测：stop → 端口释放 → start → 8188 监听，模型自动重新扫描。

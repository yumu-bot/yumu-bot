# 动画图片与 WebP 编码运行说明

## 依赖

- `dev.matrixlab.webp4j:webp4j-core:2.5.0`：JNI 绑定 libwebp 1.6.0，负责静态 WebP 编码、动画 WebP 解码和 GIF 帧读取。
- `com.dylibso.chicory:runtime:1.7.5` / `wasm:1.7.5`：纯 JVM WebAssembly 运行时，用于 Zeta 的颜色模块。

## 平台限制

webp4j 自带 Windows x64/arm64、Linux x64/aarch64、macOS x64/arm64 原生库。运行时会把对应 `.so`/`.dll`/`.dylib` 解压到系统临时目录再 `System.load`，因此要求：

- 临时目录可写，且未挂载 `noexec`；
- Linux 使用 glibc（Alpine/musl 不在支持范围）；
- Java 22+ 可能出现 restricted native access 警告。当前项目不修改启动脚本/系统属性；部署层可选择 `--enable-native-access=ALL-UNNAMED` 消除警告，但不属于本次代码改动。

## 编码流程

Epsilon 的头像动画由 `image.anim` 完成：解码 GIF/APNG/WebP 帧 → 全画布合成 → 头像区缩放（缩小 Lanczos3，放大 cubic，预乘 alpha）→ 每帧静态 WebP q90 → `RIFF VP8X/ANIM/ANMF` 封装。输出固定全画布帧、`flags=0x03`（dispose=background + no-blend）、`loop=0`；单帧也保持 animated WebP。

## APNG

APNG 只做 chunk 切分和逐帧 PNG 重建，实际 PNG 解码交给 JDK ImageIO；不引入 pngj，也不使用 GPL 库。支持 acTL/fcTL/fdAT、首帧 IDAT 判定、dispose 0/1/2、blend 0/1 和 delay 计算。CRC 缺失、序列乱序、截断、超限都会明确抛错。

# img-color wasm

- 来源：用户提供的 `yumu-image/color/wasm_wrapper_bg.wasm`。
- 文件：`src/main/resources/wasm/img-color.wasm`
- SHA-256：`c2019bba2fec844be621c3ba3f813ddc056518d7faf222a699ed7f653b4584a4`
- 用途：复用原 Zeta 的主色提取算法；只适配 wasm-bindgen host shim（3 个 import、externref 表、Rgb 指针），不修改模块。
- 授权：原仓库 `color/` 目录未附授权声明，本仓库不伪造许可证。使用/分发前需要由模块提供方补齐授权与 notices。
- 运行时：Chicory 1.7.5 纯 JVM；模块固定哈希校验；实例有界，输入字节、内存页、deadline 均有上限。

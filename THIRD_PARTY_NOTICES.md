# 第三方组件与许可声明

本工程（`local-ip`）以 MIT 许可证开源（见 [LICENSE](LICENSE)）。
但**随 APK 一起分发的部分资源来自第三方**，它们各自的许可证同样适用。
若你二次分发本工程或基于它构建的 APK，请一并保留本文件。

---

## 1. iperf3 3.22 —— BSD-3-Clause

**这是什么**：`app/src/main/jniLibs/arm64-v8a/libiperf3.so`。
它是 iperf3 的 Android/arm64 预编译可执行文件，以「原生库」的形式随 APK 分发，
由 App 在运行时通过 `nativeLibraryDir` 调用。

**二进制来源**：[davidBar-On/android-iperf3](https://github.com/davidBar-On/android-iperf3)（MIT）
的 `gh-pages` 分支，`libs/arm64-v8a/iperf3.22`。
上游 iperf3 源码：[esnet/iperf](https://github.com/esnet/iperf)。

**上游许可**：BSD 3-Clause

```
Copyright (c) 2014-2025, The Regents of the University of California,
through Lawrence Berkeley National Laboratory
( subject to receipt of any required approvals from the U.S. Dept. of Energy ).
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
```

---

## 2. Noto Serif SC —— SIL Open Font License 1.1

**这是什么**：`app/src/main/res/font/noto_serif_sc_*.ttf`（400 / 500 / 600 / 700 四个字重）。
本工程对其做了**子集化**，只保留界面实际用到的字符。

**版权**：Copyright 2014-2024 Adobe (http://www.adobe.com/)，保留字体名称 "Source"。
Noto 是 Google 的商标。

**许可**：SIL Open Font License, Version 1.1（允许自由使用、修改、再分发，包括商用；
若再分发字体文件本身，需保留本许可并不得单独售卖字体）。

许可全文：<https://scripts.sil.org/OFL>

---

## 3. JetBrains Mono —— Apache License 2.0

**这是什么**：`app/src/main/res/font/jetbrains_mono_*.ttf`（400 / 500 / 700 三个字重），
用于 IP / MAC / 网关 / DNS 等等宽数值显示。同样做了子集化。

**版权**：Copyright 2020 The JetBrains Mono Project Authors
(<https://github.com/JetBrains/JetBrainsMono>)

**许可**：Apache License, Version 2.0 —— <https://www.apache.org/licenses/LICENSE-2.0>

---

## 4. AndroidX / Jetpack Compose / Kotlin 等运行时依赖

均以 Apache License 2.0 分发，通过 Gradle 依赖引入，未修改源码，详见各依赖仓库。

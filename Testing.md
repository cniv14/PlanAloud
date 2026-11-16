# Android 反馈修复（1.5.1-preview.1）

- 悬浮助手：点击或向内横拖展开；长按把手后可移动，直接上下划不会移位。展开后系统返回关闭小窗。工具图标、文字及所在区域均可点击；左下角或右下角拖动可调整小窗宽高，尺寸保存到本机。
- 对话外观复用主界面的主题、壁纸、半透明卡片、字体和紧凑设置。
- `/` 补全显示全部匹配项，可上下滚动；方向键选择、Tab 补全，点击候选只填入输入框。
- 图片可多选，无应用内数量上限；原文件直接上传，不缩放、不重新编码。缩略图只用于显示。接口自身的格式、请求大小和设备内存仍影响能否发送。失败时保留当前附件。
- 附件菜单支持 UTF-8 TXT、Markdown、CSV、ICS、JSON、HTML。单个文本文件最多 256KB；PDF/Office 需先转换。文件内容作为资料交给 AI。
- 主界面默认只显示今天、日程、设置。设置 → 主题与动效 → 主界面栏目与列数，可选择栏目和卡片列数（自动或 1–4 列）。设置入口保留；隐藏的笔记、收集箱仍在更多功能和 `/notes`、`/inbox` 中。
- 节次支持 `1=08:00-08:45`、`第一节 8:00至8:45`、中文冒号及常见连接符；预览、编辑和正式导入共用解析。支持通过节次推导起止时间，完整保留已有明确时间。
- 课表工具返回完整草稿、可导入状态及具体校验错误；后续请求始终携带当前草稿。对话上下文由最近 8 轮增加到 32 轮，中断后保留已执行工具结果。
- 普通对话中已明确要求的命令直接执行，无需重复批准；资料整理会话仍保留人工确认。确认失败不会提前消耗确认卡。修改、删除和整次课程导入可撤销；批量撤销先检查全部课程版本，有后续编辑或同步时拒绝覆盖。
- 语音模块新增 DeepSeek 官方 ASR：`POST https://api.deepseek.com/v1/tools/asr`，multipart `file` 上传 16kHz 单声道 PCM WAV。独立保存 DeepSeek 语音密钥；只有 Agent 使用官方 DeepSeek HTTPS 地址时才复用 Agent 密钥。录音最多 120 秒，识别结果先回填输入框。
- 天气支持精确位置权限，结合 GPS 和网络定位，使用实际经纬度请求预报，并尽可能显示区县、街道。天气服务的预报网格决定最终空间分辨率。
- 设置或 `/diagnostics` 可导出诊断日志。仅记录操作名、错误类型和代码位置，不记录请求正文、图片和密钥。

## 验证入口

节次解析：

```sh
javac -d /tmp/planaloud-test android/app/src/main/java/app/planaloud/TimetableTimes.java tests/java/app/planaloud/TimetableTimesTest.java
java -cp /tmp/planaloud-test app.planaloud.TimetableTimesTest
```

DeepSeek multipart 测试需要 `org.json` 的 JVM jar，入口为 `tests/java/app/planaloud/voice/DeepSeekAsrTest.java`。Android SmokeTest 增加了直接删除与撤销、中文节次导入、40次课程的批量撤销，以及后续编辑的版本保护检查；需要在测试设备中运行。

此预览包使用调试签名，无法覆盖原正式签名安装。更换安装前请导出备份，勿直接清除现有数据。正式版更新签名和自动更新清单均未替换。

构建不内嵌本地原生语音引擎的 APK（系统语音、DeepSeek、百炼以及按需下载的本地引擎均保留）：

```sh
cd android
gradle -PskipLocalVoiceBuild=true :app:assembleDebug :app:compileDebugAndroidTestJavaWithJavac :app:lintDebug
```

不传此属性时仍按原配置构建本地原生引擎，需要先获取 Whisper 源码及对应 NDK/CMake。

## 本次验证结果

- Gradle `:app:assembleDebug`、`:app:compileDebugAndroidTestJavaWithJavac`、`:app:lintDebug` 成功。
- Lint：0 个错误、102 个警告；修复了原有资源 ID、Android 8 主题属性和受限 API 使用导致的检查错误。
- 26 项节次解析检查和 3 项 DeepSeek multipart 格式检查通过。
- APK 版本 `1.5.1-preview.1`（151），最低 Android 8.0（API 26），APK v2 签名校验通过。
- Android 设备上的 SmokeTest、真机触摸/返回/定位，以及使用真实密钥的在线 ASR 尚未执行。

# AnyWiki Reader MVP (Android/Kotlin)

本实现基于现有 Android 工程栈（Kotlin + Jetpack Compose + OkHttp + Kotlinx Serialization），在仓库内新增了一个独立的 `AnyWiki Reader` MVP 入口。

## 核心能力

- Wiki 站点接入与 API 自动发现（`/api.php`、`/w/api.php`、`/wiki/api.php`）
- 来源列表管理（添加、移除、切换当前来源）
- 当前来源站内搜索（Action API `list=search`）
- 文章阅读（Action API `action=parse`，支持目录展示、暗色、字体大小、链接处理）
- 阅读工具（历史、收藏、最近文章缓存、主题模式）
- 失败回退（解析失败自动切到原始 URL 回退模式）

## 代码结构

- `app/src/main/java/org/wikipedia/anywiki/mediawiki/`：MediaWiki API 客户端、发现逻辑、类型定义
- `app/src/main/java/org/wikipedia/anywiki/storage/`：本地存储（来源、缓存、收藏、历史、阅读设置）
- `app/src/main/java/org/wikipedia/anywiki/screens/`：页面级 Compose 屏幕
- `app/src/main/java/org/wikipedia/anywiki/components/`：通用 UI 组件（渲染器、空状态、来源卡片）
- `app/src/main/java/org/wikipedia/anywiki/AnyWikiViewModel.kt`：状态与业务编排
- `app/src/main/java/org/wikipedia/anywiki/AnyWikiReaderActivity.kt`：MVP 启动入口

## 启动入口

`app/src/main/AndroidManifest.xml` 已将 launcher alias 指向 `AnyWikiReaderActivity`，直接运行应用即可进入 AnyWiki Reader。

## 测试

- `app/src/test/java/org/wikipedia/anywiki/mediawiki/MediaWikiDiscoverTest.kt`
- `app/src/test/java/org/wikipedia/anywiki/mediawiki/MediaWikiClientUrlTest.kt`

当前沙箱环境未配置 Android SDK，无法在此环境完成 Gradle 构建与测试执行；请在本地配置 `ANDROID_HOME` 或 `local.properties` 后运行。

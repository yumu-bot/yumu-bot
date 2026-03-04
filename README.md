<p style="text-align: center">
 <img src=".github/assets/logo.png" width="580" height="340" alt="YUMU">
</p>

<div style="text-align: center">

# Yumubot

*连接 osu! 并处理数据的 bot 后端*

</div>

<p style="text-align: center">
    <a href="https://docs.365246692.xyz">使用文档</a>
    ·
    <a href="#运行">部署文档</a>
    ·
    <a href="#相关项目">相关项目</a>
</p>

<div style="text-align: center">

[![YUMU](https://repobeats.axiom.co/api/embed/2e023b139aefd7ca09d5d1ba6fbd80d54f2d05da.svg "Repobeats analytics image")](https://github.com/yumu-bot/yumu-bot)

</div>

# 运行

## 下载 jar 包

访问 [release](https://github.com/yumu-bot/yumu-bot/releases) 下载最新版本的 jar 包

## 运行环境

### 必备

- JDK 21
  - 或者支持虚拟线程的更高版本
  - 目标 Kotlin 版本是 2.3
  - 在 260228 之前的提交，使用的是 Kotlin 2.0
  - 如果你想测试并运行一个较早版本的实例，可能需要 JDK 17（不需要 Kotlin）
- PostgreSQL
  - 最低 15，越新越好

### 可选

简称你必然会踩到的坑

- Redis 数据库
  - 如果你没有装 Redis，请注释掉所有 @Cacheable（因为 application.yaml 配置项没有用）
  - 将 IdempotentService 改成简单的 Caffeine 实现
- rosuPP 本地计算
  - 如果你没有拿到 rosu-pp-v0.4.0，请前往 OsuCalculateApiService.kt，将 LOCAL 改成 false
  - 为什么这些不写到配置文件里？我也不知道。我本地都跑不通

## 配置文件

项目使用 spring boot 开发, 默认文件配置在 [application.yaml](src/main/resources/application.yaml), 根据注释进行修改.

可选: 在项目目录中创建 `logback.xml` 文件, 用于配置日志输出.

运行 `java -jar yumu.jar` 启动项目.

# 附属项目

## 必需

- [yumu-image](https://github.com/yumu-bot/yumu-image) 绘图模块, 用于将获取的数据绘制成可视化图片。**目前必须搭配主程序运行**。

## 可选

- [yumu-docs](https://docs.365246692.xyz/) 帮助文档

# 相关项目

本项目参考以下项目，或是使用了以下项目的 API：

- [rosu-pp](https://github.com/MaxOhn/rosu-pp) rosu：提供了 osu!pp 计算模块, 用于计算 osu! 谱面的星级和表现分
- [onebot11](https://onebot.dev) 提供了机器人协议标准, 用于与 QQ 机器人通信
- [diving-fish](https://www.diving-fish.com/maimaidx/prober/#Tutorial) 水鱼查分器：提供了 maimai、chunithm 的账号数据和成绩
- [Lxns-Network](https://maimai.lxns.net/docs) 落雪咖啡屋：提供了部分 maimai、chunithm 的歌曲信息和外号库
- [bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect/) 哔哩哔哩 - API 收集整理：提供了 bilibili 的部分 API 信息
- [PP+](https://github.com/Syriiin/difficalcy-performanceplus) PP+：提供了表现分加功能的算法
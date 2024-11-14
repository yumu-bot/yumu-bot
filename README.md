<p align="center">
 <img src=".github/assets/logo.png" width="580" height="340" alt="YUMU">
</p>

<div align="center">

# YUMU bot

_连接osu!并处理数据的bot后端_

</div>

<p align="center">
    <a href="https://docs.365246692.xyz">使用文档</a>
    ·
    <a href="#Run">部署文档</a>
    ·
    <a href="#Thanks">相关项目</a>
</p>

<div align="center">

[![YUMU](https://repobeats.axiom.co/api/embed/2e023b139aefd7ca09d5d1ba6fbd80d54f2d05da.svg "Repobeats analytics image")](https://github.com/yumu-bot/yumu-bot)

</div>


# Run
## 下载 jar 包
访问 [releas](https://github.com/yumu-bot/yumu-bot/releases) 下载最新的 jar 包

## 运行环境

- Jdk 21 版本及以上, 或者支持VirtualThread的其他版本
- PostgreSQL 15

## 配置文件
项目使用 spring boot 开发, 默认文件配置在 [application.yaml](src/main/resources/application.yaml), 根据注释进行修改.
可选: 在项目目录中创建 `logback.xml` 文件, 用于配置日志输出.
运行 `java --enable-preview -jar yumu.jar` 启动项目.

# Thanks

本项目实现, 感谢以下项目:

- [yumu-image](https://github.com/yumu-bot/yumu-image) 绘图模块, 用于将获取的数据绘制成可视化图片
- [rosu-pp](https://github.com/MaxOhn/rosu-pp) osu! pp计算模块, 用于计算osu!谱面的星级属性以及成绩pp值
- [onebot11](https://onebot.dev) 机器人协议标准, 用于与QQ机器人进行通信
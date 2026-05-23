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

## 指定配置文件启动

项目支持通过命令行参数指定自定义配置文件路径，适用于不同环境（本地/服务器）切换：

```bash
# Linux / macOS
java -jar nowbot-v0.8.0.jar --spring.config.location=/opt/yumubot/application.yaml

# Windows (PowerShell)
java -jar nowbot-v0.8.0.jar --spring.config.location=C:\yumubot\application.yaml

# 使用默认配置文件（放在 jar 同目录下）
java -jar nowbot-v0.8.0.jar
```

> **提示**: 配置文件必须与 jar 包在同一目录，或使用绝对路径指定。推荐为不同环境创建独立的配置文件：
> - `application-windows.yaml` — Windows 本地开发
> - `application-server.yaml` — Linux 服务器部署

## 数据库配置

### PostgreSQL 安装与初始化

#### Linux

```bash
# 1. 安装 PostgreSQL
sudo apt update
sudo apt install postgresql postgresql-contrib

# 2. 切换到 postgres 用户并进入 psql
sudo -i -u postgres
psql

# 3. 在 psql 中执行以下 SQL 完成全部授权
CREATE DATABASE bot;
CREATE USER yumu WITH PASSWORD '你的密码';
GRANT ALL PRIVILEGES ON DATABASE bot TO yumu;
\c bot
GRANT ALL ON SCHEMA public TO yumu;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO yumu;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO yumu;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO yumu;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO yumu;
ALTER USER yumu WITH SUPERUSER;
\q
```

#### Windows

```powershell
# 1. 进入 PostgreSQL bin 目录（根据安装版本调整路径）
cd "C:\Program Files\PostgreSQL\15\bin"

# 2. 连接数据库（默认用 postgres 超级用户）
.\psql -U postgres

# 3. 执行与上面相同的 SQL 授权语句
```

### application.yaml 中数据库相关配置

```yaml
spring:
  datasource:
    # PostgreSQL 连接地址（注意端口号，默认 5432，可能被改为 5433 等）
    url: jdbc:postgresql://127.0.0.1:5432/bot
    username: yumu          # 与上面 CREATE USER 创建的用户名一致
    password: 你的密码      # 与上面设置的密码一致
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10    # 最大连接数
      minimum-idle: 1          # 最小空闲连接
```

> **常见错误排查**:
> - `FATAL: database "bot" does not exist` → 先执行 `CREATE DATABASE bot;`
> - `FATAL: password authentication failed` → 检查用户名和密码是否正确
> - `permission denied for schema public` → 执行 `GRANT ALL ON SCHEMA public TO 用户名;`
> - `must be owner of table xxx` → 执行 `ALTER USER 用户名 WITH SUPERUSER;`

## 配置文件详解

以下是 `application.yaml` 各配置项的详细说明：

### 域名设置 (`yumu.*-domain`)

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `private-domain` | 内网访问地址（不带端口） | `http://localhost` |
| `bind-domain` | 绑定域名，用于 OAuth 回调。**公网可访问**才能使用链接绑定和 friend 功能 | `http://dc1.zerowolf.top:20009` |
| `public-domain` | 公网域名。如果无法通过公网访问则保持与 private-domain 一致 | `http://dc1.zerowolf.top:20009` |

> **本地测试提示**: 如果没有公网域名/内网穿透，`bind-domain` 和 `public-domain` 可以填 `http://localhost:你的端口`，此时 OAuth 绑定不可用，但可以用 `!bind osu用户名` 直接按名字绑定。

### 文件路径设置 (`yumu.file`)

| 配置项 | 说明 | Windows 示例 | Linux 示例 |
|--------|------|-------------|-----------|
| `root` | 基目录（所有其他路径基于此） | `C:/yumubot/` | `/opt/yumubot/` |
| `bgdir` | 图片素材目录 | `${yumu.file.root}img/` | |
| `font` | 字体文件目录 | `${yumu.file.root}font/` | |
| `imgbuffer` | 图片缓存目录 | `${yumu.file.root}imgbuffer/` | |
| `osuFilePath` | .osu 谱面文件缓存 | `${yumu.file.root}osufile/` | |
| `exportFile` | ExportFileV3 素材目录（来自 yumu-image releases） | `${yumu.file.bgdir}ExportFileV3/` | |

### osu! API 设置 (`yumu.osu`)

| 配置项 | 说明 |
|--------|------|
| `id` | osu! OAuth 应用 ID（在 https://osu.ppy.sh/home/account/edit 注册获取） |
| `token` | osu! OAuth 应用 Token |
| `callbackPath` | OAuth 回调接口端点（默认 `/oauth`） |

### 服务端口 (`server.port`)

| 环境 | 推荐端口 |
|------|---------|
| 本地开发 | `20009` 或任意未被占用的端口 |
| 服务器部署 | `20009` 或根据需要调整 |

### WebSocket 设置 (`shiro.ws`)

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.enable` | 是否启用 OneBot WebSocket 服务 | `true` |
| `server.url` | OneBot WebSocket 路径 | `/pub/onebotSocket` |
| `timeout` | 超时时间（秒） | `10` |

QQ Bot 客户端（NapCat/Lagrange 等）应连接到：`ws://主机地址:server.port/shiro.ws.server.url`
例如：`ws://localhost:20009/pub/onebotSocket`

## Redis 连接问题解决

### 问题现象

启动时出现以下错误之一：
```
Unable to connect to localhost/<unresolved>:6379
NOAUTH HELLO must be called with the client already authenticated
WRONGPASS invalid username-password pair or user is disabled
```

### 根本原因

程序内部使用了 Spring Data Redis + Lettuce 客户端，**无法通过配置文件完全禁用**。当环境中存在 Redis 服务时，程序会尝试连接它。

### 解决方案：Redis 不设置密码（唯一有效方法）

#### 非 Docker 方式

编辑 Redis 配置文件（通常位于 `/etc/redis/redis.conf`），找到并注释掉密码行：

```conf
# requirepass 你的旧密码
```

然后重启 Redis：

```bash
sudo systemctl restart redis
```

#### Docker 方式

停止并删除现有带密码的容器，重新启动一个无密码的容器：

```bash
# 查看当前 Redis 容器名称
docker ps | grep redis

# 停止并删除现有容器
docker stop <redis-container-name>
docker rm <redis-container-name>

# 启动新的无密码 Redis 容器
docker run -d \
  --name redis \
  --restart unless-stopped \
  -p 6379:6379 \
  redis:latest
```


> **常见错误排查**:
> - `NOAUTH ... must be authenticated` → Redis 有密码但程序默认无密码连接 → **去掉 Redis 密码**
> - `WRONGPASS invalid username-password pair` → 配置里的密码和 Redis 实际密码不匹配 → **统一密码或去掉密码**
> - `Unable to connect to localhost:6379` → Redis 未启动 → 启动 Redis 或**停止/卸载 Redis**

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
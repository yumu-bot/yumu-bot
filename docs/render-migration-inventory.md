# 渲染迁移只读盘点（65 注册面板）

## 0. 范围与事实基线

- 源实现：`/home/spring/IdeaProjects/yumu-image`
- 参考实现：`/home/spring/IdeaProjects/DrawTest`（只读）
- 当前项目：`/home/spring/IdeaProjects/yumu`
- 本轮只做静态盘点和文档落盘，未编译、未运行、未修改任何代码。
- 迁移范围：`main.js` 注册的**全部 65 个面板**。没有调用方的面板（E6、M、Beta、Eta1–Eta4）仍属于范围，只是排期可后置；Beta/Delta 的源端缺陷单列，最终按产品决策处理，不等于移出范围。
- 不迁移：`src/panel/` 中未注册的 5 个文件 `panel_D0.js`、`panel_E.js`、`panel_E2.js`、`panel_E3.js`、`panel_V3.js`。

已核对的事实：

- `yumu-image/main.js:21-58` 的 `panelSuffixes` 共 65 项。
- 每个注册项都同时注册 `panel_X` 与 `panel_X/svg`，共 130 个 handler（`main.js:50-53`）。
- `src/panel/` 有 70 个 `panel_*.js`；65 个注册 + 5 个未注册。
- 65 个注册文件中，`router` 和 `router_svg` 都存在。
- Kotlin 侧实际使用 58 个面板；7 个注册但当前无调用方：`E6`、`M`、`Beta`、`Eta1`、`Eta2`、`Eta3`、`Eta4`。

## 1. 65 注册清单（去重覆盖）

格式标记：

- `DEF`：`createImageRouter` 默认格式，即 `DEFAULT_IMAGE_FORMAT = process.env.IMAGE_FORMAT ?? 'jpeg'`。它是运行期可配置的，不等同于固定 jpeg。
- `ADV`：`createImageRouter(..., ADVANCED_IMAGE_FORMAT)`，`ADVANCED_IMAGE_FORMAT = process.env.ADVANCED_FORMAT ?? 'webp'`。
- `ADV(compact)`：A4/A5 的动态路由：正常为 `DEF`，`fields.compact === true` 时改为 `ADV` 并把 `compact` 改回 false。
- `BUFFER(webp)`：`createBufferRouter`，默认 webp，面板函数直接返回 Buffer。
- 所有行都同时有 `router_svg`（SVG 输出）。

| 面板 | 输出 | 直接 card | 直接 component | 直接 util | Kotlin 调用方 / 状态 |
|---|---|---|---|---|---|
| A1 | DEF | card_A1 | — | download,image,panelGenerate,util | FriendService |
| A2 | DEF | card_A2,card_A5 | — | download,font,image,panelGenerate,util | QualifiedMapService |
| A3 | DEF | card_A2,card_A4 | — | image,panelGenerate,util | BotWebApi,LeaderBoardService |
| A4 | ADV(compact) | card_A1,card_C,card_I4 | — | download,image,mascotBanner,panelGenerate,util | BotWebApi,BestHistoryRecover,RecentBest,SBBP,SBTodayBP,TodayBP,BP |
| A5 | ADV(compact) | card_A1,card_C,card_I4 | — | download,image,mascotBanner,panelGenerate,util | BotWebApi,MatchRecent,SBScorePR,LeaderBoard,SBScore,ScorePR,Score |
| A6 | ADV | card_A1 | component_MD | image,mascotBanner,panelGenerate,util | HelpService,SeriesRating,ServiceCount,SetGroupMode,ServiceSwitch,PrivateMessage |
| A7 | DEF | card_A1,card_C | — | font,image,panelGenerate,util | BPFixService |
| A8 | DEF | card_A1,card_C3 | — | download,image,mascotBanner,panelGenerate,util | GuessService |
| A9 | DEF | card_A1,card_A2 | component_MD | download,font,image,panelGenerate,text,util | TeamService |
| A10 | DEF | card_A1,card_C2 | — | image,panelGenerate,util | BadgeService |
| A11 | DEF | card_A1 | — | download,font,image,panelGenerate,util | GuestDifficultyService |
| A12 | DEF | card_A1,card_C | — | font,image,mascotBanner,panelGenerate,util | ExploreService |
| A13 | DEF | card_A1,card_A3 | — | font,image,panelGenerate,util | ExploreService |
| A14 | DEF | card_A2,card_A3 | — | font,image,panelGenerate,util | ExploreService |
| A15 | DEF | card_A1,card_C | — | font,image,panelGenerate,util | TopPlaysService |
| A16 | DEF | card_A2,card_A4 | — | font,image,panelGenerate,util | GroupLeaderBoardService |
| B1 | DEF | card_A1,card_B6,card_B7 | label | color,font,image,mascotBanner,panelDraw,panelGenerate,star,util | BotWebApi,PPMinus,TestPPMinus |
| B2 | DEF | card_A2,card_B6,card_B7 | label | color,font,image,panelDraw,panelGenerate,star,util | MapMinusService |
| B3 | DEF | card_A1,card_A2,card_B6,card_B7 | label | color,font,image,panelDraw,panelGenerate,star,util | PPPlusService,PPPlusMapService |
| B4 | DEF | card_A1,card_B6,card_B7 | label | color,font,image,mascotBanner,panelDraw,panelGenerate,star,util | EliteronixDuelRatingService |
| C | ADV | card_A2,card_C | — | download,image,panelGenerate,util | BotWebApi,MuRatingService |
| C2 | ADV | card_A2,card_C | — | download,image,mascotBanner,panelGenerate,text,util | SeriesRatingService |
| D | DEF | card_A1 | component_D1N,component_D5,component_D6N,component_D7 | image,mascotBanner,panelGenerate,text,util | InfoService / SBInfoService（动态） |
| D2 | DEF | card_A1 | label | color,font,image,mascotBanner,panelDraw,panelGenerate,text,util | InfoService / SBInfoService（动态） |
| D3 | DEF | card_A1 | label | color,font,image,mascotBanner,panelDraw,panelGenerate,text,util | BotWebApi,InfoService / SBInfoService（动态） |
| E5 | DEF | card_A1 | label | color,font,image,mod,panelDraw,panelGenerate,star,text,util | BotWebApi,BestHistoryRecover,MatchRecent,SBTodayBP,TodayBP,LeaderBoard |
| E6 | DEF | card_A1 | label | color,font,image,mod,panelDraw,panelGenerate,star,text,util | 注册，当前无调用方 |
| E7 | DEF | card_A2 | label | color,font,image,mod,panelDraw,panelGenerate,star,text,util | MatchMapService,MatchListenerService |
| E10 | DEF | — | label | color,font,image,mod,panelDraw,star,util | 多处 E10/E5 条件调用 |
| F | DEF | card_A2,card_F | — | download,image,mod,panelGenerate,util | BotWebApi,MatchNowService,QuickplayService |
| F3 | DEF | card_A2,card_F2,card_F3 | — | font,image,panelDraw,panelGenerate,util | MatchListenerService |
| F4 | DEF | card_F4 | — | download,image,util | MatchRoundService |
| H | DEF | card_A2,card_H | — | image,mascotBanner,util | BotWebApi,GetPoolService,MapPoolService |
| J | DEF | card_A1,card_D | component_J,label | color,download,font,image,mascotBanner,panelDraw,panelGenerate,util | BPAnalysisLegacyService / BPAnalysisService（动态） |
| J2 | DEF | card_A1 | label | color,font,image,mascotBanner,mod,panelDraw,panelGenerate,util | BotWebApi,BPAnalysisService（动态） |
| K | DEF | card_A1,card_B6,card_B7,card_K | label | color,font,image,mascotBanner,panelDraw,panelGenerate,star,util | SkillService |
| M | DEF | card_A1 | component_IM1,component_IM2,component_IM3,component_IM4 | font,image,mascotBanner,panelDraw,panelGenerate,util | 注册，当前无调用方 |
| M2 | DEF | card_A1,card_D2 | label | color,download,font,image,panelDraw,panelGenerate,text,util | BotWebApi,IMapperService |
| N | DEF | card_A2 | component_IM1 | download,font,image,panelDraw,panelGenerate,util | BotWebApi,NominationService |
| R | DEF | — | label | color,font,image,mod,panelDraw,star,text,util | BotWebApi,MapStatisticsService |
| S | DEF | card_A1 | label | color,download,font,image,panelDraw,panelGenerate,text,util | QuickplayInfoService |
| T | DEF | card_A2,card_C | label | color,font,image,mascotBanner,panelDraw,panelGenerate,util | PopularService |
| U | DEF | card_A1 | — | font,image,mascotBanner,panelDraw,panelGenerate,text,util | FriendService |
| V | ADV | card_A2 | component_V | font,image,osuFile,panelDraw,panelGenerate,star,util | ViewService |
| V2 | ADV | card_A2 | component_V2 | font,image,osuFile,panelDraw,panelGenerate,star,util | ViewService |
| Alpha | DEF | — | — | font,image | BPAnalysisService,MapPoolService,UUBAService |
| Beta | DEF | — | — | image,util | 注册，当前无调用方；源端有必崩缺陷 |
| Gamma | DEF | — | — | color,font,image,panelDraw,star,util | BotWebApi,PPMinus,InfoCard,ScorePRCard + getPanelGamma 重载 |
| Delta | DEF | — | — | color,font,image,mod,panelDraw,util | KitaService；源端有 NaN 缺陷 |
| Epsilon | BUFFER(webp) | — | — | color,font,image,panelDraw,text,util | OldAvatarService/SBOldAvatarService（动态） |
| Epsilon2 | DEF | — | — | font,image,panelDraw,text,util | OldAvatarService/SBOldAvatarService（动态） |
| Zeta | DEF | — | — | image,util | OsuAvatarCardService |
| Eta1 | DEF | — | — | color,font,image,mascotBanner,mod,panelDraw,star,text,util | 注册，当前无调用方 |
| Eta2 | DEF | — | — | color,font,image,mod,star,text,util | 注册，当前无调用方 |
| Eta3 | DEF | — | — | color,font,image,mascotBanner,mod,panelDraw,star,text,util | 注册，当前无调用方 |
| Eta4 | DEF | — | — | color,font,image,mod,panelDraw,util | 注册，当前无调用方 |
| Theta | DEF | — | — | font,image,maimai,util | MaiVersusService |
| MA | DEF | card_A1,card_I3 | — | color,font,image,mascotBanner,panelDraw,panelGenerate,util | MaiBestScoreService,MaiScoreService |
| MA2 | DEF | card_A1,card_I3 | — | color,font,image,maimai,mascotBanner,panelDraw,panelGenerate,util | ChuBestScoreService |
| MD | DEF | card_A1,card_I3 | — | image,maimai,mascotBanner,panelDraw,text,util | MaiDistributionService |
| MF | DEF | card_A1,card_MF | — | font,image,mascotBanner,panelGenerate,util | MaiFindService |
| ME | DEF | card_A1 | — | font,image,maimai,panelDraw,panelGenerate,text,util | ChuBestScoreService,MaiBestScoreService |
| MI | DEF | card_A1,card_I3 | — | font,image,mascotBanner,panelGenerate,util | MaiFilterService |
| MS | DEF | card_A2,card_MS | — | color,font,image,maimai,mascotBanner,panelDraw,panelGenerate,text,util | MaiScoreService |
| MV | DEF | card_A1 | — | color,font,image,maimai,mascotBanner,panelDraw,panelGenerate,text,util | MaiVersionScoreService |

### 1.1 数量核对

按功能段统计（与 `panelSuffixes` 一一对应）：

```text
A: A1..A16                    16
B: B1..B4                      4
C: C,C2                        2
D: D,D2,D3                     3
E: E5,E6,E7,E10                4
F: F,F3,F4                     3
H: H                           1
J: J,J2                        2
K: K                           1
M: M,M2                        2
N: N                           1
R,S,T,U                        4
alpha: Alpha,Beta,Gamma,Delta  4
greek: Epsilon,Epsilon2,Zeta   3
eta: Eta1..Eta4,Theta          5
mai: MA,MA2,MD,MF,ME,MI,MS,MV  8
合计                          65
```

去重覆盖：上表每一行只出现一次；没有把未注册的 D0/E/E2/E3/V3 计入。`E6`、`M`、`Beta`、`Eta1`–`Eta4` 均在表中，均属于迁移范围。

### 1.2 导出例外

- 所有 65 个文件都 export `router`、`router_svg`。
- panel 函数未 export、只在模块内使用的 4 个：`panel_F3`、`panel_F4`、`panel_R`、`panel_Zeta`。router 仍然正常。
- 额外导出：`panel_Beta.test`；`panel_E.stat2DataM`、`panel_E.data2Label`；`panel_Epsilon.panel_Epsilon_Svg`。
- `panel_Epsilon` 的 `router_svg` 使用 `panel_Epsilon_Svg`，不是 `panel_Epsilon`。

## 2. 输出格式契约

### 2.1 当前 Kotlin 已支持 WEBP

已核对，以下不是缺口：

- `src/main/java/com/now/nowbot/browser/ScreenshotOptions.kt:9`：`format: ScreenshotType = ScreenshotType.WEBP`。
- `ScreenshotOptions.kt:10`：`jpegQuality: Int = 80`。
- `ScreenshotOptions.kt:17`：仅限制 JPEG 不能与 `omitBackground=true` 同用，注释明确 `PNG, WEBP`。
- `src/main/java/com/now/nowbot/browser/RenderSlot.kt:157-159`：JPEG 与 WEBP 都会设置 quality。

因此 WebP 输出能力存在。真正需要补的是**逐面板格式策略**：每个面板要按源 router 的 `DEF/ADV/ADV(compact)/BUFFER(webp)` 选择格式，而不是让所有面板都用 `ScreenshotOptions` 的默认 WEBP。DrawTest 参考实现的 `PanelE5.render` 调用 `ScreenshotOptions(width,height)`，会走默认 WEBP；源 `panel_E5` 是 `createImageRouter` 默认 `DEF`（运行期 `IMAGE_FORMAT`，缺省 jpeg）。迁入时必须显式传入格式，这是核心 API 需要明确的契约，不是浏览器能力缺失。

### 2.2 输出映射

- `A4/A5`：默认 `DEF`；仅当请求体 `compact === true` 时改为 `ADV`，并同时把传给面板的数据 `compact` 改为 false。Kotlin 现有 9 处 `compact` 调用必须保留这两个语义。
- `A6/C/C2/V/V2`：固定 `ADV`。已纠正：C/C2/V/V2 不是 compact 触发，而是 router 直接传 `ADVANCED_IMAGE_FORMAT`。
- `Epsilon`：`createBufferRouter(panel_Epsilon)`，默认 webp，函数直接返回 Buffer；它的 SVG 路由用 `panel_Epsilon_Svg`。
- 其余注册面板：`DEF`，即运行期 `IMAGE_FORMAT`。不要把所有 `DEF` 面板硬编码为 jpeg。
- 未注册的 `V3` 虽然写死 webp，但不在迁移范围。

## 3. 非 SVG / 嵌套浏览器 / 特殊图片

### 3.1 Epsilon（唯一 buffer + 动画合成）

`src/panel/panel_Epsilon.js`：

- `:99` `exportWEBP(svg)`：先渲染整张旧头像面板。
- `:101` `compositeToWebP(svg_buffer, image, {x:70,y:40,width:320,height:320})`：把头像逐帧叠到背景，返回动画 WebP。
- `router = createBufferRouter(panel_Epsilon)` 默认 webp；`router_svg` 用 `panel_Epsilon_Svg`。
- 头像可能是 APNG/GIF/WebP 动画；`util/image.js` 用 `upng-js` 拆 APNG，再用 `node-webpmux` 逐帧合成。Kotlin 侧没有等价的动画 WebP 编码/合成链路，是最高风险项。

### 3.2 A6/A9（嵌套浏览器）

- `src/component/component_MD.js:53-72` 调 `renderMarkdown`。
- `src/util/svgRenderer.js:749-814`：启动本地 HTTP 服务，导航 `template/Card_Alpha/index.html`，等待 `window.setStr`，截 PNG，并返回真实宽高。
- `panel_A6.js` 把 PNG 作为 data URI 嵌回 SVG，再走外层 SVG 渲染；A6 输出 `ADV`。
- `panel_A9.js:164/172` 只在 `team.markdown_description` 非空时嵌 Markdown 截图，先剥 HTML/BBCode，宽度 1370，最后按 1410 宽、最高 240 放置。
- `template/Card_Alpha/` 内含 `index.html`、`assets/index.29dac38f.js`、`assets/index.3bc2ed74.css`；`markDownTemplate/` 是另一套同类资源。

### 3.3 Zeta（WASM 取色 + 模板函数）

`src/panel/panel_Zeta.js`：

- `readNetImage(data.banner)` 取 banner；`fs.readFile(resolveHttpPath(...))` 读本地文件。
- `call_core(new Uint8Array(banner_data), 5)` 来自 `color/wasm_wrapper.js`，使用 `color/wasm_wrapper_bg.wasm`（约 1.2MB）取主色。
- `data.color = color[0].to_hex()`；`compileTemplate("template/test/User_Avatar_Card.svg")` 后用 `new Function` 模板替换。
- 模板里的名字使用 `font-family:'JetBrainsMono NF'`，依赖系统字体；不同渲染环境字形会漂。

### 3.4 Beta / Delta 源端缺陷

- `panel_Beta.js:56-70`：`let ppData = {}` 后直接 `ppData.pp / ppData.perfect_pp`、`ppData.attr.stars`；计算 pp 的调用被注释。当前注册后每次请求都会抛 TypeError。该面板还使用 `fast-xml-parser` + `getSvgById/setSvgText` 的 XML DOM 路径。
- `panel_Delta.js:62`：`const attr = {} // await getMapAttributes(id, mod_int)`，随后使用 `attr.bpm/stars/cs/ar/od`；会输出 `NaN` 文字、`clamp(undefined)` 产生 `NaN` 宽度条。`KitaService.kt:74/84` 正在调用它。
- 两者仍在 65 范围内。已确认的处理原则：允许基于当前已有数据做最小修复，保持原布局公式；确实缺少数据时明确报错，不生成占位图。Beta 需要从可用成绩数据补齐 pp/attr 或明确报错；Delta 的 bpm/stars/cs/ar/od 从当前 `Beatmap` 已有属性补齐，不保留 `NaN`。

补充模板占位符缺陷：`Panel_A2.svg`、`Panel_A3.svg`、`Panel_H.svg` 的 `${panelheight}` 在 width/viewBox 等处重复出现，源码 `setText` 只替换第一处，输出可能残留 `${panelheight}`/`${height}`。本批 Kotlin 渲染按“保持布局公式 + 全部替换占位符”最小修复，属于源端缺陷修复，不改变布局。

补充 F4 原型缺陷：`src/panel/panel_F4.js:140` 只把 `scores[0]` 传给 `card_F4`，阈值 `100, 20000000, 500000, 0.96, 30000000` 全部硬编码；读取 `Panel_F2.svg` 后只替换 `BodyCard`，`reg_height`、`reg_panelheight`、`reg_maincard`、`reg_index`、`reg_banner` 均未使用，输出仍保留模板里的 `${height}` / `${panelheight}` 占位符。F4 是未完成原型；已确认允许基于现有 `Match`/`LazerScore` 数据最小修复，保持布局公式，缺数据明确报错，不生成占位图。

### 3.5 其它特殊素材

- D2：`panel_D2.js:802` 对 `user.profile.mascot` 调 `isPicturePng(path)`，决定 1920×790 透明竖图裁切或 820×710 slice。若 mascot 是远程 URL，`isPicturePng` 为 false；这会影响裁切分支，需要与下载层/资源 URL 策略一起处理。
- 国旗：`Flags/<CODE>` 是无扩展名 SVG，`getFlagPath` 用 `substring(60, len-6)` 内联；TW 特判 `flag-TW.png`，不存在时回退 CN。
- 图标：`component/label.js` 用 `readBinaryFromV3Cache('Icons','*.svg')` 内联 SVG。
- Alpha：纯文字面板，颜色由 `getRandomColor()` 随机取 16 色之一，输出非确定性；作为 BPAnalysis/MapPool 的 fallback。

## 4. ImageService 入口与参数

文件：`src/main/java/com/now/nowbot/service/ImageService.kt`。

| 位置 | 签名 | body / 说明 |
|---|---|---|
| :34 | `getPanel(body: Map<String, Any>, name: String)` | Map 直接作为 payload |
| :43 | `getPanel(any: Any?, name: String)` | 任意对象，主力入口 |
| :55/:70 | 旧 Markdown 入口 | 已删除，统一走 A6 |
| :81/:88/:95/:102/:115 | `getPanelA6(...)` 4 重载 + 完整版 | `markdown/name/width/user`；**没有 height**，但面板支持 `data.height ?? 600` |
| :130/:201 | `getPanelAlpha(vararg lines)` / `(StringBuilder)` | `strs` |
| :139-145 | `getPanelBeta(LazerScore?)` | 整段注释，无调用 |
| :147/:157/:166 | `getPanelGamma(...)` 3 重载 | score / info / sanity |
| :192 | `getPanelDelta(...)` | `beatmap/round/mod/position/hasBG` |

调用链：

- `ImageService` → `PanelRenderDispatcher.render(name, payload)`，在进程内完成面板渲染。
- 旧 WebSocket 渲染链路与 `/render-ws` 已删除；`JacksonUtil.mapper` 仅保留通用序列化能力。
- `JacksonUtil`（`util/JacksonUtil.kt`）全局 `SNAKE_CASE` + `NON_NULL`；JS 侧 `req.fields` 就是该对象。迁移时不要改成 camelCase，也不要依赖 null 字段存在。

### 4.1 动态 name

必须保留的运行时映射：

- `OldAvatarService.kt:294/302`、`SBOldAvatarService.kt:164/172`：`version == 2 -> Epsilon2`，否则 `Epsilon`；body `{user}`。
- `BPAnalysisService.kt:327`：`version == 1 -> J`，否则 `J2`；body `this.toMap()`；失败回退 `getPanelAlpha(*text)`。
- `InfoService.kt:252`：`1 -> D`，`2 -> D2`，`3 -> D3`，else `D`。
- `SBInfoService.kt:133`：`2 -> D2`，`3 -> D3`，else `D`。
- `MatchListenerService.kt:277/299`：`E7`、`F3`；`renderAndReply(param, panelType)` 目前只有这两处 literal。
- `SkillService.kt:210/212`：`K` + `panel:"KV"/"K"`。
- `F`：`panel:"MN"`（MatchNow/BotWebApi）或 `panel:"RP"`（Quickplay）。
- `F3`：`panel:"RR"`。
- `E5/E10`：6 处 `if (isShow) "E10" else "E5"`。

### 4.2 调用入口清单（58 个已用）

```text
A1 FriendService:164
A2 QualifiedMapService:100
A3 BotWebApi:893, LeaderBoardService:132
A4 BotWebApi:352/501, BestHistoryRecover:200, RecentBest:187, SBBP:317, SBTodayBP:219, TodayBP:230, BP:242
A5 BotWebApi:391/430/695, MatchRecent:250, SBScorePR:371, LeaderBoard:108, SBScore:298, ScorePR:353, Score:468
A6 HelpService:84/114, SeriesRating:58, ServiceCount:88, SetGroupMode:61, ServiceSwitch:414, PrivateMessage:91
A7 BPFix:261
A8 Guess:1212
A9 Team:115
A10 Badge:91
A11 GuestDifficulty:67/87
A12/13/14 Explore:328/329/330
A15 TopPlays:122
A16 GroupLeaderBoard:106
B1 BotWebApi:134/163, PPMinus:298, TestPPMinus:117
B2 MapMinus:159
B3 PPPlus:111, PPPlusMap:83
B4 EliteronixDuelRating:56/139
C BotWebApi:256, MuRating:92/124
C2 SeriesRating:115
D/D2/D3 动态（见 4.1）+ BotWebApi:919(D3)
E5 BotWebApi:365/405/444/701, BestHistoryRecover:211, MatchRecent:248, SBTodayBP:227, TodayBP:237, LeaderBoard:117
E7 MatchMap:118, MatchListener:277
E10 条件调用（见 4.1）
F BotWebApi:205, MatchNow:53/88, Quickplay:67
F3 MatchListener:299
F4 MatchRound:96
Gamma BotWebApi:98, PPMinus:299 + getPanelGamma 重载
H BotWebApi:289, GetPool:70, MapPool:113
J BPAnalysisLegacy:87 + BPAnalysis 动态
J2 BotWebApi:735 + BPAnalysis 动态
K Skill:210/212
M2 BotWebApi:974, IMapper:51/74
MA MaiBestScore:144, MaiScore:112
MA2 ChuBestScore:162
MD MaiDistribution:120
ME ChuBestScore:172, MaiBestScore:160
MF MaiFind:61
MI MaiFilter:117
MS MaiScore:114
MV MaiVersionScore:190
N BotWebApi:1025, Nomination:76/106
R BotWebApi:834, MapStatistics:72/105
S QuickplayInfo:56
T Popular:407
Theta MaiVersus:51
U Friend:142
V View:106
V2 View:105
Alpha BPAnalysis:337, MapPool:85, UUBAService:65
Delta Kita:74/84
Epsilon/Epsilon2 动态（见 4.1）
Zeta OsuAvatarCard:54/67
```

### 4.3 入口层待办

- A6 增加 height 入口（或统一 typed render request）。
- 旧 Markdown 入口已删除；不再迁 `getPanelBeta`。
- 动态 name 增加 `when` 白名单校验，避免未知 name 拼路由。
- 逐面板显式格式参数，避免全部落到 `ScreenshotOptions` 默认 WEBP。

## 5. 渲染资源与下载策略

已核对：

- `ScreenshotOptions`/`RenderSlot` 已支持 WEBP（见第 2 节）。
- `RenderSlot.kt:437` 的 `ALLOWED_RESOURCE_SCHEMES = setOf("data","blob","about")`；页面不能直接加载任意 HTTP。
- `com.now.nowbot.image.resource.RenderResourceService` 已存在，端点 `/render-resource/{id}`（`RenderResourceController`）。资源 agent 正在补“受控资源 URL 的最小 allowlist 接入”。
- 下载层已由独立 agent 实现。最终策略是**复用已有缓存 + 缓存 miss 时按源版行为安全下载 fallback**，不是只读缓存、不是渲染器内重新发任意网络请求。
- 源行为参考：`util.js` 的 `readNetImage` 先查 `IMG_BUFFER_PATH/<MD5(url)>`，miss 时下载并压缩保存，失败时回退默认图/错误图；`getAvatar`、`getBanner`、`getMapBackground`、`getDiffBackground` 都走这一套。
- 页面内素材优先使用受控 `/render-resource` URL 或 data URI；不要放开任意外网。
- A6 的 Card_Alpha 需要 HTML/CSS/JS 资源；在 allowlist 未接好前，A6/A9 的嵌套 Markdown 不能完整验证。这是集成依赖，不是渲染器能力缺失。

## 6. DrawTest util 与当前 util 的重复

当前共享包已定为 `com.now.nowbot.image`；E5 agent 提供 util/svg/font/draw/CardA1。以下是静态盘点到的重复与保真差异。

### 6.1 数字：当前 `Numbers` 已按 JS double 语义实现

- 源：`yumu-image/src/util/util.js:1350-1560` 的 `rounds/floorOrRound`。进位阈值是 `Math.abs(dec) >= 1 - Math.pow(10, -level)`，并使用 JS `Math.round/Math.floor` 的 double 行为。
- 已知必须保留的怪行为：`floors(5.8, 2)` 在原版得到 `5.79`（`Math.floor(79.999...)`），不是 `5.80`。
- DrawTest `xyz.spring.util.number.Numbers` 用 `BigDecimal`/HALF_UP 和 `rounded >= factor`，会得到 `5.8`；**不要**拿它覆盖当前实现。
- 当前 `src/main/java/com/now/nowbot/image/util/Numbers.kt:157` 已使用原版阈值；方向正确。仍需补 5.8、1.95、负数、level=0、subLevel=-2 的对照测试，防止 `jsDecimal`/`BigDecimal.valueOf` 在边界上偏离 JS。

### 6.2 时间差 / DHMS

- `yumu-image/src/util/util.js:2019-2110` 的 `getTimeDifference`：
  - 单位前无空格：`years + 'years'`。
  - `years/months/days/hours/minutes` 各自独立 `compare_moment.diff(now, unit)`，不是逐单位借位 remainder。
  - `getTimeDifferenceShort` 默认 now = `moment().subtract(8,"hours")`。
- DrawTest `util/time/TimeKit.kt:51-96`：逐单位 cursor 借位，short=1/2 单位前有空格，short=0 无空格；月份 `mo/mos` 本身与原版一致。**不能直接复用。**
- `util.js:2135-2205` 的 `getTimeByDHMS`：返回 `b+m`，有 days 时只到 `d h m`，不带秒；`has_whitespace` 为 `1d 2h 3m`；`Large` 只 b，`Small` 只 m。DrawTest `durationText` 始终补秒，不保真；`getMatchDuration` 依赖此格式。
- `getTime`（秒转 `minute/seconds`）正向行为一致；负数和 `padStart` 边界需测试。

### 6.3 Svg.number

- DrawTest `util/svg/Svg.kt:23-29` 固定 `"%.3f"` 后 trim。
- 源 JS 模板直接拼 double 的 `toString`（最短表示）。坐标/path 的 3 位截断会造成像素级差异。
- E5 agent 的 `Svg.number` 不要沿用 3 位；保真关键处使用 JS 兼容的最短 round-trip 格式，普通展示坐标可单独商量。

### 6.4 字体

- 源：`yumu-image/src/util/font.js:196-205` 共 10 个：
  `torus`(Torus-SemiBold.ttf)、`PuHuiTi`(AlibabaPuHuiTi3.0-75SemiBold-CJKTGv4.3.ttf)、`extra`(extra.gamemode.ttf)、`torusRegular`(Torus-Regular.ttf)、`torusBold`(Torus-Bold.ttf)、`poppinsBold`(Poppins-Bold.ttf)、`lineSeedSans`(LINESeedSans_Bd.ttf)、`TahomaRegular`(ft71.ttf)、`TahomaBold`(tahomabd.ttf)、`BerlinBold`(BerlinsansExpert-Bold.ttf)。
- DrawTest 只有 6 个，缺 `lineSeedSans/TahomaRegular/TahomaBold/BerlinBold`。实际使用：Delta 用 lineSeedSans，Epsilon/Epsilon2 用 Tahoma，MV/card_MS 用 BerlinBold。
- 源文件在 `/home/spring/IdeaProjects/yumu-image/font/`；当前 `DataUtil.TORUS_REGULAR`（`util/DataUtil.kt:34-48`）失败时回退 `SANS_SERIF`，不能作为渲染保真源；当前 `static/font` 也缺这 4 个。
- DrawTest 用 AWT `TextLayout` 生成 path，源版用 `text-to-svg`；即使字体齐全，字形/字宽也不是像素级一致。全局风险，需要接受或另找等价实现。

### 6.5 模式 / Mod / 谱面

- 模式：DrawTest `GameModes.kt` 与当前 `model/enums/OsuMode.kt` 重复。当前 `OsuMode` 有 RELAX/7K/AUTOPILOT 等 8 变体，JSON 只输出 shortName；渲染层用 `safeModeValue` + 现有 `fullName/shortName/charName`，不要新建第二套 enum。`getGameMode(level=-1)` 的 E800–E803 字形要补。
- Mod：DrawTest `OsuMod` + `Mods.kt` 与当前 `model/osu/LazerMod.kt` 重复。当前 `LazerMod` 已有 acronym/settings/color/`ValueMod.value` 位掩码；迁移用适配器（settings 经 Jackson 转 Map），不要另建 `OsuMod`。DrawTest 只覆盖 `name/color/fullName/multiplier/hasMod/hasAnyMod/drawLazerMods`；缺 `getModInt`、`getModPath`、`getModCirclePath`、`getModRRectPath`、`getModsBody`、`matchMod/matchAnyMod(s)`、`hasAllMod`、`addMod/delMod/getAllMod`、`getModAdditionalInformation`、`hasModChangedSR`。`drawLazerMods` 依赖 `Mods/mod-icon.png`、`Mods/mod-icon-extender.png`、每 mod 的 `Mods/<acronym>.png`；README 已注明 `sv2.png` 缺失。
- 谱面：DrawTest 没有 parser；当前 `model/beatmapParse/OsuFile.kt` + `BeatmapApiImpl.getBeatmapFileFromDirectory` 已有 `.osu` 缓存/解析，V/V2 复用；缓存 miss 由已委派的下载层按源行为处理，不移植 `osuFile.js` 的页面内下载。当前 parser 的 `HitObject` 有 start/end/type/column，足够 V/V2；`sliderPath.js` 未被 V/V2 使用。

### 6.6 其它 util 缺口

- `Images.kt` 只有 load/loadFile/place/mimeType + AssetResolver；缺 `getAvatar/getBanner/getMapBackground/getDiffBackground/getFlagPath/getMapStatusImage/getRandomBannerPath/getMascot*`。
- `star.js` 的 `getRankFromValue`、`getRankBackground`、`getRankBackgroundForI4`、`getScoreTypeImage`、`getOsuDXRatingStar`、`rankSS2X` 未迁；`hasLeaderBoard` 在 `GameModes`。
- `label.js` 2383 行，DrawTest 只有 E5 的 `LABELS`（BPM/LENGTH/CS/KEY/AR/OD/HP）；`LABEL_PPP/MM/PPM6/PPM/ETX/S/IM/RANK_OPTION` 全缺，影响 B1–B4/C/D2/D3/Eta/J/J2/K/M2/N/R/T/MA/MS。
- `color.js` 缺 `PanelColor`、`colorArray`、`getUserRankColor`、`getStarTextColor`、`getGlobalRankPercentName`、`getPlayerClassColors/Background`、`getTagCategoryColor`、`getMapStatusColor`、`getBadgeColor`、`getColorInSpectrum` 等。
- `panelGenerate.js` 2051 行，DrawTest 只有 E5Conversions；各卡片需要的 `user2CardA1/score2CardC/score2CardI4/score2CardD2/beatmap2CardA2/team2CardA2/matchRating2CardA2...` 是面板批次的最大共享前置。
- `mascotBanner.js` 全缺（随机 Banner/Mascots/Transparent/排名背景），影响 A4/A5/A6/A8/A12/B1/B4/C2/D/D2/D3/Eta1/Eta3/J/J2/K/M/MA/MA2/MD/MF/MI/MS/MV/T/U。
- `maimai.js` 全缺（MA/MA2/MD/ME/MF/MI/MS/MV/Theta）；需要 ExportFileV3 的 Maimai/Chunithm 素材。
- `text.js` 的 `layoutStatTexts`、`splitMatchName`、`isNotNumber` 等未全迁。

## 7. 面板家族与并行拆分（全部 65）

共享前置（Wave 0/1，必须先于面板批次）：

- `com.now.nowbot.image.util`：Svg/Fonts/Colors/Time/Numbers/Mods/GameModes/Images。
- `card/CardA1.kt`（E5 agent）。
- 共享 card：CardA2、CardA3、CardA4、CardA5、CardB6、CardB7、CardC、CardC2、CardC3、CardD、CardD2、CardF、CardF2、CardF3、CardF4、CardH、CardI3、CardI4、CardK、CardMF、CardMS。
- 共享 component：Labels、PanelGenerate、component_MD、component_D1N/D5/D6N/D7、component_J、component_V/V2、component_IM1–IM4。
- 输出格式策略与资源 URL allowlist/下载适配。

### G1 个人信息/头像（12）

`A1,A10,A11,A16,D,D2,D3,Gamma,U,Epsilon,Epsilon2,Zeta`

- 共享 CardA1、Images、Labels、mascot。
- Epsilon/Zeta 特殊：Epsilon 动画 buffer、Zeta WASM 取色/模板，建议各自单独子任务。
- 依赖：CardA1 + getAvatar/getBanner + 字体（Epsilon2 的 Tahoma）。

### G2 成绩列表/单成绩（18）

`A3,A4,A5,A7,A12,A15,B1,B2,B3,B4,C,C2,E5,E6,E10,J,J2,K`

- 共享 CardC/I4/B6/B7/K、Labels、star/mod/color/panelGenerate。
- E5 已有 E5 agent 核心；E6 无当前调用方但仍在范围，可后置。
- A4/A5 要保留 compact→ADV 语义。
- J/J2/E10 的排序、统计、mod 文案依赖 panelGenerate/label。

### G3 谱面/图池/比赛/信息（19）

`A2,A8,A9,A13,A14,E7,F,F3,F4,H,M2,N,R,S,T,V,V2,Delta,Theta`

- 共享 CardA2/A3/A4/A5/F*/H、OsuFile、panelGenerate。
- A9 需要 Markdown 嵌套；Delta 源端 NaN 待决策；V/V2 复用当前 OsuFile + 下载层；Theta 也可归 G5。
- F/F3/F4 依赖 match 数据结构与 card_F*。

### G4 Markdown（3）

`A6,Alpha,Beta`

- A6 需要 Card_Alpha 静态资源 + `window.setStr` + 内层 PNG；Alpha 是纯文字 fallback；Beta 源端必崩，最终按产品决策（仍在 65 范围）。
- 依赖资源 allowlist、字体、CardA1。

### G5 maimai（8）

`MA,MA2,MD,ME,MF,MI,MS,MV`

- 依赖 maimai.js、CardI3/MF/MS、Maimai/Chunithm 素材、maimai 数据模型。
- 可独立于 osu 面板并行；但共享 Labels/Images/Colors。

### G6 注册但当前无调用方（5）

`M,Eta1,Eta2,Eta3,Eta4`

- 属于确认的 65 范围，不是“暂不迁”；只是没有入口，排期可后置。
- 依赖 CardA1、CardB6/B7/CardK、IM 组件、mod/star/color，基本可复用 G1/G2 的底座。
- E6 在 G2（有注册、无调用方），不在 G6。

### 建议排期

1. Wave 0：核心格式 API、资源 allowlist、下载适配、util/svg/font/draw/CardA1。
2. Wave 1：共享 cards/components（按 G1/G2/G3 的实际依赖拆）。
3. Wave 2：G1、G2、G3 并行；G4/G5 在资源底座稳定后并行；G6 最后但必须完成。
4. 先导验证：`Alpha`（48 行，纯字体）、`F4`（144 行，单 card）、`E10`（label + mod + star，无 card），用于验证格式/字体/资源链路。
5. Beta/Delta 单独决策，不阻塞其它 63 个。

## 8. 必要资源来源

- 字体：`/home/spring/IdeaProjects/yumu-image/font/`（10 个 ttf，见 6.4）。
- 模板：`/home/spring/IdeaProjects/yumu-image/template/`：
  `Panel_A1..A6,B,C,D,Delta,F,F2,H,J,M,N,Gamma,A4,MA,MS.svg`、`Card_A1,A2,B1,B2,D,H0,C.svg`、`sp.svg`、`Mod/*.svg`、`test/User_Avatar_Card.svg`、`Card_Alpha/{index.html,assets/index.29dac38f.js,assets/index.3bc2ed74.css}`、`markDownTemplate/*`。
  当前项目只有 `src/main/resources/templates/draw/{Card_A1.svg,Panel_E.svg}`。
- ExportFileV3：当前 `yumu.file.exportFile`（`application.yaml:44`；`FileConfig.java:28` 默认 `/tmp/bot/img/ExportFileV3`；README 示例 `/home/spring/work/img/ExportFileV3`）。需要 `Banner/c1..c8.webp`、`Mascots/**`、`Mascots/Transparent/**`、`Flags/<CODE>`、`flag-TW.png`、`Mods/**`、`Icons/*.svg`、`object-score-*`、`object-beatmap-*`、`object-type-*`、`card-default.webp`、`avatar-guest.png`、`sticker_qiqi_secretly_observing.png`、`panel-oldavatar.png`、`panel-kita*.webp`、`Maimai/**`、`Chunithm/**`、`Help/**`。
- Zeta 颜色：`/home/spring/IdeaProjects/yumu-image/color/wasm_wrapper_bg.wasm` + `wasm_wrapper.js`。
- 浏览器：系统 Chrome；当前 `BrowserConfig.executablePath` 留空由 Playwright 发现；Playwright 在 `pom.xml:142`。
- 缓存：`/tmp/bot/imgbuffer`、`/tmp/bot/osufile`（当前默认，可由 `yumu.file` 覆盖）；渲染层复用缓存，miss 走安全下载层。

## 9. 已确认决定与验证项

1. 逐面板格式契约：`DEF` 取 `IMAGE_FORMAT`，`ADV` 取 `ADVANCED_FORMAT`，A4/A5 compact，Epsilon buffer；不要统一用 `ScreenshotOptions` 默认 WEBP。
2. 受控资源 URL allowlist 的具体域名/路径/生命周期；A6/A9 的 Markdown 资源加载依赖它。
3. Epsilon 动画 WebP 合成方案（Java 编码器/外部进程/降级静态帧）。
4. 已确认：Beta/Delta/F4 允许基于当前已有数据做最小修复，保持原布局公式；缺数据明确报错，不生成占位图；仍在 65 范围。
5. 已确认：仅静态审查，不编译、不执行 tests、不启动程序。
6. 已确认：A5 的原下载 tasks 使用 `data.score`、业务实际传 `scores` 的错位，按“使用当前 score/scores 数据”最小修复。
7. 字体 `text-to-svg` 与 AWT `TextLayout` 的差异接受度。
8. `Svg.number` 的 JS 兼容格式实现。
9. 下载层与缓存 miss 的安全下载 fallback 接口；渲染器不要直接发任意网络请求。

## 10. 本轮检查清单

- 已读 `yumu-image/main.js`、`src/panel/*`、`src/card/*`、`src/component/*`、`src/util/{image,util,font,mod,text,color,star,panelGenerate,panelDraw,download,osuFile,sliderPath,mascotBanner,svgRenderer}.js`、`template/*`、`font/*`、`.env.example`、`README.md`。
- 已读当前项目 `ImageService.kt`、`RenderWebsocket.kt`、`ScreenshotOptions.kt`、`RenderSlot.kt`、`BrowserConfig.kt`、`JacksonUtil.kt`、`DataUtil.kt`、`image/util/Numbers.kt`、`image/resource/*`、`FileConfig.java`、`application.yaml`、`OsuMode.kt`、`LazerMod.kt`、`model/beatmapParse/*`。
- 未编译、未运行、未启动 Chrome、未修改参考项目；仅新增本文件。

package com.now.nowbot.image.render

import com.microsoft.playwright.options.ScreenshotType
import com.now.nowbot.entity.IDUser
import com.now.nowbot.image.card.CardC3Data
import com.now.nowbot.browser.HtmlRenderer
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.markdown.MarkdownRenderer
import com.now.nowbot.image.panel.PanelA1
import com.now.nowbot.image.panel.PanelA10
import com.now.nowbot.image.panel.PanelA11
import com.now.nowbot.image.panel.PanelA12
import com.now.nowbot.image.panel.PanelA13
import com.now.nowbot.image.panel.PanelA14
import com.now.nowbot.image.panel.PanelA15
import com.now.nowbot.image.panel.PanelA16
import com.now.nowbot.image.panel.PanelA2
import com.now.nowbot.image.panel.PanelA3
import com.now.nowbot.image.panel.PanelA4
import com.now.nowbot.image.panel.PanelA5
import com.now.nowbot.image.panel.PanelA6
import com.now.nowbot.image.panel.PanelA7
import com.now.nowbot.image.panel.PanelA8
import com.now.nowbot.image.panel.PanelA9
import com.now.nowbot.image.panel.PanelB1
import com.now.nowbot.image.panel.PanelB1Data
import com.now.nowbot.image.panel.PanelB2
import com.now.nowbot.image.panel.PanelB2Data
import com.now.nowbot.image.panel.PanelB3
import com.now.nowbot.image.panel.PanelB3Data
import com.now.nowbot.image.panel.PanelB4
import com.now.nowbot.image.panel.PanelC
import com.now.nowbot.image.panel.PanelC2
import com.now.nowbot.image.panel.PanelD
import com.now.nowbot.image.panel.PanelD2
import com.now.nowbot.image.panel.PanelD2Param
import com.now.nowbot.image.panel.PanelD3
import com.now.nowbot.image.panel.PanelD3Param
import com.now.nowbot.image.panel.PanelDParam
import com.now.nowbot.image.panel.PanelE10
import com.now.nowbot.image.panel.PanelE5
import com.now.nowbot.image.panel.PanelE7
import com.now.nowbot.image.panel.PanelF
import com.now.nowbot.image.panel.PanelF3
import com.now.nowbot.image.panel.PanelF4
import com.now.nowbot.image.panel.PanelH
import com.now.nowbot.image.panel.PanelJ
import com.now.nowbot.image.panel.PanelJ2
import com.now.nowbot.image.panel.PanelK
import com.now.nowbot.image.panel.PanelKData
import com.now.nowbot.image.panel.PanelM2
import com.now.nowbot.image.panel.PanelMV
import com.now.nowbot.image.panel.PanelN
import com.now.nowbot.image.panel.PanelNData
import com.now.nowbot.image.panel.PanelR
import com.now.nowbot.image.panel.PanelS
import com.now.nowbot.image.panel.PanelSParam
import com.now.nowbot.image.panel.PanelT
import com.now.nowbot.image.panel.PanelU
import com.now.nowbot.image.panel.PanelV
import com.now.nowbot.image.panel.PanelV2
import com.now.nowbot.image.panel.PanelAlpha
import com.now.nowbot.image.panel.PanelDelta
import com.now.nowbot.image.panel.PanelDeltaParam
import com.now.nowbot.image.panel.PanelEpsilon
import com.now.nowbot.image.panel.PanelEpsilon2
import com.now.nowbot.image.panel.PanelGamma
import com.now.nowbot.image.panel.PanelMA
import com.now.nowbot.image.panel.PanelMA2
import com.now.nowbot.image.panel.PanelMD
import com.now.nowbot.image.panel.PanelME
import com.now.nowbot.image.panel.PanelMF
import com.now.nowbot.image.panel.PanelMI
import com.now.nowbot.image.panel.PanelMS
import com.now.nowbot.image.panel.PanelTheta
import com.now.nowbot.image.panel.PanelZeta
import com.now.nowbot.image.scorelist.ScoreListMapParams
import com.now.nowbot.image.view.ViewSource
import com.now.nowbot.image.scorelist.ScoreListOutputFormat
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.userlist.UserListMapParams
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.BeatmapsetSearch
import com.now.nowbot.model.osu.Covers
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import com.now.nowbot.model.mappool.old.MapPoolDto
import com.now.nowbot.model.match.Match.MatchRound
import com.now.nowbot.model.match.MatchRating
import com.now.nowbot.model.match.SeriesRating
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.ppminus.PPMinus
import com.now.nowbot.service.messageServiceImpl.EliteronixDuelRatingService.ETXParam
import com.now.nowbot.service.messageServiceImpl.GroupLeaderBoardService.GroupLeaderBoardParam
import com.now.nowbot.image.maimai.MaiVersusRenderParam
import com.now.nowbot.image.maimai.readChuMultiple
import com.now.nowbot.image.maimai.readMaiDistribution
import com.now.nowbot.image.maimai.readMaiFilterRenderParam
import com.now.nowbot.image.maimai.readMaiFindParam
import com.now.nowbot.image.maimai.readMaiMultiple
import com.now.nowbot.image.maimai.readMaiScoreParam
import com.now.nowbot.image.maimai.readMaiSingleChu
import com.now.nowbot.image.maimai.readMaiSingleMai
import com.now.nowbot.image.maimai.readMaiVersusRenderParam
import com.now.nowbot.service.messageServiceImpl.ChuBestScoreService.PanelME2Param
import com.now.nowbot.service.messageServiceImpl.MaiBestScoreService.PanelMEParam
import com.now.nowbot.service.messageServiceImpl.MatchMapService.PanelE7Param
import com.now.nowbot.service.messageServiceImpl.OsuAvatarCardService.UserAvatarCardParam
import com.now.nowbot.service.messageServiceImpl.PopularService.PanelTData
import com.now.nowbot.service.messageServiceImpl.ViewService.ViewParam
import com.now.nowbot.service.messageServiceImpl.ScorePRService.PanelE5Param
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class PanelRenderDispatcher(
    private val contextFactory: PanelRenderContextFactory,
    private val markdownRenderer: MarkdownRenderer,
    private val policy: RenderOutputPolicy,
    private val htmlRenderer: HtmlRenderer,
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
) {
    fun render(name: String, payload: Any?): ByteArray {
        return when (val key = name.trim().uppercase(Locale.ROOT)) {
            "A1" -> {
                val body = payloadMap(payload, key)
                val param = UserListMapParams.a1(body)
                renderWith(policy.defaultFormat) { assets -> PanelA1.toSvg(param, assets) }
            }

            "A2" -> {
                val body = payloadMap(payload, key)
                val search = requireTyped<BeatmapsetSearch>(body, "search", key)
                val page = intOr(body, "page", key, 1)
                val maxPage = intOr(body, "max_page", key, 1)
                renderWith(policy.defaultFormat) { assets -> PanelA2.render(search, page, maxPage, assets) }
            }

            "A3" -> {
                val body = payloadMap(payload, key)
                val beatmap = requireTyped<Beatmap>(body, "beatmap", key)
                val scores = listTyped<LazerScore>(body, "scores", key)
                val start = intOr(body, "start", key, scores.firstOrNull()?.ranking ?: 1)
                val legacy = booleanOr(body, "is_legacy", key, false)
                val viewer = optionalTyped<IDUser>(body, "user", key)
                val type = optionalString(body, "type", key).orEmpty()
                renderWith(policy.defaultFormat) { assets ->
                    PanelA3.render(viewer, beatmap, scores, start, legacy, assets, type)
                }
            }

            "A4" -> {
                val param = ScoreListMapParams.a4(strictMap(payloadMap(payload, key)))
                val request = PanelA4.imageRequest(param)
                renderWith(formatOf(request.format)) { assets -> PanelA4.toSvg(request.data, assets) }
            }

            "A5" -> {
                val param = ScoreListMapParams.a5(strictMap(payloadMap(payload, key)))
                val request = PanelA5.imageRequest(param)
                renderWith(formatOf(request.format)) { assets -> PanelA5.toSvg(request.data, assets) }
            }

            "A6" -> {
                val body = payloadMap(payload, key)
                val user = optionalTyped<OsuUser>(body, "user", key)
                val markdown = optionalString(body, "markdown", key)
                val pageName = optionalString(body, "name", key).orEmpty()
                val width = optionalInt(body, "width", key) ?: 1840
                val height = optionalInt(body, "height", key) ?: 600
                renderContext { context ->
                    context.renderSvg(policy.advancedFormat) { _ ->
                        PanelA6.toSvg(
                            user = user,
                            markdown = markdown,
                            name = pageName,
                            width = width,
                            height = height,
                            lease = context.lease,
                            markdownRenderer = markdownRenderer,
                            remote = context.remoteImageSource,
                        )
                    }
                }
            }

            "A7" -> {
                val param = ScoreListMapParams.a7(strictMap(payloadMap(payload, key)))
                renderWith(policy.defaultFormat) { assets -> PanelA7.toSvg(param, assets) }
            }

            "A8" -> {
                val body = payloadMap(payload, key)
                val user = requireTyped<OsuUser>(body, "user", key)
                val rawScores = collection(body, "scores", key)
                val decrypted = collection(body, "decrypted", key)
                renderWith(policy.defaultFormat) { assets ->
                    PanelA8.render(user, a8Cards(rawScores, decrypted, assets), assets)
                }
            }

            "A9" -> {
                val body = payloadMap(payload, key)
                renderContext { context ->
                    context.renderSvg(policy.defaultFormat) { _ ->
                        PanelA9.toSvg(body, context.lease, markdownRenderer, context.remoteImageSource)
                    }
                }
            }

            "A10" -> {
                val param = UserListMapParams.a10(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelA10.toSvg(param, assets) }
            }

            "A11" -> {
                val param = UserListMapParams.a11(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelA11.toSvg(param, assets) }
            }

            "A12" -> {
                val param = ScoreListMapParams.a12(strictMap(payloadMap(payload, key)))
                renderWith(policy.defaultFormat) { assets -> PanelA12.toSvg(param, assets) }
            }

            "A13" -> {
                val body = payloadMap(payload, key)
                val user = requireTyped<OsuUser>(body, "user", key)
                val sets = listTyped<Beatmapset>(body, "beatmapsets", key)
                val page = intOr(body, "page", key, 1)
                val maxPage = intOr(body, "max_page", key, 1)
                renderWith(policy.defaultFormat) { assets -> PanelA13.render(user, sets, page, maxPage, assets) }
            }

            "A14" -> {
                val body = payloadMap(payload, key)
                val search = requireTyped<BeatmapsetSearch>(body, "search", key)
                val page = intOr(body, "page", key, 1)
                val maxPage = intOr(body, "max_page", key, 1)
                renderWith(policy.defaultFormat) { assets -> PanelA14.render(search, page, maxPage, assets) }
            }

            "A15" -> {
                val param = ScoreListMapParams.a15(strictMap(payloadMap(payload, key)))
                renderWith(policy.defaultFormat) { assets -> PanelA15.toSvg(param, assets) }
            }

            "A16" -> {
                val param = payloadTyped<GroupLeaderBoardParam>(payload, key)
                renderWith(policy.defaultFormat) { assets ->
                    PanelA16.render(
                        viewer = param.user,
                        beatmap = param.beatmap,
                        mode = param.mode.shortName,
                        scores = param.scores,
                        group = param.group,
                        type = param.type,
                        page = param.page,
                        maxPage = param.maxPage,
                        assets = assets,
                    )
                }
            }

            "C" -> {
                val rating = payloadTyped<MatchRating>(payload, key)
                renderWith(policy.advancedFormat) { assets -> PanelC.toSvg(rating, assets) }
            }

            "C2" -> {
                val series = payloadTyped<SeriesRating>(payload, key)
                renderWith(policy.advancedFormat) { assets -> PanelC2.toSvg(series, assets) }
            }

            "D" -> {
                val body = payloadMap(payload, key)
                val param = dParam(body)
                renderWith(policy.defaultFormat) { assets -> PanelD.render(param, assets) }
            }

            "D2" -> {
                val body = payloadMap(payload, key)
                val param = d2Param(body)
                renderWith(policy.defaultFormat) { assets -> PanelD2.render(param, assets) }
            }

            "D3" -> {
                val body = payloadMap(payload, key)
                val param = d3Param(body)
                renderWith(policy.defaultFormat) { assets -> PanelD3.render(param, assets) }
            }

            "E5" -> {
                val param = e5Param(payload, key)
                renderWith(policy.defaultFormat) { assets -> PanelE5.render(param, assets) }
            }

            "E7" -> {
                val param = payloadTyped<PanelE7Param>(payload, key)
                renderWith(policy.defaultFormat) { assets -> PanelE7.render(param, assets) }
            }

            "E10" -> {
                val param = e5Param(payload, key)
                renderWith(policy.defaultFormat) { assets -> PanelE10.render(param, assets) }
            }

            "F" -> {
                val rating: MatchRating
                val quickplay: Boolean
                if (payload is MatchRating) {
                    rating = payload
                    quickplay = false
                } else {
                    val body = payloadMap(payload, key)
                    rating = requireTyped(body, "match", key)
                    quickplay = optionalString(body, "panel", key) == "RP"
                }
                renderWith(policy.defaultFormat) { assets -> PanelF.render(rating, quickplay, assets) }
            }

            "F3" -> {
                val body = payloadMap(payload, key)
                val rating = requireTyped<MatchRating>(body, "match", key)
                val round = requireTyped<MatchRound>(body, "round", key)
                val roundResult = optionalString(body, "panel", key) == "RR"
                renderWith(policy.defaultFormat) { assets -> PanelF3.render(rating, round, roundResult, assets) }
            }

            "F4" -> {
                val body = payloadMap(payload, key)
                val round = requireTyped<MatchRound>(body, "round", key)
                renderWith(policy.defaultFormat) { assets -> PanelF4.render(round, assets) }
            }

            "H" -> {
                val body = payloadMap(payload, key)
                val pool = requireTyped<MapPoolDto>(body, "pool", key)
                val modeValue = body["mode"]
                    ?: throw IllegalArgumentException("H 缺少关键字段 mode（期望 OsuMode 或字符串）")
                val mode = when (modeValue) {
                    is OsuMode -> modeValue
                    is String -> modeValue.toOsuMode()
                    else -> throw IllegalArgumentException("H 的 mode 期望 OsuMode 或字符串，实际为 ${modeValue.javaClass.name}")
                }
                renderWith(policy.defaultFormat) { assets -> PanelH.render(pool, mode, assets) }
            }

            "J" -> {
                val body = payloadMap(payload, key)
                renderWith(policy.defaultFormat) { assets -> PanelJ.render(body, assets) }
            }

            "J2" -> {
                val body = payloadMap(payload, key)
                renderWith(policy.defaultFormat) { assets -> PanelJ2.render(body, assets) }
            }

            "K" -> {
                val data = PanelKData.fromMap(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelK.toSvg(data, assets) }
            }

            "M2" -> {
                val body = payloadMap(payload, key)
                renderWith(policy.defaultFormat) { assets -> PanelM2.render(body, assets) }
            }

            "MV" -> {
                val param = payloadTyped<com.now.nowbot.service.messageServiceImpl.MaiVersionScoreService.MaiVersionResponse>(payload, key)
                renderWith(policy.defaultFormat) { assets -> PanelMV.render(param, assets) }
            }

            "N" -> {
                val data = PanelNData.fromMap(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelN.toSvg(data, assets) }
            }

            "R" -> {
                val param = payloadTyped<com.now.nowbot.service.messageServiceImpl.MapStatisticsService.PanelRParam>(payload, key)
                renderWith(policy.defaultFormat) { assets -> PanelR.render(param, assets) }
            }

            "S" -> {
                val param = PanelSParam.fromMap(strictMap(payloadMap(payload, key)))
                renderWith(policy.defaultFormat) { assets -> PanelS.toSvg(param, assets) }
            }

            "B1" -> {
                val data = PanelB1Data.fromMap(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelB1.toSvg(data, assets) }
            }

            "B2" -> {
                val data = PanelB2Data.fromMap(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelB2.toSvg(data, assets) }
            }

            "B3" -> {
                val data = PanelB3Data.fromMap(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelB3.toSvg(data, assets) }
            }

            "B4" -> {
                val param = payloadTyped<ETXParam>(payload, key)
                renderWith(policy.defaultFormat) { assets -> PanelB4.toSvg(param, assets) }
            }

            "T" -> {
                val data = payloadTyped<PanelTData>(payload, key)
                renderWith(policy.defaultFormat) { assets -> PanelT.render(data, assets) }
            }

            "U" -> {
                val param = UserListMapParams.u(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelU.toSvg(param, assets) }
            }

            "V" -> {
                val param = payloadTyped<ViewParam>(payload, key)
                val text = beatmapApiService.getBeatmapFileString(param.beatmap.beatmapID)
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("V 无法获取 .osu 原文: beatmapID=${param.beatmap.beatmapID}")
                val source = ViewSource.fromText(text, param.mode)
                renderWith(policy.advancedFormat) { assets -> PanelV.toSvg(param, source, assets) }
            }

            "V2" -> {
                val param = payloadTyped<ViewParam>(payload, key)
                val text = beatmapApiService.getBeatmapFileString(param.beatmap.beatmapID)
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("V2 无法获取 .osu 原文: beatmapID=${param.beatmap.beatmapID}")
                val source = ViewSource.fromText(text, param.mode)
                renderWith(policy.advancedFormat) { assets -> PanelV2.toSvg(param, source, assets) }
            }

            "Alpha" -> {
                val lines = alphaLines(payload, key)
                renderAlpha(lines)
            }

            "Gamma" -> {
                when (payload) {
                    is LazerScore -> renderGamma(payload)
                    is IDUser -> renderGamma(payload)
                    is Map<*, *> -> {
                        val body = payloadMap(payload, key)
                        when (optionalString(body, "panel", key)) {
                            "score" -> renderGamma(requireTyped<LazerScore>(body, "score", key))
                            "info" -> renderGamma(requireTyped<IDUser>(body, "user", key))
                            else -> renderWith(policy.defaultFormat) { assets -> PanelGamma.toSvgSanity(body, assets) }
                        }
                    }

                    else -> throw IllegalArgumentException(
                        "Gamma 期望 Map/LazerScore/IDUser payload，实际为 ${payload?.javaClass?.name ?: "null"}",
                    )
                }
            }

            "Delta" -> {
                when (payload) {
                    is PanelDeltaParam -> renderWith(policy.defaultFormat) { assets -> PanelDelta.render(payload, assets) }
                    is Map<*, *> -> {
                        val body = payloadMap(payload, key)
                        val beatmap = requireTyped<Beatmap>(body, "beatmap", key)
                        val round = optionalString(body, "round", key) ?: "Qualifier"
                        val mod = optionalString(body, "mod", key) ?: "NM"
                        val position = (body["position"] as? Number)?.toShort() ?: 1
                        val hasBg = body["hasBG"] as? Boolean ?: true
                        renderDelta(beatmap, round, mod, position, hasBg)
                    }

                    else -> throw IllegalArgumentException(
                        "Delta 期望 PanelDeltaParam/Map payload，实际为 ${payload?.javaClass?.name ?: "null"}",
                    )
                }
            }

            "Epsilon" -> {
                val user = epsilonUser(payload, key)
                renderBytes { context ->
                    PanelEpsilon.render(user, context.lease, context.remoteImageSource, htmlRenderer)
                }
            }

            "Epsilon2" -> {
                val user = epsilonUser(payload, key)
                renderBytes { context ->
                    PanelEpsilon2.renderImage(
                        user, context.lease, context.remoteImageSource, htmlRenderer, policy.defaultFormat,
                    )
                }
            }

            "Zeta" -> {
                val user = zetaUser(payload, key)
                renderBytes { context ->
                    PanelZeta.render(
                        user, context.lease, context.remoteImageSource, htmlRenderer, policy.defaultFormat,
                    )
                }
            }

            "Theta" -> {
                val param = when (payload) {
                    is MaiVersusRenderParam -> payload
                    is Map<*, *> -> readMaiVersusRenderParam(payloadMap(payload, key))
                    else -> throw IllegalArgumentException(
                        "Theta 期望 Map/MaiVersusRenderParam payload，实际为 ${payload?.javaClass?.name ?: "null"}",
                    )
                }
                renderWith(policy.defaultFormat) { assets -> PanelTheta.render(param, assets) }
            }

            "MA" -> {
                val param = readMaiMultiple(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelMA.render(param, assets) }
            }

            "MA2" -> {
                val param = readChuMultiple(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelMA2.render(param, assets) }
            }

            "MD" -> {
                val param = readMaiDistribution(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelMD.render(param, assets) }
            }

            "ME" -> {
                when (payload) {
                    is PanelMEParam -> renderWith(policy.defaultFormat) { assets -> PanelME.render(payload, assets) }
                    is PanelME2Param -> renderWith(policy.defaultFormat) { assets -> PanelME.render(payload, assets) }
                    is Map<*, *> -> {
                        val body = payloadMap(payload, key)
                        if (body.containsKey("diff") || body.containsKey("chart")) {
                            val param = readMaiSingleMai(body)
                            renderWith(policy.defaultFormat) { assets -> PanelME.render(param, assets) }
                        } else {
                            val param = readMaiSingleChu(body)
                            renderWith(policy.defaultFormat) { assets -> PanelME.render(param, assets) }
                        }
                    }

                    else -> throw IllegalArgumentException(
                        "ME 期望 Map/PanelMEParam/PanelME2Param payload，实际为 ${payload?.javaClass?.name ?: "null"}",
                    )
                }
            }

            "MF" -> {
                val param = readMaiFindParam(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelMF.render(param, assets, null) }
            }

            "MI" -> {
                val param = readMaiFilterRenderParam(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelMI.render(param, assets) }
            }

            "MS" -> {
                val param = readMaiScoreParam(payloadMap(payload, key))
                renderWith(policy.defaultFormat) { assets -> PanelMS.render(param, assets) }
            }

            else -> throw UnsupportedOperationException("面板未接入: $name")
        }
    }

    fun renderAlpha(lines: List<String?>): ByteArray {
        return renderWith(policy.defaultFormat) { _ -> PanelAlpha.toSvg(lines) }
    }

    fun renderA6(user: OsuUser?, markdown: String, name: String, width: Int = 1840): ByteArray {
        return renderContext { context ->
            context.renderSvg(policy.advancedFormat) { _ ->
                PanelA6.toSvg(
                    user = user,
                    markdown = markdown,
                    name = name,
                    width = width,
                    height = 600,
                    lease = context.lease,
                    markdownRenderer = markdownRenderer,
                    remote = context.remoteImageSource,
                )
            }
        }
    }

    fun renderGamma(score: LazerScore): ByteArray {
        return renderWith(policy.defaultFormat) { assets -> PanelGamma.toSvg(score, assets) }
    }

    fun renderGamma(user: IDUser): ByteArray {
        val osuUser = user as? OsuUser ?: userApiService.getOsuUser(user.userID)
        return renderWith(policy.defaultFormat) { assets -> PanelGamma.toSvg(osuUser, assets) }
    }

    fun renderGamma(user: OsuUser?, mode: OsuMode, my: PPMinus?): ByteArray {
        val users = if (user == null) emptyList() else listOf(user)
        val mine = if (my == null) emptyMap() else mapOf(
            "ACC" to my.value1,
            "PTT" to my.value2,
            "STA" to my.value3,
            (if (mode == OsuMode.MANIA) "PRE" else "STB") to my.value4,
            "EFT" to my.value5,
            "STH" to my.value6,
            "OVA" to my.value7,
            "SAN" to my.value8,
        )
        val stat = mapOf("is_vs" to false, "mode_int" to mode.modeValue)
        val body: Map<String, Any?> = mapOf("users" to users, "my" to mine, "stat" to stat, "panel" to "sanity")
        return renderWith(policy.defaultFormat) { assets -> PanelGamma.toSvgSanity(body, assets) }
    }

    fun renderDelta(beatmap: Beatmap, round: String, mod: String, position: Short, hasBG: Boolean): ByteArray {
        val param = PanelDeltaParam(beatmap, round, mod, position, hasBG)
        return renderWith(policy.defaultFormat) { assets -> PanelDelta.render(param, assets) }
    }

    private fun renderBytes(block: (PanelRenderContext) -> ByteArray): ByteArray {
        return contextFactory.open().use { context -> context.renderBytes(block) }
    }

    private fun alphaLines(payload: Any?, name: String): List<String?> {
        val values = when (payload) {
            is List<*> -> payload
            is Array<*> -> payload.toList()
            is Map<*, *> -> {
                val body = payloadMap(payload, name)
                val strs = body["strs"] ?: throw IllegalArgumentException("$name 缺少关键字段 strs")
                strs as? Collection<*> ?: throw IllegalArgumentException("$name 的 strs 期望集合，实际为 ${strs.javaClass.name}")
            }

            else -> throw IllegalArgumentException("$name 期望 Map/List payload，实际为 ${payload?.javaClass?.name ?: "null"}")
        }
        return values.mapIndexed { index, value ->
            value as? String ?: throw IllegalArgumentException("$name 的 strs[$index] 期望字符串，实际为 ${value?.javaClass?.name ?: "null"}")
        }
    }

    private fun epsilonUser(payload: Any?, name: String): OsuUser {
        if (payload is OsuUser) return payload
        val body = payloadMap(payload, name)
        return requireTyped(body, "user", name)
    }

    private fun zetaUser(payload: Any?, name: String): OsuUser {
        return when (payload) {
            is OsuUser -> payload
            is UserAvatarCardParam -> OsuUser().apply {
                coverUrl = payload.banner.orEmpty()
                avatarUrl = payload.avatar.orEmpty()
                username = payload.name.orEmpty()
            }

            is Map<*, *> -> {
                val body = payloadMap(payload, name)
                OsuUser().apply {
                    coverUrl = optionalString(body, "banner", name).orEmpty()
                    avatarUrl = optionalString(body, "avatar", name).orEmpty()
                    username = optionalString(body, "name", name).orEmpty()
                }
            }

            else -> throw IllegalArgumentException(
                "$name 期望 UserAvatarCardParam/OsuUser/Map payload，实际为 ${payload?.javaClass?.name ?: "null"}",
            )
        }
    }

    private fun renderWith(format: ScreenshotType, block: (ImageAssets) -> String): ByteArray {
        return contextFactory.open().use { context ->
            context.renderSvg(format) { assets -> block(assets) }
        }
    }

    private fun <T> renderContext(block: (PanelRenderContext) -> T): T {
        return contextFactory.open().use(block)
    }

    private fun formatOf(format: ScoreListOutputFormat): ScreenshotType {
        return if (format == ScoreListOutputFormat.ADVANCED) policy.advancedFormat else policy.defaultFormat
    }

    private fun a8Cards(rawScores: List<Any?>, decrypted: List<Any?>, assets: ImageAssets): List<CardC3Data> {
        return rawScores.mapIndexed { index, raw ->
            val decryptedValue = (decrypted.getOrNull(index) as? Number)?.toInt()
            when (raw) {
                is LazerScore -> {
                    val revealed = decryptedValue?.let { it >= 0 } ?: false
                    scoreA8Card(raw, index + 1, revealed, assets)
                }

                is Map<*, *> -> {
                    val revealed = decryptedValue?.let { it >= 0 } ?: true
                    mapA8Card(raw, index + 1, revealed, assets)
                }

                else -> throw IllegalArgumentException(
                    "A8 scores[$index] 期望 LazerScore 或加密 Map，实际为 ${raw?.javaClass?.name ?: "null"}",
                )
            }
        }
    }

    private fun scoreA8Card(score: LazerScore, index: Int, revealed: Boolean, assets: ImageAssets): CardC3Data {
        val star = score.beatmap.starRating
        val color = Colors.getStarRatingColor(star)
        val textColor = if (star < 4) "#1c1719" else "#fff"
        return CardC3Data(
            background = assets.mapBackground(score, "list@2x"),
            cover = if (revealed) null else assets.mapBackground(score, "cover"),
            title = score.beatmapset.title,
            title2 = score.beatmapset.titleUnicode,
            left1 = "${score.beatmapset.artist} // ${score.beatmapset.creator}",
            left2 = "[${score.beatmap.difficultyName}]",
            label1 = "#$index",
            label2 = if (revealed) null else score.beatmap.beatmapID.toString(),
            colorRrect = color,
            colorTitleText = "#fff",
            colorTitle2Text = "#bbb",
            colorLabelRrect1 = color,
            colorLabelRrect2 = if (revealed) "" else color,
            colorLabelText = textColor,
            colorLeft1Text = "#fff",
            colorLeft2Text = "#bbb",
            blur = if (revealed) 30.0 else 5.0,
        )
    }

    private fun mapA8Card(raw: Map<*, *>, index: Int, revealed: Boolean, assets: ImageAssets): CardC3Data {
        val set = raw["beatmapset"] as? Map<*, *>
            ?: throw IllegalArgumentException("A8 scores[$index] 缺少 beatmapset")
        val beatmap = raw["beatmap"] as? Map<*, *>
            ?: throw IllegalArgumentException("A8 scores[$index] 缺少 beatmap")
        val covers = set["covers"] as? Covers
        val star = (beatmap["difficulty_rating"] as? Number)?.toDouble() ?: 0.0
        val color = Colors.getStarRatingColor(star)
        val textColor = if (star < 4) "#1c1719" else "#fff"
        return CardC3Data(
            background = covers?.list2x?.takeIf { it.isNotBlank() }?.let { assets.remote(it) },
            cover = if (revealed) null else covers?.cover?.takeIf { it.isNotBlank() }?.let { assets.remote(it) },
            title = set["title"]?.toString().orEmpty(),
            title2 = set["title_unicode"]?.toString().orEmpty(),
            left1 = "${set["artist"]?.toString().orEmpty()} // ${set["creator"]?.toString().orEmpty()}",
            left2 = "[${beatmap["version"]?.toString().orEmpty()}]",
            label1 = "#$index",
            label2 = if (revealed) null else beatmap["id"]?.toString(),
            colorRrect = color,
            colorTitleText = "#fff",
            colorTitle2Text = "#bbb",
            colorLabelRrect1 = color,
            colorLabelRrect2 = if (revealed) "" else color,
            colorLabelText = textColor,
            colorLeft1Text = "#fff",
            colorLeft2Text = "#bbb",
            blur = if (revealed) 30.0 else 5.0,
        )
    }

    private fun e5Param(payload: Any?, name: String): PanelE5Param {
        if (payload is PanelE5Param) return payload
        val map = payloadMap(payload, name)
        val user = requireTyped<OsuUser>(map, "user", name)
        val score = requireTyped<LazerScore>(map, "score", name)
        val history = optionalTyped<OsuUser>(map, "history_user", name)
        val density = intArray(map, "density", name) ?: IntArray(0)
        val progress = (map["progress"] as? Number)?.toDouble()
            ?: throw IllegalArgumentException("$name 的 progress 期望数字，实际为 ${map["progress"]?.javaClass?.name ?: "null"}")
        val original = when (val value = map["original"]) {
            is Map<*, *> -> value.entries.mapNotNull { (k, v) -> v?.let { k.toString() to it } }.toMap()
            else -> throw IllegalArgumentException("$name 的 original 期望 Map，实际为 ${value?.javaClass?.name ?: "null"}")
        }
        val panel = optionalString(map, "panel", name).orEmpty()
        val position = (map["position"] as? Number)?.toInt()
        val health = (map["health"] as? Map<*, *>)?.let { value ->
            val times = (value["time"] as? Collection<*>).orEmpty().mapNotNull { (it as? Number)?.toInt() }
            val percents = (value["percent"] as? Collection<*>).orEmpty().mapNotNull { (it as? Number)?.toDouble() }
            times.zip(percents).toMap()
        }
        return PanelE5Param(
            user = user,
            history = history,
            score = score,
            position = position,
            density = density,
            progress = progress,
            original = original,
            attributes = map["attributes"],
            panel = panel,
            health = health,
        )
    }

    private fun dParam(map: Map<String, Any?>): PanelDParam {
        val user = requireTyped<OsuUser>(map, "user", "D")
        val history = optionalTyped<OsuUser>(map, "historyUser", "D")
        return PanelDParam(
            user = user,
            historyUser = history,
            mode = modeString(map["mode"], "D"),
            bpTimes = intArray(map, "bp-times", "D") ?: IntArray(0),
            bonusPp = (map["bonus_pp"] as? Number)?.toDouble() ?: 0.0,
            day = (map["day"] as? Number)?.toLong() ?: 1L,
        )
    }

    private fun d2Param(map: Map<String, Any?>): PanelD2Param {
        val user = requireTyped<OsuUser>(map, "user", "D2")
        return PanelD2Param(
            user = user,
            historyUser = optionalTyped<OsuUser>(map, "history_user", "D2"),
            scores = listTyped<LazerScore>(map, "scores", "D2"),
            bestTime = intArray(map, "best_time", "D2") ?: IntArray(0),
            mode = modeString(map["mode"], "D2"),
            historyDay = (map["history_day"] as? Number)?.toLong() ?: 1L,
        )
    }

    private fun d3Param(map: Map<String, Any?>): PanelD3Param {
        val user = requireTyped<OsuUser>(map, "user", "D3")
        val percentiles = when (val value = map["percentiles"]) {
            null -> emptyMap()
            is Map<*, *> -> value.entries.mapNotNull { (k, v) ->
                val number = (v as? Number)?.toDouble() ?: return@mapNotNull null
                k.toString() to number
            }.toMap()
            else -> throw IllegalArgumentException("D3 的 percentiles 期望 Map，实际为 ${value.javaClass.name}")
        }
        return PanelD3Param(
            user = user,
            bests = listTyped<LazerScore>(map, "bests", "D3"),
            historyUser = optionalTyped<OsuUser>(map, "history_user", "D3"),
            percentiles = percentiles,
            bonusPp = (map["bonus_pp"] as? Number)?.toDouble() ?: 0.0,
            historyDay = (map["history_day"] as? Number)?.toLong() ?: 1L,
        )
    }

    private fun modeString(value: Any?, name: String): String? {
        return when (value) {
            null -> null
            is OsuMode -> value.shortName
            is String -> value
            else -> throw IllegalArgumentException("$name 的 mode 期望 OsuMode 或字符串，实际为 ${value.javaClass.name}")
        }
    }

    private fun intArray(map: Map<String, Any?>, key: String, name: String): IntArray? {
        return when (val value = map[key]) {
            null -> null
            is IntArray -> value
            is Collection<*> -> value.mapNotNull { (it as? Number)?.toInt() }.toIntArray()
            else -> throw IllegalArgumentException("$name 的 $key 期望 IntArray，实际为 ${value.javaClass.name}")
        }
    }

    private fun payloadMap(payload: Any?, name: String): Map<String, Any?> {
        if (payload == null) {
            throw IllegalArgumentException("$name 期望 Map payload，实际为 null")
        }
        if (payload !is Map<*, *>) {
            throw IllegalArgumentException("$name 期望 Map payload，实际为 ${payload.javaClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return payload as Map<String, Any?>
    }

    private fun strictMap(map: Map<String, Any?>): Map<String, Any> {
        return map.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
    }

    private inline fun <reified T> payloadTyped(payload: Any?, name: String): T {
        if (payload == null) {
            throw IllegalArgumentException("$name 期望 ${T::class.java.simpleName} payload，实际为 null")
        }
        return payload as? T
            ?: throw IllegalArgumentException("$name 期望 ${T::class.java.simpleName} payload，实际为 ${payload.javaClass.name}")
    }

    private inline fun <reified T> requireTyped(map: Map<String, Any?>, key: String, name: String): T {
        val value = map[key] ?: throw IllegalArgumentException("$name 缺少关键字段 $key（期望 ${T::class.java.simpleName}）")
        return value as? T
            ?: throw IllegalArgumentException("$name 的 $key 期望 ${T::class.java.simpleName}，实际为 ${value.javaClass.name}")
    }

    private inline fun <reified T> optionalTyped(map: Map<String, Any?>, key: String, name: String): T? {
        val value = map[key] ?: return null
        return value as? T
            ?: throw IllegalArgumentException("$name 的 $key 期望 ${T::class.java.simpleName}，实际为 ${value.javaClass.name}")
    }

    private inline fun <reified T> listTyped(map: Map<String, Any?>, key: String, name: String): List<T> {
        val value = map[key] ?: throw IllegalArgumentException("$name 缺少关键字段 $key（期望 ${T::class.java.simpleName} 列表）")
        val collection = value as? Collection<*>
            ?: throw IllegalArgumentException("$name 的 $key 期望 ${T::class.java.simpleName} 列表，实际为 ${value.javaClass.name}")
        return collection.mapIndexed { index, item ->
            item as? T ?: throw IllegalArgumentException(
                "$name 的 $key[$index] 期望 ${T::class.java.simpleName}，实际为 ${item?.javaClass?.name ?: "null"}",
            )
        }
    }

    private fun collection(map: Map<String, Any?>, key: String, name: String): List<Any?> {
        val value = map[key] ?: throw IllegalArgumentException("$name 缺少关键字段 $key")
        val collection = value as? Collection<*>
            ?: throw IllegalArgumentException("$name 的 $key 期望集合，实际为 ${value.javaClass.name}")
        return collection.toList()
    }

    private fun intOr(map: Map<String, Any?>, key: String, name: String, default: Int): Int {
        val value = map[key] ?: return default
        return (value as? Number)?.toInt()
            ?: throw IllegalArgumentException("$name 的 $key 期望数字，实际为 ${value.javaClass.name}")
    }

    private fun optionalInt(map: Map<String, Any?>, key: String, name: String): Int? {
        val value = map[key] ?: return null
        return (value as? Number)?.toInt()
            ?: throw IllegalArgumentException("$name 的 $key 期望数字，实际为 ${value.javaClass.name}")
    }

    private fun booleanOr(map: Map<String, Any?>, key: String, name: String, default: Boolean): Boolean {
        val value = map[key] ?: return default
        return value as? Boolean
            ?: throw IllegalArgumentException("$name 的 $key 期望布尔值，实际为 ${value.javaClass.name}")
    }

    private fun optionalString(map: Map<String, Any?>, key: String, name: String): String? {
        val value = map[key] ?: return null
        if (value !is String) {
            throw IllegalArgumentException("$name 的 $key 期望字符串，实际为 ${value.javaClass.name}")
        }
        return value
    }
}

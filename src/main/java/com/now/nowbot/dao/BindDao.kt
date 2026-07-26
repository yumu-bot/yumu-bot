package com.now.nowbot.dao

import com.now.nowbot.entity.*
import com.now.nowbot.entity.OsuBindUserLite.Companion.toEntity
import com.now.nowbot.entity.OsuBindUserLite.Companion.toModel
import com.now.nowbot.entity.SBBindUserLite.Companion.toEntity
import com.now.nowbot.entity.SBBindUserLite.Companion.toModel
import com.now.nowbot.entity.bind.DiscordBindLite
import com.now.nowbot.entity.bind.QQBindLite
import com.now.nowbot.entity.bind.SBQQBindLite
import com.now.nowbot.mapper.*
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.SBBindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.ppysb.SBUser
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.BindException.NotBindException.UserNotBind
import com.now.nowbot.throwable.botRuntimeException.BindException.NotBindException.YouNotBind
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.ObjectUtils
import org.springframework.web.client.HttpClientErrorException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.optionals.getOrNull

@Component
class BindDao(
    private val bindUserMapper: BindUserMapper,
    private val sbBindUserMapper: SBBindUserMapper,
    private val osuFindNameMapper: OsuFindNameMapper,
    private val sbFindNameMapper: SBFindNameMapper,
    private val bindQQMapper: BindQQMapper,
    private val sbQQBindMapper: SBQQBindMapper,
    private val bindDiscordMapper: BindDiscordMapper,
) {
    private val updateUserSet = ConcurrentHashMap.newKeySet<Long>()
    private val nowUpdate = AtomicBoolean(false)

    var log: Logger = LoggerFactory.getLogger(BindDao::class.java)

    /**
     * 查询绑定关系（找不到时返回 null，不抛异常）
     */
    fun getBindFromQQOrNull(qq: Long): BindUser? {
        if (qq < 0) {
            return getBindUserFromOsuIDOrNull(-qq)
                ?: BindUser(-qq, "unknown")
        }

        return bindQQMapper.findById(qq).getOrNull()?.osuUser?.toModel()
    }

    /**
     * 查询绑定关系（找不到时抛出 BindException）
     */
    fun getBindFromQQ(qq: Long, isMyself: Boolean = false): BindUser {
        return getBindFromQQOrNull(qq).throwIfNull(isMyself)
    }

    fun getBindFromIDs(ids: Iterable<Long>): List<BindUser> {
        val lites = bindQQMapper.findAllByUserID(ids)

        return lites.mapNotNull { it.osuUser?.toModel() }
    }

    fun getBindFromQQs(qqs: Iterable<Long>): List<BindUser> {
        val userIDs = bindQQMapper.findAllUserByQQ(qqs).map {it.uid}
        val lites = bindQQMapper.findAllByUserID(userIDs)

        return lites.mapNotNull { it.osuUser?.toModel() }
    }

    fun saveBind(user: BindUser): BindUser {
        val lite = bindUserMapper.save(user.toEntity())
        return lite.toModel()
    }

    fun updateBind(user: BindUser): Boolean {
        bindUserMapper.update(user.toEntity())

        return true
    }

    /**
     * 根据 Osu ID 查询绑定用户（找不到或参数非法时返回 null）
     */
    fun getBindUserFromOsuIDOrNull(userID: Long): BindUser? {

        val liteData = try {
            bindUserMapper.getByOsuID(userID)
        } catch (_: IncorrectResultSizeDataAccessException) {
            bindUserMapper.deleteOutdatedByOsuID(userID)
            bindUserMapper.getByOsuID(userID)
        }

        return liteData?.toModel()
    }

    /**
     * 根据 Osu ID 查询绑定用户（找不到或参数非法时抛出对应异常）
     */
    fun getBindUserFromOsuID(userID: Long): BindUser {
        return getBindUserFromOsuIDOrNull(userID) ?: throw UserNotBind()
    }

    fun getAllBindUser(userIDs: Collection<Long>): List<OsuBindUserLite> {
        return bindUserMapper.getAllByOsuID(userIDs)
    }

    fun getQQLiteFromUserID(userID: Long): QQBindLite? {
        return bindQQMapper.findByUserID(userID)
    }

    fun getQQLiteFromQQ(qq: Long): QQBindLite? {
        return bindQQMapper.findById(qq).getOrNull()
    }

    fun bindQQ(qq: Long?, user: OsuBindUserLite): QQBindLite {
        val bindUserLite: OsuBindUserLite

        if (user.refreshToken != null) {
            val count = bindQQMapper.countByUserID(user.userID)
            if (count > 1) {
                bindUserMapper.deleteAllByUserID(user.userID)
            }

            // checkSave
            if (user.id == null && bindUserMapper.countAllByUserID(user.userID) > 0) {
                bindUserMapper.deleteOutdatedByOsuID(user.userID)
            }

            bindUserMapper.update(user)
            bindUserLite = user
        } else {
            val userLite = bindUserMapper.getFirstByOsuID(user.userID)

            // checkSave
            if (userLite == null) {
                if (user.id == null && bindUserMapper.countAllByUserID(user.userID) > 0) {
                    bindUserMapper.deleteOutdatedByOsuID(user.userID)
                }

                bindUserLite = bindUserMapper.save(user)
            } else {
                bindUserLite = userLite
            }
        }

        val qqBind = QQBindLite()

        qqBind.qq = qq
        qqBind.osuUser = bindUserLite
        return bindQQMapper.save(qqBind)
    }

    fun bindDiscord(discordID: String, user: BindUser): DiscordBindLite {
        return bindDiscord(discordID, user.toEntity())
    }

    fun bindDiscord(discordID: String, user: OsuBindUserLite?): DiscordBindLite {
        val discordBind = DiscordBindLite().apply {
            this.id = discordID
            this.osuUser = user
        }

        return bindDiscordMapper.save(discordBind)
    }

    /**
     * 会先找 refreshToken 相同的那位
     */
    fun getBindUserFromConstructor(user: BindUser): BindUser? {
        val fromToken = if (user.refreshToken != null) {
            val lite = bindUserMapper.getByRefreshToken(user.refreshToken)

            lite?.toModel()
        } else null

        return fromToken ?: getBindUserOrNull(user.userID)
    }

    fun getBindUserOrNull(name: String): BindUser? {
        val userID = getOsuID(name) ?: return null

        val lite = bindUserMapper.getByOsuID(userID)
        return lite?.toModel()
    }

    fun getBindUserOrNull(userID: Long?): BindUser? {
        if (userID == null) return null

        val lite = bindUserMapper.getByOsuID(userID)
        return lite?.toModel()
    }

    fun getBindModeFromID(userID: Long): OsuMode? {
        return getBindUserOrNull(userID)?.mode
    }

    fun getSBQQLiteFromUserID(userID: Long): SBQQBindLite? {
        return sbQQBindMapper.findByUserID(userID)
    }

    fun getSBQQLiteFromQQ(qq: Long): SBQQBindLite? {
        return sbQQBindMapper.findById(qq).getOrNull()
    }

    fun getSBBindUserOrNull(name: String): SBBindUser? {
        val userID = getSBUserID(name) ?: return null

        return getSBBindUser(userID)
    }

    /**
     * 根据 User ID 查询 SB 绑定用户（找不到或参数非法时返回 null，不抛异常）
     */
    fun getSBBindUserOrNull(userID: Long): SBBindUser? {
        val liteData = try {
            sbBindUserMapper.getUser(userID)
        } catch (_: IncorrectResultSizeDataAccessException) {
            sbBindUserMapper.deleteOutdatedBind(userID)
            sbBindUserMapper.getUser(userID)
        }

        return liteData?.toModel()
    }

    /**
     * 根据 User ID 查询 SB 绑定用户（找不到时抛出异常）
     */
    fun getSBBindUser(userID: Long, isMyself: Boolean = true): SBBindUser {
        return getSBBindUserOrNull(userID).throwIfNull(isMyself)
    }

    /**
     * 根据 QQ 查询 SB 绑定用户（找不到时返回 null 或 unknown 兜底对象，不抛异常）
     */
    fun getSBBindFromQQOrNull(qq: Long): SBBindUser? {
        if (qq < 0) {
            return getSBBindUserOrNull(-qq)
                ?: SBBindUser(-qq, "unknown")
        }

        return sbQQBindMapper.findById(qq).getOrNull()?.bindUser
    }

    /**
     * 根据 QQ 查询 SB 绑定用户（找不到时抛出对应异常）
     */
    fun getSBBindFromQQ(qq: Long, isMyself: Boolean): SBBindUser {
        return getSBBindFromQQOrNull(qq).throwIfNull(isMyself)
    }

    fun saveBind(user: SBBindUser): SBBindUser? {
        val lite = sbBindUserMapper.save(user.toEntity())
        return lite.toModel()
    }

    fun bindSBQQ(qq: Long, user: SBBindUser): SBQQBindLite {
        val data = sbBindUserMapper.getUser(user.userID)
        if (data == null) {
            return bindSBQQ(qq, user.toEntity())
        } else {
            data.userID = user.userID
            data.username = user.username
            data.time = user.time

            return bindSBQQ(qq, data)
        }
    }

    fun bindSBQQ(qq: Long, sbBind: SBBindUserLite): SBQQBindLite {
        val sbLite = sbBindUserMapper.getFirstByUserID(sbBind.userID)

        val bind: SBBindUserLite

        if (sbLite == null) {
            // 就是 checkSave
            if (sbBind.id == null && sbBindUserMapper.countAllByUserID(sbBind.userID) > 0) {
                sbBindUserMapper.deleteOutdatedBind(sbBind.userID)
            }

            bind = sbBindUserMapper.save(sbBind)
        } else {
            bind = sbBind
        }

        val qqBind = SBQQBindLite(qq, bind)

        return sbQQBindMapper.save(qqBind)
    }

    fun updateSBMode(userID: Long, mode: OsuMode) {
        sbBindUserMapper.updateMode(userID, mode.modeValue)
    }

    fun unBindSBQQ(user: SBBindUser): Boolean {
        try {
            sbQQBindMapper.unBind(user.userID)
            return true
        } catch (e: Exception) {
            log.error("e: ", e)
            return false
        }
    }

    fun bindQQ(qq: Long?, user: BindUser): QQBindLite {
        val bindUserLite = bindUserMapper.getByOsuID(user.userID)

        if (bindUserLite == null) {
            return bindQQ(qq, user.toEntity())
        } else {
            bindUserLite.accessToken = user.accessToken
            bindUserLite.refreshToken = user.refreshToken
            bindUserLite.time = user.time
            bindUserLite.username = user.username
            return bindQQ(qq, bindUserLite)
        }
    }

    fun updateToken(user: BindUser) {
        if (nowUpdate.get()) {
            updateUserSet.add(user.userID)
        }
        bindUserMapper.updateToken(user.userID, user.accessToken, user.refreshToken, user.time)
    }

    fun updateMode(uid: Long?, mode: OsuMode) {
        bindUserMapper.updateMode(uid, mode.modeValue)
    }

    fun unBindQQ(user: BindUser): Boolean {
        try {
            bindQQMapper.unBind(user.userID)
            return true
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * 高危权限
     *
     * @param user 绑定
     * @return qq
     */
    fun getQQ(user: BindUser): Long {
        return getQQ(user.userID)
    }

    fun getQQ(osuID: Long): Long {
        val qqBind = bindQQMapper.findByUserID(osuID)

        return if (qqBind?.qq != null) {
            qqBind.qq!!
        } else {
            -1L
        }
    }

    fun getQQBindInfo(user: BindUser): QQBindLite? {
        return getQQBindInfo(user.userID)
    }

    fun getQQBindInfo(userID: Long): QQBindLite? {
        return bindQQMapper.findByUserID(userID)
    }

    fun removeBind(uid: Long) {
        bindUserMapper.deleteAllByUserID(uid)
    }

    fun downgradeBind(uid: Long) {
        bindUserMapper.downgradeBind(uid)
    }

    fun getOsuID(name: String): Long? {
        return osuFindNameMapper.getUserIDByUsernameIgnoreCase(name)
    }

    fun removeNameToID(userID: Long) {
        osuFindNameMapper.deleteByUserID(userID)
    }
    
    @Transactional
    fun updateNameToID(id: Long, names: List<String>) {
        if (names.isEmpty()) return

        val exists = osuFindNameMapper.getNamesByUserID(id)

        val isSame = exists.size == names.size &&
                exists.map { it.lowercase() }.toSet() == names.map { it.lowercase() }.toSet()

        if (isSame) {
            return
        }

        osuFindNameMapper.deleteByUserID(id)

        val entities = names.mapIndexed { index, name ->
            OsuNameToIDLite(id, name, index)
        }

        osuFindNameMapper.saveAll(entities)
    }

    /**
     * 通过 osuFindNameMapper 获取
     */
    fun getUsername(userID: Long): String {
        return osuFindNameMapper.getUsername(userID) ?: userID.toString()
    }

//    fun countNameToID(userID: Long): Int {
//        return osuFindNameMapper.countByUserID(userID)
//    }

    fun updateNameToIDAsync(user: OsuUser) {
        Thread.startVirtualThread {
            updateNameToID(user)
        }
    }

    fun updateNameToID(user: OsuUser) {
        val names = buildList {
            add(user.username)
            user.previousNames?.let { addAll(it) }
        }

        updateNameToID(user.userID, names)
    }

    fun getSBUserName(userID: Long): String {
        return sbFindNameMapper.getUsername(userID) ?: userID.toString()
    }

    fun getSBUserID(name: String): Long? {
        return sbFindNameMapper.getUserIDByUsernameIgnoreCase(name)
    }

    fun removeSBNameToID(userID: Long) {
        sbFindNameMapper.deleteByUserID(userID)
    }

    @Transactional
    fun updateSBNameToID(id: Long, names: List<String>) {
        if (names.isEmpty()) return

        val exists = osuFindNameMapper.getNamesByUserID(id)

        val isSame = exists.size == names.size &&
                exists.map { it.lowercase() }.toSet() == names.map { it.lowercase() }.toSet()

        if (isSame) {
            return
        }

        osuFindNameMapper.deleteByUserID(id)

        val entities = names.mapIndexed { index, name ->
            OsuNameToIDLite(id, name, index)
        }

        osuFindNameMapper.saveAll(entities)
    }


//    fun countSBNameToID(userID: Long): Int {
//        return sbFindNameMapper.countByUserID(userID)
//    }

    fun updateSBNameToID(id: Long, name: String) {
        updateSBNameToID(SBUser(userID = id, username = name))
    }

    fun updateSBNameToID(user: SBUser) {
        val names = listOf(user.username)

        updateSBNameToID(user.userID, names)
    }

    fun getBindUserByDatabaseID(id: Long?): BindUser? {
        if (id == null) return null
        val data = bindUserMapper.findById(id)
        return data.getOrNull()?.toModel()
    }

    @Async fun refreshOldUserToken(userApiService: OsuUserApiService) {
        nowUpdate.set(true)
        updateUserSet.clear()
        try {
            refreshOldUserTokenOnce(userApiService)
        } catch (e: RuntimeException) {
            if (e !is HttpClientErrorException.Unauthorized) {
                log.error("更新用户出现异常", e)
            }
        } finally {
            updateUserSet.clear()
            nowUpdate.set(false)
        }
    }

    private fun refreshOldUserTokenOnce(userApiService: OsuUserApiService) {
        val now = System.currentTimeMillis()
        var user = bindUserMapper.getEarliestBindUser(now)
        if (user != null) {
            val u = user
            if (updateUserSet.remove(u.id)) return

            if (u.refreshToken.isNullOrBlank()) {
                bindUserMapper.downgradeBind(u.userID)
                return
            }

            log.debug("更新用户: {}", u.username)
            refreshOldUserToken(u, userApiService)
            return
        }

        user = bindUserMapper.getEarliestSuspiciousBindUser(now)
        if (user != null) {
            val u = user
            if (updateUserSet.remove(u.id)) return

            if (u.refreshToken.isNullOrBlank()) {
                bindUserMapper.downgradeBind(u.userID)
                return
            }
            // 出错超 5 次默认无法再次更新了
            if (u.updateCount > 5) {
                bindUserMapper.downgradeBind(u.id)
            }

            // log.info("更新用户: {}", u.getOsuName());
            refreshOldUserToken(u, userApiService)
        }
    }

    private fun refreshOldUserTokenPack(osuGetService: OsuUserApiService) {
        val now = System.currentTimeMillis()
        var succeedCount = 0
        var users: MutableList<OsuBindUserLite>

        // 更新暂时没失败过的
        while ((bindUserMapper.getOldBindUser(now).also { users = it.toMutableList() }).isNotEmpty()) {
            try {
                succeedCount += refreshOldUserList(osuGetService, users)
            } catch (e: RefreshException) {
                succeedCount += e.successCount
                log.error(
                    "连续失败, 停止更新, 更新用户数量: {}, 累计用时: {}s",
                    succeedCount,
                    (System.currentTimeMillis() - now) / 1000
                )
                return
            }
        }
        // 重新尝试失败的
        while ((bindUserMapper.getOldBindUserHasWrong(now).also { users = it.toMutableList() }).isNotEmpty()) {
            try {
                succeedCount += refreshOldUserList(osuGetService, users)
            } catch (e: RefreshException) {
                succeedCount += e.successCount
                log.error(
                    "停止更新, 更新用户数量: {}, 累计用时: {}s",
                    succeedCount,
                    (System.currentTimeMillis() - now) / 1000
                )
                return
            }
        }
        log.info("更新用户数量: {}, 累计用时: {}s", succeedCount, (System.currentTimeMillis() - now) / 1000)
    }

    private fun refreshOldUserList(osuGetService: OsuUserApiService, users: MutableList<OsuBindUserLite>): Int {
        var errCount = 0
        var succeedCount = 0
        while (users.isNotEmpty()) {
            val u = users.removeLast()

            if (updateUserSet.remove(u.id)) continue
            if (ObjectUtils.isEmpty(u.refreshToken)) {
                bindUserMapper.downgradeBind(u.userID)
                continue
            }
            // 出错超 5 次默认无法再次更新了
            if (u.updateCount > 5) {
                // 回退到用户名绑定
                bindUserMapper.downgradeBind(u.id)
            }
            // log.info("更新用户 {}", u.getOsuName());
            try {
                refreshOldUserToken(u, osuGetService)
                if (u.updateCount > 0) bindUserMapper.clearUpdateCount(u.id)
                errCount = 0
            } catch (_: HttpClientErrorException.Unauthorized) {
                // 绑定被取消或者过期, 不再尝试
                log.info("绑定 {} 失败：取消绑定", u.username)
                bindUserMapper.downgradeBind(u.id)
            } catch (e: Exception) {
                bindUserMapper.addUpdateCount(u.id)
                log.error("绑定 {} 第 {} 次失败：出现异常: ", u.username, errCount, e)
                errCount++
            }
            if (errCount > 5) {
                // 一般连续错误意味着网络寄了
                throw RefreshException(succeedCount)
            }
            succeedCount++
        }
        return succeedCount
    }

    private fun refreshOldUserToken(u: OsuBindUserLite, userApiService: OsuUserApiService): Boolean {
        var badRequest = 0

        while (true) {
            try {
                userApiService.getUserTokenOrBotToken(u.toModel())
                return true
            } catch(ue: NetworkException.UserException) {
                when(ue) {

                    is NetworkException.UserException.Unauthorized -> {
                        return false
                    }

                    is NetworkException.UserException.Forbidden -> {
                        log.info("刷新用户令牌：更新 {} 令牌失败, 可能被识别为滥用 API 而禁止访问", u.username)
                        return false
                    }

                    else -> {
                        badRequest++

                        if (badRequest < 3) {
                            log.error("刷新用户令牌：更新 {} 令牌失败, 第 {} 次重试", u.username, badRequest)
                        } else {
                            log.error(
                                "刷新用户令牌：更新 {} 令牌失败, 第 {} 次重试失败, 放弃更新。错误原因：",
                                u.username,
                                badRequest,
                                ue
                            )
                            return false
                        }
                    }
                }
            } catch (e: Throwable) {
                log.error("刷新用户令牌：神秘错误: ", e)
                return false
            }
        }
    }

    fun getBindUsersLimit50(offset: Int): List<BindUser> {
        return bindUserMapper.getAllBindUserLimit50(offset).map { it.toModel() }
    }

    fun getBindUserCount(): Long {
        return bindUserMapper.count()
    }

    fun getAllQQBindUser(qqs: Collection<Long>): List<QQBindLite.QQUser> {
        return bindQQMapper.findAllUserByQQ(qqs)
    }

    private class RefreshException(var successCount: Int) : RuntimeException()
    companion object {

        fun <T: Any> T?.throwIfNull(isMyself: Boolean = true): T {
            return this ?: if (isMyself) {
                throw YouNotBind()
            } else {
                throw UserNotBind()
            }
        }

        fun fromModel(user: BindUser?): OsuBindUserLite? {
            if (user == null) return null
            return OsuBindUserLite(user)
        }
    }
}

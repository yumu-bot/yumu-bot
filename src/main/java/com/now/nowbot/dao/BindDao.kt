package com.now.nowbot.dao

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.now.nowbot.entity.*
import com.now.nowbot.entity.bind.DiscordBindLite
import com.now.nowbot.entity.bind.QQBindLite
import com.now.nowbot.entity.bind.SBQQBindLite
import com.now.nowbot.mapper.*
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.SBBindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.isDefaultOrNull
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.ppysb.SBUser
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.osuApiService.impl.OsuApiBaseService.Companion.setPriority
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.BindException.BindIllegalArgumentException.IllegalQQ
import com.now.nowbot.throwable.botRuntimeException.BindException.NotBindException.UserNotBind
import com.now.nowbot.throwable.botRuntimeException.BindException.NotBindException.YouNotBind
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.util.ObjectUtils
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
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
    private val osuGroupConfigRepository: OsuGroupConfigRepository
) {
    private val updateUserSet: MutableSet<Long?> = CopyOnWriteArraySet()
    private val nowUpdate = AtomicBoolean(false)

    var log: Logger = LoggerFactory.getLogger(BindDao::class.java)

    private val indexCache: MutableMap<Long, String> = ConcurrentHashMap()
    private val captchaCache: Cache<String, Long?> = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .removalListener { _: Any?, id: Any?, _: RemovalCause? -> indexCache.remove(id) }
        .build()
    private val random = Random()

    private fun generateRandomCode(): String {
        val code = 100000 + random.nextInt(900000) // 6位数
        return code.toString()
    }

    fun generateCaptcha(userID: Long): String {
        val oldCode = indexCache.remove(userID)
        if (oldCode != null) {
            captchaCache.invalidate(oldCode)
        }

        var code: String
        do {
            code = generateRandomCode()
        } while (captchaCache.getIfPresent(code) != null)

        captchaCache.put(code, userID)
        indexCache[userID] = code
        return code
    }

    /**
     * 获取绑定的玩家
     *
     * @param qq       qq
     * @param isMyself 仅影响报错信息，不影响结果
     * @return 绑定的玩家
     */
    fun getBindFromQQ(qq: Long, isMyself: Boolean = false): BindUser {
        if (qq < 0) {
            return try {
                getBindUserFromOsuID(-qq)
            } catch (_: BindException) {
                BindUser(-qq, "unknown")
            }
        }
        val liteData = bindQQMapper.findById(qq).getOrNull()
            ?: if (isMyself) {
                throw YouNotBind()
            } else {
                throw UserNotBind()
            }

        val u = liteData.osuUser
        return fromLite(u)!!
    }

    /**
     * 不报错的方法
     */
    fun getBindFromQQOrNull(qq: Long): BindUser? {
        return try {
            getBindFromQQ(qq, true)
        } catch (_: BindException) {
            null
        }
    }

    fun getBindFromIDs(ids: Iterable<Long>): List<BindUser> {
        val lites = bindQQMapper.findAllByOsuID(ids)
        return lites.map { fromLite(it.osuUser)!! }
    }

    fun getBindFromQQs(qqs: Iterable<Long>): List<BindUser> {
        val lites = bindQQMapper.findAllByOsuID(bindQQMapper.findAllUserByQQ(qqs).map {it.uid})
        return lites.map { fromLite(it.osuUser)!! }
    }

    fun saveBind(user: BindUser): BindUser {
        var lite = OsuBindUserLite(user)
        lite = bindUserMapper.save(lite)
        return fromLite(lite)!!
    }

    fun getBindUserFromOsuID(userID: Long?): BindUser {
        if (userID == null) throw IllegalQQ()

        var liteData: OsuBindUserLite?
        try {
            liteData = bindUserMapper.getByOsuID(userID)
        } catch (_: IncorrectResultSizeDataAccessException) {
            bindUserMapper.deleteOutdatedByOsuID(userID)
            liteData = bindUserMapper.getByOsuID(userID)
        }

        if (liteData == null) throw UserNotBind()
        return fromLite(liteData)!!
    }

    fun getBindUserFromOsuIDOrNull(userID: Long?): BindUser? {
        return try {
            getBindUserFromOsuID(userID)
        } catch (_: BindException) {
            null
        }
    }

    fun getAllBindUser(userIDs: Collection<Long>): List<OsuBindUserLite> {
        return bindUserMapper.getAllByOsuID(userIDs)
    }

    fun getQQLiteFromUserID(userID: Long): QQBindLite? {
        return bindQQMapper.findByOsuID(userID)
    }

    fun getQQLiteFromQQ(qq: Long): QQBindLite? {
        return bindQQMapper.findById(qq).getOrNull()
    }

    fun verifyCaptcha(code: String): Long {
        val cachedUserId = captchaCache.getIfPresent(code)
        if (cachedUserId != null) {
            captchaCache.invalidate(code)
            indexCache.remove(cachedUserId)
            return cachedUserId
        }
        return -1
    }

    fun bindQQ(qq: Long?, user: OsuBindUserLite): QQBindLite {
        val bindUserLite: OsuBindUserLite

        if (user.refreshToken != null) {
            val count = bindQQMapper.countByUserID(user.osuID)
            if (count > 1) {
                bindUserMapper.deleteAllByOsuID(user.osuID)
            }

            // checkSave
            if (user.id == null && bindUserMapper.countAllByOsuID(user.osuID) > 0) {
                bindUserMapper.deleteOutdatedByOsuID(user.osuID)
            }

            bindUserLite = bindUserMapper.save(user)
        } else {
            val userLite = bindUserMapper.getFirstByOsuID(user.osuID)

            // checkSave
            if (userLite == null) {
                if (user.id == null && bindUserMapper.countAllByOsuID(user.osuID) > 0) {
                    bindUserMapper.deleteOutdatedByOsuID(user.osuID)
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

    fun bindDiscord(discordId: String?, user: BindUser?): DiscordBindLite {
        return bindDiscord(discordId, fromModel(user))
    }

    fun bindDiscord(discordId: String?, user: OsuBindUserLite?): DiscordBindLite {
        val discordBind = DiscordBindLite()
        discordBind.id = discordId
        discordBind.osuUser = user
        return bindDiscordMapper.save(discordBind)
    }

    fun getBindUser(name: String): BindUser? {
        val id = getOsuID(name) ?: return null
        return fromLite(bindUserMapper.getByOsuID(id))
    }

    fun getBindUser(userID: Long?): BindUser? {
        if (userID == null) return null
        return fromLite(bindUserMapper.getByOsuID(userID))
    }

    fun getSBQQLiteFromUserID(userID: Long): SBQQBindLite? {
        return sbQQBindMapper.findByUserID(userID)
    }

    fun getSBQQLiteFromQQ(qq: Long): SBQQBindLite? {
        return sbQQBindMapper.findById(qq).getOrNull()
    }

    fun getSBBindUser(name: String): SBBindUser {
        return getSBBindUser(getSBUserID(name))
    }

    fun getSBBindUser(userID: Long?): SBBindUser {
        if (userID == null) throw YouNotBind()

        var liteData: SBBindUserLite?

        try {
            liteData = sbBindUserMapper.getUser(userID)
        } catch (_: IncorrectResultSizeDataAccessException) {
            sbBindUserMapper.deleteOutdatedBind(userID)
            liteData = sbBindUserMapper.getUser(userID)
        }

        if (liteData == null) throw UserNotBind()
        return liteData.toSBBindUser()
    }

    fun getSBBindFromQQ(qq: Long, isMyself: Boolean): SBBindUser {
        if (qq < 0) {
            return try {
                getSBBindUser(-qq)
            } catch (_: BindException) {
                SBBindUser(-qq, "unknown")
            }
        }
        val liteData = sbQQBindMapper.findById(qq)
        if (liteData.isEmpty) {
            if (isMyself) {
                throw YouNotBind()
            } else {
                throw UserNotBind()
            }
        }

        return liteData.get().bindUser
    }

    fun saveBind(user: SBBindUser?): SBBindUser? {
        if (user == null) return null


        var lite = user.toSBBindUserLite()
        lite = sbBindUserMapper.save(lite)
        return lite.toSBBindUser()
    }

    fun bindSBQQ(qq: Long, user: SBBindUser): SBQQBindLite {
        val data = sbBindUserMapper.getUser(user.userID)
        if (data == null) {
            return bindSBQQ(qq, user.toSBBindUserLite())
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
            return bindQQ(qq, fromModel(user)!!)
        } else {
            bindUserLite.accessToken = user.accessToken
            bindUserLite.refreshToken = user.refreshToken
            bindUserLite.time = user.time
            bindUserLite.osuName = user.username
            return bindQQ(qq, bindUserLite)
        }
    }

    fun updateToken(uid: Long?, accessToken: String?, refreshToken: String?, time: Long?) {
        if (nowUpdate.get()) {
            updateUserSet.add(uid)
        }
        bindUserMapper.updateToken(uid, accessToken, refreshToken, time)
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
        val qqBind = bindQQMapper.findByOsuID(osuID)

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
        return bindQQMapper.findByOsuID(userID)
    }

    fun removeBind(uid: Long) {
        bindUserMapper.deleteAllByOsuID(uid)
    }

    fun backupBind(uid: Long) {
        bindUserMapper.backupBindByOsuID(uid)
    }

    fun getOsuID(name: String): Long? {
        return osuFindNameMapper.getUserIDByUsernameIgnoreCase(name)
    }

    fun removeNameToID(userID: Long) {
        osuFindNameMapper.deleteByUserID(userID)
    }

    fun saveNameToID(id: Long, names: List<String>) {
        if (names.isEmpty()) return

        names.forEachIndexed { index, name ->
            val x = OsuNameToIDLite(id, name, index)
            osuFindNameMapper.save(x)
        }
    }

    /**
     * 通过 osuFindNameMapper 获取
     */
    fun getUsername(userID: Long): String {
        return osuFindNameMapper.getUsername(userID) ?: userID.toString()
    }


    fun countNameToID(userID: Long): Int {
        return osuFindNameMapper.countByUserID(userID)
    }

    fun updateNameToID(user: OsuUser) {
        val names = listOf(user.username) + (user.previousNames ?: emptyList())

        val count = countNameToID(user.userID)

        if (count == 0 || count != names.size) {
            removeNameToID(user.userID)
            saveNameToID(user.userID, names)
        }
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

    fun saveSBNameToID(id: Long, names: List<String>) {
        if (names.isEmpty()) return

        names.forEachIndexed { index, name ->
            val x = SBNameToIDLite(id, name, index)
            sbFindNameMapper.save(x)
        }
    }

    fun countSBNameToID(userID: Long): Int {
        return sbFindNameMapper.countByUserID(userID)
    }

    fun updateSBNameToID(id: Long, name: String) {
        updateSBNameToID(SBUser(userID = id, username = name))
    }

    fun updateSBNameToID(user: SBUser) {
        val names = listOf(user.username)

        val count = countSBNameToID(user.userID)

        if (count == 0 || count != names.size) {
            removeSBNameToID(user.userID)
            saveSBNameToID(user.userID, names)
        }
    }

    fun getBindUserByDbId(id: Long?): BindUser? {
        if (id == null) return null
        val data = bindUserMapper.findById(id)
        return fromLite(data.getOrNull() ?: return null)
    }

    @Async fun refreshOldUserToken(userApiService: OsuUserApiService) {
        nowUpdate.set(true)
        updateUserSet.clear()
        try {
            refreshOldUserTokenOne(userApiService)
        } catch (e: RuntimeException) {
            if (e !is WebClientResponseException.Unauthorized) {
                log.error("更新用户出现异常", e)
            }
            // 已经 log
        } finally {
            updateUserSet.clear()
            nowUpdate.set(false)
        }
    }

    private fun refreshOldUserTokenOne(userApiService: OsuUserApiService) {
        val now = System.currentTimeMillis()
        var user = bindUserMapper.getOneOldBindUser(now)
        if (user != null) {
            val u = user
            if (updateUserSet.remove(u.id)) return

            if (ObjectUtils.isEmpty(u.refreshToken)) {
                bindUserMapper.backupBindByOsuID(u.osuID)
                return
            }

            // log.info("更新用户: {}", u.getOsuName());
            refreshOldUserToken(u, userApiService)
            return
        }

        user = bindUserMapper.getOneOldBindUserHasWrong(now)
        if (user != null) {
            val u = user
            if (updateUserSet.remove(u.id)) return
            if (ObjectUtils.isEmpty(u.refreshToken)) {
                bindUserMapper.backupBindByOsuID(u.osuID)
                return
            }
            // 出错超 5 次默认无法再次更新了
            if (u.updateCount > 5) {
                bindUserMapper.backupBindByOsuID(u.id)
            }

            // log.info("更新用户: {}", u.getOsuName());
            refreshOldUserToken(u, userApiService)
        }
    }

    private fun refreshOldUserTokenPack(osuGetService: OsuUserApiService) {
        val now = System.currentTimeMillis()
        var succeedCount = 0
        var users: MutableList<OsuBindUserLite>
        // 降低更新 token 时的优先级
        setPriority(10)
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
                bindUserMapper.backupBindByOsuID(u.osuID)
                continue
            }
            // 出错超 5 次默认无法再次更新了
            if (u.updateCount > 5) {
                // 回退到用户名绑定
                bindUserMapper.backupBindByOsuID(u.id)
            }
            // log.info("更新用户 {}", u.getOsuName());
            try {
                refreshOldUserToken(u, osuGetService)
                if (u.updateCount > 0) bindUserMapper.clearUpdateCount(u.id)
                errCount = 0
            } catch (_: WebClientResponseException.Unauthorized) {
                // 绑定被取消或者过期, 不再尝试
                log.info("绑定 {} 失败：取消绑定", u.osuName)
                bindUserMapper.backupBindByOsuID(u.id)
            } catch (e: Exception) {
                bindUserMapper.addUpdateCount(u.id)
                log.error("绑定 {} 第 {} 次失败：出现异常: ", u.osuName, errCount, e)
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
                userApiService.refreshUserToken(fromLite(u)!!)
                return true
            } catch(e: ExecutionException) {
                when(e.cause) {

                    is WebClientResponseException.Unauthorized -> {
                        log.info("刷新用户令牌：更新 {} 令牌失败, token 失效, 绑定取消", u.osuName)
                        bindUserMapper.backupBindByOsuID(u.osuID)
                        return false
                    }

                    is WebClientResponseException.Forbidden -> {
                        log.info("刷新用户令牌：更新 {} 令牌失败, 可能被识别为滥用 API 而禁止访问", u.osuName)
                        return false
                    }

                    else -> {
                        badRequest++

                        if (badRequest < 3) {
                            log.error("刷新用户令牌：更新 {} 令牌失败, 第 {} 次重试", u.osuName, badRequest)
                        } else {
                            log.error(
                                "刷新用户令牌：更新 {} 令牌失败, 第 {} 次重试失败, 放弃更新。错误原因：",
                                u.osuName,
                                badRequest,
                                e
                            )
                            return false
                        }
                    }
                }

            } catch (e1: Throwable) {
                log.error("刷新用户令牌：神秘错误: ", e1)
                return false
            }
        }
    }

    val allGroupMode: Map<Long, OsuMode>
        get() = osuGroupConfigRepository
            .findAll().associate { lite ->
                (lite.groupId ?: -1) to (lite.mainMode ?: OsuMode.DEFAULT)
            }

    fun getGroupModeConfig(event: MessageEvent?): OsuMode {
        if (event == null || event.subject !is Group) {
            return OsuMode.DEFAULT
        }

        return osuGroupConfigRepository.findById(event.subject.id).getOrNull()?.mainMode ?: OsuMode.DEFAULT
    }

    fun getAllUserIdLimit50(start: Int): List<Long> {
        return bindUserMapper.getAllBindUserIdLimit50(start)
    }

    fun getAllQQBindUser(qqs: Collection<Long>): List<QQBindLite.QQUser> {
        return bindQQMapper.findAllUserByQQ(qqs)
    }

    fun saveGroupModeConfig(groupId: Long, mode: OsuMode?) {
        if (isDefaultOrNull(mode)) {
            osuGroupConfigRepository.deleteById(groupId)
        } else {
            osuGroupConfigRepository.save(OsuGroupConfigLite(groupId, mode))
        }
    }

    private class RefreshException(var successCount: Int) : RuntimeException()
    companion object {
        @JvmStatic
        fun fromLite(buLite: OsuBindUserLite?): BindUser? {
            if (buLite == null) return null

            val bu = BindUser()
            bu.baseID = buLite.id
            bu.userID = buLite.osuID
            bu.username = buLite.osuName
            bu.accessToken = buLite.accessToken
            bu.refreshToken = buLite.refreshToken
            bu.time = buLite.time
            bu.mode = buLite.mode
            return bu
        }

        fun fromModel(user: BindUser?): OsuBindUserLite? {
            if (user == null) return null
            return OsuBindUserLite(user)
        }
    }
}

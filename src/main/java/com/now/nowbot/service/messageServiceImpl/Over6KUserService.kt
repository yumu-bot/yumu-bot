package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.config.FileConfig
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.Over6KUserService.OverUser
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.JacksonUtil
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.time.LocalDate

//@Service("FOR_NEWBIE_GROUP_WEB_API")
class Over6KUserService(private val userApiService: OsuUserApiService, fileConfig: FileConfig) : MessageService<OverUser> {
    init {
        DATA_FILE = File(fileConfig.root, "6k.out")
        try {
            if (!DATA_FILE.exists()) {
                DATA_FILE.createNewFile()
            }
        } catch (err: IOException) {
            INITED = false
        }

        INITED = true
    }

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<OverUser>
    ): Boolean {
        if (INITED && !messageText.startsWith("高阶出群")) {
            return false
        }
        val ps = messageText.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (ps.size < 3) return false
        val name = ps[1]
        val time: LocalDate
        try {
            time = LocalDate.parse(ps[2])
        } catch (e: Exception) {
            throw TipsException("日期格式错误, 必须为 yyyy-MM-dd")
        }
        val id: Long
        try {
            id = userApiService.getOsuID(name)
        } catch (e: Exception) {
            throw TipsException("没找到用户 $name"
            )
        }

        data.value = OverUser(id, name, time)
        return true
    }

    @CheckPermission(isSuperAdmin = true) @Throws(Throwable::class) override fun handleMessage(
        event: MessageEvent,
        param: OverUser
    ) {
        saveUser(param)
        event.reply("添加成功")
    }

    data class OverUser(val id: Long, val name: String, val date: LocalDate)

    @Throws(FileNotFoundException::class) private fun readFile(): RandomAccessFile {
        return RandomAccessFile(DATA_FILE, "r")
    }

    @Throws(FileNotFoundException::class) private fun writeFile(): RandomAccessFile {
        return RandomAccessFile(DATA_FILE, "rws")
    }

    @Throws(IOException::class) private fun getUsers(data: RandomAccessFile, size: Int): List<OverUser> {
        return getUsers(data, 0, size)
    }

    @Throws(IOException::class) private fun getUsers(data: RandomAccessFile, start: Int, size: Int): List<OverUser> {
        (0..start).forEach { _ -> skipUser(data) }

        val result = (0..size).mapNotNull { readUser(data) }
        return result
    }

    @Throws(IOException::class) private fun skipUser(data: RandomAccessFile) {
        val dataSize = data.readInt() - 4
        if (data.filePointer + dataSize < data.length()) {
            data.seek(dataSize.toLong())
        }
    }

    @Throws(IOException::class) private fun readUser(data: RandomAccessFile): OverUser? {
        if (data.length() - data.filePointer < 4) {
            return null
        }
        val dataSize = data.readInt() - 4
        if (data.filePointer + dataSize > data.length() || dataSize <= 18) {
            return null
        }
        val id = data.readLong()
        val date = ByteArray(dataSize - 8)
        data.read(date)
        val dateStr = String(date, 0, 10)
        val name = String(date, 10, dataSize - 18)
        return OverUser(id, name, LocalDate.parse(dateStr))
    }

    private fun getUserData(u: OverUser): ByteArray {
        val name = u.name.toByteArray()
        //  (size)4 + (uid)8 + (data)10 + data
        val size = 22 + name.size
        val data = ByteBuffer.allocate(size)
        data.putInt(size)
        data.putLong(u.id)
        data.put(u.date.toString().toByteArray())
        data.put(name)
        return data.array()
    }

    @Throws(IOException::class) private fun saveUser(id: Long, name: String, timeStr: String) {
        writeFile().use { f ->
            val u = OverUser(id, name, LocalDate.parse(timeStr))
            val d = getUserData(u)
            f.seek(f.length())
            f.write(d)
        }
    }

    @Throws(IOException::class) private fun saveUser(u: OverUser) {
        writeFile().use { f ->
            val d = getUserData(u)
            f.seek(f.length())
            f.write(d)
        }
    }

    @Throws(IOException::class) fun getResultJson(start: Int, size: Int): String {
        val d = getUsers(readFile(), start, size)
        return JacksonUtil.toJson(d)
    }

    companion object {
        private lateinit var DATA_FILE: File
        private var INITED = false
    }
}

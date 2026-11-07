package com.now.nowbot.image.remote

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Locale

internal object RemoteImageAddresses {
    private val IPV4_LITERAL = Regex("^[0-9]{1,3}(\\.[0-9]{1,3}){3}$")
    private val NUMERIC_LITERAL = Regex("^[0-9.]+$")
    private val HEX_LITERAL = Regex("^(0[xX][0-9a-fA-F]+)(\\.[0-9a-fA-F]+)*$")
    private val HOSTNAME = Regex(
        "^(?=.{1,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$"
    )
    private val BLOCKED_SUFFIXES = listOf(
        ".localhost",
        ".local",
        ".localdomain",
        ".internal",
        ".home",
        ".lan",
        ".corp",
        ".intranet",
        ".arpa",
    )

    fun isValidHost(host: String): Boolean {
        val value = host.trim()
        if (value.isEmpty() || value.length > 253 || value.endsWith('.')) return false
        if (value.any { it <= '\u0020' || it >= '\u007f' } || value.contains('%')) return false

        val lower = value.lowercase(Locale.ROOT)
        if (lower == "localhost" || BLOCKED_SUFFIXES.any { lower.endsWith(it) }) return false

        if (lower.contains(':')) {
            val address = parseIpv6Literal(lower) ?: return false
            return isPublic(address)
        }
        if (NUMERIC_LITERAL.matches(lower)) {
            val address = parseIpv4Literal(lower) ?: return false
            return isPublic(address)
        }
        if (HEX_LITERAL.matches(lower)) return false
        if (!HOSTNAME.matches(lower)) return false
        return true
    }

    fun isPublic(address: InetAddress): Boolean {
        val bytes = address.address
        return when (bytes.size) {
            4 -> isPublicIpv4(bytes)
            16 -> isPublicIpv6(bytes)
            else -> false
        }
    }

    private fun parseIpv4Literal(host: String): InetAddress? {
        if (!IPV4_LITERAL.matches(host)) return null
        val parts = host.split('.')
        if (parts.size != 4) return null
        val bytes = ByteArray(4)
        for (index in 0..3) {
            val part = parts[index]
            if (part.isEmpty() || part.length > 3) return null
            if (part.length > 1 && part[0] == '0') return null
            val value = part.toIntOrNull() ?: return null
            if (value !in 0..255) return null
            bytes[index] = value.toByte()
        }
        return InetAddress.getByAddress(bytes)
    }

    private fun parseIpv6Literal(host: String): InetAddress? = try {
        val address = InetAddress.getByName(host)
        address.takeIf { it.address.size == 16 }
    } catch (e: UnknownHostException) {
        null
    } catch (e: SecurityException) {
        null
    }

    private fun isPublicIpv4(address: ByteArray): Boolean {
        val b0 = address[0].toInt() and 0xFF
        val b1 = address[1].toInt() and 0xFF
        val b2 = address[2].toInt() and 0xFF
        if (b0 == 0 || b0 == 10 || b0 == 127) return false
        if (b0 == 100 && (b1 and 0xC0) == 64) return false
        if (b0 == 169 && b1 == 254) return false
        if (b0 == 172 && (b1 and 0xF0) == 16) return false
        if (b0 == 192 && b1 == 168) return false
        if (b0 == 192 && b1 == 0 && b2 == 0) return false
        if (b0 == 192 && b1 == 0 && b2 == 2) return false
        if (b0 == 192 && b1 == 88 && b2 == 99) return false
        if (b0 == 198 && (b1 == 18 || b1 == 19)) return false
        if (b0 == 198 && b1 == 51 && b2 == 100) return false
        if (b0 == 203 && b1 == 0 && b2 == 113) return false
        if (b0 >= 224) return false
        return true
    }

    private fun isPublicIpv6(address: ByteArray): Boolean {
        val b0 = address[0].toInt() and 0xFF
        val b1 = address[1].toInt() and 0xFF
        val b2 = address[2].toInt() and 0xFF
        val b3 = address[3].toInt() and 0xFF
        // 仅接受 2000::/3 全球单播，其余（ULA、链路本地、回环、映射/兼容、
        // NAT64、6to4、Teredo、文档与保留段）一并拒绝。
        if ((b0 and 0xE0) != 0x20) return false
        if (b0 == 0x20 && b1 == 0x01) {
            if ((b2 and 0xFE) == 0) return false
            if (b2 == 0x0D && b3 == 0xB8) return false
        }
        if (b0 == 0x20 && b1 == 0x02) return false
        if (b0 == 0x3F && b1 == 0xFE) return false
        if (b0 == 0x3F && b1 == 0xFF && (b2 and 0xF0) == 0) return false
        return true
    }
}

internal class PublicOnlyDns(
    private val resolver: (String) -> Array<InetAddress> = { InetAddress.getAllByName(it) },
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val resolved = try {
            resolver(hostname)
        } catch (e: SecurityException) {
            throw UnknownHostException(hostname)
        }
        if (resolved.isEmpty()) throw UnknownHostException(hostname)
        for (address in resolved) {
            if (!RemoteImageAddresses.isPublic(address)) throw UnknownHostException(hostname)
        }
        return resolved.toList()
    }
}

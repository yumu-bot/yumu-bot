package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.TipsRuntimeException

open class PermissionException(message: String?): TipsRuntimeException(message), BotException {

    open class GroupException(message: String?): PermissionException(message) {
        class BelowGroupAdministrator:
            GroupException("权限不足！只有机器人属于群聊管理员或群主才可以撤回其他人的消息！")

        class BelowGroupOwner:
            GroupException("权限不足！只有机器人属于群聊管理员或群主才可以撤回其他人的消息！")

        class NotGroupOwner:
            GroupException("权限不足！只有群主才可以撤回自己的消息！")
    }

    open class DeniedException(message: String?): PermissionException(message) {
        class BelowSuperAdministrator:
            DeniedException("权限不足！只有开发者或超级管理员可以使用此功能！")

        class BelowGroupAdministrator:
            DeniedException("权限不足！只有群聊管理员或群主（包括开发者）可以使用此功能！！")
    }

    open class RoleException(message: String?): PermissionException(message) {
        class NormalUserUseAdminService(service: String, qq: Long):
            RoleException("非管理员 $qq 使用管理员功能：$service")

        class AdminUseAdminService(service: String, qq: Long):
            RoleException("管理员 $qq 使用管理员功能：$service")

        class SomebodyUseTestService(service: String, qq: Long):
            RoleException("$qq 使用测试功能：$service")

    }

    open class WhiteListException(message: String?): PermissionException(message) {
        class UserFilter(service: String, qq: Long):
            WhiteListException("用户 $qq 白名单过滤：$service")

        class GroupFilter(service: String, qq: Long):
            WhiteListException("群聊 $qq 白名单过滤：$service")
    }

    open class BlackListException(message: String?): PermissionException(message) {
        class UserFilter(service: String, qq: Long):
            BlackListException("用户 $qq 黑名单过滤：$service")

        class GroupFilter(service: String, qq: Long):
            BlackListException("群聊 $qq 黑名单过滤：$service")

        class Blocked(service: String, qq: Long):
            BlackListException("$qq 权限不足，禁止使用：$service")
    }

    open class TokenBucketException(message: String?): PermissionException(message) {
        class OutOfToken(service: String, time: String):
            TokenBucketException("服务 $service 已经超过限制。请等待 $time。")

    }

}

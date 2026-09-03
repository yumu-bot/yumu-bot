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
        class On(service: String):
            BlackListException("功能 $service 已经开启，无需重复操作。")

        class Failed(service: String):
            BlackListException("功能 $service 已经尝试开启，但该功能目前仍被管理员关闭，无法使用。")

        class Other(service: String):
            BlackListException("功能 $service 已清除当前级别的设置，但该功能仍处于其他限制状态。")

        class Banned(qq: Long):
            BlackListException("已清除 $qq 的设置，但该功能目前仍被管理员封禁，无法使用。")

        class BannedGroup(group: Long):
            BlackListException("已清除群组 $group 的设置，但该功能目前仍被管理员封禁，无法使用。")
    }

    open class BlackListException(message: String?): PermissionException(message) {
        class Off(service: String):
            BlackListException("功能 $service 已经关闭，无需重复操作。")

        class Duplicate(qq: Long):
            BlackListException("$qq 已被封禁，无需重复操作。")

        class DuplicateGroup(group: Long):
            BlackListException("群组 $group 已被封禁，无需重复操作。")
    }

    open class TokenBucketException(message: String?): PermissionException(message) {
        class OutOfToken(service: String, time: String):
            TokenBucketException("服务 $service 已经超过限制。请等待 $time。")

    }

}

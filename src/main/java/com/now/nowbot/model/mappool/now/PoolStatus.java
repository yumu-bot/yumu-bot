package com.now.nowbot.model.mappool.now;

public enum PoolStatus {
    /**
     * 选图中
     */

    OPEN,
    /**
     * 公开
     */
    SHOW,
    /**
     * 截止
     * 停止选图, 但是不公开, 可以恢复继续选图
     */
    STOP,
    /**
     * 已删除
     * 标记为已删除可以通过联系管理员恢复
     */
    DELETE
}

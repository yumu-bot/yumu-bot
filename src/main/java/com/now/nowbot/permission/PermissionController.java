package com.now.nowbot.permission;

import java.util.List;
import java.util.Set;

public interface PermissionController {

    /**
     * 功能开关控制
     *
     * @param name 名
     * @param open true:开; false:关
     */
    void switchService(String name, boolean open);

    /**
     * 功能开关控制
     *
     * @param name 名
     * @param open true:开; false:关
     * @param time 到指定时间撤销修改
     */
    void switchService(String name, boolean open, Long time);

    /**
     * 拉黑群
     *
     * @param id 群id
     */
    void blockGroup(Long id);

    /**
     * 拉黑群一段时间
     *
     * @param id   群id
     * @param time 时间, 单位毫秒
     */
    void blockGroup(Long id, Long time);

    void blockGroup(String service, Long id);

    void blockGroup(String service, Long id, Long time);

    void unblockGroup(Long id);

    void unblockGroup(String service, Long id);

    void unblockGroup(String service, Long id, Long time);

    /**
     * 拉黑个人
     *
     * @param id qq
     */
    void blockUser(Long id);

    /**
     * 拉黑个人一段时间
     *
     * @param id   qq
     * @param time 多少时间
     */
    void blockUser(Long id, Long time);

    void blockUser(String service, Long id);

    void blockUser(String service, Long id, Long time);

    void unblockUser(Long id);

    void unblockUser(String service, Long id);

    void unblockUser(String service, Long id, Long time);

    /* ****************************************************************** */

    /**
     * 忽略群
     *
     * @param id 群号
     */
    void ignoreAll(Long id);

    void ignoreAll(Long id, Long time);

    void ignoreAll(String service, Long id);

    void ignoreAll(String service, Long id, Long time);

    /**
     * 取消忽略群
     *
     * @param id 群号
     */
    void unignoreAll(Long id);

    void unignoreAll(Long id, Long time);

    void unignoreAll(String service, Long id);

    void unignoreAll(String service, Long id, Long time);

    List<LockRecord> queryAllBlock();

    LockRecord queryGlobal();

    LockRecord queryBlock(String service);

    record LockRecord(String name, boolean enable, Set<Long> groups, Set<Long> users, Set<Long> ignores) {
    }
}

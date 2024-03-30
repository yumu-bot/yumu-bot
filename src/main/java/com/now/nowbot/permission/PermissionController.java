package com.now.nowbot.permission;

import java.util.List;
import java.util.Set;

public interface PermissionController {
    void switchService(String name, boolean open);

    void switchService(String name, boolean open, Long time);

    void blockGroup(Long id);

    void blockGroup(Long id, Long time);

    void blockGroup(String service, Long id);

    void blockGroup(String service, Long id, Long time);

    void unblockGroup(Long id);

    void unblockGroup(String service, Long id);

    void unblockGroup(String service, Long id, Long time);

    void blockUser(Long id);

    void blockUser(Long id, Long time);

    void blockUser(String service, Long id);

    void blockUser(String service, Long id, Long time);

    void unblockUser(Long id);

    void unblockUser(String service, Long id);

    void unblockUser(String service, Long id, Long time);

    /********************************************************************/

    void ignoreAll(Long id);

    void ignoreAll(Long id, Long time);

    void ignoreAll(String service, Long id);

    void ignoreAll(String service, Long id, Long time);

    void unignoreAll(Long id);

    void unignoreAll(Long id, Long time);

    void unignoreAll(String service, Long id);

    void unignoreAll(String service, Long id, Long time);

    List<LockRecord> queryAllBlock();

    LockRecord queryBlock(String service);

    record LockRecord(String namee, Set<Long> groups, Set<Long> users, Set<Long> ignores) {
    }
}

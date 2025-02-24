package com.now.nowbot.entity.bind;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.model.BindUser;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "osu_bind_qq")
public class QQBindLite {
    @Id
    private Long qq;
    @OneToOne(targetEntity = OsuBindUserLite.class, orphanRemoval = true)
    private OsuBindUserLite osuUser;


    public Long getQq() {
        return qq;
    }

    public void setQq(Long qq) {
        this.qq = qq;
    }

    public OsuBindUserLite getOsuUser() {
        return osuUser;
    }

    public void setOsuUser(OsuBindUserLite osuUser) {
        this.osuUser = osuUser;
    }

    public BindUser getBindUser() {
        return BindDao.fromLite(osuUser);
    }

    public interface QQUser{
        long getQid();
        long getUid();
        String getName();
    }
}

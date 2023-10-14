package com.now.nowbot.entity.bind;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.model.BinUser;
import jakarta.persistence.*;

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

    public BinUser getBinUser() {
        return BindDao.fromLite(osuUser);
    }
}

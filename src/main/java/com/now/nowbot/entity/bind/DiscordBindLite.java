package com.now.nowbot.entity.bind;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.model.BindUser;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "osu_bind_discord")
public class DiscordBindLite {
    @Id
    private String id;

    @OneToOne(targetEntity = OsuBindUserLite.class, orphanRemoval = true)
    private OsuBindUserLite osuUser;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}

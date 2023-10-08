package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("SQL")
public class SqlService implements MessageService<Matcher> {

    @Resource
    EntityManager entityManager;

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(friendlegacy|fl(?![a-zA-Z_]))+(\\s*(?<n>\\d+))?(\\s*[:-]\\s*(?<m>\\d+))?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var sql = event.getRawMessage().substring(event.getRawMessage().indexOf('\n'));
        jakarta.persistence.Query q = entityManager.createNativeQuery(sql);
        try {
            List<Object[]> res = q.getResultList();
            final var col = new StringJoiner("\n");
            class num {
                int i = 0;

                void add() {
                    i++;
                }

                boolean max() {
                    return i < 20;
                }
            }
            var n = new num();
            res.forEach(e -> {
                if (n.max()) {
                    var row = new StringJoiner(", ");
                    for (var d : e) row.add(d.toString());
                    col.add(row.toString().substring(0, Math.min(row.length(), 15)));
                }
                n.add();
            });
            event.getSubject().sendText(col.toString());
        } catch (Exception e) {
            event.getSubject().sendText(e.getMessage());
        }

    }

}

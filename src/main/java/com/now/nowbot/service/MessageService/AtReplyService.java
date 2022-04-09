package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.config.Permission;
import com.now.nowbot.entity.Wtf;
import com.now.nowbot.mapper.WtfMapper;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.regex.Matcher;

@Service("at-replay")
public class AtReplyService implements MessageService{
    WtfMapper wtfMapper;
    Random random = new Random(1611);
    Wtf defaultWtf = new Wtf("我爱你");


    @Autowired
    public AtReplyService(WtfMapper wtfMapper){
        this.wtfMapper = wtfMapper;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var at = QQMsgUtil.getType(event.getMessage(), At.class);
        if (at == null || at.getTarget() != NowbotConfig.QQ) return;
        if (Permission.isSupper(event.getSender().getId())){
            var tx = QQMsgUtil.getType(event.getMessage(), PlainText.class);
            if (tx != null) {
                var text = tx.contentToString().trim();
                wtfMapper.save(new Wtf(text));
            }
            return;
        }

        var p = random.nextInt(Math.toIntExact(wtfMapper.count()));
        var text = wtfMapper.findAll(Pageable.ofSize(1).withPage(p)).stream().findFirst().orElse(defaultWtf).getText();
        text.replaceAll("\\$\\{from}", event.getSenderName());
        event.getSubject().sendMessage(text);
    }
}

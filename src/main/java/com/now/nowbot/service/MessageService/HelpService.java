package com.now.nowbot.service.MessageService;

import com.now.nowbot.util.Instruction;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("help")
public class HelpService implements MessageService {
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        StringBuilder sb = new StringBuilder();
        for(var ins: Instruction.values()) {
            if(ins.getDesc()!=null)
                sb.append(ins.getDesc()).append("\n");
        }
        from.sendMessage(sb.toString());
    }
}

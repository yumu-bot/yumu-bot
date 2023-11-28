package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.Pattern4ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;

@Service("TRANS")
public class TransService implements MessageService<Matcher> {
    static int[] c1 = {0,2,4,5,7,9,11};
    static List<String> d1 = List.of("null","C","C#","D","D#","E","F","F#","G","G#","A","A#","B");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Pattern4ServiceImpl.TRANS.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int d_index = d1.indexOf(matcher.group("a"));
        if (d_index <= 0) throw new TipsException("输入错误");
        int x = Integer.parseInt(matcher.group("b"));
        StringBuilder sb = new StringBuilder();
        if(d_index == 2||d_index==4||d_index==7||d_index==9||d_index==11){
            sb.append("降").append(d1.get(d_index+1));
        }else {
            sb.append(d1.get(d_index));
        }
        sb.append("大调").append('\n');
        for (int j : c1) {
            if (12 < d_index + j) {
                sb.append(d1.get(d_index + j - 12)).append(x + 1).append(' ');
            } else {
                sb.append(d1.get(d_index + j)).append(x).append(' ');
            }
        }
        event.getSubject().sendMessage(sb.toString());
    }
}

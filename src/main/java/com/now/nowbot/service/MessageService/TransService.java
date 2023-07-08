package com.now.nowbot.service.MessageService;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.throwable.TipsException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;

@Service("trans")
public class TransService implements MessageService{
    static int[] c1 = {0,2,4,5,7,9,11};
    static List<String> d1 = List.of("null","C","C#","D","D#","E","F","F#","G","G#","A","A#","B");

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int d_index = d1.indexOf(matcher.group("a"));
        if (d_index <= 0) throw new TipsException("输入错误");
        int x = Integer.parseInt(matcher.group("b"));
        StringBuffer sb = new StringBuffer();
        if(d_index == 2||d_index==4||d_index==7||d_index==9||d_index==11){
            sb.append("降").append(d1.get(d_index+1));
        }else {
            sb.append(d1.get(d_index));
        }
        sb.append("大调").append('\n');
        for (int i = 0; i < c1.length; i++) {
            if (12 < d_index+c1[i] ){
                sb.append(d1.get(d_index+c1[i]-12)).append(x+1).append(' ');
            }else {
                sb.append(d1.get(d_index+c1[i])).append(x).append(' ');
            }
        }
        event.getSubject().sendMessage(sb.toString());
    }
}

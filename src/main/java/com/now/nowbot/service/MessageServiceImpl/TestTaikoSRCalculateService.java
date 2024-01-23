package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.Instructions;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

@Service("TEST_TAIKO_SR_CALCULATE")
public class TestTaikoSRCalculateService implements MessageService<Matcher> {
    private static final char CHAR_X = 'x';
    private static final char CHAR_O = 'o';
    private static final char CHAR_NONE = '-';
    enum status{
        XX,OO,XO,OX,X,O,NONE;
        static status get(char a, char b){
            if (a == CHAR_X){
                if (b == CHAR_X) return XX;
                else if (b == CHAR_O) return XO;
                else if (b == CHAR_NONE) return X;
            } else if (a == CHAR_O) {
                if (b == CHAR_X) return OX;
                else if (b == CHAR_O) return OO;
                else if (b == CHAR_NONE) return O;
            } else {
                if (b == CHAR_X) return X;
                else if (b == CHAR_O) return O;
                else if (b == CHAR_NONE) return NONE;
            }
            return NONE;
        }
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.TEST_TAIKO_SR_CALCULATE.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var xtr = matcher.group("data").trim().replaceAll("\\s+",String.valueOf(CHAR_NONE));
        event.getSubject().sendMessage("结果:"+get(xtr));
    }

    private float get(String xtr){
        int nexIndex = 4;
        LinkedList<status> statusList = new LinkedList<>();
        var datas = new char[xtr.length()];xtr.getChars(0, xtr.length(), datas, 0);
        //处理前4个
        if (datas.length <= 3) {
            //?
        } else if (datas[1] == CHAR_NONE) {
            statusList.add(datas[0] == CHAR_X ? status.XX : status.OO);
            statusList.add(status.NONE);
            statusList.add(status.get(datas[2], datas[3]));
            nexIndex = 4;
        } else if (datas[2] == CHAR_NONE){
            statusList.add(status.get(datas[0], datas[1]));
            statusList.add(status.NONE);
            nexIndex = 3;
        } else if (datas[3] == CHAR_NONE) {
            statusList.add(status.get(datas[0], datas[1]));
            statusList.add(status.get(datas[2], datas[3]));
            statusList.add(status.get(datas[1], datas[2]));
            statusList.add(status.NONE);
            nexIndex = 4;
        } else {
            statusList.add(status.get(datas[0], datas[1]));
            nexIndex = 2;
        }
        //

        for (int i = nexIndex; i < datas.length; i += nexIndex) {
            if ((i+3) >= datas.length) {
                nexIndex = i;
                break;
            }
            nexIndex = checkNext(statusList, datas[i], datas[i+1] , datas[i+2], datas[i+3]);
        }
        int t = datas.length - nexIndex;
        if (t == 1){
            statusList.add(status.get(datas[nexIndex], CHAR_NONE));
        } else if (t == 2){
            statusList.add(status.get(datas[nexIndex], datas[nexIndex+1]));
        } else if (t == 3){
            statusList.add(status.get(datas[nexIndex], datas[nexIndex+1]));
            statusList.add(status.get(datas[nexIndex+2], CHAR_NONE));
        }


        //解析
        status thisStatus = statusList.poll();
        status nextStatus;

        int a = 0;
        int b = 0;
        int c = 0;
        while ((nextStatus = statusList.poll()) != null){
            if (nextStatus == status.NONE) {
                a++;
                b++;
                break;
            }
            switch (thisStatus){
                case OO:{
                    switch (nextStatus){
                        case OX -> {b++;c++;thisStatus = nextStatus;}
                        case XO -> {a++;c++;thisStatus = nextStatus;}
                        case XX -> {b++;a++;thisStatus = nextStatus;}
                        case X -> a++;
                    }
                } break;
                case XX:{
                    switch (nextStatus){
                        case OX -> {a++;c++;thisStatus = nextStatus;}
                        case XO -> {b++;c++;thisStatus = nextStatus;}
                        case OO -> {b++;a++;thisStatus = nextStatus;}
                        case O -> a++;
                    }
                } break;
                case XO:{
                    switch (nextStatus){
                        case OX -> {a++;b++;thisStatus = nextStatus;}
                        case XX -> {b++;c++;thisStatus = nextStatus;}
                        case OO -> {a++;c++;thisStatus = nextStatus;}
                        case X -> c++;
                        case O -> {
                            a++;
                            c++;
                        }
                    }
                } break;
                case OX:{
                    switch (nextStatus){
                        case XO -> {a++;b++;thisStatus = nextStatus;}
                        case XX -> {a++;c++;thisStatus = nextStatus;}
                        case OO -> {b++;c++;thisStatus = nextStatus;}
                        case O -> c++;
                        case X -> {
                            a++;
                            c++;
                        }
                    }
                }
            }
        }
        return (0.25f*a + b) * (0.5f*c + 1);
    }

    private int checkNext(List<status> statusList, char a, char b, char c, char d){
        if (c == CHAR_NONE){
            statusList.add(status.get(a, b));
            statusList.add(status.NONE);
            return 3;
        } else if (d == CHAR_NONE) {
            statusList.add(status.get(a, b));
            statusList.add(status.get(c, d));
            statusList.add(status.get(b, c));
            statusList.add(status.NONE);
            return 4;
        } else {
            statusList.add(status.get(a, b));
            return 2;
        }
    }
}

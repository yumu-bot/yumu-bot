package com.now.nowbot.throwable;

import com.now.nowbot.model.enums.Mod;

import java.util.Arrays;

public class ModsCatchException extends TipsRuntimeException{
    public static class Create{
        public static ModsCatchException SiseException(){
            // +NFL 字符数量数量不对
            return new ModsCatchException("输入异常");
        }
        public static ModsCatchException ConflictException(Mod...mods){
            // +NM&任意mod or EZ&HR DT&HT NF&PF&SD
            return new ModsCatchException("输入异常"+ Arrays.toString(mods));
        }
    }

    public ModsCatchException(String msg){
        super(msg);
    }
}

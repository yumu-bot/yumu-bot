package com.now.nowbot.irc;

public class IRCMessagePackage {


    static IRCMessagePackage getPackage(String message) {
        return null;
    }


    boolean isPrivmsg(String msg) {
        return msg.contains("PRIVMSG " + msg);
    }

    String getMessageText(String msg) {
        int f = msg.lastIndexOf(':') + 1;
        return msg.substring(f);
    }

    String getChannel(String msg) {
        int f = msg.lastIndexOf("!cho@ppy.sh");
        return msg.substring(1, f);
    }

    public static void main(String[] args) {
        String msg = ":BanchoBot!cho@ppy.sh PRIVMSG -Spring_Night- :ROLL <number> - roll a dice and get random result from 1 to number(default 100)";
        int f = msg.lastIndexOf("!cho@ppy.sh");
        System.out.println(msg.substring(1, f));
    }
}

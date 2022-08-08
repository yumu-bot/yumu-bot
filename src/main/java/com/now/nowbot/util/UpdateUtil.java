package com.now.nowbot.util;

import java.io.IOException;

public class UpdateUtil {
    static public final void update() throws IOException {

        Runtime.getRuntime().exec("/home/spring/update.sh");

        System.exit(0);
    }
}

package com.now.nowbot.util;

import com.now.nowbot.throwable.TipsRuntimeException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class OsuFileReader {

    public static void get(String data) {
        var p = data.split("(\\s*)\\n(\\s*\\n)?");
        if (p.length < 9) throw new TipsRuntimeException("非osu文件");
        int i = 0;
        Integer file_version = Integer.parseInt(p[0].substring(17));
        System.out.println(file_version);
        i++;
        if (p[i].equals("[General]")){
            for (int j = i+1; j < p.length; j++) {
                int s_index = p[j].indexOf(':');
                if (s_index <= 0){
                    i = j;
                    break;
                }
            }
        }
    }

    static int c = 0;
    static int all = 0;

    public static void main1(String[] args) {
        var root = Path.of("A:/osu/Songs");
        try {
            var st = Files.newDirectoryStream(root, Files::isDirectory);
            readDir(st.iterator().next());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        var s = """
                osu file format v4
                                
                [General]
                AudioFilename: Apologize.mp3
                AudioLeadIn: 0
                AudioHash: 5268c9d7bd464220bd0a0f0dcfc7514c
                PreviewTime: 102731
                Countdown: 0
                SampleSet: Soft
                EditorBookmarks: 16291,48833,65104,73240,105782,138325,140358,172901
                                
                [Metadata]
                Title:Apologize
                Artist:One Republic
                Creator:Slezak
                Version:Easy
                                
                [Difficulty]
                HPDrainRate:5
                CircleSize:5
                OverallDifficulty:5
                SliderMultiplier: 1.2
                SliderTickRate: 1
                                
                [Events]
                //Background and Video events
                //Break Periods
                2,64790,72039
                2,121743,139158
                //Storyboard Layer 0 (Background)
                //Storyboard Layer 1 (Failing)
                //Storyboard Layer 2 (Passing)
                //Storyboard Layer 3 (Foreground)
                //Storyboard Sound Samples
                //Background Colour Transformations
                3,100,0,0,0
                                
                [TimingPoints]
                20,508.474576271186,4,2,0,100
                48833,508.474576271186,4,1,1,100
                                
                [HitObjects]
                128,96,16291,1,2
                256,96,17308,1,0
                384,96,18325,5,2
                384,224,19342,1,0
                288,224,20104,1,0
                256,224,20358,6,0,B|256:224|96:224|96:64,3,240
                384,96,24426,5,2
                256,96,25443,1,0
                128,96,26460,5,2
                128,224,27477,1,0
                224,224,28239,1,0
                256,224,28493,6,0,B|256:224|416:224|416:64,3,240
                384,96,32562,5,2
                320,96,33070,1,0
                256,96,33579,1,0
                192,96,34087,1,0
                192,160,34596,5,2
                256,160,35104,1,0
                320,160,35613,1,0
                320,224,36121,1,0
                288,224,36375,6,2,B|288:224|256:224|224:224|192:224|160:224|128:224|96:224|64:192|64:160|64:128|96:96|128:96|160:96|192:96|224:96|256:96,1,420
                288,96,38409,2,2,B|288:96|320:96|352:96|384:96|416:128|416:160|416:192|384:224|352:224|320:224|288:224|256:224|224:224|192:224|160:224|128:224,1,420
                144,160,40697,5,2
                208,160,41206,1,0
                272,160,41714,2,2,B|272:160|416:160,1,120
                400,224,42731,5,2
                336,224,43240,1,0
                272,224,43748,1,2
                208,224,44257,1,0
                176,224,44511,6,2,L|176:224|48:224|48:112|240:112,1,420
                256,112,46545,2,2,L|256:112|368:112|368:224|160:224,1,420
                256,192,48833,5,4
                256,192,49340,1,4
                256,192,49847,1,4
                288,192,50101,1,0
                320,192,50355,1,2
                320,192,50866,5,2
                192,192,51883,1,2
                64,224,52895,6,0,B|64:224|96:320|160:352|256:352|337:334|389:284|433:207|480:192|416:160|384:160|352:128|320:128|272:115|222:93|146:65|32:32,1,840
                256,192,56963,5,4
                256,192,57470,1,4
                256,192,57977,1,4
                224,192,58231,1,0
                192,192,58485,1,2
                192,192,59002,5,2
                320,192,60019,1,2
                448,224,61031,6,0,B|448:224|416:320|352:352|256:352|175:334|123:284|79:207|32:192|96:160|128:160|160:128|192:128|240:115|290:93|366:65|480:32,1,840
                256,128,73239,5,2
                320,128,73748,1,0
                384,128,74256,1,0
                320,128,74765,1,0
                256,128,75273,5,2
                192,128,75782,1,0
                128,128,76290,1,0
                224,128,77053,6,0,C|224:128|256:128|287:128|320:128|352:128|384:144|400:176|400:208|384:240|352:256|320:256|288:256|256:256|224:256|192:256|160:256|128:240|112:208|112:112|128:80|161:64|192:64|328:64,1,900
                384,64,81375,5,2
                384,128,81883,1,0
                320,128,82392,1,0
                256,128,82900,1,0
                192,128,83409,5,2
                192,224,84426,1,0
                288,224,85188,6,0,C|288:224|256:224|225:224|192:224|160:224|128:208|112:176|112:144|128:112|160:96|192:96|224:96|256:96|288:96|320:96|352:96|384:112|400:144|400:240|384:272|351:288|320:288|168:288,1,900
                128,288,89510,5,2
                128,224,90019,1,0
                128,160,90527,1,0
                192,160,91036,1,0
                256,160,91544,5,2
                352,160,92307,1,0
                416,160,92816,1,0
                416,256,93578,6,0,C|416:256|160:256,4,240
                416,192,98155,1,0
                352,192,98663,1,0
                288,192,99171,1,0
                224,192,99680,5,2
                160,192,100188,1,0
                96,192,100697,1,0
                96,96,101460,6,2,C|96:96|336:96,4,240
                96,192,105782,5,4
                160,192,106290,1,4
                224,192,106799,1,4
                256,192,107053,1,0
                288,192,107307,1,2
                352,192,107816,6,0,C|352:192|144:192,2,180
                448,224,109849,6,0,B|448:224|416:320|352:352|256:352|175:334|123:284|79:207|32:192|96:160|128:160|160:128|192:128|240:115|290:93|366:65|480:32,1,840
                416,192,113917,5,4
                352,192,114425,1,4
                288,192,114934,1,4
                256,192,115188,1,0
                224,192,115442,1,2
                160,192,115951,6,0,C|160:192|368:192,2,180
                64,224,117984,6,2,B|64:224|96:320|160:352|256:352|337:334|389:284|433:207|480:192|416:160|384:160|352:128|320:128|272:115|222:93|146:65|32:32,1,840
                256,192,140358,5,4
                256,192,140612,1,4
                256,192,140866,1,4
                256,192,141375,1,4
                288,192,141629,1,0
                320,192,141883,1,2
                304,128,142392,6,2,C|304:128|208:128,4,90
                448,224,144426,6,2,B|448:224|416:320|352:352|256:352|175:334|123:284|79:207|32:192|96:160|128:160|160:128|192:128|240:115|290:93|366:65|480:32,1,840
                256,192,148494,5,4
                256,192,148748,1,4
                256,192,149002,1,4
                256,192,149511,1,4
                224,192,149765,1,0
                192,192,150019,1,2
                208,128,150528,6,2,C|208:128|304:128,4,90
                64,224,152562,6,2,B|64:224|96:320|160:352|256:352|337:334|389:284|433:207|480:192|416:160|384:160|352:128|320:128|272:115|222:93|146:65|32:32,1,840
                256,224,156629,5,4
                256,224,156883,1,4
                256,224,157138,1,4
                256,224,157646,1,4
                256,192,157900,1,0
                256,160,158155,1,2
                256,96,158663,6,2,C|256:96|192:96,2,60
                256,96,159680,2,2,C|256:96|320:96,2,60
                256,40,160697,6,2,B|256:40|208:48|160:64|128:96|112:144|112:192|112:240|128:288|160:320|208:336|256:336|304:336|352:320|384:288|400:240|400:192|384:144|352:112|304:95|256:95|200:127|180:196|248:232,1,840
                256,192,164765,12,2,172900""";
        get(s);
    }

    public static void readDir(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            var st = Files.newDirectoryStream(path, "*.osu");
            for (var x : st) {

                all++;
                var rs = Files.readString(x);
                System.out.println(x.toAbsolutePath());
                get(rs);
                if (all>-4) return;
                var p = rs.replaceAll("(?m)^\\s*$(\\n|\\r\\n)", "").split("(?=\\[\\w+\\])");
                System.out.println(p[0]);
                System.out.println(p[1]);



                var s = rs.replaceAll("(?m)^\\s*$(\\n|\\r\\n)", "").split("\n")[0].trim();
                if (!s.endsWith("v14") && ! s.equals("")) {

                    System.out.println(s);
                    c++;
                }else if (s.equals("")){
                    System.out.println(x.toAbsolutePath());
                }
            }
        }
    }

    static ArrayList<String> getLineString(String data){
        var temp = data.split("(\\s*)\\n(\\s*\\n)?");
        var out = new ArrayList<String>(temp.length);
        out.addAll(Arrays.asList(temp));
        return out;
    }
}
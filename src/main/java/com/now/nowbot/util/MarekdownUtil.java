package com.now.nowbot.util;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import gui.ava.html.image.generator.HtmlImageGenerator;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class MarekdownUtil {
    public static void main(String[] args) {
        MutableDataSet options = new MutableDataSet();
        // uncomment to set optional extensions
        options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));

        // uncomment to convert soft-breaks to hard breaks
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // You can re-use parser and renderer instances
        Node document = parser.parse("""
                ---
                tags:
                  - NAT
                ---
                                
                # Nomination Assessment Team
                                
                *See also: [Beatmap Nominator evaluations](Evaluations)*
                                
                The **Nomination Assessment Team** (***NAT***) are the moderators of the [Beatmap Nominators](/wiki/People/The_Team/Beatmap_Nominators) (*BN*) and ensure the beatmapping side of osu! stays functional.
                                
                Members of the NAT are distinguished by their red username and user title. Like the [Global Moderation Team](/wiki/People/The_Team/Global_Moderation_Team) (*GMT*), they have site-wide moderation permissions and a dark red username in the game's chat channels.
                                
                **All members of the Nomination Assessment Team are sworn to uphold [the Contributor Code of Conduct](/wiki/Contributor_Code_of_Conduct) in addition to the normal [Community Rules](/wiki/Rules).**
                                
                ## Responsibilities
                                
                ***Note: To report a member of NAT misbehaving or breaking the Contributor Code of Conduct, contact the [account support team](/wiki/People/The_Team/Account_support_team#support@ppy.sh).***
                                
                The NAT are responsible for a variety of mapping-related tasks that can be separated into four subcategories:
                                
                - **Evaluation:** Evaluating the proficiency and activity of both current Beatmap Nominators and applicants. Most NAT members are responsible for contributing to this subcategory. For full details on how evaluations work, please see the [evaluations](/wiki/People/The_Team/Nomination_Assessment_Team/Evaluations) page.
                - **Structural:** Documenting topics surrounding the ranking process, amending rules and guidelines to the [ranking criteria](/wiki/Ranking_Criteria), developing and maintaining related tools and websites.
                - **Moderation:** Handling user reports and assessing inappropriate behaviour of Beatmap Nominators. This subcategory is a joint effort between the NAT and the GMT.
                - **Communication:** Promoting transparency between the NAT and the mapping/modding community, including the creation and hosting of meetings.
                                
                A member of the NAT can engage with any tasks within any subcategory that they choose, despite what their primary responsibilities may be listed as. For example, if a member of the NAT prioritises the Structural subcategory, they are not responsible for all tasks within the subcategory and they can still help with tasks from any other subcategory.
                                
                In addition to moderation abilities, NAT also possess the ability to [nominate](/wiki/Beatmap_ranking_procedure#nominations), [qualify](/wiki/Beatmap_ranking_procedure#qualification), and [reset nominations](/wiki/Beatmap_ranking_procedure#nomination-resets) on beatmaps. In this area they are considered largely the same as [full BNs](/wiki/People/The_Team/Beatmap_Nominators#full-beatmap-nominators), and all BN rules and expectations besides activity apply to NAT members. NAT members are not expected to nominate maps regularly the way BNs are due to having additional responsibilities in the osu! mapping and modding community.
                                
                Members of the NAT primarily contributing to the evaluation subcategory are also responsible for modding at least 3 beatmaps per month. This guarantees the NAT are up to date with the modding community when evaluating Beatmap Nominators. Members who fail to meet modding activity requirements will be given a warning. If their activity does not improve within the next month, they will be removed from the NAT. Members of the NAT working on other mapping-related projects may be exempt from modding requirements on a case-by-case basis.
                                
                ## Promotion to the NAT
                                
                Before joining the NAT, a user must either be a full member of the Beatmap Nominators, or previously an NAT member and still involved in the community. Most NAT candidates are initially considered based on their commitment to helping the mapping and modding community, and further demonstration of their ability to contribute to all four subcategories of NAT responsibilities is usually the basis for being promoted.
                                
                Since the majority of new NAT members will help with BN evaluation, it's important that NAT candidates are very proficient in assessing the proficiency of others. Full BNs have the option to do mock evaluations alongside the NAT's real ones, giving them an opportunity to practice. An NAT candidate has a much better chance of being promoted if their evaluations are thorough and come to similar conclusions as the NAT's (or have supportive reasoning otherwise).
                                
                The NAT keep tabs on potential NAT candidates over long periods of time, and occasionally convene to decide if a candidate should be promoted, similar to how BN evaluations are done. BNs are also allowed to ask about joining the NAT if they want to receive feedback and make sure they're being considered. However, depending on the activity and skillset of the current NAT, there may not be a need for new members. New NAT will likely be promoted only when one of the current members is becoming less active, or there are new workloads requiring more members, etc.
                                
                ## Team members
                                
                *Note: All NAT members speak English in addition to the language(s) listed below unless noted otherwise.*
                                
                The [Nomination Assessment Team group page](https://osu.ppy.sh/groups/7) lists all of the team members. In addition to areas mentioned below, all NAT members take part in [reviewing audio and visual content included in beatmaps](/wiki/Rules/Visual_Content_Considerations#getting-your-image-assessed).
                                
                ### osu!
                                
                | Name | Additional languages | Primary responsibilities |
                | :-- | :-- | :-- |
                | ![][flag_GB] [-Mo-](https://osu.ppy.sh/users/2202163) |  | Evaluation, moderation |
                | ![][flag_HK] [Chaoslitz](https://osu.ppy.sh/users/3621552) | Cantonese, Chinese | Evaluation |
                | ![][flag_CN] [Firika](https://osu.ppy.sh/users/9590557) | Chinese | Evaluation |
                | ![][flag_GB] [hypercyte](https://osu.ppy.sh/users/9155377) | Bengali, some Arabic | Evaluation, structural, moderation, communication |
                | ![][flag_SE] [Naxess](https://osu.ppy.sh/users/8129817) | Swedish | Structural, communication |
                | ![][flag_GB] [NexusQI](https://osu.ppy.sh/users/13822800) |  | Evaluation |
                | ![][flag_US] [pishifat](https://osu.ppy.sh/users/3178418) |  | Structural |
                | ![][flag_US] [StarCastler](https://osu.ppy.sh/users/12402453) |  | Evaluation |
                | ![][flag_AT] [Stixy](https://osu.ppy.sh/users/9000308) | German, Serbian | Evaluation |
                | ![][flag_US] [UberFazz](https://osu.ppy.sh/users/8646059) |  | Evaluation |
                | ![][flag_CL] [Uberzolik](https://osu.ppy.sh/users/1314547) | Spanish, French | Evaluation |
                | ![][flag_BE] [yaspo](https://osu.ppy.sh/users/4945926) | Dutch | Evaluation |
                | ![][flag_PL] [Zelq](https://osu.ppy.sh/users/8953955) | Polish | Evaluation |
                                
                ### osu!taiko
                                
                | Name | Additional languages | Primary responsibilities |
                | :-- | :-- | :-- |
                | ![][flag_DE] [Capu](https://osu.ppy.sh/users/2474015) | German | Evaluation |
                | ![][flag_HK] [Faputa](https://osu.ppy.sh/users/845733) | Cantonese, Chinese | Evaluation |
                | ![][flag_US] [radar](https://osu.ppy.sh/users/7131099) |  | Evaluation |
                                
                ### osu!catch
                                
                | Name | Additional languages | Primary responsibilities |
                | :-- | :-- | :-- |
                | ![][flag_ES] [Deif](https://osu.ppy.sh/users/318565) | Spanish, German | Evaluation, moderation |
                | ![][flag_NL] [Greaper](https://osu.ppy.sh/users/2369776) | Dutch | Evaluation, structural |
                | ![][flag_US] [Secre](https://osu.ppy.sh/users/2306637) | | Evaluation |
                                
                ### osu!mania
                                
                | Name | Additional languages | Primary responsibilities |
                | :-- | :-- | :-- |
                | ![][flag_CN] [\\_Stan](https://osu.ppy.sh/users/1653229) | Chinese | Evaluation |
                | ![][flag_ID] [Maxus](https://osu.ppy.sh/users/4335785) | Indonesian | Evaluation, communication |
                | ![][flag_ES] [Quenlla](https://osu.ppy.sh/users/4725379) | Spanish, Portuguese, Galician, Italian, Japanese | Evaluation |
                | ![][flag_US] [Unpredictable](https://osu.ppy.sh/users/7560872) |  | Evaluation |
                                
                <!-- last update: 2022-04-13 with the addition of Maxus to the NAT-->
                                
                [flag_AT]: https://osu.ppy.sh/wiki/shared/flag/AT.gif "Austria"
                [flag_BE]: https://osu.ppy.sh/wiki/shared/flag/BE.gif "Belgium"
                [flag_CL]: https://osu.ppy.sh/wiki/shared/flag/CL.gif "Chile"
                [flag_CN]: https://osu.ppy.sh/wiki/shared/flag/CN.gif "China"
                [flag_DE]: https://osu.ppy.sh/wiki/shared/flag/DE.gif "Germany"
                [flag_ES]: https://osu.ppy.sh/wiki/shared/flag/ES.gif "Spain"
                [flag_GB]: https://osu.ppy.sh/wiki/shared/flag/GB.gif "United Kingdom"
                [flag_HK]: https://osu.ppy.sh/wiki/shared/flag/HK.gif "Hong Kong"
                [flag_ID]: https://osu.ppy.sh/wiki/shared/flag/ID.gif "Indonesia"
                [flag_NL]: https://osu.ppy.sh/wiki/shared/flag/NL.gif "Netherlands"
                [flag_PL]: https://osu.ppy.sh/wiki/shared/flag/PL.gif "Poland"
                [flag_SE]: https://osu.ppy.sh/wiki/shared/flag/SE.gif "Sweden"
                [flag_US]: https://a.ppy.sh/3621552?1646452702.png "United States"
                """);
        String html = renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"


        try {
            Files.write(Path.of("D://html00.png"), bufferImg2byte(getBufferImage_swing(html)));
            Files.write(Path.of("D://html01.png"), bufferImg2byte(getBufferImage_html2img(html)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(html);
    }
    public static BufferedImage getBufferImage_swing(String html){
        JEditorPane editPane = new JEditorPane("text/html", html);
        editPane.setEditable(false);
        Dimension prefSize = editPane.getPreferredSize();
        BufferedImage img = new BufferedImage(prefSize.width,prefSize.height, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = img.getGraphics();
        editPane.setSize(prefSize);
        editPane.paint(graphics);
        graphics.dispose();
        return img;
    }

    public static BufferedImage getBufferImage_html2img(String html){
        HtmlImageGenerator imageGenerator = new HtmlImageGenerator();
        imageGenerator.loadHtml(html);
        return imageGenerator.getBufferedImage();
    }

    public static byte[] bufferImg2byte(BufferedImage img){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
            byte[] bytes = baos.toByteArray();
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    //    public static void main(String[] args) {
//        String text = """
//                name = "wiki"
//                size = 56
//                color=[255,255,255]
//
//                #注释 字体大小
//                [[text]]
//                color=[15,66,48] #没有就使用默认颜色
//                styel=1     # 0无 1加粗 2斜体 3加粗斜体
//                size=50     #此段大小 底端对齐 没有使用默认字号
//                text="321\\n"
//                [[text]]
//                text="第二段\\n"
//                """;
//
//
//        var mp = new TomlMapper();
//        try {
//            var o = mp.readValue(text, p.class);
//            System.out.println(o.name);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
//// markdown to image
//        MutableDataSet options = new MutableDataSet();
//        options.setFrom(ParserEmulationProfile.MARKDOWN);
//        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
//        Parser parser = Parser.builder(options).build();
//        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
//
//        Node document = parser.parse("""
//                # 类型
//
//                ## 值类型
//
//                **有符号** sbyte short int long float double ***十进制浮点数*** decimal
//
//                **无符号** byte ushort uint ulong char *char默认代表UTF-16*""");
//        String html = renderer.render(document);
//        System.out.println(html);
//    }
}

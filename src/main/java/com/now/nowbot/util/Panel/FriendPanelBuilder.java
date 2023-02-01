package com.now.nowbot.util.Panel;

import org.jetbrains.skija.*;

import java.util.LinkedList;

public class FriendPanelBuilder extends PanelBuilder{
    private static Paint c1 = new Paint().setARGB(255,56,46,50);
    private static Paint c2 = new Paint().setARGB(255,42,34,38);
    private static int MAX_LINE = 4;
    private LinkedList<Image> cards;
    private Image banner = null;
    private Surface outSurface;


    public FriendPanelBuilder() {
        super(1920, 330);
        drawName("Friend");
        cards = new LinkedList<>();
    }

    public FriendPanelBuilder drawBanner(Image banner){
        this.banner = banner;
        canvas.drawRRect(RRect.makeXYWH(0,0, 1920, 290,30,30, 30, 30), c2.setAlphaf(0.4f));
        return this;
    }

    public FriendPanelBuilder mainCard(Image image){
        canvas.drawImage(image, 40, 40);
        return this;
    }

    public FriendPanelBuilder addFriendCard(Image image){
        cards.push(image);
        return this;
    }

    public Image build() {
        if (isClose) return outImage;
        String text = "Friend";
        int r = 30;

        //当前指需要生成的行数
        int temp = 1+(cards.size()-1)/MAX_LINE;
        outSurface = Surface.makeRasterN32Premul(surface.getWidth(), surface.getHeight() + temp * 250);
        var canvas = outSurface.getCanvas();

        if (banner != null){
            canvas.drawImage(banner, 0, 0);
        }

        //此之后temp指当前card的下标,从0开始
        temp = 0;
        canvas.save();
        canvas.translate(0,330);
        canvas.drawRect(Rect.makeWH(1920,outSurface.getHeight()),c2);
        while (cards.size() > 0){
            var card = cards.poll();
            outSurface.getCanvas().drawImage(card, 40 + 470*(temp%MAX_LINE), 250*(temp/MAX_LINE));
            temp ++;
        }
        canvas.restore();
        canvas.drawImage(surface.makeImageSnapshot(),0,0);

        try (surface) {
            isClose = true;
            drawPanelInfo(outSurface, TOPTEXT_OFFSET, "FriendPanel v3.0");
            outImage = RRectout(outSurface, r);
            return outImage;
        }
    }
}

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
        cards = new LinkedList<>();
        canvas.drawRRect(RRect.makeXYWH(510,40, 195, 60, 15), c1);
        canvas.drawRRect(RRect.makeXYWH(0, 290, 1920, 40,30, 30, 0, 0), c2);
    }

    public FriendPanelBuilder drawBanner(Image banner){
        this.banner = banner;
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
            drawPanelInfo(outSurface, r, "FriendPanel");
            outImage = RRectout(outSurface, r);
            return outImage;
        }
    }
}

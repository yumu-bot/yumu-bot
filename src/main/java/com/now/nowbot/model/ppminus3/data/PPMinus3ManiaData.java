package com.now.nowbot.model.ppminus3.data;

import com.now.nowbot.model.ppminus3.PPMinus3;

public class PPMinus3ManiaData extends PPMinus3 {
    double stream = 0d;
    double jack = 0d;
    
    double release = 0d;
    double shield = 0d;
    double reverseShield = 0d;
    
    double bracket = 0d;
    double handLock = 0d;
    double overlap = 0d;
    
    double riceDensity = 0d;
    double lnDensity = 0d;
    
    double speedJack = 0d;
    double trill = 0d;
    double burst = 0d;
    
    double grace = 0d; 
    double delayedTail = 0d;

    double bump = 0d;
    double stop = 0d;
    double fastJam = 0d;
    double slowJam = 0d;
    double teleport = 0d;
    double negative = 0d;

    public PPMinus3ManiaData() {}
    
    public PPMinus3ManiaData clear() {
        stream = 0d;
        jack = 0d;

        release = 0d;
        shield = 0d;
        reverseShield = 0d;

        bracket = 0d;
        handLock = 0d;
        overlap = 0d;

        riceDensity = 0d;
        lnDensity = 0d;

        speedJack = 0d;
        trill = 0d;
        burst = 0d;

        grace = 0d;
        delayedTail = 0d;

        bump = 0d;
        stop = 0d;
        fastJam = 0d;
        slowJam = 0d;
        teleport = 0d;
        negative = 0d;
        return this;
    }

    public PPMinus3ManiaData add(PPMinus3ManiaData data) {
        this.stream += data.stream;
        this.jack += data.jack;
        
        this.release += data.release;
        this.shield += data.shield;
        this.reverseShield += data.reverseShield;
        
        this.bracket += data.bracket;
        this.handLock += data.handLock;
        this.overlap += data.overlap;
        
        this.riceDensity += data.riceDensity;
        this.lnDensity += data.lnDensity;
        
        this.speedJack += data.speedJack;
        this.trill += data.trill;
        this.burst = Math.max(this.burst, data.burst);
        
        this.grace += data.grace;
        this.delayedTail += data.delayedTail;
        
        this.bump += data.bump;
        this.stop += data.stop;
        this.fastJam += data.fastJam;
        this.slowJam += data.slowJam;
        this.teleport += data.teleport;
        this.negative += data.negative;
        
        return this;
    }

    public double getStream() {
        return stream;
    }

    public void setStream(double stream) {
        this.stream = stream;
    }

    public double getJack() {
        return jack;
    }

    public void setJack(double jack) {
        this.jack = jack;
    }

    public double getRelease() {
        return release;
    }

    public void setRelease(double release) {
        this.release = release;
    }

    public double getShield() {
        return shield;
    }

    public void setShield(double shield) {
        this.shield = shield;
    }

    public double getReverseShield() {
        return reverseShield;
    }

    public void setReverseShield(double reverseShield) {
        this.reverseShield = reverseShield;
    }

    public double getBracket() {
        return bracket;
    }

    public void setBracket(double bracket) {
        this.bracket = bracket;
    }

    public double getHandLock() {
        return handLock;
    }

    public void setHandLock(double handLock) {
        this.handLock = handLock;
    }

    public double getOverlap() {
        return overlap;
    }

    public void setOverlap(double overlap) {
        this.overlap = overlap;
    }

    public double getRiceDensity() {
        return riceDensity;
    }

    public void setRiceDensity(double riceDensity) {
        this.riceDensity = riceDensity;
    }

    public double getLnDensity() {
        return lnDensity;
    }

    public void setLnDensity(double lnDensity) {
        this.lnDensity = lnDensity;
    }

    public double getSpeedJack() {
        return speedJack;
    }

    public void setSpeedJack(double speedJack) {
        this.speedJack = speedJack;
    }

    public double getTrill() {
        return trill;
    }

    public void setTrill(double trill) {
        this.trill = trill;
    }

    public double getBurst() {
        return burst;
    }

    public void setBurst(double burst) {
        this.burst = burst;
    }

    public void increaseBurst() {
        this.burst += 1;
    }

    public double getGrace() {
        return grace;
    }

    public void setGrace(double grace) {
        this.grace = grace;
    }

    public double getDelayedTail() {
        return delayedTail;
    }

    public void setDelayedTail(double delayedTail) {
        this.delayedTail = delayedTail;
    }

    public double getBump() {
        return bump;
    }

    public void setBump(double bump) {
        this.bump = bump;
    }

    public double getStop() {
        return stop;
    }

    public void setStop(double stop) {
        this.stop = stop;
    }

    public double getFastJam() {
        return fastJam;
    }

    public void setFastJam(double fastJam) {
        this.fastJam = fastJam;
    }

    public double getSlowJam() {
        return slowJam;
    }

    public void setSlowJam(double slowJam) {
        this.slowJam = slowJam;
    }

    public double getTeleport() {
        return teleport;
    }

    public void setTeleport(double teleport) {
        this.teleport = teleport;
    }

    public double getNegative() {
        return negative;
    }

    public void setNegative(double negative) {
        this.negative = negative;
    }
}

package org.beurer.bf990;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.DateTimeException;
import java.time.LocalDateTime;

/** Decoder for the BF990 layouts verified against the scale display. */
final class Measurements {
    static final class Reading {
        final LocalDateTime time;
        final int user;
        final double weight, bmi;
        Double fat, water, muscle;
        final byte[] weightRaw;
        byte[] bodyRaw;
        Reading(LocalDateTime time, int user, double weight, double bmi, byte[] raw) {
            this.time=time; this.user=user; this.weight=weight; this.bmi=bmi; this.weightRaw=raw.clone();
        }
    }
    static int u16(byte[] b,int pos) { return (b[pos]&255)|((b[pos+1]&255)<<8); }
    static Reading weight(byte[] b) {
        if (b.length!=15 || (b[0]&255)!=0x0e) throw new IllegalArgumentException("Unsupported weight packet: expected 15 bytes and flags 0x0e");
        try {
            LocalDateTime t=LocalDateTime.of(u16(b,3),b[5]&255,b[6]&255,b[7]&255,b[8]&255,b[9]&255);
            return new Reading(t,b[10]&255,u16(b,1)*0.005,u16(b,11)*0.1,b);
        } catch (DateTimeException e) { throw new IllegalArgumentException("Weight packet contains an invalid timestamp",e); }
    }
    static void body(Reading r,byte[] b) {
        if (b.length!=14 || u16(b,0)!=0x0398) throw new IllegalArgumentException("Unsupported body composition packet: expected 14 bytes and flags 0x0398");
        r.bodyRaw=b.clone(); boolean empty=true; for(int i=2;i<b.length;i++) if(b[i]!=0) empty=false;
        if (!empty) { r.fat=u16(b,2)*0.1; r.muscle=u16(b,6)*0.1; if(r.weight>0) r.water=u16(b,10)*0.005/r.weight*100; }
    }
}

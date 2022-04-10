package osmreader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestZCurveIDStrategy {
    private static ZCurveIDStrategy idStrategy;
    @BeforeAll
    public static void setup() {
        idStrategy = new ZCurveIDStrategy();
    }

    @Test
    public void TestBitInterleave00001111() {
        String lat = Float.toString(Float.intBitsToFloat(Integer.parseInt("0000", 2)));
        String lon = Float.toString(Float.intBitsToFloat(Integer.parseInt("1111", 2)));
        String bits = Long.toBinaryString(idStrategy.interleave(Float.floatToRawIntBits(Float.parseFloat(lat)), Float.floatToRawIntBits(Float.parseFloat(lon))));
        Assertions.assertEquals("10101010", bits);
    }
    @Test
    public void TestBitInterleave11110000() {
        String lat = Float.toString(Float.intBitsToFloat(Integer.parseInt("1111", 2)));
        String lon = Float.toString(Float.intBitsToFloat(Integer.parseInt("0000", 2)));
        String bits = Long.toBinaryString(idStrategy.interleave(Float.floatToRawIntBits(Float.parseFloat(lat)), Float.floatToRawIntBits(Float.parseFloat(lon))));
        Assertions.assertEquals("1010101", bits);
    }
    @Test
    public void TestBitInterleave11111111() {
        String s1111 = Float.toString(Float.intBitsToFloat(Integer.parseInt("1111", 2)));
        String bits = Long.toBinaryString(idStrategy.interleave(Float.floatToRawIntBits(Float.parseFloat(s1111)), Float.floatToRawIntBits(Float.parseFloat(s1111))));
        Assertions.assertEquals("11111111", bits);
    }
    @Test
    public void TestBitInterleave00000000() {
        String s1111 = Float.toString(Float.intBitsToFloat(Integer.parseInt("0000", 2)));
        String bits = Long.toBinaryString(idStrategy.interleave(Float.floatToRawIntBits(Float.parseFloat(s1111)), Float.floatToRawIntBits(Float.parseFloat(s1111))));
        Assertions.assertEquals("0", bits);
    }
}

package me.relex.widget.waveform;

public class WaveUtil {

    //数据转毫秒
    public static long dataPixelsToTime(int dataPixels, int sampleRate, int samplesPerPixel) {
        return (long) dataPixels * samplesPerPixel / sampleRate;
    }

    //毫秒转像素
    public static int timeToPixels(long milliseconds, int sampleRate, int samplesPerPixel, float scale) {
        return (int) (milliseconds * sampleRate / samplesPerPixel * scale);
    }

    //像素转毫秒
    public static long pixelsToTime(float pixels, int sampleRate, int samplesPerPixel, float scale) {
        return (long) (pixels * samplesPerPixel / (sampleRate * scale));
    }

    // value 向下取整, 并且是 multiple 的倍数
    public static int roundDownToNearest(double value, int multiple) {
        if (multiple == 0) {
            return 0;
        }
        return multiple * (int) (value / multiple);
    }

    // value 向上取整, 并且是 multiple 的倍数
    // e.g: roundUpToNearest(5.5, 3) returns 6 （3的倍数）
    //      roundUpToNearest(141.0, 10) returns 150 （10的倍数）
    //      roundUpToNearest(-5.5, 3) returns -6
    public static int roundUpToNearest(double value, int multiple) {
        if (multiple == 0) {
            return 0;
        }

        int multiplier = 1;
        if (value < 0.0) {
            multiplier = -1;
            value = -value;
        }
        int rounded_up = (int) (Math.ceil(value));
        return multiplier * ((rounded_up + multiple - 1) / multiple) * multiple;
    }


}

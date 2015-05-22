package ch.ethz.smartheating;

import android.graphics.Color;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by schmisam on 21/05/15.
 */
public class Utility {

    public static int getColorForTemperature (double temp)
    {
        double vmin = 12.0;
        double vmax = 25.0;
        Double r = 1.0, g = 1.0, b = 1.0;
        double dv;

        if (temp < vmin)
            temp = vmin;
        if (temp > vmax)
            temp = vmax;
        dv = vmax - vmin;

        if (temp < (vmin + 0.25 * dv)) {
            r = 0d;
            g = 4 * (temp - vmin) / dv;
        } else if (temp < (vmin + 0.5 * dv)) {
            r = 0d;
            b = 1 + 4 * (vmin + 0.25 * dv - temp) / dv;
        } else if (temp < (vmin + 0.75 * dv)) {
            r = 4 * (temp - vmin - 0.5 * dv) / dv;
            b = 0d;
        } else {
            g = 1 + 4 * (vmin + 0.75 * dv - temp) / dv;
            b = 0d;
        }

        r *= 255;
        g *= 255;
        b *= 255;

        return Color.rgb(r.intValue(), g.intValue(), b.intValue());
    }
}

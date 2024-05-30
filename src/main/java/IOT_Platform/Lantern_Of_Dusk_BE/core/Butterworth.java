package IOT_Platform.Lantern_Of_Dusk_BE.core;

import org.apache.commons.math3.complex.Complex;

public class Butterworth {
    public static double[][] designHighPassFilter(int order, double cutoffFreq, double sampleRate) {
        double[] a = new double[order + 1];
        double[] b = new double[order + 1];

        // Normalize cutoff frequency
        double wc = Math.tan(Math.PI * cutoffFreq / sampleRate);

        // Poles and zeros
        Complex[] poles = new Complex[order];
        for (int k = 0; k < order; k++) {
            double theta = (2 * k + 1) * Math.PI / (2 * order);
            poles[k] = new Complex(-Math.sin(theta), Math.cos(theta));
        }

        // Gain calculation
        double gain = 1;
        for (Complex pole : poles) {
            gain *= -pole.getReal();
        }
        gain = Math.pow(wc, order) / gain;

        // Coefficients calculation
        for (int i = 0; i <= order; i++) {
            a[i] = 0;
            b[i] = 0;
        }
        b[0] = gain;
        for (int k = 0; k < order; k++) {
            Complex pole = poles[k];
            for (int j = order; j > 0; j--) {
                a[j] += a[j - 1] * 2 * pole.getReal();
                b[j] += b[j - 1] * 2 * pole.getReal();
            }
            for (int j = order; j > 1; j--) {
                a[j] += a[j - 2];
                b[j] += b[j - 2];
            }
            a[1] += 1;
            b[1] += 1;
        }

        // Denominator coefficients adjustment
        for (int i = 0; i <= order; i++) {
            a[i] = (i % 2 == 0) ? a[i] : -a[i];
        }

        return new double[][]{b, a};
    }
}

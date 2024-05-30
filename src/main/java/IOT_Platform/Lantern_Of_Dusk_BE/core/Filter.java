import IOT_Platform.Lantern_Of_Dusk_BE.core.Butterworth;

public class Filter {
    private double[] a;
    private double[] b;

    public Filter(double[] b, double[] a) {
        this.a = a;
        this.b = b;
    }

    public double[] filtfilt(double[] x) {
        int len = x.length;
        int na = a.length;
        int nb = b.length;

        // 신호의 시작과 끝을 반사하여 패딩
        double[] padSignal = new double[2 * len];
        System.arraycopy(x, 0, padSignal, len, len);
        for (int i = 0; i < len; i++) {
            padSignal[i] = 2 * x[0] - x[len - 1 - i];
            padSignal[2 * len - 1 - i] = 2 * x[len - 1] - x[i];
        }

        // 앞으로 필터링
        double[] forwardFiltered = lfilter(b, a, padSignal);

        // 신호를 반대로 뒤집기
        double[] reversedSignal = new double[forwardFiltered.length];
        for (int i = 0; i < forwardFiltered.length; i++) {
            reversedSignal[i] = forwardFiltered[forwardFiltered.length - 1 - i];
        }

        // 뒤로 필터링
        double[] backwardFiltered = lfilter(b, a, reversedSignal);

        // 신호를 다시 원래 순서로 뒤집기
        double[] result = new double[len];
        for (int i = 0; i < len; i++) {
            result[i] = backwardFiltered[backwardFiltered.length - 1 - len + i];
        }

        return result;
    }

    private double[] lfilter(double[] b, double[] a, double[] x) {
        int na = a.length;
        int nb = b.length;
        int len = x.length;

        double[] y = new double[len];
        double[] z = new double[Math.max(na, nb)];

        for (int i = 0; i < len; i++) {
            y[i] = b[0] * x[i] + z[0];
            for (int j = 1; j < nb; j++) {
                if (i - j >= 0) {
                    y[i] += b[j] * x[i - j];
                }
                z[j - 1] = z[j] + (i - j >= 0 ? b[j] * x[i - j] : 0) - a[j] * y[i];
            }
            for (int j = nb; j < na; j++) {
                z[j - 1] = z[j] - a[j] * y[i];
            }
            z[na - 1] = 0;
        }
        return y;
    }

    public static void main(String[] args) {
        double samplePeriod = 0.01; // 샘플링 주기
        double filtCutOff = 0.1; // 컷오프 주파수
        int order = 1; // 필터 차수

        // 샘플 데이터
        double[] linVel = { /* 신호 데이터 */ };

        // Butterworth 필터 설계
        double[][] ba = Butterworth.designHighPassFilter(order, filtCutOff, 1 / samplePeriod);
        double[] b = ba[0];
        double[] a = ba[1];

        // 필터 적용
        Filter filter = new Filter(b, a);
        double[] linVelHP = filter.filtfilt(linVel);

        // 필터링된 신호 사용
    }
}

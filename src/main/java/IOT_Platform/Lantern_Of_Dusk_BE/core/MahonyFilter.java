package IOT_Platform.Lantern_Of_Dusk_BE.core;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class MahonyFilter {

    private double samplePeriod;
    private RealVector quaternion;
    private double kp;
    private double ki;
    private RealVector eInt;

    public MahonyFilter(double samplePeriod, double[] quaternion, double kp, double ki) {
        this.samplePeriod = samplePeriod;
        this.quaternion = new ArrayRealVector(quaternion);
        this.kp = kp;
        this.ki = ki;
        this.eInt = new ArrayRealVector(new double[]{0, 0, 0});
    }

    private RealVector quaternProd(RealVector a, RealVector b) {
        double w1 = a.getEntry(0);
        double x1 = a.getEntry(1);
        double y1 = a.getEntry(2);
        double z1 = a.getEntry(3);

        double w2 = b.getEntry(0);
        double x2 = b.getEntry(1);
        double y2 = b.getEntry(2);
        double z2 = b.getEntry(3);

        return new ArrayRealVector(new double[]{
                w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2,
                w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2,
                w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2,
                w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2
        });
    }

    private RealVector quaternConj(RealVector q) {
        return new ArrayRealVector(new double[]{
                q.getEntry(0), -q.getEntry(1), -q.getEntry(2), -q.getEntry(3)
        });
    }

    public void updateIMU(double[] gyroscope, double[] accelerometer) {
        RealVector q = this.quaternion;

        RealVector acc = new ArrayRealVector(accelerometer);
        if (acc.getNorm() == 0) return;
        acc = acc.mapDivide(acc.getNorm());

        RealVector v = new ArrayRealVector(new double[]{
                2 * (q.getEntry(1) * q.getEntry(3) - q.getEntry(0) * q.getEntry(2)),
                2 * (q.getEntry(0) * q.getEntry(1) + q.getEntry(2) * q.getEntry(3)),
                q.getEntry(0) * q.getEntry(0) - q.getEntry(1) * q.getEntry(1) - q.getEntry(2) * q.getEntry(2) + q.getEntry(3) * q.getEntry(3)
        });

        RealVector e = crossProduct(acc, v);

        if (ki > 0) {
            eInt = eInt.add(e.mapMultiply(samplePeriod));
        } else {
            eInt = new ArrayRealVector(new double[]{0, 0, 0});
        }

        // qDot 계산을 위한 배열 준비
        double[] gyroArray = {0, gyroscope[0], gyroscope[1], gyroscope[2]};
        RealVector gyroVector = new ArrayRealVector(gyroArray);

        // Quaternion 곱셈을 위한 준비
        RealVector qDot2 = quaternProd(q, gyroVector).mapMultiply(0.5);

        // 위 코드에서, gyroscope 배열을 직접 RealVector 생성자에 넣어 0을 추가하였습니다.


        q = q.add(qDot2.mapMultiply(samplePeriod));
        quaternion = q.mapDivide(q.getNorm());
    }

    private RealVector crossProduct(RealVector u, RealVector v) {
        double u1 = u.getEntry(0);
        double u2 = u.getEntry(1);
        double u3 = u.getEntry(2);
        double v1 = v.getEntry(0);
        double v2 = v.getEntry(1);
        double v3 = v.getEntry(2);

        return new ArrayRealVector(new double[]{
                u2 * v3 - u3 * v2,
                u3 * v1 - u1 * v3,
                u1 * v2 - u2 * v1
        });
    }
}


package IOT_Platform.Lantern_Of_Dusk_BE.core;

public class MahonyAHRS {

    private double samplePeriod;
    private double[] quaternion;
    private double kp;
    private double ki;
    private double[] eInt;

    public MahonyAHRS(double samplePeriod) {
        this.samplePeriod = samplePeriod;
        this.quaternion = new double[]{1, 0, 0, 0}; // Quaternion initialization
        this.kp = 1;
        this.ki = 0;
        this.eInt = new double[]{0, 0, 0};
    }

    public void updateIMU(double[] gyroscope, double[] accelerometer) {
        if (norm(accelerometer) == 0) return;
        accelerometer = normalize(accelerometer);

        double[] v = {
                2 * (quaternion[1] * quaternion[3] - quaternion[0] * quaternion[2]),
                2 * (quaternion[0] * quaternion[1] + quaternion[2] * quaternion[3]),
                quaternion[0] * quaternion[0] - quaternion[1] * quaternion[1] - quaternion[2] * quaternion[2] + quaternion[3] * quaternion[3]
        };

        double[] e = crossProduct(accelerometer, v);

        if (ki > 0) {
            eInt[0] += e[0] * samplePeriod;
            eInt[1] += e[1] * samplePeriod;
            eInt[2] += e[2] * samplePeriod;
        } else {
            eInt = new double[]{0, 0, 0};
        }

        gyroscope[0] += kp * e[0] + ki * eInt[0];
        gyroscope[1] += kp * e[1] + ki * eInt[1];
        gyroscope[2] += kp * e[2] + ki * eInt[2];

        double[] qDot = quaternProd(quaternion, new double[]{0, gyroscope[0], gyroscope[1], gyroscope[2]});
        qDot[0] *= 0.5 * samplePeriod;
        qDot[1] *= 0.5 * samplePeriod;
        qDot[2] *= 0.5 * samplePeriod;
        qDot[3] *= 0.5 * samplePeriod;

        quaternion[0] += qDot[0];
        quaternion[1] += qDot[1];
        quaternion[2] += qDot[2];
        quaternion[3] += qDot[3];

        quaternion = normalize(quaternion); // Keep quaternion array size 4
    }

    public double[] getQuaternion() {
        return quaternion; // Return quaternion array
    }

    private double norm(double[] vec) {
        return Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);
    }

    private double[] normalize(double[] vec) {
        double norm = norm(vec);
        if (vec.length == 3) {
            return new double[]{vec[0] / norm, vec[1] / norm, vec[2] / norm};
        } else {
            return new double[]{vec[0] / norm, vec[1] / norm, vec[2] / norm, vec[3] / norm}; // Keep quaternion array size 4
        }
    }

    private double[] quaternProd(double[] a, double[] b) {
        return new double[]{
                a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3],
                a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2],
                a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1],
                a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0]
        };
    }

    private double[] crossProduct(double[] a, double[] b) {
        return new double[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
        };
    }
}

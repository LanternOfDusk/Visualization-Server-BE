package IOT_Platform.Lantern_Of_Dusk_BE.core;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class MahonyAHRS {
    private double SamplePeriod;
    private Quaternion quaternion;
    private double Kp;
    private double Ki;
    private Vector3D eInt;

    public MahonyAHRS(double SamplePeriod) {
        this.SamplePeriod = SamplePeriod;
        this.quaternion = new Quaternion(1, 0, 0, 0);
        this.Kp = 1.0;
        this.Ki = 0.0;
        this.eInt = new Vector3D(0, 0, 0);
    }

    private Quaternion quaternProd(Quaternion a, Quaternion b) {
        return a.multiply(b);
    }
    private Vector3D normalize(Vector3D v) {
        return v.normalize();
    }

    public void updateIMU(Vector3D gyro, Vector3D accel) {
        Quaternion q = quaternion;

        if (accel.getNorm() == 0) return;

        accel = normalize(accel);

        Vector3D v = new Vector3D(
                2 * (q.getQ1() * q.getQ3() - q.getQ0() * q.getQ2()),
                2 * (q.getQ0() * q.getQ1() + q.getQ2() * q.getQ3()),
                q.getQ0() * q.getQ0() - q.getQ1() * q.getQ1() - q.getQ2() * q.getQ2() + q.getQ3() * q.getQ3()
        );

        Vector3D e = accel.crossProduct(v);

        if (Ki > 0) {
            eInt = eInt.add(e.scalarMultiply(SamplePeriod));
        } else {
            eInt = new Vector3D(0, 0, 0);
        }

        gyro = gyro.add(e.scalarMultiply(Kp)).add(eInt.scalarMultiply(Ki));

        Quaternion qDot = quaternProd(q, new Quaternion(0, gyro.toArray())).multiply(0.5);

        q = q.add(qDot.multiply(SamplePeriod));
        quaternion = q.normalize();
    }

    public double[] getQuaternion() {
        return quaternion.getVectorPart();
    }
}

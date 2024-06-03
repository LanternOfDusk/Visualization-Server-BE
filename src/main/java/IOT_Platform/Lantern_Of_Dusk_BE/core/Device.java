package IOT_Platform.Lantern_Of_Dusk_BE.core;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Position;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.PositionRepository;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Device {

    private int id;
    private String ae;

    private Queue<double[][]> accelQueue = new LinkedList<>();
    private Queue<double[][]> gyroQueue = new LinkedList<>();
    private Queue<double[]> atmQueue = new LinkedList<>();

    private Queue<Position> positionQueue = new LinkedList<>();

    private double samplePeriod = 5.0 / 1000.0;
    private MahonyAHRS mahonyAHRS = new MahonyAHRS(samplePeriod);
    private double[] currentPosition = new double[3];
    private double[] currentRotation = new double[3];

    private final PositionRepository positionRepository;

    public Device(PositionRepository positionRepository, int id, String ae) {
        this.id = id;
        this.ae = ae;
        this.positionRepository = positionRepository;
    }

    public void run() {
        getData();
        processData();
        save();
    }

    public void getData() {
        String host = "http://203.253.128.177:7579/Mobius/";

        String urlIMU = host + ae + "/" + "MPU?fu=2&la=200&ty=4&rcn=4";
        String urlATM = host + ae + "/" + "ATM?fu=2&la=200&ty=4&rcn=4";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("X-M2M-RI", "12345");
        headers.set("X-M2M-Origin", "SOrigin");
        HttpEntity request = new HttpEntity(headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> responseIMU = restTemplate.exchange(
                    urlIMU,
                    HttpMethod.GET,
                    request,
                    String.class
            );
            ResponseEntity<String> responseATM = restTemplate.exchange(
                    urlATM,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            JSONArray rawIMUDataList = new JSONObject(responseIMU.getBody()).getJSONObject("m2m:rsp").getJSONArray("m2m:cin");
            JSONArray rawATMDataList = new JSONObject(responseATM.getBody()).getJSONObject("m2m:rsp").getJSONArray("m2m:cin");

            double[][] rawAccelData = new double[100][3];
            double[][] rawGyroData = new double[100][3];
            double[] rawATMData = new double[100];

            for (int i = 0; i < 4; i++) {

                JSONObject objIMU = ((JSONObject) rawIMUDataList.get(i)).getJSONObject("con");
                JSONObject objATM = ((JSONObject) rawATMDataList.get(i)).getJSONObject("con");

                rawAccelData[i][0] = objIMU.getDouble("ax");
                rawAccelData[i][1] = objIMU.getDouble("ay");
                rawAccelData[i][2] = objIMU.getDouble("az");


                rawGyroData[i][0] = objIMU.getDouble("gx");
                rawGyroData[i][1] = objIMU.getDouble("gy");
                rawGyroData[i][2] = objIMU.getDouble("gz");

                rawATMData[i] = objATM.getDouble("atm");

                System.out.println("GetData" + i + " " + objIMU);
            }

            accelQueue.add(rawAccelData);
            gyroQueue.add(rawGyroData);
            atmQueue.add(rawATMData);

        } catch (RestClientException e) {
            System.out.println("Error fetching data for " + ae + " from Mobius");
            System.out.println(e);
        }
    }

    public void processData() {
        double[][] accelData = accelQueue.remove();
        double[][] gyroData = gyroQueue.remove();
        int len = accelData.length;

        for(int i = 0; i < len; i++) {
            System.out.println("Accel" + i + " : " + Arrays.toString(accelData[i]));
        }
        for(int i = 0; i < len; i++) {
            System.out.println("Gyro" + i + " : " + Arrays.toString(gyroData[i]));
        }
        System.out.println();


        // 회전 벡터 마호니 필터 및 계산
        RealMatrix R = new Array2DRowRealMatrix(3, 3 * len);
        for (int i = 0; i < gyroData.length; i++) {
            mahonyAHRS.updateIMU(gyroData[i], accelData[i]);
            double[] q = mahonyAHRS.getQuaternion();
            if (q.length != 4) {
                throw new IllegalArgumentException("Quaternion array must have exactly 4 elements. Found: " + q.length);
            }
            double[][] rotMat = quatern2rotMat(q);
            R.setSubMatrix(rotMat, 0, i * 3);

            System.out.println("R" + i + " : " + Arrays.deepToString(R.getColumnMatrix(i).getData()));
        }

        System.out.println();

        // 회전 적용된 가속도 벡터 계산
        RealMatrix tcAcc = new Array2DRowRealMatrix(len, 3);
        for (int i = 0; i < len; i++) {
            RealMatrix accVec = new Array2DRowRealMatrix(new double[][]{accelData[i]}).transpose();
            RealMatrix tcAccVec = R.getSubMatrix(0, 2, i * 3, i * 3 + 2).multiply(accVec);
            tcAcc.setRowMatrix(i, tcAccVec.transpose());
        }

        // 지구 프레임에서의 선형 가속도 계산 및 단위 변환
        RealMatrix earthMetrix = MatrixUtils.createRealMatrix(new double[len][3]);
        for (int i = 0; i < len; i++) {
            earthMetrix.setRow(i, new double[] {0, 0, 1});
        }
        RealMatrix linAcc = tcAcc.subtract(earthMetrix);
        linAcc = linAcc.scalarMultiply(9.81);

        for (int i = 0; i < len; i++) {
            System.out.println("linAcc " + i + " : " + Arrays.toString(linAcc.getRow(i)));
        }
        System.out.println();

        RealMatrix linVel = new Array2DRowRealMatrix(len, 3);
        for (int i = 1; i < linAcc.getRowDimension(); i++) {
            linVel.setRowVector(i, linVel.getRowVector(i - 1).add(linAcc.getRowVector(i).mapMultiply(samplePeriod)));
        }

        for (int i = 0; i < len; i++) {
            System.out.println("linVel " + i + " : " + Arrays.toString(linVel.getRow(i)));
        }
        System.out.println();

        RealMatrix linPos = new Array2DRowRealMatrix(len, 3);
        for (int i = 1; i < linVel.getRowDimension(); i++) {
            linPos.setRowVector(i, linPos.getRowVector(i - 1).add(linVel.getRowVector(i).mapMultiply(samplePeriod)));
        }

        for (int i = 0; i < len; i++) {
            System.out.println("linPos " + i + " : " + Arrays.toString(linPos.getRow(i)));
        }
        System.out.println();


        // 방향 벡터를 오일러 각도로 변환
        double[] eulerR = rotationMatrixToEulerAngles(R.getSubMatrix(0, 2, (len-1) * 3, (len-1) * 3 + 2));
        System.out.println(Arrays.toString(eulerR));

        for (int i = 0; i < 3; i++) {
            currentRotation[i] += linPos.getRow(len-1)[i];
        }

        System.out.println(Arrays.toString(currentPosition));

        Position position = new Position();
        position.setDeviceId(id);
        position.setX(currentPosition[0]);
        position.setY(currentPosition[1]);
        position.setZ(currentPosition[2]);
        position.setPitch(eulerR[0]);
        position.setYaw(eulerR[1]);
        position.setRoll(eulerR[2]);

        positionQueue.add(position);
    }

    public double[] rotationMatrixToEulerAngles(RealMatrix R) {
        double[] eulerAngles = new double[3];

        // Check for gimbal lock
        if (R.getEntry(2, 0) < 1) {
            if (R.getEntry(2, 0) > -1) {
                eulerAngles[1] = Math.asin(-R.getEntry(2, 0)); // pitch
                eulerAngles[0] = Math.atan2(R.getEntry(2, 1), R.getEntry(2, 2)); // roll
                eulerAngles[2] = Math.atan2(R.getEntry(1, 0), R.getEntry(0, 0)); // yaw
            } else {
                // gimbal lock, pitch = +pi/2
                eulerAngles[1] = Math.PI / 2;
                eulerAngles[0] = -Math.atan2(-R.getEntry(1, 2), R.getEntry(1, 1)); // roll
                eulerAngles[2] = 0; // yaw
            }
        } else {
            // gimbal lock, pitch = -pi/2
            eulerAngles[1] = -Math.PI / 2;
            eulerAngles[0] = Math.atan2(-R.getEntry(1, 2), R.getEntry(1, 1)); // roll
            eulerAngles[2] = 0; // yaw
        }

        return eulerAngles;
    }
    public double[][] quatern2rotMat(double[] q) {
        if (q.length != 4) {
            throw new IllegalArgumentException("Quaternion array must have exactly 4 elements.");
        }
        double[][] R = new double[3][3];

        R[0][0] = 2 * q[0] * q[0] - 1 + 2 * q[1] * q[1];
        R[0][1] = 2 * (q[1] * q[2] + q[0] * q[3]);
        R[0][2] = 2 * (q[1] * q[3] - q[0] * q[2]);
        R[1][0] = 2 * (q[1] * q[2] - q[0] * q[3]);
        R[1][1] = 2 * q[0] * q[0] - 1 + 2 * q[2] * q[2];
        R[1][2] = 2 * (q[2] * q[3] + q[0] * q[1]);
        R[2][0] = 2 * (q[1] * q[3] + q[0] * q[2]);
        R[2][1] = 2 * (q[2] * q[3] - q[0] * q[1]);
        R[2][2] = 2 * q[0] * q[0] - 1 + 2 * q[3] * q[3];
        return R;
    }

    public void save() {
        positionRepository.save(positionQueue.remove());
    }

    public void start() {
        Runnable task = () -> {
            run();
        };
        Executors
                .newScheduledThreadPool(1)
                .scheduleAtFixedRate(task, 0, 1000, TimeUnit.MILLISECONDS);
        //run();
    }
}

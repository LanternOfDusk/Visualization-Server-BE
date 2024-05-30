package IOT_Platform.Lantern_Of_Dusk_BE.core;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Position;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.PositionRepository;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Device {

    private int id;
    private String ae;

    private Queue<double[][]> accelQueue = new LinkedList<>();
    private Queue<double[][]> gyroQueue = new LinkedList<>();
    private Queue<double[]> atmQueue = new LinkedList<>();

    private Queue<Position> positionQueue = new LinkedList<>();

    private double samplePeriod = 5 / 1000;
    private MahonyAHRS mahonyAHRS = new MahonyAHRS(samplePeriod);

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

        String urlIMU = host + ae + "/" + "IMU" + "?fu=2&la=4&ty=4&rcn=4";
        String urlATM = host + ae + "/" + "ATM" + "?fu=2&la=4&ty=4&rcn=4";

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

            double[][] rawAccelData = new double[4][3];
            double[][] rawGyroData = new double[4][3];
            double[] rawATMData = new double[4];

            for (int i = 0; i < 4; i++) {
                JSONObject objIMU = ((JSONObject) rawIMUDataList.get(i)).getJSONObject("con");
                JSONObject objATM = ((JSONObject) rawATMDataList.get(i)).getJSONObject("con");

                rawAccelData[i][0] = (Double) objIMU.get("ax");
                rawAccelData[i][1] = (Double) objIMU.get("ay");
                rawAccelData[i][2] = (Double) objIMU.get("az");

                rawGyroData[i][0] = (Double) objIMU.get("gx");
                rawGyroData[i][1] = (Double) objIMU.get("gy");
                rawGyroData[i][2] = (Double) objIMU.get("gz");

                rawATMData[i] = (Double) objATM.get("atm");
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
        double[][] accelData = accelQueue.peek();
        double[][] gyroData = gyroQueue.peek();

        int len = accelData.length;

        // 회전 벡터 마호니 필터 및 계산
        RealMatrix[] R = new RealMatrix[len];
        for (int i = 0; i < len; i++) {
            mahonyAHRS.updateIMU(new Vector3D(gyroData[i]), new Vector3D(accelData[i]));
            R[i] = new Array2DRowRealMatrix(mahonyAHRS.getQuaternion());
        }

        // 회전 적용된 가속도 벡터 계산
        double[][] tcAcc = new double[len][3];
        for (int i = 0; i < len; i++) {
            tcAcc[i] = R[i].preMultiply(accelData[i]);
        }

        // 중력 가속도를 제거한 가속도 벡터 계산
        double[][] linAcc = new double[len][3];
        for (int i = 0; i < len; i++) {
            linAcc[i][0] = tcAcc[i][0] * 9.81;
            linAcc[i][1] = tcAcc[i][1] * 9.81;
            linAcc[i][2] = (tcAcc[i][2] - 1.0) * 9.81;
        }

        // 선형 속도 계산 (가속도 적분)
        double[][] linVel = new double[len][3];
        for (int i = 1; i < len; i++) {
            for (int j = 0; j < 3; j++) {
                linVel[i][j] = linVel[i - 1][j] + linAcc[i][j] * samplePeriod;
            }
        }

        // 드리프트를 제거하기 위해 고역통과 필터 적용
        //double[][] linVelHP = highPassFilter(linVel, samplePeriod, 0.1);
        double[][] linVelHP = linVel;

        // 선형 위치 계산 (속도 적분)
        double[][] linPos = new double[len][3];
        for (int i = 1; i < len; i++) {
            for (int j = 0; j < 3; j++) {
                linPos[i][j] = linPos[i - 1][j] + linVelHP[i][j] * samplePeriod;
            }
        }

        // 드리프트를 제거하기 위해 고역통과 필터 적용
        //double[][] linPosHP = highPassFilter(linPos, samplePeriod, 0.1);
        double[][] linPosHP = linPos;

        // 방향 벡터를 오일러 각도로 변환
        Rotation rotation = new Rotation((Vector3D) R[len-1], 1e-10);
        double[] eulerR = rotation.getAngles(RotationOrder.XYZ);

        Position position = new Position();
        position.setDeviceId(id);
        position.setX(linPosHP[len-1][0]);
        position.setY(linPosHP[len-1][1]);
        position.setZ(linPosHP[len-1][2]);
        position.setPitch(eulerR[0]);
        position.setYaw(eulerR[1]);
        position.setRoll(eulerR[2]);

        positionQueue.add(position);
    }

    public void save() {
        positionRepository.save(positionQueue.peek());
    }

    public void start() {
        System.out.println("start");

        Runnable task = () -> {
            run();
        };

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(task, 0, 20, TimeUnit.MILLISECONDS);
    }
}

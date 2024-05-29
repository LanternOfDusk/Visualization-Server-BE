package IOT_Platform.Lantern_Of_Dusk_BE.service;

import IOT_Platform.Lantern_Of_Dusk_BE.core.ExtendedKalmanFilter;
import IOT_Platform.Lantern_Of_Dusk_BE.entity.Connection;
import IOT_Platform.Lantern_Of_Dusk_BE.dto.RawDataDTO;
import IOT_Platform.Lantern_Of_Dusk_BE.entity.Position;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.ConnectionRepository;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.PositionRepository;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class DataService {

    private final RestTemplate restTemplate;

    private final ConnectionRepository connectionRepository;
    private final PositionRepository positionRepository;

    private Map<Integer, Connection> connections;
    private Map<Integer, ExtendedKalmanFilter> filters;

    @Autowired
    public DataService(RestTemplateBuilder restTemplateBuilder, ConnectionRepository connectionRepository, PositionRepository positionRepository) {

        this.restTemplate = restTemplateBuilder.build();

        this.connectionRepository = connectionRepository;
        this.positionRepository = positionRepository;

        this.connections = new HashMap<>();
        this.filters = new HashMap<>();

        setConnection();
    }

    public void setConnection() {
        for( Connection connection : connectionRepository.findAll()) {
            // 초기 상태 벡터 및 공분산 행렬 설정 (모든 값이 0인 상태로 초기화)
            // 상태 전이 행렬, 프로세스 노이즈, 측정 노이즈, 관측 행렬 설정 (임의의 예시 값, 실제 응용에 따라 조정 필요)
            RealMatrix initialState = MatrixUtils.createColumnRealMatrix(new double[]{0, 0, 0, 0, 0, 0}); // [x, y, z, roll, pitch, yaw]
            RealMatrix initialCovariance = MatrixUtils.createRealDiagonalMatrix(new double[]{1, 1, 1, 1, 1, 1});
            RealMatrix stateTransition = MatrixUtils.createRealIdentityMatrix(6);
            RealMatrix processNoise = MatrixUtils.createRealDiagonalMatrix(new double[]{0.1, 0.1, 0.1, 0.1, 0.1, 0.1});
            RealMatrix measurementNoise = MatrixUtils.createRealDiagonalMatrix(new double[]{1, 1, 1, 1, 1, 1});
            RealMatrix observationMatrix = MatrixUtils.createRealIdentityMatrix(6);
            ExtendedKalmanFilter filter = new ExtendedKalmanFilter(initialState, initialCovariance, stateTransition, processNoise, measurementNoise, observationMatrix);

            connections.put(connection.getId(), connection);
            filters.put(connection.getId(), filter);
        }
    }

    @Scheduled(fixedRate = 1000)
    public void processData() {
        for (Connection connection : connections.values()) {
            List<RawDataDTO> rawDataList = getRawData(connection.getAe());
            Position position = applyFilter(rawDataList, connection.getId());
            saveData(position);
        }
    }

    @Value("${mobius.url}")
    private String mobiusServerUrl;
    @Value("${mobius.cnt.imu}")
    private String mobiusImuCntName;
    @Value("${mobius.cnt.atm}")
    private String mobiusAtmCntName;
    public List<RawDataDTO> getRawData(String ae) {
        String urlIMU = mobiusServerUrl + ae + "/" + mobiusImuCntName + "?fu=2&la=143&ty=4&rcn=4";
        String urlATM = mobiusServerUrl + ae + "/" + mobiusAtmCntName + "?fu=2&la=143&ty=4&rcn=4";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("X-M2M-RI", "12345");
        headers.set("X-M2M-Origin", "SOrigin");
        HttpEntity request = new HttpEntity(headers);

        List<RawDataDTO> result = new ArrayList<>();

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

            JSONArray rawDataIMU = new JSONObject(responseIMU.getBody()).getJSONObject("m2m:rsp").getJSONArray("m2m:cin");
            JSONArray rawDataATM = new JSONObject(responseATM.getBody()).getJSONObject("m2m:rsp").getJSONArray("m2m:cin");

            for (int i = 0; i < 143; i++) {
                RawDataDTO rawDataDTO = new RawDataDTO();

                JSONObject objIMU = ((JSONObject) rawDataIMU.get(i)).getJSONObject("con");
                JSONObject objATM = ((JSONObject) rawDataATM.get(i)).getJSONObject("con");

                // TODO: 5/29/24 테스트 코드 입력 후 구현
                System.out.println(objATM.toString());
                System.out.println(objIMU.toString());

                rawDataDTO.setAx((Double) objIMU.get("ax"));
                rawDataDTO.setAy((Double) objIMU.get("ay"));
                rawDataDTO.setAz((Double) objIMU.get("az"));
                rawDataDTO.setGx((Double) objIMU.get("gx"));
                rawDataDTO.setGy((Double) objIMU.get("gy"));
                rawDataDTO.setGz((Double) objIMU.get("gz"));
                rawDataDTO.setAtm((Double) objATM.get("atm"));

                result.add(rawDataDTO);
            }

        } catch (RestClientException e) {
            System.out.println("Error fetching data for " + ae + " from Mobius");
            System.out.println(e);
        }

        return result;
    }

    public Position applyFilter(List<RawDataDTO> rawDataDTOList, int deviceId) {
        for (RawDataDTO rawDataDTO : rawDataDTOList) {
            // 측정값 벡터 생성
            RealMatrix measurement = MatrixUtils.createColumnRealMatrix(new double[]{
                    rawDataDTO.getAx(), rawDataDTO.getAy(), rawDataDTO.getAx(),
                    rawDataDTO.getGx(), rawDataDTO.getGy(), rawDataDTO.getGz()
            });

            // 예측 단계
            filters.get(deviceId).predict();

            // 업데이트 단계
            filters.get(deviceId).update(measurement);
        }

        // 필터링된 상태 벡터 가져오기
        RealMatrix state = filters.get(deviceId).getState();

        // 상태 벡터에서 위치 정보 추출
        double x = state.getEntry(0, 0);
        double y = state.getEntry(1, 0);
        double z = state.getEntry(2, 0);
        double roll = state.getEntry(3, 0);
        double pitch = state.getEntry(4, 0);
        double yaw = state.getEntry(5, 0);

        // 위치 정보 저장
        Position position = new Position();
        position.setDeviceId(deviceId);
        position.setX(x);
        position.setY(y);
        position.setZ(z);
        position.setRoll(roll);
        position.setPitch(pitch);
        position.setYaw(yaw);

        return position;
    }

    public void saveData(Position position) {
        positionRepository.save(position);
    }
}
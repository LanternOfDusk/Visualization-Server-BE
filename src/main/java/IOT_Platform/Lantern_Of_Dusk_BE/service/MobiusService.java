package IOT_Platform.Lantern_Of_Dusk_BE.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Connection;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.ConnectionRepository;

@Service
public class MobiusService {

    private final RestTemplate restTemplate;
    private final ConnectionRepository connectionRepository;

    @Value("${mobius.server.url}")
    private String mobiusServerUrl;

    private static final Logger logger = LoggerFactory.getLogger(MobiusService.class);

    @Autowired
    public MobiusService(RestTemplateBuilder restTemplateBuilder, ConnectionRepository connectionRepository) {
        this.restTemplate = restTemplateBuilder.build();
        this.connectionRepository = connectionRepository;
    }

    /**
     * 특정 AE에 해당하는 데이터를 Mobius로부터 가져오는 메서드입니다.
     * @param ae Application Entity의 이름
     * @return Mobius로부터 받은 데이터
     */
    public String fetchDataFromMobiusForAE(String ae) {
        // Connection 테이블에서 AE에 대한 정보를 가져옵니다.
        Optional<Connection> connection = connectionRepository.findByAe(ae);

        // Connection 정보가 없는 경우 예외 처리
        if (connection.isEmpty()) {
            throw new IllegalArgumentException("Connection not found for AE: " + ae);
        }

        // 각 센서에 대한 데이터를 가져옵니다.
        String accelerationData = fetchDataForSensor(connection.orElse(null), "ACCEL");
        String gyroscopeData = fetchDataForSensor(connection.orElse(null), "GYRO");
        String pressureData = fetchDataForSensor(connection.orElse(null), "PRESSURE");

        // 데이터 조합
        String combinedData = "ACCEL: " + accelerationData + "\n" +
                "GYRO: " + gyroscopeData + "\n" +
                "PRESSURE: " + pressureData;

        return combinedData;
    }


    public String fetchDataForSensor(Connection connection, String sensor) {
        // Mobius 서버로 요청할 URL 생성
        String url = mobiusServerUrl + connection.getName() + "/" + sensor;
        try {
            // Mobius 서버에 GET 요청을 보내 데이터를 받아옵니다.
            String responseData = restTemplate.getForObject(url, String.class);

            // 받아온 JSON 데이터를 파싱하여 컨테이너 값들을 추출합니다.
            Map<String, String> containerData = parseJsonData(responseData);

            // 센서에 해당하는 데이터 반환
            return containerData.toString();
        } catch (RestClientException e) {
            // 요청에 실패한 경우 에러를 로그에 기록하고 예외를 던집니다.
            logger.error("Error fetching data for AE {} from Mobius: ", connection.getAe(), e);
            throw new RuntimeException("Failed to fetch data from Mobius for AE: " + connection.getAe(), e);
        }
    }

    /**
     * JSON 데이터를 파싱하여 Map으로 변환하는 메서드입니다.
     * @param jsonData JSON 형식의 데이터
     * @return 파싱된 데이터가 담긴 Map 객체
     */
    private Map<String, String> parseJsonData(String jsonData) {
        try {
            // JSON 데이터를 파싱하는 ObjectMapper 객체 생성
            ObjectMapper objectMapper = new ObjectMapper();
            // JSON 데이터를 JsonNode 객체로 읽어옵니다.
            JsonNode rootNode = objectMapper.readTree(jsonData);

            // 각 센서의 값을 추출하여 Map에 저장합니다.
            Map<String, String> containerData = new HashMap<>();
            containerData.put("acceleration_x", rootNode.get("acceleration").get("x").asText());
            containerData.put("acceleration_y", rootNode.get("acceleration").get("y").asText());
            containerData.put("acceleration_z", rootNode.get("acceleration").get("z").asText());

            containerData.put("gyroscope_x", rootNode.get("gyroscope").get("x").asText());
            containerData.put("gyroscope_y", rootNode.get("gyroscope").get("y").asText());
            containerData.put("gyroscope_z", rootNode.get("gyroscope").get("z").asText());

            containerData.put("pressure", rootNode.get("pressure").asText());

            return containerData;
        } catch (IOException e) {
            // JSON 파싱 중 에러가 발생한 경우 에러를 로그에 기록하고 예외를 던집니다.
            logger.error("Error parsing JSON data: ", e);
            throw new RuntimeException("Failed to parse JSON data", e);
        }
    }
}
package IOT_Platform.Lantern_Of_Dusk_BE.service;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Connection;
import IOT_Platform.Lantern_Of_Dusk_BE.entity.Data;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.ConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataService {

    private final MobiusService mobiusService;
    private final ConnectionRepository connectionRepository;
    private final FilterService filterService;
    private final Map<String, List<Data>> dataArrays;

    @Autowired
    public DataService(MobiusService mobiusService, ConnectionRepository connectionRepository, FilterService filterService) {
        this.mobiusService = mobiusService;
        this.connectionRepository = connectionRepository;
        this.filterService = filterService;
        this.dataArrays = new HashMap<>();
    }

    /**
     * 7ms마다 Mobius 서버에서 데이터를 가져와 배열에 저장하는 메서드.
     */
    @Scheduled(fixedRate = 7)
    public void fetchDataFromMobius() {

        // 데이터베이스에서 모든 Connection을 가져옵니다.
        List<Connection> connections = connectionRepository.findAll();

        for (Connection connection : connections) {
            String aeName = connection.getAe();

            try {
                // Mobius 서버에서 JSON 데이터를 가져옴
                String jsonData = mobiusService.fetchDataFromMobiusForAE(aeName);
                // JSON 데이터를 Data 객체로 변환
                Data data = Data.fromJson(jsonData);

                // 해당 AE의 데이터 배열을 가져옴
                List<Data> dataArray = dataArrays.computeIfAbsent(aeName, k -> new ArrayList<>());

                // 데이터 배열에 추가
                dataArray.add(data);

                // 1초 동안의 데이터(약 143개)를 초과하면 앞의 데이터를 제거
                if (dataArray.size() > 143) {
                    dataArray.remove(0);
                }

                // 데이터 배열이 143개가 되면 필터링 수행
                if (dataArray.size() == 143) {
                    filterService.applyFilter(new ArrayList<>(dataArray), Integer.parseInt(connection.getName()));
                }
            } catch (Exception e) {
                // 에러 처리
                System.err.println("Error fetching data for AE " + aeName + ": " + e.getMessage());
            }
        }
    }

    //테스트용 매서드
    public List<Data> getDataArray(String aeName) {
        return dataArrays.getOrDefault(aeName, new ArrayList<>());
    }
}
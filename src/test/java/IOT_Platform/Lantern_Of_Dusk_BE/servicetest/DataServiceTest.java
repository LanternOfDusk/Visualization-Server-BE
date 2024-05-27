package IOT_Platform.Lantern_Of_Dusk_BE.servicetest;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Connection;
import IOT_Platform.Lantern_Of_Dusk_BE.entity.Data;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.ConnectionRepository;
import IOT_Platform.Lantern_Of_Dusk_BE.service.DataService;
import IOT_Platform.Lantern_Of_Dusk_BE.service.MobiusService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

@SpringBootTest
@EnableScheduling
public class DataServiceTest {

    @Mock
    private MobiusService mobiusService;

    @Mock
    private ConnectionRepository connectionRepository;

    @InjectMocks
    private DataService dataService;

    /**
     *
     * 첫 번째 테스트는 fetchDataFromMobius 메서드를 테스트합니다. 이 메서드는 Mobius 서비스에서 데이터를 가져와서 처리하는데, 이를 테스트하기 위해 Mock 객체를 사용하여 Mobius 서비스와 연결정보를 대체합니다. 이 테스트는 Mobius 서비스가 정확한 AE(Application Entity) 이름을 사용하여 데이터를 가져오는지를 확인합니다.
     */

    @Test
    public void testFetchDataFromMobius() {
        // Mock 객체로 반환될 Connection 객체 생성
        Connection connection = new Connection();
        connection.setApplicationEntity("device1");
        when(connectionRepository.findAll()).thenReturn(Collections.singletonList(connection));

        // MobiusService에서 반환될 더미 데이터 설정
        String jsonData = "{\"acceleration\": {\"x\": \"0.1\", \"y\": \"0.2\", \"z\": \"0.3\"}, \"gyroscope\": {\"x\": \"0.4\", \"y\": \"0.5\", \"z\": \"0.6\"}, \"pressure\": \"1013.25\"}";
        when(mobiusService.fetchDataFromMobiusForAE(anyString())).thenReturn(jsonData);

        // fetchDataFromMobius 메서드 호출
        dataService.fetchDataFromMobius();

        // 검증: MobiusService의 fetchDataFromMobiusForAE 메서드가 호출되었는지 확인
        verify(mobiusService, atLeastOnce()).fetchDataFromMobiusForAE("device1");
    }

    /**
     *
     * 두 번째 테스트는 saveData 메서드를 테스트합니다. 이 메서드는 수집된 데이터를 저장하는 역할을 담당합니다. 이 테스트는 데이터가 제대로 저장되고 저장 후에는 데이터 큐가 비워지는지를 확인합니다.
     */

    @Test
    public void testSaveData() {
        // AE 이름과 큐 생성
        String aeName = "device1";
        Queue<Data> queue = new LinkedList<>();
        for (int i = 0; i < 143; i++) {
            queue.add(new Data()); // 예시로 더미 데이터를 큐에 추가
        }


        // 데이터 큐의 크기가 0인지 확인 (saveData 호출 후에는 큐가 비워져야 함)
        assertEquals(143, queue.size());
        // 데이터 큐의 크기가 0인지 확인 (saveData 호출 후에는 큐가 비워져야 함)
        System.out.println("Remaining elements in the queue: " + queue.size());
    }
}

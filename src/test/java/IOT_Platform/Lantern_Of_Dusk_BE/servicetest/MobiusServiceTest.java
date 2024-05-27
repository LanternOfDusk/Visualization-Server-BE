package IOT_Platform.Lantern_Of_Dusk_BE.servicetest;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Connection;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.ConnectionRepository;
import IOT_Platform.Lantern_Of_Dusk_BE.service.DataService;
import IOT_Platform.Lantern_Of_Dusk_BE.service.MobiusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

// 테스트 어노테이션 및 import 생략

public class MobiusServiceTest {

    @Mock
    private MobiusService mobiusService;

    @Mock
    private ConnectionRepository connectionRepository;

    @InjectMocks
    private DataService dataService;

    private ThreadPoolTaskScheduler taskScheduler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Mock 객체로 반환될 Connection 객체 생성
        Connection connection = new Connection();
        connection.setApplicationEntity("device1");

        // connectionRepository.findAll()이 호출되었을 때 Mock 객체 반환 설정
        when(connectionRepository.findAll()).thenReturn(Collections.singletonList(connection));

        // MobiusService에서 반환될 더미 데이터 설정
        String jsonData = "{\"acceleration\": {\"x\": \"0.1\", \"y\": \"0.2\", \"z\": \"0.3\"}, \"gyroscope\": {\"x\": \"0.4\", \"y\": \"0.5\", \"z\": \"0.6\"}, \"pressure\": \"1013.25\"}";
        when(mobiusService.fetchDataFromMobiusForAE(anyString())).thenReturn(jsonData);

        // TaskScheduler 초기화 및 스케줄링 설정
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        PeriodicTrigger trigger = new PeriodicTrigger(1000, TimeUnit.MILLISECONDS); // 1초로 변경
        taskScheduler.schedule(dataService::fetchDataFromMobius, trigger);
    }

    /**
     * MobiusService 클래스의 fetchDataFromMobiusForAE 메서드를 테스트하는 테스트 케이스입니다.
     * fetchDataFromMobiusForAE 메서드가 호출되었는지, connectionRepository.findAll()이 한 번 호출되었는지,
     * 반환된 Mock 객체의 속성이 정확히 설정되었는지를 확인합니다.
     */
    @Test
    public void testFetchDataFromMobius() throws InterruptedException {
        // fetchDataFromMobiusForAE가 호출되었는지 확인
        verify(mobiusService, atLeast(1)).fetchDataFromMobiusForAE("device1");

        // connectionRepository.findAll()이 호출되었는지 확인
        verify(connectionRepository, times(1)).findAll();

        // 반환된 Mock 객체의 속성이 예상한 대로 설정되었는지 확인
        Connection connection = connectionRepository.findAll().get(0);
        assertEquals("device1", connection.getApplicationEntity());
    }
}

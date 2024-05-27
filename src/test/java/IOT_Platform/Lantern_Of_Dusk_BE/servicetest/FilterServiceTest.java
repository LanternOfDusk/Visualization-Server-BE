package IOT_Platform.Lantern_Of_Dusk_BE.servicetest;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Data;
import IOT_Platform.Lantern_Of_Dusk_BE.entity.Position;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.PositionRepository;
import IOT_Platform.Lantern_Of_Dusk_BE.service.FilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FilterServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private FilterService filterService;

    private List<Data> mockDataList;

    @BeforeEach
    public void setUp() {
        // Mock 데이터를 생성합니다.
        mockDataList = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            mockDataList.add(new Data(i, i * 0.1, i * 0.1, i * 0.1, i * 0.01, i * 0.01, i * 0.01, 1000 + i * 0.1));
        }
    }

    @Test
    public void testApplyFilter() {


        System.out.println("Original Data:");
        for (Data data : mockDataList) {
            System.out.println(data);
        }
        // 필터링 적용
        filterService.applyFilter(mockDataList, 1);

        // 필터링된 위치 정보가 저장되었는지 확인
        verify(positionRepository, times(1)).save(any(Position.class));



        // 데이터 개수 출력
        System.out.println("Number of data: " + mockDataList.size());

        // 필터링된 데이터 개수 및 내용 출력
        Position filteredPosition = filterService.getFilteredPosition();
        System.out.println("Number of filtered data: 1"); // 필터링된 데이터는 한 개이므로 개수는 1
        System.out.println("Filtered Position: " + filteredPosition);
    }
}

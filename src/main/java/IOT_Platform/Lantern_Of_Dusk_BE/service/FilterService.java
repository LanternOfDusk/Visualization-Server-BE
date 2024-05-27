package IOT_Platform.Lantern_Of_Dusk_BE.service;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Data;
import IOT_Platform.Lantern_Of_Dusk_BE.entity.Position;
import IOT_Platform.Lantern_Of_Dusk_BE.filter.ExtendedKalmanFilter;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.PositionRepository;
import lombok.Getter;
import org.apache.commons.math3.linear.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FilterService {

    private final PositionRepository positionRepository;
    private final ExtendedKalmanFilter kalmanFilter;

    @Autowired
    public FilterService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;

        // 초기 상태 벡터 및 공분산 행렬 설정 (모든 값이 0인 상태로 초기화)
        RealMatrix initialState = MatrixUtils.createColumnRealMatrix(new double[]{0, 0, 0, 0, 0, 0}); // [x, y, z, roll, pitch, yaw]
        RealMatrix initialCovariance = MatrixUtils.createRealDiagonalMatrix(new double[]{1, 1, 1, 1, 1, 1});

        // 상태 전이 행렬, 프로세스 노이즈, 측정 노이즈, 관측 행렬 설정 (임의의 예시 값, 실제 응용에 따라 조정 필요)
        RealMatrix stateTransition = MatrixUtils.createRealIdentityMatrix(6);
        RealMatrix processNoise = MatrixUtils.createRealDiagonalMatrix(new double[]{0.1, 0.1, 0.1, 0.1, 0.1, 0.1});
        RealMatrix measurementNoise = MatrixUtils.createRealDiagonalMatrix(new double[]{1, 1, 1, 1, 1, 1});
        RealMatrix observationMatrix = MatrixUtils.createRealIdentityMatrix(6);

        this.kalmanFilter = new ExtendedKalmanFilter(initialState, initialCovariance, stateTransition, processNoise, measurementNoise, observationMatrix);
    }

    /**
     * 메서드: applyFilter
     * 설명: 수집된 데이터를 필터링하고 위치 정보를 저장합니다.
     *
     * @param dataList 143개의 센서 데이터 리스트
     * @param deviceId 장치 ID
     */
    public void applyFilter(List<Data> dataList, int deviceId) {
        for (Data data : dataList) {
            // 측정값 벡터 생성
            RealMatrix measurement = MatrixUtils.createColumnRealMatrix(new double[]{
                    data.getX(), data.getY(), data.getZ(),
                    data.getRoll(), data.getPitch(), data.getYaw()
            });

            // 예측 단계
            kalmanFilter.predict();

            // 업데이트 단계
            kalmanFilter.update(measurement);
        }

        // 필터링된 상태 벡터 가져오기
        RealMatrix state = kalmanFilter.getState();

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

        positionRepository.save(position);
    }

    /**
     * 메서드: getFilteredPosition
     * 설명: 필터링된 위치 정보를 반환합니다.
     *
     * @return 필터링된 위치 정보
     */
    public Position getFilteredPosition() {
        // 필터링된 상태 벡터 가져오기
        RealMatrix state = kalmanFilter.getState();

        // 상태 벡터에서 위치 정보 추출
        double x = state.getEntry(0, 0);
        double y = state.getEntry(1, 0);
        double z = state.getEntry(2, 0);
        double roll = state.getEntry(3, 0);
        double pitch = state.getEntry(4, 0);
        double yaw = state.getEntry(5, 0);

        // 필터링된 위치 정보 반환
        Position position = new Position();
        position.setX(x);
        position.setY(y);
        position.setZ(z);
        position.setRoll(roll);
        position.setPitch(pitch);
        position.setYaw(yaw);

        return position;
    }
}
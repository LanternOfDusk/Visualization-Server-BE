package IOT_Platform.Lantern_Of_Dusk_BE.core;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * 클래스: ExtendedKalmanFilter
 * 설명: 확장 칼만 필터 (EKF)를 구현한 클래스입니다.
 */
public class ExtendedKalmanFilter {
    private RealMatrix state; // 상태 벡터
    private RealMatrix covariance; // 상태 공분산 행렬
    private RealMatrix stateTransition; // 상태 전이 행렬
    private RealMatrix processNoise; // 프로세스 노이즈 공분산 행렬
    private RealMatrix measurementNoise; // 측정 노이즈 공분산 행렬
    private RealMatrix observationMatrix; // 관측 행렬

    /**
     * 생성자: ExtendedKalmanFilter
     * 설명: 확장 칼만 필터의 초기 상태와 매개변수를 설정합니다.
     *
     * @param initialState 초기 상태 벡터
     * @param initialCovariance 초기 상태 공분산 행렬
     * @param stateTransition 상태 전이 행렬
     * @param processNoise 프로세스 노이즈 공분산 행렬
     * @param measurementNoise 측정 노이즈 공분산 행렬
     * @param observationMatrix 관측 행렬
     */
    public ExtendedKalmanFilter(RealMatrix initialState, RealMatrix initialCovariance,
                                RealMatrix stateTransition, RealMatrix processNoise,
                                RealMatrix measurementNoise, RealMatrix observationMatrix) {
        this.state = initialState;
        this.covariance = initialCovariance;
        this.stateTransition = stateTransition;
        this.processNoise = processNoise;
        this.measurementNoise = measurementNoise;
        this.observationMatrix = observationMatrix;
    }

    /**
     * 메서드: predict
     * 설명: 상태와 공분산을 예측합니다.
     */
    public void predict() {
        state = stateTransition.multiply(state);
        covariance = stateTransition.multiply(covariance).multiply(stateTransition.transpose()).add(processNoise);
    }

    /**
     * 메서드: update
     * 설명: 새로운 측정값을 사용하여 상태와 공분산을 갱신합니다.
     *
     * @param measurement 새로운 측정값 벡터
     */
    public void update(RealMatrix measurement) {
        RealMatrix y = measurement.subtract(observationMatrix.multiply(state)); // 잔차 계산
        RealMatrix s = observationMatrix.multiply(covariance).multiply(observationMatrix.transpose()).add(measurementNoise); // 잔차 공분산
        RealMatrix k = covariance.multiply(observationMatrix.transpose()).multiply(MatrixUtils.inverse(s)); // 칼만 이득 계산
        state = state.add(k.multiply(y)); // 상태 업데이트
        RealMatrix i = MatrixUtils.createRealIdentityMatrix(state.getRowDimension());
        covariance = (i.subtract(k.multiply(observationMatrix))).multiply(covariance); // 공분산 업데이트
    }

    /**
     * 메서드: getState
     * 설명: 현재 상태 벡터를 반환합니다.
     *
     * @return 현재 상태 벡터
     */
    public RealMatrix getState() {
        return state;
    }
}
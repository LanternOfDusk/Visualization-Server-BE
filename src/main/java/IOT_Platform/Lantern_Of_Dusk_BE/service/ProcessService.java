package IOT_Platform.Lantern_Of_Dusk_BE.service;

import IOT_Platform.Lantern_Of_Dusk_BE.core.Device;
import IOT_Platform.Lantern_Of_Dusk_BE.entity.Connection;
import IOT_Platform.Lantern_Of_Dusk_BE.entity.Position;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.ConnectionRepository;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.MarkerRepository;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.PositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class ProcessService {

    private final ConnectionRepository connectionRepository;
    private final PositionRepository positionRepository;

    private Set<Device> deviceSet = new HashSet<>();

    @Autowired
    public ProcessService(ConnectionRepository connectionRepository, PositionRepository positionRepository, MarkerRepository markerRepository) {
        this.connectionRepository = connectionRepository;
        this.positionRepository = positionRepository;
    }

    public Position getPosition(int deviceId) {
        return positionRepository.findTopByDeviceIdOrderByIdDesc(deviceId).orElse(null);
    }

    public void startProcess() {
        for (Connection c : connectionRepository.findAll()) {

            System.out.println("start process : " + c.getAe());

            Device device = new Device(positionRepository, c.getId(), c.getAe());
            device.start();

            deviceSet.add(device);

            // TODO: 5/30/24 모비우스에 신호 보내는 코드 추가
        }
    }
}
package IOT_Platform.Lantern_Of_Dusk_BE.service;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Connection;
import IOT_Platform.Lantern_Of_Dusk_BE.entity.Marker;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.ConnectionRepository;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.MarkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiService {

    private final ConnectionRepository connectionRepository;
    private final MarkerRepository markerRepository;

    @Autowired
    public ApiService(ConnectionRepository connectionRepository, MarkerRepository markerRepository) {
        this.connectionRepository = connectionRepository;
        this.markerRepository = markerRepository;
    }

    public void saveConnection(Connection connection) {
        connectionRepository.save(connection);
    }
    public Connection getConnection(int id) {
        return connectionRepository.findById(id).orElse(null);
    }
    public Connection getConnection(String ae) {
        return connectionRepository.findByAe(ae).orElse(null);
    }
    public List<Connection> getConnectionList() {
        return connectionRepository.findAll();
    }
    public void deleteDevice(int id) {
        connectionRepository.deleteById(id);
    }

    public void saveMarker(Marker marker) {
        markerRepository.save(marker);
    }
    public List<Marker> getMarkerList() {
        return markerRepository.findAll();
    }
    public Marker getMarker(int id) {
        return markerRepository.findById(id).orElse(null);
    }
    public void deleteMarker(int id) {
        markerRepository.deleteById(id);
    }
}
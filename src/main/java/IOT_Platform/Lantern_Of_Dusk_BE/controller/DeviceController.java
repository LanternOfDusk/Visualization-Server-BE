package IOT_Platform.Lantern_Of_Dusk_BE.controller;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Position;
import IOT_Platform.Lantern_Of_Dusk_BE.repository.PositionRepository;
import IOT_Platform.Lantern_Of_Dusk_BE.service.ApiService;
import IOT_Platform.Lantern_Of_Dusk_BE.service.ProcessService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@CrossOrigin
@RequestMapping("/api")
public class DeviceController {

    private final ProcessService processService;
    private final PositionRepository positionRepository;
    private final ApiService apiService;

    @PostMapping("/position")
    public void createPosition(@RequestBody Position position) {
        positionRepository.save(position);
    }

    // GET /api/position/:deviceId ⇒ deviceId 위치정보 / (int deviceId)
    @GetMapping("/position/{deviceId}")
    public ResponseEntity<Position> readPosition(@PathVariable int deviceId) {
        try {
            if (apiService.getConnection(deviceId) != null && processService.getPosition(deviceId) != null) {
                Position position = processService.getPosition(deviceId);
                return new ResponseEntity<>(position, HttpStatus.OK);
            } else return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

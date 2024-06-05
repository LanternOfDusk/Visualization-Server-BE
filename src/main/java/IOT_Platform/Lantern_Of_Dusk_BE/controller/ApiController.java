package IOT_Platform.Lantern_Of_Dusk_BE.controller;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Connection;
import IOT_Platform.Lantern_Of_Dusk_BE.entity.Marker;
import IOT_Platform.Lantern_Of_Dusk_BE.service.ApiService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@AllArgsConstructor
@CrossOrigin
@RequestMapping("/api")
public class ApiController {

    private final ApiService apiService;

    // GET /api/connection/list ⇒ 모든 연결 정보 / X
    @GetMapping("/connection/list")
    public List<Connection> readConnectionList() {
        return apiService.getConnectionList();
    }

    // GET /api/connection/:id ⇒ 특정 아이디의 연결 정보 / (int id)
    @GetMapping("/connection/{id}")
    public ResponseEntity<Connection> readConnection(@PathVariable int id) {
        try {
            Connection connection = apiService.getConnection(id);
            if (connection != null) {
                return new ResponseEntity<>(connection, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /api/connection ⇒ 연결 정보 생성 / json {name, applicationEntity}
    @PostMapping("/connection")
    public ResponseEntity<Connection> createConnection(@RequestBody Connection connection) {
        try {
            if (apiService.getConnection(connection.getAe()) == null) {
                apiService.saveConnection(connection);
                return new ResponseEntity<>(HttpStatus.CREATED);
            } else return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT /api/connection/{id} ⇒ 연결 정보 업데이트 / (int id), json {id, name, applicationEntity}
    @PutMapping("/connection/{id}")
    public ResponseEntity<Connection> updateConnection(@PathVariable int id, @RequestBody Connection updatedConnection) {
        try {
            if (apiService.getConnection(id) != null) {
                Connection pastConnection = apiService.getConnection(id);
                pastConnection.setId(updatedConnection.getId());
                pastConnection.setName(updatedConnection.getName());
                pastConnection.setAe(updatedConnection.getAe());
                apiService.saveConnection(pastConnection);
                return new ResponseEntity<>(apiService.getConnection(id), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /api/connection/:id ⇒ 연결 정보 삭제 / (int id)
    @DeleteMapping("/connection/{id}")
    public ResponseEntity<String> deleteConnection(@PathVariable int id) {
        try {
            if (apiService.getConnection(id) != null) {
                apiService.deleteDevice(id);
                return new ResponseEntity<>("ID의 ae가 삭제되었습니다", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("요청에 문제가 있습니다.", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("서버에 오류 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // GET /api/marker/list ⇒ 마커 정보 / x
    @GetMapping("/marker/list")
    public List<Marker> readMarkerList() {
        return apiService.getMarkerList();
    }

    // POST /api/marker ⇒ 마커 정보 / json {x, y, z}
    @PostMapping("/marker")
    public ResponseEntity<Marker> createMarker(@RequestBody Marker marker) {
        try {
            apiService.saveMarker(marker);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /api/marker/:id ⇒ 마커 삭제 / (int id)
    @DeleteMapping("/marker/{id}")
    public ResponseEntity<String> deleteMarker(@PathVariable int id) {
        try {
            if (apiService.getMarker(id) != null) {
                apiService.deleteMarker(id);
                return new ResponseEntity<>("Marker가 삭제되었습니다.", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("요청에 문제가 있습니다.", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("서버에 오류 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

package IOT_Platform.Lantern_Of_Dusk_BE.controller;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Connection;
import IOT_Platform.Lantern_Of_Dusk_BE.entity.Marker;
import IOT_Platform.Lantern_Of_Dusk_BE.entity.Position;
import IOT_Platform.Lantern_Of_Dusk_BE.service.ApiService;
import IOT_Platform.Lantern_Of_Dusk_BE.service.DataService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@CrossOrigin
@RequestMapping("/test")
public class TestController {

    private final DataService dataService;

    @GetMapping("/1")
    public void readConnectionList() {
        System.out.println("test 1 start");
        dataService.processData();
    }
}

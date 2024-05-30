package IOT_Platform.Lantern_Of_Dusk_BE.controller;

import IOT_Platform.Lantern_Of_Dusk_BE.service.ProcessService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@CrossOrigin
@RequestMapping("/process")
public class DeviceController {

    private final ProcessService processService;

    @GetMapping("/start")
    public void startProcess() {
        processService.startProcess();
    }
}

package IOT_Platform.Lantern_Of_Dusk_BE.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RawDataDTO {
    private int id;
    private double ax;
    private double ay;
    private double az;
    private double gx;
    private double gy;
    private double gz;
    private double atm;
}
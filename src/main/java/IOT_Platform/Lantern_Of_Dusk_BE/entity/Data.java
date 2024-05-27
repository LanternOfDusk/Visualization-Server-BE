package IOT_Platform.Lantern_Of_Dusk_BE.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Data {
    @Id
    private int id;
    private double x;
    private double y;
    private double z;
    private double pitch;
    private double yaw;
    private double roll;
    private double pressure;

    public Data(double x, double y, double z, double pitch, double yaw, double roll, double pressure) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
        this.pressure = pressure;
    }

    // JSON 데이터를 파싱해서 Data 객체로 변환하는 메서드
    public static Data fromJson(String jsonData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonData);
            return new Data(
                    rootNode.get("x").asDouble(),
                    rootNode.get("y").asDouble(),
                    rootNode.get("z").asDouble(),
                    rootNode.get("pitch").asDouble(),
                    rootNode.get("yaw").asDouble(),
                    rootNode.get("roll").asDouble(),
                    rootNode.get("pressure").asDouble() // 기압 데이터 파싱
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON data", e);
        }
    }

    @Override
    public String toString() {
        return "Data{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", pitch=" + pitch +
                ", yaw=" + yaw +
                ", roll=" + roll +
                ", pressure=" + pressure +
                '}';
    }
}
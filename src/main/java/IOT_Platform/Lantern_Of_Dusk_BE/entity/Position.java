package IOT_Platform.Lantern_Of_Dusk_BE.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int deviceId;
    private double x;
    private double y;
    private double z;
    private double roll;
    private double pitch;
    private double yaw ;
    @CreationTimestamp
    private LocalDateTime timeStamp;
}


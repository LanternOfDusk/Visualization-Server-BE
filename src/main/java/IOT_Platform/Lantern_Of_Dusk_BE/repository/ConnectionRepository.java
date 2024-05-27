package IOT_Platform.Lantern_Of_Dusk_BE.repository;

import IOT_Platform.Lantern_Of_Dusk_BE.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Integer> {
    Connection findByApplicationEntity(String ae);
}

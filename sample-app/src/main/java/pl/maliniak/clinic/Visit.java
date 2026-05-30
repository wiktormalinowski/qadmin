package pl.maliniak.clinic;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import pl.maliniak.qadmin.runtime.DisplayQAdmin;

@Entity
public class Visit extends PanacheEntity {
    public LocalDate visitDate;
    public String description;

    @ManyToOne
    @DisplayQAdmin("name")
    public Pet pet;
    
    @ManyToOne
    @DisplayQAdmin("lastName")
    public Vet vet;
}

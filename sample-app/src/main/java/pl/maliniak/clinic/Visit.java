package pl.maliniak.clinic;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;

@Entity
public class Visit extends PanacheEntity {
    public LocalDate visitDate;
    public String description;

    @ManyToOne
    public Pet pet;
    
    @ManyToOne
    public Vet vet;
}

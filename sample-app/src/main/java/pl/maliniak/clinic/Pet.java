package pl.maliniak.clinic;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;

@Entity
public class Pet extends PanacheEntity {
    public String name;
    public LocalDate birthDate;
    public String type;

    @ManyToOne
    public Owner owner;
}

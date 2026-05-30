package pl.maliniak.clinic;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Vet extends PanacheEntity {
    public String firstName;
    public String lastName;
    public String specialty;
}

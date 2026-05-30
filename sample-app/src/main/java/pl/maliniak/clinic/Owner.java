package pl.maliniak.clinic;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Owner extends PanacheEntity {
    public String firstName;
    public String lastName;
    public String address;
    public String city;
    public String telephone;

    @OneToMany(mappedBy = "owner")
    @JsonIgnore
    public List<Pet> pets;
}

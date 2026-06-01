package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "taglie")
public class Taglia extends TimestampedEntity {

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, unique = true, length = 40)
    private String codice;

    @ManyToMany(mappedBy = "taglie", fetch = FetchType.LAZY)
    private Set<Product> products = new HashSet<>();

    public Taglia() {
    }

    public Taglia(Long id, String nome, String codice) {
        setId(id);
        this.nome = nome;
        this.codice = codice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Taglia taglia)) {
            return false;
        }
        if (getId() != null && taglia.getId() != null) {
            return Objects.equals(getId(), taglia.getId());
        }
        return codice != null && taglia.codice != null && codice.equalsIgnoreCase(taglia.codice);
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            return Objects.hash(getId());
        }
        return codice == null ? 0 : codice.toLowerCase(java.util.Locale.ROOT).hashCode();
    }
}

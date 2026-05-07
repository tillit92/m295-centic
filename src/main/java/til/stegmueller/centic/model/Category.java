package til.stegmueller.centic.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "categories")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String colorCode;
    private boolean globalFlag;
}

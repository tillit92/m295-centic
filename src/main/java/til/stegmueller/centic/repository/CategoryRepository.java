package til.stegmueller.centic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import til.stegmueller.centic.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}

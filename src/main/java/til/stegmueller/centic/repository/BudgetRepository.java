package til.stegmueller.centic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import til.stegmueller.centic.model.Budget;

import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    List<Budget> findByUserIdAndMonth(Long userId, String month);
}

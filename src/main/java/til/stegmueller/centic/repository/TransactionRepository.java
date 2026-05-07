package til.stegmueller.centic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import til.stegmueller.centic.model.Transaction;
import til.stegmueller.centic.model.TransactionType;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type")
    BigDecimal sumAmountByUserIdAndType(Long userId, TransactionType type);
}
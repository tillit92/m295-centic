package til.stegmueller.centic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import til.stegmueller.centic.model.Budget;
import til.stegmueller.centic.model.User;
import til.stegmueller.centic.repository.BudgetRepository;
import til.stegmueller.centic.repository.TransactionRepository;
import til.stegmueller.centic.security.CurrentUserResolver;

import java.math.BigDecimal;
import java.net.URI;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Budgets", description = "Monatliche Budgetlimits setzen (User)")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetRepository budgetRepo;
    private final TransactionRepository transactionRepo;
    private final CurrentUserResolver userResolver;

    public BudgetController(BudgetRepository budgetRepo,
                            TransactionRepository transactionRepo,
                            CurrentUserResolver userResolver) {
        this.budgetRepo = budgetRepo;
        this.transactionRepo = transactionRepo;
        this.userResolver = userResolver;
    }

    @GetMapping
    @Operation(summary = "Alle eigenen Budgets laden")
    public List<Budget> getAll() {
        User me = userResolver.getCurrentUser();
        return budgetRepo.findByUserId(me.getId());
    }

    @PostMapping
    @Operation(summary = "Budget fuer Kategorie + Monat setzen")
    public ResponseEntity<Budget> create(@Valid @RequestBody Budget budget) {
        User me = userResolver.getCurrentUser();
        budget.setUser(me);
        Budget saved = budgetRepo.save(budget);
        return ResponseEntity
                .created(URI.create("/api/budgets/" + saved.getId()))
                .body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Budget-Limit aendern")
    public ResponseEntity<Budget> update(@PathVariable Long id,
                                         @Valid @RequestBody Budget updated) {
        User me = userResolver.getCurrentUser();
        return budgetRepo.findById(id)
                .filter(b -> b.getUser().getId().equals(me.getId()))
                .map(existing -> {
                    existing.setLimitAmount(updated.getLimitAmount());
                    return ResponseEntity.ok(budgetRepo.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User me = userResolver.getCurrentUser();
        return budgetRepo.findById(id)
                .filter(b -> b.getUser().getId().equals(me.getId()))
                .map(b -> {
                    budgetRepo.delete(b);
                    return ResponseEntity.<Void>noContent().<Void>build();
                })
                .orElse(ResponseEntity.<Void>notFound().build());
    }

    // Prüft für Budgets des Monats ob das Limit überschritten wurde oder nicht
    @GetMapping("/check")
    @Operation(summary = "Pruefe ob Budget-Limits ueberschritten wurden")
    public List<Map<String, Object>> checkBudgets() {
        User me = userResolver.getCurrentUser();
        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<Budget> budgets = budgetRepo.findByUserIdAndMonth(me.getId(), currentMonth);

        List<Map<String, Object>> alerts = new ArrayList<>();

        for (Budget budget : budgets) {
            // Summe der Ausgaben für diese Kategorie im aktuellen Monat
            BigDecimal spent = transactionRepo.findByUserId(me.getId()).stream()
                    .filter(t -> t.getCategory() != null
                            && t.getCategory().getId().equals(budget.getCategory().getId())
                            && t.getDate().toString().startsWith(currentMonth))
                    .map(t -> t.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (spent.compareTo(budget.getLimitAmount()) > 0) {
                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("category", budget.getCategory().getName());
                alert.put("limit", budget.getLimitAmount());
                alert.put("spent", spent);
                alert.put("exceeded", spent.subtract(budget.getLimitAmount()));
                alerts.add(alert);
            }
        }

        return alerts;
    }
}
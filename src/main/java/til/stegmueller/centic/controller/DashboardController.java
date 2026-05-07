package til.stegmueller.centic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import til.stegmueller.centic.model.Transaction;
import til.stegmueller.centic.model.TransactionType;
import til.stegmueller.centic.model.User;
import til.stegmueller.centic.repository.TransactionRepository;
import til.stegmueller.centic.security.CurrentUserResolver;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Berechnete Finanzdaten (ROLE_USER)")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final TransactionRepository transactionRepo;
    private final CurrentUserResolver userResolver;

    public DashboardController(TransactionRepository transactionRepo,
                               CurrentUserResolver userResolver) {
        this.transactionRepo = transactionRepo;
        this.userResolver = userResolver;
    }

    /**
     * Gesamtsaldo = Summe INCOME - Summe EXPENSE
     */
    @GetMapping("/summary")
    @Operation(summary = "Gesamtsaldo und Einnahmen/Ausgaben-Summen")
    public Map<String, Object> getSummary() {
        User me = userResolver.getCurrentUser();
        BigDecimal totalIncome = transactionRepo.sumAmountByUserIdAndType(me.getId(), TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepo.sumAmountByUserIdAndType(me.getId(), TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("balance", balance);
        return summary;
    }

    /**
     * Ausgaben pro Monat (gruppiert)
     */
    @GetMapping("/monthly")
    @Operation(summary = "Ausgaben und Einnahmen pro Monat")
    public Map<String, Object> getMonthlySummary(@RequestParam(defaultValue = "6") int months) {
        User me = userResolver.getCurrentUser();
        List<Transaction> all = transactionRepo.findByUserId(me.getId());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, BigDecimal> incomeByMonth = new TreeMap<>();
        Map<String, BigDecimal> expenseByMonth = new TreeMap<>();

        // Die letzten N Monate initialisieren
        YearMonth current = YearMonth.now();
        for (int i = 0; i < months; i++) {
            String key = current.minusMonths(i).format(fmt);
            incomeByMonth.put(key, BigDecimal.ZERO);
            expenseByMonth.put(key, BigDecimal.ZERO);
        }

        for (Transaction t : all) {
            String monthKey = t.getDate().format(fmt);
            if (t.getType() == TransactionType.INCOME) {
                incomeByMonth.computeIfPresent(monthKey, (k, v) -> v.add(t.getAmount()));
            } else {
                expenseByMonth.computeIfPresent(monthKey, (k, v) -> v.add(t.getAmount()));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("income", incomeByMonth);
        result.put("expense", expenseByMonth);
        return result;
    }

    /**
     * Ausgaben pro Kategorie (aktueller Monat)
     */
    @GetMapping("/category-stats")
    @Operation(summary = "Ausgaben pro Kategorie im aktuellen Monat")
    public Map<String, BigDecimal> getCategoryStats() {
        User me = userResolver.getCurrentUser();
        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        return transactionRepo.findByUserId(me.getId()).stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE
                        && t.getDate().toString().startsWith(currentMonth)
                        && t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));
    }

    /**
     * Budget-Warnungen (wie in BudgetController, hier als Dashboard-Einstiegspunkt)
     */
    @GetMapping("/budget-alerts")
    @Operation(summary = "Aktuelle Budget-Ueberschreitungen")
    public List<String> getBudgetAlerts() {
        // Simpel: zeige Anzahl Warnungen, Details via /api/budgets/check
        return List.of("Nutze /api/budgets/check fuer detaillierte Budget-Warnungen");
    }
}
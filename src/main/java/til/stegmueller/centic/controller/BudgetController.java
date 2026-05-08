package til.stegmueller.centic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import til.stegmueller.centic.model.Budget;
import til.stegmueller.centic.model.User;
import til.stegmueller.centic.security.CurrentUserResolver;
import til.stegmueller.centic.service.BudgetService;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Monatliche Budgetlimits setzen")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
public class BudgetController {

    private final BudgetService budgetService;
    private final CurrentUserResolver userResolver;

    @GetMapping
    @Operation(summary = "Alle eigenen Budgets laden")
    public List<Budget> getAll() {
        return budgetService.getAllForUser(userResolver.getCurrentUser());
    }

    @PostMapping
    @Operation(summary = "Budget für Kategorie + Monat setzen")
    public ResponseEntity<Budget> create(@Valid @RequestBody Budget budget) {
        User me = userResolver.getCurrentUser();
        Budget saved = budgetService.create(budget, me);
        return ResponseEntity
                .created(URI.create("/api/budgets/" + saved.getId()))
                .body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Budget-Limit ändern")
    public ResponseEntity<Budget> update(@PathVariable Long id,
                                         @Valid @RequestBody Budget updated) {
        User me = userResolver.getCurrentUser();
        return ResponseEntity.ok(budgetService.updateLimit(id, updated, me));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Budget löschen")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.delete(id, userResolver.getCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check")
    @Operation(summary = "Prüfe ob Budget-Limits überschritten wurden")
    public List<Map<String, Object>> checkBudgets() {
        return budgetService.checkBudgetAlerts(userResolver.getCurrentUser());
    }
}
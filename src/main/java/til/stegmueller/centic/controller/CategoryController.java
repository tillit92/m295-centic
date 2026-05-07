package til.stegmueller.centic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import til.stegmueller.centic.model.Category;
import til.stegmueller.centic.repository.CategoryRepository;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Kategorien verwalten (Lesen: USER, Schreiben: ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryRepository categoryRepo;

    public CategoryController(CategoryRepository categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    @GetMapping
    @Operation(summary = "Alle Kategorien laden")
    public List<Category> getAll() {
        return categoryRepo.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Kategorie per ID laden")
    public ResponseEntity<Category> getById(@PathVariable Long id) {
        return categoryRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")  // Nur Admins dürfen erstellen
    @Operation(summary = "Neue Kategorie anlegen (ADMIN)")
    public ResponseEntity<Category> create(@Valid @RequestBody Category category) {
        Category saved = categoryRepo.save(category);
        return ResponseEntity
                .created(URI.create("/api/categories/" + saved.getId()))
                .body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kategorie aktualisieren (ADMIN)")
    public ResponseEntity<Category> update(@PathVariable Long id,
                                           @Valid @RequestBody Category updated) {
        return categoryRepo.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setColorCode(updated.getColorCode());
                    existing.setGlobalFlag(updated.isGlobalFlag());
                    return ResponseEntity.ok(categoryRepo.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kategorie loeschen (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!categoryRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        categoryRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
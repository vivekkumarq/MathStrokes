package com.mathstrokes.marking.controller;

import java.util.List;

import com.mathstrokes.marking.dto.MarkingSchemeRequest;
import com.mathstrokes.marking.dto.MarkingSchemeResponse;
import com.mathstrokes.marking.service.MarkingSchemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/marking-schemes")
@Tag(name = "Admin - Marking schemes", description = "Configurable scoring rules")
public class AdminMarkingSchemeController {

    private final MarkingSchemeService service;

    public AdminMarkingSchemeController(MarkingSchemeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all marking schemes")
    public List<MarkingSchemeResponse> list() {
        return service.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a marking scheme")
    public MarkingSchemeResponse create(@Valid @RequestBody MarkingSchemeRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a marking scheme",
            description = "Historical attempts keep the configuration snapshotted at their start "
                    + "time and are never affected by this change.")
    public MarkingSchemeResponse update(@PathVariable Long id,
                                        @Valid @RequestBody MarkingSchemeRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activate or deactivate a marking scheme")
    public MarkingSchemeResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
        return service.setActive(id, active);
    }
}

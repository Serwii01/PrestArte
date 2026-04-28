package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.entity.Foundation;
import com.prestarte.tfg.service.FoundationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foundations")
@RequiredArgsConstructor
public class FoundationController {

    private final FoundationService foundationService;

    @PostMapping
    public Foundation createFoundation(@RequestBody Foundation foundation) {
        return foundationService.createFoundation(foundation);
    }

    @GetMapping
    public List<Foundation> getAllFoundations() {
        return foundationService.getAllFoundations();
    }
}
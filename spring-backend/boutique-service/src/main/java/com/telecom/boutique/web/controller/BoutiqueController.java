package com.telecom.boutique.web.controller;

import com.telecom.boutique.application.BoutiqueService;
import com.telecom.boutique.application.DashboardService;
import com.telecom.boutique.application.StockSimService;
import com.telecom.boutique.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/boutiques")
@RequiredArgsConstructor
public class BoutiqueController {

    private final BoutiqueService boutiqueService;
    private final StockSimService stockSimService;
    private final DashboardService dashboardService;


    @GetMapping
    public ResponseEntity<List<BoutiqueDto>> getAll() {
        return ResponseEntity.ok(boutiqueService.getAllBoutiques());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoutiqueDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(boutiqueService.getBoutiqueById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<BoutiqueDto> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(boutiqueService.getBoutiqueByCode(code));
    }

    @PostMapping
    public ResponseEntity<BoutiqueDto> create(@Valid @RequestBody CreateBoutiqueRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boutiqueService.createBoutique(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoutiqueDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoutiqueRequest req) {
        return ResponseEntity.ok(boutiqueService.updateBoutique(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        boutiqueService.deactivateBoutique(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}/dashboard")
    public ResponseEntity<DashboardDto> getDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(dashboardService.getDashboard(id));
    }


    @GetMapping("/{id}/stock")
    public ResponseEntity<List<StockSimDto>> getStock(@PathVariable Long id) {
        return ResponseEntity.ok(stockSimService.getStockByBoutique(id));
    }

    @GetMapping("/{id}/stock/available")
    public ResponseEntity<List<StockSimDto>> getAvailableStock(@PathVariable Long id) {
        return ResponseEntity.ok(stockSimService.getAvailableStock(id));
    }

    @PostMapping("/stock/{iccid}/assign")
    public ResponseEntity<StockSimDto> assignSim(
            @PathVariable String iccid,
            @Valid @RequestBody AssignSimRequest req) {
        return ResponseEntity.ok(stockSimService.assignSim(iccid, req.clientId()));
    }

    @PostMapping("/stock/{iccid}/activate")
    public ResponseEntity<StockSimDto> activateSim(
            @PathVariable String iccid,
            @Valid @RequestBody AssignSimRequest req) {
        return ResponseEntity.ok(stockSimService.activateSim(iccid, req.clientId()));
    }

    @PostMapping("/stock/{iccid}/suspend")
    public ResponseEntity<StockSimDto> suspendSim(@PathVariable String iccid) {
        return ResponseEntity.ok(stockSimService.suspendSim(iccid));
    }

    @PostMapping("/stock/{iccid}/deactivate")
    public ResponseEntity<StockSimDto> deactivateSim(@PathVariable String iccid) {
        return ResponseEntity.ok(stockSimService.deactivateSim(iccid));
    }

    @PostMapping("/{id}/stock/batch")
    public ResponseEntity<List<StockSimDto>> addSimBatch(
            @PathVariable Long id,
            @Valid @RequestBody List<AddSimRequest> sims) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockSimService.addSimBatch(id, sims));
    }


    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionDto>> getTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(dashboardService.getTransactions(id));
    }

    @GetMapping("/{id}/transactions/today")
    public ResponseEntity<List<TransactionDto>> getTodayTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(dashboardService.getTodayTransactions(id));
    }
}

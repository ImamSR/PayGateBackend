package com.payment.controller;

import com.payment.dto.PaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.dto.TransactionSummaryResponse;
import com.payment.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentController {

    private final TransactionService transactionService;

    public PaymentController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(transactionService.createTransaction(request, authentication));
    }

    @GetMapping("/transactions")
    public List<TransactionSummaryResponse> getTransactions(Authentication authentication) {
        return transactionService.getTransactions(authentication);
    }

    @GetMapping("/transactions/{transactionId}")
    public TransactionSummaryResponse getTransaction(
            @PathVariable String transactionId,
            Authentication authentication
    ) {
        return transactionService.getTransaction(transactionId, authentication);
    }
}

package com.example.payment.dto;

import java.math.BigDecimal;
import java.util.List;

public record UserDashboardResponse(
    BigDecimal currentBalance,
    BigDecimal totalSpent,
    long activeTransactionsCount,
    List<TransactionSummaryResponse> recentTransactions
) {}


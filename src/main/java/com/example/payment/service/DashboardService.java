package com.example.payment.service;

import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import com.example.payment.dto.TransactionSummaryResponse;
import com.example.payment.dto.UserDashboardResponse;
import com.example.payment.entity.PaymentAccount;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.repository.PaymentAccountRepository;
import com.example.payment.repository.TransactionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PaymentAccountRepository paymentAccountRepository;

    public DashboardService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            PaymentAccountRepository paymentAccountRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.paymentAccountRepository = paymentAccountRepository;
    }

    @Transactional(readOnly = true)
    public UserDashboardResponse getUserDashboard(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        PaymentAccount paymentAccount = paymentAccountRepository.findById(user.getId())
                .orElse(new PaymentAccount(user.getId(), user.getUsername()));

        BigDecimal totalSpent = transactionRepository.sumAmountByAuthUserIdAndStatus(user.getId(), PaymentStatus.COMPLETED);

        long pendingCount = transactionRepository.countByAuthUserIdAndStatus(user.getId(), PaymentStatus.PENDING);
        long processingCount = transactionRepository.countByAuthUserIdAndStatus(user.getId(), PaymentStatus.PROCESSING);
        long activeTransactionsCount = pendingCount + processingCount;

        Instant since = Instant.now().minus(365, ChronoUnit.DAYS);
        List<TransactionSummaryResponse> recentTransactions = transactionRepository.findRecentTransactions(user.getId(), since)
                .stream()
                .limit(5)
                .map(TransactionSummaryResponse::from)
                .collect(Collectors.toList());

        return new UserDashboardResponse(
                paymentAccount.getBalance(),
                totalSpent,
                activeTransactionsCount,
                recentTransactions
        );
    }
}

package com.payment.repository;

import com.payment.entity.PaymentStatus;
import com.payment.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    List<Transaction> findByAuthUserId(Long authUserId, Pageable pageable);

    Page<Transaction> findByAuthUserIdAndStatus(Long authUserId, PaymentStatus status, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.authUserId = :authUserId AND t.createdAt BETWEEN :fromDate AND :toDate ORDER BY t.createdAt DESC")
    Page<Transaction> findByAuthUserIdAndDateRange(@Param("authUserId") Long authUserId,
                                           @Param("fromDate") Instant fromDate, 
                                           @Param("toDate") Instant toDate, 
                                           Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.authUserId = :authUserId AND t.amount BETWEEN :minAmount AND :maxAmount ORDER BY t.createdAt DESC")
    Page<Transaction> findByAuthUserIdAndAmountRange(@Param("authUserId") Long authUserId,
                                             @Param("minAmount") BigDecimal minAmount, 
                                             @Param("maxAmount") BigDecimal maxAmount, 
                                             Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.authUserId = :authUserId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAt <= :toDate) " +
           "AND (:minAmount IS NULL OR t.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR t.amount <= :maxAmount) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findByAuthUserIdWithFilters(@Param("authUserId") Long authUserId,
                                          @Param("status") PaymentStatus status,
                                          @Param("fromDate") Instant fromDate,
                                          @Param("toDate") Instant toDate,
                                          @Param("minAmount") BigDecimal minAmount,
                                          @Param("maxAmount") BigDecimal maxAmount,
                                          Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.authUserId = :authUserId AND t.status = :status")
    BigDecimal sumAmountByAuthUserIdAndStatus(@Param("authUserId") Long authUserId, @Param("status") PaymentStatus status);

    long countByAuthUserIdAndStatus(Long authUserId, PaymentStatus status);

    List<Transaction> findByStatus(PaymentStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.updatedAt < :cutoffTime")
    List<Transaction> findStuckTransactions(@Param("status") PaymentStatus status, 
                                          @Param("cutoffTime") Instant cutoffTime);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:status IS NULL OR t.status = :status) " +
           "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAt <= :toDate) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findAllWithFilters(@Param("status") PaymentStatus status,
                                       @Param("fromDate") Instant fromDate,
                                       @Param("toDate") Instant toDate,
                                       Pageable pageable);


    @Query("SELECT COUNT(t), " +
           "SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status = 'FAILED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status = 'COMPLETED' THEN t.amount ELSE 0 END) " +
           "FROM Transaction t WHERE t.createdAt BETWEEN :fromDate AND :toDate")
    Object[] getTransactionStatistics(@Param("fromDate") Instant fromDate, 
                                    @Param("toDate") Instant toDate);

    Optional<Transaction> findByGatewayReference(String gatewayReference);

    @Query("SELECT t FROM Transaction t WHERE t.authUserId = :authUserId AND t.createdAt >= :since ORDER BY t.createdAt DESC")
    List<Transaction> findRecentTransactions(@Param("authUserId") Long authUserId, @Param("since") Instant since);
}

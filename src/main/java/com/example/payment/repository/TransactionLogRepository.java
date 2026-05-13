package com.example.payment.repository;

import com.example.payment.entity.PaymentStatus;
import com.example.payment.entity.Transaction;
import com.example.payment.entity.TransactionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    List<TransactionLog> findByTransactionOrderByCreatedAtAsc(Transaction transaction);

    Page<TransactionLog> findByTransaction(Transaction transaction, Pageable pageable);

    Page<TransactionLog> findByUserIdentifier(String userIdentifier, Pageable pageable);

    Page<TransactionLog> findByTriggeringEvent(String triggeringEvent, Pageable pageable);

    Page<TransactionLog> findByOldStatusAndNewStatus(PaymentStatus oldStatus, PaymentStatus newStatus, Pageable pageable);

    @Query("SELECT tl FROM TransactionLog tl WHERE tl.createdAt BETWEEN :fromDate AND :toDate ORDER BY tl.createdAt DESC")
    Page<TransactionLog> findByDateRange(@Param("fromDate") Instant fromDate, 
                                       @Param("toDate") Instant toDate, 
                                       Pageable pageable);

    @Query("SELECT COUNT(tl) FROM TransactionLog tl WHERE tl.triggeringEvent = :triggeringEvent AND tl.createdAt BETWEEN :fromDate AND :toDate")
    long countByTriggeringEventAndDateRange(@Param("triggeringEvent") String triggeringEvent,
                                          @Param("fromDate") Instant fromDate,
                                          @Param("toDate") Instant toDate);

    @Query("SELECT tl FROM TransactionLog tl WHERE tl.transaction = :transaction ORDER BY tl.createdAt DESC LIMIT 1")
    TransactionLog findLatestByTransaction(@Param("transaction") Transaction transaction);

    @Query("SELECT tl FROM TransactionLog tl WHERE tl.createdAt < :cutoffDate")
    List<TransactionLog> findLogsForArchival(@Param("cutoffDate") Instant cutoffDate);

    @Query("SELECT COUNT(tl), COUNT(DISTINCT tl.transaction), COUNT(DISTINCT tl.userIdentifier) " +
           "FROM TransactionLog tl WHERE tl.createdAt BETWEEN :fromDate AND :toDate")
    Object[] getAuditStatistics(@Param("fromDate") Instant fromDate, 
                              @Param("toDate") Instant toDate);

    @Query("SELECT tl FROM TransactionLog tl WHERE tl.newStatus = 'FAILED' ORDER BY tl.createdAt DESC")
    Page<TransactionLog> findFailedTransactionLogs(Pageable pageable);

    @Query("SELECT tl FROM TransactionLog tl WHERE tl.newStatus = 'CANCELLED' ORDER BY tl.createdAt DESC")
    Page<TransactionLog> findCancelledTransactionLogs(Pageable pageable);

    @Query("DELETE FROM TransactionLog tl WHERE tl.createdAt < :cutoffDate")
    int deleteOldLogs(@Param("cutoffDate") Instant cutoffDate);
}
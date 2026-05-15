package com.smartatm.adapter.output.persistence

import com.smartatm.domain.AtmCashInventory
import com.smartatm.domain.Transaction
import com.smartatm.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UserRepository : JpaRepository<User, Long>

@Repository
interface AtmCashInventoryRepository : JpaRepository<AtmCashInventory, Long> {

    fun findAllByOrderByDenominationDesc(): List<AtmCashInventory>

    fun findByDenomination(denomination: Int): AtmCashInventory?
}

@Repository
interface TransactionRepository : JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.createdAt >= :start AND t.createdAt < :end")
    fun findAllByDay(start: LocalDateTime, end: LocalDateTime): List<Transaction>
}

package com.smartatm.application

import com.smartatm.adapter.output.persistence.AtmCashInventoryRepository
import com.smartatm.adapter.output.persistence.TransactionRepository
import com.smartatm.adapter.output.persistence.UserRepository
import com.smartatm.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class WithdrawService(
    private val userRepository: UserRepository,
    private val atmCashInventoryRepository: AtmCashInventoryRepository,
    private val transactionRepository: TransactionRepository
) {

    @Transactional
    fun withdraw(userId: Long, amount: Long): Map<Int, Int> {
        if (amount <= 0) throw InvalidAmountException(amount)

        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId) }

        if (user.balance < BigDecimal(amount)) {
            throw InsufficientBalanceException(userId, amount, user.balance.toLong())
        }

        val inventory = atmCashInventoryRepository.findAllByOrderByDenominationDesc()

        if (inventory.all { it.quantity == 0 }) throw AtmOutOfCashException()

        val notesUsed = calculateNotes(amount, inventory)
            ?: run {
                val maxAmount = calculateMaxDispensable(inventory)
                throw InsufficientNotesException(maxAmount, amount)
            }

        notesUsed.forEach { (denomination, qty) ->
            val stock = inventory.first { it.denomination == denomination }
            stock.quantity -= qty
            atmCashInventoryRepository.save(stock)
        }

        user.balance = user.balance.subtract(BigDecimal(amount))
        userRepository.save(user)

        transactionRepository.save(
            Transaction(
                user = user,
                type = TransactionType.WITHDRAW,
                amount = BigDecimal(amount),
                notesUsed = notesUsed.entries.joinToString(", ") { "R$${it.key}x${it.value}" }
            )
        )

        return notesUsed
    }

    /**
     * Algoritmo greedy: prioriza notas maiores (R$50 → R$10 → R$5 → R$2).
     * Retorna null se não for possível atender o valor exato.
     */
    private fun calculateNotes(amount: Long, inventory: List<AtmCashInventory>): Map<Int, Int>? {
        var remaining = amount
        val result = mutableMapOf<Int, Int>()

        for (stock in inventory) {
            if (remaining == 0L) break
            if (stock.quantity == 0) continue

            val notesNeeded = (remaining / stock.denomination).toInt()
            val notesAvailable = minOf(notesNeeded, stock.quantity)

            if (notesAvailable > 0) {
                result[stock.denomination] = notesAvailable
                remaining -= notesAvailable * stock.denomination
            }
        }

        return if (remaining == 0L) result else null
    }

    private fun calculateMaxDispensable(inventory: List<AtmCashInventory>): Long {
        return inventory.sumOf { it.denomination.toLong() * it.quantity }
    }
}

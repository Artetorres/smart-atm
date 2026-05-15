package com.smartatm.application

import com.smartatm.adapter.output.persistence.AtmCashInventoryRepository
import com.smartatm.adapter.output.persistence.TransactionRepository
import com.smartatm.adapter.output.persistence.UserRepository
import com.smartatm.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class DepositService(
    private val userRepository: UserRepository,
    private val atmCashInventoryRepository: AtmCashInventoryRepository,
    private val transactionRepository: TransactionRepository
) {

    companion object {
        val VALID_DENOMINATIONS = setOf(2, 5, 10, 50)
    }

    @Transactional
    fun deposit(userId: Long, notes: Map<Int, Int>): BigDecimal {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId) }

        notes.keys.forEach { denomination ->
            if (denomination !in VALID_DENOMINATIONS) {
                throw InvalidDenominationException(denomination)
            }
        }

        val totalDeposited = notes.entries.sumOf { (denomination, qty) ->
            denomination.toLong() * qty
        }

        if (totalDeposited <= 0) throw InvalidAmountException(totalDeposited)

        notes.forEach { (denomination, qty) ->
            val stock = atmCashInventoryRepository.findByDenomination(denomination)
                ?: AtmCashInventory(denomination = denomination, quantity = 0)
            stock.quantity += qty
            atmCashInventoryRepository.save(stock)
        }

        user.balance = user.balance.add(BigDecimal(totalDeposited))
        userRepository.save(user)

        transactionRepository.save(
            Transaction(
                user = user,
                type = TransactionType.DEPOSIT,
                amount = BigDecimal(totalDeposited),
                notesUsed = notes.entries.joinToString(", ") { "R$${it.key}x${it.value}" }
            )
        )

        return user.balance
    }
}

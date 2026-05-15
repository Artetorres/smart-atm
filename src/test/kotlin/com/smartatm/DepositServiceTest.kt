package com.smartatm.application

import com.smartatm.adapter.output.persistence.AtmCashInventoryRepository
import com.smartatm.adapter.output.persistence.TransactionRepository
import com.smartatm.adapter.output.persistence.UserRepository
import com.smartatm.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.util.Optional

class DepositServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var atmCashInventoryRepository: AtmCashInventoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var depositService: DepositService

    @BeforeEach
    fun setup() {
        userRepository = mock()
        atmCashInventoryRepository = mock()
        transactionRepository = mock()
        depositService = DepositService(userRepository, atmCashInventoryRepository, transactionRepository)

        whenever(atmCashInventoryRepository.save(any<AtmCashInventory>()))
            .thenAnswer { it.arguments[0] as AtmCashInventory }
        whenever(userRepository.save(any<User>()))
            .thenAnswer { it.arguments[0] as User }
        whenever(transactionRepository.save(any<Transaction>()))
            .thenAnswer { it.arguments[0] as Transaction }
    }

    @Test
    fun `should deposit successfully and update balance`() {
        val user = User(id = 1L, name = "João", balance = BigDecimal("500"))
        val stock50 = AtmCashInventory(denomination = 50, quantity = 10)
        val stock10 = AtmCashInventory(denomination = 10, quantity = 5)

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(atmCashInventoryRepository.findByDenomination(50)).thenReturn(stock50)
        whenever(atmCashInventoryRepository.findByDenomination(10)).thenReturn(stock10)

        val newBalance = depositService.deposit(userId = 1L, notes = mapOf(50 to 2, 10 to 3))

        assertEquals(BigDecimal("630"), newBalance)
        assertEquals(12, stock50.quantity)
        assertEquals(8, stock10.quantity)
    }

    @Test
    fun `should throw InvalidDenominationException for invalid denomination`() {
        val user = User(id = 1L, name = "João", balance = BigDecimal("500"))
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

        assertThrows<InvalidDenominationException> {
            depositService.deposit(userId = 1L, notes = mapOf(20 to 1))
        }
    }

    @Test
    fun `should accept all valid denominations`() {
        val user = User(id = 1L, name = "João", balance = BigDecimal("0"))

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(atmCashInventoryRepository.findByDenomination(any())).thenReturn(
            AtmCashInventory(denomination = 2, quantity = 0)
        )

        assertDoesNotThrow {
            depositService.deposit(userId = 1L, notes = mapOf(2 to 1, 5 to 1, 10 to 1, 50 to 1))
        }
    }

    @Test
    fun `should throw UserNotFoundException when user does not exist`() {
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<UserNotFoundException> {
            depositService.deposit(userId = 99L, notes = mapOf(50 to 1))
        }
    }

    @Test
    fun `should increment atm inventory after deposit`() {
        val user = User(id = 1L, name = "João", balance = BigDecimal("0"))
        val stock = AtmCashInventory(denomination = 50, quantity = 20)

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(atmCashInventoryRepository.findByDenomination(50)).thenReturn(stock)

        depositService.deposit(userId = 1L, notes = mapOf(50 to 5))

        assertEquals(25, stock.quantity)
    }
}

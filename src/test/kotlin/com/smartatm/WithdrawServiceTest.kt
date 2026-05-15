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

class WithdrawServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var atmCashInventoryRepository: AtmCashInventoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var withdrawService: WithdrawService

    @BeforeEach
    fun setup() {
        userRepository = mock()
        atmCashInventoryRepository = mock()
        transactionRepository = mock()
        withdrawService = WithdrawService(userRepository, atmCashInventoryRepository, transactionRepository)

        whenever(atmCashInventoryRepository.save(any<AtmCashInventory>()))
            .thenAnswer { it.arguments[0] as AtmCashInventory }
        whenever(userRepository.save(any<User>()))
            .thenAnswer { it.arguments[0] as User }
        whenever(transactionRepository.save(any<Transaction>()))
            .thenAnswer { it.arguments[0] as Transaction }
    }

    @Test
    fun `should withdraw using largest notes first (greedy)`() {
        val user = User(id = 1L, name = "João", balance = BigDecimal("1000"))
        val inventory = mutableListOf(
            AtmCashInventory(denomination = 50, quantity = 10),
            AtmCashInventory(denomination = 10, quantity = 10),
            AtmCashInventory(denomination = 5,  quantity = 10),
            AtmCashInventory(denomination = 2,  quantity = 10)
        )

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(atmCashInventoryRepository.findAllByOrderByDenominationDesc()).thenReturn(inventory)

        val result = withdrawService.withdraw(userId = 1L, amount = 130)

        assertEquals(2, result[50])
        assertEquals(3, result[10])
        assertNull(result[5])
        assertNull(result[2])
    }

    @Test
    fun `should use smaller notes when larger are unavailable`() {
        val user = User(id = 1L, name = "João", balance = BigDecimal("1000"))
        val inventory = mutableListOf(
            AtmCashInventory(denomination = 50, quantity = 0),
            AtmCashInventory(denomination = 10, quantity = 10),
            AtmCashInventory(denomination = 5,  quantity = 10),
            AtmCashInventory(denomination = 2,  quantity = 10)
        )

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(atmCashInventoryRepository.findAllByOrderByDenominationDesc()).thenReturn(inventory)

        val result = withdrawService.withdraw(userId = 1L, amount = 30)

        assertEquals(3, result[10])
        assertNull(result[50])
    }

    @Test
    fun `should throw InsufficientNotesException when amount cannot be dispensed exactly`() {
        val user = User(id = 1L, name = "João", balance = BigDecimal("1000"))
        val inventory = mutableListOf(
            AtmCashInventory(denomination = 50, quantity = 1),
            AtmCashInventory(denomination = 10, quantity = 0),
            AtmCashInventory(denomination = 5,  quantity = 0),
            AtmCashInventory(denomination = 2,  quantity = 0)
        )

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(atmCashInventoryRepository.findAllByOrderByDenominationDesc()).thenReturn(inventory)

        val ex = assertThrows<InsufficientNotesException> {
            withdrawService.withdraw(userId = 1L, amount = 30)
        }

        assertEquals(50L, ex.maxDispensableAmount)
    }

    @Test
    fun `should throw InsufficientBalanceException when user has no balance`() {
        val user = User(id = 1L, name = "João", balance = BigDecimal("50"))
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

        assertThrows<InsufficientBalanceException> {
            withdrawService.withdraw(userId = 1L, amount = 100)
        }
    }

    @Test
    fun `should throw AtmOutOfCashException when all notes are zero`() {
        val user = User(id = 1L, name = "João", balance = BigDecimal("1000"))
        val inventory = mutableListOf(
            AtmCashInventory(denomination = 50, quantity = 0),
            AtmCashInventory(denomination = 10, quantity = 0),
            AtmCashInventory(denomination = 5,  quantity = 0),
            AtmCashInventory(denomination = 2,  quantity = 0)
        )

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(atmCashInventoryRepository.findAllByOrderByDenominationDesc()).thenReturn(inventory)

        assertThrows<AtmOutOfCashException> {
            withdrawService.withdraw(userId = 1L, amount = 50)
        }
    }

    @Test
    fun `should throw UserNotFoundException when user does not exist`() {
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<UserNotFoundException> {
            withdrawService.withdraw(userId = 99L, amount = 50)
        }
    }

    @Test
    fun `should throw InvalidAmountException when amount is zero or negative`() {
        assertThrows<InvalidAmountException> {
            withdrawService.withdraw(userId = 1L, amount = 0)
        }
        assertThrows<InvalidAmountException> {
            withdrawService.withdraw(userId = 1L, amount = -50)
        }
    }

    @Test
    fun `should debit user balance after successful withdrawal`() {
        val user = User(id = 1L, name = "João", balance = BigDecimal("200"))
        val inventory = mutableListOf(
            AtmCashInventory(denomination = 50, quantity = 10),
            AtmCashInventory(denomination = 10, quantity = 10),
            AtmCashInventory(denomination = 5,  quantity = 10),
            AtmCashInventory(denomination = 2,  quantity = 10)
        )

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(atmCashInventoryRepository.findAllByOrderByDenominationDesc()).thenReturn(inventory)

        withdrawService.withdraw(userId = 1L, amount = 100)

        assertEquals(BigDecimal("100"), user.balance)
    }
}

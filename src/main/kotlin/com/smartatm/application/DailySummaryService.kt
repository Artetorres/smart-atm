package com.smartatm.application

import com.smartatm.adapter.output.persistence.AtmCashInventoryRepository
import com.smartatm.adapter.output.persistence.TransactionRepository
import com.smartatm.domain.TransactionType
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

data class DailySummaryResponse(
    val date: String,
    val totalWithdrawals: Int,
    val totalDeposits: Int,
    val totalAmountMoved: BigDecimal,
    val currentInventory: Map<Int, Int>,
    val aiSummary: String
)

@Service
class DailySummaryService(
    private val transactionRepository: TransactionRepository,
    private val atmCashInventoryRepository: AtmCashInventoryRepository,
    private val chatClient: ChatClient
) {

    fun generateDailySummary(): DailySummaryResponse {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay()
        val endOfDay = today.atTime(23, 59, 59)

        val transactions = transactionRepository.findAllByDay(startOfDay, endOfDay)

        val withdrawals = transactions.filter { it.type == TransactionType.WITHDRAW }
        val deposits = transactions.filter { it.type == TransactionType.DEPOSIT }
        val totalAmountMoved = transactions.sumOf { it.amount }

        val inventory = atmCashInventoryRepository.findAllByOrderByDenominationDesc()
        val inventoryMap = inventory.associate { it.denomination to it.quantity }
        val totalCashInAtm = inventory.sumOf { it.denomination * it.quantity }

        val prompt = buildPrompt(
            date = today.toString(),
            withdrawalCount = withdrawals.size,
            depositCount = deposits.size,
            totalWithdrawn = withdrawals.sumOf { it.amount },
            totalDeposited = deposits.sumOf { it.amount },
            totalAmountMoved = totalAmountMoved,
            inventoryMap = inventoryMap,
            totalCashInAtm = totalCashInAtm
        )

        val aiSummary = try {
            chatClient.prompt()
                .user(prompt)
                .call()
                .content() ?: "Resumo não disponível no momento."
        } catch (e: Exception) {
            "Serviço de IA indisponível: ${e.message}"
        }

        return DailySummaryResponse(
            date = today.toString(),
            totalWithdrawals = withdrawals.size,
            totalDeposits = deposits.size,
            totalAmountMoved = totalAmountMoved,
            currentInventory = inventoryMap,
            aiSummary = aiSummary
        )
    }

    private fun buildPrompt(
        date: String,
        withdrawalCount: Int,
        depositCount: Int,
        totalWithdrawn: BigDecimal,
        totalDeposited: BigDecimal,
        totalAmountMoved: BigDecimal,
        inventoryMap: Map<Int, Int>,
        totalCashInAtm: Int
    ): String {
        val inventoryStatus = inventoryMap.entries
            .sortedByDescending { it.key }
            .joinToString(", ") { (denom, qty) -> "R$${denom}: ${qty} notas" }

        val lowStockWarning = inventoryMap.entries
            .filter { it.value < 5 }
            .joinToString(", ") { (denom, qty) -> "R$${denom} (${qty} notas)" }

        return """
            Você é um sistema de relatório de caixa eletrônico. Gere um resumo executivo claro e profissional em português do dia $date.

            DADOS DO DIA:
            - Saques realizados: $withdrawalCount operações totalizando R$${totalWithdrawn}
            - Depósitos realizados: $depositCount operações totalizando R$${totalDeposited}
            - Volume total movimentado: R$${totalAmountMoved}

            ESTADO ATUAL DO CAIXA:
            - Estoque: $inventoryStatus
            - Total em caixa: R$${totalCashInAtm}
            ${if (lowStockWarning.isNotEmpty()) "- ATENÇÃO - Estoque baixo (menos de 5 notas): $lowStockWarning" else "- Estoque em níveis normais"}

            Gere um resumo executivo de 3 a 5 frases destacando os pontos mais relevantes do dia, incluindo volume de operações, movimentação financeira e situação do estoque. Se houver estoque baixo, mencione como prioridade.
        """.trimIndent()
    }
}

package com.smartatm.adapter.input.rest

import com.smartatm.application.DepositService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

data class DepositRequest(
    val userId: Long,
    val notes: Map<Int, Int>
)

data class DepositResponse(
    val userId: Long,
    val amountDeposited: BigDecimal,
    val newBalance: BigDecimal,
    val message: String = "Depósito realizado com sucesso"
)

@RestController
@RequestMapping("/deposit")
@Tag(name = "ATM", description = "Operações do caixa eletrônico")
class DepositController(
    private val depositService: DepositService
) {

    @PostMapping
    @Operation(
        summary = "Realizar depósito",
        description = "Aceita notas de R\$2, R\$5, R\$10 e R\$50. Informe um mapa de denominação → quantidade."
    )
    fun deposit(@RequestBody request: DepositRequest): ResponseEntity<DepositResponse> {
        val totalDeposited = request.notes.entries.sumOf { (denom, qty) -> denom.toBigDecimal() * qty.toBigDecimal() }
        val newBalance = depositService.deposit(request.userId, request.notes)
        return ResponseEntity.ok(
            DepositResponse(
                userId = request.userId,
                amountDeposited = totalDeposited,
                newBalance = newBalance
            )
        )
    }
}

package com.smartatm.adapter.input.rest

import com.smartatm.application.WithdrawService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class WithdrawRequest(
    val userId: Long,
    val amount: Long
)

data class WithdrawResponse(
    val userId: Long,
    val amountWithdrawn: Long,
    val notesDispensed: Map<Int, Int>,
    val message: String = "Saque realizado com sucesso"
)

@RestController
@RequestMapping("/withdraw")
@Tag(name = "ATM", description = "Operações do caixa eletrônico")
class WithdrawController(
    private val withdrawService: WithdrawService
) {

    @PostMapping
    @Operation(
        summary = "Realizar saque",
        description = "Dispensa notas priorizando as de maior valor (R\$50 → R\$10 → R\$5 → R\$2)"
    )
    fun withdraw(@RequestBody request: WithdrawRequest): ResponseEntity<WithdrawResponse> {
        val notes = withdrawService.withdraw(request.userId, request.amount)
        return ResponseEntity.ok(
            WithdrawResponse(
                userId = request.userId,
                amountWithdrawn = request.amount,
                notesDispensed = notes
            )
        )
    }
}

package com.smartatm.adapter.input.rest

import com.smartatm.application.DailySummaryResponse
import com.smartatm.application.DailySummaryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/daily-summary")
@Tag(name = "ATM", description = "Operações do caixa eletrônico")
class DailySummaryController(
    private val dailySummaryService: DailySummaryService
) {

    @GetMapping
    @Operation(
        summary = "Resumo diário gerado por IA",
        description = "Agrega as transações do dia e gera uma narrativa executiva usando IA"
    )
    fun getDailySummary(): ResponseEntity<DailySummaryResponse> {
        return ResponseEntity.ok(dailySummaryService.generateDailySummary())
    }
}

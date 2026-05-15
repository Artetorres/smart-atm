package com.smartatm.adapter.input.rest

import com.smartatm.domain.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val detail: Map<String, Any>? = null
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException::class)
    fun handleInsufficientBalance(ex: InsufficientBalanceException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            ErrorResponse(422, "Insufficient Balance", ex.message ?: "Saldo insuficiente")
        )

    @ExceptionHandler(InsufficientNotesException::class)
    fun handleInsufficientNotes(ex: InsufficientNotesException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            ErrorResponse(
                status = 422,
                error = "Insufficient Notes",
                message = ex.message ?: "Caixa não consegue dispensar o valor exato",
                detail = mapOf("maxDispensableAmount" to ex.maxDispensableAmount)
            )
        )

    @ExceptionHandler(AtmOutOfCashException::class)
    fun handleAtmOutOfCash(ex: AtmOutOfCashException) =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ErrorResponse(503, "ATM Out of Cash", ex.message ?: "Caixa sem notas")
        )

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(404, "User Not Found", ex.message ?: "Usuário não encontrado")
        )

    @ExceptionHandler(InvalidAmountException::class)
    fun handleInvalidAmount(ex: InvalidAmountException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(400, "Invalid Amount", ex.message ?: "Valor inválido")
        )

    @ExceptionHandler(InvalidDenominationException::class)
    fun handleInvalidDenomination(ex: InvalidDenominationException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(400, "Invalid Denomination", ex.message ?: "Denominação inválida")
        )
}

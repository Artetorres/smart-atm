package com.smartatm.domain

class InsufficientBalanceException(userId: Long, requested: Long, available: Long) :
    RuntimeException("User $userId has insufficient balance. Requested: R$$requested, Available: R$$available")

class InsufficientNotesException(
    val maxDispensableAmount: Long,
    requested: Long
) : RuntimeException("ATM cannot dispense R$$requested. Max dispensable amount: R$$maxDispensableAmount")

class AtmOutOfCashException :
    RuntimeException("ATM is out of cash")

class UserNotFoundException(userId: Long) :
    RuntimeException("User with id $userId not found")

class InvalidAmountException(amount: Long) :
    RuntimeException("Invalid amount: R$$amount. Amount must be positive")

class InvalidDenominationException(denomination: Int) :
    RuntimeException("Invalid denomination: R$$denomination. Accepted: R$2, R$5, R$10, R$50")

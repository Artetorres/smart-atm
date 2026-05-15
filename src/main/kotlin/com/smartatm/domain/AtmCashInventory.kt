package com.smartatm.domain

import jakarta.persistence.*

@Entity
@Table(name = "atm_cash_inventory")
data class AtmCashInventory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val denomination: Int,

    @Column(nullable = false)
    var quantity: Int
)

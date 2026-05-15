package com.smartatm.domain

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "users")
data class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    var balance: BigDecimal = BigDecimal.ZERO
)

package com.example.exchanger.domain

import lombok.Data
import org.springframework.data.annotation.Id

@Data
data class User(
    @Id
    var id: String? = null,
    var name: String? = null,
    var balance: Long? = null,
    var stocks: MutableMap<String, Int>? = null
)
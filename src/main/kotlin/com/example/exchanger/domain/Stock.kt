package com.example.exchanger.domain

import lombok.Data
import org.springframework.data.annotation.Id

@Data
data class Stock(
    @Id
    var id: String? = null,
    var companyId: String? = null,
    var name: String? = null,
    var price: Long? = null
)
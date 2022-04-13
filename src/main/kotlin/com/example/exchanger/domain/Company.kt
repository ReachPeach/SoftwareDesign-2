package com.example.exchanger.domain

import lombok.Data
import org.springframework.data.annotation.Id
import org.springframework.web.bind.annotation.Mapping

@Data
data class Company(
    @Id
    var id: String? = null,
    var name: String? = null,
    var stocks: MutableMap<String, Int>? = null
)
package com.example.exchanger.repository

import com.example.exchanger.domain.Stock
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface StockRepository : MongoRepository<Stock?, String?>
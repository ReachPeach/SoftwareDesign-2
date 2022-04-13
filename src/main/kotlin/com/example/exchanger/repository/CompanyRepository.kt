package com.example.exchanger.repository

import com.example.exchanger.domain.Company
import org.springframework.data.mongodb.repository.MongoRepository

interface CompanyRepository : MongoRepository<Company?, String?>
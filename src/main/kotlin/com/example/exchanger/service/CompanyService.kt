package com.example.exchanger.service

import com.example.exchanger.domain.Company
import com.example.exchanger.domain.Stock
import com.example.exchanger.repository.CompanyRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CompanyService @Autowired constructor(private val repository: CompanyRepository) {
    fun all(): List<Company?> {
        return repository.findAll()
    }

    fun getById(id: String): Company {
        return repository.findById(id).orElseThrow { NoSuchElementException() }!!
    }

    fun deleteById(id: String): MutableSet<String> {
        val deleted = getById(id).stocks!!.keys
        repository.deleteById(id)
        return deleted
    }

    fun save(company: Company): Company {
        return repository.save(company)
    }

    fun updateFreedStocks(freedStocks: Map<String, Int>) {
        all().forEach { company ->
            var needOverride = false
            freedStocks.forEach {
                if (company!!.stocks!!.contains(it.key)) {
                    company.stocks!![it.key] = company.stocks!![it.key]!! + it.value
                    needOverride = true
                }
            }
            if (needOverride) save(company!!)
        }
    }

    fun deleteStock(stockId: String) {
        all().forEach { company ->
            if (company!!.stocks!!.contains(stockId)) {
                company.stocks!!.remove(stockId)
                save(company)
            }
        }
    }

    fun cellStock(stockId: String) {
        all().forEach { company ->
            if (company!!.stocks!!.contains(stockId)) {
                if (company.stocks!![stockId] == 0) throw IllegalStateException("All stocks are already celled")
                company.stocks!![stockId] = company.stocks!![stockId]!! - 1
                save(company)
            }
        }
    }

    fun buyStock(stockId: String) {
        all().forEach { company ->
            if (company!!.stocks!!.contains(stockId)) {
                company.stocks!![stockId] = company.stocks!![stockId]!! + 1
                save(company)
            }
        }
    }

    fun saveStock(stock: Stock) {
        val company = getById(stock.companyId!!)
        if (!company.stocks!!.contains(stock.id)) company.stocks!![stock.id!!] = 0
        company.stocks!![stock.id!!] = company.stocks!![stock.id!!]!! + 1
        save(company)
    }
}
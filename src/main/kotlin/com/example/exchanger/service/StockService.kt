package com.example.exchanger.service

import com.example.exchanger.domain.Stock
import com.example.exchanger.repository.StockRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.stream.Collectors
import kotlin.random.Random

@Service
class StockService @Autowired constructor(private val repository: StockRepository) {
    fun all(): List<Stock?> {
        return repository.findAll()
    }

    fun getById(id: String): Stock {
        return repository.findById(id).orElseThrow { NoSuchElementException() }!!
    }

    fun allByCompanyId(companyId: String): MutableList<Stock?>? {
        val found = repository.findAll().stream()
            .filter { stock: Stock? -> stock!!.companyId == companyId }
            .collect(Collectors.toList())
        if (found.isEmpty()) throw NoSuchElementException()
        return found
    }

    fun deleteById(id: String) {
        repository.deleteById(id)
    }

    fun deleteAllByCompanyId(companyId: String): MutableList<String>? {
        val deletingIds = allByCompanyId(companyId)!!.stream().map { obj: Stock? -> obj!!.id!! }
            .collect(Collectors.toList())
        repository.deleteAllById(deletingIds)
        return deletingIds
    }

    fun saveStock(stock: Stock): Stock {
        return repository.save(stock)
    }

    fun updateStock(stockId: String, delta: Long) {
        val stock = getById(stockId)
        stock.price = stock.price!! + delta
        saveStock(stock)
    }

    fun updateStocks(stockId: MutableSet<String>, delta: Long) {
        stockId.forEach { updateStock(it, delta) }
    }

    fun updateAllStocks() {
        all().forEach { updateStock(it!!.id!!, Random.nextLong(0, 1000)) }
    }

    fun updateAllStocks(delta: Long) {
        all().forEach { updateStock(it!!.id!!, delta) }
    }

    fun getTotalPrice(stocks: MutableMap<String, Int>): Long {
        var sum: Long = 0
        stocks.forEach { sum += getById(it.key).price!! * it.value }
        return sum
    }
}
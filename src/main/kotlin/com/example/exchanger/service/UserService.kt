package com.example.exchanger.service

import com.example.exchanger.domain.User
import com.example.exchanger.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserService @Autowired constructor(private val repository: UserRepository) {
    fun all(): List<User?> {
        return repository.findAll()
    }

    fun getById(id: String): User {
        return repository.findById(id).orElseThrow { NoSuchElementException() }!!
    }

    fun deleteById(id: String): MutableMap<String, Int> {
        val freedStocks = getById(id).stocks!!
        repository.deleteById(id)
        return freedStocks
    }

    fun save(user: User): User {
        return repository.save(user)
    }

    fun deleteStock(stockId: String) {
        all().forEach { user ->
            if (user!!.stocks!!.contains(stockId)) {
                user.stocks!!.remove(stockId)
                save(user)
            }
        }
    }

    fun deleteStocks(deletedStocks: MutableSet<String>) {
        all().forEach { user ->
            if (user!!.stocks!!.keys.stream().anyMatch { deletedStocks.contains(it) }) {
                deletedStocks.forEach { user.stocks!!.remove(it) }
                save(user)
            }
        }
    }

    fun changeBalance(id: String, delta: Long) {
        val user = getById(id)
        user.balance = user.balance!! + delta
        save(user)
    }

    fun buyStock(userId: String, stockId: String, price: Long) {
        val user = getById(userId)
        if (user.balance!! < price) throw IllegalStateException("Given user doesn't have enough balance")
        user.balance = user.balance!! - price
        user.stocks!![stockId] = user.stocks!!.getOrDefault(stockId, 0) + 1
        save(user)
    }

    fun cellStock(userId: String, stockId: String, price: Long) {
        val user = getById(userId)
        user.balance = user.balance!! + price
        user.stocks!![stockId] = user.stocks!![stockId]!! - 1
        if (user.stocks!![stockId]!! == 0) user.stocks!!.remove(stockId)
        save(user)
    }
}
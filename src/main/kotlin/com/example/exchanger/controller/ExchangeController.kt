package com.example.exchanger.controller

import com.example.exchanger.domain.Company
import com.example.exchanger.domain.Stock
import com.example.exchanger.domain.User
import com.example.exchanger.service.CompanyService
import com.example.exchanger.service.StockService
import com.example.exchanger.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
class ExchangeController @Autowired constructor(
    private val stockService: StockService,
    private val userService: UserService,
    private val companyService: CompanyService
) {
    // Listing
    @GetMapping("/stocks")
    fun allStocks(): List<Stock?>? {
        return stockService.all()
    }

    @GetMapping("/companies")
    fun allCompanies(): List<Company?>? {
        return companyService.all()
    }

    @GetMapping("/users")
    fun allUsers(): List<User?>? {
        return userService.all()
    }

    @GetMapping("/stock/{id}")
    fun stock(@PathVariable id: String): Stock {
        return stockService.getById(id)
    }

    @GetMapping("/company/{id}")
    fun company(@PathVariable id: String): Company {
        return companyService.getById(id)
    }

    @GetMapping("/user/{id}")
    fun user(@PathVariable id: String): User {
        return userService.getById(id)
    }

    // Adding
    @PostMapping("/stock")
    fun addStock(@RequestBody stock: Stock): Stock? {
        companyService.saveStock(stock)
        return stockService.saveStock(stock)
    }

    @PostMapping("/company")
    fun addCompany(@RequestBody company: Company): Company? {
        return companyService.save(company)
    }

    @PostMapping("/user")
    fun addUser(@RequestBody user: User): User {
        return userService.save(user)
    }

    // Deleting
    @Transactional
    @DeleteMapping("/user/{id}")
    fun deleteUser(@PathVariable id: String) {
        val freedStocks = userService.deleteById(id)
        companyService.updateFreedStocks(freedStocks)
    }

    @Transactional
    @DeleteMapping("/stock/{id}")
    fun deleteStock(@PathVariable id: String) {
        val deletingStock = stockService.getById(id)
        userService.deleteStock(deletingStock.id!!)
        companyService.deleteStock(deletingStock.id!!)
        stockService.deleteById(id)
    }

    @Transactional
    @DeleteMapping("/company/{id}")
    fun deleteCompany(@PathVariable id: String) {
        val deletedStocks = companyService.deleteById(id)
        stockService.deleteAllByCompanyId(id)
        userService.deleteStocks(deletedStocks)
    }

    // Manipulating
    @PostMapping("/user/{id}:{delta}")
    fun changeBalance(@PathVariable delta: String, @PathVariable id: String): Long? {
        delta.toLongOrNull()?.also {
            if (it > 0) {
                userService.changeBalance(id, it)
            }
        }
        return userService.getById(id).balance
    }

    @Transactional
    @GetMapping("/user/{id}/total")
    fun getStocksValue(@PathVariable id: String): Long {
        val stocks = userService.getById(id).stocks!!
        return stockService.getTotalPrice(stocks)
    }

    @Transactional
    @PostMapping("/user/{uId}/buy/{sId}")
    fun buyStockByUser(@PathVariable uId: String, @PathVariable sId: String) {
        val stock = stockService.getById(sId)
        companyService.cellStock(sId)
        userService.buyStock(uId, sId, stock.price!!)
    }

    @Transactional
    @PostMapping("/user/{uId}/cell/{sId}")
    fun cellStockByUser(@PathVariable uId: String, @PathVariable sId: String) {
        val stock = stockService.getById(sId)
        companyService.buyStock(sId)
        userService.cellStock(uId, sId, stock.price!!)
    }

    // ADMIN
    @PostMapping("/admin/change-stock/{id}:{delta}")
    fun changeSingleCourse(@PathVariable id: String, @PathVariable delta: String) {
        delta.toLongOrNull()?.also {
            stockService.updateStock(id, it)
        }
    }

    @PostMapping("/admin/change-company/{id}:{delta}")
    fun changeCompanyCourses(@PathVariable id: String, @PathVariable delta: String) {
        delta.toLongOrNull()?.also {
            val stockIds = companyService.getById(id).stocks!!.keys
            stockService.updateStocks(stockIds, it)
        }
    }

    @PostMapping("/admin/change-every/{delta}")
    fun changeAllCourses(@PathVariable delta: String) {
        delta.toLongOrNull()?.also {
            stockService.updateAllStocks(it)
        }
    }

    @PostMapping("/admin/change-random")
    fun changeRandomlyAll() {
        stockService.updateAllStocks()
    }
}
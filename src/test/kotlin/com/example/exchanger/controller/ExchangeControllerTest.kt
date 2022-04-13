package com.example.exchanger.controller

import com.example.exchanger.domain.Company
import com.example.exchanger.domain.Stock
import com.example.exchanger.domain.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.env.MapPropertySource
import org.springframework.http.HttpMethod
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.HttpServerErrorException
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(initializers = [ExchangeControllerTest.Initializer::class])
class ExchangeControllerTest {
    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            val mongoDBContainer = MongoDBContainer("mongo:5.0").withExposedPorts(PORT)
            mongoDBContainer.start()
            val properties: MutableMap<String, Any> = HashMap()
            properties["spring.data.mongodb.host"] = mongoDBContainer.containerIpAddress
            properties["spring.data.mongodb.port"] = mongoDBContainer.getMappedPort(PORT)
            applicationContext.environment.propertySources
                .addLast(MapPropertySource("TestConfigProperties", properties))
        }
    }

    @Autowired
    private val restTemplateBuilder: RestTemplateBuilder? = null

    private fun assertCompaniesEqual(company1: Company, company2: Company) {
        Assertions.assertEquals(company1.id, company2.id)
        Assertions.assertEquals(company1.name, company2.name)
        Assertions.assertEquals(company1.stocks!!.size, company2.stocks!!.size)
        company1.stocks!!.forEach { (key, value) ->
            Assertions.assertTrue(company2.stocks!!.contains(key))
            Assertions.assertEquals(value, company2.stocks!![key])
        }
    }

    private fun assertStocksEqual(stock1: Stock, stock2: Stock) {
        Assertions.assertEquals(stock1.id, stock2.id)
        Assertions.assertEquals(stock1.name, stock2.name)
        Assertions.assertEquals(stock1.companyId, stock2.companyId)
        Assertions.assertEquals(stock1.price, stock2.price)
    }

    private fun assertUsersEqual(user1: User, user2: User) {
        Assertions.assertEquals(user1.id, user2.id)
        Assertions.assertEquals(user1.name, user2.name)
        Assertions.assertEquals(user1.balance, user2.balance)
        Assertions.assertEquals(user1.stocks!!.size, user2.stocks!!.size)
        user1.stocks!!.forEach { (key, value) ->
            Assertions.assertTrue(user2.stocks!!.contains(key))
            Assertions.assertEquals(value, user2.stocks!![key])
        }
    }

    @Tag("adding")
    @Test
    fun addingCompany() {
        val restTemplate = restTemplateBuilder!!.build()
        val company = Company("company", "named", mutableMapOf())

        val actualCompany: Company? = restTemplate.postForEntity(
            "$HTTP_LOCALHOST_8080/company", company,
            Company::class.java
        ).body

        Assertions.assertNotNull(actualCompany)
        Assertions.assertEquals(actualCompany!!.id, company.id)
        Assertions.assertEquals(actualCompany.name, company.name)
        Assertions.assertEquals(actualCompany.stocks!!.size, company.stocks!!.size)
    }

    @Tag("adding")
    @Test
    fun addingStock() {
        val restTemplate = restTemplateBuilder!!.build()
        val stock = Stock("stock", "company", "name", 0)

        val company = Company("company", "named", mutableMapOf())

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/company", company, Company::class.java)

        val actualStock: Stock? = restTemplate.postForEntity(
            "$HTTP_LOCALHOST_8080/stock", stock,
            Stock::class.java
        ).body

        Assertions.assertNotNull(actualStock)
        Assertions.assertEquals(actualStock!!.id, stock.id)
        Assertions.assertEquals(actualStock.companyId, stock.companyId)
        Assertions.assertEquals(actualStock.name, stock.name)
        Assertions.assertEquals(actualStock.price, stock.price)
        restTemplate.delete("$HTTP_LOCALHOST_8080/company/${company.id}")
    }

    @Tag("adding")
    @Test
    fun addingUser() {
        val restTemplate = restTemplateBuilder!!.build()
        val user = User("id", "name", 0, mutableMapOf())

        val actualUser: User? = restTemplate.postForEntity(
            "$HTTP_LOCALHOST_8080/user", user,
            User::class.java
        ).body

        Assertions.assertNotNull(actualUser)
        assertUsersEqual(actualUser!!, user)
        restTemplate.delete("$HTTP_LOCALHOST_8080/user/${user.id}")
    }

    @Order(1)
    @Tag("getting")
    @Test
    fun testGettingNonUsers() {
        val restTemplate = restTemplateBuilder!!.build()

        val company1 = Company("cid1", "MMM", mutableMapOf())
        val company2 = Company("cid2", "WWW", mutableMapOf())

        val stock1 = Stock("sid1", "cid1", "lie", 555)
        val stock2 = Stock("sid2", "cid1", "false", 333)
        val stock3 = Stock("sid3", "cid1", "deception", 111)

        val stock4 = Stock("sid4", "cid2", "truth", 1000)
        val stock5 = Stock("sid5", "cid2", "honor", 777)

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/company", company1, Company::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/company", company2, Company::class.java)

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock1, Stock::class.java)
        for (i in 1..2)
            restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock2, Stock::class.java)

        for (i in 1..3)
            restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock3, Stock::class.java)

        for (i in 1..2)
            restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock4, Stock::class.java)

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock5, Stock::class.java)
        company1.stocks!![stock1.id!!] = 1
        company1.stocks!![stock2.id!!] = 2
        company1.stocks!![stock3.id!!] = 3

        company2.stocks!![stock4.id!!] = 2
        company2.stocks!![stock5.id!!] = 1

        val typeReference1: ParameterizedTypeReference<List<Company>> =
            object : ParameterizedTypeReference<List<Company>>() {}
        val companies: List<Company> = restTemplate.exchange(
            "$HTTP_LOCALHOST_8080/companies",
            HttpMethod.GET,
            null,
            typeReference1
        ).body!!

        Assertions.assertNotNull(companies)
        assertCompaniesEqual(companies[0], company1)
        assertCompaniesEqual(companies[1], company2)

        val typeReference2: ParameterizedTypeReference<List<Stock>> =
            object : ParameterizedTypeReference<List<Stock>>() {}
        val stocks: List<Stock> = restTemplate.exchange(
            "$HTTP_LOCALHOST_8080/stocks",
            HttpMethod.GET,
            null,
            typeReference2
        ).body!!

        assertStocksEqual(stocks[0], stock1)
        assertStocksEqual(stocks[1], stock2)
        assertStocksEqual(stocks[2], stock3)
        assertStocksEqual(stocks[3], stock4)
        assertStocksEqual(stocks[4], stock5)
    }

    @Order(1)
    @Tag("getting")
    @Test
    fun testGettingUsers() {
        val restTemplate = restTemplateBuilder!!.build()

        val user1 = User("uid1", "bala", 0, mutableMapOf())
        val user2 = User("uid2", "ance", 1000, mutableMapOf())

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user", user1, User::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user", user2, User::class.java)

        val typeReference: ParameterizedTypeReference<List<User>> =
            object : ParameterizedTypeReference<List<User>>() {}
        val users: List<User> = restTemplate.exchange(
            "$HTTP_LOCALHOST_8080/users",
            HttpMethod.GET,
            null,
            typeReference
        ).body!!

        Assertions.assertNotNull(users)
        assertUsersEqual(users[0], user1)
        assertUsersEqual(users[1], user2)
    }

    @Tag("balance")
    @Test
    fun refillBalance() {
        val restTemplate = restTemplateBuilder!!.build()
        val user = User("uid1", "bala", 0, mutableMapOf())
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user", user, User::class.java)
        val balance =
            restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}:600", null, Long::class.java).body

        Assertions.assertNotNull(balance)
        Assertions.assertEquals(balance, 600)
    }

    @Tag("buying")
    @Test
    fun buyStockSuccessfully() {
        val restTemplate = restTemplateBuilder!!.build()
        val user = User("uid1", "bala", 0, mutableMapOf())
        val company = Company("cid1", "MMM", mutableMapOf())
        val stock = Stock("sid1", "cid1", "lie", 500)

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/company", company, Company::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock, Stock::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user", user, User::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}:600", null, Long::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/buy/${stock.id}", null, Void::class.java)

        val actualCompany =
            restTemplate.getForEntity("$HTTP_LOCALHOST_8080/company/${company.id}", Company::class.java).body
        val actualUser =
            restTemplate.getForEntity("$HTTP_LOCALHOST_8080/user/${user.id}", User::class.java).body

        Assertions.assertNotNull(actualUser)
        Assertions.assertNotNull(actualCompany)
        Assertions.assertEquals(actualUser!!.balance, 100)
        Assertions.assertEquals(actualCompany!!.stocks!![stock.id], 0)

        val userActives = restTemplate.getForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/total", Long::class.java).body
        Assertions.assertNotNull(userActives)
        Assertions.assertEquals(userActives, 500)
    }

    @Tag("buying")
    @Test
    fun buyStockUnsuccessfully() {
        val restTemplate = restTemplateBuilder!!.build()
        val user = User("uid1", "bala", 0, mutableMapOf())
        val company = Company("cid1", "MMM", mutableMapOf())
        val stock = Stock("sid1", "cid1", "lie", 1000)

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/company", company, Company::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock, Stock::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user", user, User::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}:200", null, Long::class.java)
        Assertions.assertThrows(
            HttpServerErrorException::class.java
        ) {
            restTemplate.postForEntity(
                "$HTTP_LOCALHOST_8080/user/${user.id}/buy/${stock.id}",
                null,
                Void::class.java
            )
        }
    }

    @Tag("market-up")
    @Test
    fun stockGrowth() {
        val restTemplate = restTemplateBuilder!!.build()
        val user = User("uid1", "bala", 1000, mutableMapOf())
        val company = Company("cid1", "MMM", mutableMapOf())
        val stock = Stock("sid1", "cid1", "lie", 1000)

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/company", company, Company::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock, Stock::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user", user, User::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/buy/${stock.id}", null, Void::class.java)

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/admin/change-stock/${stock.id}:1000", null, Void::class.java)
        val actualStock = restTemplate.getForEntity("$HTTP_LOCALHOST_8080/stock/${stock.id}", Stock::class.java).body
        val userActives = restTemplate.getForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/total", Long::class.java).body

        Assertions.assertNotNull(actualStock)
        Assertions.assertNotNull(userActives)
        Assertions.assertEquals(userActives, 2000)
        Assertions.assertEquals(actualStock!!.price, 2000)
    }

    @Tag("market-up")
    @Test
    fun companyGrowth() {
        val restTemplate = restTemplateBuilder!!.build()
        val user = User("uid1", "bala", 1000, mutableMapOf())
        val company = Company("cid1", "MMM", mutableMapOf())
        val stock1 = Stock("sid1", "cid1", "lie", 500)
        val stock2 = Stock("sid1", "cid1", "lie", 500)

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/company", company, Company::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock1, Stock::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock2, Stock::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user", user, User::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/buy/${stock1.id}", null, Void::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/buy/${stock2.id}", null, Void::class.java)

        restTemplate.postForEntity(
            "$HTTP_LOCALHOST_8080/admin/change-company/${company.id}:500",
            null,
            Void::class.java
        )

        val userActives = restTemplate.getForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/total", Long::class.java).body

        Assertions.assertNotNull(userActives)
        Assertions.assertEquals(userActives, 2000)
    }

    @Tag("market-up")
    @Test
    fun userCellsGoods() {
        val restTemplate = restTemplateBuilder!!.build()
        val user = User("uid1", "bala", 2000, mutableMapOf())
        val company = Company("cid1", "MMM", mutableMapOf())
        val stock1 = Stock("sid1", "cid1", "lie", 500)
        val stock2 = Stock("sid1", "cid1", "lie", 500)

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/company", company, Company::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock1, Stock::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock2, Stock::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user", user, User::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/buy/${stock1.id}", null, Void::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/buy/${stock2.id}", null, Void::class.java)

        restTemplate.postForEntity(
            "$HTTP_LOCALHOST_8080/admin/change-company/${company.id}:500",
            null,
            Void::class.java
        )

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/cell/${stock1.id}", null, Void::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/cell/${stock2.id}", null, Void::class.java)

        val actualUser = restTemplate.getForEntity("$HTTP_LOCALHOST_8080/user/${user.id}", User::class.java).body
        Assertions.assertNotNull(actualUser)
        Assertions.assertEquals(3000, actualUser!!.balance)
    }

    @Tag("exit-market")
    @Test
    fun userLeftsMarket() {
        val restTemplate = restTemplateBuilder!!.build()
        val user = User("uid1", "bala", 2000, mutableMapOf())
        val company = Company("cid1", "MMM", mutableMapOf())
        val stock = Stock("sid1", "cid1", "lie", 500)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/company", company, Company::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock, Stock::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user", user, User::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/buy/${stock.id}", null, Void::class.java)

        restTemplate.delete("$HTTP_LOCALHOST_8080/user/${user.id}")
        val actualCompany =
            restTemplate.getForEntity("$HTTP_LOCALHOST_8080/company/${company.id}", Company::class.java).body
        Assertions.assertNotNull(actualCompany)
        Assertions.assertEquals(actualCompany!!.stocks!![stock.id], 1)
    }

    @Tag("exit-market")
    @Test
    fun companyLeftsMarket() {
        val restTemplate = restTemplateBuilder!!.build()
        val user = User("uid1", "bala", 2000, mutableMapOf())
        val company = Company("cid1", "MMM", mutableMapOf())
        val stock1 = Stock("sid1", "cid1", "lie", 500)
        val stock2 = Stock("sid2", "cid1", "pie", 500)

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/company", company, Company::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock1, Stock::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock2, Stock::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user", user, User::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/buy/${stock1.id}", null, Void::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/buy/${stock2.id}", null, Void::class.java)

        restTemplate.delete("$HTTP_LOCALHOST_8080/company/${company.id}")
        val actualUser =
            restTemplate.getForEntity("$HTTP_LOCALHOST_8080/user/${user.id}", User::class.java).body
        Assertions.assertNotNull(actualUser)
        Assertions.assertTrue(actualUser!!.stocks!!.isEmpty())
        Assertions.assertEquals(1000, actualUser.balance)
        val userActives = restTemplate.getForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/total", Long::class.java).body
        Assertions.assertNotNull(userActives)
        Assertions.assertEquals(0, userActives)
    }

    @Tag("exit-market")
    @Test
    fun stockLeftsMarket() {
        val restTemplate = restTemplateBuilder!!.build()
        val user = User("uid1", "bala", 2000, mutableMapOf())
        val company = Company("cid1", "MMM", mutableMapOf())
        val stock1 = Stock("sid1", "cid1", "lie", 500)
        val stock2 = Stock("sid2", "cid1", "pie", 500)

        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/company", company, Company::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock1, Stock::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/stock", stock2, Stock::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user", user, User::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/buy/${stock1.id}", null, Void::class.java)
        restTemplate.postForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/buy/${stock2.id}", null, Void::class.java)

        restTemplate.delete("$HTTP_LOCALHOST_8080/stock/${stock1.id}")

        val actualUser =
            restTemplate.getForEntity("$HTTP_LOCALHOST_8080/user/${user.id}", User::class.java).body
        Assertions.assertNotNull(actualUser)
        Assertions.assertEquals(1, actualUser!!.stocks!!.size)
        Assertions.assertEquals(1, actualUser.stocks!![stock2.id])

        val userActives = restTemplate.getForEntity("$HTTP_LOCALHOST_8080/user/${user.id}/total", Long::class.java).body
        Assertions.assertNotNull(userActives)
        Assertions.assertEquals(500, userActives)

        val actualCompany =
            restTemplate.getForEntity("$HTTP_LOCALHOST_8080/company/${company.id}", Company::class.java).body
        Assertions.assertNotNull(actualCompany)
        Assertions.assertEquals(1, actualCompany!!.stocks!!.size)
        Assertions.assertEquals(0, actualCompany.stocks!![stock2.id])
    }


    companion object {
        const val PORT = 27017
        const val HTTP_LOCALHOST_8080 = "http://localhost:8080"
    }
}
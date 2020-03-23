package volkov.stock.cabinet

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import volkov.stock.cabinet.container.KFixedHostPortGenericContainer
import volkov.stock.cabinet.container.MongodbReactiveClientBase
import volkov.stock.cabinet.dto.CreateAccount
import volkov.stock.cabinet.dto.Operation
import volkov.stock.cabinet.dto.Payment
import volkov.stock.cabinet.dto.TradeOperation
import volkov.stock.cabinet.model.Account
import volkov.stock.cabinet.repository.MongoAccountRepository
import java.net.URI
import java.util.*


fun main() = runBlocking {
    val mongoContainer = MongodbReactiveClientBase()

    val stockEmulator = KFixedHostPortGenericContainer("emulator:1.0")
        .withFixedExposedPort(8080, 8080)
        .withExposedPorts(8080)
    stockEmulator.start()

    val mongoClient = mongoContainer.createMongoClient()
    val stockClient = StockClient(URI.create("http://localhost:8080"))
    val repository = MongoAccountRepository(mongoClient.getDatabase("test"))

    suspend fun getAccount(accountId: UUID): Account? {
        val account = repository.getAccount(accountId)
        return if (account == null) {
            null
        } else {
            val updateStocks = account.stocks.map {
                val stock = stockClient.getStock(it.companyId, it.id)!!
                it.copy(price = stock.price)
            }
            val total = updateStocks.map { it.price * it.count }.sum()
            account.copy(totalPrice = total, stocks = updateStocks)
        }
    }

    val server = embeddedServer(Netty, port = 8081) {
        install(ContentNegotiation) {
            jackson {
                findAndRegisterModules()
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                setSerializationInclusion(JsonInclude.Include.NON_NULL);
                registerModule(JavaTimeModule())
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        }
        routing {
            post("/account") {
                val request = call.receive<CreateAccount>()
                val account = Account(UUID.randomUUID(), request.name)

                repository.addAccount(account)
                call.respond(account)
            }
            get("/account/{accountId}") {
                val accountId = call.parameters["accountId"]!!.let(UUID::fromString)
                val account = getAccount(accountId)
                if (account == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(account)
                }
            }
            post("/account/{accountId}/payment") {
                val payment = call.receive<Payment>()
                val accountId = call.parameters["accountId"]!!.let(UUID::fromString)

                repository.payment(accountId, payment.sum)
                val account = getAccount(accountId)!!

                call.respond(account)
            }
            post("/account/{accountId}/stock") {
                val op = call.receive<TradeOperation>()
                val accountId = call.parameters["accountId"]!!.let(UUID::fromString)

                if (op.type == Operation.BUY) {
                    val stock = stockClient.buyStock(op.companyId, op.stockId, op.count)
                    repository.hold(accountId, stock.price * op.count)
                    repository.addStock(accountId, stock.copy(count = op.count))
                } else {
                    val stock = stockClient.sellStock(op.companyId, op.stockId, op.count)
                    repository.payment(accountId, stock.price * op.count)
                    repository.deleteStock(accountId, stock.copy(count = op.count))
                }

                val account = getAccount(accountId)
                if (account == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(account)
                }
            }
        }
    }.start(wait = true)
}
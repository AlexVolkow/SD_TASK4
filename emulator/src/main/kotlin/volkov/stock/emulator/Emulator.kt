package volkov.stock.emulator

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.mongodb.reactivestreams.client.MongoClients
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
import volkov.stock.emulator.dto.CreateCompany
import volkov.stock.emulator.dto.CreateStock
import volkov.stock.emulator.dto.Operation
import volkov.stock.emulator.dto.TradeOperation
import volkov.stock.emulator.model.Company
import volkov.stock.emulator.model.Stock
import volkov.stock.emulator.repository.InMemoryStockRepository
import java.util.*
import kotlin.random.Random


fun main() = runBlocking {
    val mongoClient = MongoClients.create()
    val repository = InMemoryStockRepository()

    val server = embeddedServer(Netty, port = 8080) {
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
            post("/company") {
                val request = call.receive<CreateCompany>()
                val company = Company(UUID.randomUUID(), request.name)

                repository.addCompany(company)
                call.respond(company)
            }
            get("/company/{companyId}") {
                val companyId = call.parameters["companyId"]!!.let(UUID::fromString)
                val company = repository.getCompany(companyId)
                if (company == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(company)
                }
            }
            post("/company/{companyId}/stock") {
                val request = call.receive<CreateStock>()
                val companyId = call.parameters["companyId"]!!.let(UUID::fromString)
                val stock = Stock(
                    id = UUID.randomUUID(),
                    companyId = companyId,
                    name = request.name,
                    count = request.count,
                    price = request.price
                )

                repository.addStock(stock)
                call.respond(stock)
            }
            get("/company/{companyId}/stock/{stockId}") {
                val companyId = call.parameters["companyId"]!!.let(UUID::fromString)
                val stockId = call.parameters["stockId"]!!.let(UUID::fromString)

                val stock = repository.getStock(stockId)
                if (stock != null) {
                    val newPrice = stock.price + Random.nextInt(-150, 150)
                    repository.updatePrice(stockId, newPrice)
                    call.respond(stock.copy(price = newPrice))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            post("/company/{companyId}/stock/{stockId}/trade") {
                val companyId = call.parameters["companyId"]!!.let(UUID::fromString)
                val stockId = call.parameters["stockId"]!!.let(UUID::fromString)

                val op = call.receive<TradeOperation>()

                val stock = repository.getStock(stockId)
                if (stock != null) {
                    if (stock.count < op.count) {
                        call.respond("Not enough stocks")
                        return@post
                    }
                    val count: Int
                    val price = if (op.type == Operation.BUY) {
                        count = -op.count
                        repository.buyStock(stockId, op.count)
                    } else {
                        count = op.count
                        repository.sellStock(stockId, op.count)
                    }
                    call.respond(stock.copy(price = price, count = stock.count + count))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }.start(wait = true)
}
package volkov.stock.cabinet

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.future.await
import volkov.stock.cabinet.model.Stock
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.*


class StockClient(
    private val host: URI
) {
    private val objectMapper = jacksonObjectMapper().apply {
        findAndRegisterModules()
    }

    private val client = HttpClient.newHttpClient();

    suspend fun getStock(companyId: UUID, stockId: UUID): Stock? {
        val url = host.resolve("/company/$companyId/stock/$stockId")

        val request = HttpRequest.newBuilder()
            .GET()
            .uri(url)
            .build()

        val response = client.sendAsync(request, BodyHandlers.ofString()).await().body()
        return objectMapper.readValue(response)
    }

    suspend fun buyStock(companyId: UUID, stockId: UUID, count: Int): Stock {
        val url = host.resolve("/company/$companyId/stock/$stockId/trade")

        val op = """
            {
            "type": "BUY",
            "count": $count
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(op))
            .header("Content-Type", "application/json")
            .uri(url)
            .build()

        val response = client.sendAsync(request, BodyHandlers.ofString()).await().body()
        return objectMapper.readValue(response)
    }

    suspend fun sellStock(companyId: UUID, stockId: UUID, count: Int): Stock {
        val url = host.resolve("/company/$companyId/stock/$stockId/trade")

        val op = """
            {
            "type": "SELL",
            "count": $count
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(op))
            .header("Content-Type", "application/json")
            .uri(url)
            .build()

        val response = client.sendAsync(request, BodyHandlers.ofString()).await().body()
        return objectMapper.readValue(response)
    }
}
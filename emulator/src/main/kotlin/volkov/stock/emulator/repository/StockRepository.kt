package volkov.stock.emulator.repository

import volkov.stock.emulator.model.Company
import volkov.stock.emulator.model.Stock
import java.util.*

interface StockRepository {
    suspend fun addCompany(company: Company)

    suspend fun addStock(stock: Stock)

    suspend fun buyStock(stockId: UUID, count: Int): Int

    suspend fun sellStock(stockId: UUID, count: Int): Int

    suspend fun updatePrice(stockId: UUID, price: Int)

    suspend fun getStock(stockId: UUID): Stock?

    suspend fun getCompany(companyId: UUID): Company?
}
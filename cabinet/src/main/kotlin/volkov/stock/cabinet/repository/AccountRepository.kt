package volkov.stock.cabinet.repository


import volkov.stock.cabinet.model.Account
import volkov.stock.cabinet.model.Stock
import java.util.*

interface AccountRepository {
    suspend fun addAccount(account: Account)

    suspend fun getAccount(accountId: UUID): Account?

    suspend fun payment(accountId: UUID, sum: Int)

    suspend fun hold(accountId: UUID, sum: Int)

    suspend fun addStock(accountId: UUID, stock: Stock)

    suspend fun deleteStock(accountId: UUID, stock: Stock)
}
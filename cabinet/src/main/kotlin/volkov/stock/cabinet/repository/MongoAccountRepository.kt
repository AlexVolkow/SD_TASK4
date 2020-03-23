package volkov.stock.cabinet.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates.*
import com.mongodb.reactivestreams.client.MongoDatabase
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.Document
import volkov.stock.cabinet.model.Account
import volkov.stock.cabinet.model.Stock
import java.util.*

class MongoAccountRepository(private val database: MongoDatabase) : AccountRepository {
    private val collection = database.getCollection(COLLECTION)


    override suspend fun addAccount(account: Account) {
        collection.insertOne(AccountConverter.encode(account)).awaitFirstOrNull()
    }

    override suspend fun getAccount(accountId: UUID): Account? {
        return collection.find(Filters.eq(AccountScheme.ID, accountId)).awaitFirstOrNull()
            ?.let { AccountConverter.decode(it) }
    }

    override suspend fun payment(accountId: UUID, sum: Int) {
        collection.updateOne(Filters.eq(AccountScheme.ID, accountId), inc(AccountScheme.BALANCE, sum))
            .awaitFirstOrNull()
    }

    override suspend fun hold(accountId: UUID, sum: Int) {
        collection.updateOne(Filters.eq(AccountScheme.ID, accountId), inc(AccountScheme.BALANCE, -sum))
            .awaitFirstOrNull()
    }

    override suspend fun addStock(accountId: UUID, stock: Stock) {
        val update = push(AccountScheme.STOCKS, StockConverter.encode(stock))

        val awaitFirst = collection.updateOne(Filters.eq(AccountScheme.ID, accountId), update).awaitFirst()
        println(awaitFirst)
    }

    override suspend fun deleteStock(accountId: UUID, stock: Stock) {
        val update = pull(AccountScheme.STOCKS, StockConverter.encode(stock))

        val awaitFirst = collection.updateOne(Filters.eq(AccountScheme.ID, accountId), update).awaitFirst()
        println(awaitFirst)
    }

    private companion object {
        const val COLLECTION = "account"
    }
}

object AccountScheme {
    const val ID = "_ID"
    const val NAME = "name"
    const val BALANCE = "balance"
    const val STOCKS = "stocks"
}

object AccountConverter {
    fun encode(account: Account): Document = Document().apply {
        append(AccountScheme.ID, account.id)
        append(AccountScheme.NAME, account.name)
        append(AccountScheme.BALANCE, account.balance)
        append(AccountScheme.STOCKS, account.stocks.map { StockConverter.encode(it) })
    }

    fun decode(document: Document): Account = Account(
        id = document.get(AccountScheme.ID, UUID::class.java),
        name = document.getString(AccountScheme.NAME),
        balance = document.getInteger(AccountScheme.BALANCE),
        stocks = document.getList(AccountScheme.STOCKS, Document::class.java).map { StockConverter.decode(it) },
        totalPrice = 0
    )
}


object StockScheme {
    const val ID = "_ID"
    const val COMPANY_ID = "company_id"
    const val NAME = "name"
    const val COUNT = "count"
    const val PRICE = "price"
}

object StockConverter {
    fun encode(stock: Stock): Document = Document().apply {
        append(StockScheme.ID, stock.id)
        append(StockScheme.COMPANY_ID, stock.companyId)
        append(StockScheme.NAME, stock.name)
        append(StockScheme.COUNT, stock.count)
        append(StockScheme.PRICE, stock.price)
    }

    fun decode(document: Document): Stock = Stock(
        id = document.get(StockScheme.ID, UUID::class.java),
        companyId = document.get(StockScheme.COMPANY_ID, UUID::class.java),
        name = document.getString(StockScheme.NAME),
        count = document.getInteger(StockScheme.COUNT),
        price = document.getInteger(StockScheme.PRICE)
    )
}

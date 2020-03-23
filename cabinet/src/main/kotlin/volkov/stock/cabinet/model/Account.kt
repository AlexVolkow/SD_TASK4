package volkov.stock.cabinet.model

import java.util.*

data class Account(
    val id: UUID,
    val name: String,
    val balance: Int = 0,
    val stocks: List<Stock> = emptyList(),
    val totalPrice: Int = 0
)
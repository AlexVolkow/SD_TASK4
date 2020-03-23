package volkov.stock.cabinet.dto

import java.util.*

data class TradeOperation(
    val stockId: UUID,
    val companyId: UUID,
    val type: Operation,
    val count: Int
)

enum class Operation {
    BUY,
    SELL
}
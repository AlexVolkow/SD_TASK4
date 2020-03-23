package volkov.stock.emulator.dto

data class TradeOperation(
    val type: Operation,
    val count: Int
)

enum class Operation {
    BUY,
    SELL
}
package volkov.stock.emulator.dto

data class CreateStock(
    val name: String,
    val count: Int,
    val price: Int
)
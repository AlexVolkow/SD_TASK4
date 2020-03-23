package volkov.stock.cabinet.model

import java.util.*

data class Stock(
    val id: UUID,
    val companyId: UUID,
    val name: String,
    val count: Int,
    val price: Int
)
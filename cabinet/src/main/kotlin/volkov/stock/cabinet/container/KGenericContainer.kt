package volkov.stock.cabinet.container

import org.testcontainers.containers.FixedHostPortGenericContainer

class KFixedHostPortGenericContainer(imageName: String) : FixedHostPortGenericContainer<KFixedHostPortGenericContainer>(imageName)
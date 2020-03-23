package volkov.stock.cabinet.container

abstract class BaseTestContainersMongodbTest {

    companion object {
        const val MONGODB_EXPOSED_PORT = 27017
        const val MONGODB_USERNAME = "test"
        const val MONGODB_PASSWORD = "test"

        @JvmStatic
        val mongo: KGenericContainer = KGenericContainer(
            "mongo:3.6.7"
        )
            .withExposedPorts(MONGODB_EXPOSED_PORT)
            .withEnv("MONGO_INITDB_ROOT_USERNAME",
                MONGODB_USERNAME
            )
            .withEnv("MONGO_INITDB_ROOT_PASSWORD",
                MONGODB_PASSWORD
            )

        init {
            mongo.start()
        }
    }
}

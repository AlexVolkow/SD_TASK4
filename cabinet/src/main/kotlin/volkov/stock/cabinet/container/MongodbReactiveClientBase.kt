package volkov.stock.cabinet.container

import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients

open class MongodbReactiveClientBase : BaseTestContainersMongodbTest() {

    fun createMongoClient(): MongoClient {
        val connectionString =
            "mongodb://$MONGODB_USERNAME:$MONGODB_PASSWORD@${mongo.containerIpAddress}:${mongo.getMappedPort(
                MONGODB_EXPOSED_PORT
            )}"

        return MongoClients.create(connectionString)
    }
}

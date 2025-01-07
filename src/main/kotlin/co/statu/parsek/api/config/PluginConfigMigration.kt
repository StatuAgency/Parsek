package co.statu.parsek.api.config

import io.vertx.core.json.JsonObject

abstract class PluginConfigMigration {
    abstract val from: Int
    abstract val to: Int
    abstract val versionInfo: String

    fun isMigratable(version: Int) = version == from

    abstract fun migrate(config: JsonObject)
}
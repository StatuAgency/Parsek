package co.statu.parsek.config.migration

import co.statu.parsek.annotation.Migration
import co.statu.parsek.config.ConfigManager
import co.statu.parsek.config.ConfigMigration

@Migration
class ConfigMigration1To2() : ConfigMigration(1, 2, "Add server config") {
    override fun migrate(configManager: ConfigManager) {
        val config = configManager.getConfig()

        config.put(
            "server", mapOf(
                "host" to "0.0.0.0",
                "port" to 8088
            )
        )
    }
}
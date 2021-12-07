package xyz.gary600.nexusclasses

import co.aikar.commands.BukkitCommandManager
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.collections.HashMap

/**
 * NexusClasses: custom character class plugin for CMURPGA's Nexus RP
 */
class NexusClasses : JavaPlugin() {
    // The collection of all player data
    private val playerData = HashMap<UUID, PlayerData>()

    fun getPlayerData(id: UUID): PlayerData {
        val data = playerData[id]

        // If there's no data for this player, create it and store it in the map
        return if (data == null) {
            val newData = PlayerData()
            playerData[id] = newData
            newData
        }
        // Otherwise return it
        else {
            data
        }
    }

    override fun onEnable() {
        // ACF command manager
        val commandManager = BukkitCommandManager(this)

        // Register command
        commandManager.registerCommand(ClassCommand(this))

        // Register pearl enchantment
        // Reflection tomfoolery to force Spigot to allow us to register a new enchant (apparently this is normal????)
        val acceptingNewField = Enchantment::class.java.getDeclaredField("acceptingNew")
        acceptingNewField.trySetAccessible()
        acceptingNewField.set(null, true)
        // Actually register
        val enchantKey = NamespacedKey(this, "free_pearl")
        val enchant = FreePearlEnchantment(enchantKey)
        Enchantment.registerEnchantment(enchant)

        // Register event handler
        server.pluginManager.registerEvents(ClassListener(this, enchant), this)
    }
}
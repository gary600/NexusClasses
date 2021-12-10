package xyz.gary600.nexusclasses

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import xyz.gary600.nexusclasses.extension.*

@Suppress("unused")
@CommandAlias("nexusclass|class")
class ClassCommand : BaseCommand() {
    @Subcommand("choose|select")
    @Description("Select your class")
    @Syntax("<class>")
    @CommandPermission("nexusclasses.choose")
    fun commandChoose(player: Player, nexusClass: NexusClass) {
        player.nexusClass = nexusClass
        NexusClasses.instance!!.saveData()
        player.sendNexusMessage("Your class is now ${nexusClass.name}")
    }

    @Subcommand("set")
    @Description("Set another player's class")
    @Syntax("<class> <player>")
    @CommandPermission("nexusclasses.set")
    fun commandSet(sender: CommandSender, player: OnlinePlayer, nexusClass: NexusClass) {
        player.player.nexusClass = nexusClass
        NexusClasses.instance!!.saveData()
        player.player.sendNexusMessage("Your class has been set to ${nexusClass.name}")
        sender.sendNexusMessage("Set ${player.player.displayName}'s class to ${nexusClass.name}")
    }

    @Default
    @Subcommand("get")
    @Description("Get a player's class")
    @Syntax("[<player>]")
    fun commandGet(sender: CommandSender, @Optional player: OnlinePlayer?) {
        if (player == null) {
            if (sender is Player) {
                sender.sendNexusMessage("Your class is ${sender.nexusClass}")
            }
            else {
                sender.sendNexusMessage("Must supply a player when on console")
            }
        }
        else {
            sender.sendNexusMessage("${player.player.displayName}'s class is ${player.player.nexusClass}")
        }
    }

    @Subcommand("item")
    @Description("Gives the class item if it exists and you don't have it already")
    fun commandItem(player: Player) {
        when (player.nexusClass) {
            NexusClass.Builder -> giveClassItem(player, Material.STICK, "Transmute", "Builder Class Item")
            NexusClass.Artist -> giveClassItem(player, Material.ENDER_PEARL, "Planar Blink", "Artist Class Item")
            else -> {
                player.sendNexusMessage("Class ${player.nexusClass} doesn't have a class item")
            }
        }
    }

    // Helper function to give an enchanted class item
    private fun giveClassItem(player: Player, type: Material, displayName: String, loreText: String) {
        val item = ItemStack(type, 1)

        // Metadata
        item.itemMeta = item.itemMeta?.apply {
            // Make it pretty
            setDisplayName(displayName)
            lore = listOf(loreText)
            addEnchant(Enchantment.LOYALTY, 1, true) // Dummy enchant to add item glow
            addItemFlags(ItemFlag.HIDE_ENCHANTS) // Hide the enchants (nobody shall know it's really Loyalty...)

            // Mark as class item so it works
            isClassItem = true
        }

        // Only give class item if player doesn't have one yet
        if (!player.inventory.containsAtLeast(item, 1)) {
            player.inventory.addItem(item)
        }
    }

    @Subcommand("world")
    @Description("Enables/disables class effects in the current world, or gets whether it's enabled or not")
    @Syntax("[<enabled>]")
    @CommandPermission("nexusclasses.configure")
    fun commandWorld(player: Player, @Optional enabled: Boolean?) {
        when (enabled) {
            null -> {
                if (player.world.nexusClassesEnabled) {
                    player.sendNexusMessage("Class effects are enabled in this world")
                }
                else {
                    player.sendNexusMessage("Class effects are disabled in this world")
                }
            }
            true -> {
                player.world.nexusClassesEnabled = true
                NexusClasses.instance!!.saveData()
                player.sendNexusMessage("Class effects enabled for this world")
            }
            false -> {
                player.world.nexusClassesEnabled = false
                NexusClasses.instance!!.saveData()
                player.sendMessage("Class effects disabled for this world")
            }
        }
    }

    @Subcommand("debugMessages")
    @Private
    fun commandMessages(player: Player, yesno: Boolean) {
        player.debugMessages = yesno
        NexusClasses.instance!!.saveData()
        if (yesno) {
            player.sendNexusMessage("You will now receive debug messages")
        }
        else {
            player.sendNexusMessage("You will no longer receive debug messages")
        }
    }
}
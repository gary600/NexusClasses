@file:Suppress("unused")

package xyz.gary600.nexusclasses

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

/**
 * The event listeners used by NexusClasses
 */
class ClassesListener(private val plugin: NexusClasses, private val classItemEnchantment: ClassItemEnchantment) : Listener {
    // Builder Perk: Inhibit fall damage [DONE]
    @EventHandler
    fun builderNoFallDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (
            entity is Player
            && plugin.getPlayerData(entity.uniqueId).nexusClass == NexusClass.Builder
            && event.cause == EntityDamageEvent.DamageCause.FALL
        ) {
            event.isCancelled = true
            plugin.sendPerkMessage(entity, "[NexusClasses] Builder perk: Fall damage cancelled!")
        }
    }

    // Builder Perk: Transmute blocks [DONE w/ changes]
    @EventHandler
    fun builderTransmute(event: PlayerInteractEvent) {
        // Only trigger when block right-clicked with a stick in the primary hand
        if (
            event.action == Action.RIGHT_CLICK_BLOCK
            && event.hand == EquipmentSlot.HAND
            && event.player.inventory.getItem(event.player.inventory.heldItemSlot)?.type == Material.STICK // Change: only when holding a stick
            && event.player.inventory.getItem(event.player.inventory.heldItemSlot)?.enchantments?.containsKey(classItemEnchantment) == true
        ) {
            // Transmute if builder
            if (plugin.getPlayerData(event.player.uniqueId).nexusClass == NexusClass.Builder) {
                var transmuted = true
                val block = event.clickedBlock!! // cannot be null because of action type
                block.type = when (block.type) {
                    // Cycle 1: cobble -> stone -> stone brick -> obsidian
                    Material.COBBLESTONE -> Material.STONE
                    Material.STONE -> Material.STONE_BRICKS
                    Material.STONE_BRICKS -> Material.OBSIDIAN
                    Material.OBSIDIAN -> Material.COBBLESTONE

                    // Cycle 2: deepslate -> tuff -> nether brick -> blackstone (change: remove obsidian from loop, that'd cause a conflict)
                    //TODO: add cobbled deepslate for parity with the other cycle?
                    Material.DEEPSLATE -> Material.TUFF
                    Material.TUFF -> Material.NETHER_BRICKS
                    Material.NETHER_BRICKS -> Material.BLACKSTONE
                    Material.BLACKSTONE -> Material.DEEPSLATE

                    // Otherwise keep it the same
                    else -> {
                        transmuted = false
                        block.type
                    }
                }
                if (transmuted) {
                    block.world.spawnParticle(
                        Particle.BLOCK_DUST,
                        block.location.add(0.5, 0.5, 0.5),
                        32,
                        block.blockData
                    ) // Spawn particles at center of block
                    block.world.playSound(
                        block.location,
                        block.blockData.soundGroup.breakSound,
                        1.0f,
                        1.0f
                    ) // Play block break sound

                    plugin.sendPerkMessage(event.player, "[NexusClasses] Builder perk: Block transmuted!")
                }
            }
            // If not builder, delete item
            else {
                event.player.inventory.getItem(event.player.inventory.heldItemSlot)?.amount = 0
            }
        }
    }

    // Builder Weakness: Burn in sun w/o helmet
    //TODO

    // Miner Perk: Certain ores additionally drop emerald
    @EventHandler
    fun minerFreeEmerald(event: BlockBreakEvent) {
        if (
            plugin.getPlayerData(event.player.uniqueId).nexusClass == NexusClass.Miner
            && event.block.type in arrayOf(Material.GOLD_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE, Material.IRON_ORE)
            && event.player.gameMode != GameMode.CREATIVE // Don't drop for creative mode players
        ) {
            // We're not allowed to add items to the block drop list for some reason, so just drop it manually where the block is
            event.block.world.dropItemNaturally(event.block.location, ItemStack(Material.EMERALD, 1))
            plugin.sendPerkMessage(event.player, "[NexusClasses] Miner perk: Free emerald!")
        }
    }

    // Miner Perk: Night vision below y=60
    //TODO

    // Miner Weakness: Extra damage from zombies [DONE]
    @EventHandler
    fun minerZombieWeakness(event: EntityDamageByEntityEvent) {
        val entity = event.entity
        if (
            entity is Player
            && plugin.getPlayerData(entity.uniqueId).nexusClass == NexusClass.Miner
            && event.damager is Zombie
        ) {
            event.damage *= 2 // Double damage
            plugin.sendPerkMessage(entity, "[NexusClasses] Miner weakness: double damage from zombies!")
        }
    }

    // Warrior perk: Automatic fire aspect on golden weapons [DONE]
    @EventHandler
    fun warriorFireAspect(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (
            damager is Player
            && plugin.getPlayerData(damager.uniqueId).nexusClass == NexusClass.Warrior
            && damager.inventory.getItem(damager.inventory.heldItemSlot)?.type in arrayOf(
                Material.GOLDEN_SWORD,
                Material.GOLDEN_AXE,
                Material.GOLDEN_PICKAXE,
                Material.GOLDEN_SHOVEL,
                Material.GOLDEN_HOE
            )
        ) {
            event.entity.fireTicks = 80 // equivalent to Fire Aspect 1
            plugin.sendPerkMessage(damager, "[NexusClasses] Warrior perk: Enemy ignited!")
        }
    }

    // Warrior perk: Holding gold weapons gives strength II
    //TODO

    // Warrior perk: Wearing gold armor gives fire immunity [DONE w/ changes]
    @EventHandler
    fun warriorFireResist(event: EntityDamageEvent) {
        val entity = event.entity
        if (
            entity is Player
            && plugin.getPlayerData(entity.uniqueId).nexusClass == NexusClass.Warrior
            && (
                event.cause == EntityDamageEvent.DamageCause.FIRE
                || event.cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || event.cause == EntityDamageEvent.DamageCause.LAVA // Change: add lava
            )
        ) {
            event.isCancelled = true
            plugin.sendPerkMessage(entity, "[NexusClasses] Warrior perk: Fire resistance!") // very spammy
        }
    }

    // Warrior weakness: mining fatigue while holding iron weapon, slowness while wearing iron armor
    //TODO

    // Artist perk: free end pearl at all times
    @EventHandler
    fun artistFreeEndPearl(event: PlayerInteractEvent) {
        if (
            (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK)
            && event.hand == EquipmentSlot.HAND
        ) {
            val classItem = event.player.inventory.getItem(event.player.inventory.heldItemSlot)
            if (
                classItem?.type == Material.ENDER_PEARL
                && classItem.enchantments.containsKey(classItemEnchantment)
                && event.player.getCooldown(Material.ENDER_PEARL) <= 0 // don't give pearl when on pearl cooldown
            ) {
                if (plugin.getPlayerData(event.player.uniqueId).nexusClass == NexusClass.Artist) {
                    classItem.amount = 2
                    plugin.sendPerkMessage(event.player, "[NexusClasses] Artist perk: free end pearl!")
                }
                // Don't let non-Artists use the pearl
                else {
                    classItem.amount = 0
                    event.isCancelled = true
                }
            }
        }
    }

    // Prevent dropping class items by that class, delete if dropped by another class
    @EventHandler
    fun preventDropClassItem(event: PlayerDropItemEvent) {
        if (event.itemDrop.itemStack.enchantments.containsKey(classItemEnchantment)) {
            // Players of that class can't drop the item
            if (
                (
                    plugin.getPlayerData(event.player.uniqueId).nexusClass == NexusClass.Artist
                    && event.itemDrop.itemStack.type == Material.ENDER_PEARL
                )
                || (
                    plugin.getPlayerData(event.player.uniqueId).nexusClass == NexusClass.Builder
                    && event.itemDrop.itemStack.type == Material.STICK
                )
            ) {
                event.isCancelled = true
            }
            // Other classes can drop to delete it
            else {
                event.itemDrop.remove()
            }
        }
    }
    // Prevent putting class items in any other inventory
    @EventHandler
    fun preventMoveClassItem(event: InventoryClickEvent) {
        if (
            // If shift clicked from player's inventory
            (
                event.click.isShiftClick
                && event.clickedInventory == event.whoClicked.inventory // inventory *is* the player's
                && event.currentItem?.enchantments?.containsKey(classItemEnchantment) == true // item *under* cursor is the class item
            )
            // If item moved into other inventory normally
            || (
                event.clickedInventory != event.whoClicked.inventory // inventory is *not* the player's
                && event.cursor?.enchantments?.containsKey(classItemEnchantment) == true // item *on* cursor is the class item
            )
        ) {
            event.isCancelled = true
        }
    }
    // Prevent dragging class items
    @EventHandler
    fun preventDragClassItem(event: InventoryDragEvent) {
        if (event.oldCursor.enchantments.containsKey(classItemEnchantment)) {
            event.isCancelled = true
        }
    }
}
package com.willfp.reforges.reforges

import com.willfp.eco.core.config.interfaces.JSONConfig
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.builder.SkullBuilder
import com.willfp.eco.core.recipe.Recipes
import com.willfp.reforges.ReforgesPlugin
import com.willfp.reforges.effects.ConfiguredEffect
import com.willfp.reforges.effects.Effects
import com.willfp.reforges.reforges.meta.ReforgeTarget
import com.willfp.reforges.reforges.util.ReforgeUtils
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

class Reforge(
    private val config: JSONConfig,
    private val plugin: ReforgesPlugin
) {
    val id = config.getString("id")

    val name = config.getString("name")

    val description = config.getString("description")

    val targets = config.getStrings("targets").map { ReforgeTarget.getByName(it) }.toSet()

    val effects = config.getSubsections("effects").map {
        val effect = Effects.getByName(it.getString("id")) ?: return@map null
        ConfiguredEffect(effect, it)
    }.filterNotNull().toSet()

    val requiresStone = config.getBool("stone.enabled")

    val stone: ItemStack = SkullBuilder().apply {
        if (config.has("stone.texture")) {
            setSkullTexture(config.getString("texture"))
        }
        setDisplayName(plugin.configYml.getString("stone.name").replace("%reforge%", name))
        addLoreLines(
            plugin.configYml.getStrings("stone.lore").map { "${Display.PREFIX}${it.replace("%reforge%", name)}" })
    }.build()

    init {
        ReforgeUtils.setReforgeStone(stone, this)

        CustomItem(
            plugin.namespacedKeyFactory.create("stone_" + this.id),
            { test -> ReforgeUtils.getReforgeStone(test) == this },
            stone
        ).register()

        if (config.getBool("craftable")) {
            Recipes.createAndRegisterRecipe(
                plugin,
                "stone_" + this.id,
                stone,
                config.getStrings("recipe", false)
            )
        }
    }

    fun handleApplication(itemStack: ItemStack) {
        handleRemoval(itemStack)
        itemStack.itemMeta = this.handleApplication(itemStack.itemMeta!!)
    }

    fun handleApplication(meta: ItemMeta): ItemMeta {
        for ((effect, config) in this.effects) {
            effect.handleApplication(meta, config)
        }
        return meta
    }

    fun handleRemoval(itemStack: ItemStack) {
        itemStack.itemMeta = this.handleRemoval(itemStack.itemMeta!!)
    }

    fun handleRemoval(meta: ItemMeta): ItemMeta {
        for ((effect, _) in this.effects) {
            effect.handleRemoval(meta)
        }
        return meta
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is Reforge) {
            return false
        }

        return other.id == this.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    override fun toString(): String {
        return "Reforge{$id}"
    }
}
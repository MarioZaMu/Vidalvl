package com.vidalvl.app.domain.model

/**
 * Which catalog items the user should receive. Pure functions: the caller supplies what is
 * already owned, so a rule can never hand out the same item twice.
 */
object RewardRules {
    /**
     * Level items whose level is reached and whose attribute requirement is met.
     * Called after every completion, so an item blocked by an attribute level arrives once it is reached.
     */
    fun levelItemsDue(
        totalLevel: Int,
        attributeLevels: Map<Attribute, Int>,
        owned: Set<String>,
        catalog: List<CatalogItem> = ItemCatalog.items
    ): List<CatalogItem> = catalog
        .filter { item ->
            val unlock = item.unlock
            unlock is ItemUnlock.TotalLevel &&
                    unlock.level <= totalLevel &&
                    item.id !in owned &&
                    (attributeLevels[item.attribute] ?: 1) >= item.requiredAttributeLevel
        }
        .sortedBy { (it.unlock as ItemUnlock.TotalLevel).level }

    /** Next unowned item of the attribute's weekly sequence, or null if it is not reachable yet (rare, level too low). */
    fun nextWeeklyItem(
        attribute: Attribute,
        attributeLevel: Int,
        owned: Set<String>,
        catalog: List<CatalogItem> = ItemCatalog.items
    ): CatalogItem? = catalog
        .filter { it.attribute == attribute && it.unlock is ItemUnlock.Weekly && it.id !in owned }
        .minByOrNull { (it.unlock as ItemUnlock.Weekly).order }
        ?.takeIf { attributeLevel >= it.requiredAttributeLevel }

    /** The mission's special item, if it exists in the catalog and is not owned yet. */
    fun rewardMissionItem(
        rewardItemId: String?,
        owned: Set<String>,
        catalog: List<CatalogItem> = ItemCatalog.items
    ): CatalogItem? = rewardItemId
        ?.takeIf { it !in owned }
        ?.let { id -> catalog.firstOrNull { it.id == id && it.unlock is ItemUnlock.RewardMission } }
}

package com.vidalvl.app.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.vidalvl.app.R

/** How an item is earned. */
sealed interface ItemUnlock {
    /** Given when the total level reaches [level]. */
    data class TotalLevel(val level: Int) : ItemUnlock

    /** Given by completing a weekly mission of the item's attribute; [order] is its place in that attribute's sequence. */
    data class Weekly(val order: Int) : ItemUnlock

    /** Given once by the mission whose `rewardItemId` is this item. */
    data object RewardMission : ItemUnlock
}

/**
 * An item of the fixed catalog. [requiredAttributeLevel] > 1 marks a rare item.
 * [spriteRes] is the front sprite (the male one for outfits); [backSpriteRes] is drawn behind the body.
 */
data class CatalogItem(
    val id: String,
    val slot: ItemSlot,
    val attribute: Attribute,
    @StringRes val nameRes: Int,
    @DrawableRes val spriteRes: Int,
    val unlock: ItemUnlock,
    val requiredAttributeLevel: Int = 1,
    @DrawableRes val femaleSpriteRes: Int? = null,
    @DrawableRes val backSpriteRes: Int? = null,
    val hairEffect: HairEffect = HairEffect.NONE
) {
    val isRare: Boolean get() = requiredAttributeLevel > 1

    /** Outfits have one sprite per gender; the other slots share one. */
    @DrawableRes
    fun spriteFor(gender: Gender): Int? =
        if (slot == ItemSlot.OUTFIT && gender == Gender.FEMALE) femaleSpriteRes else spriteRes

    /** False for outfits that only exist in the male version. */
    fun availableFor(gender: Gender): Boolean = spriteFor(gender) != null
}

object ItemCatalog {
    /** Special item of the "reward" mission. */
    const val SPECIAL_REWARD_ID = "hat_013"

    val items: List<CatalogItem> = listOf(
        CatalogItem("outfit_001", ItemSlot.OUTFIT, Attribute.BODY, R.string.item_outfit_001, R.drawable.outfit_male_001, ItemUnlock.TotalLevel(2), femaleSpriteRes = R.drawable.outfit_female_001),
        CatalogItem("hat_011", ItemSlot.HAT, Attribute.BODY, R.string.item_hat_011, R.drawable.hat_011, ItemUnlock.TotalLevel(3)),
        CatalogItem("glasses_001_1", ItemSlot.GLASSES, Attribute.MIND, R.string.item_glasses_001_1, R.drawable.glasses_001_1, ItemUnlock.TotalLevel(4)),
        CatalogItem("outfit_009", ItemSlot.OUTFIT, Attribute.MIND, R.string.item_outfit_009, R.drawable.outfit_male_009, ItemUnlock.TotalLevel(5), femaleSpriteRes = R.drawable.outfit_female_009),
        CatalogItem("hat_005", ItemSlot.HAT, Attribute.SOCIAL, R.string.item_hat_005, R.drawable.hat_005, ItemUnlock.TotalLevel(6), hairEffect = HairEffect.TRIMMED),
        CatalogItem("glasses_003_1", ItemSlot.GLASSES, Attribute.SOCIAL, R.string.item_glasses_003_1, R.drawable.glasses_003_1, ItemUnlock.TotalLevel(7)),
        CatalogItem("cloak_002", ItemSlot.CLOAK, Attribute.SOCIAL, R.string.item_cloak_002, R.drawable.cloak_002, ItemUnlock.TotalLevel(8), backSpriteRes = R.drawable.cloak_002_back),
        CatalogItem("outfit_014", ItemSlot.OUTFIT, Attribute.DISCIPLINE, R.string.item_outfit_014, R.drawable.outfit_male_014, ItemUnlock.TotalLevel(9), femaleSpriteRes = R.drawable.outfit_female_014),
        CatalogItem("hat_014", ItemSlot.HAT, Attribute.DISCIPLINE, R.string.item_hat_014, R.drawable.hat_014, ItemUnlock.TotalLevel(10), hairEffect = HairEffect.TRIMMED),
        CatalogItem("glasses_002_1", ItemSlot.GLASSES, Attribute.MIND, R.string.item_glasses_002_1, R.drawable.glasses_002_1, ItemUnlock.TotalLevel(11)),
        CatalogItem("outfit_012", ItemSlot.OUTFIT, Attribute.BODY, R.string.item_outfit_012, R.drawable.outfit_male_012, ItemUnlock.TotalLevel(12), femaleSpriteRes = R.drawable.outfit_female_012),
        CatalogItem("hat_009", ItemSlot.HAT, Attribute.SOCIAL, R.string.item_hat_009, R.drawable.hat_009, ItemUnlock.TotalLevel(13), hairEffect = HairEffect.TRIMMED),
        CatalogItem("cloak_004", ItemSlot.CLOAK, Attribute.DISCIPLINE, R.string.item_cloak_004, R.drawable.cloak_004, ItemUnlock.TotalLevel(14), backSpriteRes = R.drawable.cloak_004_back),
        CatalogItem("outfit_013", ItemSlot.OUTFIT, Attribute.DISCIPLINE, R.string.item_outfit_013, R.drawable.outfit_male_013, ItemUnlock.TotalLevel(15), femaleSpriteRes = R.drawable.outfit_female_013),
        CatalogItem("glasses_005_1", ItemSlot.GLASSES, Attribute.MIND, R.string.item_glasses_005_1, R.drawable.glasses_005_1, ItemUnlock.TotalLevel(16)),
        CatalogItem("hat_008", ItemSlot.HAT, Attribute.SOCIAL, R.string.item_hat_008, R.drawable.hat_008, ItemUnlock.TotalLevel(17), hairEffect = HairEffect.TRIMMED),
        CatalogItem("outfit_010", ItemSlot.OUTFIT, Attribute.SOCIAL, R.string.item_outfit_010, R.drawable.outfit_male_010, ItemUnlock.TotalLevel(18), femaleSpriteRes = R.drawable.outfit_female_010),
        CatalogItem("glasses_006_1", ItemSlot.GLASSES, Attribute.DISCIPLINE, R.string.item_glasses_006_1, R.drawable.glasses_006_1, ItemUnlock.TotalLevel(19)),
        CatalogItem("cloak_003", ItemSlot.CLOAK, Attribute.SOCIAL, R.string.item_cloak_003, R.drawable.cloak_003, ItemUnlock.TotalLevel(20), backSpriteRes = R.drawable.cloak_003_back),
        CatalogItem("outfit_016", ItemSlot.OUTFIT, Attribute.DISCIPLINE, R.string.item_outfit_016, R.drawable.outfit_male_016, ItemUnlock.TotalLevel(21), femaleSpriteRes = R.drawable.outfit_female_016),
        CatalogItem("hat_010", ItemSlot.HAT, Attribute.BODY, R.string.item_hat_010, R.drawable.hat_010, ItemUnlock.Weekly(1)),
        CatalogItem("outfit_018", ItemSlot.OUTFIT, Attribute.BODY, R.string.item_outfit_018, R.drawable.outfit_male_018, ItemUnlock.Weekly(2), femaleSpriteRes = R.drawable.outfit_female_018),
        CatalogItem("outfit_003", ItemSlot.OUTFIT, Attribute.BODY, R.string.item_outfit_003, R.drawable.outfit_male_003, ItemUnlock.Weekly(3), femaleSpriteRes = R.drawable.outfit_female_003),
        CatalogItem("hat_001_1", ItemSlot.HAT, Attribute.BODY, R.string.item_hat_001_1, R.drawable.hat_001_1, ItemUnlock.Weekly(4), requiredAttributeLevel = 2, hairEffect = HairEffect.TRIMMED),
        CatalogItem("outfit_019", ItemSlot.OUTFIT, Attribute.BODY, R.string.item_outfit_019, R.drawable.outfit_male_019, ItemUnlock.Weekly(5), requiredAttributeLevel = 2, femaleSpriteRes = R.drawable.outfit_female_019),
        CatalogItem("hat_001_2", ItemSlot.HAT, Attribute.BODY, R.string.item_hat_001_2, R.drawable.hat_001_2, ItemUnlock.Weekly(6), requiredAttributeLevel = 2, hairEffect = HairEffect.TRIMMED),
        CatalogItem("outfit_008", ItemSlot.OUTFIT, Attribute.BODY, R.string.item_outfit_008, R.drawable.outfit_male_008, ItemUnlock.Weekly(7), requiredAttributeLevel = 3, femaleSpriteRes = R.drawable.outfit_female_008),
        CatalogItem("hat_001_3", ItemSlot.HAT, Attribute.BODY, R.string.item_hat_001_3, R.drawable.hat_001_3, ItemUnlock.Weekly(8), requiredAttributeLevel = 3, hairEffect = HairEffect.TRIMMED),
        CatalogItem("outfit_020", ItemSlot.OUTFIT, Attribute.BODY, R.string.item_outfit_020, R.drawable.outfit_male_020, ItemUnlock.Weekly(9), requiredAttributeLevel = 4),
        CatalogItem("glasses_001_2", ItemSlot.GLASSES, Attribute.MIND, R.string.item_glasses_001_2, R.drawable.glasses_001_2, ItemUnlock.Weekly(1)),
        CatalogItem("hat_006", ItemSlot.HAT, Attribute.MIND, R.string.item_hat_006, R.drawable.hat_006, ItemUnlock.Weekly(2), hairEffect = HairEffect.TRIMMED),
        CatalogItem("glasses_002_2", ItemSlot.GLASSES, Attribute.MIND, R.string.item_glasses_002_2, R.drawable.glasses_002_2, ItemUnlock.Weekly(3)),
        CatalogItem("outfit_007", ItemSlot.OUTFIT, Attribute.MIND, R.string.item_outfit_007, R.drawable.outfit_male_007, ItemUnlock.Weekly(4), requiredAttributeLevel = 2, femaleSpriteRes = R.drawable.outfit_female_007),
        CatalogItem("glasses_004_1", ItemSlot.GLASSES, Attribute.MIND, R.string.item_glasses_004_1, R.drawable.glasses_004_1, ItemUnlock.Weekly(5), requiredAttributeLevel = 2),
        CatalogItem("hat_007", ItemSlot.HAT, Attribute.MIND, R.string.item_hat_007, R.drawable.hat_007, ItemUnlock.Weekly(6), requiredAttributeLevel = 2, hairEffect = HairEffect.TRIMMED),
        CatalogItem("outfit_015", ItemSlot.OUTFIT, Attribute.MIND, R.string.item_outfit_015, R.drawable.outfit_male_015, ItemUnlock.Weekly(7), requiredAttributeLevel = 3, femaleSpriteRes = R.drawable.outfit_female_015),
        CatalogItem("glasses_007_1", ItemSlot.GLASSES, Attribute.MIND, R.string.item_glasses_007_1, R.drawable.glasses_007_1, ItemUnlock.Weekly(8), requiredAttributeLevel = 3),
        CatalogItem("cloak_005", ItemSlot.CLOAK, Attribute.MIND, R.string.item_cloak_005, R.drawable.cloak_005, ItemUnlock.Weekly(9), requiredAttributeLevel = 4, backSpriteRes = R.drawable.cloak_005_back),
        CatalogItem("hat_003", ItemSlot.HAT, Attribute.DISCIPLINE, R.string.item_hat_003, R.drawable.hat_003, ItemUnlock.Weekly(1)),
        CatalogItem("outfit_005", ItemSlot.OUTFIT, Attribute.DISCIPLINE, R.string.item_outfit_005, R.drawable.outfit_male_005, ItemUnlock.Weekly(2), femaleSpriteRes = R.drawable.outfit_female_005),
        CatalogItem("glasses_007_2", ItemSlot.GLASSES, Attribute.DISCIPLINE, R.string.item_glasses_007_2, R.drawable.glasses_007_2, ItemUnlock.Weekly(3)),
        CatalogItem("outfit_006", ItemSlot.OUTFIT, Attribute.DISCIPLINE, R.string.item_outfit_006, R.drawable.outfit_male_006, ItemUnlock.Weekly(4), requiredAttributeLevel = 2, femaleSpriteRes = R.drawable.outfit_female_006),
        CatalogItem("hat_002", ItemSlot.HAT, Attribute.DISCIPLINE, R.string.item_hat_002, R.drawable.hat_002, ItemUnlock.Weekly(5), requiredAttributeLevel = 2),
        CatalogItem("outfit_004", ItemSlot.OUTFIT, Attribute.DISCIPLINE, R.string.item_outfit_004, R.drawable.outfit_male_004, ItemUnlock.Weekly(6), requiredAttributeLevel = 2, hairEffect = HairEffect.HIDDEN, femaleSpriteRes = R.drawable.outfit_female_004),
        CatalogItem("cloak_008", ItemSlot.CLOAK, Attribute.DISCIPLINE, R.string.item_cloak_008, R.drawable.cloak_008, ItemUnlock.Weekly(7), requiredAttributeLevel = 3, backSpriteRes = R.drawable.cloak_008_back),
        CatalogItem("outfit_021", ItemSlot.OUTFIT, Attribute.DISCIPLINE, R.string.item_outfit_021, R.drawable.outfit_male_021, ItemUnlock.Weekly(8), requiredAttributeLevel = 3),
        CatalogItem("hat_012", ItemSlot.HAT, Attribute.DISCIPLINE, R.string.item_hat_012, R.drawable.hat_012, ItemUnlock.Weekly(9), requiredAttributeLevel = 4, hairEffect = HairEffect.HIDDEN),
        CatalogItem("hat_015_1", ItemSlot.HAT, Attribute.SOCIAL, R.string.item_hat_015_1, R.drawable.hat_015_1, ItemUnlock.Weekly(1)),
        CatalogItem("outfit_002", ItemSlot.OUTFIT, Attribute.SOCIAL, R.string.item_outfit_002, R.drawable.outfit_male_002, ItemUnlock.Weekly(2), femaleSpriteRes = R.drawable.outfit_female_002),
        CatalogItem("hat_015_2", ItemSlot.HAT, Attribute.SOCIAL, R.string.item_hat_015_2, R.drawable.hat_015_2, ItemUnlock.Weekly(3)),
        CatalogItem("hat_015_3", ItemSlot.HAT, Attribute.SOCIAL, R.string.item_hat_015_3, R.drawable.hat_015_3, ItemUnlock.Weekly(4), requiredAttributeLevel = 2),
        CatalogItem("outfit_011", ItemSlot.OUTFIT, Attribute.SOCIAL, R.string.item_outfit_011, R.drawable.outfit_male_011, ItemUnlock.Weekly(5), requiredAttributeLevel = 2, femaleSpriteRes = R.drawable.outfit_female_011),
        CatalogItem("hat_016_1", ItemSlot.HAT, Attribute.SOCIAL, R.string.item_hat_016_1, R.drawable.hat_016_1, ItemUnlock.Weekly(6), requiredAttributeLevel = 2),
        CatalogItem("hat_016_2", ItemSlot.HAT, Attribute.SOCIAL, R.string.item_hat_016_2, R.drawable.hat_016_2, ItemUnlock.Weekly(7), requiredAttributeLevel = 3),
        CatalogItem("hat_016_3", ItemSlot.HAT, Attribute.SOCIAL, R.string.item_hat_016_3, R.drawable.hat_016_3, ItemUnlock.Weekly(8), requiredAttributeLevel = 3),
        CatalogItem("outfit_017", ItemSlot.OUTFIT, Attribute.SOCIAL, R.string.item_outfit_017, R.drawable.outfit_male_017, ItemUnlock.Weekly(9), requiredAttributeLevel = 4, femaleSpriteRes = R.drawable.outfit_female_017),
        CatalogItem("hat_013", ItemSlot.HAT, Attribute.DISCIPLINE, R.string.item_hat_013, R.drawable.hat_013, ItemUnlock.RewardMission, hairEffect = HairEffect.TRIMMED),
        CatalogItem("cloak_006", ItemSlot.CLOAK, Attribute.SOCIAL, R.string.item_cloak_006, R.drawable.cloak_006, ItemUnlock.RewardMission, backSpriteRes = R.drawable.cloak_006_back),
        CatalogItem("cloak_007", ItemSlot.CLOAK, Attribute.MIND, R.string.item_cloak_007, R.drawable.cloak_007, ItemUnlock.RewardMission),
        CatalogItem("hat_017_1", ItemSlot.HAT, Attribute.SOCIAL, R.string.item_hat_017_1, R.drawable.hat_017_1, ItemUnlock.RewardMission),
        CatalogItem("hat_017_2", ItemSlot.HAT, Attribute.SOCIAL, R.string.item_hat_017_2, R.drawable.hat_017_2, ItemUnlock.RewardMission),
        CatalogItem("hat_017_3", ItemSlot.HAT, Attribute.SOCIAL, R.string.item_hat_017_3, R.drawable.hat_017_3, ItemUnlock.RewardMission)
    )

    private val byId = items.associateBy { it.id }

    fun byId(id: String): CatalogItem? = byId[id]
}

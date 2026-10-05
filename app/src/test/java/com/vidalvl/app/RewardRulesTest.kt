package com.vidalvl.app

import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.Gender
import com.vidalvl.app.domain.model.ItemCatalog
import com.vidalvl.app.domain.model.ItemUnlock
import com.vidalvl.app.domain.model.RewardRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardRulesTest {
    private val level1 = Attribute.entries.associateWith { 1 }

    private fun weeklySequence(attribute: Attribute) = ItemCatalog.items
        .filter { it.attribute == attribute && it.unlock is ItemUnlock.Weekly }
        .sortedBy { (it.unlock as ItemUnlock.Weekly).order }

    @Test
    fun catalog_ids_are_unique() {
        val ids = ItemCatalog.items.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun no_level_item_before_level_two() {
        assertTrue(RewardRules.levelItemsDue(1, level1, emptySet()).isEmpty())
    }

    @Test
    fun level_items_are_cumulative_and_skip_owned() {
        val atLevel3 = RewardRules.levelItemsDue(3, level1, emptySet()).map { it.id }
        assertEquals(listOf("outfit_001", "hat_011"), atLevel3)

        val withoutOwned = RewardRules.levelItemsDue(3, level1, setOf("outfit_001")).map { it.id }
        assertEquals(listOf("hat_011"), withoutOwned)
    }

    @Test
    fun there_is_one_level_item_per_level_without_gaps() {
        val levels = ItemCatalog.items
            .mapNotNull { (it.unlock as? ItemUnlock.TotalLevel)?.level }
            .sorted()
        assertEquals((2..21).toList(), levels)
    }

    @Test
    fun weekly_items_follow_the_fixed_order() {
        assertEquals("hat_010", RewardRules.nextWeeklyItem(Attribute.BODY, 1, emptySet())?.id)
    }

    @Test
    fun rare_weekly_item_waits_for_attribute_level() {
        val owned = weeklySequence(Attribute.BODY).take(3).map { it.id }.toSet()
        assertNull(RewardRules.nextWeeklyItem(Attribute.BODY, 1, owned))
        assertEquals("hat_001_1", RewardRules.nextWeeklyItem(Attribute.BODY, 2, owned)?.id)
    }

    @Test
    fun weekly_sequence_ends_when_everything_is_owned() {
        val owned = weeklySequence(Attribute.BODY).map { it.id }.toSet()
        assertNull(RewardRules.nextWeeklyItem(Attribute.BODY, 10, owned))
    }

    @Test
    fun every_attribute_has_a_weekly_sequence_of_nine_without_gaps() {
        Attribute.entries.forEach { attribute ->
            val orders = weeklySequence(attribute).map { (it.unlock as ItemUnlock.Weekly).order }
            assertEquals((1..9).toList(), orders)
            assertEquals(1, weeklySequence(attribute).first().requiredAttributeLevel)
        }
    }

    @Test
    fun reward_mission_item_is_given_once() {
        val id = ItemCatalog.SPECIAL_REWARD_ID
        assertEquals(id, RewardRules.rewardMissionItem(id, emptySet())?.id)
        assertNull(RewardRules.rewardMissionItem(id, setOf(id)))
        assertNull(RewardRules.rewardMissionItem(null, emptySet()))
        // Only items made for reward missions can be given this way.
        assertNull(RewardRules.rewardMissionItem("hat_011", emptySet()))
    }

    @Test
    fun only_two_outfits_are_male_only() {
        val maleOnly = ItemCatalog.items.filter { !it.availableFor(Gender.FEMALE) }.map { it.id }
        assertEquals(listOf("outfit_020", "outfit_021"), maleOnly.sorted())
        assertTrue(ItemCatalog.items.all { it.availableFor(Gender.MALE) })
    }
}

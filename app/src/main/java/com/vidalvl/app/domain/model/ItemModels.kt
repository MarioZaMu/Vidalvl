package com.vidalvl.app.domain.model

/** Where an avatar item can be equipped. Stored as text, so new slots need no migration. */
enum class ItemSlot { OUTFIT, HAT, GLASSES, CLOAK }

/** What an equipped item does to the avatar's hair: nothing, swap to the trimmed hair sprites, or hide it. */
enum class HairEffect { NONE, TRIMMED, HIDDEN }

/** How an item reached the inventory. */
enum class ItemSource { LEVEL, WEEKLY, REWARD_MISSION, ACHIEVEMENT }
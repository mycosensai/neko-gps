package com.nekogps.app

/**
 * Converts a travel bearing into a human-readable compass direction.
 * Pure logic extracted from [NavigationActivity] so the activity stays
 * under the function-count limit.
 */
object BearingDirections {

    private const val FULL_CIRCLE_DEGREES = 360.0
    private const val SECTOR_WIDTH_DEGREES = 45.0
    private const val HALF_SECTOR_DEGREES = 22.5
    private const val SECTOR_COUNT = 8
    private val DIRECTIONS = arrayOf(
        "Head north",
        "Head northeast",
        "Head east",
        "Head southeast",
        "Head south",
        "Head southwest",
        "Head west",
        "Head northwest"
    )


    fun toDirection(bearing: Double): String {
        val normalized = ((bearing % FULL_CIRCLE_DEGREES) + FULL_CIRCLE_DEGREES) % FULL_CIRCLE_DEGREES
        val sector = ((normalized + HALF_SECTOR_DEGREES) / SECTOR_WIDTH_DEGREES).toInt() % SECTOR_COUNT
        return DIRECTIONS[sector]
    }
}

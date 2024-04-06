package me.fexus.examples.coolvoxelrendering.player

// contains only player stats
data class PlayerStats(
    var deathCount: Int,
    var blocksMined: Int
    ) {

    companion object {
        fun newDefault(): PlayerStats =
            PlayerStats(
                0,
                0,
            )
    }
}
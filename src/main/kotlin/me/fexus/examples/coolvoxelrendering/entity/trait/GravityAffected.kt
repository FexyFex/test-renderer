package me.fexus.examples.coolvoxelrendering.entity.trait

import me.fexus.examples.coolvoxelrendering.entity.Entity
import me.fexus.examples.coolvoxelrendering.entity.EntityTrait
import me.fexus.examples.coolvoxelrendering.entity.component.Gravity
import me.fexus.examples.coolvoxelrendering.entity.component.IsGrounded
import me.fexus.examples.coolvoxelrendering.entity.component.Velocity


class GravityAffected(override val entity: Entity): EntityTrait {
    fun tickGravity(delta: Float) {
        if (entity.getComponent<IsGrounded>()!!.isGrounded) {
            entity.withComponent<Velocity> { if (velocity.y < 0f) velocity.y = 0f }
            return
        }
        val gravity = entity.getComponent<Gravity>()!!.gravityVector
        entity.withComponent<Velocity> {
            velocity += gravity * delta
        }
    }
}
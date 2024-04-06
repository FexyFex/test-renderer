package me.fexus.examples.coolvoxelrendering.entity.type

import me.fexus.examples.coolvoxelrendering.collision.CollisionAABB
import me.fexus.examples.coolvoxelrendering.entity.Entity
import me.fexus.examples.coolvoxelrendering.entity.component.CollisionArea
import me.fexus.examples.coolvoxelrendering.entity.component.Gravity
import me.fexus.examples.coolvoxelrendering.entity.component.IsGrounded
import me.fexus.examples.coolvoxelrendering.entity.component.Velocity
import me.fexus.examples.coolvoxelrendering.entity.trait.GravityAffected
import me.fexus.examples.coolvoxelrendering.entity.trait.TerrainCollision
import me.fexus.examples.coolvoxelrendering.world.position.WorldPosition
import me.fexus.math.vec.Vec3


class PlayerSurvivalEntity(override val worldPosition: WorldPosition, id: Long? = null): Entity(id) {
    init {
        addComponent(IsGrounded(true))
        addComponent(Gravity(Vec3(0f, -28f, 0f)))
        addComponent(Velocity(Vec3(0f)))
        addComponent(CollisionArea(CollisionAABB(worldPosition, Vec3(0.6f))))

        addTrait<TerrainCollision>()
        addTrait<GravityAffected>()
    }


    override fun tick(delta: Float) {
        withTrait<GravityAffected> { tickGravity(delta) }
        //withTrait<TerrainCollision> { moveWithCollisionRestraints(delta) }
    }
}
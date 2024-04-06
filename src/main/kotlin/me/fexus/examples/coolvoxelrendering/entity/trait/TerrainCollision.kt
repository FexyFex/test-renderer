package me.fexus.examples.coolvoxelrendering.entity.trait

import me.fexus.examples.coolvoxelrendering.World
import me.fexus.examples.coolvoxelrendering.collision.CollisionAABB
import me.fexus.examples.coolvoxelrendering.collision.CollisionLine
import me.fexus.examples.coolvoxelrendering.collision.intersection
import me.fexus.examples.coolvoxelrendering.entity.Entity
import me.fexus.examples.coolvoxelrendering.entity.EntityTrait
import me.fexus.examples.coolvoxelrendering.entity.component.CollisionArea
import me.fexus.examples.coolvoxelrendering.entity.component.IsGrounded
import me.fexus.examples.coolvoxelrendering.entity.component.Velocity
import me.fexus.examples.coolvoxelrendering.world.position.VoxelWorldPosition
import me.fexus.math.repeat3D
import me.fexus.math.repeatCubed
import me.fexus.math.vec.DVec3
import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec3
import me.fexus.voxel.type.VoidVoxel
import kotlin.math.absoluteValue
import kotlin.math.sign


class TerrainCollision(override val entity: Entity) : EntityTrait {

    fun moveWithCollisionRestraints(world: World, delta: Float) {
        val collisionArea = entity.getComponent<CollisionArea>()!!.collisionBox
        val pos = entity.worldPosition
        val vel = entity.getComponent<Velocity>()!!.velocity * delta

        val centerBlock = pos.toVoxelWorldPosition()
        val radius = 1

        var hitGround: Boolean = false
        repeatCubed(radius * 2 + 1) { x, y, z ->
            val blockPos = VoxelWorldPosition(IVec3(x, y, z) - radius + centerBlock)
            val block = world.getBlockAt(blockPos)
            if (block == VoidVoxel) return@repeatCubed
            val blockBoundingBox = CollisionAABB(DVec3(blockPos.x, blockPos.y, blockPos.z), Vec3(1f))

            for (it in arrayOf(1, 0, 2)) {
                val offset = Vec3(0f)
                offset[it] = (blockPos[it] - centerBlock[it]).sign.toFloat()

                val boxPos = pos + (collisionArea.extent * offset / 2f)

                val attempted = vel[it].toDouble()
                var minAllowed = attempted
                val epsilon = 0.01f

                //test the 4 corner points
                for (cX in arrayOf(-collisionArea.extent[(it + 1) % 3] / 2f + epsilon, collisionArea.extent[(it + 1) % 3] / 2f - epsilon)) {
                    for (cY in arrayOf(-collisionArea.extent[(it + 2) % 3] / 2f + epsilon, collisionArea.extent[(it + 2) % 3] / 2f - epsilon)) {
                        val rayPos = DVec3(boxPos.x, boxPos.y, boxPos.z)
                        rayPos[(it + 1) % 3] += cX.toDouble()
                        rayPos[(it + 2) % 3] += cY.toDouble()
                        val ray = CollisionLine(rayPos, offset)
                        val intersection = blockBoundingBox.intersection(ray) ?: continue
                        val allowed = (intersection - boxPos)[it]
                        if (allowed.absoluteValue < minAllowed.absoluteValue) minAllowed = allowed
                    }
                }

                val allowed = minAllowed
                val trace = offset[it].toDouble()

                val allowedDirection = if (allowed == 0.0) trace.sign else allowed.sign
                val forced = allowedDirection != trace.sign
                val movingInAllowedDirection = attempted.sign == allowedDirection

                val direction = if (forced) allowedDirection else attempted.sign

                val distance = when {
                    forced && movingInAllowedDirection -> {
                        // if we are already moving faster in the forced direction, we will use that movement
                        allowed.absoluteValue.coerceAtLeast(attempted.absoluteValue)
                    }

                    forced                             -> {
                        // if the movement is forced, we can only move into the allowed direction
                        allowed.absoluteValue
                    }

                    movingInAllowedDirection           -> {
                        // if we're attempting movement towards the block, limit to the allowed movement
                        attempted.absoluteValue.coerceAtMost(allowed.absoluteValue)
                    }

                    else                               -> {
                        // otherwise, we're moving away from the block, so we can just carry out the movement normally
                        attempted.absoluteValue
                    }
                }

                vel[it] = (direction * distance).toFloat()

                if (it == 1 && distance == 0.0 && attempted.sign == -1.0) hitGround = true
            }
        }

        entity.withComponent<IsGrounded> { isGrounded = hitGround }
        if (hitGround) vel.y = 0f

        entity.worldPosition += vel
    }
}

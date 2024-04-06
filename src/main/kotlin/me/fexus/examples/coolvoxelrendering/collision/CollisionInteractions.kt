package me.fexus.examples.coolvoxelrendering.collision

import me.fexus.math.vec.DVec3
import me.fexus.math.vec.Vec3
import kotlin.math.max
import kotlin.math.min


fun sphereToSphere(sphere1: CollisionSphere, sphere2: CollisionSphere): Boolean {
    val dist = (sphere1.position - sphere2.position).length
    val neededDist = sphere1.radius + sphere2.radius
    return dist <= neededDist
}

fun aabbToAABB(aabb1: CollisionAABB, aabb2: CollisionAABB): Boolean {
    // Our hitboxes have their origin in the center, hence the position translation
    val aabb1pos = aabb1.position - (aabb1.extent / 2)
    val aabb2pos = aabb2.position - (aabb2.extent / 2)
    return aabb1pos.x <= (aabb2pos.x + aabb2.extent.x) &&
            (aabb1pos.x + aabb1.extent.x) >= aabb2pos.x &&
            aabb1pos.y <= (aabb2pos.y + aabb2.extent.y) &&
            (aabb1pos.y + aabb1.extent.y) >= aabb2pos.y &&
            aabb1pos.z <= (aabb2pos.z + aabb2.extent.z) &&
            (aabb1pos.z + aabb1.extent.z) >= aabb2pos.z
}

fun pointToAABB(aabb: CollisionAABB, point: CollisionPoint) = pointToAABB(point, aabb)
fun pointToAABB(point: CollisionPoint, aabb: CollisionAABB): Boolean {
    val aabbPos = aabb.position - (aabb.extent / 2f)
    return (point.position.x >= aabbPos.x) && (point.position.x <= (aabbPos.x + aabb.extent.x)) &&
            (point.position.y >= aabbPos.y) && (point.position.y <= (aabbPos.y + aabb.extent.y)) &&
            (point.position.z >= aabbPos.z) && (point.position.z <= (aabbPos.z + aabb.extent.z))
}

fun lineToAAPlane(plane: CollisionAAPlane, line: CollisionLine) = lineToAAPlane(line, plane)
fun lineToAAPlane(line: CollisionLine, plane: CollisionAAPlane): Boolean {
    return line.intersection(plane) != null
}

fun pointToAAPlane(plane: CollisionAAPlane, point: CollisionPoint) = pointToAAPlane(point, plane)
fun pointToAAPlane(point: CollisionPoint, plane: CollisionAAPlane): Boolean {
    if (plane.normal.x != 0f) {
        //val range = DoubleRange(plane.position.x, plane.position.x + plane.extent.x)
        if (!(point.position.x >= plane.position.x && point.position.x <= plane.position.x + plane.extent.x)) return false
    }
    if (plane.normal.y != 0f) {
        if (!(point.position.y >= plane.position.y && point.position.y <= plane.position.y + plane.extent.y)) return false
    }
    if (plane.normal.z != 0f) {
        if (!(point.position.z >= plane.position.z && point.position.z <= plane.position.z + plane.extent.z)) return false
    }
    return true
}

fun CollisionLine.intersection(plane: CollisionAAPlane): DVec3? {
    val dot: Float = plane.normal.dot(this.vector)

    // only continue if line and normal point in opposing directions
    if (dot < 0f) {
        val w = this.position - plane.position
        val fac = -(DVec3(plane.normal).dot(w) / dot)
        val u = this.vector * fac
        val intersection = this.position + u
        val distToIntersection = (this.position - intersection).length
        if (distToIntersection > this.vector.length) return null // if vector is too short to reach plane then gtfo
        val collPoint = CollisionPoint(intersection)
        return if (pointToAAPlane(collPoint, plane)) intersection else null
    }

    return null
}


fun CollisionAABB2.intersection(other: CollisionAABB2): Vec3? {
    val min1 = this.min
    val max1 = this.max
    val min2 = other.min
    val max2 = other.max

    val intersectsX = max1.x >= min2.x && min1.x <= max2.x
    val intersectsY = max1.y >= min2.y && min1.y <= max2.y
    val intersectsZ = max1.z >= min2.z && min1.z <= max2.z

    if (!(intersectsX && intersectsY && intersectsZ)) return null

    val extent = (min1 - max2).abs
    if (min1.x >= min2.x && max1.x <= max2.x) extent.x = this.extent.x
    if (min1.y >= min2.y && max1.y <= max2.y) extent.y = this.extent.y
    if (min1.z >= min2.z && max1.z <= max2.z) extent.z = this.extent.z

    return extent
}

// https://tavianator.com/2022/ray_box_boundary.html
fun CollisionAABB.intersection(line: CollisionLine): DVec3? {
    val minPos = this.position - this.extent / 2
    val maxPos = minPos + this.extent
    val normal = line.vector.normalize()

    val inverseDirection = Vec3(1f) / normal
    var tmin = Double.MIN_VALUE
    var tmax = Double.MAX_VALUE

    repeat(3) {
        val t1 = (minPos[it] - line.position[it]) * inverseDirection[it]
        val t2 = (maxPos[it] - line.position[it]) * inverseDirection[it]

        tmin = minButCool(maxButCool(t1, tmin), maxButCool(t2, tmin))
        tmax = maxButCool(minButCool(t1, tmax), minButCool(t2, tmax))
    }

    // no collision
    if (tmin > tmax) return null

    // exit point behind the ray origin
    if (tmax < 0) return null

    // entry point exceeds distance
    if (tmin > line.vector.length) return null
    if (tmin.isNaN()) error("BRUH MOMENT ALERT!!!! $inverseDirection")

    return line.position + normal * tmin
}


// Provided by the man, the myth, the fxsh himself.
private fun minButCool(a: Float, b: Float) = when {
    a.isNaN() -> b
    b.isNaN() -> a
    else      -> min(a, b)
}

private fun minButCool(a: Double, b: Double) = when {
    a.isNaN() -> b
    b.isNaN() -> a
    else      -> min(a, b)
}

private fun maxButCool(a: Float, b: Float) = when {
    a.isNaN() -> b
    b.isNaN() -> a
    else      -> max(a, b)
}

private fun maxButCool(a: Double, b: Double): Double = when {
    a.isNaN() -> b
    b.isNaN() -> a
    else      -> max(a, b)
}

private fun maxButCool(a: Double, b: Float): Double = when {
    a.isNaN() -> b.toDouble()
    b.isNaN() -> a
    else      -> max(a, b.toDouble())
}

fun main() {
    val plane = CollisionAAPlane(DVec3(0.0), Vec3(1,1,0), Vec3(0,0,1))
    val box = CollisionAABB2(Vec3(-0.5f,-0.5f,-0.5f), Vec3(1,1,1))
    box.intersection(plane)
}
fun CollisionAABB2.intersection(plane: CollisionAAPlane): DVec3? {
    val points = plane.getPoints()

    var thing: DVec3? = null
    var doCollide: Boolean = false
    for (it in points) {
        doCollide = (it.x >= this.min.x && it.y >= this.min.y && it.z >= this.min.z &&
                it.x <= this.max.x && it.y <= this.max.y && it.z <= this.max.z)
        if (doCollide) {
            thing = it
            break
        }
    }

    return if (!doCollide) null
    else (max - thing!!).abs * plane.normal
}
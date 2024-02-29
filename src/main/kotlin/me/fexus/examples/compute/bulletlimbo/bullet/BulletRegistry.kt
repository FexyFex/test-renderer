package me.fexus.examples.compute.bulletlimbo.bullet

import me.fexus.examples.compute.bulletlimbo.bullet.type.BulletType


class BulletRegistry {
    private val bulletNames = mutableMapOf<String, BulletType>()
    private val bulletIds = mutableMapOf<Int, BulletType>()
    private var nextID = 1


    fun init(): BulletRegistry {
        BulletType::class.sealedSubclasses.forEach {
            addToRegistry(it.objectInstance!!)
        }
        return this
    }


    fun addToRegistry(bullet: BulletType) {
        bullet.id = nextID++

        bulletNames[bullet.name] = bullet
        bulletIds[bullet.id] = bullet
    }
}
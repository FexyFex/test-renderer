package me.fexus.examples.compute.bulletlimbo.enemy

import me.fexus.examples.compute.bulletlimbo.enemy.type.EnemyType


class EnemyRegistry {
    private val enemyNames = mutableMapOf<String, EnemyType>()
    private val enemyIds = mutableMapOf<Int, EnemyType>()
    private var nextID = 0


    fun init(): EnemyRegistry {
        EnemyType::class.sealedSubclasses.forEach {
            addToRegistry(it.objectInstance!!)
        }
        return this
    }


    fun addToRegistry(enemy: EnemyType) {
        enemy.id = nextID++

        enemyNames[enemy.name] = enemy
        enemyIds[enemy.id] = enemy
    }
}
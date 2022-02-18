package core

import java.util.*

typealias Entity = Int

const val MAX_ENTITIES = 10000

class EntityManager {
    private val _availableEntities = LinkedList<Entity>()
    private val _signatures = Array(MAX_ENTITIES) { Signature() }
    private var _entityCount = 0

    init {
        for (i in MAX_ENTITIES - 1 downTo 0) _availableEntities.push(i)
    }

    fun createEntity() : Entity {
        assert(_entityCount < MAX_ENTITIES) { "Too many entities in game." }
        val entity = _availableEntities.pop()
        _entityCount++

        return entity
    }

    fun destroyEntity(entity: Entity) {
        assert(_entityCount < MAX_ENTITIES) { "Invalid entity ID: $entity (out of range)"}
        _signatures[entity].clear()
        _availableEntities.push(entity)
        _entityCount--
    }

    fun setSignature(entity: Entity, signature: Signature) {
        assert(entity < MAX_ENTITIES) { "Invalid entity ID: $entity (out of range)" }
        _signatures[entity] = signature
    }

    fun getSignature(entity: Entity) : Signature {
        assert(entity < MAX_ENTITIES) { "Invalid entity ID: $entity (out of range)" }
        return _signatures[entity]
    }

}

package core

import java.util.*

typealias Entity = Int

class EntityManager {

    class IdCounter {
        private var totalElements = 0
        private val holeQueue : Queue<Int> = LinkedList<Int>()

        fun nextElement() : Int {
            var element = totalElements
            if (holeQueue.size > 0) {
                element = holeQueue.poll()
            }
            totalElements++
            return element
        }

        fun deleteElement(element: Int) {
            totalElements--
            holeQueue.add(element)
        }

        fun peekElement() : Int {
            if (holeQueue.size > 0)
                return holeQueue.peek()
            return totalElements
        }
    }

    private val _indexCounter = IdCounter()
    private val _entitySignature = ArrayList<Signature>()

    fun createEntity() : Entity {
        val entity = _indexCounter.nextElement()
        if (_entitySignature.size <= entity) {
            _entitySignature.add(Signature(maxComponentTypes))
        }
        else {
            _entitySignature[entity] = Signature(maxComponentTypes)
        }
        return entity
    }

    fun destroyEntity(entity: Entity) {
        _indexCounter.deleteElement(entity)
        _entitySignature[entity] = Signature()
    }

    fun setSignature(entity: Entity, signature: Signature) {
        _entitySignature[entity] = signature
    }

    fun getSignature(entity: Entity) : Signature = _entitySignature[entity]

    fun hasEntity(entity: Entity) : Boolean {
        return _indexCounter.peekElement() < entity
    }
}
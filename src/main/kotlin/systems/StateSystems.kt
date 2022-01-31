package systems

import components.*
import components.MoveCommand.Direction
import core.*
import java.util.*
import kotlin.collections.HashMap


private abstract class State(protected val _entity: Entity) {

    private val eventQueue = LinkedList<Event>();
    private val commandQueue = LinkedList<Command>();

    abstract fun update(instance: Instance, delta: Float): State?
    abstract fun onCommand(instance: Instance, command: Command): State?
    abstract fun onEvent(event: Event): State?
}

private class IdleState(entity: Entity) : State(entity) {

    override fun update(instance: Instance, delta: Float): State? {
        // does nothing
        return null
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        if (command is MoveCommand) {
            // movement command, return movement state
            return MovingState(command.direction, _entity)
        } else if (command is ShootCommand) {
            // returns shooting state
            return ShootingState(command.angle, _entity)
        }
        return null
    }

    override fun onEvent(event: Event): State? {
        if (event is HitEvent) {
            return HitState(event.duration, event.entity)
        }
        return null
    }
}

private class MovingState(val direction: Direction, entity: Entity) : State(entity) {
    override fun update(instance: Instance, delta: Float): State? {
        // get character max speed
        val characterComponent = instance.getComponent<CharacterComponent>(_entity)
        val dynamicComponent = instance.getComponent<DynamicComponent>(_entity)

        // updates character max speed before going through the dynamic system
        when (direction) {
            Direction.UP -> dynamicComponent.speed =
                Vec3F(0.0f, characterComponent.maxSpeed, 0.0f)
            Direction.DOWN -> dynamicComponent.speed =
                Vec3F(0.0f, -characterComponent.maxSpeed, 0.0f)
            Direction.LEFT -> dynamicComponent.speed =
                Vec3F(-characterComponent.maxSpeed, 0.0f, 0.0f)
            Direction.RIGHT -> dynamicComponent.speed =
                Vec3F(characterComponent.maxSpeed, 0.0f, 0.0f)
        }

        return null
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        when (command) {
            is StopCommand -> return IdleState(_entity)
            is ShootCommand -> return ShootingState(command.angle, _entity)
            is MoveCommand -> return MovingState(command.direction, _entity)
            else -> return null
        }
    }

    override fun onEvent(event: Event): State? {
        if (event is HitEvent) {
            return HitState(event.duration, event.entity)
        }
        return null
    }
}

private class ShootingState(angle: Float, entity: Entity) : State(entity) {
    override fun update(instance: Instance, delta: Float): State? {
        TODO("Not yet implemented")
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        TODO("Not yet implemented")
    }

    override fun onEvent(event: Event): State? {
        TODO("Not yet implemented")
    }
}

private class HitState(duration: Float, entity: Entity) : State(entity) {
    override fun update(instance: Instance, delta: Float): State? {
        TODO("Not yet implemented")
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        TODO("Not yet implemented")
    }

    override fun onEvent(event: Event): State? {
        TODO("Not yet implemented")
    }
}

class StateSystem : System() {

    private val entityStates = HashMap<Entity, State>()

    override fun initializeLogic(vararg arg: Any): Boolean {
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        entities.forEach { entity ->
            val commandComponent = instance.getComponent<CommandComponent>(entity)
            commandComponent.commands.forEach { cmd ->
                if (cmd is StateCommand) {
                    val newState = entityStates[entity]!!.onCommand(instance, cmd)
                    commandComponent.commands.remove(cmd) // possible illegal since we are looping
                    if (newState != null) entityStates[entity] = newState
                }
            }
            val newState = entityStates[entity]!!.update(instance, delta)
            if (newState != null) entityStates[entity] = newState
        }
    }

    override fun onEntityAdded(entity: Entity) {
        entityStates[entity] = IdleState(entity)
    }

    override fun onEntityRemoved(entity: Entity) {
        entityStates.remove(entity)
    }

    override fun onEvent(event: Event, observable: IObservable) {
        if (event is EntityEvent) {
            entityStates[event.entity]!!.onEvent(event)
        }
    }
}
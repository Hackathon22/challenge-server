package systems

import components.*
import core.*
import java.util.*
import kotlin.collections.HashMap


private abstract class State(protected val _entity: Entity) {

    private val eventQueue = LinkedList<Event>();
    private val commandQueue = LinkedList<Command>();

    abstract val state : States

    abstract fun update(instance: Instance, delta: Float): State?
    abstract fun onCommand(instance: Instance, command: Command): State?
    abstract fun onEvent(event: Event): State?


    abstract fun onStateBegin(instance: Instance)
    abstract fun onStateEnd(instance: Instance)

    protected fun setState(instance: Instance) {
        val stateComponent = instance.getComponent<StateComponent>(_entity)
        stateComponent.state = state
    }
}

private class IdleState(entity: Entity) : State(entity) {

    override val state: States
        get() = States.IDLE

    override fun update(instance: Instance, delta: Float): State? {
        val dynamicComponent = instance.getComponent<DynamicComponent>(_entity)
        dynamicComponent.speed = Vec3F(0f, 0f, 0f)

        // does nothing
        return null
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        if (command is MoveCommand) {
            // movement command, return movement state
            if (!command.release)
                return MovingState(command.direction, _entity)
        } else if (command is ShootCommand) {
            // returns shooting state
            return ShootingState(_entity)
        }
        return null
    }

    override fun onEvent(event: Event): State? {
        if (event is HitEvent) {
            return HitState(event.duration, event.entity)
        }
        return null
    }

    override fun onStateBegin(instance: Instance) {
        setState(instance)
    }

    override fun onStateEnd(instance: Instance) {
    }
}

private class MovingState(private var _direction: Vec3F, entity: Entity) : State(entity) {

    override val state: States
        get() = States.MOVING

    override fun update(instance: Instance, delta: Float): State? {
        setState(instance)

        // get character max speed
        val characterComponents = instance.getComponent<CharacterComponent>(_entity)
        val dynamicComponent = instance.getComponent<DynamicComponent>(_entity)

        // updates character max speed before going through the dynamic system
        dynamicComponent.speed = _direction * characterComponents.maxSpeed

        return null
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        return when (command) {
            is ShootCommand -> ShootingState(_entity)
            is MoveCommand -> changeDirection(instance, command.direction)
            else -> null
        }
    }

    override fun onEvent(event: Event): State? {
        if (event is HitEvent) {
            return HitState(event.duration, event.entity)
        }
        return null
    }

    fun changeDirection(instance: Instance, direction : Vec3F) : State? {
        _direction += direction

        // get character max speed
        val characterComponents = instance.getComponent<CharacterComponent>(_entity)
        val dynamicComponent = instance.getComponent<DynamicComponent>(_entity)

        // updates character max speed before going through the dynamic system
        dynamicComponent.speed = _direction * characterComponents.maxSpeed

        if (_direction.x == 0f && _direction.y == 0f && _direction.z == 0f) return IdleState(_entity)
        return null
    }

    override fun onStateBegin(instance: Instance) {
        setState(instance)
    }

    override fun onStateEnd(instance: Instance) {
        _direction = Vec3F(0.0f, 0.0f, 0.0f)
        changeDirection(instance, Vec3F(0.0f, 0.0f, 0.0f))
    }
}

private class ShootingState(entity: Entity) : State(entity) {

    override val state: States
        get() = States.SHOOTING

    private var _recoveryTime : Float? = null

    override fun update(instance: Instance, delta: Float): State? {
        this._recoveryTime = this._recoveryTime!!.minus(delta)
        if (this._recoveryTime!! <= 0) return IdleState(_entity)
        return null
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        // as the character is on cooldown, it cannot move
        return null
    }

    override fun onEvent(event: Event): State? {
        return when (event) {
            is HitEvent -> HitState(event.duration, _entity)
            else -> null
        }
    }

    override fun onStateBegin(instance: Instance) {
        setState(instance)
        val weaponComponent = instance.getComponent<WeaponComponent>(_entity) ?: return
        val weaponSystem = instance.getSystem<WeaponSystem>()
        _recoveryTime = weaponSystem.shoot(instance, _entity)
    }

    override fun onStateEnd(instance: Instance) {
    }
}

private class HitState(private var _duration: Float, entity: Entity) : State(entity) {

    override val state: States
        get() = States.HIT

    override fun update(instance: Instance, delta: Float): State? {
        _duration -= delta
        if (_duration <= 0) {
            return IdleState(_entity)
        }
        return null
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        return null
    }

    override fun onEvent(event: Event): State? {
        return when (event) {
            is HitEvent -> HitState(event.duration, _entity)
            else -> null
        }
    }

    override fun onStateBegin(instance: Instance) {
        setState(instance)
    }

    override fun onStateEnd(instance: Instance) {
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
            val commandIterator = commandComponent.commands.iterator()
            while (commandIterator.hasNext()) {
                val cmd = commandIterator.next()
                if (cmd is StateCommand) {
                    val newState = entityStates[entity]!!.onCommand(instance, cmd)
                    commandIterator.remove()
                    changeState(instance, newState, entity)
                }
            }
            val newState = entityStates[entity]!!.update(instance, delta)
            changeState(instance, newState, entity)
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
            entityStates[event.entity]?.onEvent(event)
        }
    }

    private fun changeState(instance: Instance, state: State?, entity: Entity) {
        if (state != null) {
            entityStates[entity]?.onStateEnd(instance)
            entityStates[entity] = state
            entityStates[entity]?.onStateBegin(instance)
        }
    }
}
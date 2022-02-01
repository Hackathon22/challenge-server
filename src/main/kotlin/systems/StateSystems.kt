package systems

import components.*
import components.MoveCommand.Direction
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

    protected fun setState(instance: Instance) {
        val stateComponent = instance.getComponent<StateComponent>(_entity)
        stateComponent.state = state
    }
}

private class IdleState(entity: Entity) : State(entity) {

    override val state: States
        get() = States.IDLE

    override fun update(instance: Instance, delta: Float): State? {
        setState(instance)
        // does nothing
        return null
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        if (command is MoveCommand) {
            // movement command, return movement state
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
}

private class MovingState(val direction: Direction, entity: Entity) : State(entity) {

    override val state: States
        get() = States.MOVING

    override fun update(instance: Instance, delta: Float): State? {
        setState(instance)
        // get character max speed
        val characterComponents = instance.getComponent<CharacterComponent>(_entity)
        val dynamicComponent = instance.getComponent<DynamicComponent>(_entity)

        // updates character max speed before going through the dynamic system
        when (direction) {
            Direction.UP -> dynamicComponent.speed =
                Vec3F(0.0f, characterComponents.maxSpeed, 0.0f)
            Direction.DOWN -> dynamicComponent.speed =
                Vec3F(0.0f, -characterComponents.maxSpeed, 0.0f)
            Direction.LEFT -> dynamicComponent.speed =
                Vec3F(-characterComponents.maxSpeed, 0.0f, 0.0f)
            Direction.RIGHT -> dynamicComponent.speed =
                Vec3F(characterComponents.maxSpeed, 0.0f, 0.0f)
        }

        return null
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        return when (command) {
            is StopCommand -> IdleState(_entity)
            is ShootCommand -> ShootingState(_entity)
            is MoveCommand -> MovingState(command.direction, _entity)
            else -> null
        }
    }

    override fun onEvent(event: Event): State? {
        if (event is HitEvent) {
            return HitState(event.duration, event.entity)
        }
        return null
    }
}

private class ShootingState(entity: Entity) : State(entity) {

    override val state: States
        get() = States.SHOOTING

    // Was the shot made?
    private var _hasShot = false

    private var _recoveryTime : Float? = null

    override fun update(instance: Instance, delta: Float): State? {
        setState(instance)
        if (!_hasShot) {
            // checks if the state entity has a weapon, if not return to Idle
            val weaponComponent =
                instance.getComponent<WeaponComponent>(_entity) ?: return IdleState(_entity);
            val weaponSystem = instance.getSystem<WeaponSystem>()
            _recoveryTime = weaponSystem.shoot(instance, _entity)
            _hasShot = true
        }
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
}

private class HitState(private var _duration: Float, entity: Entity) : State(entity) {

    override val state: States
        get() = States.HIT

    override fun update(instance: Instance, delta: Float): State? {
        setState(instance)
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
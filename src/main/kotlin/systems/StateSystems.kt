package systems

import components.*
import core.*
import kotlin.math.PI
import kotlin.math.atan


private abstract class State(protected val _entity: Entity) {

    abstract val state: States

    abstract fun update(instance: Instance, delta: Float): State?
    abstract fun onCommand(instance: Instance, command: Command): State?
    abstract fun onEvent(event: Event): State?


    abstract fun onStateBegin(instance: Instance)
    abstract fun onStateEnd(instance: Instance)

    protected fun setState(instance: Instance) {
        val stateComponent = instance.getComponent<StateComponent>(_entity)
        stateComponent.state = state
    }

    protected fun rotate(instance: Instance, cursorPosition: Vec3F) {
        val transformComponent = instance.getComponent<TransformComponent>(_entity)
        // get the angle
        var angle =
            (if (cursorPosition.x == transformComponent.pos.x) PI/2f else atan((cursorPosition.y - transformComponent.pos.y) / (cursorPosition.x - transformComponent.pos.x))).toFloat()
        if (cursorPosition.x < transformComponent.pos.x) {
            angle += PI.toFloat()
        }

        val degreeAngle = angle * (180f / PI.toFloat())
        transformComponent.rot.z = degreeAngle
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
            if (!command.release) return MovingState(command.direction, _entity)
        } else if (command is ShootCommand) {
            // returns shooting state
            return ShootingState(_entity, command.angle)
        } else if (command is CursorMovedCommand) {
            rotate(instance, command.worldPosition)
        }
        return null
    }

    override fun onEvent(event: Event): State? {
        if (event is HitEvent) {
            return HitState(event.duration, event.entity)
        }
        if (event is DamageEvent) {
            if (event.fatal) return DeadState(entity = _entity)
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
        applyDirection(instance)
        return null
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        when (command) {
            is ShootCommand -> return ShootingState(_entity, command.angle)
            is MoveCommand -> changeDirection(instance, command.direction)
            is CursorMovedCommand -> rotate(instance, command.worldPosition)
        }
        return null
    }

    override fun onEvent(event: Event): State? {
        if (event is HitEvent) {
            return HitState(event.duration, event.entity)
        }
        if (event is CollisionEvent) {
            if (event.entity == _entity || event.collidedEntity == _entity)
                return IdleState(_entity)
        }
        if (event is DamageEvent) {
            if (event.fatal) return DeadState(entity = _entity)
        }
        return null
    }

    fun changeDirection(instance: Instance, direction: Vec3F): State? {
        if (_direction != direction) {
            _direction = direction

            applyDirection(instance)
            if (_direction.x == 0f && _direction.y == 0f && _direction.z == 0f) return IdleState(_entity)
        }
        return null
    }

    fun applyDirection(instance: Instance) {
        // get character max speed
        val characterComponents = instance.getComponent<CharacterComponent>(_entity)
        val dynamicComponent = instance.getComponent<DynamicComponent>(_entity)

        // normalizes the direction
        val normalizedDirection = _direction.normalized()

        // updates character max speed before going through the dynamic system
        dynamicComponent.speed = normalizedDirection * characterComponents.maxSpeed
    }

    override fun onStateBegin(instance: Instance) {
        setState(instance)
    }

    override fun onStateEnd(instance: Instance) {
        changeDirection(instance, Vec3F(0.0f, 0.0f, 0.0f))
    }
}

private class ShootingState(entity: Entity, val angle: Float? = null) : State(entity) {

    override val state: States
        get() = States.SHOOTING

    private var _recoveryTime: Float? = null

    override fun update(instance: Instance, delta: Float): State? {
        this._recoveryTime = this._recoveryTime!!.minus(delta)
        if (this._recoveryTime!! <= 0)
            return IdleState(_entity)
        return null
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        // as the character is on cooldown, it cannot move
        return null
    }

    override fun onEvent(event: Event): State? {
        return when (event) {
            is HitEvent -> HitState(event.duration, _entity)
            is DamageEvent -> return if (event.fatal) DeadState(entity = _entity) else null
            else -> null
        }
    }

    override fun onStateBegin(instance: Instance) {
        setState(instance)
        val weaponSystem = instance.getSystem<WeaponSystem>()
        _recoveryTime = weaponSystem.shoot(instance, _entity, angle)
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
            is DamageEvent -> return if (event.fatal) DeadState(entity = _entity) else null
            else -> null
        }
    }

    override fun onStateBegin(instance: Instance) {
        setState(instance)
    }

    override fun onStateEnd(instance: Instance) {
    }
}

private class DeadState(private var _duration: Float = 5f, entity: Entity) : State(entity) {

    private var waitTime = 0f

    override val state: States
        get() = States.DEAD

    override fun update(instance: Instance, delta: Float): State? {
        waitTime += delta
        if (waitTime >= _duration) {
            return IdleState(_entity)
        }
        return null
    }

    override fun onCommand(instance: Instance, command: Command): State? {
        return null
    }

    override fun onEvent(event: Event): State? {
        return null
    }

    override fun onStateBegin(instance: Instance) {
        // moves the player away from the map
        val transformComponent = instance.getComponent<TransformComponent>(_entity)
        transformComponent.pos.x = -1000f
        transformComponent.pos.y = -1000f
    }

    override fun onStateEnd(instance: Instance) {
        val spawnerSystem = instance.getSystem<SpawnerSystem>()
        spawnerSystem.respawn(instance, _entity)
    }
}

class StateSystem : System() {

    private val _entityStates = HashMap<Entity, State>()

    override fun initializeLogic(vararg arg: Any): Boolean {
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        entities.forEach { entity ->
            val commandComponent = instance.getComponent<CommandComponent>(entity)
            val commandIterator = commandComponent.commands.iterator()
            while (commandIterator.hasNext()) {
                val cmd = commandIterator.next()
                if (cmd is StateCommand || cmd is CursorMovedCommand) {
                    val newState = _entityStates[entity]!!.onCommand(instance, cmd)
                    commandIterator.remove()
                    if (newState != null) changeState(instance, newState, entity)
                }
            }
            val newState = _entityStates[entity]!!.update(instance, delta)
            if (newState != null) changeState(instance, newState, entity)
        }
    }

    override fun onEntityAdded(entity: Entity) {
        _entityStates[entity] = IdleState(entity)
    }

    override fun onEntityRemoved(entity: Entity) {
        _entityStates.remove(entity)
    }

    override fun onEvent(event: Event, observable: IObservable, instance: Instance) {
        if (event is EntityEvent) {
            val newState = _entityStates[event.entity]?.onEvent(event)
            if (newState != null) changeState(instance, newState, event.entity)
        }
        if (event is CollisionEvent) {
            var newState = _entityStates[event.entity]?.onEvent(event)
            if (newState != null) changeState(instance, newState, event.entity)

            newState = _entityStates[event.collidedEntity]?.onEvent(event)
            if (newState != null) changeState(instance, newState, event.collidedEntity)
        }
    }

    private fun changeState(instance: Instance, state: State, entity: Entity) {
        _entityStates[entity]?.onStateEnd(instance)
        _entityStates[entity] = state
        _entityStates[entity]?.onStateBegin(instance)
    }
}
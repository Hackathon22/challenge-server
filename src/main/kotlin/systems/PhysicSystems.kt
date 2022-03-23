package systems

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import components.BodyComponent
import components.DynamicComponent
import components.TransformComponent
import core.*
import java.util.*
import kotlin.math.PI

class MovementSystem : System() {

    override fun initializeLogic(vararg arg: Any): Boolean {
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        entities.forEach {
            val transformComponent = instance.getComponent<TransformComponent>(it)
            val dynamicComponent = instance.getComponent<DynamicComponent>(it)

            // acceleration modifies speed
            dynamicComponent.speed += dynamicComponent.acceleration * delta
            dynamicComponent.speed.y += dynamicComponent.gravity * delta
            // speed modifies position
            transformComponent.pos += dynamicComponent.speed * delta
        }
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }
}

class CollisionSystem : System() {

    private val _entitiesToBodies = HashMap<Entity, Body>()

    private val _bodiesToEntities = HashMap<Body, Entity>()

    private val _callbackPositions = HashMap<Entity, Vec3F>()

    private val _collisionQueue = LinkedList<Triple<Entity, Entity, Vec3F>>()

    private val _entityCollided = HashMap<Entity, Boolean>()

    private val _entityPositions = HashMap<Entity, Vec3F>()

    private val _addedEntities = LinkedList<Entity>()

    private val _removedEntities = LinkedList<Entity>()

    private val _world = World(Vector2(0.0f, 0.0f), false)

    override fun initializeLogic(vararg arg: Any): Boolean {
        try {
            val instance = arg[0] as Instance
            val contactListener = object : ContactListener {
                override fun beginContact(contact: Contact) {
                    val entityA = _bodiesToEntities[contact.fixtureA.body]
                    val entityB = _bodiesToEntities[contact.fixtureB.body]

                    if (entityA != null && entityB != null && entityA != entityB) {

                        // computes the collision normal vector
                        val normalVector = Vec3F(
                            contact.worldManifold.normal.x,
                            contact.worldManifold.normal.y,
                            0f
                        )

                        // rolling back entity position to last non colliding position
                        _callbackPositions[entityA] = _entityPositions[entityA]!!
                        _callbackPositions[entityB] = _entityPositions[entityB]!!


                        if (!_entityCollided[entityA]!! || !_entityCollided[entityB]!!) {
                            _entityCollided[entityA] = true
                            _entityCollided[entityB] = true

                            // enqueues the collisions so they can be notified to listeners in the update
                            // cycle
                            _collisionQueue.add(Triple(entityA, entityB, normalVector))
                        }
                    }
                }

                override fun endContact(contact: Contact) {
                }

                override fun preSolve(contact: Contact, oldManifold: Manifold?) {
                }

                override fun postSolve(contact: Contact, impulse: ContactImpulse?) {
                }
            }
            _world.setContactListener(contactListener)
            return true
        } catch (exc: TypeCastException) {
            return false
        }
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        _removedEntities.forEach {
            val body = _entitiesToBodies[it]
            if (body != null)
                _world.destroyBody(_entitiesToBodies[it])
            _entitiesToBodies.remove(it)
            _bodiesToEntities.remove(body)
            _entityCollided.remove(it)
        }

        _addedEntities.forEach {
            // adds the entity entry in the position registry
            val transformComponent = instance.getComponent<TransformComponent>(it)
            _entityPositions[it] = Vec3F(transformComponent.pos)

            // gets the hitbox from the body component
            val bodyComponent = instance.getComponent<BodyComponent>(it)
            val width = bodyComponent.width
            val height = bodyComponent.height

            // box2d body, defines the object to the world
            val bodyDef = BodyDef()
            bodyDef.type =
                if (!bodyComponent.static) BodyDef.BodyType.DynamicBody else BodyDef.BodyType.StaticBody
            bodyDef.fixedRotation = true

            // box2d fixture, defines the collision box
            val fixDef = FixtureDef()

            // creates a rectangle around the body, the x, y position is on the bottom left
            val shape = PolygonShape()
            shape.setAsBox(width / 2.0f, height / 2.0f)

            fixDef.shape = shape
            fixDef.isSensor = true // only behaves as a sensor (no knockback on collision)
            // adds the body, and it's fixture to the world
            val body = _world.createBody(bodyDef).createFixture(fixDef).body

            body.setTransform(
                transformComponent.pos.x,
                transformComponent.pos.y,
                transformComponent.rot.z * (PI.toFloat() / 180f)
            )

            _entitiesToBodies[it] = body
            _bodiesToEntities[body] = it
            _entityCollided[it] = false
        }
        _addedEntities.clear()
        _removedEntities.clear()

        // saves the entity position
        entities.forEach {
            // sets the body position in the world
            val transformComponent = instance.getComponent<TransformComponent>(it)
            val dynamicComponent = instance.getComponentDynamicUnsafe(it, DynamicComponent::class)
            _entitiesToBodies[it]?.setTransform(
                transformComponent.pos.x,
                transformComponent.pos.y,
                transformComponent.rot.z * (PI.toFloat() / 180f)
            )
            if (dynamicComponent != null) {
                val dynamicComponentCasted = dynamicComponent as DynamicComponent
                _entitiesToBodies[it]?.setLinearVelocity(dynamicComponentCasted.speed.x, dynamicComponent.speed.y)
            }
        }

        // detects collisions, rolling back the current positions
        _world.step(delta, 1, 1)

        // rolling back position to last non-colliding position
        _callbackPositions.forEach { (entity, pos) ->
            // saves the rolled back position to the physical model
            val transformComponent = instance.getComponent<TransformComponent>(entity)
            val difference = transformComponent.pos - pos
            transformComponent.pos.set(transformComponent.pos - (difference * 3f))
            _entitiesToBodies[entity]?.setTransform(
                transformComponent.pos.x,
                transformComponent.pos.y,
                transformComponent.rot.z * (PI.toFloat() / 180f)
            )
        }
        _callbackPositions.clear()

        // saving non colliding positions
        entities.forEach {
            // saves the last transform
            if (!_entityCollided[it]!!) {
                val transformComponent = instance.getComponent<TransformComponent>(it)
                _entityPositions[it]?.set(transformComponent.pos)
            }
            _entityCollided[it] = false
        }

        // emitting collision events to other systems
        _collisionQueue.forEach {
            notifyObservers(
                CollisionEvent(it.first, it.second, it.third), instance
            )
        }
        _collisionQueue.clear()
    }

    override fun onEntityAdded(entity: Entity) {
        if (!_addedEntities.contains(entity))
            _addedEntities.add(entity)
    }

    override fun onEntityRemoved(entity: Entity) {
        if (!_removedEntities.contains(entity))
            _removedEntities.add(entity)
    }
}
package systems

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import components.BodyComponent
import components.DynamicComponent
import components.TransformComponent
import core.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.PI

const val pixelsToMetersRatio = 1.0f / 50.0f

class PhysicsSystem : System() {

    private val _entitiesToBodies = HashMap<Entity, Body>()
    private val _bodiesToEntities = HashMap<Body, Entity>()

    private val _world = World(Vector2(0.0f, 0.0f), false)

    // Yes this is unholy af
    private var _instance: Instance? = null

    override fun initializeLogic(vararg arg: Any): Boolean {
        if (arg.isEmpty()) return false
        return try {
            _instance = arg[0] as Instance
            val contactListener = object : ContactListener {
                override fun beginContact(contact: Contact) {
                    val entityA = _bodiesToEntities[contact.fixtureA.body]!!
                    val entityB = _bodiesToEntities[contact.fixtureB.body]!!
                    val angle = contact.worldManifold.normal.angleDeg()
                    notifyObservers(CollisionEvent(entityA, entityB, angle), _instance!!)
                }

                override fun endContact(contact: Contact) {
                }

                override fun preSolve(contact: Contact, oldManifold: Manifold) {
                }

                override fun postSolve(contact: Contact, impulse: ContactImpulse) {
                }
            }
            _world.setContactListener(contactListener)
            true
        } catch (exc: TypeCastException) {
            false
        }
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        // uploads position, speed etc to the world
        entities.forEach {
            val body = _entitiesToBodies[it]!!
            val transformComponent = instance.getComponent<TransformComponent>(it)
            val dynamicComponent = instance.getComponentDynamicUnsafe(it, DynamicComponent::class)
            if (dynamicComponent is DynamicComponent) {
                body.setTransform(
                    transformComponent.pos.x * pixelsToMetersRatio,
                    transformComponent.pos.y * pixelsToMetersRatio,
                    transformComponent.rot.z * PI.toFloat() / 180.0f
                )
                body.setLinearVelocity(
                    dynamicComponent.speed.x * pixelsToMetersRatio,
                    dynamicComponent.speed.y * pixelsToMetersRatio
                )
            }
        }

        // performs a simulation step
        _world.step(delta, 6, 6)

        // downloads the position and speed to the world
        entities.forEach {
            val body = _entitiesToBodies[it]!!
            val transformComponent = instance.getComponent<TransformComponent>(it)
            transformComponent.pos.x = body.position.x / pixelsToMetersRatio
            transformComponent.pos.y = body.position.y / pixelsToMetersRatio
        }
    }

    override fun onEntityAdded(entity: Entity) {
        if (_entitiesToBodies[entity] == null) {
            val transformComponent = _instance!!.getComponent<TransformComponent>(entity)
            val bodyComponent = _instance!!.getComponent<BodyComponent>(entity)
            val dynamicComponent =
                _instance!!.getComponentDynamicUnsafe(entity, DynamicComponent::class)

            val width = bodyComponent.width
            val height = bodyComponent.height

            // adds body
            val bodyDefinition = BodyDef()
            if (dynamicComponent is DynamicComponent)
                bodyDefinition.type = BodyDef.BodyType.DynamicBody
            else
                bodyDefinition.type = BodyDef.BodyType.StaticBody
            // bodyDefinition.fixedRotation = true

            val fixtureDef = FixtureDef()
            fixtureDef.friction = 0.0f
            fixtureDef.density = 0.0f
            fixtureDef.restitution = 0.0f
            val fixtureShape = PolygonShape()
            fixtureShape.setAsBox(
                width * pixelsToMetersRatio / 2.0f,
                height * pixelsToMetersRatio / 2.0f
            )
            fixtureDef.shape = fixtureShape

            val body = _world.createBody(bodyDefinition).createFixture(fixtureDef).body

            body.massData.mass = 0.0f

            body.setTransform(
                transformComponent.pos.x * pixelsToMetersRatio,
                transformComponent.pos.y * pixelsToMetersRatio,
                transformComponent.rot.z * (PI.toFloat() / 180.0f)
            )

            _entitiesToBodies[entity] = body
            _bodiesToEntities[body] = entity
        }
    }

    override fun onEntityRemoved(entity: Entity) {
        if (_entitiesToBodies[entity] != null) {
            val removedBody = _entitiesToBodies[entity]
            _entitiesToBodies.remove(entity)
            _bodiesToEntities.remove(removedBody)
            _world.destroyBody(removedBody)
        }
    }

}
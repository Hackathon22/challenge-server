package systems

import com.badlogic.gdx.math.MathUtils.*
import components.*
import core.*
import game.EntityRegistry
import java.util.*
import kotlin.collections.ArrayList

/**
 * System that takes care of adding new projectile upon shooting, and that takes care of projectile collision events
 */
class WeaponSystem : System() {

    override fun initializeLogic(vararg arg: Any): Boolean {
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }

    /**
     * Creates a projectile, giving to it a certain angle and speed. Then adds the projectile to the
     * instance entities.
     */
    fun shoot(instance: Instance, entity: Entity): Float {
        // checks for entity weapon component
        val entityWeaponComponent = instance.getComponent<WeaponComponent>(entity)
        val entityTransformComponent = instance.getComponent<TransformComponent>(entity)

        val shooterAngle = entityTransformComponent.rot.z

        // initializes the projectile
        val projectile = instance.createEntity()

        // transform component, giving the position of the player + 10 pixels
        val transformComponent = TransformComponent()

        transformComponent.pos.x =
            entityTransformComponent.pos.x + 32 * cosDeg(shooterAngle)
        transformComponent.pos.y =
            entityTransformComponent.pos.y + 32 * sinDeg(shooterAngle)

        transformComponent.rot.z = shooterAngle

        // dynamic component, giving the speed of the projectile
        val dynamicComponent = DynamicComponent()
        dynamicComponent.speed.x = entityWeaponComponent.projectile.maxSpeed * cosDeg(shooterAngle)
        dynamicComponent.speed.y = entityWeaponComponent.projectile.maxSpeed * sinDeg(shooterAngle)

        // projectile component, containing all the projectile information
        val projectileComponent = ProjectileComponent(
            entityWeaponComponent.projectile.maxBounces,
            entityWeaponComponent.projectile.maxTime,
            entityWeaponComponent.impact.copy(),
            entityWeaponComponent.projectile.copy()
        )

        val bodyComponent = BodyComponent(
            entityWeaponComponent.projectile.width,
            entityWeaponComponent.projectile.height
        )

        val spriteComponent = SpriteComponent(entityWeaponComponent.projectileSprite)


        instance.addComponent(projectile, transformComponent)
        instance.addComponent(projectile, dynamicComponent)
        instance.addComponent(projectile, projectileComponent)
        instance.addComponent(projectile, spriteComponent)
        instance.addComponent(projectile, bodyComponent)

        return entityWeaponComponent.coolDown
    }

    override fun onEvent(event: Event, observable: IObservable, instance: Instance) {
    }
}

/**
 * System that takes care of tracking all the projectile lifetime, bounces and impacts.
 */
class ProjectileSystem : System() {

    /**
     * Contains a list of pairs containing the collided entities as well as their
     */
    private val _collidedEntities = LinkedList<Triple<Entity, Entity, Float>>()

    override fun initializeLogic(vararg arg: Any): Boolean {
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        // decreases the projectiles lifetime
        val toRemoveEntities = ArrayList<Entity>()
        entities.forEach {
            val projectileComponent = instance.getComponent<ProjectileComponent>(it)
            projectileComponent.remainingTime -= delta
            if (projectileComponent.remainingTime < 0) {
                explode(it, instance)
                toRemoveEntities.add(it)
            }
        }
        _collidedEntities.forEach {
            // get the projectile's component, if it still exists
            val projectileComponent =
                instance.getComponentDynamicUnsafe(it.first, ProjectileComponent::class) ?: return

            // checks if the projectile collided with a character
            val collidedCharacter =
                instance.getComponentDynamicUnsafe(it.second, CharacterComponent::class)
            if (collidedCharacter != null) {
                explode(it.first, instance)
                toRemoveEntities.add(it.first)
                return@forEach
            }

            // reduces the bounces
            (projectileComponent as ProjectileComponent).remainingBounces -= 1
            if ((projectileComponent as ProjectileComponent).remainingBounces < 0) {
                explode(it.first, instance)
                toRemoveEntities.add(it.first)
            } else bounce(it.first, instance, it.third)
        }

        toRemoveEntities.forEach {
            instance.destroyEntity(it)
        }
        _collidedEntities.clear()
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }

    /**
     * Projectile explosion on other entities.
     */
    private fun explode(entity: Entity, instance: Instance) {
        // projectile's position
        val projectileTransformComponent = instance.getComponent<TransformComponent>(entity)

        // adds an explosion entity
        val components = EntityRegistry.loadEntity("baseExplosion")
        val explosionEntity = instance.createEntity()
        components.forEach {
            instance.addComponentDynamic(explosionEntity, it)
        }

        // sets explosion position
        val explosionTransformComponent = instance.getComponent<TransformComponent>(explosionEntity)
        explosionTransformComponent.pos.set(projectileTransformComponent.pos)

        // implements damage, take all the players and computes from the radius

    }

    private fun bounce(entity: Entity, instance: Instance, angle: Float) {
        // changes the projectile speed
        val dynamicComponent = instance.getComponent<DynamicComponent>(entity)

        // TODO implement bounce by changing the dynamic component
    }

    override fun onEvent(event: Event, observable: IObservable, instance: Instance) {
        if (event is CollisionEvent) {
            if (event.entity in entities) {
                // TODO compute angle from normal vector
                _collidedEntities.add(Triple(event.entity, event.collidedEntity, 0f))
            }
        }
    }
}
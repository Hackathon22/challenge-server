package systems

import com.badlogic.gdx.math.MathUtils.*
import components.*
import core.*

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
            entityTransformComponent.pos.x + 10 * cosDeg(shooterAngle)
        transformComponent.pos.y =
            entityTransformComponent.pos.y + 10 * sinDeg(shooterAngle)

        transformComponent.rot.z = shooterAngle

        // dynamic component, giving the speed of the projectile
        val dynamicComponent = DynamicComponent()
        dynamicComponent.speed.x = entityWeaponComponent.projectile.maxSpeed * cosDeg(shooterAngle)
        dynamicComponent.speed.y = entityWeaponComponent.projectile.maxSpeed * sinDeg(shooterAngle)

        // projectile component, containing all the
        val projectileComponent = ProjectileComponent(
            entityWeaponComponent.impact.copy(),
            entityWeaponComponent.projectile.copy()
        )

        val spriteComponent = SpriteComponent(entityWeaponComponent.projectileSprite)


        instance.addComponent(projectile, transformComponent)
        instance.addComponent(projectile, dynamicComponent)
        instance.addComponent(projectile, projectileComponent)
        instance.addComponent(projectile, spriteComponent)

        return entityWeaponComponent.coolDown
    }

    override fun onEvent(event: Event, observable: IObservable) {
        if (event is CollisionEvent) {
            // TODO code collision behaviour
        }
    }
}
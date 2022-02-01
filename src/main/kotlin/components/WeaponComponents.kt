package components

import core.IComponent

/**
 * Contains all the components related to the weapons and the related projectiles.
 */

/**
 * Impact information of the projectile, such as KnockBack, KnockBack time, stun time and damage.
 */
data class ImpactInfo(val stunDuration : Float,
                      val knockBackDuration : Float,
                      val knockBackSpeed : Float,
                      val damage : Float)


data class ProjectileInfo(val maxSpeed : Float, val maxDamage : Float, val maxBounces : Int)

data class ProjectileComponent(val info : ProjectileInfo, val impactInfo: ImpactInfo) : IComponent

data class WeaponComponent(val impact: ImpactInfo, val coolDown: Float) : IComponent
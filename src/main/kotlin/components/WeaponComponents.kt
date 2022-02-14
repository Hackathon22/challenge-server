package components

import core.IComponent

/**
 * Contains all the components related to the weapons and the related projectiles.
 */

/**
 * Impact information of the projectile, such as KnockBack, KnockBack time, stun time and damage.
 */
data class ImpactInfo(
    val stunDuration: Float,
    val knockBackDuration: Float,
    val knockBackSpeed: Float,
    val damage: Float
)

/**
 * Information of the explosion, such as the radius and how is the damage modified.
 */
data class ExplosionInfo(val explosionRadius: Float)

data class ProjectileInfo(
    val maxSpeed: Float,
    val maxBounces: Int,
    val maxTime: Float
)

data class ProjectileComponent(var remainingBounces: Int, var remainingTime: Float, val impact: ImpactInfo, val info: ProjectileInfo) : IComponent

/**
 * Component attached to the carrier of the weapon, generally a character
 */
data class WeaponComponent(
    val impact: ImpactInfo,
    val projectile: ProjectileInfo,
    val coolDown: Float,
    val projectileSprite: String
) : IComponent
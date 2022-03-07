package systems

import components.ScoreComponent
import components.TransformComponent
import components.ZoneComponent
import core.Entity
import core.Instance
import core.System
import java.util.*

class ScoreSystem : System() {

    private var _gameTime: Float = 0.0f

    private val _zoneEntities = LinkedList<Entity>()

    private val _playerEntities = LinkedList<Entity>()

    private val _addedEntities = LinkedList<Entity>()

    private val _removedEntities = LinkedList<Entity>()

    private var _endOfGame = false

    override fun initializeLogic(vararg arg: Any): Boolean {
        if (arg.isEmpty()) return false
        return try {
            _gameTime = arg[0] as Float
            true
        } catch (exc: TypeCastException) {
            false
        }
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        _removedEntities.forEach {
            _zoneEntities.remove(it)
            _playerEntities.remove(it)
        }
        _removedEntities.clear()
        _addedEntities.forEach {
            val zoneComponent = instance.getComponentDynamicUnsafe(it, ZoneComponent::class)
            val scoreComponent = instance.getComponentDynamicUnsafe(it, ScoreComponent::class)
            if (zoneComponent != null)
                _zoneEntities.add(it)
            else if (scoreComponent != null)
                _playerEntities.add(it)
        }
        _addedEntities.clear()

        // check wherever there are players inside the zone
        // iterate over each zone
        _zoneEntities.forEach { zn ->
            val insidePlayers = LinkedList<Entity>()
            val zoneComponent = instance.getComponent<ZoneComponent>(zn)
            val zoneTransformComponent = instance.getComponent<TransformComponent>(zn)

            // iterates over each player and check their position
            _playerEntities.forEach { pl ->
                val playerTransformComponent = instance.getComponent<TransformComponent>(pl)
                if (playerTransformComponent.pos.x > zoneTransformComponent.pos.x - zoneComponent.width / 2f &&
                    playerTransformComponent.pos.x < zoneTransformComponent.pos.x + zoneComponent.width / 2f &&
                    playerTransformComponent.pos.y > zoneTransformComponent.pos.y - zoneComponent.height / 2f &&
                    playerTransformComponent.pos.y < zoneTransformComponent.pos.y + zoneComponent.height / 2f
                )
                    insidePlayers.add(pl)
            }

            // check how many players are in the list
            if (insidePlayers.size == 1) {
                val playerScoreComponent = instance.getComponent<ScoreComponent>(insidePlayers[0])
                playerScoreComponent.score += delta
            }
        }

        _gameTime -= delta
        if (_gameTime <= 0f) {
            if (!_endOfGame) {
                println("End of the game") // TODO: Compute score and give winner
                _playerEntities.forEach {
                    val scoreComponent = instance.getComponent<ScoreComponent>(it)
                    scoreComponent.gameOn = false
                    println("Player ${scoreComponent.username} - Score: ${scoreComponent.score}")
                }
            }
            _endOfGame = true
        }
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
package systems

import components.*
import core.Entity
import core.Instance
import core.System
import game.EntityRegistry
import java.util.*

data class GameResult(val team: Int, val username: String, val score: Float, val won: Boolean)

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

    fun gameOver(): Boolean {
        return _endOfGame
    }

    fun results(instance: Instance): List<GameResult> {
        // checks which team won first
        var winningTeam = 0
        var winningScore = 0f
        entities.forEach {
            val scoreComponent = instance.getComponent<ScoreComponent>(it)
            if (scoreComponent.score > winningScore) {
                winningScore = scoreComponent.score
                winningTeam = scoreComponent.team
            }
        }
        // sends the score for each team
        val results = ArrayList<GameResult>()
        entities.forEach {
            val scoreComponent = instance.getComponent<ScoreComponent>(it)
            results.add(
                GameResult(
                    scoreComponent.team,
                    scoreComponent.username,
                    scoreComponent.score,
                    winningTeam == scoreComponent.team
                )
            )
        }
        return results
    }

}

class SpawnerSystem : System() {
    override fun initializeLogic(vararg arg: Any): Boolean {
        return true
    }

    override fun updateLogic(instance: Instance, delta: Float) {
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }

    fun respawn(instance: Instance, entity: Entity) {
        val scoreComponent = instance.getComponent<ScoreComponent>(entity)
        val transformComponent = instance.getComponent<TransformComponent>(entity)
        entities.forEach {
            val spawnerComponent = instance.getComponent<SpawnerComponent>(it)
            if (spawnerComponent.team == scoreComponent.team) {
                val characterComponent = instance.getComponent<CharacterComponent>(entity)
                val spawnerTransformComponent = instance.getComponent<TransformComponent>(it)
                transformComponent.pos.set(spawnerTransformComponent.pos)
                characterComponent.health = characterComponent.maxHealth // heals up to 100%
                return@forEach
            }
        }
    }

    fun spawn(
        instance: Instance,
        entity: Entity,
        username: String,
        team: Int,
        controllerType: ControllerType,
        windowless: Boolean = false,
    ): Entity {
        if (windowless) {
            val playerComponents = EntityRegistry.loadEntity("basePlayerWindowless")
            playerComponents.forEach {
                instance.addComponentDynamic(entity, it)
            }
        }
        else {
            val characterComponents = EntityRegistry.loadEntity("baseCharacter")
            val controllerComponent = CommandComponent(controllerType = controllerType)
            characterComponents.forEach {
                instance.addComponentDynamic(entity, it)
            }
            instance.addComponentDynamic(entity, controllerComponent)
        }

        val scoreComponent = instance.getComponent<ScoreComponent>(entity)
        scoreComponent.team = team
        scoreComponent.username = username

        entities.forEach {
            val spawnerComponent = instance.getComponent<SpawnerComponent>(it)
            if (spawnerComponent.team == scoreComponent.team) {
                val spawnerTransformComponent = instance.getComponent<TransformComponent>(it)
                val transformComponent = instance.getComponent<TransformComponent>(entity)
                transformComponent.pos.set(spawnerTransformComponent.pos)
                return@forEach
            }
        }
        return entity
    }
}

package game

import components.*
import core.Instance
import core.Signature
import core.System
import org.lwjgl.Sys
import systems.*
import kotlin.system.exitProcess

open class WindowlessReplayClient(
    private val gameFile: String
) {

    private val _instance = Instance()

    private val _physicsSystem : System = _instance.registerSystem<PhysicsSystem>()

    private val _stateSystem: System = _instance.registerSystem<StateSystem>()

    private val _weaponSystem: System = _instance.registerSystem<WeaponSystem>()

    private val _projectileSystem: System = _instance.registerSystem<ProjectileSystem>()

    private val _scoreSystem: System = _instance.registerSystem<ScoreSystem>()

    private val _timerSystem: System = _instance.registerSystem<TimerSystem>()

    private val _spawnerSystem: System = _instance.registerSystem<SpawnerSystem>()

    private val _replaySystem: System = _instance.registerSystem<ReplaySystem>()

    private val _aiTime : Float

    private val _gameTime : Float

    private val _actionsPerSeconds : Float

    init {
        // toggles-off the windowed flag
        WINDOW_MODE = false

        // registers components
        _instance.registerComponent<TransformComponent>()
        _instance.registerComponent<DynamicComponent>()
        _instance.registerComponent<CommandComponent>()
        _instance.registerComponent<StateComponent>()
        _instance.registerComponent<CharacterComponent>()
        _instance.registerComponent<WeaponComponent>()
        _instance.registerComponent<ProjectileComponent>()
        _instance.registerComponent<BodyComponent>()
        _instance.registerComponent<ZoneComponent>()
        _instance.registerComponent<ScoreComponent>()
        _instance.registerComponent<TimerComponent>()
        _instance.registerComponent<SpawnerComponent>()

        _replaySystem.initialize(gameFile)
        val replaySignature = Signature()
        _instance.setSystemSignature<ReplaySystem>(replaySignature)

        _aiTime = (_replaySystem as ReplaySystem).aiTime()
        _gameTime = _replaySystem.gameTime()
        _actionsPerSeconds = _replaySystem.commandsPerSeconds()

        _physicsSystem.initialize(_instance)
        val physicsSignature = Signature()
        physicsSignature.set(_instance.getComponentType<TransformComponent>(), true)
        physicsSignature.set(_instance.getComponentType<BodyComponent>(), true)
        _instance.setSystemSignature<PhysicsSystem>(physicsSignature)

        _stateSystem.initialize()
        val stateSignature = Signature()
        stateSignature.set(_instance.getComponentType<CommandComponent>(), true)
        stateSignature.set(_instance.getComponentType<StateComponent>(), true)
        stateSignature.set(_instance.getComponentType<TransformComponent>(), true)
        stateSignature.set(_instance.getComponentType<DynamicComponent>(), true)
        _instance.setSystemSignature<StateSystem>(stateSignature)

        _weaponSystem.initialize()
        val weaponSignature = Signature()
        weaponSignature.set(_instance.getComponentType<ProjectileComponent>(), true)
        _instance.setSystemSignature<WeaponSystem>(weaponSignature)

        _projectileSystem.initialize()
        val projectileSignature = Signature()
        projectileSignature.set(_instance.getComponentType<ProjectileComponent>(), true)
        projectileSignature.set(_instance.getComponentType<TransformComponent>(), true)
        projectileSignature.set(_instance.getComponentType<DynamicComponent>(), true)
        _instance.setSystemSignature<ProjectileSystem>(projectileSignature)

        _scoreSystem.initialize(_gameTime)
        val scoreSignature = Signature() // accepts all kind of entities
        _instance.setSystemSignature<ScoreSystem>(scoreSignature)

        _timerSystem.initialize()
        val timerSignature = Signature()
        timerSignature.set(_instance.getComponentType<TimerComponent>(), true)
        _instance.setSystemSignature<TimerSystem>(timerSignature)

        _spawnerSystem.initialize()
        val spawnerSignature = Signature()
        spawnerSignature.set(_instance.getComponentType<TransformComponent>(), true)
        spawnerSignature.set(_instance.getComponentType<SpawnerComponent>(), true)
        _instance.setSystemSignature<SpawnerSystem>(spawnerSignature)

        // sets observers on non-graphical systems
        _physicsSystem.addObserver(_projectileSystem)  // bullet collision
        _physicsSystem.addObserver(_stateSystem)  // state change
        _projectileSystem.addObserver(_stateSystem)

        println("Components and Systems are initialized.")
    }

    fun create() {
        val scene = SceneRegistry.loadScene("baseSceneWindowless")
        scene.forEach { (_, components) ->
            val entity = _instance.createEntity()
            components.forEach {
                _instance.addComponentDynamic(entity, it)
            }
        }

        (_replaySystem as ReplaySystem).agents().forEach {
            addAgent(it)
        }
    }

    fun play() {
        val deltaTime = 1.0f / 60.0f
        var tickCounter = 0
        val aiModulo = (60 / _actionsPerSeconds).toInt()
        while (!(_scoreSystem as ScoreSystem).gameOver()) {
            try {
                if (tickCounter % aiModulo == 0)
                    _replaySystem.update(_instance, deltaTime)
                _timerSystem.update(_instance, deltaTime)
                _stateSystem.update(_instance, deltaTime)
                _physicsSystem.update(_instance, deltaTime)
                _projectileSystem.update(_instance, deltaTime)
//                _scoreSystem.update(_instance, deltaTime)
                tickCounter += 1
            } catch (exc: Exception) {
                println("Internal server exception.")
                exc.printStackTrace()
                (_scoreSystem as ScoreSystem).forceFinishGame(_instance, -1)
            }
        }
    }

    private fun addAgent(agentData: AgentData) {
        if (agentData.valid) {
            (_spawnerSystem as SpawnerSystem).spawn(
                _instance,
                agentData.entity,
                agentData.username,
                agentData.team,
                ControllerType.AI,
                true
            )
        } else {
            throw Exception("Error establishing a python agent.")
        }
    }

}

package game

import components.*
import core.Instance
import core.Signature
import core.System
import systems.*

open class WindowlessClient(
    private val gameFile: String,
    private val gameTime: Float = 60f,
    private val aiTime: Float = 60f,
    private val actionsPerSecond: Float = 4f,
    private val port: Int = 2049
) {

    private val _instance = Instance()

    private val _movementSystem: System = _instance.registerSystem<MovementSystem>()

    private val _stateSystem: System = _instance.registerSystem<StateSystem>()

    private val _weaponSystem: System = _instance.registerSystem<WeaponSystem>()

    private val _projectileSystem: System = _instance.registerSystem<ProjectileSystem>()

    private val _collisionSystem: System = _instance.registerSystem<CollisionSystem>()

    private val _scoreSystem: System = _instance.registerSystem<ScoreSystem>()

    private val _timerSystem: System = _instance.registerSystem<TimerSystem>()

    private val _spawnerSystem: System = _instance.registerSystem<SpawnerSystem>()

    private val _aiSystem: System = _instance.registerSystem<PythonAISystem>()

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

        // initializes non-graphical systems
        _movementSystem.initialize()
        val movementSignature = Signature()
        movementSignature.set(_instance.getComponentType<TransformComponent>(), true)
        movementSignature.set(_instance.getComponentType<DynamicComponent>(), true)
        _instance.setSystemSignature<MovementSystem>(movementSignature)

        _collisionSystem.initialize(_instance)
        val collisionSignature = Signature()
        collisionSignature.set(_instance.getComponentType<TransformComponent>(), true)
        collisionSignature.set(_instance.getComponentType<BodyComponent>(), true)
        _instance.setSystemSignature<CollisionSystem>(collisionSignature)

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

        _scoreSystem.initialize(gameTime)
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

        _aiSystem.initialize(aiTime, gameTime, actionsPerSecond, gameFile, port)
        val aiSignature = Signature()
        aiSignature.set(_instance.getComponentType<CommandComponent>(), true)
        _instance.setSystemSignature<PythonAISystem>(aiSignature)

        // sets observers on non-graphical systems
        _collisionSystem.addObserver(_projectileSystem)  // bullet collision
        _collisionSystem.addObserver(_stateSystem)  // state change
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

        // waits for the two python agents to connect
        for (i in 0..1) addAgent()
    }

    fun play() {
        val deltaTime = 1.0f / actionsPerSecond
        while (!(_scoreSystem as ScoreSystem).gameOver()) {
            _aiSystem.update(_instance, deltaTime)
            _timerSystem.update(_instance, deltaTime)
            _stateSystem.update(_instance, deltaTime)
            _movementSystem.update(_instance, deltaTime)
            _collisionSystem.update(_instance, deltaTime)
            _projectileSystem.update(_instance, deltaTime)
            _scoreSystem.update(_instance, deltaTime)
        }
        val gameResult = _scoreSystem.results(_instance)
        (_aiSystem as PythonAISystem).saveToFile()
        if (!_aiSystem.aborted()) {
            (_aiSystem).finish(gameResult)
            println("Game finished, game results are: $gameResult")
        }
        else {
            println("Game aborted, game results are: $gameResult")
        }
    }

    private fun addAgent() {
        val agentData = (_aiSystem as PythonAISystem).addAgent(_instance)
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
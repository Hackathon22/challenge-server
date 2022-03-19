package game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import components.*
import core.*
import render.SpriteRegister
import systems.*
import java.util.concurrent.atomic.AtomicBoolean

const val BASE_WIDTH = 1200
const val BASE_HEIGHT = 800

open class DesktopClient(private val gameFile: String? = null, private val gameTime: Float = 60f) :
    ApplicationAdapter(), IObservable {

    private val _running = AtomicBoolean(false)

    private val _instance = Instance()

    private val _movementSystem: System = _instance.registerSystem<MovementSystem>()

    private val _cameraSystem: System = _instance.registerSystem<CameraSystem>()

    private val _spriteSystem: System = _instance.registerSystem<SpriteRenderSystem>()

    private val _inputSystem: System = _instance.registerSystem<InputSystem>()

    private val _stateSystem: System = _instance.registerSystem<StateSystem>()

    private val _weaponSystem: System = _instance.registerSystem<WeaponSystem>()

    private val _projectileSystem: System = _instance.registerSystem<ProjectileSystem>()

    private val _collisionSystem: System = _instance.registerSystem<CollisionSystem>()

    private val _scoreSystem: System = _instance.registerSystem<ScoreSystem>()

    private val _uiSystem: System = _instance.registerSystem<UISystem>()

    private val _timerSystem: System = _instance.registerSystem<TimerSystem>()

    private val _spawnerSystem: System = _instance.registerSystem<SpawnerSystem>()

    override val observers = ArrayList<IObserver>()

    init {
        // registers components
        _instance.registerComponent<NetworkComponent>()
        _instance.registerComponent<TransformComponent>()
        _instance.registerComponent<DynamicComponent>()
        _instance.registerComponent<CameraComponent>()
        _instance.registerComponent<SpriteComponent>()
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

        // sets observers on non-graphical systems
        _collisionSystem.addObserver(_projectileSystem)  // bullet collision
        _collisionSystem.addObserver(_stateSystem)  // state change
        _projectileSystem.addObserver(_stateSystem)
    }

    override fun create() {
        // Loads all the game sprites
        SpriteRegister.initialize()

        // initializes input systems
        _inputSystem.initialize()
        val inputSignature = Signature()
        inputSignature.set(_instance.getComponentType<CommandComponent>(), true)
        _instance.setSystemSignature<InputSystem>(inputSignature)

        // initializes graphical systems
        _cameraSystem.initialize(BASE_WIDTH, BASE_HEIGHT)
        val cameraSignature = Signature()
        cameraSignature.set(_instance.getComponentType<CameraComponent>(), true)
        cameraSignature.set(_instance.getComponentType<TransformComponent>(), true)
        _instance.setSystemSignature<CameraSystem>(cameraSignature)

        _spriteSystem.initialize()
        val spriteSignature = Signature()
        spriteSignature.set(_instance.getComponentType<TransformComponent>(), true)
        spriteSignature.set(_instance.getComponentType<SpriteComponent>(), true)
        _instance.setSystemSignature<SpriteRenderSystem>(spriteSignature)

        _uiSystem.initialize()
        val uiSignature = Signature()
        uiSignature.set(_instance.getComponentType<ScoreComponent>(), true)
        _instance.setSystemSignature<UISystem>(uiSignature)

        // set observers on graphical systems
        this.addObserver(_cameraSystem)

        // loads the scene
        val scene = SceneRegistry.loadScene("baseScene")
        scene.forEach { (_, components) ->
            val entity = _instance.createEntity()
            components.forEach {
                _instance.addComponentDynamic(entity, it)
            }
        }

        // loads two players
        (_spawnerSystem as SpawnerSystem).spawn(
            _instance,
            "player_1",
            0,
            ControllerType.LOCAL_INPUT
        )
        (_spawnerSystem as SpawnerSystem).spawn(_instance, "player_2", 1, ControllerType.AI)

        _running.set(true)
    }

    override fun dispose() {
        _running.set(false)
    }

    override fun resize(width: Int, height: Int) {
        notifyObservers(WindowResizeEvent(Vec2F(width.toFloat(), height.toFloat())), _instance)
    }

    override fun render() {
        val deltaTime = Gdx.graphics.deltaTime
        // get inputs
        _inputSystem.update(_instance, deltaTime)

        // simulate timer
        _timerSystem.update(_instance, deltaTime)

        // simulate player state
        _stateSystem.update(_instance, deltaTime)

        // physic systems
        _movementSystem.update(_instance, deltaTime)
        _collisionSystem.update(_instance, deltaTime)

        _projectileSystem.update(_instance, deltaTime)

        // rule systems
        _scoreSystem.update(_instance, deltaTime)

        // render systems
        _cameraSystem.update(_instance, deltaTime)
        _spriteSystem.update(_instance, deltaTime)
        _uiSystem.update(_instance, deltaTime)
    }
}


open class WindowlessClient(
    private val gameFile: String,
    private val gameTime: Float = 60f,
    private val aiTime: Float = 60f,
    private val actionsPerSecond : Float = 4f
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

    private val _aiSystem : System = _instance.registerSystem<PythonAISystem>()

    init {
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

        println("Components initialized.")


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

        _aiSystem.initialize(aiTime, gameFile)
        val aiSignature = Signature()
        aiSignature.set(_instance.getComponentType<CommandComponent>(), true)
        _instance.setSystemSignature<PythonAISystem>(aiSignature)

        // sets observers on non-graphical systems
        _collisionSystem.addObserver(_projectileSystem)  // bullet collision
        _collisionSystem.addObserver(_stateSystem)  // state change
        _projectileSystem.addObserver(_stateSystem)

        println("Systems initialized.")
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
        (_aiSystem as PythonAISystem).finish(gameResult)
    }

    private fun addAgent() {
        val agentData = (_aiSystem as PythonAISystem).addAgent(_instance)
        if (agentData.valid) {
            (_spawnerSystem as SpawnerSystem).spawn(_instance, agentData.username, agentData.team, ControllerType.AI)
        }
        else {
            throw Exception("Error establishing a python agent.")
        }
    }

}
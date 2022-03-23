package game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import components.*
import core.*
import render.SpriteRegister
import systems.*
import java.util.concurrent.atomic.AtomicBoolean

open class ReplayClient(private val gameFile: String) :
    ApplicationAdapter(), IObservable {

    private val _running = AtomicBoolean(false)

    private val _instance = Instance()

    private val _movementSystem: System = _instance.registerSystem<MovementSystem>()

    private val _cameraSystem: System = _instance.registerSystem<CameraSystem>()

    private val _spriteSystem: System = _instance.registerSystem<SpriteRenderSystem>()

    private val _stateSystem: System = _instance.registerSystem<StateSystem>()

    private val _weaponSystem: System = _instance.registerSystem<WeaponSystem>()

    private val _projectileSystem: System = _instance.registerSystem<ProjectileSystem>()

    private val _collisionSystem: System = _instance.registerSystem<CollisionSystem>()

    private val _scoreSystem: System = _instance.registerSystem<ScoreSystem>()

    private val _uiSystem: System = _instance.registerSystem<UISystem>()

    private val _timerSystem: System = _instance.registerSystem<TimerSystem>()

    private val _spawnerSystem: System = _instance.registerSystem<SpawnerSystem>()

    private val _replaySystem: System = _instance.registerSystem<ReplaySystem>()

    private var _gameTime = 60f

    private var _aiTime = 150f

    private var _commandsPerSeconds = 4f

    private var _agentData = ArrayList<AgentData>()

    private var _tickCounter = 0

    private var _aiModulo = (60 / _commandsPerSeconds).toInt()

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

        _replaySystem.initialize(gameFile)
        val replaySignature = Signature()
        _instance.setSystemSignature<ReplaySystem>(replaySignature)

        _gameTime = (_replaySystem as ReplaySystem).gameTime()
        _aiTime = _replaySystem.aiTime()
        _commandsPerSeconds = _replaySystem.commandsPerSeconds()

        _replaySystem.agents().forEach {
            _agentData.add(it)
        }

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
        _collisionSystem.addObserver(_projectileSystem)  // bullet collision
        _collisionSystem.addObserver(_stateSystem)  // state change
        _projectileSystem.addObserver(_stateSystem)
    }

    override fun create() {
        // Loads all the game sprites
        SpriteRegister.initialize()

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

        // loads new agents
        _agentData.forEach { agent ->
            val entity = _instance.createEntity()
            assert(entity == agent.entity) // gros schlague
            (_spawnerSystem as SpawnerSystem).spawn(
                _instance,
                entity,
                agent.username,
                agent.team,
                ControllerType.AI
            )
        }

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
        if (_tickCounter % _aiModulo == 0)
            _replaySystem.update(_instance, deltaTime)

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

        _tickCounter += 1
    }
}

package game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import components.*
import core.*
import render.SpriteRegister
import systems.*
import java.awt.color.ICC_ProfileGray
import java.util.concurrent.atomic.AtomicBoolean

const val BASE_WIDTH = 1200
const val BASE_HEIGHT = 800


var WINDOW_MODE = true

open class DesktopClient(private val gameFile: String? = null, private val gameTime: Float = 60f) :
    ApplicationAdapter(), IObservable {

    private val _running = AtomicBoolean(false)

    private val _instance = Instance()

    private val _cameraSystem: System = _instance.registerSystem<CameraSystem>()

    private val _spriteSystem: System = _instance.registerSystem<SpriteRenderSystem>()

    private val _inputSystem: System = _instance.registerSystem<InputSystem>()

    private val _stateSystem: System = _instance.registerSystem<StateSystem>()

    private val _weaponSystem: System = _instance.registerSystem<WeaponSystem>()

    private val _projectileSystem: System = _instance.registerSystem<ProjectileSystem>()

    private val _scoreSystem: System = _instance.registerSystem<ScoreSystem>()

    private val _uiSystem: System = _instance.registerSystem<UISystem>()

    private val _timerSystem: System = _instance.registerSystem<TimerSystem>()

    private val _spawnerSystem: System = _instance.registerSystem<SpawnerSystem>()

    private val _physicsSystem : System = _instance.registerSystem<PhysicsSystem>()

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

        _physicsSystem.initialize(_instance)
        val physicsSignature = Signature()
        physicsSignature.set(_instance.getComponentType<TransformComponent>(), true)
        physicsSignature.set(_instance.getComponentType<BodyComponent>(), true)
        _instance.setSystemSignature<PhysicsSystem>(physicsSignature)

        // sets observers on non-graphical systems
//        _collisionSystem.addObserver(_projectileSystem)  // bullet collision
//        _collisionSystem.addObserver(_stateSystem)  // state change
        _physicsSystem.addObserver(_projectileSystem)
        _physicsSystem.addObserver(_stateSystem)
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
        val player0Entity = _instance.createEntity()
        (_spawnerSystem as SpawnerSystem).spawn(
            _instance,
            player0Entity,
            "player_1",
            0,
            ControllerType.LOCAL_INPUT
        )
        val player1Entity = _instance.createEntity()
        (_spawnerSystem as SpawnerSystem).spawn(
            _instance,
            player1Entity,
            "player_2",
            1,
            ControllerType.AI
        )

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

        // _movementSystem.update(_instance, deltaTime)
        // _collisionSystem.update(_instance, deltaTime)
        _physicsSystem.update(_instance, deltaTime)

        _projectileSystem.update(_instance, deltaTime)

        // rule systems
        _scoreSystem.update(_instance, deltaTime)

        // render systems
        _cameraSystem.update(_instance, deltaTime)
        _spriteSystem.update(_instance, deltaTime)
        _uiSystem.update(_instance, deltaTime)
    }
}

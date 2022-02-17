package game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryonet.Client
import components.*
import core.*
import net.ClientStatus
import render.SpriteRegister
import systems.*
import java.util.concurrent.atomic.AtomicBoolean

const val DEFAULT_TIMEOUT = 10000

const val BASE_WIDTH = 1200
const val BASE_HEIGHT = 800

open class ClientSession(private val sceneName : String? = "baseScene") : ApplicationAdapter(), IObservable {

    private val _client = Client()

    private var _status = ClientStatus.DISCONNECTED
    private var _initializedClass = false
    private var _username : String? = null

    private val _running = AtomicBoolean(false)

    private val _instance = Instance()

    private val _movementSystem : System = _instance.registerSystem<MovementSystem>()

    private val _cameraSystem : System = _instance.registerSystem<CameraSystem>()

    private val _spriteSystem : System = _instance.registerSystem<SpriteRenderSystem>()

    private val _inputSystem : System = _instance.registerSystem<InputSystem>()

    private val _stateSystem : System = _instance.registerSystem<StateSystem>()

    private val _weaponSystem : System = _instance.registerSystem<WeaponSystem>()

    private val _projectileSystem : System = _instance.registerSystem<ProjectileSystem>()

    private val _collisionSystem : System = _instance.registerSystem<CollisionSystem>()

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

        // sets observers on non-graphical systems
        _collisionSystem.addObserver(_projectileSystem)  // bullet collision
        _collisionSystem.addObserver(_stateSystem)  // state change
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

        // set observers on graphical systems
        this.addObserver(_cameraSystem)

        // loads the scene
        if (sceneName != null) {
            val scene = SceneRegistry.loadScene(sceneName)
            scene.forEach { (_, components) ->
                val entity = _instance.createEntity()
                components.forEach {
                    _instance.addComponentDynamic(entity, it)
                }
            }
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
        _inputSystem.update(_instance, deltaTime)

        // simulate player state
        _stateSystem.update(_instance, deltaTime)

        // physic systems
        _movementSystem.update(_instance, deltaTime)
        _collisionSystem.update(_instance, deltaTime)

        _projectileSystem.update(_instance, deltaTime)

        // render systems
        _cameraSystem.update(_instance, deltaTime)
        _spriteSystem.update(_instance, deltaTime)
    }
}

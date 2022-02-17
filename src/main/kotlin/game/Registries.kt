package game

import components.*
import core.IComponent
import core.Instance
import parser.Scene
import systems.CollisionSystem
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


object EntityRegistry {
    private val entityMap: HashMap<String, () -> MutableList<IComponent>> = hashMapOf(
        "baseCharacter" to ::baseCharacter,
        "basePlayer" to ::basePlayer,
        "baseCamera" to ::baseCamera,
        "baseRocketLauncher" to ::baseRocketLauncher
    )

    fun loadEntity(name: String): MutableList<IComponent> {
        assert(entityMap[name] != null)
        return entityMap[name]!!()
    }

    private fun baseCharacter(): MutableList<IComponent> {
        val simpleTransformComponent = TransformComponent()
        val simpleSpriteComponent = SpriteComponent()
        simpleSpriteComponent.sprite = "soldier"
        val simpleStateComponent = StateComponent()
        val simpleDynamicComponent = DynamicComponent()
        val simpleCharacterComponent = CharacterComponent(100.0f, 200.0f)
        val simpleBodyComponent = BodyComponent(32.0f, 32.0f)
        return arrayListOf(
            simpleTransformComponent,
            simpleSpriteComponent,
            simpleStateComponent,
            simpleDynamicComponent,
            simpleCharacterComponent,
            simpleBodyComponent
        )
    }

    private fun basePlayer(): MutableList<IComponent> {
        val components = loadEntity("baseCharacter")
        val simpleCommandComponent = CommandComponent(ControllerType.LOCAL_INPUT, LinkedList())
        components.add(simpleCommandComponent)
        return components
    }

    private fun baseCamera(): MutableList<IComponent> {
        val cameraCameraComponent = CameraComponent()
        val cameraTransformComponent = TransformComponent()
        return arrayListOf(cameraCameraComponent, cameraTransformComponent)
    }

    private fun baseRocketLauncher(): MutableList<IComponent> {
        val impactInfo = ImpactInfo(
            stunDuration = 1.0f,
            knockBackDuration = 0.3f,
            knockBackSpeed = 300.0f,
            damage = 10.0f
        )
        val projectileInfo = ProjectileInfo(
            width = 32.0f,
            height = 32.0f,
            maxSpeed = 400.0f,
            maxBounces = 2,
            maxTime = 5.0f  // maximum two seconds
        )
        return arrayListOf(WeaponComponent(impactInfo, projectileInfo, 0.5f, "rocket"))
    }
}

/**
 * This file contains hardcoded scenes and entities. XML serialization really sucks, and I have a
 * tight deadline so here comes hardcoding!!
 */
object SceneRegistry {

    private val sceneMap: HashMap<String, () -> Scene> = hashMapOf(
        "baseScene" to ::baseScene
    )

    private val _instance = Instance()

    init {
        _instance.registerComponent<TransformComponent>()
        _instance.registerComponent<DynamicComponent>()
        _instance.registerComponent<CharacterComponent>()
        _instance.registerComponent<CameraComponent>()
        _instance.registerComponent<NetworkComponent>()
        _instance.registerComponent<CommandComponent>()
        _instance.registerComponent<StateComponent>()
        _instance.registerComponent<SpriteComponent>()
        _instance.registerComponent<ProjectileComponent>()
        _instance.registerComponent<WeaponComponent>()
        _instance.registerComponent<BodyComponent>()
    }

    fun loadScene(name: String): Scene {
        assert(sceneMap[name] != null)
        return sceneMap[name]!!()
    }

    private fun baseScene(): Scene {

        val scene = HashMap<Int, ArrayList<IComponent>>()

        val cameraEntity = _instance.createEntity()
        EntityRegistry.loadEntity("baseCamera").forEach {
            _instance.addComponentDynamic(cameraEntity, it)
        }
        scene[cameraEntity] = _instance.getAllComponents(cameraEntity)

        val simpleEntity = _instance.createEntity()
        EntityRegistry.loadEntity("basePlayer").forEach {
            _instance.addComponentDynamic(simpleEntity, it)
        }
        EntityRegistry.loadEntity("baseRocketLauncher").forEach {
            _instance.addComponentDynamic(simpleEntity, it)
        }
        _instance.getComponent<TransformComponent>(simpleEntity).pos.x = -100.0f
        _instance.getComponent<TransformComponent>(simpleEntity).pos.y = 0f
        _instance.getComponent<TransformComponent>(simpleEntity).rot.z = 45.0f

        scene[simpleEntity] = _instance.getAllComponents(simpleEntity)

        val simpleEntity2 = _instance.createEntity()
        EntityRegistry.loadEntity("baseCharacter").forEach {
            _instance.addComponentDynamic(simpleEntity2, it)
        }
        EntityRegistry.loadEntity("baseRocketLauncher").forEach {
            _instance.addComponentDynamic(simpleEntity2, it)
        }

        scene[simpleEntity2] = _instance.getAllComponents(simpleEntity2)

        return scene
    }


}
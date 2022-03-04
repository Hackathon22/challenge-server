package game

import components.*
import core.Entity
import core.IComponent
import core.Instance
import core.Vec3F
import parser.Scene
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


object EntityRegistry {
    private val entityMap: HashMap<String, () -> MutableList<IComponent>> = hashMapOf(
        "baseCharacter" to ::baseCharacter,
        "basePlayer" to ::basePlayer,
        "baseCamera" to ::baseCamera,
        "baseRocketLauncher" to ::baseRocketLauncher,
        "baseWall" to ::baseWall,
        "baseZone" to ::baseZone
    )

    fun loadEntity(name: String): MutableList<IComponent> {
        assert(entityMap[name] != null)
        return entityMap[name]!!()
    }

    private fun baseZone() : MutableList<IComponent> {
        val simpleTransformComponent = TransformComponent()
        val simpleZoneComponent = ZoneComponent(200f, 200f)
        val simpleSpriteComponent = SpriteComponent("zone", repeat = false)
        return arrayListOf(
            simpleTransformComponent,
            simpleZoneComponent,
            simpleSpriteComponent
        )
    }

    private fun baseWall() : MutableList<IComponent> {
        val simpleTransformComponent = TransformComponent()
        val simpleSpriteComponent = SpriteComponent("bricks", repeat = true)
        val simpleBodyComponent = BodyComponent(32f, 32f)
        return arrayListOf(
            simpleTransformComponent,
            simpleSpriteComponent,
            simpleBodyComponent
        )
    }

    private fun baseCharacter(): MutableList<IComponent> {
        val simpleTransformComponent = TransformComponent()
        val simpleSpriteComponent = SpriteComponent()
        simpleSpriteComponent.sprite = "soldier"
        val simpleStateComponent = StateComponent()
        val simpleDynamicComponent = DynamicComponent()
        val simpleCharacterComponent = CharacterComponent(100.0f, 200.0f)
        val simpleBodyComponent = BodyComponent(32.0f, 32.0f)
        val simpleScoreComponent = ScoreComponent()
        return arrayListOf(
            simpleTransformComponent,
            simpleSpriteComponent,
            simpleStateComponent,
            simpleDynamicComponent,
            simpleCharacterComponent,
            simpleBodyComponent,
            simpleScoreComponent
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
            height = 16.0f,
            maxSpeed = 400.0f,
            maxBounces = 2,
            maxTime = 2.0f  // maximum two seconds
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
        _instance.registerComponent<ZoneComponent>()
        _instance.registerComponent<ScoreComponent>()
    }

    fun loadScene(name: String): Scene {
        assert(sceneMap[name] != null)
        return sceneMap[name]!!()
    }

    private fun addWall(x: Float, y: Float, width: Float, height: Float) : Entity {
        val wallEntity = _instance.createEntity()
        EntityRegistry.loadEntity("baseWall").forEach {
            _instance.addComponentDynamic(wallEntity, it)
        }
        val wallTransformComponent = _instance.getComponent<TransformComponent>(wallEntity)
        wallTransformComponent.pos.x = x
        wallTransformComponent.pos.y = y
        wallTransformComponent.scale.x = width / 32f
        wallTransformComponent.scale.y = height / 32f
        val wallBodyComponent = _instance.getComponent<BodyComponent>(wallEntity)
        wallBodyComponent.height = height
        wallBodyComponent.width = width

        return wallEntity
    }

    private fun baseScene(): Scene {

        val scene = HashMap<Int, ArrayList<IComponent>>()

        val cameraEntity = _instance.createEntity()
        EntityRegistry.loadEntity("baseCamera").forEach {
            _instance.addComponentDynamic(cameraEntity, it)
        }
        scene[cameraEntity] = _instance.getAllComponents(cameraEntity)

        val zoneEntity = _instance.createEntity()
        EntityRegistry.loadEntity("baseZone").forEach {
            _instance.addComponentDynamic(zoneEntity, it)
        }
        val zoneTransformComponent = _instance.getComponent<TransformComponent>(zoneEntity)
        zoneTransformComponent.pos.set(Vec3F(0f, 0f, 0f))
        scene[zoneEntity] = _instance.getAllComponents(zoneEntity)

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

        // scene walls
        val topWall = addWall(0f, 316f, 664f, 32f)
        scene[topWall] = _instance.getAllComponents(topWall)

        val bottomWall = addWall(0f, -316f, 664f, 32f)
        scene[bottomWall] = _instance.getAllComponents(bottomWall)

        val leftWall = addWall(-316f, 0f, 32f, 664f)
        scene[leftWall] = _instance.getAllComponents(leftWall)

        val rightWall = addWall(316f, 0f, 32f, 664f)
        scene[rightWall] = _instance.getAllComponents(rightWall)

        val leftCornerTopWall = addWall(-125f, 200f, 182f, 32f)
        scene[leftCornerTopWall] = _instance.getAllComponents(leftCornerTopWall)

        val leftCornerBottomWall = addWall(-200f, 125f, 32f, 182f)
        scene[leftCornerBottomWall] = _instance.getAllComponents(leftCornerBottomWall)

        val rightCornerTopWall = addWall(200f, -125f, 32f, 182f)
        scene[rightCornerTopWall] = _instance.getAllComponents(rightCornerTopWall)

        val rightCornerBottomWall = addWall(125f, -200f, 182f, 32f)
        scene[rightCornerBottomWall] = _instance.getAllComponents(rightCornerBottomWall)

        val bottomLeftWall = addWall(-150f, -150f, 32f, 32f)
        scene[bottomLeftWall] = _instance.getAllComponents(bottomLeftWall)

        val topRightWall = addWall(150f, 150f, 32f, 32f)
        scene[topRightWall] = _instance.getAllComponents(topRightWall)

        return scene
    }


}
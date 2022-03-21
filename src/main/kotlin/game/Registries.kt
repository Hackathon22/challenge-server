package game

import components.*
import core.Entity
import core.IComponent
import core.Instance
import core.Vec3F
import java.util.*

typealias Scene = HashMap<Entity, ArrayList<IComponent>>

object EntityRegistry {
    private val entityMap: HashMap<String, () -> MutableList<IComponent>> = hashMapOf(
        "baseCharacter" to ::baseCharacter,
        "basePlayer" to ::basePlayer,
        "basePlayerWindowless" to ::basePlayerWindowless,
        "baseCamera" to ::baseCamera,
        "baseRocketLauncher" to ::baseRocketLauncher,
        "baseWall" to ::baseWall,
        "baseZone" to ::baseZone,
        "baseExplosion" to ::baseExplosion,
        "baseSpawner" to ::baseSpawner
    )

    fun loadEntity(name: String): MutableList<IComponent> {
        assert(entityMap[name] != null)
        return entityMap[name]!!()
    }

    private fun baseZone(): MutableList<IComponent> {
        val simpleTransformComponent = TransformComponent()
        val simpleZoneComponent = ZoneComponent(200f, 200f)
        val simpleSpriteComponent = SpriteComponent("zone", repeat = false)
        return arrayListOf(
            simpleTransformComponent,
            simpleZoneComponent,
            simpleSpriteComponent
        )
    }

    private fun baseWall(): MutableList<IComponent> {
        val simpleTransformComponent = TransformComponent()
        val simpleSpriteComponent = SpriteComponent("bricks", repeat = true)
        val simpleBodyComponent = BodyComponent(32f, 32f, static = true)
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
        val simpleCharacterComponent = CharacterComponent(health = 100.0f, maxSpeed = 200.0f)
        val simpleBodyComponent = BodyComponent(32.0f, 32.0f)
        val simpleScoreComponent = ScoreComponent()
        val componentList = arrayListOf(
            simpleTransformComponent,
            simpleSpriteComponent,
            simpleStateComponent,
            simpleDynamicComponent,
            simpleCharacterComponent,
            simpleBodyComponent,
            simpleScoreComponent
        )
        componentList.addAll(baseRocketLauncher())
        return componentList
    }

    private fun basePlayer(): MutableList<IComponent> {
        val components = loadEntity("baseCharacter")
        val simpleCommandComponent = CommandComponent(ControllerType.LOCAL_INPUT, LinkedList())
        components.add(simpleCommandComponent)
        return components
    }

    private fun basePlayerWindowless() : MutableList<IComponent> {
        val simpleTransformComponent = TransformComponent()
        val simpleStateComponent = StateComponent()
        val simpleDynamicComponent = DynamicComponent()
        val simpleCharacterComponent = CharacterComponent(health = 100.0f, maxSpeed = 200.0f)
        val simpleBodyComponent = BodyComponent(32.0f, 32.0f)
        val simpleScoreComponent = ScoreComponent()
        val simpleCommandComponent = CommandComponent(ControllerType.AI)
        val componentList = arrayListOf(
            simpleTransformComponent,
            simpleStateComponent,
            simpleDynamicComponent,
            simpleCharacterComponent,
            simpleBodyComponent,
            simpleScoreComponent,
            simpleCommandComponent
        )
        componentList.addAll(baseRocketLauncher())
        return componentList
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
            damage = 40.0f,
            radius = 30.0f
        )
        val projectileInfo = ProjectileInfo(
            width = 32.0f,
            height = 16.0f,
            maxSpeed = 400.0f,
            maxBounces = 2,
            maxTime = 2.0f  // maximum two seconds
        )
        return arrayListOf(WeaponComponent(impactInfo, projectileInfo, 0.45f, "rocket"))
    }

    private fun baseExplosion(): MutableList<IComponent> {
        val transformComponent = TransformComponent()
        val spriteComponent = SpriteComponent("explosion")
        val timerComponent = TimerComponent(.5f)
        return arrayListOf(transformComponent, spriteComponent, timerComponent)
    }

    private fun baseSpawner(): MutableList<IComponent> {
        val transformComponent = TransformComponent()
        val spawnerComponent = SpawnerComponent()
        return arrayListOf(transformComponent, spawnerComponent)
    }
}

/**
 * This file contains hardcoded scenes and entities. XML serialization really sucks, and I have a
 * tight deadline so here comes hardcoding!!
 */
object SceneRegistry {

    private val sceneMap: HashMap<String, () -> Scene> = hashMapOf(
        "baseScene" to ::baseScene,
        "baseSceneWindowless" to ::baseSceneWindowless
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
        _instance.registerComponent<TimerComponent>()
        _instance.registerComponent<SpawnerComponent>()
    }

    fun loadScene(name: String): Scene {
        assert(sceneMap[name] != null)
        return sceneMap[name]!!()
    }

    private fun addWall(x: Float, y: Float, width: Float, height: Float): Entity {
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

        val firstSpawner = _instance.createEntity()
        EntityRegistry.loadEntity("baseSpawner").forEach {
            _instance.addComponentDynamic(firstSpawner, it)
        }
        val firstSpawnerTransformComponent =
            _instance.getComponent<TransformComponent>(firstSpawner)
        firstSpawnerTransformComponent.pos.x = -250f
        firstSpawnerTransformComponent.pos.y = 250f
        val firstSpawnerSpawnerComponent = _instance.getComponent<SpawnerComponent>(firstSpawner)
        firstSpawnerSpawnerComponent.team = 0
        scene[firstSpawner] = _instance.getAllComponents(firstSpawner)

        val secondSpawner = _instance.createEntity()
        EntityRegistry.loadEntity("baseSpawner").forEach {
            _instance.addComponentDynamic(secondSpawner, it)
        }
        val secondSpawnerTransformComponent =
            _instance.getComponent<TransformComponent>(secondSpawner)
        secondSpawnerTransformComponent.pos.x = 250f
        secondSpawnerTransformComponent.pos.y = -250f
        val secondSpawnerSpawnerComponent = _instance.getComponent<SpawnerComponent>(secondSpawner)
        secondSpawnerSpawnerComponent.team = 1
        scene[secondSpawner] = _instance.getAllComponents(secondSpawner)


        val zoneEntity = _instance.createEntity()
        EntityRegistry.loadEntity("baseZone").forEach {
            _instance.addComponentDynamic(zoneEntity, it)
        }
        val zoneTransformComponent = _instance.getComponent<TransformComponent>(zoneEntity)
        zoneTransformComponent.pos.set(Vec3F(0f, 0f, 0f))
        scene[zoneEntity] = _instance.getAllComponents(zoneEntity)

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

    private fun baseSceneWindowless(): Scene {
        val scene = baseScene()
        scene.forEach { (entity, components) ->
            val componentIterator = components.iterator()
            while (componentIterator.hasNext()) {
                val component = componentIterator.next()
                if (component is CameraComponent ||
                    component is SpriteComponent
                ) {
                    componentIterator.remove()
                }
            }
        }
        return scene
    }
}
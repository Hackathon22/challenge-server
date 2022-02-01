package game

import components.*
import core.IComponent
import core.Instance
import parser.Scene
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * This file contains hardcoded scenes and entities. XML serialization really sucks, and I have a
 * tight deadline so here comes hardcoding!!
 */

object SceneRegistry {

    private val sceneMap = HashMap<String, () -> Scene>()

    fun initialize() {
        sceneMap["baseScene"] = ::baseScene
    }

    fun loadScene(name: String) : Scene {
        assert(sceneMap[name] != null)
        return sceneMap[name]!!()
    }

    private fun baseScene() : Scene {
        val instance = Instance()

        val scene = HashMap<Int, ArrayList<IComponent>>()

        val cameraEntity = instance.createEntity()
        val cameraCameraComponent = CameraComponent()
        val cameraTransformComponent = TransformComponent()

        scene[cameraEntity] = arrayListOf(cameraCameraComponent, cameraTransformComponent)

        val simpleEntity = instance.createEntity()
        val simpleTransformComponent = TransformComponent()
        simpleTransformComponent.pos.x = -106.0f
        val simpleSpriteComponent = SpriteComponent()
        simpleSpriteComponent.sprite
        val simpleStateComponent = StateComponent()
        val simpleDynamicComponent = DynamicComponent()
        val simpleCommandComponent = CommandComponent(ControllerType.LOCAL_INPUT, LinkedList<Command>())
        val simpleCharacterComponent = CharacterComponent(50.0f)

        scene[simpleEntity] = arrayListOf(
            simpleTransformComponent,
            simpleSpriteComponent,
            simpleStateComponent,
            simpleDynamicComponent,
            simpleCommandComponent,
            simpleCharacterComponent
        )

        val simpleEntity2 = instance.createEntity()
        val simpleTransformComponent2 = TransformComponent()
        simpleTransformComponent2.pos.x = 42.0f
        val simpleSpriteComponent2 = SpriteComponent("invalid")
        simpleSpriteComponent2.sprite = "error"

        scene[simpleEntity2] = arrayListOf(simpleTransformComponent2, simpleSpriteComponent2)

        val dynamicEntity = instance.createEntity()
        val dynamicDynamicComponent = DynamicComponent()
        dynamicDynamicComponent.speed.y = 200.0f
        val dynamicTransformComponent = TransformComponent()
        dynamicTransformComponent.pos.x = -32.0f
        val dynamicSpriteComponent = SpriteComponent()

        return scene
    }


}
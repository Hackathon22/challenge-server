import core.IComponent
import kotlin.reflect.KClass

/**
 * Component holding all the values to synchronize over network
 */
class NetworkComponent : IComponent {
    // map containing lists of component properties to synchronize for each Component
    val synchronizedProperties = HashMap<KClass<out IComponent>, List<String>>()
    val clientPredicted = false
    val ignorePrediction = false
}

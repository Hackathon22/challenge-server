package components

import core.IComponent
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

typealias PropertyRegister = HashMap<KClass<out IComponent>, ArrayList<String>>

/**
 * Component holding all the values to synchronize over network
 */
class NetworkComponent : IComponent {
    // map containing lists of component properties to synchronize for each Component
    val synchronizedProperties = PropertyRegister()
    var clientPredicted = false
    var ignorePrediction = false
    var networkID : UUID? = null
}

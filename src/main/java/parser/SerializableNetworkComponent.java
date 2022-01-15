package parser;

import core.IComponent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * As the NetworkComponent contains components class objects to describe which component is being networked, it cannot
 * be serialized to XML. This Class is a placeholder that saves the NetworkComponent with strings instead of the class name
 *
 * This object has to be defined in Java code instead of Kotlin as XMLEncoding doesn't work on nested HashMaps.
 */
public class SerializableNetworkComponent implements IComponent {
    public boolean clientPredicted = false;
    public boolean ignorePrediction = false;
    public HashMap<String, ArrayList<String>> synchronizedProperties = new HashMap<>();
}

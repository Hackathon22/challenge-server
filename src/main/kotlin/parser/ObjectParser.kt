package parser

import java.beans.XMLDecoder
import java.io.BufferedInputStream
import java.io.FileInputStream

/**
 * XML object deserialization utils
 */
object XMLObjectReader {

    /**
     * Reads an object from the given file and casts it to type T
     *
     * @param path, path to the XML file.
     * @throws java.io.FileNotFoundException, if the specified file does not exist.
     * @throws ArrayIndexOutOfBoundsException, if the file does not contain any object.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> readObject(input: String) : T {
        val inputStream = javaClass.getResourceAsStream(input)!!
        val decoder = XMLDecoder(BufferedInputStream(inputStream))
        val obj = decoder.readObject() as T
        decoder.close()
        return obj
    }
}

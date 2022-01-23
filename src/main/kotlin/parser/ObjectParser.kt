package parser

import java.beans.XMLDecoder
import java.beans.XMLEncoder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

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
    fun <T> readObject(path: String) : T {
        val decoder = XMLDecoder(BufferedInputStream(FileInputStream(path)))
        val obj = decoder.readObject() as T
        decoder.close()
        return obj
    }
}

/**
 * XML object serialization utils
 */
object XMLObjectWriter {

    /**
     * Writes an object to the given file.
     *
     * @param path, path to the XML file.
     * @throws java.io.FileNotFoundException, if the specified file does not exist.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> writeObject(path: String, obj: T) {
        val encoder = XMLEncoder(BufferedOutputStream(FileOutputStream(path)))
        encoder.writeObject(obj)
        encoder.close()
    }
}
@file:JvmName("KotPopularityLinker")
package edu.unh.cs980

import com.aliasi.dict.DictionaryEntry
import com.aliasi.dict.ExactDictionaryChunker
import com.aliasi.dict.ApproxDictionaryChunker
import com.aliasi.dict.MapDictionary
import com.aliasi.test.unit.tokenizer.TokenLengthTokenizerFactoryTest
import com.aliasi.tokenizer.EnglishStopTokenizerFactory
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import com.aliasi.tokenizer.TokenLengthTokenizerFactory
import com.aliasi.util.AbstractExternalizable
import java.io.*

/**
 * Desc: Connects to hyperlink database and creates a dictionary chunker from the entity mentions in database.
 *      Used for retrieving the most popular candidate entities. Entities may be filtered by setting minPop.
 *
 * @param minPop: Minimum popularity score (0.0 - 1.0) that candidate must have (or else it is filtered)
 */
class PopularityLinker(databaseLoc: String, var minPop: Double = 0.0) {
    val hyperIndexer = HyperlinkIndexer(databaseLoc)
    val chunker: ExactDictionaryChunker = buildDictionaryChunker()

    // No longer using this, but this is an example of serializing dictionary
    fun saveDictionary(out: String, dict: MapDictionary<String>) {
        val outStream = ObjectOutputStream(FileOutputStream(File(out)))
        AbstractExternalizable.compileOrSerialize(dict, outStream)
    }

    // No longer using this, but this is an example of deserializing dictionary
    fun loadDictionary(dictLoc: String): MapDictionary<String> {
        val inStream = ObjectInputStream(FileInputStream(File(dictLoc)))
        return AbstractExternalizable.readObject(File(dictLoc)) as MapDictionary<String>
    }

    fun createDictionary(): MapDictionary<String> {
        val dict = MapDictionary<String>()
        hyperIndexer.mentionSet.forEach { mention ->
            mention .replace("_", " ")
                .apply { dict.addEntry(DictionaryEntry<String>(this, "")) }
        }
        return dict
    }

    fun buildDictionaryChunker(): ExactDictionaryChunker {
        val dict = createDictionary()

        // I arbitrarily filter out chunks that are too small or too large, or are stop words (maybe not a good idea)
        val factory = IndoEuropeanTokenizerFactory()
            .run(::EnglishStopTokenizerFactory)
            .run { TokenLengthTokenizerFactory(this, 4, 30) }
        return ExactDictionaryChunker(dict, factory, false, false)
    }

    /**
     * Desc: For given string, chunks up string and checks to see if the chunks are entity mentions that exist
     *       in hyperlink database. If they are, retrieve the candidate that has linked the most number of times
     *       from the given entity mention. Does this for each of the chunks, so returns a list of entities.
     *       Note: filters out entities that are not popular enough (using @param minPop)
     *
     */
    fun annotateByPopularity(text: String): List<String> =
            chunker
                .chunk(text)
                .chunkSet()
                .map {  chunk -> text.substring(chunk.start(), chunk.end()) }
                .filter(hyperIndexer::hasEntityMention)
                .map(hyperIndexer::getPopular)
                .filter { (token, popularity) ->  popularity > minPop}
                .map { (token, popularity) -> token }
}


@file:JvmName("KotPopularityLinker")
package edu.unh.cs980

import com.aliasi.dict.DictionaryEntry
import com.aliasi.dict.ExactDictionaryChunker
import com.aliasi.dict.MapDictionary
import com.aliasi.tokenizer.EnglishStopTokenizerFactory
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import com.aliasi.util.AbstractExternalizable
import java.io.*

class PopularityLinker(databaseLoc: String, dictLoc: String) {
    val hyperIndexer = HyperlinkIndexer(databaseLoc)
    val chunker = getChunker(dictLoc)

    fun saveDictionary(out: String, dict: MapDictionary<String>) {
        val outStream = ObjectOutputStream(FileOutputStream(File(out)))
        AbstractExternalizable.compileOrSerialize(dict, outStream)
    }

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

//        val tokenFactory = IndoEuropeanTokenizerFactory().run(::EnglishStopTokenizerFactory)
//        val tokenFactory = IndoEuropeanTokenizerFactory()
//        ExactDictionaryChunker
//        return ExactDictionaryChunker(dict, IndoEuropeanTokenizerFactory.INSTANCE, false, false)
    }

    fun getChunker(dictLoc: String): ExactDictionaryChunker {
        val dict =  if (!File(dictLoc).exists()) createDictionary().apply { saveDictionary(dictLoc, this) }
                    else loadDictionary(dictLoc)

        return ExactDictionaryChunker(dict, IndoEuropeanTokenizerFactory.INSTANCE, false, false)
    }

    fun annotateByPopularity(text: String): List<String> {
        chunker.chunk(text).chunkSet().forEach {  chunk ->
            println(text.substring(chunk.start(), chunk.end()))
            println(chunk.score())
        }
        return emptyList()
    }
}


@file:JvmName("KotPopularityLinker")
package edu.unh.cs980

import com.aliasi.dict.DictionaryEntry
import com.aliasi.dict.ExactDictionaryChunker
import com.aliasi.dict.MapDictionary
import com.aliasi.tokenizer.EnglishStopTokenizerFactory
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import java.io.*

class PopularityLinker(databaseLoc: String, chunkerLoc: String) {
    val hyperIndexer = HyperlinkIndexer(databaseLoc)
    val chunker = getChunker(chunkerLoc)

    fun saveChunker(out: String, chunker: ExactDictionaryChunker) {
        val outStream = ObjectOutputStream(FileOutputStream(File(out)))
        outStream.writeObject(chunker)
    }

    fun loadChunker(chunkerLoc: String): ExactDictionaryChunker {
        val inStream = ObjectInputStream(FileInputStream(File(chunkerLoc)))
        return inStream.readObject() as ExactDictionaryChunker
    }

    fun createChunker(): ExactDictionaryChunker {
        val dict = MapDictionary<String>()
        hyperIndexer.mentionSet.forEach { mention ->
            mention .replace("_", " ")
                .apply { dict.addEntry(DictionaryEntry<String>(this, "")) }
        }

        val tokenFactory = IndoEuropeanTokenizerFactory().run(::EnglishStopTokenizerFactory)
        return ExactDictionaryChunker(dict, tokenFactory, false, false)
    }

    fun getChunker(chunkerLoc: String): ExactDictionaryChunker =
            if (!File(chunkerLoc).exists()) createChunker().apply { saveChunker(chunkerLoc, this) }
            else loadChunker(chunkerLoc)

    fun annotateByPopularity(text: String): List<String> {
        chunker.chunk(text).chunkSet().forEach {  chunk ->
            println(text.substring(chunk.start(), chunk.end()))
            println(chunk.score())
        }
        return emptyList()
    }
}


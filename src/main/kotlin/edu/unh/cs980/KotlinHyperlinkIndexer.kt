@file:JvmName("KotHyperlinkIndexer")
package edu.unh.cs980

import com.aliasi.dict.Dictionary
import com.aliasi.dict.DictionaryEntry
import com.aliasi.dict.ExactDictionaryChunker
import com.aliasi.dict.MapDictionary
import com.aliasi.tokenizer.EnglishStopTokenizerFactory
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import com.aliasi.tokenizer.LowerCaseTokenizerFactory
import com.aliasi.tokenizer.TokenizerFactory
import edu.unh.cs.treccar_v2.Data
import edu.unh.cs.treccar_v2.read_data.DeserializeData
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.mapdb.serializer.SerializerArrayTuple
import java.io.File
import java.io.StringReader
import java.util.concurrent.atomic.AtomicInteger


class HyperlinkIndexer(filename: String) {
    val db = DBMaker
        .fileDB(filename)
        .fileMmapEnable()
        .closeOnJvmShutdown()
        .concurrencyScale(60)
        .make()

    val map = db.treeMap("links")
        .keySerializer(SerializerArrayTuple(Serializer.STRING, Serializer.STRING))
        .valueSerializer(Serializer.INTEGER)
        .createOrOpen()

    val mentionSet = db.hashSet("mentions")
        .serializer(Serializer.STRING)
        .createOrOpen()

    val analyzer = StandardAnalyzer()

    fun addLink(anchorText: String, entity: String) {
        map.compute(arrayOf(anchorText, entity), { key, value -> value?.inc() ?: 1 })
        mentionSet += anchorText
    }

    val dict = MapDictionary<String>()

    fun addLinks(links: List<Pair<String, String>> ) =
            links.forEach { (anchorText, entity) -> addLink(anchorText, entity) }

    fun getLink(anchorText: String, entity: String): Int =
            map.get(arrayOf(anchorText, entity)) ?: 0

    fun getPopular(anchorText: String): Pair<String, Double> =
            map.prefixSubMap(arrayOf(anchorText))
                .let { entitiesByAnchorText ->
                    val total = entitiesByAnchorText.entries.sumBy { it.value }

                    entitiesByAnchorText
                        .entries
                        .sortedByDescending { (k,v) -> v }
                        .first()
                        .let { mostPopularEntity ->
                            (mostPopularEntity.key[1] as String) to mostPopularEntity.value / total.toDouble()
                        }
                }

    fun hasEntityMention(mention: String) = mention in mentionSet

    fun indexHyperlinks(filename: String) {
        val f = File(filename).inputStream().buffered(16 * 1024)
        val clean = {string: String -> string.toLowerCase().replace(" ", "_")}
        val counter = AtomicInteger()

        DeserializeData.iterableAnnotations(f)
            .forEachParallel { page ->

                // This is just to keep track of how many pages we've parsed
                counter.incrementAndGet().let {
                    if (it % 100000 == 0) {
                        println(it)
                    }
                }

                // Extract all of the anchors/entities and add them to database
                page.flatSectionPathsParagraphs()
                    .flatMap { psection ->
                        psection.paragraph.bodies
                            .filterIsInstance<Data.ParaLink>()
                            .map { paraLink -> clean(paraLink.anchorText) to clean(paraLink.page) } }
                    .apply(this::addLinks)
            }
    }

    fun addDictionaryEntries() {
        mentionSet.forEach { mention ->
            mention.replace("_", " ")
                .apply { dict.addEntry(DictionaryEntry<String>(this, "")) }
        }
    }

    fun annotateByPopularity(text: String): List<String> {
        if (dict.isEmpty()) {
            addDictionaryEntries()
        }

        val tokenFactory = IndoEuropeanTokenizerFactory()
        val chunker = ExactDictionaryChunker(dict, tokenFactory)
        chunker.chunk(text).chunkSet().forEach {  chunk ->
            println(text.substring(chunk.start(), chunk.end()))
        }
        return emptyList()
    }

}


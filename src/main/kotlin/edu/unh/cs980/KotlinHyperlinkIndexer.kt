@file:JvmName("KotHyperlinkIndexer")
package edu.unh.cs980

import com.aliasi.dict.Dictionary
import com.aliasi.dict.DictionaryEntry
import com.aliasi.dict.ExactDictionaryChunker
import com.aliasi.dict.MapDictionary
import com.aliasi.tokenizer.EnglishStopTokenizerFactory
import edu.unh.cs.treccar_v2.Data
import edu.unh.cs.treccar_v2.read_data.DeserializeData
import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.mapdb.serializer.SerializerArrayTuple
import java.io.*
import java.util.concurrent.atomic.AtomicInteger


class HyperlinkIndexer(filename: String) {
    // Database used to store anchor text / candidate entities
    val db = DBMaker
        .fileDB(filename)
        .fileMmapEnable()
        .closeOnJvmShutdown()
        .concurrencyScale(60)
        .make()

    // Key: (anchor text, linked entity)
    // value: Number of times we have seen anchor text refer to linked entity
    val map = db.treeMap("links")
        .keySerializer(SerializerArrayTuple(Serializer.STRING, Serializer.STRING))
        .valueSerializer(Serializer.INTEGER)
        .createOrOpen()

    // Just the entity mentions themselves (used for building dictionary chunker)
    val mentionSet = db.hashSet("mentions")
        .serializer(Serializer.STRING)
        .createOrOpen()


    /**
     * Desc: Increment the numbers of times we've seen this anchor text refer to the linked entity.
     */
    fun addLink(anchorText: String, entity: String) {
        map.compute(arrayOf(anchorText, entity), { key, value -> value?.inc() ?: 1 })
        mentionSet += anchorText
    }

    // Links all of the mentions in the list
    fun addLinks(links: List<Pair<String, String>> ) =
            links.forEach { (anchorText, entity) -> addLink(anchorText, entity) }

    fun getLink(anchorText: String, entity: String): Int =
            map.get(arrayOf(anchorText, entity)) ?: 0

    /**
     * Desc: Given an entity mention, returns the most popular linked entity and its popularity score.
     */
    fun getPopular(anchorText: String): Pair<String, Double> =
            // Prefix submap lets us return all keys that contain the entity mention
            map.prefixSubMap(arrayOf(anchorText))
                .let { entitiesLinkedByAnchorText ->
                    val total = entitiesLinkedByAnchorText.entries.sumBy { it.value }

                    entitiesLinkedByAnchorText
                        .entries
                        .sortedByDescending { (k,v) -> v }
                        .first()
                        .let { mostPopularEntity ->
                            (mostPopularEntity.key[1] as String) to mostPopularEntity.value / total.toDouble()
                        }
                }

    fun hasEntityMention(mention: String) = mention in mentionSet

    /**
     * Desc: Retrieves pairs of anchor text and linked entities and updates database in parallel.
     */
    fun indexHyperlinks(filename: String) {
        val f = File(filename).inputStream().buffered(16 * 1024)
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
                            .map { paraLink -> paraLink.anchorText.toLowerCase() to paraLink.page.toLowerCase() } }
                    .apply(this::addLinks)
            }
    }
}


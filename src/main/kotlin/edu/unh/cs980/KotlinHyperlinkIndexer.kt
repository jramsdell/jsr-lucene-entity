@file:JvmName("KotHyperlinkIndexer")
package edu.unh.cs980

import edu.unh.cs.treccar_v2.Data
import edu.unh.cs.treccar_v2.read_data.DeserializeData
import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.mapdb.serializer.SerializerArrayTuple
import java.io.File
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

    fun addLink(anchorText: String, entity: String) {
        map.compute(arrayOf(anchorText, entity), { key, value -> value?.inc() ?: 1 })
        mentionSet += anchorText
    }

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
}

fun indexHyperlinks(filename: String, databaseName: String) {
    val kotIndexer = HyperlinkIndexer(databaseName)
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
                .apply(kotIndexer::addLinks)
        }
}


fun main(args: Array<String>) {
    val indexer = HyperlinkIndexer("entity_links.db")
    indexer.addLink("yo", "yep")
    indexer.addLink("yo", "yep")
    indexer.addLink("yo", "yep")
    indexer.addLink("yo", "yep")
    indexer.addLink("yo", "yee")
    indexer.addLink("yo", "yee")
    indexer.addLink("yo", "yee")
    println(indexer.getPopular("yo"))
}


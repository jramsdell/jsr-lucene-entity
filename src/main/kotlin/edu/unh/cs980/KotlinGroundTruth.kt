@file:JvmName("KotGroundTruth")
package edu.unh.cs980

import edu.unh.cs.treccar_v2.Data
import edu.unh.cs.treccar_v2.read_data.DeserializeData
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import retrieveSpotlightEntities
import retrieveTagMeEntities
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

data class ParData(val entities: HashSet<String>, val text: String)

class KotlinGroundTruth(filename: String) {
    val clean = {string: String -> string.toLowerCase().replace(" ", "_")}

    val paragraphs =
            File(filename)
                .inputStream()
                .buffered()
                .let { file ->
                    DeserializeData.iterableParagraphs(file)
                        .map { paragraph ->
                            (paragraph) to ParData(paragraph.entitiesOnly.map(clean).toHashSet(),
                                                    paragraph.textOnly) }
                        .toMap()
                }

    fun evaluateLinker(f: (String) -> List<String>): Double =
        paragraphs.entries
            .pmap { (pid, paragraph) ->
                val entityMentions = f(paragraph.text).map(clean).toHashSet()
                val correctlyLinked = paragraph.entities.intersect(entityMentions).size
                val recall = correctlyLinked / paragraph.entities.size.toDouble()
                val precision = correctlyLinked / entityMentions.size.toDouble()

                // return F1-mesure
                (2 * precision * recall) / (precision + recall) }
            .map { if (it.isFinite()) it else 0.0 }
            .average()


    fun evaluateGroundTruths() {
        val tagMeResult = evaluateLinker(::retrieveTagMeEntities)
        println("TagMe: $tagMeResult")
        val dbPediaResult = evaluateLinker(::retrieveSpotlightEntities)
        println("DBPedia: $dbPediaResult")
        val popLinker = PopularityLinker("mydb.db", "hyperlink_dict.txt", 0.2)
        val popLinkerResult = evaluateLinker(popLinker::annotateByPopularity)
        println("Popularity Linker: $popLinkerResult")

    }
}




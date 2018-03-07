@file:JvmName("KotGroundTruth")
package edu.unh.cs980

import edu.unh.cs.treccar_v2.Data
import edu.unh.cs.treccar_v2.read_data.DeserializeData
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
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
                val entityMentions = f(paragraph.text).toHashSet()
                val correctlyLinked = paragraph.entities.intersect(entityMentions).size
                val recall = correctlyLinked / paragraph.entities.size.toDouble()
                val precision = correctlyLinked / entityMentions.size.toDouble()

                // return F1-mesure
                (2 * precision * recall) / (precision + recall) }
            .map { if (it.isFinite()) it else 0.0 }
            .average()

    fun getPrecision(entityMentions: HashSet<String>, groundTruthEntities: HashSet<String>): Double {
        val correctlyLinked = groundTruthEntities.intersect(entityMentions).size
        return correctlyLinked / groundTruthEntities.size.toDouble()
    }

    fun getRecall(entityMentions: HashSet<String>, groundTruthEntities: HashSet<String>): Double {
        val correctlyLinked = groundTruthEntities.intersect(entityMentions).size
        return correctlyLinked / groundTruthEntities.size.toDouble()
    }

    fun evaluateGroundTruths() {
        val tagMeResult = evaluateLinker(::retrieveTagMeEntities)
        println("TagMe: $tagMeResult")

    }
}




@file:JvmName("KotGroundTruth")
package edu.unh.cs980

import edu.unh.cs.treccar_v2.read_data.DeserializeData
import retrieveSpotlightEntities
import retrieveTagMeEntities
import java.io.File

/**
 * Desc: Represents the data contained in one of the lead paragraphs.
 * @entities: Set of entities (used for ground truth) that paragraph was annotated with.
 * @text: The text of the paragraph, which will be used in entity linking.
 */
data class ParData(val entities: HashSet<String>, val text: String)

/**
 * Desc: Reads lead-paragraphs.cbor and derives ground truth (of entities) for the paragraphs it contains.
 *       Accepts functions that return entities from text and evaluates these functions on the text of each paragraph.
 */
class KotlinGroundTruth(filename: String, val dbLocation: String) {
    val clean = {string: String -> string.toLowerCase().replace(" ", "_")}

    val paragraphs =
            File(filename)
                .inputStream()
                .buffered()
                .let { file ->
                    // Create a map where the keys are paragraph ids and the values are ParData classes.
                    DeserializeData.iterableParagraphs(file)
                        .map { paragraph ->
                            (paragraph) to ParData(paragraph.entitiesOnly.map(clean).toHashSet(),
                                                    paragraph.textOnly) }
                        .toMap()
                }

    /**
     * Desc: Accepts a function that takes text and returns a list of entities.
     *      Evaluates based on F1-measure.
     */
    fun evaluateLinker(f: (String) -> List<String>): Double =
        paragraphs.entries
            .filter { it.value.entities.isNotEmpty() }
            .pmap { (pid, paragraph) ->
                // Return entity linkage results for each  paragraph as a triple
                val entityMentions = f(paragraph.text).map(clean).toHashSet()
                val correctlyLinked = paragraph.entities.intersect(entityMentions).size
                Triple(correctlyLinked, entityMentions.size, paragraph.entities.size) }

            .reduce { acc, triple ->  Triple(
                    // Tally up each of the results of linking each paragraph
                    acc.first + triple.first,
                    acc.second + triple.second,
                    acc.third + triple.third)}

            .let { (correctlyLinked, entityMentions, shouldBeMentioned) ->
                // Once we've tallied the results, calculate F1-measure
                val recall = correctlyLinked / shouldBeMentioned.toDouble()
                val precision = correctlyLinked / entityMentions.toDouble()
                (2 * precision * recall) / (precision + recall)
            }


    /**
     * Desc: Evaluates TagMe, DBPedia, and (if database is available) my hyperlink method
     */
    fun evaluateGroundTruths() {
        val tagMeResult = evaluateLinker(::retrieveTagMeEntities)
        println("TagMe: $tagMeResult")
        val dbPediaResult = evaluateLinker(::retrieveSpotlightEntities)
        println("DBPedia: $dbPediaResult")

        if (dbLocation == "") {
            println("No --db location given: skipping Hyperlink method.")
            return
        }

        // Run method of entity linking using hyperlinks
        println("Building ExactDictionary chunker (this may take time")
        val popLinker = PopularityLinker(dbLocation, 0.0)
        println("Building complete! Evaluating hyperlink entity linker:")
        (0 .. 10).forEach {
            popLinker.minPop = it * 0.1
            val popLinkerResult = evaluateLinker(popLinker::annotateByPopularity)
            println("Popularity Linker(${it * 0.1}): $popLinkerResult")
        }

    }
}




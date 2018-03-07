@file:JvmName("KotCborParser")
package edu.unh.cs980

import edu.unh.cs.treccar_v2.Data
import edu.unh.cs.treccar_v2.read_data.DeserializeData
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import java.io.File
import java.util.concurrent.atomic.AtomicInteger


fun getStuff(filename: String, databaseName: String) {
    val kotIndexer = HyperlinkIndexer(databaseName)
    val f = File(filename).inputStream().buffered(100000)
    val clean = {string: String -> string.toLowerCase().replace(" ", "_")}

    DeserializeData.iterableAnnotations(f)
        .forEachParallel { page ->
            page.flatSectionPathsParagraphs()
                .flatMap { psection ->
                    psection.
                        paragraph
                        .bodies.filterIsInstance<Data.ParaLink>()
                        .map { paraLink -> clean(paraLink.anchorText) to clean(paraLink.page) } }
                .apply(kotIndexer::addLinks)
        }
//        .asSequence()
//        .map { page ->
//            page.flatSectionPathsParagraphs()
//                .flatMap { psection ->
//                                psection.
//                                    paragraph
//                                    .bodies.filterIsInstance<Data.ParaLink>()
//                                    .map { paraLink -> clean(paraLink.anchorText) to clean(paraLink.page)}
//                         }
//            }
//        .forEach(kotIndexer::addLinks)

//        .forEach { p ->
//            p.bodies.filterIsInstance<Data.ParaLink>()
//                .map { paraLink -> paraLink.anchorText to paraLink.page}
//        }
//        .flatMap    {  page ->
//            page.bodies.filterIsInstance<Data.ParaLink>()
//                .map { paraLink -> paraLink.anchorText to paraLink.page}
//                .asSequence()
//        }
}


fun main(args: Array<String>) {
}

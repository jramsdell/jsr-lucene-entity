@file:JvmName("KotCborParser")
package edu.unh.cs980

import edu.unh.cs.treccar_v2.Data
import edu.unh.cs.treccar_v2.read_data.DeserializeData
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import java.io.File

fun <A>Sequence<A>.forEachParallel(f: suspend (A) -> Unit): Unit = runBlocking {
    map { async(CommonPool) { f(it) } }.forEach { it.await() }
}


fun getStuff(filename: String, databaseName: String) {
    val kotIndexer = HyperlinkIndexer(databaseName)
    val f = File(filename).inputStream()
    val clean = {string: String -> string.toLowerCase().replace(" ", "_")}

    DeserializeData.iterableAnnotations(f)
        .asSequence()
        .take(10000)
        .flatMap { page ->
            page.flatSectionPathsParagraphs()
                .flatMap { psection ->
                                psection.
                                    paragraph
                                    .bodies.filterIsInstance<Data.ParaLink>()
                                    .map { paraLink -> clean(paraLink.anchorText) to clean(paraLink.page)}
                         }
                .asSequence()
            }
        .chunked(1000)
        .forEachParallel { links -> kotIndexer.addLinks(links) }

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

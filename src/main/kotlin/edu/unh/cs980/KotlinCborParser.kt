@file:JvmName("KotCborParser")
package edu.unh.cs980

import edu.unh.cs.treccar_v2.Data
import edu.unh.cs.treccar_v2.read_data.DeserializeData
import java.io.File


fun getStuff(filename: String) {
    val f = File(filename).inputStream()
    val clean = {string: String -> string.toLowerCase().replace(" ", "_")}

    DeserializeData.iterableAnnotations(f)
        .asSequence()
        .take(1)
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
        .take(100)
        .forEach { p -> println(p)

        }
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

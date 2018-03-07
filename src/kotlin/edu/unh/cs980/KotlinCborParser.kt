@file:JvmName("KotCborParser")
package edu.unh.cs980

import edu.unh.cs.treccar_v2.Data
import edu.unh.cs.treccar_v2.read_data.DeserializeData
import java.io.File

fun getStuff(filename: String) {
    val f = File("wee").inputStream()
    DeserializeData.iterableParagraphs(f)
        .asSequence<Data.Paragraph>()
        .flatMap    {  page ->
                            page.bodies.filterIsInstance<Data.ParaLink>()
                                .map { paraLink -> paraLink.anchorText to paraLink.page}
                                .asSequence()
                    }
        .take(1)
        .onEach { println(it) }
//        .forEach { p ->
//            p.bodies.filterIsInstance<Data.ParaLink>()
//                .map { paraLink -> paraLink.anchorText to paraLink.page}
//        }
}


fun main(args: Array<String>) {
}

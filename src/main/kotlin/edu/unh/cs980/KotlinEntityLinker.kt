@file:JvmName("KotEntityLinker")

import org.jsoup.Jsoup
import sun.net.www.http.HttpClient
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import khttp.post
import org.json.JSONObject


/**
 * Desc: Queries spotlight server with string and retrieve list of linked entities.
 * @return List of linked entities (strings). Empty if no entities were linked or if errors were encountered.
 */
fun retrieveSpotlightEntities(content: String): List<String> {
    val url = "http://model.dbpedia-spotlight.org/en/annotate"        // Hardcoded url to local server

    // Retrieve html file from the Spotlight server
    val jsoupDoc = Jsoup.connect(url)
            .data("text", content)
            .post()

    // Parse urls, returning only the last word of the url (after the last /)
    val links = jsoupDoc.select("a[href]")
    return links.map {  element ->
        val title = element.attr("title")
        title.substring(title.lastIndexOf("/") + 1)}
            .toList()
}

/**
 * Desc: Queries TagMe and returns a list of entities.
 */
fun retrieveTagMeEntities(content: String): List<String> {
    val tok = "7fa2ade3-fce7-4f4a-b994-6f6fefc7e665-843339462"
    val url = "https://tagme.d4science.org/tagme/tag"
    val p = post(url, data = mapOf(
            "gcube-token" to tok,
            "text" to content
    ))
    val results = p.jsonObject.getJSONArray("annotations")
    return  results
                .filter { result -> (result as JSONObject).getDouble("rho") > 0.2 }
                .map { result -> (result as JSONObject) .getString("title")
                                                        .replace(" ", "_")}


}

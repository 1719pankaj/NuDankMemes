package com.example.nudankmemes

import android.util.Log
import org.w3c.dom.NodeList
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class Runners {
    companion object {
        suspend fun fetchKeys(): List<String> {
            val keys = mutableListOf<String>()
            try {
                val url = URL("https://findthatmeme.us-southeast-1.linodeobjects.com/")
                val connection = url.openConnection()
                val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                val document = documentBuilder.parse(connection.getInputStream())
                val xPath = XPathFactory.newInstance().newXPath()

                val nodeList = xPath.evaluate("/ListBucketResult/Contents/Key", document, XPathConstants.NODESET) as? NodeList
                if (nodeList != null) {
                    for (i in 0 until nodeList.length) {
                        keys.add(nodeList.item(i).textContent)
                    }
                } else {
                    Log.e("FtmFragment", "Failed to evaluate XPath expression")
                }
            } catch (e: Exception) {
                Log.e("FtmFragment", "Failed to fetch keys", e)
            }
            return keys
        }
    }
}
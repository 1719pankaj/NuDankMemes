package com.example.nudankmemes.data

class BackstackAndKeys {
    companion object {
        var keys = emptyList<String>()
        val FMTmemeBackStack = ArrayList<String>()
        var FMTcurrentMemeIndex = -1
        var FMTnextMemeUrl: String? = null

        val RedditmemeBackStack = ArrayList<String>()
        var RedditcurrentMemeIndex = -1
        var RedditnextMemeUrl: String? = null

        var XKCDFirstRunFlag = true
        val XKCDmemeBackStack = ArrayList<String>()
        var XKCDcurrentMemeIndex = -1
        var XKCDnextMemeUrl: String? = null

        var SavedcurrentMemeIndex = 0
    }
}
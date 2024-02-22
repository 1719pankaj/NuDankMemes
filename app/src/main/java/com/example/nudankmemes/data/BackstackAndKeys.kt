package com.example.nudankmemes.data

class BackstackAndKeys {
    companion object {
        var keys = emptyList<String>()
        var FTMfirstRunFlag = true
        val FTMmemeBackStack = ArrayList<String>()
        var FTMcurrentMemeIndex = -1

        val RedditmemeBackStack = ArrayList<String>()
        var RedditcurrentMemeIndex = -1

        var XKCDFirstRunFlag = true
        val XKCDmemeBackStack = ArrayList<String>()
        var XKCDcurrentMemeIndex = -1

    }
}
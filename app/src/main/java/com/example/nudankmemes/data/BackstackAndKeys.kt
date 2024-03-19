package com.example.nudankmemes.data

class BackstackAndKeys {
    companion object {
        var keys = emptyList<String>()
        var FTMfirstRunFlag = true
        val FTMmemeBackStack = ArrayList<String>()
        var FTMcurrentMemeIndex = -1

        val RedditmemeBackStack = ArrayList<String>()
        var RedditcurrentMemeIndex = 0

        var FavMemesFirstRunFlag = false
        var FavMemesCurrentMemeIndex = -1
        var FavMemesList = ArrayList<String>()

        var XKCDFirstRunFlag = true
        val XKCDmemeBackStack = ArrayList<String>()
        var XKCDcurrentMemeIndex = -1

    }
}
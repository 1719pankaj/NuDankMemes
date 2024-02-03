package com.example.nudankmemes.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nudankmemes.data.BackstackAndKeys.Companion.SavedcurrentMemeIndex
import com.example.nudankmemes.databinding.FragmentSavedBinding
import java.io.File

class SavedFragment : Fragment() {

    private lateinit var binding: FragmentSavedBinding
    private lateinit var memeFiles: List<File>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSavedBinding.inflate(inflater, container, false)
        val view = binding.root

        val memeDirectory = File("${Environment.getExternalStorageDirectory()}/pictures/NuDankMemes")
        memeFiles = memeDirectory.listFiles()?.toList() ?: emptyList()

        nextMeme()

        binding.nextBT.setOnClickListener {
            nextMeme()
        }

        binding.prevBT.setOnClickListener {
            prevMeme()
        }

        return view
    }

    private fun prevMeme() {
        if (SavedcurrentMemeIndex > 0) {
            SavedcurrentMemeIndex--
            displayMeme()
        }
    }

    private fun nextMeme() {
        if (SavedcurrentMemeIndex < memeFiles.size - 1) {
            SavedcurrentMemeIndex++
            displayMeme()
        }
    }

    private fun displayMeme() {
        val bitmap = BitmapFactory.decodeFile(memeFiles[SavedcurrentMemeIndex].absolutePath)
        binding.imageView.setImageBitmap(bitmap)
    }

}
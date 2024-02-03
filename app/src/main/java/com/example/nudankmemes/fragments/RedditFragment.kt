package com.example.nudankmemes.fragments

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.nudankmemes.R
import com.example.nudankmemes.data.BackstackAndKeys.Companion.RedditcurrentMemeIndex
import com.example.nudankmemes.data.BackstackAndKeys.Companion.RedditmemeBackStack
import com.example.nudankmemes.data.BackstackAndKeys.Companion.RedditnextMemeUrl
import com.example.nudankmemes.databinding.FragmentRedditBinding
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class RedditFragment : Fragment() {

    private lateinit var binding: FragmentRedditBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRedditBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.imageView.setImageResource(R.drawable.holdup)

        getNextComic()

        binding.nextBT.setOnClickListener {
            getNextComic()
        }

        binding.prevBT.setOnClickListener {
            goBack()
        }

        binding.shareBT.setOnClickListener {
            shareCurrentMeme()
        }

        binding.saveBT.setOnClickListener {
            saveCurrentMeme()
        }

        return view
    }

    private fun saveCurrentMeme() {
        val drawable = binding.imageView.drawable
        if (drawable == null) {
            Toast.makeText(context, "No image to save", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmap = drawable.toBitmap()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "image.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "pictures/NuDankMemes/")
            }

            val uri = context?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            context?.contentResolver?.openOutputStream(uri!!)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        } else {
            val externalStorageDirectory = Environment.getExternalStorageDirectory().toString()
            val file = File("$externalStorageDirectory/pictures/NuDankMemes", "image.png")
            file.parentFile?.mkdirs()
            val outStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.flush()
            outStream.close()
        }

        Toast.makeText(context, "Image saved successfully", Toast.LENGTH_SHORT).show()
    }

    private fun shareCurrentMeme() {
        try {
            val drawable = binding.imageView.drawable
            if (drawable == null) {
                Toast.makeText(context, "No image to share", Toast.LENGTH_SHORT).show()
                return
            }

            val bitmap = drawable.toBitmap()
            val cachePath = File(context?.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "image.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(requireContext(), "com.example.nudankmemes.provider", file)
            if (contentUri == null) {
                Toast.makeText(context, "Failed to create content URI", Toast.LENGTH_SHORT).show()
                return
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context?.contentResolver?.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
            }
            startActivity(Intent.createChooser(shareIntent, "Share image via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getNextComic() {
        CoroutineScope(Dispatchers.Main).launch {
            val imageUrl: String
            if (RedditcurrentMemeIndex < RedditmemeBackStack.size - 1) {
                // If we're not at the end of the backstack, move forward
                RedditcurrentMemeIndex++
                imageUrl = RedditmemeBackStack[RedditcurrentMemeIndex]
            } else {
                // If we're at the end of the backstack, use the preloaded meme if available
                imageUrl = RedditnextMemeUrl ?: getRandomMeme()
                RedditmemeBackStack.add(imageUrl) // Add the meme to the backstack
                RedditcurrentMemeIndex = RedditmemeBackStack.size - 1
                RedditnextMemeUrl = null // Reset the preloaded meme
            }
            loadWithGlide(imageUrl, binding.imageView)

            // Preload the next meme if we're at the end of the backstack
            if (RedditcurrentMemeIndex == RedditmemeBackStack.size - 1) {
                preloadNextMeme()
            }
        }
    }

    fun goBack() {
        if (RedditcurrentMemeIndex > 0) {
            // If we're not at the start of the backstack, move backward
            RedditcurrentMemeIndex--
            val imageUrl = RedditmemeBackStack[RedditcurrentMemeIndex]
            loadWithGlide(imageUrl, binding.imageView)
        } else {
            Toast.makeText(context, "No more memes to go back to", Toast.LENGTH_SHORT).show()
        }

        // If we're at the end of the backstack, there's no next meme to preload
        if (RedditcurrentMemeIndex == RedditmemeBackStack.size - 1) {
            RedditnextMemeUrl = null
        }
    }

    suspend fun getRandomMeme(): String {
        return withContext(Dispatchers.IO) {
            val url = URL("https://meme-api.com/gimme")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            connection.disconnect()
            val jsonObject = JSONObject(response)
            jsonObject.getString("url")
        }
    }


    fun preloadNextMeme() {
        CoroutineScope(Dispatchers.IO).launch {
            RedditnextMemeUrl = getRandomMeme()
        }
    }

    fun loadWithGlide(imageUrl: String, imageView: PhotoView) {
        if (isAdded && activity != null) {
            Glide.with(this@RedditFragment)
                .load(imageUrl)
                .placeholder(R.drawable.reddit_waiting)
                .fitCenter()
                .into(imageView)
        }
    }

}
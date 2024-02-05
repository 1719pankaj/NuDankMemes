package com.example.nudankmemes.fragments

import android.content.ContentValues
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.nudankmemes.R
import com.example.nudankmemes.data.BackstackAndKeys.Companion.XKCDFirstRunFlag
import com.example.nudankmemes.data.BackstackAndKeys.Companion.XKCDcurrentMemeIndex
import com.example.nudankmemes.data.BackstackAndKeys.Companion.XKCDmemeBackStack
import com.example.nudankmemes.data.BackstackAndKeys.Companion.XKCDnextMemeUrl
import com.example.nudankmemes.databinding.FragmentXkcdBinding
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class XKCDFragment : Fragment() {

    private lateinit var binding: FragmentXkcdBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentXkcdBinding.inflate(layoutInflater)
        val view = binding.root

        binding.progressBar.isIndeterminate = true
        binding.imageView.setImageResource(R.drawable.holdup)

        if (XKCDmemeBackStack.isNotEmpty() && XKCDcurrentMemeIndex >= 0) {
            // If there's a meme in the backstack, display it
            loadWithGlide(XKCDmemeBackStack[XKCDcurrentMemeIndex], binding.imageView)
        } else if (XKCDFirstRunFlag) {
            // If it's the first run, fetch the latest comic
            getLatestComic()
            XKCDFirstRunFlag = false
        } else {
            // Otherwise, fetch the next comic
            getNextComic()
        }

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

        val imageUrl = XKCDmemeBackStack[XKCDcurrentMemeIndex]
        val fileExtension = when {
            imageUrl.endsWith(".gif") -> "gif"
            imageUrl.endsWith(".png") -> "png"
            else -> "png"
        }
        val fileName = "image.$fileExtension"
        val mimeType = "image/$fileExtension"

        CoroutineScope(Dispatchers.IO).launch {
            val file = Glide.with(requireContext())
                .asFile()
                .load(imageUrl)
                .submit()
                .get()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "pictures/NuDankMemes/")
                }

                val uri = context?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                context?.contentResolver?.openOutputStream(uri!!)?.use { outputStream ->
                    file.inputStream().copyTo(outputStream)
                }
            } else {
                val externalStorageDirectory = Environment.getExternalStorageDirectory().toString()
                val outputFile = File("$externalStorageDirectory/pictures/NuDankMemes", fileName)
                outputFile.parentFile?.mkdirs()
                file.copyTo(outputFile, overwrite = true)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Image saved successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareCurrentMeme() {
        try {
            val drawable = binding.imageView.drawable
            if (drawable == null) {
                Toast.makeText(context, "No image to share", Toast.LENGTH_SHORT).show()
                return
            }

            val imageUrl = XKCDmemeBackStack[XKCDcurrentMemeIndex]
            val fileExtension = when {
                imageUrl.endsWith(".gif") -> "gif"
                imageUrl.endsWith(".png") -> "png"
                else -> "png"
            }
            val fileName = "image.$fileExtension"

            CoroutineScope(Dispatchers.IO).launch {
                val file = Glide.with(requireContext())
                    .asFile()
                    .load(imageUrl)
                    .submit()
                    .get()

                val cachePath = File(context?.cacheDir, "images")
                cachePath.mkdirs()
                val outputFile = File(cachePath, fileName)
                file.copyTo(outputFile, overwrite = true)

                val contentUri = FileProvider.getUriForFile(requireContext(), "com.example.nudankmemes.provider", outputFile)
                if (contentUri == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to create content URI", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context?.contentResolver?.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                }

                withContext(Dispatchers.Main) {
                    startActivity(Intent.createChooser(shareIntent, "Share image via"))
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getNextComic() {
        CoroutineScope(Dispatchers.Main).launch {
            val imageUrl: String
            if (XKCDcurrentMemeIndex < XKCDmemeBackStack.size - 1) {
                // If we're not at the end of the backstack, move forward
                XKCDcurrentMemeIndex++
                imageUrl = XKCDmemeBackStack[XKCDcurrentMemeIndex]
            } else {
                // If we're at the end of the backstack, use the preloaded meme if available
                imageUrl = XKCDnextMemeUrl ?: fetchNewMeme()
                XKCDmemeBackStack.add(imageUrl) // Add the meme to the backstack
                XKCDcurrentMemeIndex = XKCDmemeBackStack.size - 1
                XKCDnextMemeUrl = null // Reset the preloaded meme
            }
            loadWithGlide(imageUrl, binding.imageView)

            // Preload the next meme if we're at the end of the backstack
            if (XKCDcurrentMemeIndex == XKCDmemeBackStack.size - 1) {
                preloadNextMeme()
            }
        }
    }


    private fun goBack() {
        if (XKCDcurrentMemeIndex > 0) {
            // If we're not at the start of the backstack, move backward
            XKCDcurrentMemeIndex--
            val imageUrl = XKCDmemeBackStack[XKCDcurrentMemeIndex]
            loadWithGlide(imageUrl, binding.imageView)
        } else {
            Toast.makeText(context, "Backstack Empty", Toast.LENGTH_SHORT).show()
        }

        // If we're at the end of the backstack, there's no next meme to preload
        if (XKCDcurrentMemeIndex == XKCDmemeBackStack.size - 1) {
            XKCDnextMemeUrl = null
        }
    }

    private suspend fun fetchNewMeme(): String {
        val latestComicNum = fetchLatestComicNum()
        val randomComicNum = (1..latestComicNum).random()
        return fetchComicImageUrl(randomComicNum)
    }

    private fun preloadNextMeme() {
        CoroutineScope(Dispatchers.IO).launch {
            XKCDnextMemeUrl = fetchNewMeme()
        }
    }

    private suspend fun fetchLatestComicNum(): Int {
        return withContext(Dispatchers.IO) {
            val url = URL("https://xkcd.com/info.0.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            connection.disconnect()
            val jsonObject = JSONObject(response)
            jsonObject.getInt("num")
        }
    }

    private suspend fun fetchComicImageUrl(comicNum: Int): String {
        return withContext(Dispatchers.IO) {
            val url = URL("https://xkcd.com/$comicNum/info.0.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            connection.disconnect()
            val jsonObject = JSONObject(response)
            jsonObject.getString("img")
        }
    }

    private fun getLatestComic() {
    CoroutineScope(Dispatchers.Main).launch {
        val latestComicNum = fetchLatestComicNum()
        val imageUrl = fetchComicImageUrl(latestComicNum)
        XKCDmemeBackStack.add(imageUrl) // Add the meme to the backstack
        XKCDcurrentMemeIndex = XKCDmemeBackStack.size - 1 // Update the current meme index
        loadWithGlide(imageUrl, binding.imageView)
        preloadNextMeme()
    }
}

    private fun loadWithGlide(imageUrl: String, imageView: PhotoView) {
        if (isAdded && activity != null) {
            binding.progressBar.visibility = View.VISIBLE
            Glide.with(this@XKCDFragment)
                .load(imageUrl)
                .placeholder(R.drawable.xkcd_waiting)
                .fitCenter()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.progressBar.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.progressBar.visibility = View.GONE
                        return false
                    }
                })
                .into(imageView)
        }
    }
}
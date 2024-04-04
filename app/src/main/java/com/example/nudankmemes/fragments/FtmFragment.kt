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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.nudankmemes.R
import com.example.nudankmemes.data.BackstackAndKeys
import com.example.nudankmemes.data.BackstackAndKeys.Companion.FTMcurrentMemeIndex
import com.example.nudankmemes.data.BackstackAndKeys.Companion.FTMfirstRunFlag
import com.example.nudankmemes.data.BackstackAndKeys.Companion.FTMmemeBackStack
import com.example.nudankmemes.data.BackstackAndKeys.Companion.keys
import com.example.nudankmemes.databinding.FragmentFtmBinding
import com.example.nudankmemes.global.Variables
import com.example.nudankmemes.helpers.SharedPrefManager
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.NodeList
import java.io.File
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.random.Random


class FtmFragment : Fragment() {

    private lateinit var binding: FragmentFtmBinding
    private lateinit var sharedPrefManager: SharedPrefManager
    private var isLoading = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFtmBinding.inflate(layoutInflater)
        val view = binding.root

        sharedPrefManager = SharedPrefManager(requireContext())

        if(FTMfirstRunFlag) {
            binding.progressBar.visibility = View.VISIBLE
            FTMfirstRunFlag = false
        } else {
            binding.progressBar.visibility = View.GONE
        }

        binding.progressBar.isIndeterminate = true
        binding.imageView.setImageResource(R.drawable.holdup)

        if(keys.isEmpty()) {
            lifecycleScope.launch(Dispatchers.Main) {
                keys = fetchKeys()
                Log.d("FtmFragment", "Keys: $keys")
                binding.progressBar.visibility = View.GONE
                if (FTMmemeBackStack.isNotEmpty() && FTMcurrentMemeIndex >= 0) {
                    // If there's a meme in the backstack, display it
                    loadWithGlide(FTMmemeBackStack[FTMcurrentMemeIndex], binding.imageView)
                } else {
                    // Otherwise, fetch the next comic
                    getNextComic()
                }

                changeClickListenersForCompatMode()

                binding.shareBT.setOnClickListener {
                    if (!isLoading) {
                        shareCurrentMeme()
                    }
                }

                binding.saveBT.setOnClickListener {
                    if (!isLoading) {
                        saveCurrentMeme()
                    }
                }

                binding.addToFavBT.setOnClickListener {
                    if (!isLoading) {
                        val currentMemeUrl = FTMmemeBackStack[FTMcurrentMemeIndex]
                        sharedPrefManager.saveMeme(currentMemeUrl)
                    }
                }
            }
        } else {

            if (FTMmemeBackStack.isNotEmpty() && FTMcurrentMemeIndex >= 0) {
                // If there's a meme in the backstack, display it
                loadWithGlide(FTMmemeBackStack[FTMcurrentMemeIndex], binding.imageView)
            } else {
                // Otherwise, fetch the next comic
                getNextComic()
            }

            changeClickListenersForCompatMode()

            binding.shareBT.setOnClickListener {
                if (!isLoading) {
                    shareCurrentMeme()
                }
            }

            binding.saveBT.setOnClickListener {
                if (!isLoading) {
                    saveCurrentMeme()
                }
            }

            binding.addToFavBT.setOnClickListener {
                    if (!isLoading) {
                        val currentMemeUrl = FTMmemeBackStack[FTMcurrentMemeIndex]
                        sharedPrefManager.saveMeme(currentMemeUrl)
                    }
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        changeClickListenersForCompatMode()
    }

    private fun changeClickListenersForCompatMode() {
        if(Variables.config_compatibility_mode_buttons) {
            binding.nextBT.visibility = View.VISIBLE
            binding.prevBT.visibility = View.VISIBLE
            binding.legacyModeTV.visibility = View.VISIBLE
            binding.nextBT.setOnClickListener {
                if (!isLoading) {
                    getNextComic()
                }
            }
            binding.prevBT.setOnClickListener {
                if (!isLoading) {
                    goBack()
                }
            }
        } else {
            binding.nextBT.visibility = View.GONE
            binding.prevBT.visibility = View.GONE
            binding.legacyModeTV.visibility = View.GONE
            binding.imageView.setOnViewTapListener { vw, x, y ->
                when {
                    x <= 200 -> goBack()
                    x >= 650 -> getNextComic()
                }
            }
        }
    }

    private fun getNextComic() {
        CoroutineScope(Dispatchers.Main).launch {
            val imageUrl: String
            if (FTMcurrentMemeIndex < FTMmemeBackStack.size - 1) {
                // If we're not at the end of the backstack, move forward
                FTMcurrentMemeIndex++
                imageUrl = FTMmemeBackStack[FTMcurrentMemeIndex]
            } else {
                // If we're at the end of the backstack, use the preloaded meme if available
                imageUrl = getRandomMeme()
                FTMmemeBackStack.add(imageUrl) // Add the meme to the backstack
                FTMcurrentMemeIndex = FTMmemeBackStack.size - 1
            }
            loadWithGlide(imageUrl, binding.imageView)
        }
    }

    private fun goBack() {
        if (FTMcurrentMemeIndex > 0) {
            // If we're not at the start of the backstack, move backward
            FTMcurrentMemeIndex--
            val imageUrl = FTMmemeBackStack[FTMcurrentMemeIndex]
            loadWithGlide(imageUrl, binding.imageView)
        } else {
            Toast.makeText(context, "No more memes to go back to", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getRandomMeme(): String {
        val randomKey = keys[Random.nextInt(keys.size)]
        return "https://findthatmeme.us-southeast-1.linodeobjects.com/$randomKey"
    }


    private suspend fun fetchKeys(): List<String> = withContext(Dispatchers.IO) {
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
            Log.e("FtmFragment", "Error fetching keys", e)
        }
        keys
    }

    private fun saveCurrentMeme() {
        val drawable = binding.imageView.drawable
        if (drawable == null) {
            Toast.makeText(context, "No image to save", Toast.LENGTH_SHORT).show()
            return
        }

        val imageUrl = FTMmemeBackStack[FTMcurrentMemeIndex]
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

            val imageUrl = FTMmemeBackStack[FTMcurrentMemeIndex]
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

    private fun loadWithGlide(imageUrl: String, imageView: PhotoView) {
        if (isAdded && activity != null) {
            isLoading = true
            binding.progressBar.visibility = View.VISIBLE
            Glide.with(this@FtmFragment)
                .load(imageUrl)
                .placeholder(R.drawable.ftm_waiting)
                .fitCenter()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.progressBar.visibility = View.GONE
                        isLoading = false
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
                        isLoading = false
                        return false
                    }
                })
                .into(imageView)

            CoroutineScope(Dispatchers.IO).launch {
                if(FTMmemeBackStack.size - FTMcurrentMemeIndex < 5) {
                    for (i in 1..(5- (FTMmemeBackStack.size - FTMcurrentMemeIndex))) {
                        val newMemeUrl = getRandomMeme()
                        FTMmemeBackStack.add(newMemeUrl)
                        if (isAdded && activity != null) {
                            Glide.with(this@FtmFragment)
                                .load(newMemeUrl)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .preload()
                            Log.i("FtmFragment", "Preloading meme: $newMemeUrl")
                        }
                    }
                }
            }
        }
    }

}
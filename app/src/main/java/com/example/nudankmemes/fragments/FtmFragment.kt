package com.example.nudankmemes.fragments

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.nudankmemes.R
import com.example.nudankmemes.data.BackstackAndKeys.Companion.FMTcurrentMemeIndex
import com.example.nudankmemes.data.BackstackAndKeys.Companion.FMTmemeBackStack
import com.example.nudankmemes.data.BackstackAndKeys.Companion.FMTnextMemeUrl
import com.example.nudankmemes.data.BackstackAndKeys.Companion.keys
import com.example.nudankmemes.databinding.FragmentFtmBinding
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.NodeList
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.random.Random


class FtmFragment : Fragment() {

    private lateinit var binding: FragmentFtmBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFtmBinding.inflate(layoutInflater)
        val view = binding.root

        binding.imageView.setImageResource(R.drawable.holdup)

        if(keys.isEmpty()) {
            lifecycleScope.launch(Dispatchers.Main) {
                keys = fetchKeys()
                Log.d("FtmFragment", "Keys: $keys")

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
            }
        } else {
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
        }

        return view
    }

    fun getNextComic() {
        CoroutineScope(Dispatchers.Main).launch {
            val imageUrl: String
            if (FMTcurrentMemeIndex < FMTmemeBackStack.size - 1) {
                // If we're not at the end of the backstack, move forward
                FMTcurrentMemeIndex++
                imageUrl = FMTmemeBackStack[FMTcurrentMemeIndex]
            } else {
                // If we're at the end of the backstack, use the preloaded meme if available
                imageUrl = FMTnextMemeUrl ?: getRandomMeme()
                FMTmemeBackStack.add(imageUrl) // Add the meme to the backstack
                FMTcurrentMemeIndex = FMTmemeBackStack.size - 1
                FMTnextMemeUrl = null // Reset the preloaded meme
            }
            loadWithGlide(imageUrl, binding.imageView)

            // Preload the next meme if we're at the end of the backstack
            if (FMTcurrentMemeIndex == FMTmemeBackStack.size - 1) {
                preloadNextMeme()
            }
        }
    }

    fun goBack() {
        if (FMTcurrentMemeIndex > 0) {
            // If we're not at the start of the backstack, move backward
            FMTcurrentMemeIndex--
            val imageUrl = FMTmemeBackStack[FMTcurrentMemeIndex]
            loadWithGlide(imageUrl, binding.imageView)
        } else {
            Toast.makeText(context, "No more memes to go back to", Toast.LENGTH_SHORT).show()
        }

        // If we're at the end of the backstack, there's no next meme to preload
        if (FMTcurrentMemeIndex == FMTmemeBackStack.size - 1) {
            FMTnextMemeUrl = null
        }
    }

    fun getRandomMeme(): String {
        val randomKey = keys[Random.nextInt(keys.size)]
        return "https://findthatmeme.us-southeast-1.linodeobjects.com/$randomKey"
    }

    fun preloadNextMeme() {
        CoroutineScope(Dispatchers.IO).launch {
            FMTnextMemeUrl = getRandomMeme()
        }
    }


    suspend fun fetchKeys(): List<String> = withContext(Dispatchers.IO) {
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

    fun loadWithGlide(imageUrl: String, imageView: PhotoView) {
        if (isAdded && activity != null) {
            Glide.with(this@FtmFragment)
                .load(imageUrl)
                .placeholder(R.drawable.ftm_waiting)
                .fitCenter()
                .into(imageView)
        }
    }

}
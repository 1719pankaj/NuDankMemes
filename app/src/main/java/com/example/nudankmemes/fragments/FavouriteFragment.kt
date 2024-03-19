package com.example.nudankmemes.fragments

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.nudankmemes.R
import com.example.nudankmemes.data.BackstackAndKeys
import com.example.nudankmemes.data.BackstackAndKeys.Companion.FavMemesCurrentMemeIndex
import com.example.nudankmemes.data.BackstackAndKeys.Companion.FavMemesFirstRunFlag
import com.example.nudankmemes.data.BackstackAndKeys.Companion.FavMemesList
import com.example.nudankmemes.databinding.FragmentFavouriteBinding
import com.example.nudankmemes.helpers.SharedPrefManager
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FavouriteFragment : Fragment() {

    private lateinit var binding: FragmentFavouriteBinding
    private var isLoading = false
    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFavouriteBinding.inflate(layoutInflater)

        binding.progressBar.isIndeterminate = true
        binding.imageView.setImageResource(R.drawable.holdup)

        sharedPrefManager = SharedPrefManager(requireContext())

        binding.imageView.setOnViewTapListener { vw, x, y ->
            when {
                x <= 200 -> goBack()
                x >= 650 -> getNextComic()
            }
        }

        binding.shareBT.setOnClickListener {
            if (!isLoading) {
                shareCurrentMeme()
            }
        }

        binding.deleteFavBT.setOnClickListener {
            if (!isLoading) {
                deleteMeme()
            }
        }

        binding.saveBT.setOnClickListener {
            if (!isLoading) {
                saveCurrentMeme()
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        if(FavMemesList.isEmpty()) {
            FavMemesList = mapToList(sharedPrefManager.getAllMemes())
        }

        if(FavMemesList.isNotEmpty()){
            if (FavMemesList == mapToList(sharedPrefManager.getAllMemes())) {
                if (FavMemesFirstRunFlag)
                    loadWithGlide(FavMemesList[FavMemesCurrentMemeIndex], binding.imageView)
                else {
                    FavMemesFirstRunFlag = true
                    getNextComic()
                }
            } else {
                FavMemesList = mapToList(sharedPrefManager.getAllMemes())
                if (FavMemesList.isNotEmpty()) {
                    FavMemesCurrentMemeIndex = 0
                    loadWithGlide(FavMemesList[FavMemesCurrentMemeIndex], binding.imageView)
                } else {
                    binding.imageView.setImageResource(R.drawable.holdup)
                    binding.progressBar.visibility = View.GONE
                }
            }
        } else {
            binding.imageView.setImageResource(R.drawable.holdup)
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun mapToList(map: Map<String, String>): ArrayList<String> {
        val flippedMezanineMapToList = flipMap(sharedPrefManager.getAllMemes()).toList()
        val sortedList = flippedMezanineMapToList.sortedByDescending { it.first }
        return sortedList.map { it.second } as ArrayList<String>
    }

    private fun deleteMeme() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete Meme")
        builder.setMessage("Are you sure you want to un-favourite this meme?🥺")
        builder.setPositiveButton("Delete") { dialog, _ ->
            val imageUrl = FavMemesList[FavMemesCurrentMemeIndex]
            sharedPrefManager.removeMeme(imageUrl)
            FavMemesList.removeAt(FavMemesCurrentMemeIndex)
            if (FavMemesCurrentMemeIndex > 0) FavMemesCurrentMemeIndex--
            if (FavMemesList.isNotEmpty()) {
                loadWithGlide(FavMemesList[FavMemesCurrentMemeIndex], binding.imageView)
            } else {
                binding.imageView.setImageResource(R.drawable.holdup)
            }
            Toast.makeText(requireContext(), "Meme Deleted", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun flipMap(map: Map<String, String>): Map<String, String> {
        return map.map { (k, v) -> v to k }.toMap()
    }

    private fun getNextComic() {
        if (FavMemesCurrentMemeIndex < FavMemesList.size - 1) {
            FavMemesCurrentMemeIndex++
            val imageUrl = FavMemesList[FavMemesCurrentMemeIndex]
            loadWithGlide(imageUrl, binding.imageView)
        } else {
            Toast.makeText(context, "No more memes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goBack() {
        if (FavMemesCurrentMemeIndex > 0) {
            FavMemesCurrentMemeIndex--
            val imageUrl = FavMemesList[FavMemesCurrentMemeIndex]
            loadWithGlide(imageUrl, binding.imageView)
        } else {
            Toast.makeText(context, "Empty Backstack", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCurrentMeme() {
        val drawable = binding.imageView.drawable
        if (drawable == null) {
            Toast.makeText(context, "No image to save", Toast.LENGTH_SHORT).show()
            return
        }

        val imageUrl = BackstackAndKeys.FavMemesList[FavMemesCurrentMemeIndex]
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

            val imageUrl = BackstackAndKeys.FavMemesList[FavMemesCurrentMemeIndex]
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
            binding.progressBar.visibility = View.VISIBLE
            Glide.with(this@FavouriteFragment)
                .load(imageUrl)
                .placeholder(R.drawable.reddit_waiting)
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
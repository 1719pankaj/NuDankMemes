package com.example.nudankmemes



import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.nudankmemes.databinding.ActivityMainBinding
import com.example.nudankmemes.fragments.ConfigFragment
import com.example.nudankmemes.fragments.FtmFragment
import com.example.nudankmemes.fragments.RedditFragment
import com.example.nudankmemes.fragments.SavedFragment
import com.example.nudankmemes.fragments.XKCDFragment


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        replaceFragment(XKCDFragment())
        binding.bottomNavigationView.background = null
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.xkcd -> replaceFragment(XKCDFragment())
                R.id.ftm -> replaceFragment(FtmFragment())
                R.id.saved -> replaceFragment(SavedFragment())
                R.id.dank_memes -> replaceFragment(RedditFragment())
                R.id.config -> replaceFragment(ConfigFragment())
            }
            true
        }

    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}
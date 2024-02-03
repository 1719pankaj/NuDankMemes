package com.example.nudankmemes.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nudankmemes.R
import com.example.nudankmemes.databinding.FragmentConfigBinding


class ConfigFragment : Fragment() {

    private lateinit var binding: FragmentConfigBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentConfigBinding.inflate(layoutInflater)
        val view = binding.root



        return view
    }

}
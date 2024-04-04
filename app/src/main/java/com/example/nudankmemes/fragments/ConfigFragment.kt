package com.example.nudankmemes.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.nudankmemes.databinding.FragmentConfigBinding
import com.example.nudankmemes.global.Functions.Companion.loadCompatModeSharedPrefs
import com.example.nudankmemes.global.Functions.Companion.updateCompatModeSharedPrefs
import com.example.nudankmemes.global.Variables.Companion.config_compatibility_mode_buttons


class ConfigFragment : Fragment() {

    private lateinit var binding: FragmentConfigBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentConfigBinding.inflate(layoutInflater)

        loadCompatModeSharedPrefs(requireActivity())

        if(config_compatibility_mode_buttons)
            binding.compatibilityModeSwitch.setChecked(true)
        else
            binding.compatibilityModeSwitch.setChecked(false)

        binding.compatibilityModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            config_compatibility_mode_buttons = isChecked
            updateCompatModeSharedPrefs(requireActivity())
        }

        binding.compatInfoIcon.setOnClickListener {
            val infoMessage = "If you are using an older device you might encounter issue with moving between memes. Enable Compatibility mode to fix the issue.\nNote that pinch to zoom may or may not work in Compat mode."
            showinfoDialog(infoMessage)
        }

        return binding.root
    }

    private fun showinfoDialog(infoMessage: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(infoMessage)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }


}
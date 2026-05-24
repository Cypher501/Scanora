package com.scanora.app

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import com.scanora.app.databinding.FragmentFirstBinding

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawerLayout = view.findViewById(R.id.drawerLayout)

        // Hamburger opens sidebar
        view.findViewById<Button>(R.id.menuButton).setOnClickListener {
            drawerLayout.openDrawer(view.findViewById(R.id.sidebarMenu))
        }

        // Main scan button
        view.findViewById<Button>(R.id.scanButton).setOnClickListener {
            startActivity(Intent(requireContext(), MainActivity::class.java))
        }

        // Sidebar items
        view.findViewById<TextView>(R.id.menuScan).setOnClickListener {
            drawerLayout.closeDrawers()
            startActivity(Intent(requireContext(), MainActivity::class.java))
        }

        view.findViewById<TextView>(R.id.menuCamera).setOnClickListener {
            drawerLayout.closeDrawers()
            startActivity(Intent(requireContext(), CameraActivity::class.java))
        }

        view.findViewById<TextView>(R.id.menuFtp).setOnClickListener {
            drawerLayout.closeDrawers()
            startActivity(Intent(requireContext(), FtpActivity::class.java))
        }

        view.findViewById<TextView>(R.id.menuMirror).setOnClickListener {
            drawerLayout.closeDrawers()
            startActivity(Intent(requireContext(), MirrorActivity::class.java))
        }

        view.findViewById<TextView>(R.id.menuAbout).setOnClickListener {
            drawerLayout.closeDrawers()
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }

        view.findViewById<TextView>(R.id.menuExamCirculars).setOnClickListener {
            drawerLayout.closeDrawers()
            startActivity(Intent(requireContext(), ExamCircularsActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
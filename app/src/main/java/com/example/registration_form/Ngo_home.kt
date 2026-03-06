@file:Suppress("PrivatePropertyName")
package com.example.registration_form

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.drawerlayout.widget.DrawerLayout
import com.example.registration_form.databinding.ActivityNgoHomeBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

@Suppress("DEPRECATION")
class Ngo_home : AppCompatActivity() {
    private lateinit var binding: ActivityNgoHomeBinding
    private lateinit var drawerLayout: DrawerLayout

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNgoHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        hideSystemBars()

        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout
        val adapter = TabPagerAdapter(this)

        viewPager.adapter = adapter

        val tabTitles = listOf("Available Donations", "Accepted Donations")
        val tabIcons = listOf(R.drawable.ic_available, R.drawable.ic_accepted)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val customView = LayoutInflater.from(this).inflate(R.layout.custom_tab, tabLayout, false).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val tabContent = customView.findViewById<LinearLayout>(R.id.tab_content)
            val tabIcon = customView.findViewById<ImageView>(R.id.tab_icon)
            val tabText = customView.findViewById<TextView>(R.id.tab_text)

            tabIcon.setImageResource(tabIcons[position])
            tabText.text = tabTitles[position]

            val unselectedColor = ContextCompat.getColor(this, R.color.gray)
            tabText.setTextColor(unselectedColor)
            tabIcon.setColorFilter(unselectedColor)

            // Initial state - centered icon
            tabContent.post {
                val iconCenterX = tabIcon.left + tabIcon.width / 2
                val parentCenterX = tabContent.width / 2
                tabContent.translationX = (parentCenterX - iconCenterX).toFloat()
            }

            tab.customView = customView
        }.attach()

// Set initial selected tab
        tabLayout.getTabAt(0)?.let { tab ->
            tab.select()
            tab.customView?.let { tabView ->
                val tabText = tabView.findViewById<TextView>(R.id.tab_text)
                val tabIcon = tabView.findViewById<ImageView>(R.id.tab_icon)
                val tabContent = tabView.findViewById<LinearLayout>(R.id.tab_content)
                val selectedColor = ContextCompat.getColor(this, R.color.green_dark)

                tabText.setTextColor(selectedColor)
                tabIcon.setColorFilter(selectedColor)

                // Show text with animation
                tabText.visibility = View.VISIBLE
                tabText.alpha = 0f
                tabText.translationX = 20f

                // Calculate new position to keep content centered
                tabContent.post {
                    val newWidth = tabIcon.width + tabText.width + tabText.paddingStart
                    val translation = (tabContent.width - newWidth) / 2f

                    tabContent.animate()
                        .translationX(translation)
                        .setDuration(200)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()

                    tabText.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(200)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.customView?.let { tabView ->
                    val tabText = tabView.findViewById<TextView>(R.id.tab_text)
                    val tabIcon = tabView.findViewById<ImageView>(R.id.tab_icon)
                    val tabContent = tabView.findViewById<LinearLayout>(R.id.tab_content)
                    val selectedColor = ContextCompat.getColor(this@Ngo_home, R.color.green_dark)

                    tabText.setTextColor(selectedColor)
                    tabIcon.setColorFilter(selectedColor)

                    tabText.visibility = View.VISIBLE
                    tabText.alpha = 0f
                    tabText.translationX = 20f

                    tabContent.post {
                        val newWidth = tabIcon.width + tabText.width + tabText.paddingStart
                        val translation = (tabContent.width - newWidth) / 2f

                        tabContent.animate()
                            .translationX(translation)
                            .setDuration(200)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()

                        tabText.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(200)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.customView?.let { tabView ->
                    val tabText = tabView.findViewById<TextView>(R.id.tab_text)
                    val tabIcon = tabView.findViewById<ImageView>(R.id.tab_icon)
                    val tabContent = tabView.findViewById<LinearLayout>(R.id.tab_content)
                    val unselectedColor = ContextCompat.getColor(this@Ngo_home, R.color.gray)

                    tabText.setTextColor(unselectedColor)
                    tabIcon.setColorFilter(unselectedColor)

                    tabContent.post {
                        val parentCenterX = tabContent.width / 2
                        val iconCenterX = tabIcon.left + tabIcon.width / 2
                        val targetTranslation = (parentCenterX - iconCenterX).toFloat()

                        tabContent.animate()
                            .translationX(targetTranslation)
                            .setDuration(200)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()

                        tabText.animate()
                            .alpha(0f)
                            .translationX(20f)
                            .setDuration(200)
                            .withEndAction {
                                tabText.visibility = View.INVISIBLE
                                tabText.translationX = 0f
                            }
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()
                    }
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Optional: handle tab reselection
            }
        })

        setDarkStatusBarIcons(window)
        setupViews()
        setupClickListeners()

    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setNavigationBarColor(android.graphics.Color.TRANSPARENT)
            window.insetsController?.let {
                it.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                ) // Dark icons
                it.hide(android.view.WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR // Dark icons
        }
    }

     fun showLoading(isLoading: Boolean) {
         binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
         binding.viewPager.visibility = if (isLoading) View.GONE else View.VISIBLE
    }


    private fun setDarkStatusBarIcons(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }

    private fun setupViews() {
        window.statusBarColor = getColor(R.color.green_dark)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupClickListeners() {
        binding.menuIcon.setOnClickListener {
            showAnchoredPopupMenu(binding.menuIcon)
        }

        binding.settingsIcon.setOnClickListener {
            startActivity(Intent(this, Settings::class.java))
        }
    }

    @SuppressLint("UseKtx", "InflateParams")
    private fun showAnchoredPopupMenu(anchor: View) {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.floating_menu_dialog, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popupWindow.elevation = 16f

        popupView.findViewById<TextView>(R.id.profile).setOnClickListener {
            startActivity(Intent(this, MyProfileActivity::class.java))
            popupWindow.dismiss()
        }

        popupView.findViewById<TextView>(R.id.logout).setOnClickListener {
            Toast.makeText(this, "Logout Successful", Toast.LENGTH_SHORT).show()
            getSharedPreferences("MyPrefs", MODE_PRIVATE).edit().clear().apply()

            val intent = Intent(this, Launch::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchor, 0, 20)
    }

    fun refreshAcceptedTab() {
        val frag = supportFragmentManager.findFragmentByTag("AcceptedDonationFragment")
        if (frag is AcceptedDonationFragment) {
            frag.fetchAcceptedDonations()
        }
    }
    fun refreshAvailableTab() {
        val fragment = supportFragmentManager.findFragmentByTag("AvailableDonationsFragment")
        if (fragment is AvailableDonationsFragment) {
            fragment.fetchDonations()
        }
    }


}
package com.example.registration_form

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.registration_form.Home_Fragments.DonationDialogFragment
import com.example.registration_form.Home_Fragments.MyDonationsFragment
import com.example.registration_form.databinding.ActivityHomeBinding
import com.exyte.animatednavbar.AnimatedNavigationBar
import com.exyte.animatednavbar.animation.balltrajectory.Straight
import com.exyte.animatednavbar.animation.indendshape.Height
import com.exyte.animatednavbar.animation.indendshape.shapeCornerRadius
import com.exyte.animatednavbar.utils.noRippleClickable
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class Home : AppCompatActivity() {

    private var isLoadingDonations by mutableStateOf(false)

    private val viewModel: SharedViewModel by viewModels()
    private lateinit var binding: ActivityHomeBinding
    private lateinit var drawerLayout: DrawerLayout

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)


        drawerLayout = binding.drawerLayout

        setDarkStatusBarIcons(window)
        setupComposeBottomBar()
        setupViews()
        setupAnimations()
        setupClickListeners()
        hideSystemBars()

        lifecycleScope.launch {
            delay(1500)
            animateNumber(binding.mealsCount, 128)
            delay(1500)
            animateNumber(binding.restaurantsCount, 42)
            delay(1500)
            animateNumber(binding.ngosCount, 18)
        }

        binding.statsCard1.setOnClickListener { animateNumber(binding.mealsCount, 128) }
        binding.statsCard2.setOnClickListener { animateNumber(binding.restaurantsCount, 42) }
        binding.statsCard3.setOnClickListener { animateNumber(binding.ngosCount, 18) }

    }

    @Composable
    private fun BottomBarContent(
        onHomeClick: () -> Unit,
        onDonationsClick: () -> Unit
    ) {
        val navigationItems = listOf(
            NavigationItem(Icons.Default.Home, "Home"),
            NavigationItem(Icons.Default.Favorite, "My Donations")
        )

        var selectedIndex by remember { mutableStateOf(0) }

        Box(
            modifier = Modifier.height(64.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedNavigationBar(
                modifier = Modifier
                    .width(435.dp)
                    .height(64.dp),
                selectedIndex = selectedIndex,
                cornerRadius = shapeCornerRadius(30.dp),
                barColor = Color(0xFF2E7D32),
                ballColor = Color(0xFF2E7D32),
                ballAnimation = Straight(animationSpec = tween(500)),
                indentAnimation = Height(animationSpec = tween(500))
            ) {
                navigationItems.forEachIndexed { index, item ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .noRippleClickable {
                                if (!isLoadingDonations && selectedIndex != index) {
                                    selectedIndex = index
                                    when (index) {
                                        0 -> onHomeClick()
                                        1 -> {
                                            isLoadingDonations = true
                                            onDonationsClick()
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp),
                                tint = if (selectedIndex == index) Color.White else Color.LightGray
                            )

                            AnimatedVisibility(
                                visible = selectedIndex == index,
                                enter = fadeIn(tween(delayMillis = 200)) + expandHorizontally(tween(200)),
                                exit = fadeOut(tween(180)) + shrinkHorizontally(tween(180))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.label,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private fun setupComposeBottomBar() {
        val composeView = findViewById<ComposeView>(R.id.composeBottomBar)

        composeView.setContent {
            MaterialTheme {
                // Shared loading flag
                var isDonationsLoading by remember { mutableStateOf(false) }

                BottomBarContent(
                    onHomeClick = {
                        if (!isDonationsLoading) showHomeContent()
                    },
                    onDonationsClick = {
                        if (!isDonationsLoading) {
                            isDonationsLoading = true
                            showDonationsFragment {
                                isDonationsLoading = false
                            }
                        }
                    }
                )
            }
        }
    }


    data class NavigationItem(
        val icon: ImageVector,
        val label: String
    )

    private fun showHomeContent() {
        binding.fragmentContainer.visibility = View.GONE
        binding.nestedScrollView.visibility = View.VISIBLE
        binding.fab.visibility = View.VISIBLE

        findViewById<ImageButton>(R.id.fabMenu).visibility = View.VISIBLE
        findViewById<ImageButton>(R.id.fabSettings).visibility = View.VISIBLE
    }

    private fun showDonationsFragment(onLoaded: () -> Unit) {
        binding.nestedScrollView.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        findViewById<ImageButton>(R.id.fabMenu).visibility = View.GONE
        findViewById<ImageButton>(R.id.fabSettings).visibility = View.GONE

        fetchDataAndShowDonationsFragment {
            onLoaded()
        }
    }


    private fun fetchDataAndShowDonationsFragment(onLoaded: () -> Unit) {
        val dbRef = FirebaseDatabase.getInstance().getReference("donations")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isLoadingDonations = false
                binding.progressBar.visibility = View.GONE

                val fragment = supportFragmentManager.findFragmentByTag("MyDonationsFragment")
                    ?: MyDonationsFragment()

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment, "MyDonationsFragment")
                    .commit()

                supportFragmentManager.executePendingTransactions()

                val donationFragment = supportFragmentManager.findFragmentByTag("MyDonationsFragment") as? MyDonationsFragment
                donationFragment?.triggerLayoutAnimation()

                binding.fragmentContainer.visibility = View.VISIBLE
                binding.fragmentContainer.translationX = 0f

                onLoaded()
            }

            override fun onCancelled(error: DatabaseError) {
                isLoadingDonations = false
                binding.progressBar.visibility = View.GONE
                onLoaded()
            }
        })
    }


    private fun animateNumber(textView: TextView, target: Int) {
        val animator = ValueAnimator.ofInt(1, target)
        animator.duration = 1500
        animator.addUpdateListener {
            val animatedValue = it.animatedValue as Int
            textView.text = animatedValue.toString()
        }
        animator.start()
    }

    fun setDarkStatusBarIcons(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }

    private fun setupClickListeners() {
        // Existing FAB (Donate)
        binding.fab.setOnClickListener {
            DonationDialogFragment().show(supportFragmentManager, "DonationDialog")
        }

        // Menu Button
        findViewById<ImageButton>(R.id.fabMenu).setOnClickListener {
            showAnchoredPopupMenu(it)
        }

        // Settings Button
        findViewById<ImageButton>(R.id.fabSettings).setOnClickListener {
            startActivity(Intent(this, Settings::class.java))
        }



        binding.donateButton.setOnClickListener {
            binding.nestedScrollView.smoothScrollTo(0, binding.howItWorksTitle.top)
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

        popupWindow.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        popupWindow.elevation = 16f

        if (anchor is ImageButton) {
            anchor.backgroundTintList = getColorStateList(R.color.green)
        }


        popupView.findViewById<TextView>(R.id.profile).setOnClickListener {
            startActivity(Intent(this, MyProfileActivity::class.java))
            popupWindow.dismiss()
        }

        popupView.findViewById<TextView>(R.id.logout).setOnClickListener {
            val sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            sharedPref.edit().clear().apply()

            val intent = Intent(this, Launch::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            popupWindow.dismiss()
        }

        popupWindow.setOnDismissListener {
            if (anchor is ImageButton) {
                anchor.backgroundTintList = getColorStateList(R.color.green_dark)
            }
        }

        popupWindow.showAsDropDown(anchor, 30, -20)
    }

    private fun setupViews() {
        window.statusBarColor = android.graphics.Color.WHITE

        // Animate stats cards
        listOf(binding.statsCard1, binding.statsCard2, binding.statsCard3).forEach { card ->
            card.postDelayed({ card.isVisible = true }, 100)
        }

        // Animate "How It Works" cards
        listOf(binding.howItWorksCard1, binding.howItWorksCard2, binding.howItWorksCard3).forEachIndexed { index, card ->
            card.postDelayed({ card.isVisible = true }, 200 * (index + 1).toLong())
        }
    }

    private fun setupAnimations() {
        val floatAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.statsCard1.startAnimation(floatAnimation)
        binding.statsCard2.startAnimation(floatAnimation.apply { startOffset = 150 })
        binding.statsCard3.startAnimation(floatAnimation.apply { startOffset = 300 })
        binding.heroCard.animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
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

}
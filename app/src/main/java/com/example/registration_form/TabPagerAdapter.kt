package com.example.registration_form

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TabPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AvailableDonationsFragment()
            1 -> AcceptedDonationFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
}
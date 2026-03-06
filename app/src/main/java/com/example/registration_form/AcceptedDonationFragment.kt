package com.example.registration_form

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AcceptedDonationFragment : Fragment() {

    private lateinit var dbRef: DatabaseReference
    private lateinit var adapter: NgoDonationAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: PullDownSwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_accepted_donation, container, false)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView = view.findViewById(R.id.recyclerView)

        swipeRefreshLayout.setColorSchemeResources(R.color.green_dark)
        swipeRefreshLayout.setProgressViewOffset(true, -100, 20)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutAnimation =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_float_up)

        adapter = NgoDonationAdapter(
            isNgo = true,
            isAcceptedTab = true,
            onAccept = {},
            onDeny = {},
            onStatusChanged = {
                fetchAcceptedDonations()
                (activity as? Ngo_home)?.refreshAvailableTab()
            }
        )
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            fetchAcceptedDonations()
            recyclerView.scheduleLayoutAnimation()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                swipeRefreshLayout.isEnabled = !rv.canScrollVertically(-1)
            }
        })

        fetchAcceptedDonations()
        return view
    }

    fun fetchAcceptedDonations() {
        dbRef = FirebaseDatabase.getInstance().getReference("donations")

        (activity as? Ngo_home)?.showLoading(true)
        swipeRefreshLayout.isRefreshing = true

        val startTime = System.currentTimeMillis()

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val acceptedList = mutableListOf<Donation>()
                for (donationSnap in snapshot.children) {
                    val donation = donationSnap.getValue(Donation::class.java)
                    if (donation?.status == "ACCEPTED") {
                        acceptedList.add(donation)
                    }
                }

                val sortedList = acceptedList.sortedByDescending { it.date }
                adapter.submitList(sortedList)

                recyclerView.scheduleLayoutAnimation()

                val elapsed = System.currentTimeMillis() - startTime
                val delay = (1200L - elapsed).coerceAtLeast(0L)
                Handler(Looper.getMainLooper()).postDelayed({
                    swipeRefreshLayout.isRefreshing = false
                    (activity as? Ngo_home)?.showLoading(false)
                }, delay)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
                (activity as? Ngo_home)?.showLoading(false)
            }
        })
    }
}

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class AvailableDonationsFragment : Fragment() {

    private lateinit var dbRef: DatabaseReference
    private lateinit var adapter: NgoDonationAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_available_donations, container, false)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView = view.findViewById(R.id.recyclerView)

        swipeRefreshLayout.setColorSchemeResources(R.color.green_dark)
        swipeRefreshLayout.setProgressViewOffset(true, -100, 20)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NgoDonationAdapter(
            isNgo = true,
            onAccept = { donation -> updateStatus(donation, "ACCEPTED") },
            onDeny = { donation -> updateStatus(donation, "DENIED") },
            onStatusChanged = {
                fetchDonations()
                (activity as? Ngo_home)?.refreshAcceptedTab()
            }
        )
        recyclerView.adapter = adapter

        // Enable swipe only when RecyclerView is at top
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                swipeRefreshLayout.isEnabled = !rv.canScrollVertically(-1)
            }
        })

        swipeRefreshLayout.setOnRefreshListener {
            fetchDonations()
        }

        fetchDonations()
        return view
    }

    fun fetchDonations() {
        dbRef = FirebaseDatabase.getInstance().getReference("donations")

        val startTime = System.currentTimeMillis()
        (activity as? Ngo_home)?.showLoading(true)
        swipeRefreshLayout.isRefreshing = true

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val donationList = mutableListOf<Donation>()
                val rawDonations = snapshot.children.mapNotNull { it.getValue(Donation::class.java) }
                    .filter { it.status == "PENDING" }

                val donationsWithUid = rawDonations.filter { !it.createdBy.isNullOrEmpty() }
                val uids = donationsWithUid.mapNotNull { it.createdBy }.distinct().take(10) // cap to 10 for safety

                if (uids.isEmpty()) {
                    finishLoading(rawDonations.sortedByDescending { it.date }, startTime)
                    return
                }

                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                firestore.collection("users")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), uids)
                    .get()
                    .addOnSuccessListener { result ->
                        val nameMap = result.associate { it.id to (it.getString("name") ?: "Unknown Restaurant") }

                        val finalList = rawDonations.map {
                            val name = nameMap[it.createdBy] ?: it.restaurantName ?: "Unknown Restaurant"
                            it.copy(restaurantName = name)
                        }

                        finishLoading(finalList.sortedByDescending { it.date }, startTime)
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Couldn't load restaurant names", Toast.LENGTH_SHORT).show()
                        finishLoading(rawDonations.sortedByDescending { it.date }, startTime)
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
                (activity as? Ngo_home)?.showLoading(false)
            }
        })
    }


    private fun finishLoading(list: List<Donation>, startTime: Long) {
        val delay = (1200L - (System.currentTimeMillis() - startTime)).coerceAtLeast(0L)
        Handler(Looper.getMainLooper()).postDelayed({
            adapter.submitList(list)
            recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(
                requireContext(), R.anim.layout_animation_float_up
            )
            recyclerView.scheduleLayoutAnimation()
            swipeRefreshLayout.isRefreshing = false
            (activity as? Ngo_home)?.showLoading(false)
        }, delay)
    }


    private fun updateStatus(donation: Donation, newStatus: String) {
        val donationId = donation.id
        if (donationId.isNullOrEmpty()) return

        FirebaseDatabase.getInstance()
            .getReference("donations")
            .child(donationId)
            .child("status")
            .setValue(newStatus)
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

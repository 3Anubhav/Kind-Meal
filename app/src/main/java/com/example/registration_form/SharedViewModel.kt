package com.example.registration_form

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SharedViewModel : ViewModel() {

    private val _donations = MutableLiveData<List<Donation>>()
    val donations: LiveData<List<Donation>> get() = _donations

    private val databaseRef: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("donations")

    private val donationsListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val donationList = mutableListOf<Donation>()
            for (child in snapshot.children) {
                val donation = child.getValue(Donation::class.java)
                if (donation != null) {
                    donationList.add(donation)
                }
            }
            _donations.postValue(
                donationList.sortedByDescending { it.date }
            )
        }

        override fun onCancelled(error: DatabaseError) {
            // Log or handle the error as needed
        }
    }

    init {
        // Attach listener for live updates
        databaseRef.addValueEventListener(donationsListener)
    }

    /**
     * Manually fetch donations once (used for pull-to-refresh)
     */
    fun fetchDonations() {
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val donationList = mutableListOf<Donation>()
                for (child in snapshot.children) {
                    val donation = child.getValue(Donation::class.java)
                    if (donation != null) {
                        donationList.add(donation)
                    }
                }
                _donations.postValue(
                    donationList.sortedByDescending { it.date }
                )
            }

            override fun onCancelled(error: DatabaseError) {
                // Optional: handle error during refresh
            }
        })
    }

    /**
     * Add a new donation
     */
    fun addDonation(donation: Donation) {
        val donationId = databaseRef.push().key ?: return
        val newDonation = donation.copy(id = donationId)
        databaseRef.child(donationId).setValue(newDonation)
    }

    /**
     * Update donation status (e.g., from pending to accepted/denied)
     */
    fun updateDonationStatus(donationId: String, newStatus: String) {
        databaseRef.child(donationId).child("status").setValue(newStatus)
    }

    override fun onCleared() {
        super.onCleared()
        databaseRef.removeEventListener(donationsListener)
    }
}

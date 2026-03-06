package com.example.registration_form.Home_Fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.registration_form.Donation
import com.example.registration_form.R
import com.example.registration_form.SharedViewModel
import com.example.registration_form.databinding.FragmentMyDonationsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyDonationsFragment : Fragment() {

    private var _binding: FragmentMyDonationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SharedViewModel by activityViewModels()

    private lateinit var donationsAdapter: DonationsAdapter
    private val donationsList = mutableListOf<Donation>()

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyDonationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        observeDonations()
    }

    private fun setupRecyclerView() {
        donationsAdapter = DonationsAdapter(donationsList)

        val controller = AnimationUtils.loadLayoutAnimation(
            requireContext(),
            R.anim.layout_animation_fade_up
        )

        binding.donationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = donationsAdapter
            layoutAnimation = controller
            itemAnimator = null
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(
            R.color.green_dark,
            android.R.color.holo_orange_light
        )

        // Trigger data fetch when swiped
        swipeRefreshLayout.setOnRefreshListener {
            refreshDonations()
        }

        // Enable swipe only when at top of RecyclerView
        binding.donationsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                swipeRefreshLayout.isEnabled = !recyclerView.canScrollVertically(-1)
            }
        })
    }

    private fun refreshDonations() {
        // Start spinner
        swipeRefreshLayout.isRefreshing = true

        // Trigger actual LiveData refresh logic
        viewModel.fetchDonations()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeDonations() {
        viewModel.donations.observe(viewLifecycleOwner) { donations ->
            donations?.let {
                donationsList.clear()
                donationsList.addAll(it)
                donationsAdapter.notifyDataSetChanged()
                binding.donationsRecyclerView.scheduleLayoutAnimation()
                updateUI(it.isEmpty())
                Log.d("Donations", "Updated list: ${it.size} items")
            }
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun updateUI(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.donationsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class DonationsAdapter(private val donations: List<Donation>) :
        RecyclerView.Adapter<DonationsAdapter.DonationViewHolder>() {

        inner class DonationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val foodIcon: ImageView = itemView.findViewById(R.id.food_icon)
            val foodType: TextView = itemView.findViewById(R.id.food_type)
            val quantity: TextView = itemView.findViewById(R.id.tvQuantity)
            val date: TextView = itemView.findViewById(R.id.date)
            val title: TextView = itemView.findViewById(R.id.tvTitle)
            val description: TextView = itemView.findViewById(R.id.tvDescription)
            val status: TextView = itemView.findViewById(R.id.tvStatus)
            val cardView: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardView) // Change to CardView type
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_donation, parent, false)
            return DonationViewHolder(view)
        }

        override fun onBindViewHolder(holder: DonationViewHolder, position: Int) {
            val donation = donations[position]

            // Use setCardBackgroundColor instead of setBackgroundColor
            when (donation.status) {
                "ACCEPTED" -> holder.cardView.setCardBackgroundColor(Color.parseColor("#FBFFFA"))
                "PENDING" -> holder.cardView.setCardBackgroundColor(Color.parseColor("#FFFDF5"))
                "DENIED" -> holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF5F5"))
                else -> holder.cardView.setCardBackgroundColor(Color.WHITE)
            }

            holder.foodType.text = donation.foodType
            holder.quantity.text = "Servings: ${donation.quantity}"
            holder.date.text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(donation.date))
            holder.description.text = donation.description

            when (donation.status) {
                "ACCEPTED" -> {
                    holder.status.text = "Accepted by ${donation.acceptedBy}"
                    holder.status.setTextColor(requireContext().getColor(R.color.green))
                }
                "DENIED" -> {
                    holder.status.text = "DENIED"
                    holder.status.setTextColor(requireContext().getColor(R.color.error_red))
                }
                else -> {
                    holder.status.text = donation.status
                    holder.status.setTextColor(requireContext().getColor(R.color.default_status_color))
                }
            }

            holder.foodIcon.setImageResource(getFoodIcon(donation.foodType))

            // Hide addedBy text
            val addedByText: TextView = holder.itemView.findViewById(R.id.tvAddedBy)
            addedByText.visibility = View.GONE
        }

        private fun getFoodIcon(foodType: String): Int {
            return when (foodType.lowercase(Locale.ROOT)) {
                "vegetables", "fruits", "grains", "dairy" -> R.drawable.ic_food
                else -> R.drawable.ic_food
            }
        }

        override fun getItemCount() = donations.size
    }

    fun triggerLayoutAnimation() {
        binding.donationsRecyclerView.scheduleLayoutAnimation()
    }
}
package com.codingwithtashi.dailyprayer.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.databinding.FragmentAudioPlayerBinding
import com.codingwithtashi.dailyprayer.adapter.AudioPlaylistAdapter
import com.codingwithtashi.dailyprayer.viewmodel.AudioDownloadViewModel
import com.codingwithtashi.dailyprayer.viewmodel.AudioViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class AudioPlayerFragment : Fragment() {

    companion object {
        private const val TAG = "AudioPlayerFragment"
    }

    private var _binding: FragmentAudioPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AudioViewModel by viewModels()
    private val downloadViewModel: AudioDownloadViewModel by viewModels()
    private lateinit var playlistAdapter: AudioPlaylistAdapter

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
        } else {
            Log.w(TAG, "Notification permission denied")
            Toast.makeText(
                requireContext(),
                "Notification permission denied. You won't see playback controls.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Log.d(TAG, "Fragment created")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioPlayerBinding.inflate(inflater, container, false)
        Log.d(TAG, "View created")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "View created - setting up UI")

        requestNotificationPermission()
        setupRecyclerView()
        setupControls()
        observeViewModel()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Notification permission already granted")
                }
                else -> {
                    Log.d(TAG, "Requesting notification permission")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.audio_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_download_all -> {
                Log.d(TAG, "Download all clicked")
                downloadViewModel.downloadAllPrayers()
                Toast.makeText(requireContext(), "Downloading all prayers...", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_clear_cache -> {
                Log.d(TAG, "Clear cache clicked")
                downloadViewModel.clearAllCache()
                Toast.makeText(requireContext(), "Cache cleared", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")

        playlistAdapter = AudioPlaylistAdapter(
            onPrayerClick = { prayer, position ->
                Log.d(TAG, "Prayer clicked: ${prayer.title} at position $position")
                Log.d(TAG, "Prayer ID: ${prayer.id}, URL: ${prayer.downloadUrl}")

                // Check if service is bound
                if (viewModel.isServiceBound()) {
                    viewModel.playPrayerAtIndex(position)
                    Toast.makeText(requireContext(), "Playing: ${prayer.title}", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Service not bound!")
                    Toast.makeText(requireContext(), "Audio service not ready, please wait...", Toast.LENGTH_SHORT).show()
                }
            },
            onDownloadClick = { prayer ->
                Log.d(TAG, "Download clicked for: ${prayer.title}")
                downloadViewModel.downloadPrayerAudio(prayer)
            },
            isCachedChecker = { prayer ->
                downloadViewModel.isCached(prayer)
            }
        )

        binding.recyclerViewPlaylist.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = playlistAdapter
            Log.d(TAG, "RecyclerView configured")
        }
    }

    private fun setupControls() {
        Log.d(TAG, "Setting up controls")

        binding.btnPlayPause.setOnClickListener {
            Log.d(TAG, "Play/Pause button clicked")
            if (viewModel.isPlaying.value == true) {
                Log.d(TAG, "Pausing")
                viewModel.pause()
            } else {
                Log.d(TAG, "Playing")
                viewModel.play()
            }
        }

        binding.btnNext.setOnClickListener {
            Log.d(TAG, "Next button clicked")
            viewModel.playNext()
        }

        binding.btnPrevious.setOnClickListener {
            Log.d(TAG, "Previous button clicked")
            viewModel.playPrevious()
        }

        binding.btnRepeat.setOnClickListener {
            Log.d(TAG, "Repeat button clicked")
            viewModel.toggleRepeat()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    Log.d(TAG, "Seek to: $progress")
                    viewModel.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Log.d(TAG, "Started seeking")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Log.d(TAG, "Stopped seeking")
            }
        })
    }

    private fun observeViewModel() {
        Log.d(TAG, "Setting up observers")

        // Observe prayers from database
        viewModel.prayersWithAudio.observe(viewLifecycleOwner) { prayers ->
            Log.d(TAG, "Prayers received: ${prayers.size} items")

            if (prayers.isNotEmpty()) {
                prayers.forEachIndexed { index, prayer ->
                    Log.d(TAG, "Prayer $index: ${prayer.title}, URL: ${prayer.downloadUrl}")
                }
                val filteredPrayers = prayers.filter { it.downloadUrl.isNotEmpty() }

                // Set playlist in viewModel
                viewModel.setPlaylist(filteredPrayers)

                // Update adapter
                playlistAdapter.submitList(filteredPrayers)

                binding.txtNoAudio.visibility = View.GONE
                binding.recyclerViewPlaylist.visibility = View.VISIBLE

                Log.d(TAG, "Playlist set with ${prayers.size} prayers")
            } else {
                Log.w(TAG, "No prayers with audio found!")
                binding.txtNoAudio.visibility = View.VISIBLE
                binding.recyclerViewPlaylist.visibility = View.GONE
            }
        }

        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            Log.d(TAG, "Playback state changed: $isPlaying")
            binding.btnPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        viewModel.currentPrayer.observe(viewLifecycleOwner) { prayer ->
            if (prayer != null) {
                Log.d(TAG, "Current prayer changed: ${prayer.title}")
                binding.txtPrayerTitle.text = prayer.title
                playlistAdapter.setCurrentPlaying(prayer.id!!)
            } else {
                Log.d(TAG, "No current prayer")
                binding.txtPrayerTitle.text = "Select a prayer to play"
            }
        }

        viewModel.currentPosition.observe(viewLifecycleOwner) { position ->
            binding.seekBar.progress = position
            binding.txtCurrentTime.text = formatTime(position)
        }

        viewModel.duration.observe(viewLifecycleOwner) { duration ->
            binding.seekBar.max = duration
            binding.txtDuration.text = formatTime(duration)
        }

        viewModel.isRepeatMode.observe(viewLifecycleOwner) { isRepeat ->
            Log.d(TAG, "Repeat mode changed: $isRepeat")
            binding.btnRepeat.setImageResource(
                if (isRepeat) R.drawable.ic_repeat_on else R.drawable.ic_repeat
            )
        }

        viewModel.playlist.observe(viewLifecycleOwner) { prayers ->
            Log.d(TAG, "Playlist updated: ${prayers.size} prayers")
        }

        viewModel.currentIndex.observe(viewLifecycleOwner) { index ->
            Log.d(TAG, "Current index: $index")
            if (index >= 0 && index < playlistAdapter.itemCount) {
                binding.recyclerViewPlaylist.scrollToPosition(index)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e(TAG, "Error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Download progress
        downloadViewModel.downloadProgress.observe(viewLifecycleOwner) { progress ->
            Log.d(TAG, "Download progress: ${progress.message}")
            Toast.makeText(requireContext(), progress.message, Toast.LENGTH_SHORT).show()
            playlistAdapter.notifyDataSetChanged()
        }

        downloadViewModel.cacheSize.observe(viewLifecycleOwner) { size ->
            Log.d(TAG, "Cache size: $size")
            activity?.invalidateOptionsMenu()
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "Fragment started - binding service")
        viewModel.bindService(requireContext())
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "Fragment stopped - unbinding service")
        viewModel.unbindService(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "View destroyed")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Fragment destroyed")
    }
}
package io.fastpix.app.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import io.fastpix.app.databinding.ActivityVideoListScreenBinding
import java.util.UUID

class VideoListScreen : AppCompatActivity() {
    private val binding: ActivityVideoListScreenBinding by lazy { ActivityVideoListScreenBinding.inflate(layoutInflater) }
    private val videoAdapter by lazy {
        VideoAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        actionBar?.hide()
        supportActionBar?.hide()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        videoAdapter.passDataToAdapter(dummyData)
        binding.recyclerView.adapter = videoAdapter

        videoAdapter.onVideoClick = { video ->
            val intent = Intent(this, VideoPlayerActivity::class.java)
            intent.putExtra("video_model", video)
            val index = dummyData.indexOfFirst { it.id == video?.id }
            intent.putExtra("current_index", if (index >= 0) index else 0)
            intent.putParcelableArrayListExtra("video_list", ArrayList(dummyData))
            startActivity(intent)
        }
    }


    companion object {
        val viewerId = UUID.randomUUID().toString()
    }
}
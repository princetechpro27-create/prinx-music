package com.prinx.music

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

data class Song(val id: Long, val title: String, val artist: String, val duration: Long, val uri: String)

class MainActivity : ComponentActivity() {

    private lateinit var player: ExoPlayer
    private var songsState = mutableStateOf<List<Song>>(emptyList())
    private var currentSongState = mutableStateOf<Song?>(null)
    private var isPlayingState = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) loadLocalMusic()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()

        checkAndRequestPermissions()

        setContent {
            PrinxMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    var splashFinished by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        delay(2200) // Animation du splash screen
                        splashFinished = true
                    }

                    if (!splashFinished) {
                        SplashScreen()
                    } else {
                        MainScreen(
                            songs = songsState.value,
                            currentSong = currentSongState.value,
                            isPlaying = isPlayingState.value,
                            onSongClick = { song -> playSong(song) },
                            onPlayPauseClick = { togglePlayPause() }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadLocalMusic()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun loadLocalMusic() {
        val musicList = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Audio.Media.INTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )

        contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Titre inconnu"
                val artist = cursor.getString(artistColumn) ?: "Artiste inconnu"
                val duration = cursor.getLong(durationColumn)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()

                musicList.add(Song(id, title, artist, duration, uri))
            }
        }
        songsState.value = musicList
    }

    private fun playSong(song: Song) {
        currentSongState.value = song
        player.setMediaItem(MediaItem.fromUri(song.uri))
        player.prepare()
        player.play()
        isPlayingState.value = true
    }

    private fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
            isPlayingState.value = false
        } else {
            player.play()
            isPlayingState.value = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}

@Composable
fun PrinxMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF0A0A0A),
            primary = Color(0xFF0066FF),
            secondary = Color(0xFF00E5FF),
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "PRINX MUSIC",
                color = Color(0xFF00E5FF),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "YOUR MUSIC. YOUR UNIVERSE.",
                color = Color.Gray,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "Created by Prince",
                color = Color.LightGray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "~corcello~",
                color = Color(0xFF0066FF),
                fontSize = 24.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Cursive
            )
        }
    }
}

@Composable
fun MainScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "PRINX MUSIC",
            color = Color(0xFF00E5FF),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "Bibliothèque locale — Samsung Galaxy A14 Optimisé",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(songs) { song ->
                SongItem(song = song, onClick = { onSongClick(song) })
            }
        }

        if (currentSong != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = currentSong.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 14.sp)
                        Text(text = currentSong.artist, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                    }
                    Button(
                        onClick = onPlayPauseClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                    ) {
                        Text(text = if (isPlaying) "Pause" else "Lecture", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(song.duration)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(song.duration) % 60

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = song.artist, color = Color.Gray, fontSize = 12.sp)
        }
        Text(text = String.format("%d:%02d", minutes, seconds), color = Color(0xFF00E5FF), fontSize = 12.sp)
    }
}

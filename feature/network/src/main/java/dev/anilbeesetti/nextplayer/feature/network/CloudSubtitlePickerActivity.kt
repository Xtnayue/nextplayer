package dev.anilbeesetti.nextplayer.feature.network

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.anilbeesetti.nextplayer.core.data.repository.NetworkConnectionRepository
import dev.anilbeesetti.nextplayer.core.media.network.NetworkClient
import dev.anilbeesetti.nextplayer.core.media.network.NetworkClientFactory
import dev.anilbeesetti.nextplayer.core.media.network.isNetworkSubtitleFile
import dev.anilbeesetti.nextplayer.core.media.network.proxy.NetworkStreamingProxy
import dev.anilbeesetti.nextplayer.core.model.NetworkFile
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CloudSubtitlePickerActivity : ComponentActivity() {
    @Inject lateinit var connectionRepository: NetworkConnectionRepository
    @Inject lateinit var streamingProxy: NetworkStreamingProxy

    private var client: NetworkClient? = null
    private var files by mutableStateOf<List<NetworkFile>>(emptyList())
    private var loading by mutableStateOf(true)
    private var title by mutableStateOf("")
    private var currentPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val connectionId = intent.getLongExtra(EXTRA_CONNECTION_ID, -1L)
        currentPath = intent.getStringExtra(EXTRA_DIRECTORY_PATH).orEmpty()
        setContent {
            NextPlayerTheme(darkTheme = true) {
                PickerContent(
                    title = title,
                    files = files,
                    loading = loading,
                    onBack = ::navigateBack,
                    onClick = ::open,
                )
            }
        }
        lifecycleScope.launch {
            val connection = connectionRepository.getConnection(connectionId) ?: run {
                finish()
                return@launch
            }
            client = NetworkClientFactory.create(connection)
            load(currentPath.ifBlank { client?.rootPath.orEmpty() })
        }
    }

    private fun load(path: String) {
        val activeClient = client ?: return
        currentPath = path
        title = path.trimEnd('/').substringAfterLast('/').ifBlank { getString(R.string.network) }
        loading = true
        lifecycleScope.launch {
            if (!activeClient.isConnected()) activeClient.connect().getOrThrow()
            files = activeClient.listFiles(path).getOrThrow()
                .filter { it.isDirectory || isNetworkSubtitleFile(it.name) }
                .sortedWith(compareByDescending<NetworkFile> { it.isDirectory }.thenBy { it.name.lowercase() })
            loading = false
        }
    }

    private fun open(file: NetworkFile) {
        if (file.isDirectory) {
            load(file.path)
        } else {
            val connectionId = intent.getLongExtra(EXTRA_CONNECTION_ID, -1L)
            lifecycleScope.launch {
                val connection = connectionRepository.getConnection(connectionId) ?: return@launch
                val uri = streamingProxy.registerStream(
                    connection = connection,
                    filePath = file.path,
                    fileName = file.name,
                    keepExistingStreams = true,
                ).toUri()
                setResult(Activity.RESULT_OK, Intent().setData(uri))
                finish()
            }
        }
    }

    private fun navigateBack() {
        val parent = currentPath.trimEnd('/').substringBeforeLast('/', "")
        if (parent.isBlank()) finish() else load(parent)
    }

    override fun onDestroy() {
        client?.let { activeClient -> lifecycleScope.launch { runCatching { activeClient.disconnect() } } }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CONNECTION_ID = "connection_id"
        const val EXTRA_DIRECTORY_PATH = "directory_path"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun PickerContent(
    title: String,
    files: List<NetworkFile>,
    loading: Boolean,
    onBack: () -> Unit,
    onClick: (NetworkFile) -> Unit,
) {
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = title,
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(NextIcons.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(files, key = { it.path }) { file ->
                    ListItem(
                        headlineContent = { Text(file.name) },
                        modifier = Modifier.clickable { onClick(file) },
                    )
                }
            }
        }
    }
}

package it.fast4x.rimusic.ui.screens.playlist

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import com.valentinilk.shimmer.shimmer
import it.fast4x.compose.persist.persist
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.requests.playlistPage
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.PopupType
import it.fast4x.rimusic.enums.ThumbnailRoundness
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.models.Playlist
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.models.SongPlaylistMap
import it.fast4x.rimusic.query
import it.fast4x.rimusic.service.isLocal
import it.fast4x.rimusic.transaction
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.ShimmerHost
import it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.rimusic.ui.components.themed.HeaderIconButton
import it.fast4x.rimusic.ui.components.themed.HeaderPlaceholder
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.ui.components.themed.InputTextDialog
import it.fast4x.rimusic.ui.components.themed.LayoutWithAdaptiveThumbnail
import it.fast4x.rimusic.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.rimusic.ui.components.themed.PlaylistsItemMenu
import it.fast4x.rimusic.ui.components.themed.SmartMessage
import it.fast4x.rimusic.ui.components.themed.adaptiveThumbnailContent
import it.fast4x.rimusic.ui.items.SongItem
import it.fast4x.rimusic.ui.items.SongItemPlaceholder
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.favoritesIcon
import it.fast4x.rimusic.ui.styling.px
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.completed
import it.fast4x.rimusic.utils.disableScrollingTextKey
import it.fast4x.rimusic.utils.downloadedStateMedia
import it.fast4x.rimusic.utils.durationTextToMillis
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.forcePlayAtIndex
import it.fast4x.rimusic.utils.forcePlayFromBeginning
import it.fast4x.rimusic.utils.formatAsTime
import it.fast4x.rimusic.utils.getDownloadState
import it.fast4x.rimusic.utils.isDownloadedSong
import it.fast4x.rimusic.utils.isLandscape
import it.fast4x.rimusic.utils.manageDownload
import it.fast4x.rimusic.utils.medium
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.secondary
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.utils.showFloatingIconKey
import it.fast4x.rimusic.utils.thumbnailRoundnessKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.knighthat.colorPalette
import me.knighthat.typography
import timber.log.Timber


@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun PlaylistSongList(
    navController: NavController,
    browseId: String,
    params: String?,
    maxDepth: Int?,
) {
    val binder = LocalPlayerServiceBinder.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current

    var playlistPage by persist<Innertube.PlaylistOrAlbumPage?>("playlist/$browseId/playlistPage")

    var filter: String? by rememberSaveable { mutableStateOf(null) }

    LaunchedEffect(Unit, filter) {
        if (playlistPage != null && playlistPage?.songsPage?.continuation == null) return@LaunchedEffect

        playlistPage = withContext(Dispatchers.IO) {
            Innertube.playlistPage(BrowseBody(browseId = browseId))?.completed()?.getOrNull()
        }

        /*
        playlistPage = withContext(Dispatchers.IO) {
            Innertube
                .playlistPage(BrowseBody(browseId = browseId, params = params))
                ?.completed()
                ?.getOrNull()
        }
         */
        //Log.d("mediaPlaylist", "${playlistPage?.title} songs ${playlistPage?.songsPage?.items?.size} continuation ${playlistPage?.songsPage?.continuation}")

/*
                playlistPage = withContext(Dispatchers.IO) {
                    Innertube.playlistPage(BrowseBody(browseId = browseId, params = params))
                        ?.completed(maxDepth = maxDepth ?: Int.MAX_VALUE)?.getOrNull()
                }
 */


/*
        playlistPage = withContext(Dispatchers.IO) {
            Innertube.playlistPage(BrowseBody(browseId = browseId))?.completed()?.getOrNull()
        }
*/
    }

    var filterCharSequence: CharSequence
    filterCharSequence = filter.toString()
    //Log.d("mediaItemFilter", "<${filter}>  <${filterCharSequence}>")
    if (!filter.isNullOrBlank())
        playlistPage?.songsPage?.items =
        playlistPage?.songsPage?.items?.filter {songItem ->
                songItem.asMediaItem.mediaMetadata.title?.contains(filterCharSequence,true) ?: false
                        || songItem.asMediaItem.mediaMetadata.artist?.contains(filterCharSequence,true) ?: false
            }

    var searching by rememberSaveable { mutableStateOf(false) }

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px

    var isImportingPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
/*
    var showAddPlaylistSelectDialog by remember {
        mutableStateOf(false)
    }

    val playlistPreviews by remember {
        Database.playlistPreviews(PlaylistSortBy.Name, SortOrder.Ascending)
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

    var showPlaylistSelectDialog by remember {
        mutableStateOf(false)
    }
 */

    var totalPlayTimes = 0L
    playlistPage?.songsPage?.items?.forEach {
        totalPlayTimes += it.durationText?.let { it1 ->
            durationTextToMillis(it1) }?.toLong() ?: 0
    }

    if (isImportingPlaylist) {
        InputTextDialog(
            onDismiss = { isImportingPlaylist = false },
            title = stringResource(R.string.enter_the_playlist_name),
            value = playlistPage?.title ?: "",
            placeholder = "https://........",
            setValue = { text ->
                query {
                    transaction {
                        val playlistId = Database.insert(Playlist(name = text, browseId = browseId))

                        playlistPage?.songsPage?.items
                            ?.map(Innertube.SongItem::asMediaItem)
                            ?.onEach(Database::insert)
                            ?.mapIndexed { index, mediaItem ->
                                SongPlaylistMap(
                                    songId = mediaItem.mediaId,
                                    playlistId = playlistId,
                                    position = index
                                )
                            }?.let(Database::insertSongPlaylistMaps)
                    }
                }
                SmartMessage(context.resources.getString(R.string.done), PopupType.Success, context = context)
            }
        )
    }

    var position by remember {
        mutableIntStateOf(0)
    }

    val headerContent: @Composable () -> Unit = {
        if (playlistPage == null) {
            HeaderPlaceholder(
                modifier = Modifier
                    .shimmer()
            )
        } else {

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                /*
                HeaderWithIcon(
                    title = playlistPage?.title ?: "Unknown",
                    iconId = R.drawable.playlist,
                    enabled = true,
                    showIcon = true,
                    modifier = Modifier
                        .padding(bottom = 8.dp),
                    onClick = {}
                ) //{
 */
            }



            //Header(title = playlistPage?.title ?: "Unknown") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            ) {
                /*
                HeaderInfo(
                    title = "${playlistPage?.songsPage?.items?.size}",
                    icon = painterResource(R.drawable.musical_notes),
                    spacer = 0
                )

                Spacer(
                    modifier = Modifier
                        .weight(1f)
                )
                */
                HeaderIconButton(
                    onClick = { searching = !searching },
                    icon = R.drawable.search_circle,
                    color = colorPalette().text,
                    iconSize = 24.dp
                )

                HeaderIconButton(
                    icon = R.drawable.downloaded,
                    color = colorPalette().text,
                    onClick = {},
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                downloadState = Download.STATE_DOWNLOADING
                                if (playlistPage?.songsPage?.items?.isNotEmpty() == true)
                                    playlistPage?.songsPage?.items?.forEach {
                                        binder?.cache?.removeResource(it.asMediaItem.mediaId)
                                        query {
                                            Database.resetFormatContentLength(it.asMediaItem.mediaId)
                                        }
                                        manageDownload(
                                            context = context,
                                            mediaItem = it.asMediaItem,
                                            downloadState = false
                                        )
                                    }
                            },
                            onLongClick = {
                                SmartMessage(context.resources.getString(R.string.info_download_all_songs), context = context)
                            }
                        )
                )

                HeaderIconButton(
                    icon = R.drawable.download,
                    color = colorPalette().text,
                    onClick = {},
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                downloadState = Download.STATE_DOWNLOADING
                                if (playlistPage?.songsPage?.items?.isNotEmpty() == true)
                                    playlistPage?.songsPage?.items?.forEach {
                                        binder?.cache?.removeResource(it.asMediaItem.mediaId)
                                        query {
                                            Database.resetFormatContentLength(it.asMediaItem.mediaId)
                                        }
                                        manageDownload(
                                            context = context,
                                            mediaItem = it.asMediaItem,
                                            downloadState = true
                                        )
                                    }
                            },
                            onLongClick = {
                                SmartMessage(context.resources.getString(R.string.info_remove_all_downloaded_songs), context = context)
                            }
                        )
                )



                HeaderIconButton(
                    icon = R.drawable.enqueue,
                    enabled = playlistPage?.songsPage?.items?.isNotEmpty() == true,
                    color =  if (playlistPage?.songsPage?.items?.isNotEmpty() == true) colorPalette().text else colorPalette().textDisabled,
                    onClick = {},
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                playlistPage?.songsPage?.items?.map(Innertube.SongItem::asMediaItem)?.let { mediaItems ->
                                    binder?.player?.enqueue(mediaItems, context)
                                }
                            },
                            onLongClick = {
                                SmartMessage(context.resources.getString(R.string.info_enqueue_songs), context = context)
                            }
                        )
                )

                HeaderIconButton(
                    icon = R.drawable.shuffle,
                    enabled = playlistPage?.songsPage?.items?.isNotEmpty() == true,
                    color = if (playlistPage?.songsPage?.items?.isNotEmpty() ==true) colorPalette().text else colorPalette().textDisabled,
                    onClick = {},
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                if (playlistPage?.songsPage?.items?.isNotEmpty() == true) {
                                    binder?.stopRadio()
                                    playlistPage?.songsPage?.items?.shuffled()?.map(Innertube.SongItem::asMediaItem)
                                        ?.let {
                                            binder?.player?.forcePlayFromBeginning(
                                                it
                                            )
                                        }
                                }
                            },
                            onLongClick = {
                                SmartMessage(context.resources.getString(R.string.info_shuffle), context = context)
                            }
                        )
                )

                HeaderIconButton(
                    icon = R.drawable.add_in_playlist,
                    color = colorPalette().text,
                    onClick = {},
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                menuState.display {
                                    PlaylistsItemMenu(
                                        navController = navController,
                                        modifier = Modifier.fillMaxHeight(0.4f),
                                        onDismiss = menuState::hide,
                                        onImportOnlinePlaylist = {
                                            isImportingPlaylist = true
                                        },

                                        //NOT NECESSARY IN ONLINE PLAYLIST USE IMPORT
                                        onAddToPlaylist = { playlistPreview ->
                                            position =
                                                playlistPreview.songCount.minus(1) ?: 0
                                            if (position > 0) position++ else position = 0

                                            playlistPage!!.songsPage?.items?.forEachIndexed { index, song ->
                                                runCatching {
                                                    Database.insert(song.asMediaItem)
                                                    Database.insert(
                                                        SongPlaylistMap(
                                                            songId = song.asMediaItem.mediaId,
                                                            playlistId = playlistPreview.playlist.id,
                                                            position = position + index
                                                        )
                                                    )
                                                }.onFailure {
                                                    Timber.e(it.message)
                                                }
                                            }
                                            CoroutineScope(Dispatchers.Main).launch {
                                                SmartMessage(context.resources.getString(R.string.done), type = PopupType.Success, context = context)
                                            }
                                        },
                                        onGoToPlaylist = {
                                            navController.navigate("${NavRoutes.localPlaylist.name}/$it")
                                        },
                                        disableScrollingText = disableScrollingText
                                    )
                                }
                            },
                            onLongClick = {
                                SmartMessage(context.resources.getString(R.string.info_add_in_playlist), context = context)
                            }
                        )
                )

                /*
                if (showAddPlaylistSelectDialog)
                    SelectorDialog(
                        title = stringResource(R.string.add_in_playlist),
                        onDismiss = { showAddPlaylistSelectDialog = false },
                        values = listOf(
                            Info("a", stringResource(R.string.import_playlist)),
                            Info("s", stringResource(R.string.add_all_in_playlist))
                        ),
                        onValueSelected = {
                            if (it == "a") {
                                isImportingPlaylist = true
                            } else showPlaylistSelectDialog = true

                            showAddPlaylistSelectDialog = false
                        }
                    )


                if (showPlaylistSelectDialog) {

                    SelectorDialog(
                        title = stringResource(R.string.playlists),
                        onDismiss = { showPlaylistSelectDialog = false },
                        values = playlistPreviews.map {
                            Info(
                                it.playlist.id.toString(),
                                "${it.playlist.name} (${it.songCount})"
                            )
                        },
                        onValueSelected = {
                            var position = 0
                            query {
                                position =
                                    Database.getSongMaxPositionToPlaylist(it.toLong())
                                //Log.d("mediaItemMaxPos", position.toString())
                            }
                            if (position > 0) position++

                                playlistPage!!.songsPage?.items?.forEachIndexed { position, song ->
                                    //Log.d("mediaItemMaxPos", position.toString())
                                    transaction {
                                        Database.insert(song.asMediaItem)
                                        Database.insert(
                                            SongPlaylistMap(
                                                songId = song.asMediaItem.mediaId,
                                                playlistId = it.toLong(),
                                                position = position
                                            )
                                        )
                                    }
                                    //Log.d("mediaItemPos", "add position $position")
                                }

                            showPlaylistSelectDialog = false
                        }
                    )
                }
                 */

                HeaderIconButton(
                    icon = R.drawable.share_social,
                    color = colorPalette().text,
                    onClick = {
                        (playlistPage?.url ?: "https://music.youtube.com/playlist?list=${browseId.removePrefix("VL")}").let { url ->
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, url)
                            }

                            context.startActivity(Intent.createChooser(sendIntent, null))
                        }
                    }
                )
            }

            Row (
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .padding(all = 10.dp)
                    .fillMaxWidth()
            ) {
                AnimatedVisibility(visible = searching) {
                    val focusRequester = remember { FocusRequester() }
                    val focusManager = LocalFocusManager.current
                    val keyboardController = LocalSoftwareKeyboardController.current

                    LaunchedEffect(searching) {
                        focusRequester.requestFocus()
                    }

                    BasicTextField(
                        value = filter ?: "",
                        onValueChange = { filter = it },
                        textStyle = typography().xs.semiBold,
                        singleLine = true,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (filter.isNullOrBlank()) filter = ""
                            focusManager.clearFocus()
                        }),
                        cursorBrush = SolidColor(colorPalette().text),
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                            ) {
                                IconButton(
                                    onClick = {},
                                    icon = R.drawable.search,
                                    color = colorPalette().favoritesIcon,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .size(16.dp)
                                )
                            }
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 30.dp)
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = filter?.isEmpty() ?: true,
                                    enter = fadeIn(tween(100)),
                                    exit = fadeOut(tween(100)),
                                ) {
                                    BasicText(
                                        text = stringResource(R.string.search),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = typography().xs.semiBold.secondary.copy(color = colorPalette().textDisabled)
                                    )
                                }

                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .height(30.dp)
                            .fillMaxWidth()
                            .background(
                                colorPalette().background4,
                                shape = thumbnailRoundness.shape()
                            )
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (!it.hasFocus) {
                                    keyboardController?.hide()
                                    if (filter?.isBlank() == true) {
                                        filter = null
                                        searching = false
                                    }
                                }
                            }
                    )
                }
            }

        }
    }

    val thumbnailContent = adaptiveThumbnailContent(playlistPage == null, playlistPage?.thumbnail?.url)

    val lazyListState = rememberLazyListState()

    LayoutWithAdaptiveThumbnail(thumbnailContent = thumbnailContent) {
        Box(
            modifier = Modifier
                .background(colorPalette().background0)
                //.fillMaxSize()
                .fillMaxHeight()
                .fillMaxWidth(
                    if( NavigationBarPosition.Right.isCurrent() )
                        Dimensions.contentWidthRightBar
                    else
                        1f
                )
        ) {
            LazyColumn(
                state = lazyListState,
                //contentPadding = LocalPlayerAwareWindowInsets.current
                //.only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
                modifier = Modifier
                    .background(colorPalette().background0)
                    .fillMaxSize()
            ) {
                item(
                    key = "header",
                    contentType = 0
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        if (!isLandscape) thumbnailContent()

                        if (playlistPage != null) {
                            headerContent()
                            playlistPage?.title?.let {
                                BasicText(
                                    text = it,
                                    style = typography().xs.semiBold,
                                    maxLines = 1
                                )
                            }
                            BasicText(
                                text = playlistPage?.songsPage?.items?.size.toString() + " "
                                        + stringResource(R.string.songs)
                                        + " - " + formatAsTime(totalPlayTimes),
                                style = typography().xxs.medium,
                                maxLines = 1
                            )
                        } else {
                            BasicText(
                                text = stringResource(R.string.info_wait_it_may_take_a_few_minutes),
                                style = typography().xxs.medium,
                                maxLines = 1
                            )
                        }
                    }
                }

                itemsIndexed(items = playlistPage?.songsPage?.items ?: emptyList()) { index, song ->
                    val isLocal by remember { derivedStateOf { song.asMediaItem.isLocal } }
                    downloadState = getDownloadState(song.asMediaItem.mediaId)
                    val isDownloaded = if (!isLocal) isDownloadedSong(song.asMediaItem.mediaId) else true
                    SongItem(
                        song = song,
                        onDownloadClick = {
                            binder?.cache?.removeResource(song.asMediaItem.mediaId)
                            query {
                                Database.resetFormatContentLength(song.asMediaItem.mediaId)
                            }

                            if (!isLocal)
                            manageDownload(
                                context = context,
                                mediaItem = song.asMediaItem,
                                downloadState = isDownloaded
                            )
                        },
                        downloadState = downloadState,
                        thumbnailSizePx = songThumbnailSizePx,
                        thumbnailSizeDp = songThumbnailSizeDp,
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = {
                                    menuState.display {
                                        NonQueuedMediaItemMenu(
                                            navController = navController,
                                            onDismiss = menuState::hide,
                                            mediaItem = song.asMediaItem,
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                },
                                onClick = {
                                    searching = false
                                    filter = null
                                    playlistPage?.songsPage?.items?.map(Innertube.SongItem::asMediaItem)?.let { mediaItems ->
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayAtIndex(mediaItems, index)
                                    }
                                }
                            ),
                        disableScrollingText = disableScrollingText
                    )
                }

                item(
                    key = "footer",
                    contentType = 0,
                ) {
                    Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
                }

                if (playlistPage == null) {
                    item(key = "loading") {
                        ShimmerHost(
                            modifier = Modifier
                                .fillParentMaxSize()
                        ) {
                            repeat(4) {
                                SongItemPlaceholder(thumbnailSizeDp = songThumbnailSizeDp)
                            }
                        }
                    }
                }
            }

            val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
            if( UiType.ViMusic.isCurrent() && showFloatingIcon )
            FloatingActionsContainerWithScrollToTop(
                lazyListState = lazyListState,
                iconId = R.drawable.shuffle,
                onClick = {
                    playlistPage?.songsPage?.items?.let { songs ->
                        if (songs.isNotEmpty()) {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(
                                songs.shuffled().map(Innertube.SongItem::asMediaItem)
                            )
                        }
                    }
                }
            )


        }
    }
}

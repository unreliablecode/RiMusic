package it.fast4x.rimusic.ui.screens.statistics

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import it.fast4x.compose.persist.persistList
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.MaxStatisticsItems
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.StatisticsType
import it.fast4x.rimusic.enums.ThumbnailRoundness
import it.fast4x.rimusic.models.Album
import it.fast4x.rimusic.models.Artist
import it.fast4x.rimusic.models.PlaylistPreview
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.query
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import it.fast4x.rimusic.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.rimusic.ui.items.AlbumItem
import it.fast4x.rimusic.ui.items.ArtistItem
import it.fast4x.rimusic.ui.items.PlaylistItem
import it.fast4x.rimusic.ui.items.SongItem
import it.fast4x.rimusic.ui.screens.settings.SettingsEntry
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.px
import it.fast4x.rimusic.ui.styling.shimmer
import it.fast4x.rimusic.utils.UpdateYoutubeAlbum
import it.fast4x.rimusic.utils.UpdateYoutubeArtist
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.disableScrollingTextKey
import it.fast4x.rimusic.utils.durationTextToMillis
import it.fast4x.rimusic.utils.forcePlayAtIndex
import it.fast4x.rimusic.utils.formatAsTime
import it.fast4x.rimusic.utils.getDownloadState
import it.fast4x.rimusic.utils.isDownloadedSong
import it.fast4x.rimusic.utils.isLandscape
import it.fast4x.rimusic.utils.manageDownload
import it.fast4x.rimusic.utils.maxStatisticsItemsKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.utils.showStatsListeningTimeKey
import it.fast4x.rimusic.utils.thumbnailRoundnessKey
import me.knighthat.colorPalette
import me.knighthat.typography
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun StatisticsPage(
    navController: NavController,
    statisticsType: StatisticsType
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val albumThumbnailSizeDp = 108.dp
    val albumThumbnailSizePx = albumThumbnailSizeDp.px
    val artistThumbnailSizeDp = 92.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px
    val playlistThumbnailSizeDp = 108.dp
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px

    val scrollState = rememberScrollState()
    val quickPicksLazyGridState = rememberLazyGridState()

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    val thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    val showStatsListeningTime by rememberPreference(showStatsListeningTimeKey,   true)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    val context = LocalContext.current

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSize = thumbnailSizeDp.px

    var songs by persistList<Song>("statistics/songs")
    var allSongs by persistList<Song>("statistics/allsongs")
    var artists by persistList<Artist>("statistics/artists")
    var albums by persistList<Album>("statistics/albums")
    var playlists by persistList<PlaylistPreview>("statistics/playlists")



    val now: Long = System.currentTimeMillis()

    val today: Duration = 1.days
    val lastWeek: Duration = 7.days
    val lastMonth: Duration = 30.days
    val last3Month: Duration = 90.days
    val last6Month: Duration = 180.days
    val lastYear: Duration = 365.days
    val last50Year: Duration = 18250.days


    val from = when (statisticsType) {
        StatisticsType.Today -> today.inWholeMilliseconds
        StatisticsType.OneWeek -> lastWeek.inWholeMilliseconds
        StatisticsType.OneMonth -> lastMonth.inWholeMilliseconds
        StatisticsType.ThreeMonths -> last3Month.inWholeMilliseconds
        StatisticsType.SixMonths -> last6Month.inWholeMilliseconds
        StatisticsType.OneYear -> lastYear.inWholeMilliseconds
        StatisticsType.All -> last50Year.inWholeMilliseconds
    }

    var maxStatisticsItems by rememberPreference(
        maxStatisticsItemsKey,
        MaxStatisticsItems.`10`
    )

    var totalPlayTimes = 0L
    allSongs.forEach {
        totalPlayTimes += it.durationText?.let { it1 ->
            durationTextToMillis(it1)
        }?.toLong() ?: 0
    }

    if (showStatsListeningTime) {
        LaunchedEffect(Unit) {
            Database.songsMostPlayedByPeriod(from, now).collect { allSongs = it }
        }
    }
    LaunchedEffect(Unit) {
        Database.artistsMostPlayedByPeriod(from, now, maxStatisticsItems.number.toInt()).collect { artists = it }
    }
    LaunchedEffect(Unit) {
        Database.albumsMostPlayedByPeriod(from, now, maxStatisticsItems.number.toInt()).collect { albums = it }
    }
    LaunchedEffect(Unit) {
        Database.playlistsMostPlayedByPeriod(from, now, maxStatisticsItems.number.toInt()).collect { playlists = it }
    }
    LaunchedEffect(Unit) {
        Database.songsMostPlayedByPeriod(from, now, maxStatisticsItems.number).collect { songs = it }
    }

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    BoxWithConstraints {
        val quickPicksLazyGridItemWidthFactor = if (isLandscape && maxWidth * 0.475f >= 320.dp) {
            0.475f
        } else {
            0.9f
        }
/*
        val snapLayoutInfoProvider = remember(quickPicksLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = quickPicksLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * quickPicksLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }
*/
        val itemInHorizontalGridWidth = maxWidth * quickPicksLazyGridItemWidthFactor

        Column(
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
                .verticalScroll(scrollState)
                /*
                .padding(
                    windowInsets
                        .only(WindowInsetsSides.Vertical)
                        .asPaddingValues()
                )

                 */
        ) {

            HeaderWithIcon(
                title = when (statisticsType) {
                    StatisticsType.Today -> stringResource(R.string.today)
                    StatisticsType.OneWeek -> stringResource(R.string._1_week)
                    StatisticsType.OneMonth -> stringResource(R.string._1_month)
                    StatisticsType.ThreeMonths -> stringResource(R.string._3_month)
                    StatisticsType.SixMonths -> stringResource(R.string._6_month)
                    StatisticsType.OneYear -> stringResource(R.string._1_year)
                    StatisticsType.All -> stringResource(R.string.all)
                },
                iconId = when (statisticsType) {
                    StatisticsType.Today -> R.drawable.stat_today
                    StatisticsType.OneWeek -> R.drawable.stat_week
                    StatisticsType.OneMonth -> R.drawable.stat_month
                    StatisticsType.ThreeMonths -> R.drawable.stat_3months
                    StatisticsType.SixMonths -> R.drawable.stat_6months
                    StatisticsType.OneYear -> R.drawable.stat_year
                    StatisticsType.All -> R.drawable.calendar_clear
                },
                enabled = true,
                showIcon = true,
                modifier = Modifier,
                onClick = {}
            )

            if (showStatsListeningTime)
            SettingsEntry(
                title = "${allSongs.size} ${stringResource(R.string.statistics_songs_heard)}",
                text = "${formatAsTime(totalPlayTimes)} ${stringResource(R.string.statistics_of_time_taken)}",
                onClick = {},
                trailingContent = {
                    Image(
                        painter = painterResource(R.drawable.musical_notes),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette().shimmer),
                        modifier = Modifier
                            .size(34.dp)
                    )
                },
                modifier = Modifier
                    .background(
                        color = colorPalette().background4,
                        shape = thumbnailRoundness.shape()
                    )

            )

            if (allSongs.isNotEmpty())
                BasicText(
                    text = "${maxStatisticsItems} ${stringResource(R.string.most_played_songs)}",
                    style = typography().m.semiBold,
                    modifier = sectionTextModifier
                )

                LazyHorizontalGrid(
                    state = quickPicksLazyGridState,
                    rows = GridCells.Fixed(2),
                    flingBehavior = ScrollableDefaults.flingBehavior(),
                    contentPadding = endPaddingValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((songThumbnailSizeDp + Dimensions.itemsVerticalPadding * 2) * 2)
                ) {

                    items(
                        count = songs.count(),
                        ) {
                        downloadState = getDownloadState(songs.get(it).asMediaItem.mediaId)
                        val isDownloaded = isDownloadedSong(songs.get(it).asMediaItem.mediaId)
                        SongItem(
                            song = songs.get(it).asMediaItem,
                            onDownloadClick = {
                                binder?.cache?.removeResource(songs.get(it).asMediaItem.mediaId)
                                query {
                                    Database.resetFormatContentLength(songs.get(it).asMediaItem.mediaId)
                                }
                                manageDownload(
                                    context = context,
                                    mediaItem = songs.get(it).asMediaItem,
                                    downloadState = isDownloaded
                                )
                            },
                            downloadState = downloadState,
                            thumbnailSizeDp = thumbnailSizeDp,
                            thumbnailSizePx = thumbnailSize,
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {

                                            //when (builtInPlaylist) {
                                            NonQueuedMediaItemMenu(
                                                navController = navController,
                                                mediaItem = songs.get(it).asMediaItem,
                                                onDismiss = menuState::hide,
                                                disableScrollingText = disableScrollingText
                                            )
                                            /*
                                                BuiltInPlaylist.Offline -> InHistoryMediaItemMenu(
                                                    song = song,
                                                    onDismiss = menuState::hide
                                                )
                                                */
                                            //}

                                        }
                                    },
                                    onClick = {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayAtIndex(
                                            songs.map(Song::asMediaItem),
                                            it
                                        )
                                    }
                                )
                                .animateItemPlacement()
                                .width(itemInHorizontalGridWidth),
                            disableScrollingText = disableScrollingText
                        )

                    }

                }

            if (artists.isNotEmpty())
                BasicText(
                    text = "${maxStatisticsItems} ${stringResource(R.string.most_listened_artists)}",
                    style = typography().m.semiBold,
                    modifier = sectionTextModifier
                )

            LazyRow(contentPadding = endPaddingValues) {
                items(
                    count = artists.count()
                ) {

                    if(artists[it].thumbnailUrl.toString() == "null")
                        UpdateYoutubeArtist(artists[it].id)

                    ArtistItem(
                        artist = artists[it],
                        thumbnailSizePx = artistThumbnailSizePx,
                        thumbnailSizeDp = artistThumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier
                            .clickable(onClick = {
                                if (artists[it].id != "") {
                                    //onGoToArtist(artists[it].id)
                                    navController.navigate("${NavRoutes.artist.name}/${artists[it].id}")
                                }
                            }),
                        disableScrollingText = disableScrollingText
                    )
                }
            }


            if (albums.isNotEmpty())
                BasicText(
                    text = "${maxStatisticsItems} ${stringResource(R.string.most_albums_listened)}",
                    style = typography().m.semiBold,
                    modifier = sectionTextModifier
                )

            LazyRow(contentPadding = endPaddingValues) {
                items(
                    count = albums.count()
                ) {

                    if(albums[it].thumbnailUrl.toString() == "null")
                        UpdateYoutubeAlbum(albums[it].id)

                    AlbumItem(
                        album = albums[it],
                        thumbnailSizePx = albumThumbnailSizePx,
                        thumbnailSizeDp = albumThumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier
                            .clickable(onClick = {
                                if (albums[it].id != "" )
                                //onGoToAlbum(albums[it].id)
                                    navController.navigate("${NavRoutes.album.name}/${albums[it].id}")
                            }),
                        disableScrollingText = disableScrollingText
                    )
                }
            }


            if (playlists.isNotEmpty())
                BasicText(
                    text = "${maxStatisticsItems} ${stringResource(R.string.most_played_playlists)}",
                    style = typography().m.semiBold,
                    modifier = sectionTextModifier
                )

            LazyRow(contentPadding = endPaddingValues) {
                items(
                    count = playlists.count()
                ) {

                    PlaylistItem(
                        playlist = playlists[it],
                        thumbnailSizePx = playlistThumbnailSizePx,
                        thumbnailSizeDp = playlistThumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier
                            .clickable(onClick = {

                               // if (playlists[it].playlist.browseId != "" )
                                    //onGoToPlaylist(playlists[it].playlist.id)
                                navController.navigate("${NavRoutes.playlist.name}/${playlists[it].playlist.id}")
                                 //   onGoToPlaylist(
                                 //       playlists[it].playlist.browseId,
                                 //       null
                                 //   )

                            }),
                        disableScrollingText = disableScrollingText
                    )
                }


            }


            Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))

        }
    }
}

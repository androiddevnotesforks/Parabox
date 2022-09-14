package com.ojhdtapp.parabox.ui.file

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.google.accompanist.flowlayout.FlowRow
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.splitKeeping
import com.ojhdtapp.parabox.core.util.toTimeUntilNow
import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import com.ojhdtapp.parabox.domain.model.File
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.message.AreaState
import com.ojhdtapp.parabox.ui.message.ContactReadFilterState
import com.ojhdtapp.parabox.ui.message.DropdownMenuItemEvent
import com.ojhdtapp.parabox.ui.setting.EditUserNameDialog
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.FileNavGraph
import com.ojhdtapp.parabox.ui.util.SearchAppBar
import com.ojhdtapp.parabox.ui.util.UserProfileDialog
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.Integer.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Destination
@FileNavGraph(start = true)
@Composable
fun FilePage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    drawerState: DrawerState,
    onEvent: (ActivityEvent) -> Unit
) {
    val viewModel: FilePageViewModel = hiltViewModel()
    val mainState by viewModel.fileStateFlow.collectAsState()
    LaunchedEffect(key1 = mainState, block = {
        Log.d("parabox", mainState.toString())
    })
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    BackHandler(enabled = mainState.area != FilePageState.MAIN_AREA) {
        viewModel.setArea(FilePageState.MAIN_AREA)
    }
    BackHandler(enabled = viewModel.searchBarActivateState.value != SearchAppBar.NONE) {
        viewModel.setSearchBarActivateState(SearchAppBar.NONE)
    }
    LaunchedEffect(key1 = true) {
        viewModel.onSearch("", withoutDelay = true)
        viewModel.uiEventFlow.collectLatest {
            when (it) {
                is FilePageUiEvent.ShowSnackBar -> {
                    snackBarHostState.showSnackbar((it.message))
                }
            }
        }
    }
    UserProfileDialog(
        openDialog = mainSharedViewModel.showUserProfileDialogState.value,
        userName = mainSharedViewModel.userNameFlow.collectAsState(initial = "User").value,
        avatarUri = mainSharedViewModel.userAvatarFlow.collectAsState(initial = null).value,
        pluginList = mainSharedViewModel.pluginListStateFlow.collectAsState().value,
        sizeClass = sizeClass,
        onUpdateName = {
            mainSharedViewModel.setEditUserNameDialogState(true)
        },
        onUpdateAvatar = {
            onEvent(ActivityEvent.SetUserAvatar)
        },
        onDismiss = { mainSharedViewModel.setShowUserProfileDialogState(false) })
    EditUserNameDialog(
        openDialog = mainSharedViewModel.editUserNameDialogState.value,
        userName = mainSharedViewModel.userNameFlow.collectAsState(initial = "User").value,
        onConfirm = {
            mainSharedViewModel.setEditUserNameDialogState(false)
            mainSharedViewModel.setUserName(it)
        },
        onDismiss = { mainSharedViewModel.setEditUserNameDialogState(false) }
    )
    Scaffold(modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            SearchAppBar(
                text = viewModel.searchText.value,
                onTextChange = viewModel::onSearch,
                placeholder = "搜索文件",
                fileSelection = viewModel.selectedFiles,
                activateState = viewModel.searchBarActivateState.value,
                avatarUri = mainSharedViewModel.userAvatarFlow.collectAsState(initial = null).value,
                onActivateStateChanged = {
                    viewModel.setSearchBarActivateState(it)
                    when (it) {
                        SearchAppBar.SEARCH -> viewModel.setArea(FilePageState.SEARCH_AREA)
                        SearchAppBar.NONE -> {
                            viewModel.setArea(FilePageState.MAIN_AREA)
                            viewModel.clearSelectedFiles()
                        }
                    }
                },
                sizeClass = sizeClass,
                onMenuClick = {
                    coroutineScope.launch {
                        drawerState.open()
                    }
                },
                onAvatarClick = {
                    mainSharedViewModel.setShowUserProfileDialogState(true)
                },
                onDropdownMenuItemEvent = {
                    when (it) {
                        is DropdownMenuItemEvent.DownloadFile -> {}
                        is DropdownMenuItemEvent.SaveToCloud -> {}
                    }
                }
            )
        },
        bottomBar = {

        }) { paddingValues ->
        AnimatedContent(
            targetState = mainState.area,
//            transitionSpec = {
//                if (targetState == FilePageState.SEARCH_AREA && initialState == FilePageState.MAIN_AREA) {
//                    expandVertically(expandFrom = Alignment.Top).with(
//                        scaleOut(
//                            tween(200),
//                            0.9f
//                        ) + fadeOut(tween(200))
//                    ).apply {
//                        targetContentZIndex = 2f
//                    }
//                } else if (targetState == FilePageState.MAIN_AREA && initialState == FilePageState.SEARCH_AREA) {
//                    (scaleIn(tween(200), 0.9f) + fadeIn(tween(200))).with(
//                        shrinkVertically(
//                            shrinkTowards = Alignment.Top
//                        )
//                    ).apply {
//                        targetContentZIndex = 1f
//                    }
//                } else {
//                    fadeIn() with fadeOut()
//                }
//            }
        ) {
            when (it) {
                FilePageState.MAIN_AREA -> MainArea(
                    mainState = mainState,
                    onSetRecentFilter = { type, value -> viewModel.setRecentFilter(type, value) },
                    searchText = viewModel.searchText.value,
                    selectedFileList = viewModel.selectedFiles,
                    paddingValues = paddingValues,
                    onEvent = onEvent,
                    searchAppBarState = viewModel.searchBarActivateState.value,
                    onChangeSearchAppBarState = {
                        viewModel.setSearchBarActivateState(it)
                    },
                    onChangeArea = { viewModel.setArea(it) },
                    onAddOrRemoveFile = viewModel::addOrRemoveItemOfSelectedFileList
                )
                FilePageState.SEARCH_AREA -> SearchArea()
                else -> {}
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainArea(
    modifier: Modifier = Modifier,
    mainState: FilePageState,
    searchText: String,
    selectedFileList: List<File>,
    paddingValues: PaddingValues,
    searchAppBarState: Int,
    onChangeSearchAppBarState: (state: Int) -> Unit,
    onEvent: (ActivityEvent) -> Unit,
    onChangeArea: (area: Int) -> Unit,
    onAddOrRemoveFile: (file: File) -> Unit,
    onSetRecentFilter: (type: Int, value: Boolean) -> Unit
) {
    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxSize(),
        columns = GridCells.Adaptive(352.dp),
        contentPadding = paddingValues
    ) {
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .animateItemPlacement()
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "最近的",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FlowRow(
                    modifier = Modifier.padding(bottom = 8.dp),
                    mainAxisSpacing = 8.dp
                ) {
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentDocsFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.DOCS,
                                !mainState.enableRecentDocsFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentDocsFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "文档") },
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentSlidesFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.SLIDES,
                                !mainState.enableRecentSlidesFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentSlidesFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "演示文稿") },
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentSheetsFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.SHEETS,
                                !mainState.enableRecentSheetsFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentSheetsFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "电子表格") },
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentVideoFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.VIDEO,
                                !mainState.enableRecentVideoFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentVideoFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "视频") },
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentAudioFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.AUDIO,
                                !mainState.enableRecentAudioFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentAudioFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "音频") },
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentPictureFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.PICTURE,
                                !mainState.enableRecentPictureFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentPictureFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "图片") },
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentPDFFilter,
                        onClick = {
                            onSetRecentFilter(ExtensionFilter.PDF, !mainState.enableRecentPDFFilter)
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentPDFFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "便携式文档") },
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentCompressedFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.COMPRESSED,
                                !mainState.enableRecentCompressedFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentCompressedFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "压缩文件") },
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                }
                if (mainState.recentFilterData.isEmpty()) {
                    val context = LocalContext.current
                    val imageLoader = ImageLoader.Builder(context)
                        .components {
                            add(SvgDecoder.Factory())
                        }
                        .build()
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(R.drawable.empty_2)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(224.dp)
                            .padding(top = 32.dp, bottom = 16.dp)
                    )
                    Text(
                        modifier = Modifier
                            .padding(bottom = 32.dp)
                            .align(Alignment.CenterHorizontally),
                        text = "暂无可显示的文件",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    mainState.recentFilterData.take(5)
                        .forEachIndexed { index, file ->
                            FileItem(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .animateItemPlacement(),
                                file = file,
                                searchText = searchText,
                                isFirst = index == 0,
                                isLast = index == min(mainState.recentFilterData.lastIndex, 4),
                                isSelected = selectedFileList.contains(file),
                                onClick = {
                                    if (searchAppBarState == SearchAppBar.FILE_SELECT) {
                                        onAddOrRemoveFile(file)
                                    } else {
                                        if (file.downloadingState is DownloadingState.None || file.downloadingState is DownloadingState.Failure) {
                                            onEvent(ActivityEvent.DownloadFile(file))
                                        } else {

                                        }
                                    }
                                },
                                onLongClick = {
                                    onChangeSearchAppBarState(SearchAppBar.FILE_SELECT)
                                    onAddOrRemoveFile(file)
                                },
                                onAvatarClick = {
                                    onChangeSearchAppBarState(SearchAppBar.FILE_SELECT)
                                    onAddOrRemoveFile(file)
                                })
                        }
                    TextButton(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        onClick = { onChangeArea(FilePageState.SEARCH_AREA) }
                    ) {
                        Text(text = "查看完整列表")
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .animateItemPlacement()
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "云服务",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SearchArea(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(
    modifier: Modifier = Modifier,
    file: File,
    searchText: String,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val topRadius by animateDpAsState(targetValue = if (isFirst) 24.dp else 0.dp)
    val bottomRadius by animateDpAsState(targetValue = if (isLast) 24.dp else 0.dp)
    Surface(
        modifier = modifier.height(IntrinsicSize.Min),
        shape = RoundedCornerShape(
            topStart = topRadius,
            topEnd = topRadius,
            bottomStart = bottomRadius,
            bottomEnd = bottomRadius
        ),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = LocalIndication.current,
                    enabled = true,
                    onLongClick = onLongClick,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Crossfade(targetState = isSelected) {
                if (it) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                onAvatarClick()
                            }
                    ) {
                        Icon(
                            modifier = Modifier.align(Alignment.Center),
                            imageVector = Icons.Outlined.Done,
                            contentDescription = "selected",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                onAvatarClick()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (file.downloadingState) {
                            is DownloadingState.None -> {
                                Icon(
                                    imageVector = when (file.extension) {
                                        "apk" -> Icons.Outlined.Android
                                        "bmp", "jpeg", "jpg", "png", "tif", "gif", "pcx", "tga", "exif", "fpx", "svg", "psd", "cdr", "pcd", "dxf", "ufo", "eps", "ai", "raw", "webp", "avif", "apng", "tiff" -> Icons.Outlined.Image
                                        "txt", "log", "md", "json", "xml" -> Icons.Outlined.Description
                                        "cd", "wav", "aiff", "mp3", "wma", "ogg", "mpc", "flac", "ape", "3gp" -> Icons.Outlined.AudioFile
                                        "avi", "wmv", "mp4", "mpeg", "mpg", "mov", "flv", "rmvb", "rm", "asf" -> Icons.Outlined.VideoFile
                                        "zip", "rar", "7z", "bz2", "tar", "jar", "gz", "deb" -> Icons.Outlined.FolderZip
                                        else -> Icons.Outlined.FilePresent
                                    }, contentDescription = "type",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            is DownloadingState.Downloading -> {
                                val progress by animateFloatAsState(targetValue = file.downloadingState.progress.toFloat() / 100)
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    progress = progress,
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp,
                                )
                                Icon(
                                    imageVector = Icons.Outlined.FileDownload,
                                    contentDescription = "download",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            is DownloadingState.Failure -> {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = "warning",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            is DownloadingState.Done -> {
                                Icon(
                                    imageVector = Icons.Outlined.FileDownloadDone,
                                    contentDescription = "download done",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Icon(
                            imageVector = when (file.extension) {
                                "apk" -> Icons.Outlined.Android
                                "bmp", "jpeg", "jpg", "png", "tif", "gif", "pcx", "tga", "exif", "fpx", "svg", "psd", "cdr", "pcd", "dxf", "ufo", "eps", "ai", "raw", "webp", "avif", "apng", "tiff" -> Icons.Outlined.Image
                                "txt", "log", "md", "json", "xml" -> Icons.Outlined.Description
                                "cd", "wav", "aiff", "mp3", "wma", "ogg", "mpc", "flac", "ape", "3gp" -> Icons.Outlined.AudioFile
                                "avi", "wmv", "mp4", "mpeg", "mpg", "mov", "flv", "rmvb", "rm", "asf" -> Icons.Outlined.VideoFile
                                "zip", "rar", "7z", "bz2", "tar", "jar", "gz", "deb" -> Icons.Outlined.FolderZip
                                else -> Icons.Outlined.FilePresent
                            }, contentDescription = "type",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildAnnotatedString {
                        file.profileName.splitKeeping(searchText).forEach {
                            if (it == searchText) {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(it)
                                }
                            } else {
                                append(it)
                            }
                        }
                        append("  ")
                        if (file.downloadingState is DownloadingState.Downloading) {
                            append(FileUtil.getSizeString(file.downloadingState.downloadedBytes.toLong()))
                            append(" ")
                            append("/")
                            append(" ")
                            append(FileUtil.getSizeString(file.downloadingState.totalBytes.toLong()))
                        } else {
                            append(FileUtil.getSizeString(file.size))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.Top),
                text = file.timestamp.toTimeUntilNow(),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}


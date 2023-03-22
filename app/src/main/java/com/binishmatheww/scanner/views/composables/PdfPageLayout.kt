package com.binishmatheww.scanner.views.composables

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.pdf.PdfRenderer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.utils.log
import com.binishmatheww.scanner.common.utils.noRippleCombinedClickable
import com.binishmatheww.scanner.models.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


@Composable
fun PdfPageLayout(
    modifier: Modifier = Modifier,
    page: PdfFile,
    index: Int,
    isEditing: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onPageUp:(Int) -> Unit,
    onPageDelete:(Int) -> Unit,
    onPageDown:(Int) -> Unit,
){
    val configuration = LocalConfiguration.current
    var filter by remember { mutableStateOf<ColorFilter?>(null) }
    if(configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
        ConstraintLayout(
            modifier = modifier
        ){

            val (
                previewConstraint,
                toolsConstraint
            ) = createRefs()

            Page(
                modifier = Modifier
                    .constrainAs(previewConstraint) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    },
                page = page,
                index = index,
                onLongClick = onLongClick,
                onClick = onClick,
                filter = filter
            )

            AnimatedVisibility(
                modifier = Modifier
                    .constrainAs(toolsConstraint) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        height = Dimension.fillToConstraints
                        width = Dimension.fillToConstraints
                    },
                visible = isEditing,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 500
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = 500
                    )
                )
            ) {

                PdfPageEditingToolsLayout(
                    modifier = Modifier
                        .fillMaxSize(),
                    index = index,
                    onPageUp = onPageUp,
                    onPageDelete = onPageDelete,
                    onPageDown = onPageDown,
                    onFilterChange = {
                        filter = it
                    }
                )

            }

        }
    }
    else{
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Page(
                modifier = Modifier
                    .height(configuration.screenHeightDp.dp)
                    .fillMaxWidth()
                    .weight(weight = 1f),
                page = page,
                index = index,
                onLongClick = onLongClick,
                onClick = onClick,
                filter = filter
            )
            PdfPageEditingToolsLayout(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(weight = 1f),
                index = index,
                onPageUp = onPageUp,
                onPageDelete = onPageDelete,
                onPageDown = onPageDown,
                onFilterChange = {
                    filter = it
                }
            )
        }
    }

}


@Composable
private fun Page(
    modifier: Modifier,
    page: PdfFile,
    index: Int,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    filter: ColorFilter?
){
    val context = LocalContext.current
    val mutex = remember { Mutex() }
    val imageLoader by remember { mutableStateOf(context.imageLoader) }
    val imageLoadingScope = rememberCoroutineScope()
    val cacheKey by remember { mutableStateOf(MemoryCache.Key(page.uri.toString())) }
    var bitmap by remember { mutableStateOf( imageLoader.memoryCache?.get(cacheKey)?.bitmap ) }
    DisposableEffect(page.uri.toString()) {
        val job = imageLoadingScope.launch(Dispatchers.IO) {
            mutex.withLock {
                if (!coroutineContext.isActive) return@launch
                try {
                    context.contentResolver.openFileDescriptor(page.uri, "r")?.use { parcelFileDescriptor ->
                        val renderer = PdfRenderer(parcelFileDescriptor)
                        renderer.openPage(0).use { pdfPage ->
                            val destinationBitmap = Bitmap.createBitmap(pdfPage.width , pdfPage.height , Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(destinationBitmap)
                            canvas.drawColor(Color.White.toArgb())
                            canvas.drawBitmap(destinationBitmap, 0.0f, 0.0f, null)
                            pdfPage.render(
                                destinationBitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                            //destinationBitmap = getResizedBitmap(destinationBitmap, 720)
                            log("Loading bitmap for ${page.name}")
                            bitmap = destinationBitmap
                        }
                    }
                } catch (e: Exception) {
                    //Just catch and return in case the renderer is being closed
                    return@launch
                }
            }
        }
        onDispose {
            job.cancel()
        }
    }
    if (bitmap == null) {
        Box(
            modifier = modifier
                .background(
                    color = Color.White,
                )
                .fillMaxWidth()
                .height(720.dp)
        )
    }
    else {
        val request by remember {
            mutableStateOf(
                ImageRequest
                    .Builder(context)
                    .crossfade(true)
                    .memoryCachePolicy(
                        CachePolicy.ENABLED
                    )
                    .memoryCacheKey(cacheKey)
                    .data(bitmap)
                    .build()
            )
        }
        DisposableEffect(page.uri.toString()) {
            imageLoader.enqueue(request)
            onDispose {}
        }
        AsyncImage(
            modifier = modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .background(
                    color = Color.White
                )
                .noRippleCombinedClickable(
                    onClick = {
                        onClick.invoke()
                    },
                    onLongClick = {
                        onLongClick.invoke()
                    }
                ),
            model = request,
            contentScale = ContentScale.FillWidth,
            contentDescription = context.getString(R.string.Page).plus(" ").plus(index+1),
            colorFilter = filter?.asComposeColorFilter()
        )
    }

}
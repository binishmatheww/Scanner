package com.binishmatheww.scanner.views.composables

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.binishmatheww.scanner.common.utils.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfPageLayout(
    modifier: Modifier = Modifier,
    pageFile: File,
    index: Int,
    isEditing: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onPageUp:(Int) -> Unit,
    onPageDelete:(Int) -> Unit,
    onPageDown:(Int) -> Unit,
){

    ConstraintLayout(
        modifier = modifier
    ){

        val context = LocalContext.current

        val (
            previewConstraint,
            toolsConstraint
        ) = createRefs()

        val mutex = remember { Mutex() }

        val imageLoader by remember { mutableStateOf(context.imageLoader) }

        val imageLoadingScope = rememberCoroutineScope()

        val cacheKey by remember { mutableStateOf(MemoryCache.Key(pageFile.absolutePath)) }

        var bitmap by remember { mutableStateOf( imageLoader.memoryCache?.get(cacheKey)?.bitmap ) }

        var filter by remember { mutableStateOf<ColorFilter?>(null) }

        DisposableEffect(pageFile.absolutePath) {

            val job = imageLoadingScope.launch(Dispatchers.IO) {

                mutex.withLock {

                    if (!coroutineContext.isActive) return@launch

                    try {

                        val renderer = PdfRenderer(ParcelFileDescriptor.open(pageFile, ParcelFileDescriptor.MODE_READ_ONLY))

                        renderer.openPage(0).use { page ->

                            val destinationBitmap = Bitmap.createBitmap(page.width , page.height , Bitmap.Config.ARGB_8888)

                            val canvas = Canvas(destinationBitmap)

                            canvas.drawColor(Color.White.toArgb())

                            canvas.drawBitmap(destinationBitmap, 0.0f, 0.0f, null)

                            page.render(
                                destinationBitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )

                            //destinationBitmap = getResizedBitmap(destinationBitmap, 720)

                            log("Loading bitmap for ${pageFile.name}")

                            bitmap = destinationBitmap

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
                modifier = Modifier
                    .constrainAs(previewConstraint) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
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

            DisposableEffect(pageFile.absolutePath) {

                imageLoader.enqueue(request)

                onDispose {}

            }

            AsyncImage(
                modifier = Modifier
                    .constrainAs(previewConstraint) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .background(
                        color = Color.White
                    )
                    .combinedClickable(
                        onLongClick = {
                            onLongClick.invoke()
                        },
                        onClick = {
                            onClick.invoke()
                        }
                    ),
                model = request,
                contentScale = ContentScale.FillWidth,
                contentDescription = "Page ${index+1}",
                colorFilter = filter?.asComposeColorFilter()
            )

        }

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
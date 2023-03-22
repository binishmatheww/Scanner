package com.binishmatheww.scanner.views.composables

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.utils.noRippleClickable

@Composable
fun PdfPageEditingToolsLayout(
    modifier: Modifier = Modifier,
    index: Int,
    onPageUp:(Int) -> Unit,
    onPageDelete:(Int) -> Unit,
    onPageDown:(Int) -> Unit,
    onFilterChange: (android.graphics.ColorFilter) -> Unit
){

    val context = LocalContext.current

    var filterSliderValue by remember { mutableStateOf(100f) }

    ConstraintLayout(
        modifier = modifier
    ) {

        val (
            reorderPageConstraint,
            pageFilterConstraint,
            pageNumberConstraint
        ) = createRefs()

        Column(
            modifier = Modifier
                .constrainAs(reorderPageConstraint) {
                    end.linkTo(parent.end, 2.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(pageFilterConstraint.top, 2.dp)
                }
                .wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ){

            Image(
                modifier = Modifier
                    .size(96.dp)
                    .noRippleClickable {
                        onPageUp.invoke(index)
                    },
                painter = painterResource(id = R.drawable.reordup),
                contentDescription = context.getString(R.string.Up),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )

            Text(
                modifier = Modifier
                    .noRippleClickable {
                        onPageDelete.invoke(index)
                    },
                text = LocalContext.current.getString(R.string.Delete),
                color = MaterialTheme.colorScheme.primary
            )

            Image(
                modifier = Modifier
                    .size(96.dp)
                    .noRippleClickable {
                        onPageDown.invoke(index)
                    },
                painter = painterResource(id = R.drawable.reorddown),
                contentDescription = context.getString(R.string.Down),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )

        }

        Slider(
            modifier = Modifier.constrainAs(pageFilterConstraint){
                start.linkTo(parent.start, 4.dp)
                end.linkTo(parent.end, 4.dp)
                bottom.linkTo(pageNumberConstraint.top, 2.dp)
            },
            value = filterSliderValue,
            valueRange = 0f..200f,
            onValueChange = { value ->

                filterSliderValue = value

                val progress = filterSliderValue/100f

                val brightness = 0f

                onFilterChange.invoke(
                    ColorMatrixColorFilter(

                        ColorMatrix(
                            floatArrayOf(
                                progress, 0f, 0f, 0f, brightness,
                                0f, progress, 0f, 0f, brightness,
                                0f, 0f, progress, 0f, brightness,
                                0f, 0f, 0f, 1f, brightness
                            )
                        )

                    )
                )

            }
        )

        Text(
            modifier = Modifier
                .constrainAs(pageNumberConstraint) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom, 2.dp)
                }
                .wrapContentSize(),
            text = "Page ${index + 1}",
            color = MaterialTheme.colorScheme.primary,
        )

    }

}
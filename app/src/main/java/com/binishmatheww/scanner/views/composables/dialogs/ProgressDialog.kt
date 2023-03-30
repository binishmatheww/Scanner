package com.binishmatheww.scanner.views.composables.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import com.binishmatheww.scanner.R

@Composable
fun ProgressDialog(
    modifier: Modifier = Modifier,
    dismissOnBackPress: Boolean = false,
    dismissOnClickOutside: Boolean = false,
    title: String,
    progress: Float,
    onDismissRequest: () -> Unit
){
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        ),
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation()
        ){
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                val (
                    titleTextConstraint,
                    progressBarConstraint,
                    cancelButtonConstraint,
                ) = createRefs()
                Text(
                    modifier = Modifier
                        .constrainAs(titleTextConstraint){
                            top.linkTo(parent.top, 8.dp)
                            start.linkTo(parent.start, 4.dp)
                            end.linkTo(parent.end, 4.dp)
                        },
                    text = title
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .constrainAs(progressBarConstraint) {
                            top.linkTo(titleTextConstraint.bottom, 8.dp)
                            start.linkTo(parent.start, 4.dp)
                            end.linkTo(parent.end, 4.dp)
                        }
                        .height(5.dp)
                        .wrapContentWidth(),
                    progress = progress
                )
                Button(
                    modifier = Modifier
                        .constrainAs(cancelButtonConstraint){
                            top.linkTo(progressBarConstraint.bottom, 8.dp)
                            start.linkTo(parent.start, 4.dp)
                            end.linkTo(parent.end, 4.dp)
                            bottom.linkTo(parent.bottom, 8.dp)
                        },
                    onClick = onDismissRequest
                ) {
                    Text(
                        text = LocalContext.current.getString(R.string.Cancel)
                    )
                }
            }
        }
    }
}
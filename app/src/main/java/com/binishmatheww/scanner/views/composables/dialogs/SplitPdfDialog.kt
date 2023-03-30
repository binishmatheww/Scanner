package com.binishmatheww.scanner.views.composables.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import com.binishmatheww.scanner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPdfDialog(
    modifier: Modifier = Modifier,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = false,
    onDismissRequest: () -> Unit,
    splitIndex: String,
    onPageConfirmed: (String) -> Unit
){
    var splt by rememberSaveable { mutableStateOf(splitIndex) }
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
                    passwordInputConstraint,
                    okButtonConstraint,
                ) = createRefs()
                Text(
                    modifier = Modifier
                        .constrainAs(titleTextConstraint){
                            top.linkTo(parent.top, 8.dp)
                            start.linkTo(parent.start, 4.dp)
                            end.linkTo(parent.end, 4.dp)
                        },
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    text = LocalContext.current.getString(R.string.EnterThePageNumberToSplitAt)
                )
                TextField(
                    modifier = Modifier
                        .constrainAs(passwordInputConstraint){
                            top.linkTo(titleTextConstraint.bottom, 8.dp)
                            start.linkTo(parent.start, 4.dp)
                            end.linkTo(parent.end, 4.dp)
                        },
                    placeholder = {
                        Text(
                            text = LocalContext.current.getString(R.string.EnterThePageNumber)
                        )
                    },
                    value = splt,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    onValueChange = {
                        splt = it
                    }
                )
                Button(
                    modifier = Modifier
                        .constrainAs(okButtonConstraint){
                            top.linkTo(passwordInputConstraint.bottom, 8.dp)
                            start.linkTo(parent.start, 4.dp)
                            end.linkTo(parent.end, 4.dp)
                            bottom.linkTo(parent.bottom, 8.dp)
                        },
                    enabled = splt.isNotEmpty(),
                    onClick = {
                        onPageConfirmed.invoke(splt)
                    }
                ) {
                    Text(
                        text = LocalContext.current.getString(R.string.Ok)
                    )
                }
            }
        }
    }
}
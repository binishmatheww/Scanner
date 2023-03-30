package com.binishmatheww.scanner.views.composables.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.composables.ClickableIcon


@Composable
fun EditorExtraDialog(
    modifier: Modifier = Modifier,
    onOptionSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
){

    //val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
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
                    firstRowConstraint,
                    secondRowConstraint,
                    thirdRowConstraint,
                ) = createRefs()

                Row(
                    modifier = Modifier
                        .constrainAs(firstRowConstraint){
                            top.linkTo(parent.top, 12.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                            height = Dimension.wrapContent
                        },
                    horizontalArrangement = Arrangement.Center
                ) {

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.addpdf),
                        onClick = {
                            onOptionSelected.invoke("addPdf")
                        }
                    )

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.addimg),
                        onClick = {
                            onOptionSelected.invoke("addImages")
                        }
                    )

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.addtxt),
                        onClick = {
                            onOptionSelected.invoke("addText")
                        }
                    )

                }

                Row(
                    modifier = Modifier
                        .constrainAs(secondRowConstraint){
                            top.linkTo(firstRowConstraint.bottom, 6.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                            height = Dimension.wrapContent
                        },
                    horizontalArrangement = Arrangement.Center
                ) {

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.exportpdf),
                        onClick = {
                            onOptionSelected.invoke("exportPdf")
                        }
                    )

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.splitpdf),
                        onClick = {
                            onOptionSelected.invoke("splitPdf")
                        }
                    )

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.pdftoimg),
                        onClick = {
                            onOptionSelected.invoke("pdfToImages")
                        }
                    )

                }

                Row(
                    modifier = Modifier
                        .constrainAs(thirdRowConstraint){
                            top.linkTo(secondRowConstraint.bottom, 6.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                            height = Dimension.wrapContent
                        },
                    horizontalArrangement = Arrangement.Center
                ) {

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.compresspdf),
                        onClick = {
                            onOptionSelected.invoke("compressPdf")
                        }
                    )

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.encryptpdf),
                        onClick = {
                            onOptionSelected.invoke("encryptPdf")
                        }
                    )

                }

            }

        }

    }

}
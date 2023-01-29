package com.binishmatheww.scanner.views.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import androidx.fragment.app.DialogFragment
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.composables.ClickableIcon
import com.binishmatheww.scanner.views.listeners.OnDialogButtonClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class EditorExtraDialog(private val onDialogButtonClickListener: OnDialogButtonClickListener) : DialogFragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_editor_extra, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.RoundShapeTheme)

        val inflater = requireActivity().layoutInflater
        val layout : View = inflater.inflate(R.layout.dialog_editor_extra, null)

        val addPdf : ImageView = layout.findViewById(R.id.addPdf)
        addPdf.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("addPdf")
        }

        val addImages : ImageView = layout.findViewById(R.id.addImages)
        addImages.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("addImages")
        }

        val addText : ImageView = layout.findViewById(R.id.addText)
        addText.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("addText")
        }

        val exportPdf : ImageView = layout.findViewById(R.id.exportPdf)
        exportPdf.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("exportPdf")
        }

        val splitPdf : ImageView = layout.findViewById(R.id.splitPdf)
        splitPdf.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("splitPdf")
        }

        val pdfToImages : ImageView = layout.findViewById(R.id.pdfToImages)
        pdfToImages.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("pdfToImages")
        }

        val compressPdf : ImageView = layout.findViewById(R.id.compressPdf)
        compressPdf.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("compressPdf")
        }

        val encryptPdf : ImageView = layout.findViewById(R.id.encryptPdf)
        encryptPdf.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("encryptPdf")
        }

        builder.setView(layout)

        return builder.create()
    }


}

@Composable
fun EditorExtraDialog(
    modifier: Modifier = Modifier,
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

                        }
                    )

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.addimg),
                        onClick = {

                        }
                    )

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.addtxt),
                        onClick = {

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

                        }
                    )

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.splitpdf),
                        onClick = {

                        }
                    )

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.pdftoimg),
                        onClick = {

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

                        }
                    )

                    ClickableIcon(
                        modifier = Modifier
                            .size(100.dp),
                        painter = painterResource(id = R.drawable.encryptpdf),
                        onClick = {

                        }
                    )

                }

            }

        }

    }

}
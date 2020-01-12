package justin.apackage.com.hypemap.ui

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.EditText
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.HypeMapViewModel

class AddUserDialog(context: Context,
                    val viewModel: HypeMapViewModel): AlertDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        val view = LayoutInflater.from(context).inflate(R.layout.add_user_dialog, null)
        val editText: EditText = view.findViewById(R.id.userEditText)
        setView(view)
        setTitle("Add a user to follow")
        setButton(DialogInterface.BUTTON_NEGATIVE, "Close") { dialog, _ ->
            dismiss()
        }
        setButton(DialogInterface.BUTTON_POSITIVE, "OK") { dialog, _ ->
            addUser(editText.text.toString())
        }
        getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
        setOnDismissListener {
            viewModel.mMap.setPadding(0, 0, 0, 0)
        }
        super.onCreate(savedInstanceState)
    }

    private fun setUpEditText(editText: EditText) {
        editText.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                addUser(editText.text.toString())
                return@setOnKeyListener true
            }
            false
        }
        editText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = s?.trim() != ""
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
    }

    private fun addUser(userName: String) {
        viewModel.addUser(userName)
        dismiss()
    }
}

package com.infnet.tp1finalversion

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

fun View.showSnackbar(view: View, msg: String, length: Int, actionMenssage: CharSequence?, action: (View) -> Unit) {
    val snackbar = Snackbar.make(view, msg, length)
    if (actionMenssage != null){
        snackbar.setAction(actionMenssage){
            action(this)
        }.show()
    }
    else{
        snackbar.show()
    }

}

fun AppCompatActivity.toast(msg: String){
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
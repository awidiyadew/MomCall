package id.semetondevs.momcall

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.Window
import android.widget.Button
import id.semetondevs.momcall.database.Contact
import kotlinx.android.synthetic.main.dialog_manage_contact.*


class ManageContactDialogFragment : DialogFragment() {

    private var manageContactListener: ManageContactListener? = null
    private lateinit var selectedContact: Contact

    interface ManageContactListener {
        fun onInputValid(contact: Contact)
    }

    companion object {
        private val ARGS_CONTACT = "contact_args"

        fun newInstance(contact: Contact) : ManageContactDialogFragment {
            val args = Bundle()
            args.putParcelable(ARGS_CONTACT, contact)
            val dialogFragment = ManageContactDialogFragment()
            dialogFragment.arguments = args
            Log.d("Dialog", contact.toString())
            return dialogFragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(context, R.style.Theme_AppCompat_Light_Dialog_Alert)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_manage_contact)
        dialog.setCancelable(false)
        activateListener(manageContactListener, dialog.dialog_btn_positive, dialog.dialog_btn_negative)

        selectedContact = arguments.getParcelable<Contact>(ARGS_CONTACT)
        bindContact(selectedContact, dialog)
        return dialog
    }

    private fun bindContact(contact: Contact?, dialog: Dialog) {
        if (contact == null) return
        dialog.dialog_contact_name.setText(contact.name)
        dialog.dialog_contact_num.setText(contact.number)
    }

    private fun activateListener(listener: ManageContactListener?, buttonPositive: Button, buttonNegative: Button) {
        buttonNegative.setOnClickListener { dialog.dismiss() }

        buttonPositive.setOnClickListener {
            if (isInputValid()) {
                listener?.onInputValid(getContactData())
                dialog.dismiss()
            }
        }
    }

    private fun getContactData(): Contact {
        selectedContact.name = dialog.dialog_contact_name.text.toString()
        selectedContact.number = dialog.dialog_contact_num.text.toString()
        selectedContact.photoUrl = "http://picsum.photos/450/650/?image=1005"
        return selectedContact
    }

    fun setManageContactListener(listener: ManageContactListener) {
        this.manageContactListener = listener
    }

    private fun isInputValid(): Boolean {
        if (dialog.dialog_contact_name.text.isEmpty()) return false
        if (dialog.dialog_contact_num.text.isEmpty()) return false

        return true
    }

}
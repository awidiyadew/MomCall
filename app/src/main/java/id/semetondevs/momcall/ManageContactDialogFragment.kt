package id.semetondevs.momcall

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.arch.persistence.room.Room
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import id.semetondevs.momcall.database.Contact
import id.semetondevs.momcall.database.ContactDatabase
import kotlinx.android.synthetic.main.dialog_manage_contact.*
import siclo.com.ezphotopicker.api.EZPhotoPick
import siclo.com.ezphotopicker.api.EZPhotoPickStorage
import siclo.com.ezphotopicker.api.models.EZPhotoPickConfig
import siclo.com.ezphotopicker.api.models.PhotoSource
import java.io.File


class ManageContactDialogFragment : DialogFragment() {

    private var manageContactListener: ManageContactListener? = null
    private lateinit var selectedContact: Contact
    private var photoPath: String? = null
    val contactDb: ContactDatabase by lazy {
        Room.databaseBuilder(activity, ContactDatabase::class.java, "contact_db")
                .allowMainThreadQueries()
                .build()
    }

    interface ManageContactListener {
        fun onSaveContactSuccess()
        fun onContactDeleteSuccess()
        fun onUpdateContactSuccess()
    }

    companion object {
        private val ARGS_CONTACT = "contact_args"
        val PHOTO_DIR = "contact_photos"

        fun newInstance(contact: Contact) : ManageContactDialogFragment {
            val args = Bundle()
            args.putParcelable(ARGS_CONTACT, contact)
            val dialogFragment = ManageContactDialogFragment()
            dialogFragment.arguments = args
            return dialogFragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(context, R.style.Theme_AppCompat_Light_Dialog_Alert)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_manage_contact)
        dialog.setCancelable(false)
        activateListener(manageContactListener,
                dialog.dialog_btn_positive,
                dialog.dialog_btn_negative,
                dialog.btn_add_photo,
                dialog.btn_delete_contact)

        selectedContact = arguments.getParcelable<Contact>(ARGS_CONTACT)
        bindContact(selectedContact, dialog)
        return dialog
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return
        }

        if (requestCode == EZPhotoPick.PHOTO_PICK_GALLERY_REQUEST_CODE
                || requestCode == EZPhotoPick.PHOTO_PICK_CAMERA_REQUEST_CODE) {
            val photoName = data?.getStringExtra(EZPhotoPick.PICKED_PHOTO_NAME_KEY)
            val photoPath = EZPhotoPickStorage(activity).getAbsolutePathOfStoredPhoto(PHOTO_DIR, photoName)
            val photoFile = File(photoPath)

            if (photoFile.exists()) {
                val outputDirectory = File(activity.applicationInfo.dataDir)
                val fileName = "${selectedContact.name.replace(" ","_")}.jpg"
                val newPhotoFile = photoFile.moveFile(outputDirectory, fileName)
                if (newPhotoFile.exists()) {
                    this.photoPath = newPhotoFile.path
                    setPhoto(dialog.iv_contact_photo, Uri.fromFile(newPhotoFile))
                } else {
                    Toast.makeText(activity, "Failed to add photo", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(activity, "Failed to add photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setPhoto(imgView: ImageView, photoUri: Uri) {
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        imgView.setImageURI(photoUri)
    }

    private fun bindContact(contact: Contact?, dialog: Dialog) {
        if (contact == null) return
        dialog.dialog_contact_name.setText(contact.name)
        dialog.dialog_contact_num.setText(contact.number)

        if (contact.isEditMode()) {
            dialog.iv_contact_photo.loadFromPath(contact.photo, R.drawable.icn_nopicture)
        }
    }

    private fun activateListener(listener: ManageContactListener?,
                                 buttonPositive: Button,
                                 buttonNegative: Button,
                                 buttonAddPhoto: View,
                                 buttonDeleteContact: View) {
        buttonNegative.setOnClickListener { dialog.dismiss() }

        buttonPositive.setOnClickListener {
            if (isInputValid()) {
                if (selectedContact.isEditMode()) {
                    updateContact(contactDb, listener)
                } else {
                    saveContact(contactDb, listener)
                }
            } else {
                Toast.makeText(activity, "Input not valid or no image selected", Toast.LENGTH_SHORT).show()
            }
        }

        buttonAddPhoto.setOnClickListener { pickPhoto() }

        buttonDeleteContact.setOnClickListener { deleteContact(contactDb, listener) }
    }

    private fun deleteContact(db: ContactDatabase, listener: ManageContactListener?) {
        if (selectedContact.isEditMode()) {
            db.contactDao().deleteContact(selectedContact)
            dialog.dismiss()
            listener?.onContactDeleteSuccess()
        } else {
            dialog.dismiss()
        }
    }

    private fun updateContact(db: ContactDatabase, listener: ManageContactListener?) {
        if (selectedContact.isEditMode()) {
            db.contactDao()
                    .updateContact(getContactData())
            dialog.dismiss()
            listener?.onUpdateContactSuccess()
        }
    }

    private fun saveContact(db: ContactDatabase, listener: ManageContactListener?) {
        if (!selectedContact.isEditMode()) {
            db.contactDao()
                    .saveContact(getContactData())
            dialog.dismiss()
            listener?.onSaveContactSuccess()
        }
    }

    private fun pickPhoto() {
        val config = EZPhotoPickConfig()
        config.storageDir = PHOTO_DIR
        config.photoSource = PhotoSource.GALLERY
        config.isAllowMultipleSelect = false
        EZPhotoPick.startPhotoPickActivity(this, config)
    }

    private fun getContactData(): Contact {
        selectedContact.name = dialog.dialog_contact_name.text.toString()
        selectedContact.number = dialog.dialog_contact_num.text.toString()
        selectedContact.photo = photoPath
        return selectedContact
    }

    fun setManageContactListener(listener: ManageContactListener) {
        this.manageContactListener = listener
    }

    private fun isInputValid(): Boolean {
        if (dialog.dialog_contact_name.text.isEmpty()) return false
        if (dialog.dialog_contact_num.text.isEmpty()) return false
        if (photoPath == null) return false

        return true
    }

}
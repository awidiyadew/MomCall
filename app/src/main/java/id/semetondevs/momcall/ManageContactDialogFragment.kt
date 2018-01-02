package id.semetondevs.momcall

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.arch.persistence.room.Room
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.Log
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel


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
    }

    companion object {
        private val TAG = ManageContactDialogFragment::class.java.simpleName
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
        activateListener(manageContactListener, dialog.dialog_btn_positive, dialog.dialog_btn_negative, dialog.iv_add_photo)

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
            val pickedPhoto = EZPhotoPickStorage(activity).loadLatestStoredPhotoBitmap()
            setPhoto(dialog.iv_contact_photo, pickedPhoto)

            val photoName = data?.getStringExtra(EZPhotoPick.PICKED_PHOTO_NAME_KEY)
            val photoPath = EZPhotoPickStorage(activity).getAbsolutePathOfStoredPhoto(PHOTO_DIR, photoName)
            val photoFile = File(photoPath)

            if (photoFile.exists()) {
                val outputDirectory = File(activity.applicationInfo.dataDir)
                val newPhotoFile = moveFile(photoFile, outputDirectory, "${selectedContact.name.replace(" ","_")}.jpg")
                if (newPhotoFile.exists()) {
                    this.photoPath = newPhotoFile.path
                    Toast.makeText(activity, "saved in ${this.photoPath}", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "photo saved in ${this.photoPath}")
                }
            } else {
                Toast.makeText(activity, "photo doesn't exist", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Throws(IOException::class)
    private fun moveFile(inputFile: File, outputDir: File, fileName: String): File {
        val newFile = File(outputDir, fileName)
        var outputChannel: FileChannel? = null
        var inputChannel: FileChannel? = null
        try {
            outputChannel = FileOutputStream(newFile).channel
            inputChannel = FileInputStream(inputFile).channel
            inputChannel.transferTo(0, inputChannel.size(), outputChannel)
            inputChannel.close()
            inputFile.delete()
        } finally {
            if (inputChannel != null) inputChannel.close()
            if (outputChannel != null) outputChannel.close()
        }

        return newFile

    }

    private fun setPhoto(imgView: ImageView, bitmap: Bitmap) {
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        imgView.setImageBitmap(bitmap)
    }

    private fun bindContact(contact: Contact?, dialog: Dialog) {
        if (contact == null) return
        dialog.dialog_contact_name.setText(contact.name)
        dialog.dialog_contact_num.setText(contact.number)
    }

    private fun activateListener(listener: ManageContactListener?,
                                 buttonPositive: Button,
                                 buttonNegative: Button,
                                 buttonAddPhoto: ImageView) {
        buttonNegative.setOnClickListener { dialog.dismiss() }

        buttonPositive.setOnClickListener {
            if (isInputValid()) {
                contactDb.contactDao()
                        .saveContact(getContactData())
                dialog.dismiss()
                listener?.onSaveContactSuccess()
            }
        }

        buttonAddPhoto.setOnClickListener { pickPhoto() }
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
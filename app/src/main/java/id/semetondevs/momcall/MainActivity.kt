package id.semetondevs.momcall

import android.Manifest
import android.app.Activity
import android.arch.persistence.room.Room
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.SwipeDirection
import id.semetondevs.momcall.database.Contact
import id.semetondevs.momcall.database.ContactDatabase
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val RC_SELECT_CONTACT = 1
    }

    private var selectedContact: Contact? = null
    private var listContact: List<Contact>? = null
    val contactDb: ContactDatabase by lazy {
        Room.databaseBuilder(this, ContactDatabase::class.java, "contact_db")
            .allowMainThreadQueries()
            .build()
    }
    private val cardStackAdapter: ContactAdapter by lazy {
        ContactAdapter(this, object: ContactAdapter.ContactEditListener{
            override fun onEditClick(contact: Contact) {
                showManageContactDialog(contact)
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (listContact == null) listContact = getContacts(contactDb)
        setupCardStack(card_stack_view)
        setSelectedContact(listContact, card_stack_view.topIndex)

        btn_voice_call.setOnClickListener {
            if (selectedContact != null) {
                doVoiceCall(selectedContact?.voiceCallId ?: 0)
            }
        }

        btn_video_call.setOnClickListener {
            if (selectedContact != null) {
                doVideoCall(selectedContact?.videoCallId ?: 0)
            }
        }

        btn_add_contact.setOnClickListener { selectContact() }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SELECT_CONTACT && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            val cursor = contentResolver.query(uri, null, null, null, null)

            cursor.moveToFirst()
            val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneNo = cursor.getString(phoneIndex)
            val name = cursor.getString(nameIndex)
            cursor.close()

            val findWhatsAppContact = findWhatsAppContact(phoneNo, name)
            if (findWhatsAppContact == null) {
                Toast.makeText(this, "contact has no whatsapp account", Toast.LENGTH_SHORT).show()
            } else {
                showManageContactDialog(findWhatsAppContact)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == RC_SELECT_CONTACT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectContact()
            } else {
                requestContactPermission()
            }
        }
    }

    private fun showManageContactDialog(contact: Contact) {
        val dialog = ManageContactDialogFragment.newInstance(contact)
        dialog.setManageContactListener(object : ManageContactDialogFragment.ManageContactListener{
            override fun onSaveContactSuccess() {
                resetContactStack(card_stack_view, true)
                Toast.makeText(this@MainActivity, "Save success!", Toast.LENGTH_SHORT).show()
            }

            override fun onContactDeleteSuccess() {
                resetContactStack(card_stack_view, true)
                Toast.makeText(this@MainActivity, "Contact deleted", Toast.LENGTH_SHORT).show()
            }

            override fun onUpdateContactSuccess() {
                resetContactStack(card_stack_view, true)
                Toast.makeText(this@MainActivity, "Update success!", Toast.LENGTH_SHORT).show()
            }
        })
        dialog.show(supportFragmentManager, "ManageContactDialogFragment")
    }

    private fun setSelectedContact(listContact: List<Contact>?, currentPos: Int) {
        if (listContact?.isEmpty() != false) {
            showNoContact()
            return
        }

        selectedContact = listContact[currentPos]
    }

    private fun showNoContact() {
        Toast.makeText(this, "no contact found", Toast.LENGTH_SHORT).show()
    }

    private fun getContacts(contactDb: ContactDatabase): List<Contact> {
        return contactDb.contactDao().getAllContact()
    }

    private fun resetContactStack(card: CardStackView, reloadData: Boolean) {
        card.visibility = View.GONE

        if (reloadData) {
            listContact = getContacts(contactDb)
            cardStackAdapter.clear()
            cardStackAdapter.addAll(listContact)
        }

        Handler().postDelayed(Runnable {
            card.setAdapter(cardStackAdapter)
            cardStackAdapter.notifyDataSetChanged()
            card.visibility = View.VISIBLE
        }, 50)
    }

    private fun setupCardStack(card: CardStackView) {
        card.setAdapter(cardStackAdapter)
        cardStackAdapter.addAll(listContact)
        cardStackAdapter.notifyDataSetChanged()

        card.setCardEventListener(object : CardStackView.CardEventListener {
            override fun onCardSwiped(direction: SwipeDirection?) {
                val selectedIdx = if (card.topIndex == listContact?.size) 0 else card.topIndex
                setSelectedContact(listContact, selectedIdx)

                if (card.topIndex == listContact?.size) {
                    resetContactStack(card, false)
                }
            }

            override fun onCardDragging(percentX: Float, percentY: Float) { }

            override fun onCardReversed() { }

            override fun onCardMovedToOrigin() { }

            override fun onCardClicked(index: Int) { }
        })
    }

    private fun selectContact() {
        if (!hasContactPermission()) {
            requestContactPermission()
        } else {
            val contactPickerIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            startActivityForResult(contactPickerIntent, RC_SELECT_CONTACT)
        }
    }

    private fun hasContactPermission(): Boolean = !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)

    private fun requestContactPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Need Contact Permission")
            builder.setMessage("This app needs contact permission.")
            builder.setPositiveButton("Grant", { dialog, _ ->
                dialog.cancel()
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_CONTACTS), RC_SELECT_CONTACT)
            })
            builder.setNegativeButton("Cancel", { dialog, _ -> dialog.cancel() })
            builder.show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), RC_SELECT_CONTACT)
        }
    }

    private fun doVoiceCall(id: Long) {
        if (id <= 0) return
        val intentCall = Intent()
        intentCall.action = Intent.ACTION_VIEW
        intentCall.setDataAndType(Uri.parse(
                "content://com.android.contacts/data/$id"),
                "vnd.android.cursor.item/vnd.com.whatsapp.voip.call")
        intentCall.`package` = "com.whatsapp"
        startActivity(intentCall)
    }

    private fun doVideoCall(id: Long) {
        if (id <= 0) return
        val intentCall = Intent()
        intentCall.action = Intent.ACTION_VIEW
        intentCall.setDataAndType(Uri.parse(
                "content://com.android.contacts/data/$id"),
                "vnd.android.cursor.item/vnd.com.whatsapp.video.call")
        intentCall.`package` = "com.whatsapp"
        startActivity(intentCall)
    }

    private fun findWhatsAppContact(contactNumber: String, displayName: String): Contact? {
        val mimeTypeVoice = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
        val mimeTypeVideo = "vnd.android.cursor.item/vnd.com.whatsapp.video.call"
        val selection = "((${ContactsContract.Data.MIMETYPE} =? OR ${ContactsContract.Data.MIMETYPE} =?) AND ${ContactsContract.Data.DISPLAY_NAME} =?)"
        val args = arrayOf(mimeTypeVideo, mimeTypeVoice, displayName)
        val cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, selection, args, ContactsContract.Contacts.DISPLAY_NAME)

        var contact: Contact? = null
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID))
            val contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME))
            val mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE))

            if (contact == null) contact = Contact()
            contact.name = contactName
            contact.number = contactNumber
            if (mimeType == mimeTypeVideo) {
                contact.videoCallId = id
            } else {
                contact.voiceCallId = id
            }

            Log.d(TAG, contact.toString())
        }

        cursor.close()

        return contact
    }

}

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
import android.view.Menu
import android.view.MenuItem
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

    private val cardStackAdapter: ContactAdapter by lazy { ContactAdapter(this) }
    private var selectedContact: Contact? = null
    val contactDb: ContactDatabase by lazy {
        Room.databaseBuilder(this, ContactDatabase::class.java, "contact_db")
            .allowMainThreadQueries()
            .build()
    }
    private val listContact: List<Contact> by lazy { getContacts(contactDb) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupCardStack(card_stack_view)

        selectedContact = listContact[card_stack_view.topIndex]
        btn_voice_call.setOnClickListener { _ ->
            if (selectedContact != null) {
                doVoiceCall(selectedContact?.voiceCallId ?: 0)
            }
        }

        btn_video_call.setOnClickListener { _ ->
            if (selectedContact != null) {
                doVideoCall(selectedContact?.voiceCallId ?: 0)
            }
        }

        btn_setting.setOnClickListener { _ ->
            selectContact()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                selectContact()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
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
                Toast.makeText(this, name, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Failed to select contact", Toast.LENGTH_SHORT).show()
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

    private fun setupDb(contactDb: ContactDatabase) {
        contactDb.contactDao()
                .saveContact(Contact(1, 3305, 3306, "Made Awidiya", "085737546xxx", null, "http://picsum.photos/450/650/?image=1012"))

        contactDb.contactDao()
                .getAllContact()
                .forEach { Log.d(TAG, "got contact $it") }
    }

    private fun getContacts(contactDb: ContactDatabase): List<Contact> {
        /*return Arrays.asList(
                Contact(1, 3305, 3306, "Made Awidiya", "085737546xxx", null, "http://picsum.photos/450/650/?image=1012"),
                Contact(2, 1, 1, "Gede Mancung", "085737546xxx", null, "http://picsum.photos/450/650/?image=1027"),
                Contact(3, 1, 2, "Komang Ganteng", "085737546xxx", null, "http://picsum.photos/450/650/?image=1005"),
                Contact(4, 1, 2, "Ketut Gaul", "085737546xxx", null, "http://picsum.photos/450/650/?image=1010"),
                Contact(5, 1, 2, "Wayan Mabuk", "085737546xxx", null, "http://picsum.photos/450/650/?image=1025"),
                Contact(6, 1, 2, "Kadek Manis", "085737546xxx", null, "http://picsum.photos/450/650/?image=996"))*/
        contactDb.contactDao()
                .saveContact(Contact(1, 3305, 3306, "Made Awidiya", "085737546xxx", null, "http://picsum.photos/450/650/?image=1012"))

        return contactDb.contactDao().getAllContact()
    }

    private fun setupCardStack(card: CardStackView) {
        card.setAdapter(cardStackAdapter)
        cardStackAdapter.addAll(listContact)
        cardStackAdapter.notifyDataSetChanged()

        card.setCardEventListener(object : CardStackView.CardEventListener {
            override fun onCardDragging(percentX: Float, percentY: Float) {
                Log.d(TAG, "onCardDragging")
            }

            override fun onCardSwiped(direction: SwipeDirection?) {
                Log.d(TAG, "onCardSwiped top index ${card.topIndex}")

                var selectedIdx = if (card.topIndex == listContact.size) 0 else card.topIndex
                selectedContact = listContact[selectedIdx]

                if (card.topIndex == listContact.size) {
                    card.visibility = View.GONE
                    Handler().postDelayed(Runnable {
                        card.setAdapter(cardStackAdapter)
                        card.visibility = View.VISIBLE
                    }, 50)
                }
            }

            override fun onCardReversed() {
                Log.d(TAG, "onCardReversed")
            }

            override fun onCardMovedToOrigin() {
                Log.d(TAG, "onCardMovedToOrigin")
            }

            override fun onCardClicked(index: Int) {
                Log.d(TAG, "onCardClicked pos = $index")
                Toast.makeText(this@MainActivity, listContact[index].name, Toast.LENGTH_SHORT).show()
            }
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

package id.semetondevs.momcall

import android.app.Activity
import android.arch.persistence.room.Room
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
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
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val RC_SELECT_CONTACT = 1
    }

    private val listContact: List<Contact> by lazy { getContacts() }
    private val cardStackAdapter: ContactAdapter by lazy { ContactAdapter(this) }
    private var selectedContact: Contact? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupCardStack(card_stack_view)

        selectedContact = listContact[card_stack_view.topIndex]
        btn_voice_call.setOnClickListener { _ ->
            if (selectedContact != null) {
                doVoiceCall(selectedContact?.voiceCallId ?:0)
            }
        }

        btn_video_call.setOnClickListener { _ ->
            if (selectedContact != null) {
                doVideoCall(selectedContact?.voiceCallId ?:0)
            }
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

            Log.d(TAG, "got it! $name -> $phoneNo")
        } else {
            Toast.makeText(this, "Failed to select contact", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDb() {
        val contactDb = Room.databaseBuilder(this, ContactDatabase::class.java, "contact_db")
                .allowMainThreadQueries()
                .build()

        contactDb.contactDao()
                .saveContact(Contact(1,3305, 3306, "Made Awidiya", "085737546xxx", null, "http://picsum.photos/450/650/?image=1012"))

        contactDb.contactDao()
                .getAllContact()
                .forEach { Log.d(TAG, "got contact $it") }
    }

    private fun getContacts(): List<Contact> {
        return Arrays.asList(
                Contact(1,3305, 3306, "Made Awidiya", "085737546xxx", null, "http://picsum.photos/450/650/?image=1012"),
                Contact(2,1, 1, "Gede Mancung", "085737546xxx", null, "http://picsum.photos/450/650/?image=1027"),
                Contact(3, 1, 2, "Komang Ganteng", "085737546xxx", null, "http://picsum.photos/450/650/?image=1005"),
                Contact(4, 1, 2, "Ketut Gaul", "085737546xxx", null, "http://picsum.photos/450/650/?image=1010"),
                Contact(5, 1, 2, "Wayan Mabuk", "085737546xxx", null, "http://picsum.photos/450/650/?image=1025"),
                Contact(6, 1, 2, "Kadek Manis", "085737546xxx", null, "http://picsum.photos/450/650/?image=996"))
    }

    private fun setupCardStack(card: CardStackView) {
        card.setAdapter(cardStackAdapter)
        cardStackAdapter.addAll(listContact)
        cardStackAdapter.notifyDataSetChanged()

        card.setCardEventListener(object: CardStackView.CardEventListener{
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
        val contactPickerIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        startActivityForResult(contactPickerIntent, RC_SELECT_CONTACT)
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

    private fun scanContact() {
        val contentResolver = this.contentResolver
        val cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI,
                null, null, null,
                ContactsContract.Contacts.DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID))
            val displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME))
            val mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE))
            Log.d("MainActivity", "$displayName | $id | $mimeType")
        }

        cursor.close()
    }

}

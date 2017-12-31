package id.semetondevs.momcall

import android.net.Uri

data class Contact(val waVoiceCallId: Long,
                   val waVideoCallId: Long,
                   val name: String,
                   val number: String,
                   val photo: Uri?,
                   val photoUrl: String?)
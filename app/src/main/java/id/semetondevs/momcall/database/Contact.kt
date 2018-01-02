package id.semetondevs.momcall.database

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import java.io.File

@Entity(tableName = "contact") 
data class Contact(
        @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long?,
        @ColumnInfo(name = "voice_call_Id") var voiceCallId: Long?,
        @ColumnInfo(name = "video_call_id") var videoCallId: Long?,
        @ColumnInfo(name = "name") var name: String,
        @ColumnInfo(name = "number") var number: String,
        @ColumnInfo(name = "photo_path") var photo: String?,
        var photoUrl: String?) : Parcelable {

    @Ignore constructor() : this(null, 0, 0, "", "", "", "")

    fun getPhotoUri(): Uri? {
        if (this.photo != null && !this.photo!!.isEmpty()) {
            val photoFile = File(this.photo)
            if (photoFile.exists()) {
                return Uri.fromFile(photoFile)
            }

            return null
        }

        return null
    }

    fun isEditMode(): Boolean = this.id != null || this.id != 0L

    constructor(source: Parcel) : this(
            source.readValue(Long::class.java.classLoader) as Long?,
            source.readValue(Long::class.java.classLoader) as Long?,
            source.readValue(Long::class.java.classLoader) as Long?,
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeValue(id)
        writeValue(voiceCallId)
        writeValue(videoCallId)
        writeString(name)
        writeString(number)
        writeString(photo)
        writeString(photoUrl)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Contact> = object : Parcelable.Creator<Contact> {
            override fun createFromParcel(source: Parcel): Contact = Contact(source)
            override fun newArray(size: Int): Array<Contact?> = arrayOfNulls(size)
        }
    }    
    
}
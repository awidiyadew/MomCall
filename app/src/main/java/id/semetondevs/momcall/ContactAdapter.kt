package id.semetondevs.momcall

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import id.semetondevs.momcall.database.Contact


class ContactAdapter(context: Context, private val contactEditListener: ContactEditListener?) : ArrayAdapter<Contact>(context, 0) {

    interface ContactEditListener {
        fun onEditClick(contact: Contact)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ContactVH?
        var view = convertView

        if (view == null) {
            view = LayoutInflater.from(context)
                    .inflate(R.layout.item_contact, parent, false)
            holder = ContactVH(view!!)
            view.tag = holder
        } else {
            holder = view.tag as ContactVH
        }

        holder.bind(getItem(position), contactEditListener)
        return view
    }

    internal class ContactVH(view: View) {
        private val tvContactName: TextView = view.findViewById<TextView>(R.id.tv_contact_name) as TextView
        private val tvContactNum: TextView = view.findViewById<TextView>(R.id.tv_contact_number) as TextView
        private val ivContactPhoto: ImageView = view.findViewById<ImageView>(R.id.iv_contact_photo) as ImageView
        private val btnEditContact: View = view.findViewById<View>(R.id.btn_edit_contact) as View

        fun bind(contact: Contact, listener: ContactEditListener?) {
            tvContactName.text = contact.name
            tvContactNum.text = contact.number
            btnEditContact.setOnClickListener { listener?.onEditClick(contact) }
            ivContactPhoto.loadFromPath(contact.photo, R.drawable.icn_nopicture)
        }

    }

}
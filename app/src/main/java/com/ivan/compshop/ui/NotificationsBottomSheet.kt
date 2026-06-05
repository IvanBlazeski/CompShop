package com.ivan.compshop.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ivan.compshop.R

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val time: String = "",
    val isRead: Boolean = false
)

class NotificationsBottomSheet : BottomSheetDialogFragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadNotifications(view)
    }

    private fun loadNotifications(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.notificationsContainer)
        val tvCount = view.findViewById<TextView>(R.id.tvNotificationCount)
        val btnMarkAll = view.findViewById<TextView>(R.id.btnMarkAllRead)

        val userId = auth.currentUser?.email ?: auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                val notifications = snapshot.documents.mapNotNull { doc ->
                    AppNotification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        time = doc.getTimestamp("createdAt")?.toDate()?.let {
                            android.text.format.DateUtils.getRelativeTimeSpanString(
                                it.time,
                                System.currentTimeMillis(),
                                android.text.format.DateUtils.MINUTE_IN_MILLIS
                            ).toString()
                        } ?: "",
                        isRead = doc.getBoolean("isRead") ?: false
                    )
                }

                val unreadCount = notifications.count { !it.isRead }
                tvCount?.text = if (unreadCount > 0) "$unreadCount new" else "All read"

                container?.removeAllViews()

                if (notifications.isEmpty()) {
                    val emptyView = TextView(requireContext()).apply {
                        text = "No notifications yet"
                        textSize = 14f
                        setTextColor(android.graphics.Color.parseColor("#80FFFFFF"))
                        gravity = android.view.Gravity.CENTER
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 32, 0, 32)
                        layoutParams = params
                    }
                    container?.addView(emptyView)
                } else {
                    notifications.forEach { notification ->
                        val itemView = createNotificationItem(notification)
                        container?.addView(itemView)
                    }
                }

                btnMarkAll?.setOnClickListener {
                    notifications.forEach { notif ->
                        firestore.collection("users")
                            .document(userId)
                            .collection("notifications")
                            .document(notif.id)
                            .update("isRead", true)
                    }
                }
            }
    }

    private fun createNotificationItem(notification: AppNotification): View {
        val context = requireContext()

        val item = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.btn_social_neon)
            setPadding(48, 40, 48, 40)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
        }

        val tvTitle = TextView(context).apply {
            text = notification.title
            textSize = 14f
            setTextColor(
                if (!notification.isRead) android.graphics.Color.WHITE
                else android.graphics.Color.parseColor("#80FFFFFF")
            )
            setTypeface(null, android.graphics.Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 4)
            layoutParams = params
        }

        val tvMessage = TextView(context).apply {
            text = notification.message
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#99FFFFFF"))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 4)
            layoutParams = params
        }

        val tvTime = TextView(context).apply {
            text = notification.time
            textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#00D4FF"))
        }

        // Unread indicator
        if (!notification.isRead) {
            val dot = View(context).apply {
                val params = LinearLayout.LayoutParams(16, 16)
                params.setMargins(0, 0, 0, 8)
                layoutParams = params
                setBackgroundResource(R.drawable.btn_neon_gradient)
            }
            item.addView(dot)
        }

        item.addView(tvTitle)
        item.addView(tvMessage)
        item.addView(tvTime)

        return item
    }
}
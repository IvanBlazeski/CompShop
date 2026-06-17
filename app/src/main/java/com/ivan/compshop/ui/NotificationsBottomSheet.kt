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
    val isRead: Boolean = false,
    val isGlobal: Boolean = false
)

class NotificationsBottomSheet : BottomSheetDialogFragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var personalNotifications = listOf<AppNotification>()
    private var globalNotifications = listOf<AppNotification>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPersonalNotifications(view)
        loadGlobalNotifications(view)
    }

    private fun loadGlobalNotifications(view: View) {
        val userId = auth.currentUser?.email ?: auth.currentUser?.uid ?: return

        firestore.collection("globalNotifications")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                firestore.collection("users")
                    .document(userId)
                    .collection("readGlobalNotifications")
                    .get()
                    .addOnSuccessListener { readSnapshot ->
                        val readIds = readSnapshot.documents.map { it.id }.toSet()
                        globalNotifications = snapshot.documents.mapNotNull { doc ->
                            if (readIds.contains(doc.id)) return@mapNotNull null
                            AppNotification(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                message = doc.getString("message") ?: "",
                                time = doc.getTimestamp("createdAt")?.toDate()?.let {
                                    android.text.format.DateUtils.getRelativeTimeSpanString(
                                        it.time, System.currentTimeMillis(),
                                        android.text.format.DateUtils.MINUTE_IN_MILLIS
                                    ).toString()
                                } ?: "",
                                isRead = false,
                                isGlobal = true
                            )
                        }
                        updateUI(view)
                    }
            }
    }

    private fun loadPersonalNotifications(view: View) {
        val userId = auth.currentUser?.email ?: auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                personalNotifications = snapshot.documents.mapNotNull { doc ->
                    AppNotification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        time = doc.getTimestamp("createdAt")?.toDate()?.let {
                            android.text.format.DateUtils.getRelativeTimeSpanString(
                                it.time, System.currentTimeMillis(),
                                android.text.format.DateUtils.MINUTE_IN_MILLIS
                            ).toString()
                        } ?: "",
                        isRead = doc.getBoolean("isRead") ?: false,
                        isGlobal = false
                    )
                }
                updateUI(view)
            }
    }

    private fun updateUI(view: View) {
        if (!isAdded) return
        val container = view.findViewById<LinearLayout>(R.id.notificationsContainer)
        val tvCount = view.findViewById<TextView>(R.id.tvNotificationCount)
        val btnMarkAll = view.findViewById<TextView>(R.id.btnMarkAllRead)
        val userId = auth.currentUser?.email ?: auth.currentUser?.uid ?: return

        val allNotifications = (globalNotifications + personalNotifications)
        val unreadCount = personalNotifications.count { !it.isRead } + globalNotifications.size
        tvCount?.text = if (unreadCount > 0) "$unreadCount new" else "All read"

        container?.removeAllViews()

        if (allNotifications.isEmpty()) {
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
            allNotifications.forEach { notification ->
                container?.addView(createNotificationItem(notification))
            }
        }

        btnMarkAll?.setOnClickListener {
            val userId2 = auth.currentUser?.email ?: auth.currentUser?.uid ?: return@setOnClickListener

            personalNotifications.forEach { notif ->
                firestore.collection("users")
                    .document(userId2)
                    .collection("notifications")
                    .document(notif.id)
                    .update("isRead", true)
            }

            val batch = firestore.batch()
            globalNotifications.forEach { notif ->
                val ref = firestore.collection("users")
                    .document(userId2)
                    .collection("readGlobalNotifications")
                    .document(notif.id)
                batch.set(ref, mapOf("readAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
            }
            batch.commit().addOnSuccessListener {
                personalNotifications = personalNotifications.map { it.copy(isRead = true) }
                globalNotifications = emptyList()
                updateUI(view)
                loadGlobalNotifications(view)
            }
        }
    }

    private fun createNotificationItem(notification: AppNotification): View {
        val context = requireContext()
        val userId = auth.currentUser?.email ?: auth.currentUser?.uid ?: return View(context)

        // Outer wrapper
        val wrapper = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.btn_social_neon)
            setPadding(48, 40, 24, 40)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Content
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        if (!notification.isRead) {
            val dot = View(context).apply {
                val params = LinearLayout.LayoutParams(16, 16)
                params.setMargins(0, 0, 0, 8)
                layoutParams = params
                setBackgroundResource(R.drawable.btn_neon_gradient)
            }
            content.addView(dot)
        }

        val tvTitle = TextView(context).apply {
            text = notification.title
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
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
            text = if (notification.isGlobal) "📢 ${notification.time}" else notification.time
            textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#00D4FF"))
        }

        content.addView(tvTitle)
        content.addView(tvMessage)
        content.addView(tvTime)

        // Delete button
        val btnDelete = TextView(context).apply {
            text = "✕"
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#FF4081"))
            gravity = android.view.Gravity.CENTER
            val params = LinearLayout.LayoutParams(48, 48)
            params.setMargins(16, 0, 0, 0)
            layoutParams = params
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (notification.isGlobal) {
                    // Означи глобалната како прочитана (сокриј ја)
                    firestore.collection("users")
                        .document(userId)
                        .collection("readGlobalNotifications")
                        .document(notification.id)
                        .set(mapOf("readAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                } else {
                    // Избриши персонална нотификација
                    firestore.collection("users")
                        .document(userId)
                        .collection("notifications")
                        .document(notification.id)
                        .delete()
                }
            }
        }

        wrapper.addView(content)
        wrapper.addView(btnDelete)

        return wrapper
    }
}
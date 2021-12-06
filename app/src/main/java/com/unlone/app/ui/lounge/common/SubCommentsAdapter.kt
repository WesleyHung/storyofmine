package com.unlone.app.ui.lounge.common

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.unlone.app.instance.Report
import com.unlone.app.utils.convertTimeStamp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.collections.ArrayList

import com.unlone.app.R
import com.unlone.app.databinding.RecyclerviewSubcommentBinding
import com.unlone.app.instance.Comment
import com.unlone.app.instance.SubComment
import com.unlone.app.utils.CommentDiffUtil
import com.unlone.app.utils.SubCommentDiffUtil


class SubCommentsAdapter(
    private val context:Context,
    private val pid: String,
    private val onLikeCallback: (SubComment) -> Unit,
    private val onFocusEdittextCallback: (String, String) -> Unit
) :
    RecyclerView.Adapter<SubCommentsAdapter.ViewHolder>() {

    private var subCommentList = emptyList<SubComment>()
    private lateinit var recyclerView: RecyclerView
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var mFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    class ViewHolder(val binding: RecyclerviewSubcommentBinding) :
        RecyclerView.ViewHolder(binding.root)


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RecyclerviewSubcommentBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager) DataViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("TAG", "DISPLAYING SUB COMMENTS: $subCommentList")
        holder.binding.username.text = subCommentList[position].username
        holder.binding.date.text =
            subCommentList[position].timestamp?.let { convertTimeStamp(it, "COMMENT") }
        holder.binding.comment.text = subCommentList[position].content

        // init "like" button
        holder.binding.likeButton.setOnClickListener {
            onLikeCallback(subCommentList[position])
        }
        isLiked(holder.binding.likeButton, subCommentList[position])

        // init "more" button
        holder.binding.moreButton.setOnClickListener { v: View ->
            showMenu(
                v,
                R.menu.comment_popup_menu,
                subCommentList[position],
                holder.binding.cardView
            )
        }
        holder.binding.commentButton.setOnClickListener {
            val repliedName = subCommentList[position].username
            val repliedCid = subCommentList[position].parent_cid
            if (repliedName != null && repliedCid != null) {
                onFocusEdittextCallback(repliedCid, repliedName)
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = subCommentList.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }


    fun setSubCommentList(newSubCommentList: List<SubComment>) {
        Log.d("TAG", "setSubCommentList oldSubCommentList: ${this.subCommentList}")
        Log.d("TAG", "setSubCommentList newSubCommentList: $newSubCommentList")
        val diffUtil = SubCommentDiffUtil(this.subCommentList, newSubCommentList)
        val diffResults = DiffUtil.calculateDiff(diffUtil)
        this.subCommentList = newSubCommentList
        diffResults.dispatchUpdatesTo(this)
    }


    private fun isLiked(likeButton: ImageView, subComment: SubComment) {
        mFirestore.collection("posts").document(pid)
            .collection("comments").document(subComment.cid!!)
            .collection("likes").whereEqualTo("likedBy", mAuth.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("TAG", "Listen failed.", e)
                    return@addSnapshotListener
                }
                val likeList = ArrayList<String>()
                for (doc in snapshot!!) {
                    doc.getString("likedBy")?.let {
                        likeList.add(it)
                    }
                }
                assert(likeList.size <= 1)
                Log.d(TAG, "People who has liked: $likeList")

                if (likeList.size == 1) {    // user has liked
                    likeButton.setImageResource(R.drawable.ic_heart_filled)
                    likeButton.tag = "liked"
                } else {
                    likeButton.setImageResource(R.drawable.ic_heart)
                    likeButton.tag = "like"
                }
            }
    }

    private fun showMenu(
        v: View,
        @MenuRes menuRes: Int,
        subComment: SubComment,
        commentView: MaterialCardView
    ) {
        val popup = PopupMenu(context, v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                R.id.action_comment_report -> {
                    context.let { it ->
                        val reportMap = mapOf(
                            it.getString(R.string.hate_speech) to "Hate Speech",
                            it.getString(R.string.span_or_irrelevant) to "Span or Irrelevant",
                            it.getString(R.string.sexual_or_inappropriate) to "Sexual or Inappropriate",
                            it.getString(R.string.just_dont_like) to "I just don’t like it"
                        )
                        val singleItems = reportMap.keys.toList().toTypedArray()
                        var checkedItem = 1

                        // show dialog
                        MaterialAlertDialogBuilder(it, R.style.ThemeOverlay_App_MaterialAlertDialog)
                            .setTitle(it.getString(R.string.why_report))
                            .setNeutralButton(it.getString(R.string.cancel)) { _, _ ->
                                // Respond to neutral button press
                            }
                            .setPositiveButton(it.getString(R.string.report)) { _, _ ->
                                // Respond to positive button press
                                Log.d("TAG", singleItems[checkedItem])
                                val report = mAuth.uid?.let { it1 ->
                                    Report.SubCommentReport(
                                        subComment = subComment,
                                        reportReason = reportMap[singleItems[checkedItem]],
                                        reportedBy = it1
                                    )
                                }

                                Log.d("TAG", report.toString())
                                if (report != null) {
                                    mFirestore.collection("reports")
                                        .add(report)
                                        .addOnSuccessListener {
                                            Log.d(
                                                "TAG",
                                                "Report DocumentSnapshot successfully written!"
                                            )
                                            showConfirmation(commentView)
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w(
                                                "TAG",
                                                "Error saving post\n",
                                                e
                                            )
                                        }
                                }

                            }// Single-choice items (initialized with checked item)
                            .setSingleChoiceItems(singleItems, checkedItem) { dialog, which ->
                                // Respond to item chosen
                                Log.d("TAG", which.toString())
                                checkedItem = which

                            }
                            .show()
                    }

                    true
                }
                else -> false
            }
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

    private fun showConfirmation(commentView: MaterialCardView) {
        context.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(it.getString(R.string.thank_you))
                .setMessage(it.getString(R.string.report_text))
                .setPositiveButton(it.getString(R.string.confirm)) { dialog, which ->
                    // Hide the comment
                    commentView.visibility = View.GONE
                }
                .show()
        }
    }
}
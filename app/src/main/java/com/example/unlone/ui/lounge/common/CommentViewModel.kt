package com.example.unlone.ui.lounge.common

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.unlone.instance.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CommentViewModel : ViewModel() {
    private var _comments: MutableLiveData<List<Comment>> = MutableLiveData()
    private var _commentList: MutableList<Comment> = mutableListOf()
    private var mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var mFirestore = FirebaseFirestore.getInstance()
    val comments: LiveData<List<Comment>> = _comments
    private var lastVisible: DocumentSnapshot? = null
    var endOfComments: Boolean = false


    fun loadComments(numberPost: Long, pid: String, loadMore: Boolean = false) {

        if (lastVisible == null || !loadMore) {
            _commentList.clear()
            mFirestore.collection("posts").document(pid).collection("comments")
                .orderBy(
                    "timestamp",
                    Query.Direction.DESCENDING
                )        // will be ranked by AI later
                .limit(numberPost)
                .get()
                .addOnSuccessListener { result ->
                    if (result.size() > 0)
                        lastVisible = result.documents[result.size() - 1]
                    if (result.size() < 5) {
                        endOfComments = true
                    }
                    val lastComment = lastVisible?.toObject<Comment>()
                    Log.d("TAG", "last visible: $lastComment")
                    Log.d("TAG", "number of comments: " + result.size())
                    for (document in result) {
                        Log.d(TAG, document.id + " ==> " + document.data)
                        val comment = document.toObject<Comment>()
                        comment.cid = document.id
                        Log.d("TAG", "comment object: $comment")
                        _commentList.add(comment)
                        Log.d("TAG", "commentsss: " + _comments.value)
                    }
                    _comments.value = _commentList
                }.addOnFailureListener { exception ->
                    Log.d(TAG, "Error getting documents: ", exception)
                }
        } else {
            // run when order more comments
            mFirestore.collection("posts").document(pid).collection("comments")
                .orderBy("score", Query.Direction.DESCENDING)
                .startAfter(lastVisible!!)
                .limit(numberPost)
                .get()
                .addOnSuccessListener { result ->
                    Log.d("TAG", "next result: $result")
                    if (result.size() == 5) {
                        lastVisible = result.documents[result.size() - 1]
                    } else {
                        lastVisible = null
                        endOfComments = true
                    }

                    Log.d("TAG", "number of NEXT comments: " + result.size())
                    for (document in result) {
                        Log.d(TAG, document.id + " ==> " + document.data)
                        val comment = document.toObject<Comment>()
                        comment.cid = document.data.toString()
                        Log.d("TAG", "comment object LOADED: $comment")
                        _commentList.add(comment)
                    }
                    Log.d("TAG", "comments: " + _comments.value)

                    _comments.value = _commentList
                }.addOnFailureListener { exception ->
                    Log.d(TAG, "Error getting documents: ", exception)
                }
        }
    }

    fun processCommentLike(comment: Comment, pid: String) {
        Log.d("TAG", "comment: $comment")

        val docRef = mFirestore.collection("posts").document(pid)
            .collection("comments").document(comment.cid!!)
            .collection("likes")

        CoroutineScope(Dispatchers.IO).launch {
            val result = docRef.get().await()
            val removeDocId = async {
                // Find if the user has already like the comment, unlike it if found
                for (document in result) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                    if (document.getString("likedBy") == mAuth.uid) {
                        // Found the document, remove later
                        return@async document.id
                    }
                }
                return@async null
            }

            if (removeDocId.await() != null){
                // Remove the like document
                docRef.document(removeDocId.await() as String)
                    .delete().await()
            } else{
                // add the like document
                val data = hashMapOf(
                    "likedBy" to mAuth.uid,
                    "timeStamp" to (System.currentTimeMillis() / 1000).toString()
                )
                docRef.add(data).await()
            }
        }
    }
}
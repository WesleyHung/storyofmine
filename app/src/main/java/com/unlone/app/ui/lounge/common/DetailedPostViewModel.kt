package com.unlone.app.ui.lounge.common

import android.content.ContentValues
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.unlone.app.R
import com.unlone.app.model.*
import com.unlone.app.utils.ObservableViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*

class DetailedPostViewModel(val pid: String) : ObservableViewModel() {

    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val mFirestore = FirebaseFirestore.getInstance()
    val uid = mAuth.uid.toString()

    // post field
    private val post: MutableLiveData<Post?> = MutableLiveData()
    val observablePost: LiveData<Post?>
        get() = post
    private val _category: MutableLiveData<String?> = MutableLiveData()
    val category: LiveData<String?> = _category
    private val _isPostSaved: MutableLiveData<Boolean> = MutableLiveData(false)
    val isPostSaved: LiveData<Boolean> = _isPostSaved


    // comments list field
    private val mComments: Long = 4   // how many comment loaded each time
    private var _comments: MutableLiveData<List<Comment>> = MutableLiveData()
    private var _commentList: MutableList<Comment> = mutableListOf()

    private var _uiComments: MutableLiveData<List<UiComment>> = MutableLiveData()
    val uiComments: LiveData<List<UiComment>> = _uiComments

    private var lastVisible: Float? = null
    var endOfComments: Boolean = true

    // type comment edittext field
    private var _commentEditTextFocused: MutableLiveData<Boolean> = MutableLiveData(false)
    val commentEditTextFocused: LiveData<Boolean> = _commentEditTextFocused
    var parentCid: String? = null
    var parentCommenter: String? = null
    val isSelfPost by lazy { post.value?.author_uid == uid }

    // Report
    private val reportMap = mapOf(
        (R.string.hate_speech) to "Hate Speech",
        (R.string.span_or_irrelevant) to "Span or Irrelevant",
        (R.string.sexual_or_inappropriate) to "Sexual or Inappropriate",
        (R.string.just_dont_like) to "I just don’t like it"
    )
    val singleItems = reportMap.keys.toList().toTypedArray()
    var reportSuccessful: Boolean? = null

    init {
        viewModelScope.launch {
            withContext(Dispatchers.Default) { loadPost() }
            getCategoryTitle()
            isSaved()
        }
    }

    private suspend fun loadPost() {
        val documentSnapshot = mFirestore.collection("posts")
            .document(pid)
            .get()
            .await()
        val p = documentSnapshot.toObject<Post>()

        withContext(Dispatchers.Main){
            post.value = p
        }
    }

    fun deletePost(pid: String) {
        mFirestore.collection("posts")
            .document(pid)
            .delete()
            .addOnSuccessListener { Log.d("TAG", "DocumentSnapshot successfully deleted!") }
            .addOnFailureListener { e -> Log.w("TAG", "Error deleting document", e) }
    }

    fun savePost() {
        if (_isPostSaved.value != true) {
            val timestamp =
                hashMapOf("saveTime" to System.currentTimeMillis().toString())
            mAuth.uid?.let { uid ->
                mFirestore.collection("users").document(uid)
                    .collection("saved")
                    .document(pid)
                    .set(timestamp)
                    .addOnSuccessListener {}
                    .addOnFailureListener {}
            }
            _isPostSaved.value = true
        } else {
            // User uncheck chose the "Saving" item, save the post...
            mAuth.uid?.let { uid ->
                mFirestore.collection("users").document(uid)
                    .collection("saved")
                    .document(pid)
                    .delete()
                    .addOnSuccessListener {}
                    .addOnFailureListener {}
            }
            _isPostSaved.value = false
        }
    }

    fun reportPost(checkedItem: Int) {
        val report = Report.PostReport(
            post = post.value,
            reportReason = reportMap[singleItems[checkedItem]],
            reportedBy = uid
        )
        Log.d("TAG", report.toString())
        uploadReport(report)
    }

    fun uploadReport(report: Report) {
        mFirestore.collection("reports")
            .add(report)
            .addOnSuccessListener {
                reportSuccessful = true
            }
            .addOnFailureListener {
                reportSuccessful = false
            }
    }

    private fun isSaved() {
        viewModelScope.launch {
            val result = mFirestore.collection("users").document(mAuth.uid!!)
                .collection("saved")
                .document(pid)
                .get()
                .await()
            _isPostSaved.value = (result != null && result.exists())
        }
    }

    fun loadUiComments(loadMore: Boolean = false) {
        val uiCommentList: ArrayList<UiComment> = ArrayList()
        viewModelScope.launch(Dispatchers.IO) {
            loadComments(loadMore, mComments)
            withContext(Dispatchers.Main) {
                for (comment in _comments.value!!) {
                    val isLiked = fireStoreIsLike(comment)
                    // load sub comments
                    val uiSubComments = loadUiSubComments(comment, mComments)
                    uiCommentList.add(UiComment(comment, isLiked, false, uiSubComments))
                }
                _uiComments.value = uiCommentList.distinct()
            }
        }
    }

    private suspend fun loadComments(
        loadMore: Boolean,
        numberPost: Long
    ) {
        if (lastVisible == null || !loadMore) {
            _commentList.clear()
            withContext(Dispatchers.IO) {
                val commentCollection =
                    mFirestore.collection("posts").document(pid).collection("comments")
                        .orderBy("score", Query.Direction.DESCENDING)
                        .limit(numberPost)
                        .get()
                        .await()
                endOfComments = commentCollection.size() < numberPost

                Log.d("TAG", "number of comments: " + commentCollection.size())
                for (document in commentCollection) {
                    Log.d(ContentValues.TAG, document.id + " ==> " + document.data)
                    val comment = document.toObject<Comment>()
                    comment.cid = document.id
                    val userDoc = mFirestore.collection("users")
                        .document(comment.uid!!)
                        .get()
                        .await()
                    val user = userDoc.toObject(User::class.java)
                    withContext(Dispatchers.Main) {
                        comment.username = user?.username
                        if (comment.referringPid == null) {
                            comment.referringPid = pid
                        }

                        Log.d("TAG", "comment object: $comment")
                        _commentList.add(comment)
                        lastVisible = comment.score
                    }
                }
                // sort the postList
                val sortedCommentList = _commentList.sortedByDescending { it.score }
                withContext(Dispatchers.Main) {
                    Log.d("TAG", "sorted postList: $sortedCommentList")
                    _comments.value = sortedCommentList
                }
            }
        } else {
            // run when order more comments
            Log.d("TAG", "lastVisible: $lastVisible")
            withContext(Dispatchers.IO) {
                val commentCollection =
                    mFirestore.collection("posts").document(pid).collection("comments")
                        .orderBy("score", Query.Direction.DESCENDING)
                        .startAfter(lastVisible)
                        .limit(numberPost)
                        .get()
                        .await()

                endOfComments = commentCollection.size() < numberPost

                Log.d("TAG", "number of NEXT comments: " + commentCollection.size())
                for (document in commentCollection) {
                    Log.d(ContentValues.TAG, document.id + " ==> " + document.data)
                    val comment = document.toObject<Comment>()
                    comment.cid = document.id

                    // check if comment already existed
                    fun List<Comment>.filterByCid(cid: String) = this.filter { it.cid == cid }
                    val containedComments = _commentList.filterByCid(document.id)
                    if (containedComments.isNotEmpty()) {
                        // comment already existed
                        Log.d("TAG", "caught duplicated comment: $comment")
                        continue
                    }
                    val userDoc = mFirestore.collection("users")
                        .document(comment.uid!!)
                        .get()
                        .await()
                    val user = userDoc.toObject(User::class.java)

                    withContext(Dispatchers.Main) {
                        comment.username = user?.username
                        if (comment.referringPid == null) {
                            comment.referringPid = pid
                        }
                        lastVisible = comment.score
                        Log.d("TAG", "comment object LOADED: $comment")
                        _commentList.add(comment)
                    }
                }
                // sort the postList
                val sortedCommentList = _commentList.sortedByDescending { it.score }
                withContext(Dispatchers.Main) {
                    Log.d("TAG", "sorted postList: $sortedCommentList")
                    _comments.value = sortedCommentList
                }
            }
        }
    }

    private suspend fun loadUiSubComments(
        comment: Comment,
        numberPost: Long = mComments
    ): ArrayList<UiSubComment> {
        val uiSubCommentList = ArrayList<UiSubComment>()
        val subCommentList = loadSubComments(comment, numberPost)
        for (sc in subCommentList) {
            val isLiked = fireStoreSubCommentIsLike(sc)
            uiSubCommentList.add(UiSubComment(sc, isLiked))
        }
        return uiSubCommentList
    }

    private suspend fun loadSubComments(
        comment: Comment,
        numberPost: Long = mComments
    ): MutableList<SubComment> {
        val subCommentList: MutableList<SubComment> = mutableListOf()
        val subCommentCollection = comment.cid?.let {
            mFirestore.collection("posts").document(pid)
                .collection("comments").document(it)
                .collection("sub comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
        }

        withContext(Dispatchers.Main) {
            if (subCommentCollection != null) {
                for (sc in subCommentCollection) {
                    Log.d(ContentValues.TAG, sc.id + " => " + sc.data)
                    val subComment = sc.toObject<SubComment>()
                    subComment.cid = sc.id
                    withContext(Dispatchers.IO) {
                        val userDoc = mFirestore.collection("users")
                            .document(subComment.uid!!)
                            .get()
                            .await()
                        val user = userDoc.toObject(User::class.java)
                        withContext(Dispatchers.Main) {
                            subComment.username = user?.username
                            if (subComment.parent_pid == null) {
                                subComment.parent_pid = pid
                            }
                        }
                    }
                    subCommentList.add(subComment)
                }
                subCommentList.sortedByDescending { it.timestamp }
                Log.d(ContentValues.TAG, "comment with sub comments added: $comment")
            }
        }

        return subCommentList
    }

    private suspend fun fireStoreIsLike(comment: Comment): Boolean {
        val result = comment.referringPid?.let {
            mFirestore.collection("posts").document(it)
                .collection("comments").document(comment.cid!!)
                .collection("likes").whereEqualTo("likedBy", mAuth.uid)
                .get()
                .await()
        }

        val likeList = ArrayList<String>()
        for (doc in result!!) {
            doc.getString("likedBy")?.let {
                likeList.add(it)
            }
        }
        assert(likeList.size <= 1)
        Log.d(ContentValues.TAG, "People who has liked: $likeList")

        return likeList.size == 1       // this user has like the comment

    }

    private suspend fun fireStoreSubCommentIsLike(subComment: SubComment): Boolean {
        val result = subComment.parent_pid?.let {
            mFirestore.collection("posts").document(it)
                .collection("comments").document(subComment.parent_cid!!)
                .collection("sub comments").document(subComment.cid!!)
                .collection("likes").whereEqualTo("likedBy", mAuth.uid)
                .get()
                .await()
        }

        val likeList = ArrayList<String>()
        for (doc in result!!) {
            doc.getString("likedBy")?.let {
                likeList.add(it)
            }
        }
        assert(likeList.size <= 1)
        Log.d(ContentValues.TAG, "People who has liked: $likeList")

        return likeList.size == 1       // this user has like the comment

    }

    fun processCommentLike(uiComment: UiComment) {
        val comment = uiComment.comment
        Log.d("TAG", "comment: $comment")

        val docRef = comment.referringPid?.let {
            mFirestore.collection("posts").document(it)
                .collection("comments").document(comment.cid!!)
                .collection("likes")
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = docRef?.get()?.await()
            val removeDocId = async {
                // Find if the user has already like the comment, unlike it if found
                if (result != null) {
                    for (document in result) {
                        Log.d(ContentValues.TAG, "${document.id} => ${document.data}")
                        if (document.getString("likedBy") == mAuth.uid) {
                            return@async document.id
                        }
                    }
                }
                return@async null
            }

            if (removeDocId.await() != null) {
                // Remove the like document
                docRef?.document(removeDocId.await() as String)?.delete()?.await()
                // update the UiComment list
                // this comment is now UNLIKED by the user
                _uiComments.value?.find { it.comment.cid == comment.cid }?.likedByUser = false
            } else {
                // Add the like document
                val data = hashMapOf(
                    "likedBy" to mAuth.uid,
                    "timeStamp" to (System.currentTimeMillis() / 1000).toString()
                )
                val docRefId = docRef?.add(data)?.await()?.id
                // update the UiComment list
                // this comment is now LIKED by the user
                _uiComments.value?.find { it.comment.cid == comment.cid }?.likedByUser = true

            }
        }
    }

    fun processSubCommentLike(subComment: SubComment) {
        Log.d("TAG", "subComment: $subComment")

        val docRef = subComment.cid?.let { commentCid ->
            subComment.parent_cid?.let { parentCid ->
                subComment.parent_pid?.let { ParentPid ->
                    mFirestore.collection("posts").document(ParentPid)
                        .collection("comments").document(parentCid)
                        .collection("sub comments").document(commentCid)
                        .collection("likes")
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val result = docRef?.get()?.await()
            val removeDocId = async {
                // Find if the user has already like the comment, unlike it if found
                if (result != null) {
                    for (document in result) {
                        Log.d(ContentValues.TAG, "${document.id} => ${document.data}")
                        if (document.getString("likedBy") == mAuth.uid) {
                            // Found the document, remove later
                            return@async document.id
                        }
                    }
                }
                return@async null
            }

            if (removeDocId.await() != null) {
                // Remove the like document
                docRef?.document(removeDocId.await() as String)?.delete()?.await()
            } else {
                // add the like document
                val data = hashMapOf(
                    "likedBy" to mAuth.uid,
                    "timeStamp" to (System.currentTimeMillis() / 1000).toString()
                )
                val docRefId = docRef?.add(data)?.await()?.id
                Log.d("TAG", "like doc added: $docRefId")
            }
        }
    }

    fun uploadComment(commentContent: String) {
        if (parentCid == null) {
            // normal comment
            viewModelScope.launch(Dispatchers.IO) {
                val comment = Comment(
                    uid = mAuth.uid!!,
                    content = commentContent,
                    timestamp = System.currentTimeMillis().toString(),
                    referringPid = pid
                )
                mFirestore.collection("posts").document(pid)
                    .collection("comments")
                    .add(comment)
                    .await()
            }
        } else {
            // sub comment
            uploadSubComment(commentContent)
        }
        // clear parent cid and username
        clearSubCommentPrerequisite()
    }

    private fun uploadSubComment(commentContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val subComment = SubComment(
                uid = mAuth.uid!!,
                content = commentContent,
                timestamp = System.currentTimeMillis().toString(),
                parent_cid = parentCid,
                parent_pid = pid
            )
            parentCid?.let {
                mFirestore.collection("posts").document(pid)
                    .collection("comments").document(it)
                    .collection("sub comments")
                    .add(subComment)
                    .await()
            }
        }
    }

    fun focusEdittextToSubComment(parentCid: String, parentCommenter: String) {
        _commentEditTextFocused.value = true
        this.parentCid = parentCid
        this.parentCommenter = parentCommenter
    }

    fun clearSubCommentPrerequisite() {
        _commentEditTextFocused.value = false
        parentCid = null
        parentCommenter = null
    }

    // display topic
    private fun getCategoryTitle() {
        val categoryId = post.value?.category
        val appLanguage = when (Locale.getDefault().language) {
            "zh" -> "zh_hk"          // if the device language is set to Chinese, use chinese text
            else -> "default"        // default language (english)
        }
        viewModelScope.launch {
            _category.value = categoryId?.let {
                mFirestore.collection("categories")
                    .document("pre_defined_categories")
                    .collection("categories_name")
                    .document(it)
                    .get()
                    .await()
                    .data?.get(appLanguage)
            } as String?
        }
    }
}
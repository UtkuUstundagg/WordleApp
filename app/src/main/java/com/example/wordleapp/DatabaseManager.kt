package com.example.wordleapp

import android.content.Context
import android.widget.Toast
import com.example.wordleapp.Models.GameActivityModel
import com.example.wordleapp.Models.GameInviteModel
import com.example.wordleapp.Models.PointModel
import com.example.wordleapp.Models.RematchModel
import com.example.wordleapp.Models.ResultPopupModel
import com.example.wordleapp.Models.UserModel
import com.example.wordleapp.Models.WordModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DatabaseManager {

    private lateinit var userDbRef: DatabaseReference
    private val const = com.example.wordleapp.Models.Constants()

    fun insertUser(user: UserModel){
        userDbRef = FirebaseDatabase.getInstance().getReference("Users")

        user.id = userDbRef.push().key!!
        userDbRef.child(user.id!!).setValue(user)
    }

    fun addUserToChannel(user: UserModel, path:String) {
        userDbRef = FirebaseDatabase.getInstance().getReference(path)
        user.status = const.USER_ACTIVE
        userDbRef.child("ACTIVEUSERS/" + user.id.toString()).setValue(user)
    }

    fun removeUserFromChannel(path:String, user: UserModel) {
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.child(user.id.toString()).removeValue()
    }

    fun listenForUsersAtSameChannel(path: String, callback: (MutableList<UserModel>) -> Unit) {
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        val userList: MutableList<UserModel> = mutableListOf()

        val eventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userList.clear()

                for (snapshot in dataSnapshot.children) {
                    val id = snapshot.child("id").getValue(String::class.java)
                    val username = snapshot.child("username").getValue(String::class.java)
                    val password = snapshot.child("password").getValue(String::class.java)
                    val status = snapshot.child("status").getValue(String::class.java)

                    val userModel = UserModel(id, username, password, status)
                    userList.add(userModel)
                }

                callback(userList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        }

        userDbRef.addValueEventListener(eventListener)
        callback.invoke(userList)
    }

    fun listenInvites(path: String, user: UserModel, callback: (GameInviteModel?) -> Unit) {
        val userDbRef = FirebaseDatabase.getInstance().getReference(path)

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var invite: GameInviteModel? = null

                for (snapshot in dataSnapshot.children) {
                    val receiverId = snapshot.child("receiverId").getValue(String::class.java)
                    val senderId = snapshot.child("senderId").getValue(String::class.java)

                    if (receiverId == user.id || senderId == user.id) {
                        val id = snapshot.child("id").getValue(String::class.java)
                        val senderId = snapshot.child("senderId").getValue(String::class.java)
                        val receiverId = snapshot.child("receiverId").getValue(String::class.java)
                        val inviteStatus = snapshot.child("inviteStatus").getValue(String::class.java)

                        invite = GameInviteModel(id, senderId, receiverId, inviteStatus)
                        break
                    }
                }

                callback(invite)
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        }

        userDbRef.addValueEventListener(valueEventListener)
    }

    fun listenGameActivities(path: String, inviteId: String, callback: (GameActivityModel?) -> Unit) {
        val userDbRef = FirebaseDatabase.getInstance().getReference(path)

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var game: GameActivityModel? = null

                for (snapshot in dataSnapshot.children) {
                    val dbInviteId = snapshot.child("inviteId").getValue(String::class.java)

                    if (dbInviteId == inviteId) {
                        val dbId = snapshot.child("id").getValue(String::class.java)
                        val dbInviteId = snapshot.child("inviteId").getValue(String::class.java)
                        val dbGameStatus = snapshot.child("gameStatus").getValue(String::class.java)
                        val dbFirstUser = snapshot.child("user1").getValue(UserModel::class.java)
                        val dbSecondUser = snapshot.child("user2").getValue(UserModel::class.java)
                        val dbFirstUserGuess = snapshot.child("user1Guess").getValue(WordModel::class.java)
                        val dbSecondUserGuess = snapshot.child("user2Guess").getValue(WordModel::class.java)
                        val dbResultPopup = snapshot.child("resultPopup").getValue(ResultPopupModel::class.java)
                        val dbFirstUserPoint = snapshot.child("user1Point").getValue(PointModel::class.java)
                        val dbSecondUserPoint = snapshot.child("user2Point").getValue(PointModel::class.java)

                        game = GameActivityModel(dbId, dbInviteId, dbGameStatus, dbFirstUser, dbSecondUser,
                            dbFirstUserGuess, dbSecondUserGuess/*, dbResultPopup, dbFirstUserPoint, dbSecondUserPoint*/)
                        break
                    }
                }

                callback(game)
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        }

        userDbRef.addValueEventListener(valueEventListener)
    }


    fun sendInvite(path:String, senderId:String, receiverId:String, context: Context){
        if(senderId == receiverId){
            Toast.makeText(context,"Kendinize Davet Gönderemezsiniz",Toast.LENGTH_SHORT).show()
            return
        }

        userDbRef = FirebaseDatabase.getInstance().getReference(path)
        val id = userDbRef.push().key!!
        userDbRef.child(id).setValue(GameInviteModel(id, senderId, receiverId, const.INVITE_PENDING))
    }

    fun deleteInvite(path:String, gameInviteModel: GameInviteModel){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.child(gameInviteModel.id.toString()).removeValue()
    }

    fun declineInvite(path:String, gameInviteModel: GameInviteModel){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.child(gameInviteModel.id.toString()).child("inviteStatus").setValue(const.INVITE_DECLINED)
    }

    fun acceptInvite(path:String, gameInviteModel: GameInviteModel){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.child(gameInviteModel.id.toString()).child("inviteStatus").setValue(const.INVITE_ACCEPTED)
        createGame(path, gameInviteModel.id!!, gameInviteModel.senderId!!, gameInviteModel.receiverId!!)
    }

    fun createGame(oldpath:String, inviteId: String, firstUserId: String, secondUserId: String){
        val path = changePathToGame(oldpath)

        findUserById(firstUserId){ firstUser->
            findUserById(secondUserId){ secondUser->
                userDbRef = FirebaseDatabase.getInstance().getReference(path)
                val id = userDbRef.push().key!!
                userDbRef.child(id).setValue(GameActivityModel(id, inviteId,const.GAME_STATUS_CREATED, firstUser, secondUser, null, null, null, null, null))

                findInviteById(inviteId,oldpath){foundInvite->
                    deleteInvite(oldpath, foundInvite!!)
                }
            }
        }
    }

    private fun changePathToGame(path:String): String{
        var tempString =""
        var test = path.split("/")
        for (i in 0 until test.size - 1){
            tempString += test.get(i) + "/"
        }
        return tempString + "GAMEACTIVITIES"
    }

    fun findUserByUsernameAndPassword(username: String, password: String, callback: (UserModel?) -> Unit) {
        userDbRef = FirebaseDatabase.getInstance().getReference("Users")

        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var foundUser: UserModel? = null

                for (snapshot in dataSnapshot.children) {
                    val id = snapshot.child("id").getValue(String::class.java)
                    val dbUsername = snapshot.child("username").getValue(String::class.java)
                    val dbPassword = snapshot.child("password").getValue(String::class.java)

                    if (dbUsername == username && dbPassword == password) {
                        foundUser = UserModel(id, dbUsername, dbPassword)
                        break
                    }
                }

                callback(foundUser)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    fun findUserById(id:String, callback: (UserModel?) -> Unit){
        userDbRef = FirebaseDatabase.getInstance().getReference("Users")

        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var foundUser: UserModel? = null

                for (snapshot in dataSnapshot.children) {
                    val dbId = snapshot.child("id").getValue(String::class.java)
                    val dbUsername = snapshot.child("username").getValue(String::class.java)
                    val dbPassword = snapshot.child("password").getValue(String::class.java)
                    val dbStatus = snapshot.child("status").getValue(String::class.java)

                    if (id == dbId) {
                        foundUser = UserModel(id, dbUsername, dbPassword, dbStatus)
                        break
                    }
                }
                callback(foundUser)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    fun findInviteById(id:String, path:String, callback: (GameInviteModel?) -> Unit){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var foundGameInviteModel: GameInviteModel? = null

                for (snapshot in dataSnapshot.children) {
                    val dbId = snapshot.child("id").getValue(String::class.java)
                    val dbSenderId = snapshot.child("senderId").getValue(String::class.java)
                    val dbReceiverId = snapshot.child("receiverId").getValue(String::class.java)
                    val dbInviteStatus = snapshot.child("inviteStatus").getValue(String::class.java)

                    if(id == dbId){
                        foundGameInviteModel = GameInviteModel(dbId, dbSenderId, dbReceiverId, dbInviteStatus)
                        break
                    }
                }
                callback(foundGameInviteModel)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    fun insertWordModelForUser(wordModel: WordModel, user:UserModel, path: String){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val gameActivity = dataSnapshot.getValue(GameActivityModel::class.java)

                if (gameActivity != null) {
                    if (user.id != gameActivity.user1!!.id && user.id == gameActivity.user2!!.id) {
                        wordModel.id = userDbRef.push().key!!
                        userDbRef.child("user1Guess").setValue(wordModel)
                    }
                    if (user.id != gameActivity.user2!!.id && user.id == gameActivity.user1!!.id) {
                        wordModel.id = userDbRef.push().key!!
                        userDbRef.child("user2Guess").setValue(wordModel)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })

    }

    fun checkForApproval(path: String, callback: (Pair<String, String>) -> Unit){
        var winnerText = const.USER_NO_WINNER
        var winnerUsername = ""

        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val gameActivity = dataSnapshot.getValue(GameActivityModel::class.java)

                if (gameActivity!!.user1Guess?.isApproved == const.WORD_APPROVED
                    && gameActivity.user2Guess?.isApproved == const.WORD_APPROVED) {
                    winnerText = const.WORD_APPROVED
                }
                if(gameActivity.user1Guess != null && gameActivity.user2Guess == null){
                    winnerText = const.USER_2_WIN
                    winnerUsername = gameActivity.user2!!.username!!
                }
                if(gameActivity.user1Guess == null && gameActivity.user2Guess != null){
                    winnerText = const.USER_1_WIN
                    winnerUsername = gameActivity.user1!!.username!!
                }
                if(gameActivity.user1Guess == null && gameActivity.user2Guess == null){
                    winnerText = const.USER_NO_WINNER
                }
                callback(Pair(winnerText, winnerUsername))
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    fun deleteGameRoomById(path: String){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.removeValue()
    }

    fun listenGameResultPopup(path: String, callback: (ResultPopupModel?) -> Unit) {
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val resultPopupModel = dataSnapshot.child("resultPopup").getValue(ResultPopupModel::class.java)

                    callback(resultPopupModel)

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }

    fun insertPopupModelV2(path: String, popupModel: ResultPopupModel ){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.hasChild("resultPopup")) {
                    popupModel.id = userDbRef.push().key!!
                    userDbRef.child("resultPopup").setValue(popupModel)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    fun deletePopup(path: String){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.child("resultPopup").removeValue()
    }

    fun findPopupAndSetStatusFalse(path: String){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists()){
                    var popup = dataSnapshot.child("resultPopup").getValue(ResultPopupModel::class.java)
                    if (popup != null) {
                        popup.status = false
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    fun getUserGuessWord(path: String, user: UserModel, callback: (WordModel) -> Unit){
        getGameRoom(path) { gameRoom ->
            if (gameRoom!!.user1!!.id == user.id) {
                val wordModel = WordModel(
                    gameRoom.user1Guess?.id,
                    gameRoom.user1Guess?.isApproved,
                    gameRoom.user1Guess?.char1, null,
                    gameRoom.user1Guess?.char2, null,
                    gameRoom.user1Guess?.char3, null,
                    gameRoom.user1Guess?.char4, null,
                    gameRoom.user1Guess?.char5, null,
                    gameRoom.user1Guess?.char6, null,
                    gameRoom.user1Guess?.char7, null)
                callback(wordModel)
            }
            else if (gameRoom.user2!!.id == user.id) {
                val wordModel = WordModel(
                    gameRoom.user2Guess?.id,
                    gameRoom.user2Guess?.isApproved,
                    gameRoom.user2Guess?.char1, null,
                    gameRoom.user2Guess?.char2, null,
                    gameRoom.user2Guess?.char3, null,
                    gameRoom.user2Guess?.char4, null,
                    gameRoom.user2Guess?.char5, null,
                    gameRoom.user2Guess?.char6, null,
                    gameRoom.user2Guess?.char7, null)
                callback(wordModel)
            }
        }

    }

    private fun getGameRoom(path: String, callback: (GameActivityModel?) -> Unit) {
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val gameActivity = dataSnapshot.getValue(GameActivityModel::class.java)
                callback(gameActivity)
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    fun getEnemyUser(path: String, user: UserModel, callback: (UserModel?) -> Unit) {
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val foundUser = dataSnapshot.getValue(UserModel::class.java)

                if(foundUser!!.id != user.id){
                    callback(foundUser)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })


    }

    fun insertPointsForUser(path: String, point: PointModel){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        point.id = userDbRef.push().key!!
        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val gameActivity = dataSnapshot.getValue(GameActivityModel::class.java)

                if (gameActivity != null) {
                    if (point.userId == gameActivity.user1!!.id) {
                        userDbRef.child("user1Point").setValue(point)
                    }
                    if (point.userId == gameActivity.user2!!.id) {
                        userDbRef.child("user2Point").setValue(point)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    fun updateGameRoomStatus(path: String, status:String){
        userDbRef = FirebaseDatabase.getInstance().getReference(path +"/gameStatus")

        userDbRef.setValue(status)
    }

    fun updateUserStatus(path: String, user: UserModel){
        userDbRef = FirebaseDatabase.getInstance().getReference(path + "/${user.id}/status")

        userDbRef.setValue(user.status)
    }

    fun getBothPoints(path: String, callback: (Pair<PointModel,PointModel>?) -> Unit){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val gameActivity = dataSnapshot.getValue(GameActivityModel::class.java)

                if (gameActivity != null) {
                    val user1Point = gameActivity.user1Point
                    val user2Point = gameActivity.user2Point

                    if (user1Point != null && user2Point != null) {
                        callback(Pair(user1Point, user2Point))
                    }
                    else {
                        callback(null)
                    }
                }
                else {
                    callback(null)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        }

        userDbRef.addValueEventListener(valueEventListener)
    }

    fun insertRematchPopUp(path: String, rematchModel: RematchModel){
        userDbRef = FirebaseDatabase.getInstance().getReference(path + "/rematchPopup")
        rematchModel.id = userDbRef.push().key!!
        userDbRef.setValue(rematchModel)
    }

    fun listenRematchPopup(path: String, callback: (RematchModel?) -> Unit){
        userDbRef = FirebaseDatabase.getInstance().getReference(path+"/rematchPopup")

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val rematchModel = dataSnapshot.getValue(RematchModel::class.java)
                if (rematchModel != null) {
                    callback(rematchModel)
                }
                else {
                    callback(null)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        }

        userDbRef.addValueEventListener(valueEventListener)
    }

    fun findRematchPopupById(path: String, id: String, callback: (RematchModel?) -> Unit){
        userDbRef = FirebaseDatabase.getInstance().getReference(path + "/rematchPopup")

        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var foundRematchModel = dataSnapshot.getValue(RematchModel::class.java)

                callback(foundRematchModel)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    fun updateRematchPopup(path:String, rematchModel: RematchModel){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.child("rematchPopup").setValue(rematchModel)
    }

    fun getBothUsers(path: String, callback: (Pair<UserModel,UserModel>?) -> Unit){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val gameActivity = dataSnapshot.getValue(GameActivityModel::class.java)

                if (gameActivity != null) {
                    val user1= gameActivity.user1
                    val user2 = gameActivity.user2

                    if (user1 != null && user2 != null) {
                        callback(Pair(user1, user2))
                    }
                    else {
                        callback(null)
                    }
                }
                else {
                    callback(null)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    fun createRematch(pathForActivity:String, pathForGameRoom:String, rematchId:String, firstUserId: String, secondUserId: String){
        userDbRef = FirebaseDatabase.getInstance().getReference(pathForActivity)
        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.child(rematchId).exists()) {
                    getBothUsers(pathForGameRoom) { users ->
                        userDbRef = FirebaseDatabase.getInstance().getReference(pathForActivity)
                        userDbRef.child(rematchId).setValue(
                            GameActivityModel(rematchId, rematchId, const.GAME_STATUS_CREATED,
                                users?.first, users?.second, null, null,
                                null, null, null))
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })

    }
    fun findAndDeleteOldGameRoom(path:String){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)
        userDbRef.removeValue()
    }

    fun listenForRematch(path: String, user: UserModel, callback: (GameActivityModel?) -> Unit) {
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (childSnapshot in dataSnapshot.children) {
                    val gameActivity = childSnapshot.getValue(GameActivityModel::class.java)
                    if (gameActivity != null && gameActivity.gameStatus == const.GAME_STATUS_CREATED) {
                        if(gameActivity.user1!!.id == user.id || gameActivity.user2!!.id == user.id ){
                            callback(gameActivity)
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })

    }

    fun setResultPopupFalse(path: String){
        userDbRef = FirebaseDatabase.getInstance().getReference(path)

        userDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val resultPopupModel = dataSnapshot.getValue(ResultPopupModel::class.java)

                if (resultPopupModel != null) {
                    resultPopupModel.status = false
                    userDbRef.child("resultPopup").setValue(resultPopupModel)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }


}
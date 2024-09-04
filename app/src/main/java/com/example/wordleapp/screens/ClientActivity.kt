package com.example.wordleapp.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.wordleapp.DatabaseManager
import com.example.wordleapp.Models.GameInviteModel
import com.example.wordleapp.R
import com.example.wordleapp.Models.UserModel

class ClientActivity : AppCompatActivity() {
    private lateinit var gameTypeText: TextView
    private lateinit var charSizeText: TextView
    private lateinit var userNameText: TextView

    private lateinit var gameMode: String
    private lateinit var user: UserModel
    private var charSize: Int = 0

    private val dbManager = DatabaseManager()
    private val const = com.example.wordleapp.Models.Constants()

    private lateinit var pathForActiveUsers: String
    private lateinit var pathForGameInvites: String
    private lateinit var pathForGameActivities: String

    private lateinit var receiverAlertDialog: AlertDialog
    private lateinit var senderAlertDialog: AlertDialog
    lateinit var countDownTimer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        setLabels()
        setClickListeners()
        getUsers()
        getInvites()
        dummyActivity()
    }
    private fun dummyActivity(){
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("charSize", charSize)
        intent.putExtra("gameMode", gameMode)
        intent.putExtra("user", user)
        intent.putExtra("activeUsers", pathForActiveUsers)
        intent.putExtra("gameRoomId", "-NujinQ8F-1rNjmyYz9M")
        startActivity(intent)
    }
    private fun setLabels(){
        gameMode = intent.getStringExtra("gameMode") ?: ""
        charSize = intent.getIntExtra("charSize", 0)
        user = intent.getSerializableExtra("user") as UserModel

        pathForActiveUsers = "Channels/$gameMode/$gameMode$charSize/ACTIVEUSERS"
        pathForGameInvites = "Channels/$gameMode/$gameMode$charSize/GAMEINVITES"
        pathForGameActivities = "Channels/$gameMode/$gameMode$charSize/GAMEACTIVITIES"

        gameTypeText = findViewById(R.id.gameTypeText)
        charSizeText = findViewById(R.id.charSizeText)
        userNameText = findViewById(R.id.userNameText)

        gameTypeText.text = buildString {
            append(gameTypeText.text.toString())
            append(gameMode)
        }
        charSizeText.text = buildString {
            append(charSizeText.text.toString())
            append(charSize)
        }
        userNameText.text = buildString {
            append(userNameText.text.toString())
            append(user.username)
        }
    }

    private fun setClickListeners(){
        val quitButton: Button = findViewById(R.id.quitButton)

        quitButton.setOnClickListener {
            dbManager.removeUserFromChannel(pathForActiveUsers,user)
            val intent = Intent(this, ChannelActivity::class.java).apply {
                putExtra("user", user)
            }
            startActivity(intent)
        }
    }

    private fun getUsers(){
        dbManager.listenForUsersAtSameChannel(pathForActiveUsers){
            userList -> if(userList.isNotEmpty()){

                val adapter = UserAdapterWithButton(this, userList, pathForGameInvites, user)
                val listView: ListView = findViewById(R.id.userListView)
                listView.adapter = adapter
            }
        }
    }

    private fun getInvites(){
        dbManager.listenInvites(pathForGameInvites,user){ invite ->
            if (invite != null) {
                if (invite.inviteStatus == const.INVITE_PENDING){
                    if(invite.receiverId == user.id){
                        showReceiverInvitePopupWithCountdown(invite)
                    }
                    else{
                        showSenderInvitePopupWithCountdown(invite)
                    }

                }
                if(invite.inviteStatus == const.INVITE_ACCEPTED){
                    dbManager.listenGameActivities(pathForGameActivities, invite.id!!){ game ->
                        if (game != null && game.gameStatus != const.GAME_STATUS_STARTED
                            && game.gameStatus == const.GAME_STATUS_CREATED) {
                            user.status = const.USER_INGAME
                            val intent = Intent(this, GameActivity::class.java)
                            intent.putExtra("charSize", charSize)
                            intent.putExtra("gameMode", gameMode)
                            intent.putExtra("user", user)
                            intent.putExtra("gameRoomId", game.id)
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    private fun showReceiverInvitePopupWithCountdown(invite: GameInviteModel) {
        val alertDialogBuilder = AlertDialog.Builder(this)

        dbManager.findUserById(invite.senderId!!){ foundUser ->
            if (foundUser != null) {
                alertDialogBuilder.apply {
                    setTitle("Yeni İstek")
                    setMessage("${foundUser.username} size bir istek gönderdi.")
                    setPositiveButton("Kabul Et") { dialog, _ ->
                        dialog.dismiss()
                        findAndAcceptInvite(invite.id!!)
                    }
                    setNegativeButton("Reddet") { dialog, _ ->
                        dialog.dismiss()
                        findAndDeclineInvite(invite.id!!)
                    }
                }
                senderAlertDialog = alertDialogBuilder.create()
                countDownTimer = object : CountDownTimer(10000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        senderAlertDialog.setMessage("${foundUser.username} size bir istek gönderdi. (${millisUntilFinished / 1000} saniye içinde kapanacak)")
                    }

                    override fun onFinish() {
                        senderAlertDialog.dismiss()
                        findAndDeleteInvite(invite.id!!)
                    }
                }

                countDownTimer.start()
                senderAlertDialog.show()
            }
        }

    }
    private fun showSenderInvitePopupWithCountdown(invite:GameInviteModel) {
        val alertDialogBuilder = AlertDialog.Builder(this)

        dbManager.findUserById(invite.receiverId!!){ foundUser ->
            if (foundUser != null) {
                alertDialogBuilder.apply {
                    setTitle("İstek Gönderildi.")
                    setMessage("${foundUser.username} Kullanıcının cevabı bekleniyor.")
                }
                receiverAlertDialog = alertDialogBuilder.create()
                countDownTimer = object : CountDownTimer(10000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        receiverAlertDialog.setMessage("${foundUser.username} Yanıtlamaz ise ${millisUntilFinished / 1000} saniye içinde kapanacak")
                    }

                    override fun onFinish() {
                        receiverAlertDialog.dismiss()
                        findAndDeleteInvite(invite.id!!)
                    }
                }

                countDownTimer.start()
                receiverAlertDialog.show()
            }
        }

    }
    private fun findAndDeleteInvite(receiverId: String){
        dbManager.findInviteById(receiverId,pathForGameInvites){ foundGameInvite->
            if(foundGameInvite != null && (foundGameInvite.inviteStatus == const.INVITE_PENDING
                        || foundGameInvite.inviteStatus == const.INVITE_DECLINED)){
                dbManager.deleteInvite(pathForGameInvites,foundGameInvite)
            }
        }
    }
    private fun findAndDeclineInvite(inviteId: String){
        dbManager.findInviteById(inviteId,pathForGameInvites){ foundGameInvite->
            if(foundGameInvite != null && foundGameInvite.inviteStatus == const.INVITE_PENDING){
                dbManager.declineInvite(pathForGameInvites,foundGameInvite)
                dbManager.deleteInvite(pathForGameInvites,foundGameInvite)
            }
        }
    }

    private fun findAndAcceptInvite(inviteId: String){
        dbManager.findInviteById(inviteId,pathForGameInvites){ foundGameInvite->
            if(foundGameInvite != null && foundGameInvite.inviteStatus == const.INVITE_PENDING){
                dbManager.acceptInvite(pathForGameInvites,foundGameInvite)
            }
        }
    }




    class UserAdapterWithButton(context: Context,
        private val users: MutableList<UserModel>,
        private val path: String,
        private val user: UserModel) : ArrayAdapter<UserModel>(context, 0, users) {

        private val dbManager = DatabaseManager()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val listItemView = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.list_item_user_with_button, parent, false
            )

            val currentUser = users[position]

            val userInfoTextView: TextView = listItemView.findViewById(R.id.userInfoTextView)
            userInfoTextView.text = buildString {
                append("User ID: ${currentUser.id}\n")
                append("Username: ${currentUser.username}\n")
                append("Status: ${currentUser.status}")
            }

            val sendRequestButton: Button = listItemView.findViewById(R.id.sendRequestButton)
            sendRequestButton.setOnClickListener {
                if(user.id == currentUser.id){
                    Toast.makeText(context,"Kendi Kendinize İstek Atamazsınız",Toast.LENGTH_SHORT).show()
                }
                else{
                    dbManager.sendInvite(path, user.id!!, currentUser.id!!, this.context)
                }
            }

            return listItemView
        }
    }
}
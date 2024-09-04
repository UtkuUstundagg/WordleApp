package com.example.wordleapp.screens

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Color.rgb
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.wordleapp.DatabaseManager
import com.example.wordleapp.Models.Constants
import com.example.wordleapp.Models.PointModel
import com.example.wordleapp.Models.RematchModel
import com.example.wordleapp.Models.ResultPopupModel
import com.example.wordleapp.Models.UserModel
import com.example.wordleapp.Models.WordModel
import com.example.wordleapp.R
import java.io.InputStream
import java.util.Random
import java.util.concurrent.TimeUnit


class GameActivity : AppCompatActivity() {
    private lateinit var pathForActiveUsers: String
    private lateinit var pathForGameActivities: String
    private lateinit var pathForGameRoom: String


    private lateinit var gameRoomId: String
    private lateinit var gameType: String
    private lateinit var user: UserModel
    private var charSize: Int = 0

    private val const = Constants()
    private val dbManager = DatabaseManager()

    private lateinit var quitButton: Button
    private lateinit var selectButton: Button

    val editTexts = mutableListOf<EditText>()
    val editTextsForGame = mutableListOf<EditText>()
    val harfler = mutableListOf<String>()
    val renkler = mutableListOf<String>()

    private lateinit var countdownTextView: TextView
    private lateinit var countDownTimer: CountDownTimer

    private var alreadyCounting: Boolean = false
    private var buttonVisibility: Boolean = true

    private val words4Letters = mutableListOf<String>()
    private val words5Letters = mutableListOf<String>()
    private val words6Letters = mutableListOf<String>()
    private val words7Letters = mutableListOf<String>()

    private var focusedRow: Int = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameType = intent.getStringExtra("gameMode") ?: ""
        charSize = intent.getIntExtra("charSize", 0)
        user =  intent.getSerializableExtra("user") as UserModel
        gameRoomId = intent.getStringExtra("gameRoomId") ?: ""

        pathForActiveUsers = "Channels/$gameType/$gameType$charSize/ACTIVEUSERS"
        pathForGameActivities = "Channels/$gameType/$gameType$charSize/GAMEACTIVITIES"
        pathForGameRoom = "Channels/$gameType/$gameType$charSize/GAMEACTIVITIES/$gameRoomId"
        countdownTextView = findViewById(R.id.countdownTextView)

        countdownTextView = findViewById(R.id.countdownTextView)

        quitButton = findViewById(R.id.quitButton)
        selectButton = findViewById(R.id.selectButton)

        updateGameRoomStatus(pathForGameRoom, const.GAME_STATUS_STARTED)
        updateUserStatus(user)
        setWords()
        setClickListeners()
        setEditTextsForInput(gameType, charSize)
        if(!alreadyCounting){
            startCountdown(15000)
        }
        listenForPopUps()
        listenForPoints()
        listenForRematch()
    }
    private fun listenForRematch(){
        dbManager.listenRematchPopup(pathForGameRoom){rematch->
            if(rematch != null){
                showRematchPopup(this,rematch)
                if(rematch.user1Status == const.INVITE_ACCEPTED
                    && rematch.user2Status == const.INVITE_ACCEPTED){
                    dbManager.createRematch(pathForGameActivities, pathForGameRoom,rematch.id!!, rematch.user1Id!!, rematch.user2Id!!)
                    dbManager.listenForRematch(pathForGameActivities, user){ game ->
                        if (game != null && game.gameStatus != const.GAME_STATUS_STARTED
                            && game.gameStatus == const.GAME_STATUS_CREATED) {
                            user.status = const.USER_INGAME
                            val intent = Intent(this, GameActivity::class.java)
                            intent.putExtra("charSize", charSize)
                            intent.putExtra("gameMode", gameType)
                            intent.putExtra("user", user)
                            intent.putExtra("gameRoomId", game.id)
                            dbManager.findAndDeleteOldGameRoom(pathForGameRoom)
                            startActivity(intent)
                        }
                    }
                }
                else if(rematch.user1Status == const.INVITE_DECLINED
                    || rematch.user2Status == const.INVITE_DECLINED){
                    finish()
                }
            }
        }
    }
    private fun showRematchPopup(context: Context, rematchModel: RematchModel){
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle("Sonuç")
        var message = ""

        if(rematchModel.user1Id == user.id){
            message = "${rematchModel.user2Id} Kullanıcısı Rövanş istiyor"
        }
        else{
            message = "${rematchModel.user1Id} Kullanıcısı Rövanş istiyor"
        }
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setPositiveButton("Kabul") { dialog, _ ->
            dialog.dismiss()
            findAndAcceptInvite(rematchModel.id!!)
        }
        alertDialogBuilder.setNegativeButton("Red") { dialog, _ ->
            dialog.dismiss()
            findAndDeclineInvite(rematchModel.id!!)
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
    private fun findAndDeclineInvite(inviteId: String){
        dbManager.findRematchPopupById(pathForGameRoom,inviteId){ foundRematch->
            if(foundRematch != null && foundRematch.user1Id == user.id){
                foundRematch.user1Status = const.INVITE_DECLINED
                dbManager.updateRematchPopup(pathForGameRoom,foundRematch)
            }
            if(foundRematch != null && foundRematch.user2Id == user.id){
                foundRematch.user2Status = const.INVITE_DECLINED
                dbManager.updateRematchPopup(pathForGameRoom,foundRematch)
            }
        }
    }

    private fun findAndAcceptInvite(inviteId: String){
        dbManager.findRematchPopupById(pathForGameRoom, inviteId){ foundRematch->
            if(foundRematch != null && foundRematch.user1Id == user.id){
                foundRematch.user1Status = const.INVITE_ACCEPTED
                dbManager.updateRematchPopup(pathForGameRoom,foundRematch)
            }
            if(foundRematch != null && foundRematch.user2Id == user.id){
                foundRematch.user2Status = const.INVITE_ACCEPTED
                dbManager.updateRematchPopup(pathForGameRoom,foundRematch)
            }
        }
    }

    private fun updateGameRoomStatus(path: String, status:String){
        dbManager.updateGameRoomStatus(path, status)
    }
    private fun updateUserStatus(user: UserModel){
        dbManager.updateUserStatus(pathForActiveUsers, user)
    }
    private fun setClickListeners(){
        selectButton.setOnClickListener {
            if(buttonVisibility){
                selectButton.visibility = Button.INVISIBLE
                buttonVisibility = false
            }

            sendWordModel()
        }
        quitButton.setOnClickListener {
            quitPopupDialog()
        }
    }
    private fun quitPopupDialog(){
        val builder = AlertDialog.Builder(this)
        var username = "----"

        builder.setTitle("Uyarı")
        builder.setMessage("Oyundan çıkmanız halinde oyunu kaybedeceksiniz. Çıkmak istiyor musunuz?")
        builder.setPositiveButton("Onayla") { dialogInterface: DialogInterface, _: Int ->
            dbManager.getEnemyUser(pathForGameRoom, user){ foundUser->
                if (foundUser != null) {
                    username = foundUser.username.toString()
                }
            }
            dbManager.insertPopupModelV2(pathForGameRoom, ResultPopupModel(null, username, true,))
        }
        builder.setNegativeButton("Red") { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
        }
        val dialog = builder.create()
        dialog.show()

    }
    private fun sendWordModel(){
        for (editText in editTexts){
            val harf = editText.text.toString()
            harfler.add(harf)
        }

        if(checkIfWordExists(harfler)){
            if(harfler.size == 4) {
                dbManager.insertWordModelForUser(WordModel(null, const.WORD_APPROVED,
                    harfler[0], null,
                    harfler[1], null,
                    harfler[2], null,
                    harfler[3], null),
                    user, pathForGameRoom)
            }
            else if(harfler.size == 5) {
                dbManager.insertWordModelForUser(WordModel(null, const.WORD_APPROVED,
                    harfler[0], null,
                    harfler[1], null,
                    harfler[2], null,
                    harfler[3], null,
                    harfler[4], null),
                    user, pathForGameRoom)
            }
            else if(harfler.size == 6) {
                dbManager.insertWordModelForUser(WordModel(null, const.WORD_APPROVED,
                    harfler[0],null,
                    harfler[1], null,
                    harfler[2], null,
                    harfler[3], null,
                    harfler[4], null,
                    harfler[5], null),
                    user, pathForGameRoom)
            }
            else if(harfler.size == 7) {
                dbManager.insertWordModelForUser(WordModel(null, const.WORD_APPROVED,
                    harfler[0],null,
                    harfler[1], null,
                    harfler[2], null,
                    harfler[3], null,
                    harfler[4], null,
                    harfler[5], null,
                    harfler[6], null),
                    user, pathForGameRoom)
            }
        }
        else{
            Toast.makeText(this, "Böyle Bir Kelime Bulunamadı", Toast.LENGTH_SHORT).show()
            selectButton.visibility = Button.VISIBLE
            buttonVisibility = true
        }

    }
    private fun setEditTextsForInput(gameType: String, charSize: Int){
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val editTextWidth = (screenWidth - (charSize * 40)) / charSize

        val layout = findViewById<LinearLayout>(R.id.editTextContainer)
        layout.orientation = LinearLayout.HORIZONTAL

        for (i in 0 until charSize) {
            val editText = EditText(this)
            var params  = LinearLayout.LayoutParams(editTextWidth, editTextWidth)
            params.rightMargin = 20
            val editTextId = "editText$i"
            editText.id = resources.getIdentifier(editTextId, "id", packageName)
            editText.layoutParams = params
            editText.setBackgroundColor(Color.WHITE)
            editText.textAlignment = EditText.TEXT_ALIGNMENT_CENTER
            editText.textSize = (editTextWidth /5).toFloat()
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT

            val filterArray = arrayOfNulls<InputFilter>(1)
            filterArray[0] = InputFilter.LengthFilter(1)
            editText.filters = filterArray

            val border = GradientDrawable()
            border.setColor(Color.WHITE)
            border.setStroke(15, Color.TRANSPARENT)
            border.cornerRadius = 20f
            editText.background = border

            layout.addView(editText)
            editTexts.add(editText)
        }

        if(gameType == const.GAME_TYPE_ASSIGNED){
            //todo words içerisinde uzunluğu charSize olan ve
            val random = Random()
            val randomNumber = random.nextInt(editTexts.size)

            val turkishAlphabet = "abcçdefgğhıijklmnoöprsştuüvyz"
            val alphabetLength = turkishAlphabet.length

            var randomIndex = random.nextInt(alphabetLength)
            var randomLetter = turkishAlphabet[randomIndex].uppercaseChar()

            while(randomIndex == 0 && randomLetter == 'Ğ'){
                randomIndex = random.nextInt(alphabetLength)
                randomLetter = turkishAlphabet[randomIndex].uppercaseChar()
            }
            var isWordContains = getWordList(randomNumber, randomLetter)

            while(isWordContains.isEmpty()){
                randomIndex = random.nextInt(alphabetLength)
                randomLetter = turkishAlphabet[randomIndex].uppercaseChar()
                isWordContains = getWordList(randomNumber, randomLetter)
            }

            editTexts[randomNumber].setText(randomLetter.toString())
        }
    }
    private fun getWordList(randomNumber: Int, randomLetter: Char): List<String>{
        if(charSize == 4) {
            return words4Letters.filter { it[randomNumber].uppercaseChar() == randomLetter }
        }
        else if(charSize == 5) {
            return words5Letters.filter { it[randomNumber].uppercaseChar() == randomLetter }
        }
        else if(charSize == 6) {
            return words6Letters.filter { it[randomNumber].uppercaseChar() == randomLetter }
        }
        else {
            return words7Letters.filter { it[randomNumber].uppercaseChar() == randomLetter }
        }
    }
    private fun startCountdown(durationInMillis: Long) {
        if(!alreadyCounting){
            countDownTimer = object : CountDownTimer(durationInMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                    val timeLeftFormatted = String.format("%02d:%02d", minutes, seconds)

                    countdownTextView.text = timeLeftFormatted
                }

                override fun onFinish() {
                    countdownTextView.text = "00:00"
                    checkForApproval()
                }
            }
            alreadyCounting = true
            countDownTimer.start()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
    }
    private fun restartCountdown() {
        alreadyCounting = false
        countDownTimer.cancel()
        startCountdown(60000)
    }
    private fun checkForApproval(){
        dbManager.checkForApproval(pathForGameRoom){result->
            when (result.first) {
                const.WORD_APPROVED -> {
                    startGame()
                }
                const.USER_NO_WINNER -> {
                    restartCountdown()
                }
                const.USER_1_WIN, const.USER_2_WIN -> {
                    dbManager.insertPopupModelV2(pathForGameRoom, ResultPopupModel(null, result.second, true))
                }
                else -> {
                }
            }
        }
    }
    private fun listenForPopUps(){
        dbManager.listenGameResultPopup(pathForGameRoom){ popup->
            if(popup != null && popup.status == true){
                showResultPopup(this@GameActivity, popup)
            }
            if(popup != null && popup.status == false){
                dbManager.deletePopup(pathForGameRoom)
            }
        }
    }
    private fun showResultPopup(context: Context, resultPopupModel: ResultPopupModel) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle("Sonuç")
        var message = ""

        if(resultPopupModel.username != null
            && resultPopupModel.user1Point == null && resultPopupModel.user2Point == null){
            message = "${resultPopupModel.username} Kullanıcısı oyunu kazandı"
        }
        else if (resultPopupModel.username == null
                && resultPopupModel.user1Point != null && resultPopupModel.user2Point != null){
            val winnerUsername = if(resultPopupModel.status == true) {
                if ((resultPopupModel.user1Point?.point ?: 0) > (resultPopupModel.user2Point?.point ?: 0)) {
                    resultPopupModel.user1Point?.username
                }
                else {
                    resultPopupModel.user2Point?.username
                }
            }
            else {
                null
            }
            message = "$winnerUsername Kullanıcısı oyunu kazandı\n\n" +
                    "Oyuncu 1: ${resultPopupModel.user1Point?.username ?: "Bilinmiyor"}\n " +
                    "${resultPopupModel.user1Point?.point ?: 0} Puan\n" +
                    "Oyuncu 2: ${resultPopupModel.user2Point?.username ?: "Bilinmiyor"}\n " +
                    "${resultPopupModel.user2Point?.point ?: 0} Puan\n"
        }
        else if (resultPopupModel.username == "Berabere"
            && resultPopupModel.user1Point != null && resultPopupModel.user2Point != null){

            message = "Berabere\n\n" +
                    "Oyuncu 1: ${resultPopupModel.user1Point?.username ?: "Bilinmiyor"}\n " +
                    "${resultPopupModel.user1Point?.point ?: 0} Puan\n" +
                    "Oyuncu 2: ${resultPopupModel.user2Point?.username ?: "Bilinmiyor"}\n " +
                    "${resultPopupModel.user2Point?.point ?: 0} Puan\n"
        }

        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setPositiveButton("Yeniden Oyna") { dialog, _ ->
            dialog.dismiss()
            dbManager.setResultPopupFalse(pathForGameRoom)
            dbManager.getBothUsers(pathForGameRoom){ pair->
                if(pair != null){
                    dbManager.insertRematchPopUp(pathForGameRoom, RematchModel(null,
                        pair.first.id, const.INVITE_PENDING,
                        pair.second.id, const.INVITE_PENDING))
                }
            }

        }
        alertDialogBuilder.setNegativeButton("Çık") { dialog, _ ->
            dialog.dismiss()
            endGame()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
    private fun endGame(){
        dbManager.findPopupAndSetStatusFalse(pathForGameRoom)
        dbManager.deleteGameRoomById(pathForGameRoom)
        finish()
    }
    private fun readRawFile(resources: Resources, resId: Int): String {
        val inputStream: InputStream = resources.openRawResource(resId)
        val buffer = ByteArray(inputStream.available())
        inputStream.read(buffer)
        inputStream.close()
        return String(buffer)
    }
    private fun setWords(){
        val rawString = readRawFile(resources, R.raw.words)
        val lines = rawString.split("\n")

        lines.forEach { word ->
            if (word.length == 4 && !words4Letters.contains(word)) {
                words4Letters.add(word)
            }
            else if (word.length == 5 && !words5Letters.contains(word)) {
                words5Letters.add(word)
            }
            else if (word.length == 6 && !words6Letters.contains(word)) {
                words6Letters.add(word)
            }
            else if (word.length == 7 && !words7Letters.contains(word)) {
                words7Letters.add(word)
            }
        }
    }
    private fun checkIfWordExists(enteredWordList: MutableList<String>): Boolean{
        var bool = true
        if(enteredWordList.isEmpty()){
            bool = false
        }

        val combinedString = enteredWordList.joinToString(separator = "")
        if(charSize == 4){
            if (!words4Letters.contains(combinedString.toLowerCase())) {
                bool = false
            }
        }
        else if(charSize == 5){
            if (!words5Letters.contains(combinedString.toLowerCase())) {
                bool = false
            }
        }
        else if(charSize == 6){
            if (!words6Letters.contains(combinedString.toLowerCase())) {
                bool = false
            }
        }
        else{
            if (!words7Letters.contains(combinedString.toLowerCase())) {
                bool = false
            }
        }

        return bool
    }
    private fun startGame(){
        val layout = findViewById<LinearLayout>(R.id.editTextContainer)

        for (i in 0 until charSize) {
            val editTextId = "editText$i"
            val resourceId = resources.getIdentifier(editTextId, "id", packageName)
            val editTextToRemove = findViewById<EditText>(resourceId)
            layout.removeView(editTextToRemove)
        }
        setGameScreen()
    }
    private fun setGameScreen(){
        selectButton.visibility = Button.INVISIBLE
        countdownTextView.visibility = TextView.INVISIBLE

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val editTextWidth = (screenWidth - (charSize * 40)) / charSize

        val layout = findViewById<LinearLayout>(R.id.gameEditTextContainer)
        layout.orientation = LinearLayout.VERTICAL

        for (i in 1 until charSize + 1) {
            val linearLayout = LinearLayout(this)
            linearLayout.orientation = LinearLayout.HORIZONTAL
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 20, 0, 0)
            linearLayout.layoutParams = layoutParams
            for (j in 1 until charSize + 1) {
                val editText = EditText(this)
                var params = LinearLayout.LayoutParams(editTextWidth, editTextWidth)
                params.rightMargin = 20
                val editTextId = "editText$i$j"
                editText.id = resources.getIdentifier(editTextId, "id", packageName)
                editText.layoutParams = params
                editText.setBackgroundColor(Color.WHITE)
                editText.textAlignment = EditText.TEXT_ALIGNMENT_CENTER
                editText.textSize = (editTextWidth / 5).toFloat()
                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                val filterArray = arrayOfNulls<InputFilter>(1)
                filterArray[0] = InputFilter.LengthFilter(1)
                editText.filters = filterArray

                val border = GradientDrawable()
                border.setColor(Color.WHITE)
                border.setStroke(15, Color.TRANSPARENT)
                border.cornerRadius = 20f
                editText.background = border

                if (i == 1) {
                    editText.isFocusable = true
                    editText.isFocusableInTouchMode = true
                } else {
                    editText.isFocusable = false
                    editText.isFocusableInTouchMode = false
                }

                linearLayout.addView(editText)
                editTextsForGame.add(editText)
            }
            layout.addView(linearLayout)
        }

        val selectButton = Button(this)
        selectButton.text = "Kelimeyi Onayla"
        selectButton.setBackgroundResource(android.R.drawable.btn_default)
        val cornerRadius = 64
        val backgroundDrawable = GradientDrawable()
        backgroundDrawable.cornerRadius = cornerRadius.toFloat()
        backgroundDrawable.setColor(ContextCompat.getColor(this, R.color.white))
        selectButton.background = backgroundDrawable
        val layoutParams = LinearLayout.LayoutParams(500, 100)
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL
        layoutParams.topMargin = 35
        layoutParams.bottomMargin = 20
        selectButton.layoutParams = layoutParams
        selectButton.setOnClickListener {
            check(focusedRow)
            focusedRow++
        }
        layout.addView(selectButton)
    }
    private fun check(currentRow: Int) {
        harfler.clear()
        val indices = getEditTextIndices(currentRow, charSize)
        for (index in indices){
            harfler.add(editTextsForGame[index].text.toString())
        }
        if (checkIfWordExists(harfler)) {
            checkIfCorrect(currentRow, harfler)

            setFocusableForEditTexts(currentRow, false)
            setFocusableForEditTexts(currentRow + 1, true)
        }
        else{
            Toast.makeText(this, "Kelime Geçerli Değil", Toast.LENGTH_SHORT).show()
        }

    }
    private fun checkIfCorrect(currentRow: Int, enteredWordList: MutableList<String>){
        renkler.clear()
        dbManager.getUserGuessWord(pathForGameRoom, user){wordModel ->
            val wordList = convertWordModelToList(wordModel)
            for(i in 0 until charSize){
                if(enteredWordList.contains(wordList[i])){
                    if(enteredWordList[i] == wordList[i]){
                        renkler.add(const.WORD_CORRECT_PLACE)
                    }
                    else{
                        renkler.add(const.WORD_CONTAINS)
                    }
                }
                else {
                    renkler.add(const.WORD_NOT_CONTAINS)
                }
            }
            setColors(currentRow, renkler)
            if(checkForEnd(renkler)){
                dbManager.insertPopupModelV2(pathForGameRoom, ResultPopupModel(null, user.username, true))
            }
            checkIfAnswerRemaining(currentRow, renkler)
        }


    }
    private fun setColors(currentRow: Int, colors: MutableList<String>){
        val indices = getEditTextIndices(currentRow, charSize)

        for (index in indices) {
            if(colors[index % charSize] == const.WORD_CORRECT_PLACE){
                val border = GradientDrawable()
                border.setColor(rgb(4, 128, 16))
                border.setStroke(15, Color.TRANSPARENT)
                border.cornerRadius = 20f
                editTextsForGame[index].background = border
            }
            else if(colors[index % charSize] == const.WORD_CONTAINS){
                val border = GradientDrawable()
                border.setColor(rgb(214, 157, 43))
                border.setStroke(15, Color.TRANSPARENT)
                border.cornerRadius = 20f
                editTextsForGame[index].background = border
            }
            else if(colors[index % charSize] == const.WORD_NOT_CONTAINS){
                val border = GradientDrawable()
                border.setColor(rgb(173, 173, 173))
                border.setStroke(15, Color.TRANSPARENT)
                border.cornerRadius = 20f
                editTextsForGame[index].background = border
            }
        }
    }
    private fun checkForEnd(colors: MutableList<String>): Boolean {
        return colors.all { it == const.WORD_CORRECT_PLACE }
    }
    private fun checkIfAnswerRemaining(row:Int, colors: MutableList<String>){
        if(row == charSize){
            var points = 0
            for(color in colors){
                if(color == const.WORD_CORRECT_PLACE){
                    points+= 10
                }
                else if(color == const.WORD_CONTAINS){
                    points+= 5
                }
            }
            dbManager.insertPointsForUser(pathForGameRoom, PointModel(null, user.id, user.username, points))
        }

    }
    private fun convertWordModelToList(wordModel: WordModel): MutableList<String>{
        val newList = mutableListOf<String>()
        if(charSize == 4){
            newList.add(wordModel.char1.toString())
            newList.add(wordModel.char2.toString())
            newList.add(wordModel.char3.toString())
            newList.add(wordModel.char4.toString())
        }
        else if(charSize == 5){
            newList.add(wordModel.char1.toString())
            newList.add(wordModel.char2.toString())
            newList.add(wordModel.char3.toString())
            newList.add(wordModel.char4.toString())
            newList.add(wordModel.char5.toString())
        }
        else if(charSize == 6){
            newList.add(wordModel.char1.toString())
            newList.add(wordModel.char2.toString())
            newList.add(wordModel.char3.toString())
            newList.add(wordModel.char4.toString())
            newList.add(wordModel.char5.toString())
            newList.add(wordModel.char6.toString())
        }
        else if(charSize == 7){
            newList.add(wordModel.char1.toString())
            newList.add(wordModel.char2.toString())
            newList.add(wordModel.char3.toString())
            newList.add(wordModel.char4.toString())
            newList.add(wordModel.char5.toString())
            newList.add(wordModel.char6.toString())
            newList.add(wordModel.char7.toString())
        }

        return newList
    }
    private fun setFocusableForEditTexts(currentRow: Int, isFocusable: Boolean) {
        val indices = getEditTextIndices(currentRow, charSize)
        for (index in indices) {
            if(index < editTextsForGame.size){
                editTextsForGame[index].isFocusable = isFocusable
                editTextsForGame[index].isFocusableInTouchMode = isFocusable
            }

        }
    }
    private fun getEditTextIndices(currentRow: Int, charSize: Int): IntRange {
        val startIndex = (currentRow - 1) * charSize
        val endIndex = startIndex + charSize - 1
        return startIndex..endIndex
    }
    private fun listenForPoints(){
        dbManager.getBothPoints(pathForGameRoom){result->
            if(result != null && result.first.point!! > result.second.point!!){
                dbManager.insertPopupModelV2(pathForGameRoom, ResultPopupModel(null, null, true, result.first, result.second))
            }
            else if(result != null && result.second.point!! > result.first.point!!){
                dbManager.insertPopupModelV2(pathForGameRoom, ResultPopupModel(null, null, true, result.first, result.second))
            }
            else if(result != null && result.second.point == result.first.point){
                dbManager.insertPopupModelV2(pathForGameRoom, ResultPopupModel(null, "Berabere", true, result.first, result.second))
            }

        }
    }

}

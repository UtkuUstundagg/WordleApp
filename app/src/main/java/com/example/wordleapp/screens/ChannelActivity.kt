package com.example.wordleapp.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.wordleapp.DatabaseManager
import com.example.wordleapp.Models.Constants
import com.example.wordleapp.R
import com.example.wordleapp.Models.UserModel

class ChannelActivity : AppCompatActivity() {
    private lateinit var user: UserModel
    private var constants = Constants()
    private var dbManager = DatabaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_channel)

        setupButtonListeners()
        initialize()
    }

    private fun initialize() {
        user = intent.getSerializableExtra("user") as UserModel
        val kullaniciAdiTextView = findViewById<TextView>(R.id.welcomeUsername)
        kullaniciAdiTextView.text = user.username + " !"
    }

    private fun setupButtonListeners() {
        val assignedLetterGameButton = findViewById<Button>(R.id.assignedLetterGame)
        assignedLetterGameButton.setOnClickListener {
            showAssignedLetterButtons()
        }

        val randomLetterGameButton = findViewById<Button>(R.id.randomLetterGame)
        randomLetterGameButton.setOnClickListener {
            showRandomLetterButtons()
        }

        val random4LetterGameButton = findViewById<Button>(R.id.randomLetterButton1)
        random4LetterGameButton.setOnClickListener{
            joinChannel(constants.GAME_TYPE_RANDOM, 4)
        }

        val random5LetterGameButton = findViewById<Button>(R.id.randomLetterButton2)
        random5LetterGameButton.setOnClickListener{
            joinChannel(constants.GAME_TYPE_RANDOM,5)
        }

        val random6LetterGameButton = findViewById<Button>(R.id.randomLetterButton3)
        random6LetterGameButton.setOnClickListener{
            joinChannel(constants.GAME_TYPE_RANDOM,6)
        }

        val random7LetterGameButton = findViewById<Button>(R.id.randomLetterButton4)
        random7LetterGameButton.setOnClickListener{
            joinChannel(constants.GAME_TYPE_RANDOM,7)
        }

        val assigned4LetterButton = findViewById<Button>(R.id.assignedLetterButton1)
        assigned4LetterButton.setOnClickListener{
            joinChannel(constants.GAME_TYPE_ASSIGNED,4)
        }

        val assigned5LetterButton = findViewById<Button>(R.id.assignedLetterButton2)
        assigned5LetterButton.setOnClickListener{
            joinChannel(constants.GAME_TYPE_ASSIGNED,5)
        }

        val assigned6LetterButton = findViewById<Button>(R.id.assignedLetterButton3)
        assigned6LetterButton.setOnClickListener{
            joinChannel(constants.GAME_TYPE_ASSIGNED,6)
        }

        val assigned7LetterButton = findViewById<Button>(R.id.assignedLetterButton4)
        assigned7LetterButton.setOnClickListener{
            joinChannel(constants.GAME_TYPE_ASSIGNED,7)
        }


    }

    private fun showAssignedLetterButtons() {
        val assignedLetterButtonsLayout = findViewById<LinearLayout>(R.id.assignedLetterButtons)
        assignedLetterButtonsLayout.visibility = View.VISIBLE

        val randomLetterButtonsLayout = findViewById<LinearLayout>(R.id.randomLetterButtons)
        randomLetterButtonsLayout.visibility = View.GONE
    }

    private fun showRandomLetterButtons() {
        val assignedLetterButtonsLayout = findViewById<LinearLayout>(R.id.assignedLetterButtons)
        assignedLetterButtonsLayout.visibility = View.GONE

        val randomLetterButtonsLayout = findViewById<LinearLayout>(R.id.randomLetterButtons)
        randomLetterButtonsLayout.visibility = View.VISIBLE
    }

    fun joinChannel(gameMode: String, charSize: Int){
        dbManager.addUserToChannel(user,"Channels/$gameMode/$gameMode$charSize")
        val intent = Intent(this, ClientActivity::class.java)
        intent.putExtra("gameMode", gameMode)
        intent.putExtra("charSize", charSize)
        intent.putExtra("user", user)
        startActivity(intent)
    }
}
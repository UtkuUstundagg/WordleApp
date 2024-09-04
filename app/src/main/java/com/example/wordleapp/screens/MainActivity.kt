package com.example.wordleapp.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wordleapp.DatabaseManager
import com.example.wordleapp.R


class MainActivity : AppCompatActivity() {
    private lateinit var dbManager: DatabaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbManager = DatabaseManager()
        setButtonListeners()
    }

    private fun setButtonListeners() {

        val registerButton = findViewById<Button>(R.id.registerButton)
        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val usernameText = findViewById<EditText>(R.id.usernameText)
        val passwordText = findViewById<EditText>(R.id.passwordText)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val username = usernameText.text.toString()
            val password = passwordText.text.toString()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                dbManager.findUserByUsernameAndPassword(username, password) { user ->
                    if (user != null) {
                        val intent = Intent(this, ChannelActivity::class.java).apply {
                            putExtra("user", user)

                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Böyle Bir Kullanıcı Yok", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Hatalı Giriş", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


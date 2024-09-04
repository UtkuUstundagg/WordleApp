package com.example.wordleapp.screens

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wordleapp.DatabaseManager
import com.example.wordleapp.R
import com.example.wordleapp.Models.UserModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var dbManager: DatabaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbManager = DatabaseManager()
        setButtonListeners()
    }

    private fun setButtonListeners() {
        val usernameText = findViewById<EditText>(R.id.usernameText)
        val passwordText = findViewById<EditText>(R.id.passwordText)
        val confirmPasswordText = findViewById<EditText>(R.id.confirmPasswordText)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val username = usernameText.text.toString()
            val password = passwordText.text.toString()
            val confirmPassword = confirmPasswordText.text.toString()

            if(username.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()
                && password.equals(confirmPassword)) {
                //dbManager.removeUserFromChannel()
                dbManager.insertUser(UserModel(null, username, password))
                Toast.makeText(this, "Kayıt Başarılı", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "Hatalı Giriş", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
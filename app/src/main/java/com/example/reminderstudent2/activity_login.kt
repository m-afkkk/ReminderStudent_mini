package com.example.reminderstudent2

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reminderstudent2.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private  lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.tvGoRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }


        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString()
            val pass = binding.etLoginPassword.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        Toast.makeText(this, "جاري تسجيل الدخول...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, mapLoginError(it.exception), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "الرجاء تعبئة جميع الحقول", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mapLoginError(error: Exception?): String {
        return when (error) {
            is FirebaseAuthInvalidCredentialsException -> "الإيميل أو كلمة المرور غير صحيحة"
            is FirebaseAuthInvalidUserException -> "هذا الحساب غير موجود"
            else -> "تعذر تسجيل الدخول، تأكد من البيانات أو حاول لاحقًا"
        }
    }
}
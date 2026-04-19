package com.example.reminderstudent2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reminderstudent2.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.tvGoLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etRegisterName.text.toString().trim()
            val email = binding.etRegisterEmail.text.toString().trim()
            val pass = binding.etRegisterPassword.text.toString().trim()
            val confirmPass = binding.confirmPass.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "الرجاء تعبئة جميع الحقول", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(this, "كلمتا المرور غير متطابقتين", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    LocalDataStore.getInstance(this).saveUserName(name)
                    val uid = firebaseAuth.currentUser?.uid.orEmpty()
                    if (uid.isNotEmpty()) {
                        firestore.collection("users")
                            .document(uid)
                            .set(
                                hashMapOf(
                                    "fullName" to name,
                                    "email" to email
                                )
                            )
                    }
                    Toast.makeText(this, "تم إنشاء الحساب بنجاح", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, mapRegisterError(task.exception), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mapRegisterError(error: Exception?): String {
        return when (error) {
            is FirebaseAuthUserCollisionException -> "هذا البريد مسجل مسبقًا"
            is FirebaseAuthWeakPasswordException -> "كلمة المرور ضعيفة، استخدم 6 أحرف على الأقل"
            is FirebaseAuthInvalidCredentialsException -> "صيغة البريد الإلكتروني غير صحيحة"
            else -> "فشل إنشاء الحساب، حاول مرة أخرى"
        }
    }
}
// feature_auth/ui/LoginActivity.kt
package com.example.petracker.feature_auth.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.petracker.R
import com.example.petracker.core.network.RetrofitClient
import com.example.petracker.core.storage.TokenStore
import com.example.petracker.core.util.UiState
import com.example.petracker.feature_auth.data.AuthApi
import com.example.petracker.feature_auth.data.AuthRepository
import com.example.petracker.feature_menu.ui.MenuActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginActivity: ComponentActivity() {

    private lateinit var vm: LoginViewModel
    private lateinit var etEmail: EditText
    private lateinit var etPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val tokenStore = TokenStore(this)
        val retrofit = RetrofitClient.create(tokenStore)
        val repo = AuthRepository(retrofit.create(AuthApi::class.java), tokenStore)
        vm = LoginViewModel(repo)

        etEmail = findViewById(R.id.etEmail)
        etPass  = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoRegister = findViewById(R.id.tvGoRegister)

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            clearFieldErrors()
            val err = validateFields()
            if (err != null) {
                showFieldErrors(err)
                return@setOnClickListener
            }
            vm.login(
                email = etEmail.text.toString().trim(),
                pass = etPass.text.toString()
            )
        }


        etPass.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                btnLogin.performClick(); true
            } else false
        }

        lifecycleScope.launch {
            vm.state.collectLatest { s ->
                when (s) {
                    is UiState.Loading -> btnLogin.isEnabled = false
                    is UiState.Success -> {
                        btnLogin.isEnabled = true
                        startActivity(Intent(this@LoginActivity, MenuActivity::class.java))
                        finish()
                    }
                    is UiState.Error -> {
                        btnLogin.isEnabled = true
                        Toast.makeText(this@LoginActivity, s.message, Toast.LENGTH_SHORT).show()
                    }
                    is UiState.Idle -> btnLogin.isEnabled = true
                }
            }
        }

    }

    // ---- validación local ----

    private data class FieldError(
        val email: String? = null,
        val pass: String? = null
    )

    private fun validateFields(): FieldError? {
        var emailErr: String? = null
        var passErr: String? = null

        val email = etEmail.text.toString().trim()
        val pass  = etPass.text.toString()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            emailErr = "Email inválido"
        if (pass.isEmpty())
            passErr = "Ingresa tu contraseña"

        return if (emailErr != null || passErr != null) FieldError(emailErr, passErr) else null
    }

    private fun showFieldErrors(e: FieldError) {
        e.email?.let { etEmail.error = it }
        e.pass?.let { etPass.error = it }
    }

    private fun clearFieldErrors() {
        etEmail.error = null
        etPass.error = null
    }
}

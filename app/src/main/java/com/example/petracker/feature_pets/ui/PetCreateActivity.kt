package com.example.petracker.feature_pets.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.petracker.R
import com.example.petracker.core.network.RetrofitClient
import com.example.petracker.core.storage.TokenStore
import com.example.petracker.feature_pets.data.PetCreate
import com.example.petracker.feature_pets.data.PetsApi
import com.example.petracker.feature_pets.data.PetsRepository
import kotlinx.coroutines.launch
import java.util.*
import androidx.activity.addCallback
import android.app.AlertDialog
import android.widget.*

class PetCreateActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_create)

        val etName = findViewById<EditText>(R.id.etName)
        val spSpecies = findViewById<Spinner>(R.id.spSpecies)
        val spSex = findViewById<Spinner>(R.id.spSex)
        val etBreed = findViewById<EditText>(R.id.etBreed)
        val etWeight = findViewById<EditText>(R.id.etWeight)
        val etBirth = findViewById<EditText>(R.id.etBirth)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener { handleCancel() }
        onBackPressedDispatcher.addCallback(this) { handleCancel() }

        spSpecies.adapter = ArrayAdapter.createFromResource(
            this, R.array.species_values, android.R.layout.simple_spinner_dropdown_item
        )
        spSex.adapter = ArrayAdapter.createFromResource(
            this, R.array.sex_values, android.R.layout.simple_spinner_dropdown_item
        )

        // DatePicker para YYYY-MM-DD
        etBirth.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                etBirth.setText(String.format("%04d-%02d-%02d", y, m+1, d))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        val repo = PetsRepository(RetrofitClient.create(TokenStore(this)).create(PetsApi::class.java))

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) { toast("Nombre requerido"); return@setOnClickListener }

            val species = spSpecies.selectedItem.toString()
            val sex = spSex.selectedItem.toString()
            val breed = etBreed.text.toString().ifBlank { null }
            val weight = etWeight.text.toString().toDoubleOrNull()
            val birth = etBirth.text.toString().ifBlank { null }

            val body = PetCreate(
                name = name, species = species, sex = sex,
                breed = breed, weight_kg = weight, birth_date = birth
            )

            btnSave.isEnabled = false
            lifecycleScope.launch {
                val r = repo.create(body)
                btnSave.isEnabled = true
                r.onSuccess {
                    toast("Mascota creada")
                    finish()
                }.onFailure {
                    toast("Error: ${it.message}")
                }
            }
        }
    }
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun handleCancel() {
        if (hasUnsavedChanges()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.discard_changes_title))
                .setMessage(getString(R.string.discard_changes_msg))
                .setPositiveButton(getString(R.string.yes)) { _, _ -> finish() }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        } else {
            finish()
        }
    }

    private fun hasUnsavedChanges(): Boolean {
        val etName = findViewById<EditText>(R.id.etName)
        val etBreed = findViewById<EditText>(R.id.etBreed)
        val etWeight = findViewById<EditText>(R.id.etWeight)
        val etBirth = findViewById<EditText>(R.id.etBirth)

        return etName.text.isNotBlank()
                || etBreed.text.isNotBlank()
                || etWeight.text.isNotBlank()
                || etBirth.text.isNotBlank()
    }
}

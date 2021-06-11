package maniacorp.c17130804.parquimetro

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class SingUp : AppCompatActivity() {

    lateinit var  btn_singUp : Button
    lateinit var  editTextEmail : EditText
    lateinit var  editTextPassword : EditText
    lateinit var  editTextVerify: EditText
    lateinit var  editTextName : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sing_up)

        btn_singUp = findViewById(R.id.btn_SingIn)
        editTextEmail = findViewById(R.id.emailEditText)
        editTextName = findViewById(R.id.nameEditText)
        editTextPassword = findViewById(R.id.passwordEditText)
        editTextVerify = findViewById(R.id.verifyEditText)


        btn_singUp.setOnClickListener{

            if(!editTextName.text.toString().isEmpty() &&
               !editTextEmail.text.toString().isEmpty() &&
               !editTextPassword.text.toString().isEmpty() &&
               !editTextVerify.text.toString().isEmpty()){

               if(editTextPassword.text.toString() == editTextVerify.text.toString()){

                   FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                           editTextEmail.text.toString(),
                           editTextPassword.text.toString()
                   ).addOnCompleteListener {
                       if(it.isSuccessful){
                           
                           var imgUrl = ""

                           guardarCredenciales(editTextEmail.text.toString(),editTextPassword.text.toString())
                           var intent = Intent(this,MainActivity::class.java).apply{
                               putExtra("email",editTextEmail.text.toString())
                               putExtra("name",editTextName.text.toString())
                               putExtra("img", imgUrl)
                           }
                           startActivity(intent)
                       }else{
                           showMessage("Error al iniciar Sesion")
                       }
                   }

               }else{
                   showMessage("La Contraseña no Coincide")
               }
            }else{
                showMessage("Por favor ingresar los datos solicitados")
            }
        }
    }


    private fun guardarCredenciales(email:String,password:String){
        var preference = getSharedPreferences(getString(R.string.preferences), Context.MODE_PRIVATE)
        preference.edit().apply {
            putString("email",email)
            putString("password",password)
        }.apply()
    }

    private fun showMessage(mensaje:String) {
        Snackbar.make(findViewById(R.id.rootLayout),mensaje,Snackbar.LENGTH_LONG).show()
    }
}
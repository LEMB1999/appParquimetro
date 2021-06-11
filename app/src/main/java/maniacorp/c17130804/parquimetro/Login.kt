package maniacorp.c17130804.parquimetro

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


enum class Provider{
    BASIC,
    GOOGLE,
    FACEBOOK,
}

class Login : AppCompatActivity() {

    val GOOGLE_ID_CODE = 1000
    private val callbackManager = CallbackManager.Factory.create()

    lateinit var  btn_singUp : TextView
    lateinit var  btn_logIn : Button
    lateinit var  editTextEmail : TextInputEditText
    lateinit var  editTextPassword : TextInputEditText
    lateinit var  btn_google : ImageButton
    lateinit var  btn_facebook : ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btn_singUp = findViewById(R.id.sing_up) as TextView
        btn_logIn  =  findViewById(R.id.btn_LogIn) as Button
        btn_google = findViewById(R.id.btn_google) as ImageButton
        btn_facebook = findViewById(R.id.btn_facebook) as ImageButton

        editTextEmail = findViewById(R.id.emailEditText)
        editTextPassword = findViewById(R.id.passwordEditText)

        var preference = getSharedPreferences(getString(R.string.preferences),Context.MODE_PRIVATE)
        var email = preference.getString("email","")
        var password = preference.getString("password","")


        if(email!!.isNotEmpty() || password!!.isNotEmpty()){
            var intent = Intent(this,MainActivity::class.java).apply{
                putExtra("email",email)
                putExtra("provider",Provider.BASIC.name)
            }
            startActivity(intent)
        }

        btn_singUp.setOnClickListener {
            var intent = Intent(this,SingUp::class.java)
            startActivity(intent)
        }


        btn_logIn.setOnClickListener {
            if(!editTextEmail.text.toString().isEmpty() &&
                !editTextPassword.text.toString().isEmpty()){
                FirebaseAuth.getInstance().signInWithEmailAndPassword(
                    editTextEmail.text.toString(),
                    editTextPassword.text.toString()
                ).addOnCompleteListener {
                    if(it.isSuccessful){
                        guardarCredenciales(editTextEmail.text.toString(),editTextPassword.text.toString())
                        var intent = Intent(this,MainActivity::class.java).apply{
                            putExtra("email",editTextEmail.text.toString())
                            putExtra("provider",Provider.BASIC.name)
                        }
                        startActivity(intent)
                    }else{
                       showMessage("Error al iniciar Sesion")
                    }
                }
            }
        }

        btn_google.setOnClickListener {

            var googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()

            val googleClient = GoogleSignIn.getClient(this,googleConf)
            googleClient.signOut()
            startActivityForResult(googleClient.signInIntent,GOOGLE_ID_CODE)

        }

        btn_facebook.setOnClickListener {

         LoginManager.getInstance().logInWithReadPermissions(this,listOf("email","public_profile"))

         LoginManager.getInstance().registerCallback(callbackManager,
             object : FacebookCallback<LoginResult>{

                 override fun onSuccess(result: LoginResult?) {

                     result?.let{

                         val token = it.accessToken
                         val credencial = FacebookAuthProvider.getCredential(token.token)
                         FirebaseAuth.getInstance().signInWithCredential(credencial).addOnCompleteListener {
                             if (it.isSuccessful) {
                                 var intent = Intent(this@Login, MainActivity::class.java).apply {
                                     putExtra("nombre", it.result?.user?.displayName)
                                     putExtra("correo", it.result?.user?.email)
                                     putExtra("provider",Provider.FACEBOOK.name)
                                 }
                                 startActivity(intent)
                             } else {
                                 showMessage("Error al Iniciar Sesion")
                             }
                     }
                   }
                 }

                 override fun onCancel() {}

                 override fun onError(error: FacebookException?) {
                    showMessage("Error al Iniciar Sesion")
                 }

         })
       }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode,resultCode,data)

        if(requestCode == GOOGLE_ID_CODE){
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(intent)
            try {
                val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                if(account != null) {
                    val credencial = GoogleAuthProvider.getCredential(account.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credencial).addOnCompleteListener {
                        if (it.isSuccessful) {
                            var intent = Intent(this, MainActivity::class.java).apply {
                                putExtra("nombre", account.displayName)
                                putExtra("correo", account.email)
                                putExtra("provider",Provider.GOOGLE.name)
                            }
                            startActivity(intent)
                        } else {
                            showMessage("Error al Iniciar Sesion")
                        }

                    }
                }
            }catch(e:ApiException){
                showMessage("Error al Iniciar Sesion")
            }
        }
    }

    private fun showMessage(message:String){
        Snackbar.make(findViewById(R.id.rootLayout),message,Snackbar.LENGTH_LONG).show()
    }


    private fun guardarCredenciales(email:String,password:String){
        var preference = getSharedPreferences(getString(R.string.preferences),Context.MODE_PRIVATE)
        preference.edit().apply {
            putString("email",email)
            putString("password",password)
        }.apply()
    }



}
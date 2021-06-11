package maniacorp.c17130804.parquimetro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView



class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    lateinit var qr_btn : Button
    lateinit var  btn_logOut : Button
    lateinit var  imageUser : ImageView
    lateinit var  textViewCorreo : TextView
    val pickImageCode = 1999

    lateinit var  uri  : Uri
    lateinit var  handler: Handler
    lateinit var  textViewSecond : TextView
    lateinit var  textViewMinutes : TextView
    lateinit var  textViewHours : TextView
    lateinit var  btn_cobrar : Button
    lateinit var  queue : RequestQueue
    lateinit var  cronometro_layout : LinearLayout
    lateinit var  cronometro_obj : cronometro


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

         queue = Volley.newRequestQueue(this)
         cronometro_layout = findViewById(R.id.cronometro)


        textViewSecond =  findViewById(R.id.segundosTextView)
        textViewMinutes =  findViewById(R.id.minutosTextView)
        textViewHours = findViewById(R.id.horasTextView)

        handler = object : Handler(Looper.getMainLooper()){
            override fun handleMessage(msg: Message) {
                var seconds =  textViewSecond.text
                var minutes =  textViewMinutes.text
                var hours =    textViewHours.text
                with(msg.data){
                    getString("seconds")?.let {
                         seconds = it
                    }
                    getString("minutes")?.let {
                        minutes = it
                    }
                    getString("hours")?.let{
                        hours = it
                    }
                }

                textViewSecond.text = seconds
                textViewMinutes.text = minutes
                textViewHours.text = hours
                removeMessages(0); //this is very important
            }
        }




        btn_logOut = findViewById(R.id.btn_LogOut) as Button
        imageUser = findViewById(R.id.imgView) as ImageView
        textViewCorreo = findViewById(R.id.correoTextView) as TextView
        qr_btn = findViewById(R.id.btn_qr) as Button

        imageUser.setOnClickListener {
            /*Log.i("entro","mensaje")
            var intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(intent,pickImageCode)*/

            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setCropShape(CropImageView.CropShape.OVAL)
                    .start(this)

        }


        //obtener los parametros pasados al activity
        var email = intent.getStringExtra("email")
        var provider = intent.getStringExtra("provider")
        var nombre = intent.getStringExtra("nombre")


       textViewCorreo.setText(email)


        qr_btn.setOnClickListener {

            var intent = IntentIntegrator(this)
            intent.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            intent.setPrompt("Escanear el QR");
            intent.initiateScan()

        }


        btn_logOut.setOnClickListener {

            if (provider == "FACEBOOK" ){
                LoginManager.getInstance().logOut()
            }

            if(provider == "BASIC"){
                var preference = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE)
                preference.edit().apply{
                    remove("email")
                    remove("password")
                }.apply()
            }

            //borrar credenciales
            FirebaseAuth.getInstance().signOut()
            onBackPressed()

        }


        btn_cobrar = findViewById(R.id.finalizar)
        btn_cobrar.setOnClickListener {

            val url = "http://maniacorp.ddns.net:3000/cobro"
            // Request a string response from the provided URL.
            val stringRequest = StringRequest(Request.Method.GET, url, { response ->
                runOnUiThread {
                    Toast.makeText(this, response, Toast.LENGTH_LONG).show()
                }
                cronometro_obj.stop(true)
            },
                { Log.i("error", "error al realizar el cobro") })

            // Add the request to the RequestQueue.
            queue.add(stringRequest)

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
     super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            var  result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                var resultUri = result.getUri();
                Glide.with(this)
                    .load(resultUri)
                    .circleCrop()
                    .into(imageUser)
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                var  error = result.getError();
            }
        }


     var result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result!=null){
            if(result.contents == null){
                Toast.makeText(this, "Canceled", Toast.LENGTH_LONG).show()
            }else{
                //obtener el qr leido


                val url = "http://maniacorp.ddns.net:3000/checar?id="+result.contents.toString()
                // Request a string response from the provided URL.
                val stringRequest = StringRequest(Request.Method.GET, url, { response ->
                    if (response.equals("false", ignoreCase = true)) {
                        Toast.makeText(this, "Por favor estacionar el auto", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        //iniciar con el cronometro
                        cronometro_layout.visibility = View.VISIBLE
                        cronometro_obj = cronometro()
                        Thread(cronometro_obj).start()
                    }
                },
                    { Log.i("error", "error al enviar la peticion") })

                // Add the request to the RequestQueue.
                queue.add(stringRequest)
            }

        }


    }

    inner class cronometro: Runnable {

        var status = true
        override fun run() {

            var  bundle  = Bundle()
            var  message : Message
            var  minutes = 0
            var  seconds = 0
            var   hours  = 0
            while (status){
                message = Message()
                Thread.sleep(1000)
                seconds++
                if(seconds == 60){
                    minutes++
                    seconds = 0
                    bundle.putString("minutes", minutes.toString())
                }
                if(minutes == 60){
                    minutes = 0
                    hours++
                    bundle.putString("hours", hours.toString())
                }

                bundle.putString("seconds", seconds.toString())
                message.data =  bundle
                handler.sendMessage(message)
            }
        }
        fun stop(status:Boolean){
            this.status= !status
        }

    }





}
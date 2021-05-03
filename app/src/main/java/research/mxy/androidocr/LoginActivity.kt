package research.mxy.androidocr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider


class LoginActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null
    private val user: FirebaseUser? = null
    val RC_SIGN_IN: Int = 1;
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var mGoogleSignInOptions: GoogleSignInOptions
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.

        configureGoogleSignIn()
        setupUI()

        firebaseAuth = FirebaseAuth.getInstance()

    }

    private fun setupUI() {
        val button = findViewById<Button>(R.id.buttonGSO)
        button.setOnClickListener() {
            signIn()
        }
    }



    /* When the user presses on the log in button.
    The user will be prompted to select an authentication account.
    The signInIntent is used to handle the sign in process and for starting the intent the startActivityForResult() is used.
    */
    private fun signIn() {
        val signinIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signinIntent, RC_SIGN_IN)
    }



    private fun configureGoogleSignIn() {
        mGoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("341251165584-fjbktv1v0i4c11qd7bn378sn0dsokp5h.apps.googleusercontent.com")
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, mGoogleSignInOptions)
    }

    override fun onStart() {
        super.onStart()
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        val user = FirebaseAuth.getInstance().currentUser
        if (user!=null){

            Toast.makeText(
                this, "Login Success.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun updateUI() {
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        var account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            Toast.makeText(
                this, "Login Success.",
                Toast.LENGTH_SHORT
            ).show()
            var intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN){
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)

            try{
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            }catch (e:ApiException){
                Toast.makeText(this,"Google sign in failed:(",Toast.LENGTH_LONG).show()
                Log.w("signInError","signInResult:failed code=" + e.getStatusCode())
                // Show fail message as Toast
                // Write error infomation into log.
            }


        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken,null)

        firebaseAuth.signInWithCredential(credential).addOnCompleteListener{

            if(it.isSuccessful){
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            else{
                Toast.makeText(this,"Google sign in failed :(",Toast.LENGTH_LONG).show()
            }

        }
    }

    companion object {
        fun getlaunchIntent(from:Context) = Intent(from,LoginActivity::class.java).apply{
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }



}
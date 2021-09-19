package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationViewModel.AuthenticationState.AUTHENTICATED
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    lateinit var binding: ActivityAuthenticationBinding
    private val authenticationViewModel by viewModels<AuthenticationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)

        // Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google
        // If the user was authenticated, send him to RemindersActivity
        observeAuthenticationState()

        binding.buttonLogin.setOnClickListener { launchFirebaseLogin() }
    }

    private fun launchFirebaseLogin() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent
        // Since in this App we use the observer to check the login status,
        // onActivityResult has no use to me, therefore I just do not bother to use
        // the deprecated startActivityForResult(...)
        startActivity(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
        )
    }

    /**
     * Observes the authentication state and changes the UI accordingly.
     * If there is a logged in user: (1) show a logout button and (2) display their name.
     * If there is no logged in user: show a login button
     */
    private fun observeAuthenticationState() {
        authenticationViewModel.authenticationState.observe(this, Observer { authenticationState ->
            if (authenticationState == AUTHENTICATED) {
                // Upon successful login, navigate the user to the Reminders screen
                // Remainder screen terminated when navigating to this screen, 
                // therefore we cannot use back.
                val intent = Intent(this, RemindersActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            // If not logged in, stay on this screen waiting for the user to click on the login button
        })
    }
}

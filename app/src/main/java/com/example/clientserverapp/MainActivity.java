package com.example.clientserverapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final int SIGN_IN_REQUEST_ON_CREATE = 1;
    private static final String TAG = MainActivity.class.getSimpleName();
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser user = auth.getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check if the user is signed-in to FireBase
        if (user == null) {
            /*
            If user is not signed in, create an intent that opens a new activity for sign-in/sign-up
             */
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_IN_REQUEST_ON_CREATE);

        } else { // user is connected to FireBase
            // if user is signed in, show details and start messaging
            toastWithDetails(true);
        }
    }

    /**
     * This method creates a Toast message with user details
     * (in case the user successfully connected to firebase) or
     * Error message if user sign-in/sign-up eventually failed
     *
     * @param success
     */
    private void toastWithDetails(boolean success) {
        if (success) {
            // user has successfully either signed-in or signed-up
            String userDetails = "Hi, " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName()
                    + "\n id: " + FirebaseAuth.getInstance().getCurrentUser().getUid();
            Toast.makeText(this, userDetails, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the sign-in activity that is now finished was originated by the onCreate method
        if (requestCode == SIGN_IN_REQUEST_ON_CREATE) {
            // an activity was created because the user was not signed in
            if (resultCode == RESULT_OK) {
                // check if the activity for sign-in was finished successfully
                toastWithDetails(true);
            } else {
                // either sign-in or sign-up failed (SignInActivity using signInIntent)
                toastWithDetails(false);
                // only registered users can message each other.
                // terminate the application
                finish(); // close MainActivity
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.logout_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.log_out_item:
                auth.signOut();
                startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_IN_REQUEST_ON_CREATE);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void serverPhone(View view) {
        Log.d(TAG, "Clicked button in MainActivity");
        Intent intent = new Intent(this, ServerPhoneActivity.class);
        startActivity(intent);
    }

    public void clientPhone(View view) {
        Log.d(TAG, "Clicked button in MainActivity");
        Intent intent = new Intent(this, ClientPhoneActivity.class);
        startActivity(intent);
    }

    public void clientPC(View view) {
        Log.d(TAG, "Clicked button in MainActivity");
        Intent intent = new Intent(this, ClientPCActivity.class);
        startActivity(intent);
    }
}


package com.lazooz.lbm;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.lazooz.lbm.chat.ui.activities.*;
import com.lazooz.lbm.communications.ServerCom;
import com.lazooz.lbm.preference.MySharedPreferences;
import com.lazooz.lbm.utils.Utils;
import com.quickblox.auth.QBAuth;
import com.quickblox.auth.model.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.core.QBSettings;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import android.support.v4.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.List;

public class RideRequestActivity extends ActionBarActivity implements View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<People.LoadPeopleResult> {

    private static final String TAG = "RideRequestActivity";

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mIsInResolution;
    private boolean mIntentInProgress;
    private static final int RC_SIGN_IN = 0;


    private ConnectionResult mConnectionResult;

    private SignInButton btnSignIn;
    private Button btnSignOut, btnRevokeAccess;
    private ImageView imgProfilePic;
    private TextView txtName, txtEmail,txtDestPlace;
    private LinearLayout llProfileLayout;
    // Profile pic image size in pixels
    private static final int PROFILE_PIC_SIZE = 400;
    private  static String mMessage;
    private Button AcceptBtn;
    private Button RejectBtn;

    private static String personName;
    private static String personPhotoUrl;
    private static String personGooglePlusProfile;
    private static String email ;
    private static String destination_place_id ;
    /**
     * Called when the activity is starting. Restores the activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }

        mMessage = getIntent().getStringExtra("MESSAGE");

        setContentView(R.layout.riderequest);


        imgProfilePic = (ImageView) findViewById(R.id.imgProfilePic);
        txtName = (TextView) findViewById(R.id.txtName);
        txtEmail = (TextView) findViewById(R.id.txtEmail);
        // txtDestPlace = (TextView) findViewById(R.id.txtDestPlace);
        llProfileLayout = (LinearLayout) findViewById(R.id.llProfile);



            //llProfileLayout.setVisibility(View.VISIBLE);

            AcceptBtn = (Button)findViewById(R.id.btn_accept);
            AcceptBtn.setVisibility(View.GONE);
            AcceptBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String mMessageArray[] =  mMessage.split(" ");
                    Integer OpponentId = Integer.valueOf(mMessageArray[10]);
                    Intent intent = new Intent(RideRequestActivity.this, com.lazooz.lbm.chat.ui.activities.SplashChatActivity.class);
                    String ChatLogin = MySharedPreferences.getInstance().getUserProfile(RideRequestActivity.this,"ChatLogin");
                    intent.putExtra("USER_LOGIN",ChatLogin);
                    intent.putExtra("OPPONENT_LOGIN",mMessageArray[1]+mMessageArray[2]);
                    intent.putExtra("PASSWORD","LAZOOZ10");
                    intent.putExtra("OPPONENTID",OpponentId);
                    startActivity(intent);
                    finish();
                }
            });
            RejectBtn = (Button)findViewById(R.id.btn_reject);
            RejectBtn.setVisibility(View.GONE);
            RejectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(RideRequestActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        if (mGoogleApiClient == null) {
            rebuildGoogleApiClient();
        }
        mGoogleApiClient.connect();
            // Update the UI after signin
            //updateUI(true);
        }


    /**
     * Called when the Activity is made visible.
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("Profile Google OnStart");

    }

    protected synchronized void rebuildGoogleApiClient() {
        // When we build the GoogleApiClient we specify where connected and connection failed
        // callbacks should be returned, which Google APIs our app uses and which OAuth 2.0
        // scopes our app requests.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0 /* clientId */, this)
                .addConnectionCallbacks(this)
                .addApi(Places.GEO_DATA_API)
                .build();
    }

    /**
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onStop() {

            if (mGoogleApiClient != null) {
                mGoogleApiClient.disconnect();
            }

        super.onStop();
    }

    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case REQUEST_CODE_RESOLUTION:
                    retryConnecting();
                    break;
            }

    }

    private void retryConnecting() {
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");

        Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();
        getProfileInformationWithoutLogin();

    }
    /**
     * Updating the UI, showing/hiding buttons and profile layout
     * */
    private void updateUI(boolean isSignedIn) {
        if (isSignedIn) {
            btnSignIn.setVisibility(View.GONE);
            btnSignOut.setVisibility(View.VISIBLE);
            btnRevokeAccess.setVisibility(View.VISIBLE);
            llProfileLayout.setVisibility(View.VISIBLE);
        } else {
            btnSignIn.setVisibility(View.VISIBLE);
            btnSignOut.setVisibility(View.GONE);
            btnRevokeAccess.setVisibility(View.GONE);
            llProfileLayout.setVisibility(View.GONE);
        }
    }


    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");

            retryConnecting();

    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), this, 0, new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    private void signUpQuickBlox(final String UserLogin, final String Password)
    {
        final String APP_ID = "22467";
        final String AUTH_KEY = "Bd7VTXM7R8rj93X";
        final String AUTH_SECRET = "qukUw5ksyj46qVN";

        QBChatService chatService;

        QBSettings.getInstance().fastConfigInit(APP_ID, AUTH_KEY, AUTH_SECRET);
        if (!QBChatService.isInitialized()) {
            QBChatService.init(this);
        }
        chatService = QBChatService.getInstance();

        QBAuth.createSession(new QBEntityCallbackImpl<QBSession>() {
            @Override
            public void onSuccess(QBSession qbSession, Bundle bundle) {
                SignUpUser(UserLogin, Password);

            }

            @Override
            public void onError(List<String> errors) {

            }
        });
    }

    private void SignUpUser(String name ,String Password) {
        QBUser qbUser = new QBUser();
        qbUser.setLogin(name);
        qbUser.setPassword(Password);
        QBUsers.signUpSignInTask(qbUser, new QBEntityCallbackImpl<QBUser>() {
            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {

                Integer ChatId = qbUser.getId();

                SubmitProfileToServer(personName,personPhotoUrl,personGooglePlusProfile,email,ChatId.toString());

                //System.out.println("signUpSignInTask ok");
                //Toast.makeText(ProfileGoogleActivity.this, "", Toast.LENGTH_LONG).show();
                // finish();
            }

            @Override
            public void onError(List<String> strings) {
                //progressDialog.hide();
                //DialogUtils.showLong(context, strings.get(0));
                Toast.makeText(RideRequestActivity.this, "fail to sign to QuickBlox chat", Toast.LENGTH_LONG).show();
                System.out.println("signUpSignInTask fail");
                finish();
            }
        });
    }

    private void getProfileInformationWithoutLogin() {
        try {
            String splitMessage[] = mMessage.split(" ");
            personName = splitMessage[1]+" "+splitMessage[2];
            personPhotoUrl = splitMessage[4];
            personGooglePlusProfile = splitMessage[6];
            email = splitMessage[8];
            destination_place_id = splitMessage[12];

            Log.e(TAG, "Name: " + personName + ", plusProfile: "
                    + personGooglePlusProfile + ", email: " + email
                    + ", Image: " + personPhotoUrl +",DestinationID: "+destination_place_id);

            txtName.setText(personName);
            //  txtEmail.setText(email);

                 /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, destination_place_id);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);




            // by default the profile url gives 50x50 px image only
            // we can replace the value with whatever dimension we want by
            // replacing sz=X
            personPhotoUrl = personPhotoUrl.substring(0,
                    personPhotoUrl.length() - 2)
                    + PROFILE_PIC_SIZE;


            new LoadProfileImage(imgProfilePic).execute(personPhotoUrl);
            //SubmitProfileToServer(personName,personPhotoUrl,personGooglePlusProfile,email);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Callback for results from a Places Geo Data API query that shows the first place result in
     * the details view on screen.
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                com.lazooz.lbm.logger.Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());

                return;
            }
            // Get the Place object from the buffer.
            final Place place = places.get(0);

            // Format details of the place for display and show it in a TextView.
            /*
            mPlaceDetailsText.setText(formatPlaceDetails(getResources(), place.getName(),
                    place.getId(), place.getAddress(), place.getPhoneNumber(),
                    place.getWebsiteUri()));
*/
            txtEmail.setText(place.getName() + " " + place.getAddress());
            com.lazooz.lbm.logger.Log.i(TAG, "Place details received: " + place.getName());
        }
    };

    @Override
    public void onResult(People.LoadPeopleResult loadPeopleResult) {

    }

    /**
     * Background Async task to load user profile picture from url
     * */
    private class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public LoadProfileImage(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                mIcon11 = BitmapFactory.decodeStream(in,null,options);

            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);

                AcceptBtn.setVisibility(View.VISIBLE);
                RejectBtn.setVisibility(View.VISIBLE);


        }
    }
    /**
     * Button on click listener
     * */
    @Override
    public void onClick(View v) {
        /*
        switch (v.getId()) {
            case R.id.btn_sign_in:
                // Signin button clicked
                signInWithGplus();
                break;
            case R.id.btn_sign_out:
                // Signout button clicked
                signOutFromGplus();
                break;
            case R.id.btn_revoke_access:
                // Revoke access button clicked
                revokeGplusAccess();
                break;
        }
        */
    }

    protected void SubmitProfileToServer(String personName ,
                                         String personPhotoUrl,
                                         String personGooglePlusProfil,
                                         String email,
                                         String ChatId) {
        SubmitProfileToServer submitProfileToServer = new SubmitProfileToServer();
        submitProfileToServer.execute(personName,personPhotoUrl,personGooglePlusProfil,email,ChatId);

    }

    private class SubmitProfileToServer extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {

            ServerCom bServerCom = new ServerCom(RideRequestActivity.this);

            String personName  = params[0];
            String personPhotoUrl = params[1];
            String personGooglePlusProfile   = params[2];
            String email    = params[3];
            String chatId    = params[4];



            JSONObject jsonReturnObj=null;
            try {
                MySharedPreferences msp = MySharedPreferences.getInstance();
                bServerCom.setUserProfile(msp.getUserId(RideRequestActivity.this), msp.getUserSecret(RideRequestActivity.this),personName,personPhotoUrl,personGooglePlusProfile,email,chatId);
                jsonReturnObj = bServerCom.getReturnObject();
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            String serverMessage = "";

            try {
                if (jsonReturnObj == null)
                    serverMessage = "ConnectionError";
                else {
                    serverMessage = jsonReturnObj.getString("message");
                    if (serverMessage.equals("success")){

                    }
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
                serverMessage = "GeneralError";
            }


            return serverMessage;
        }

        @Override
        protected void onPostExecute(String result) {


        }


        @Override
        protected void onPreExecute() {

        }
    }


}
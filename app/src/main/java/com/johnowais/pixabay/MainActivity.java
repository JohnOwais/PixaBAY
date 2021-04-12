package com.johnowais.pixabay;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    ConnectionDetector connectionDetector;
    Handler handler = new Handler();
    JSONArray jsonArray;
    GridLayout gridLayout;
    CardView cardView;
    ImageView imageView;
    Button loadMoreButton;
    DrawerLayout.LayoutParams layoutParams;
    String url = "https://pixabay.com/api/?key=21106994-88ecc2dee7c6d334826242d90&q=islam&image_type=photo&pretty=true";
    int CODE = 1;
    float scale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        connectionDetector = new ConnectionDetector(this);
        gridLayout = findViewById(R.id.mainLayout);
        loadMoreButton = findViewById(R.id.loadMore);
        loadMoreButton.setVisibility(View.GONE);
        scale = this.getResources().getDisplayMetrics().density;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int size = (int) (displayMetrics.widthPixels / 2.2);
        layoutParams = new DrawerLayout.LayoutParams(size, size);
        layoutParams.setMargins((int) (11 * scale), (int) (12 * scale), 0, 0);
        if (!connectionDetector.isConnected()) {
            Toast internet = Toast.makeText(this, "No Internet Connection !!!", Toast.LENGTH_SHORT);
            internet.setGravity(Gravity.CENTER, 0, 0);
            internet.show();
            handler.postDelayed(this::finish, 2000);
        } else if (mAuth.getCurrentUser() == null)
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAllowNewEmailAccounts(true).build(), CODE);
        else
            loadImages(url);
    }

    public void loadImages(String url) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                jsonArray = jsonObject.getJSONArray("hits");
                displayImages(0);
            } catch (Exception ignored) {
            }
        }, error -> {
        });
        stringRequest.setShouldCache(false);
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
        Handler handler = new Handler();
        handler.postDelayed(() -> loadMoreButton.setVisibility(View.VISIBLE), 1000);
    }

    private void displayImages(int startIndex) {
        try {
            for (int i = startIndex; i < startIndex + 10; i++) {
                imageView = new ImageView(this);
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                JSONObject object = jsonArray.getJSONObject(i);
                Picasso.with(this).load(object.getString("largeImageURL")).into(imageView);
                cardView = new CardView(this);
                cardView.setLayoutParams(layoutParams);
                cardView.addView(imageView);
                gridLayout.addView(cardView);
            }
            loadMoreButton.setOnClickListener(v -> {
                gridLayout.setRowCount(10);
                displayImages(10);
                loadMoreButton.setVisibility(View.GONE);
            });
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == CODE) {
                Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).reload();
                handler.postDelayed(() -> {
                    if (Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).isEmailVerified()) {
                        loadImages(url);
                    } else {
                        Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).sendEmailVerification().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast toast = Toast.makeText(this, "Please verify your email first", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
                                if (launchIntent != null) {
                                    startActivity(launchIntent);
                                    finish();
                                }
                            }
                        });
                    }
                }, 3000);
            } else {
                onActivityResult(requestCode, resultCode, data);
            }
        } catch (Exception e) {
            Toast failed = Toast.makeText(this, "Register/Login Failed !!!", Toast.LENGTH_SHORT);
            failed.setGravity(Gravity.CENTER, 0, 0);
            failed.show();
            handler.postDelayed(this::finish, 2000);
        }
    }
}
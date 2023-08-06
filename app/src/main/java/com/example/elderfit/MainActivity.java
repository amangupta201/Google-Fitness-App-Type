package com.example.elderfit;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    //private TextView textStepCountValue;
    //private TextView textDistanceValue;
    //private TextView textCaloriesValue;
    //private TextView textHeartRateValue;
    private String[] FitnessData = new String[4];
    private String[] FitnessName = new String[]{"Step Count", "Distance(in meters)", "Calories Expended", "Heart Points"};
    private int[] FitnessIcons = new int[]{R.drawable.brocolli, R.drawable.almond, R.drawable.banana, R.drawable.apple};
    private ListView fitnessList;
    private RecyclerView fitnessRecyclerView;
    private FitnessDataAdapter adapter;
    private FitnessDataAdapter fitnessDataAdapter;


    private static final int MY_PERMISSIONS = 101;
    private static final int RC_SIGN_IN = 123;
    private static final int RC_GOOGLE_FIT_PERMISSIONS = 102;
    private static final int RC_BODY_SENSORS_PERMISSIONS = 103;

    private ActivityResultLauncher<Intent> signInLauncher;
    private static final String BROADCAST_DETECTED_ACTIVITY = "activity_intent";
    private static BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fitness_list);
        fitnessRecyclerView = findViewById(R.id.fitnessRecyclerView);
        fitnessRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        fitnessDataAdapter = new FitnessDataAdapter(FitnessName, FitnessIcons, new String[4][1]);
        fitnessRecyclerView.setAdapter(fitnessDataAdapter);

        signInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Handle the result in the lambda expression below
                    handleGoogleSignInResult(result.getResultCode(), result.getData());
                });


        // Call the checkForPermissions method here or wherever appropriate in your code
        checkForPermissions();
        startGoogleSignIn();
    }

    public void checkForPermissions() {
        String[] arrayOfPermission;
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_HEART_POINTS, FitnessOptions.ACCESS_READ)
                .build();
        if (GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            fetchFitnessData();
        } else {
            GoogleSignIn.requestPermissions(
                    this,
                    RC_GOOGLE_FIT_PERMISSIONS,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOfPermission = new String[]{Manifest.permission.ACTIVITY_RECOGNITION};
        } else {
            arrayOfPermission = new String[]{"com.android.gms.permission.ACTIVITY_RECOGNITION"};
        }

        // Check if the permission is not granted
        if (ContextCompat.checkSelfPermission(this, arrayOfPermission[0]) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            ActivityCompat.requestPermissions(this, arrayOfPermission, MY_PERMISSIONS);
        } else {
            // Permission already granted, you can proceed with using the permission.
            // For example, you can start using the activity recognition API here.
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, RC_BODY_SENSORS_PERMISSIONS);
        } else {
        }
    }


    // Override onRequestPermissionsResult to handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS) {
            // Check if the permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now proceed with using the permission.
                // For example, you can start using the activity recognition API here.
            } else {
                // Permission denied, check if the user clicked "Don't Ask Again"
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                    // User clicked "Don't Ask Again," show a dialog explaining why the permission is needed
                    showPermissionRationaleDialog();
                } else {
                    // User clicked "Deny," show a toast or perform any action you want
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == RC_BODY_SENSORS_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now proceed with using the permission.
                // For example, you can start using the body sensors API here.
            } else {
                // Permission denied, check if the user clicked "Don't Ask Again"
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BODY_SENSORS)) {
                    // User clicked "Don't Ask Again," show a dialog explaining why the permission is needed
                    showBodySensorsPermissionRationaleDialog();
                } else {
                    // User clicked "Deny," show a toast or perform any action you want
                    Toast.makeText(this, "Body Sensors Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Show a dialog to explain why the permission is needed
    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs to access your activity recognition data to provide personalized services.")
                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Open app settings to allow permission
                        openAppSettings();
                    }
                })
                .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Handle the situation when the user clicks "Deny"
                        Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                    }
                })
                .setCancelable(false)
                .show();
    }

    // Show a dialog to explain why the body sensors permission is needed
    private void showBodySensorsPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Body Sensors Permission Required")
                .setMessage("This app needs access to your body sensors to monitor your health and fitness data.")
                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Open app settings to allow permission
                        openAppSettings();
                    }
                })
                .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Handle the situation when the user clicks "Deny"
                        Toast.makeText(MainActivity.this, "Body Sensors Permission denied", Toast.LENGTH_SHORT).show();
                    }
                })
                .setCancelable(false)
                .show();
    }


    // Add your other methods and code here
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void startGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            // Google Sign-In was successful, you can now access the GoogleSignInAccount
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            // Use the account information as needed (e.g., display user's name, email, etc.)
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                String displayName = account.getDisplayName();
                String email = account.getEmail();
                // ... Handle other account information as needed
                fetchFitnessData();
            } else {
                // Handle Google Sign-In failure
                //Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Handle Google Sign-In failure or cancellation
            //Toast.makeText(this, "Google Sign-In failed or canceled", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchFitnessData() {
        // ... Rest of your code for fetching fitness data ...
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_HEART_POINTS, FitnessOptions.ACCESS_READ)
                .build();

        // Check if the user has granted the necessary fitness permissions
        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            // If permissions are not granted, request them again
            GoogleSignIn.requestPermissions(this, RC_GOOGLE_FIT_PERMISSIONS, account, fitnessOptions);
            return;
        }

        // If permissions are granted, proceed with reading fitness data

        // Set the time range for the data query (e.g., past week)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        // Set the start time to the beginning of the current day
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startTime = calendar.getTimeInMillis();
        // Set the end time to the current time
        long endTime = System.currentTimeMillis();

        // Create a DataReadRequest to retrieve step count, distance, and calories data
        DataReadRequest dataReadRequest = new DataReadRequest.Builder()
                .read(DataType.TYPE_STEP_COUNT_DELTA)
                .read(DataType.TYPE_DISTANCE_DELTA)
                .read(DataType.TYPE_CALORIES_EXPENDED)
                .read(DataType.TYPE_HEART_POINTS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        // Fetch the fitness data asynchronously using the HistoryClient
        HistoryClient historyClient = Fitness.getHistoryClient(this, account);
        Task<DataReadResponse> responseTask = historyClient.readData(dataReadRequest);

        // Handle the data read result
        responseTask.addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
            @Override
            public void onSuccess(DataReadResponse readResponse) {
                // Process the fitness data here

                // Check if the response contains any data
                if (readResponse.getDataSet(DataType.TYPE_STEP_COUNT_DELTA).isEmpty()) {
                    // No step count data available for the current day, set the value to 0
                    FitnessData[0] = "0";
                } else {
                    // Step count data is available, calculate the total steps
                    int totalSteps = 0;
                    for (DataPoint dataPoint : readResponse.getDataSet(DataType.TYPE_STEP_COUNT_DELTA).getDataPoints()) {
                        totalSteps += dataPoint.getValue(Field.FIELD_STEPS).asInt();
                    }
                    FitnessData[0] = String.valueOf(totalSteps);
                }

                // Calculate total distance for the current day
                if (readResponse.getDataSet(DataType.TYPE_DISTANCE_DELTA).isEmpty()) {
                    // No step count data available for the current day, set the value to 0
                    FitnessData[1] = "0";
                } else {
                    float totalDistance = 0;
                    for (DataPoint dataPoint : readResponse.getDataSet(DataType.TYPE_DISTANCE_DELTA).getDataPoints()) {
                        totalDistance += dataPoint.getValue(Field.FIELD_DISTANCE).asFloat();
                    }
                    FitnessData[1] = String.valueOf(totalDistance);
                }

                // Calculate total calories expended for the current day
                if (readResponse.getDataSet(DataType.TYPE_CALORIES_EXPENDED).isEmpty()) {
                    // No step count data available for the current day, set the value to 0
                    FitnessData[2] = "0";
                } else {
                    float totalCalories = 0;
                    for (DataPoint dataPoint : readResponse.getDataSet(DataType.TYPE_CALORIES_EXPENDED).getDataPoints()) {
                        totalCalories += dataPoint.getValue(Field.FIELD_CALORIES).asFloat();
                    }
                    FitnessData[2] = String.valueOf(totalCalories);
                }

                // Calculate total heart points for the current day (if available)
                float totalHeartPoints = 0;
                if (readResponse.getDataSet(DataType.TYPE_HEART_POINTS).isEmpty()) {
                    FitnessData[3] = "0";
                } else {
                    for (DataPoint dataPoint : readResponse.getDataSet(DataType.TYPE_HEART_POINTS).getDataPoints()) {
                        totalHeartPoints += dataPoint.getValue(Field.FIELD_INTENSITY).asFloat();
                    }
                    FitnessData[3] = String.valueOf(totalHeartPoints);
                }

                // Update the data in the adapter
                if (fitnessDataAdapter != null) {
                    String[][] newData = {{FitnessData[0]}, {FitnessData[1]}, {FitnessData[2]}, {FitnessData[3]}};
                    fitnessDataAdapter.updateData(newData);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle the failure here, if needed
                Log.e("Fitness Data", "Failed to fetch fitness data: " + e.getMessage());
            }
        });
    }

    private void setListData() {
        int chunkSize = 4;
        String[][] fitnessDataChunks = new String[FitnessData.length / chunkSize][chunkSize];
        int dataIndex = 0;

        // Split FitnessData into chunks and store them in fitnessDataChunks
        for (int i = 0; i < fitnessDataChunks.length; i++) {
            for (int j = 0; j < chunkSize; j++) {
                fitnessDataChunks[i][j] = FitnessData[dataIndex];
                dataIndex++;
            }
        }

        // Update the data in the adapter using the updateData() method
        fitnessDataAdapter.updateData(fitnessDataChunks);
    }
}

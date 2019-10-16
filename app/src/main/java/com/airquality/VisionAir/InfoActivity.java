package com.airquality.VisionAir;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        TextView policy = findViewById(R.id.ptBodyA);
        policy.setText(
                "This Privacy Policy for VisionAir describes how VisionAir collects, uses and stores personal information which you consent to by using this application. Personal information refers to the information that personally identifies you, such as your name or other data that can be reasonably used to infer this information. The use of ‘We’, ‘we’, ‘us’ and all other such terms refers to the developer team ‘ Make it App’n ’.\n" +
                "\n\nWe understand that maintaining the user’s privacy is vital and hence the user’s privacy is of the utmost importance to us.\n" +
                "\nWhat Personal Information does VisionAir Application collect?\n\n" +
                "1.\tThe application captures an image to extract the required features from the image. The feature extraction from the image is done entirely On – Device.\n" +
                "2.\tThe application requires the user’s fine location to retrieve the distance from the nearest pollution station. However, the co-ordinates of the pollution stations are present in the application and the distance is calculated on-device.\n" +
                "3.\tThe coarse location of the user is used to get the weather parameters. However, the location used is rounded off to one decimal place and hence cannot be used to target an individual user.\n" +
                "4.\tThe current hour of the day is taken to use as a feature.\n" +
                "\n\nHow do we use this information?\n\n" +
                "The features extracted from the image, along with the weather parameters and the hour are used to estimate the PM 2.5 levels on-device. The model is locally available and the features and parameters are NOT sent outside the device.\n" +
                "APIs Used:\n" +
                "\n1.\tOpenWeatherMap’ Weather API: A call to this API is made to get the weather parameters. The API requires the user’s coarse location.\n" +
                "https://api.openweathermap.org/data/2.5/weather\n" +
                "\n" +
                "\n2.\tData.gov.in API: A call to this API is made to get the PM 2.5 values from the nearest Central Pollution Control Board (CPCB) station. This is done to get the label for On-Device Training. The API requires the nearest CPCB as input which is calculated On-Device.\n" +
                "https://api.data.gov.in/resource/3b01bcb8-0b14-4abf-b6f2-c1bfd384ba69\n" +
                "\n\nPermissions Required by VisionAir:\n\n" +
                "The following permissions are required by VisionAir:\n" +
                "1.\tRear Camera: The application requires access to the rear camera to capture images of the surroundings for PM 2.5 level estimation.\n" +
                "\n2.\tFine Location: The user’s fine location is required to get the distance from the nearest pollution station. Note that the user’s fine location does not leave the device.\n" +
                "\n3.\tInternet: The application requires an internet connection the receive the updated model.\n" +
                "\n\nPersonalized Model Opt-In:\n\n" +
                "If the user decides to opt-in for the ‘Personalized Model’ feature, the following data will be collected:\n" +
                "\n1.\tThe PM 2.5 estimate and the current PM 2.5 reading will be used to train the model On-Device.\n" +
                "\n2.\tOnce trained, the updated parameters of the Machine Learning model will be sent anonymously to a server where the parameters will be processed such that individual contribution is no longer distinguishable to produce a better model. The server is hosted on Heroku which uses HTTPS for secure communication. Please note that the parameters sent to the server are destroyed once the model is updated.\n");
    }
}

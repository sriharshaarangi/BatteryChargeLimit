package com.slash.batterychargelimit;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * Created by harsha on 10/5/17.
 */

public class About extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        displayVersion();
        displayDevelopers();
        displayTranslators();
        displaySourceLink();
        displayXdaLink();
    }

    private void displayVersion(){
        TextView versionTV = (TextView) findViewById(R.id.app_version);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;
            String display = version + " (" + versionCode + ")";
            versionTV.setText(display);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void displayDevelopers(){
        TextView developersTV = (TextView) findViewById(R.id.developers);
        StringBuilder builder = new StringBuilder();
        for (String s : Constants.DEVELOPERS) {
            builder.append(s).append("\n");
        }
        developersTV.setText(builder.toString());
    }

    private void displayTranslators(){
        TextView translatorsTV = (TextView) findViewById(R.id.translators);
        StringBuilder builder = new StringBuilder();
        for (String s : Constants.TRANSLATORS) {
            builder.append(s).append("\n");
        }
        translatorsTV.setText(builder.toString());
    }

    private void displaySourceLink(){
        TextView sourceTV = (TextView) findViewById(R.id.source_code);
        sourceTV.setText(Constants.SOURCE_CODE);
        sourceTV.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void displayXdaLink(){
        TextView xdaTV = (TextView) findViewById(R.id.xda_thread);
        xdaTV.setText(Constants.XDA_THREAD);
        xdaTV.setMovementMethod(LinkMovementMethod.getInstance());
    }
}

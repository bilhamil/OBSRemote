package com.bilhamil.obsremote;

import com.bilhamil.obsremote.R;
import com.bilhamil.obsremote.state.OBSRemoteApplication;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Splash extends Activity {
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.splash);
        
        //Set font for title
        TextView headerTextView=(TextView)findViewById(R.id.splashheader);
        Typeface typeFace=Typeface.createFromAsset(getAssets(),"fonts/neometricmedium.ttf");
        headerTextView.setTypeface(typeFace);
    }
	
	
	public void connect(View view)
	{
		//Get hostname and connect
		String hostname = ((EditText)findViewById(R.id.hostentry)).getText().toString();
		
		OBSRemoteApplication app = (OBSRemoteApplication)getApplicationContext();
		app.connect(hostname);
	}
}

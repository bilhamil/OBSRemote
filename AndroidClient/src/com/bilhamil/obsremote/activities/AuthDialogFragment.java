package com.bilhamil.obsremote.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.bilhamil.obsremote.OBSRemoteApplication;
import com.bilhamil.obsremote.R;

public class AuthDialogFragment extends DialogFragment {
    
    public String message;
    public OBSRemoteApplication app;
    
    public static void startAuthentication(FragmentActivity fragAct, OBSRemoteApplication app)
    {
        startAuthentication(fragAct, app, null);
    }
    
    public static void startAuthentication(FragmentActivity fragAct, OBSRemoteApplication app, String errorMessage)
    {
        AuthDialogFragment frag = new AuthDialogFragment();
        frag.message = errorMessage;
        frag.app = app;
        fragAct.getSupportFragmentManager().beginTransaction().add(frag, OBSRemoteApplication.TAG).commitAllowingStateLoss();
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.password_dialog, null);
        CheckBox rememberCheckbox = (CheckBox) dialogView.findViewById(R.id.rememberPassword);
        rememberCheckbox.setChecked(app.getRememberPassword());
        
        //Set Error message
        if(message != null)
            ((TextView)dialogView.findViewById(R.id.authErrorView)).setText(message);
        
        builder.setView(dialogView);
        
        
        builder.setMessage(R.string.authenticate)
               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       String password = ((EditText)AuthDialogFragment.this.getDialog().findViewById(R.id.password)).getText().toString();
                       boolean rememberPassword = ((CheckBox)AuthDialogFragment.this.getDialog().findViewById(R.id.rememberPassword)).isChecked();
                       
                       app.setRememberPass(rememberPassword);
                       
                       app.service.authenticate(password);
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // User cancelled the dialog, shutdown everything
                       if(app.service != null)
                       {
                           app.service.disconnect();
                       }
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}

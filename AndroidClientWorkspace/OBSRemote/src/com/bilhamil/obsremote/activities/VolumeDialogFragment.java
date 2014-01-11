package com.bilhamil.obsremote.activities;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;

import com.bilhamil.obsremote.OBSRemoteApplication;
import com.bilhamil.obsremote.R;
import com.bilhamil.obsremote.RemoteUpdateListener;
import com.bilhamil.obsremote.WebSocketService;
import com.bilhamil.obsremote.messages.ResponseHandler;
import com.bilhamil.obsremote.messages.requests.GetVolumes;
import com.bilhamil.obsremote.messages.requests.SetVolume;
import com.bilhamil.obsremote.messages.requests.ToggleMute;
import com.bilhamil.obsremote.messages.responses.Response;
import com.bilhamil.obsremote.messages.responses.VolumesResponse;
import com.bilhamil.obsremote.messages.util.Source;

public class VolumeDialogFragment extends DialogFragment implements RemoteUpdateListener {
    
    private View dialogView;
    public WebSocketService service;
    public static final String TAG = "VolumeDialogFragment";
    
    public static void startDialog(FragmentActivity fragAct, WebSocketService s)
    {
        VolumeDialogFragment frag = new VolumeDialogFragment();
        frag.service = s;
        frag.show(fragAct.getSupportFragmentManager(), TAG);
    }
    
    public OBSRemoteApplication getApp()
    {
        return (OBSRemoteApplication)this.getActivity().getApplicationContext();
    }
    
    private void updateVolumes()
    {
        service.sendRequest(new GetVolumes(), new ResponseHandler()
        {
            
            @Override
            public void handleResponse(Response resp, String jsonMessage)
            {
                VolumesResponse volumes = getApp().getGson().fromJson(jsonMessage, VolumesResponse.class);
                
                if(volumes == null)
                    return;
                
                dialogView.setVisibility(View.VISIBLE);
                
                setDesktopVolume(volumes.desktopVolume);
                setDesktopMuted(volumes.desktopMuted);
                
                setMicVolume(volumes.micVolume);
                setMicMuted(volumes.micMuted);
            }
        });
    }
    
    protected void setMicMuted(boolean muted)
    {
        ImageButton button = (ImageButton) dialogView.findViewById(R.id.mic_button);
        if(muted)
        {
            button.setImageResource(R.drawable.microphone_off);
        }
        else
        {
            button.setImageResource(R.drawable.microphone_on);
        }
    }
    
    protected void setMicVolume(double volume)
    {
        View meter = dialogView.findViewById(R.id.mic_meter);
        
        RelativeLayout red = (RelativeLayout) meter.findViewById(R.id.volume_red);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, (float)volume);
        red.setLayoutParams(params);
        
        RelativeLayout gray = (RelativeLayout) meter.findViewById(R.id.volume_gray);
        params = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, (float)(1 - volume));        
        gray.setLayoutParams(params);
        
    }

    protected void setDesktopMuted(boolean muted)
    {
        ImageButton button = (ImageButton) dialogView.findViewById(R.id.desktop_button);
        if(muted)
        {
            button.setImageResource(R.drawable.desktop_offs);
        }
        else
        {
            button.setImageResource(R.drawable.desktop_ons);
        }
    }

    protected void setDesktopVolume(double volume)
    {
        View meter = dialogView.findViewById(R.id.desktop_meter);
        
        RelativeLayout red = (RelativeLayout) meter.findViewById(R.id.volume_red);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, (float)volume);
        red.setLayoutParams(params);
        
        RelativeLayout gray = (RelativeLayout) meter.findViewById(R.id.volume_gray);
        params = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, (float)(1 - volume));        
        gray.setLayoutParams(params);
        
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	
    	setRetainInstance(true);
    	
        service.addUpdateListener(this);
    }
    
    @Override
    public void onDestroyView() {

        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
        service.removeUpdateListener(VolumeDialogFragment.this);
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        dialogView = inflater.inflate(R.layout.audio_dialog, null);
        
        builder.setView(dialogView);
        
        
        /* Setup Mute Toggle Actions */
        ImageButton micButton = (ImageButton) dialogView.findViewById(R.id.mic_button);
        micButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                service.sendRequest(ToggleMute.getMicrophoneMute());
            }
        });
        
        ImageButton desktopButton = (ImageButton) dialogView.findViewById(R.id.desktop_button);
        desktopButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                service.sendRequest(ToggleMute.getDesktopMute());
            }
        });
        
        /* Setup volume meter touch events */
        View desktopMeter = dialogView.findViewById(R.id.desktop_meter);
        desktopMeter.setOnTouchListener(new MeterTouchListener("desktop"));
        
        View micMeter = dialogView.findViewById(R.id.mic_meter);
        micMeter.setOnTouchListener(new MeterTouchListener("microphone"));
        
        builder.setMessage(R.string.adjustvolume)
               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       /* do nothing */
                   }
               });
               
        // Create the AlertDialog object and return it
        return builder.create();
    }

    public class MeterTouchListener implements OnTouchListener
    {
        public String channel;
        
        public MeterTouchListener(String chan)
        {
            this.channel = chan;
        }
        
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            SetVolume req = new SetVolume();
            req.channel = channel;
            
            if(event.getAction() == MotionEvent.ACTION_UP)
                req.finalValue = true;
            else
                req.finalValue = false;
            
            float xVal = event.getX();
            float volumeVal = xVal / v.getWidth();
            volumeVal = Math.min(1.0f, volumeVal);
            volumeVal = Math.max(0.0f, volumeVal);
            
            req.volume = volumeVal;
            
            service.sendRequest(req);
            
            return true;
        }
        
    }
    
    @Override
    public void onStart()
    {
        super.onStart();
        
        /* initial volume acquisition */
        updateVolumes();
    }

    @Override
    public void onConnectionAuthenticated()
    {}

    @Override
    public void onConnectionClosed(int code, String reason)
    {
    	this.dismissAllowingStateLoss();
    }
    
    @Override
    public void onStreamStarting(boolean previewOnly)
    {}

    @Override
    public void onStreamStopping()
    {}

    @Override
    public void onFailedAuthentication(String message)
    {}

    @Override
    public void onNeedsAuthentication()
    {}

    @Override
    public void onStreamStatusUpdate(int totalStreamTime, int fps,
            float strain, int numDroppedFrames, int numTotalFrames, int bps)
    {}
    
    @Override
    public void onSceneSwitch(String sceneName)
    {}

    @Override
    public void onScenesChanged()
    {}
    
    @Override
    public void onSourceChanged(String sourceName, Source source)
    {}

    @Override
    public void onSourceOrderChanged(ArrayList<String> sources)
    {}
    
    @Override
    public void onRepopulateSources(ArrayList<Source> sources)
    {}

    @Override
    public void onVolumeChanged(String channel, boolean finalValue,
            float volume, boolean muted)
    {
        if(channel.equals(ToggleMute.DESKTOP))
        {
            this.setDesktopMuted(muted);
            this.setDesktopVolume(volume);
        }
        else if(channel.equals(ToggleMute.MICROPHONE))
        {
            this.setMicMuted(muted);
            this.setMicVolume(volume);
        }
    }

    @Override
    public void onVersionMismatch(float version)
    {}
}

package com.example.contentapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.contentapp.matter.MatterAgentClient;
import com.matter.tv.app.api.Clusters;
import com.matter.tv.app.api.MatterIntentConstants;
import com.matter.tv.app.api.SetSupportedClustersRequest;
import com.matter.tv.app.api.SupportedCluster;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = "ContentAppMainActivity";
  private static final String ATTR_PS_PLAYING = "Playback State : PLAYING";
  private static final String ATTR_PS_PAUSED = "Playback State : PAUSED";
  private static final String ATTR_PS_NOT_PLAYING = "Playback State : NOT_PLAYING";
  private static final String ATTR_PS_BUFFERING = "Playback State : BUFFERING";
  private static final String ATTR_TL_LONG_BAD = "Target List : LONG BAD";
  private static final String ATTR_TL_LONG = "Target List : LONG";
  private static final String ATTR_TL_SHORT = "Target List : SHORT";

  // Additional attribute constants
  private static final String ATTR_DURATION_SHORT = "Duration : 60s";
  private static final String ATTR_DURATION_MEDIUM = "Duration : 300s";
  private static final String ATTR_DURATION_LONG = "Duration : 3600s";
  private static final String ATTR_POSITION_START = "Position : 0s";
  private static final String ATTR_POSITION_MIDDLE = "Position : 30s";
  private static final String ATTR_POSITION_END = "Position : 55s";
  private static final String ATTR_SPEED_NORMAL = "Speed : 1.0x";
  private static final String ATTR_SPEED_FAST = "Speed : 2.0x";
  private static final String ATTR_SPEED_SLOW = "Speed : 0.5x";
  private static final String ATTR_TRACKS_NONE = "Tracks : None";
  private static final String ATTR_TRACKS_AUDIO = "Tracks : Audio";
  private static final String ATTR_TRACKS_SUBTITLE = "Tracks : Subtitle";

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private String setupPIN = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    MatterAgentClient.initialize(getApplicationContext());

    setContentView(R.layout.activity_main);

    Intent intent = getIntent();
    String commandPayload = intent.getStringExtra(MatterIntentConstants.EXTRA_COMMAND_PAYLOAD);
    String commandId = intent.getStringExtra(MatterIntentConstants.EXTRA_COMMAND_ID);
    String clusterId = intent.getStringExtra(MatterIntentConstants.EXTRA_CLUSTER_ID);

    // use the text in a TextView
    TextView textView = (TextView) findViewById(R.id.commandTextView);
    textView.setText("Command Id = " + commandId + " cluster ID = " + clusterId + " Payload : " + commandPayload);

    // Setup PIN section
    Button setupPINButton = findViewById(R.id.setupPINButton);
    if (!setupPIN.isEmpty()) {
      EditText pinText = findViewById(R.id.setupPINText);
      pinText.setText(setupPIN);
    }
    setupPINButton.setOnClickListener(
            view -> {
              EditText pinText = findViewById(R.id.setupPINText);
              String pinStr = pinText.getText().toString();
              setupPIN = pinStr;
              CommandResponseHolder.getInstance()
                      .setResponseValue(
                              Clusters.AccountLogin.Id,
                              Clusters.AccountLogin.Commands.GetSetupPIN.ID,
                              "{\""
                                      + Clusters.AccountLogin.Commands.GetSetupPINResponse.Fields.SetupPIN
                                      + "\":\""
                                      + pinStr
                                      + "\"}");
            });

    // Setup playback state spinner and button
    setupPlaybackStateSpinner();
    Button attributeUpdateButton = findViewById(R.id.updateAttributeButton);
    attributeUpdateButton.setOnClickListener(
            view -> {
              Spinner dropdown = findViewById(R.id.spinnerAttribute);
              String attribute = (String) dropdown.getSelectedItem();
              updatePlaybackState(attribute);
            });

    // Setup additional attributes spinner and button
    setupAdditionalAttributesSpinner();
    Button additionalAttributeUpdateButton = findViewById(R.id.updateAdditionalAttributeButton);
    additionalAttributeUpdateButton.setOnClickListener(
            view -> {
              Spinner dropdown = findViewById(R.id.spinnerAdditionalAttributes);
              String attribute = (String) dropdown.getSelectedItem();
              updateAdditionalAttribute(attribute);
            });

    MatterAgentClient matterAgentClient = MatterAgentClient.getInstance();
    if (matterAgentClient != null) {
      SetSupportedClustersRequest supportedClustersRequest = new SetSupportedClustersRequest();
      supportedClustersRequest.supportedClusters = new ArrayList<SupportedCluster>();
      SupportedCluster supportedCluster = new SupportedCluster();
      supportedCluster.clusterIdentifier = 1;
      supportedClustersRequest.supportedClusters.add(supportedCluster);
      executorService.execute(() -> matterAgentClient.reportClusters(supportedClustersRequest));
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (intent != null) {
      String commandPayload = intent.getStringExtra(MatterIntentConstants.EXTRA_COMMAND_PAYLOAD);
      String commandId = intent.getStringExtra(MatterIntentConstants.EXTRA_COMMAND_ID);
      String clusterId = intent.getStringExtra(MatterIntentConstants.EXTRA_CLUSTER_ID);

      if (commandId != null && clusterId != null) {
        TextView textView = (TextView) findViewById(R.id.commandTextView);
        textView.setText("Command Id = " + commandId + " cluster ID = " + clusterId + " Payload : " + commandPayload);
      }
    }
  }

  private void setupPlaybackStateSpinner() {
    Spinner dropdown = findViewById(R.id.spinnerAttribute);
    String[] items =
            new String[] {
                    ATTR_PS_PLAYING,
                    ATTR_PS_PAUSED,
                    ATTR_PS_NOT_PLAYING,
                    ATTR_PS_BUFFERING,
                    ATTR_TL_LONG,
                    ATTR_TL_SHORT,
                    ATTR_TL_LONG_BAD
            };
    ArrayAdapter<String> adapter =
            new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
    dropdown.setAdapter(adapter);
  }

  private void setupAdditionalAttributesSpinner() {
    Spinner dropdown = findViewById(R.id.spinnerAdditionalAttributes);
    String[] items =
            new String[] {
                    ATTR_DURATION_SHORT,
                    ATTR_DURATION_MEDIUM,
                    ATTR_DURATION_LONG,
                    ATTR_POSITION_START,
                    ATTR_POSITION_MIDDLE,
                    ATTR_POSITION_END,
                    ATTR_SPEED_NORMAL,
                    ATTR_SPEED_FAST,
                    ATTR_SPEED_SLOW,
                    ATTR_TRACKS_NONE,
                    ATTR_TRACKS_AUDIO,
                    ATTR_TRACKS_SUBTITLE
            };
    ArrayAdapter<String> adapter =
            new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
    dropdown.setAdapter(adapter);
  }

  private void updatePlaybackState(String attribute) {
    switch (attribute) {
      case ATTR_PS_PLAYING:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.CurrentState,
                        Clusters.MediaPlayback.Types.PlaybackStateEnum.Playing);
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.CurrentState);
        break;
      case ATTR_PS_PAUSED:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.CurrentState,
                        Clusters.MediaPlayback.Types.PlaybackStateEnum.Paused);
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.CurrentState);
        break;
      case ATTR_PS_BUFFERING:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.CurrentState,
                        Clusters.MediaPlayback.Types.PlaybackStateEnum.Buffering);
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.CurrentState);
        break;
      case ATTR_PS_NOT_PLAYING:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.CurrentState,
                        Clusters.MediaPlayback.Types.PlaybackStateEnum.NotPlaying);
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.CurrentState);
        break;
      case ATTR_TL_LONG_BAD:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.TargetNavigator.Id,
                        Clusters.TargetNavigator.Attributes.TargetList,
                        AttributeHolder.TL_LONG_BAD);
        reportAttributeChange(
                Clusters.TargetNavigator.Id, Clusters.TargetNavigator.Attributes.TargetList);
        break;
      case ATTR_TL_LONG:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.TargetNavigator.Id,
                        Clusters.TargetNavigator.Attributes.TargetList,
                        AttributeHolder.TL_LONG);
        reportAttributeChange(
                Clusters.TargetNavigator.Id, Clusters.TargetNavigator.Attributes.TargetList);
        break;
      case ATTR_TL_SHORT:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.TargetNavigator.Id,
                        Clusters.TargetNavigator.Attributes.TargetList,
                        AttributeHolder.TL_SHORT);
        reportAttributeChange(
                Clusters.TargetNavigator.Id, Clusters.TargetNavigator.Attributes.TargetList);
        break;
    }
  }

  private void updateAdditionalAttribute(String attribute) {
    switch (attribute) {
      case ATTR_DURATION_SHORT:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.Duration,
                        60000L); // 60 seconds in ms
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.Duration);
        break;
      case ATTR_DURATION_MEDIUM:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.Duration,
                        300000L); // 300 seconds in ms
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.Duration);
        break;
      case ATTR_DURATION_LONG:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.Duration,
                        3600000L); // 3600 seconds in ms
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.Duration);
        break;
      case ATTR_POSITION_START:
        // Create position struct with updatedAt and position
        String positionStart = "{\"updatedAt\": " + System.currentTimeMillis() + ", \"position\": 0}";
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.SampledPosition,
                        positionStart);
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.SampledPosition);
        break;
      case ATTR_POSITION_MIDDLE:
        // Create position struct with updatedAt and position
        String positionMiddle = "{\"updatedAt\": " + System.currentTimeMillis() + ", \"position\": 30000}";
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.SampledPosition,
                        positionMiddle);
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.SampledPosition);
        break;
      case ATTR_POSITION_END:
        // Create position struct with updatedAt and position
        String positionEnd = "{\"updatedAt\": " + System.currentTimeMillis() + ", \"position\": 55000}";
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.SampledPosition,
                        positionEnd);
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.SampledPosition);
        break;
      case ATTR_SPEED_NORMAL:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.PlaybackSpeed,
                        1.0f);
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.PlaybackSpeed);
        break;
      case ATTR_SPEED_FAST:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.PlaybackSpeed,
                        2.0f);
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.PlaybackSpeed);
        break;
      case ATTR_SPEED_SLOW:
        AttributeHolder.getInstance()
                .setAttributeValue(
                        Clusters.MediaPlayback.Id,
                        Clusters.MediaPlayback.Attributes.PlaybackSpeed,
                        0.5f);
        reportAttributeChange(
                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.PlaybackSpeed);
        break;
      case ATTR_TRACKS_NONE:
        // Empty list for no tracks
//        AttributeHolder.getInstance()
//                .setAttributeValue(
//                        Clusters.MediaPlayback.Id,
//                        Clusters.MediaPlayback.Attributes.AvailableAudioTracks,
//                        "[]");
//        reportAttributeChange(
//                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.AvailableAudioTracks);
//        AttributeHolder.getInstance()
//                .setAttributeValue(
//                        Clusters.MediaPlayback.Id,
//                        Clusters.MediaPlayback.Attributes.AvailableTextTracks,
//                        "[]");
//        reportAttributeChange(
//                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.AvailableTextTracks);
        break;
      case ATTR_TRACKS_AUDIO:
        // Example audio tracks
//        String audioTracks = "[{\"id\":\"audio1\",\"trackAttributes\":{\"languageCode\":\"en\",\"displayName\":\"English\"}}, {\"id\":\"audio2\",\"trackAttributes\":{\"languageCode\":\"es\",\"displayName\":\"Spanish\"}}]";
//        AttributeHolder.getInstance()
//                .setAttributeValue(
//                        Clusters.MediaPlayback.Id,
//                        Clusters.MediaPlayback.Attributes.AvailableAudioTracks,
//                        audioTracks);
//        reportAttributeChange(
//                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.AvailableAudioTracks);
//        AttributeHolder.getInstance()
//                .setAttributeValue(
//                        Clusters.MediaPlayback.Id,
//                        Clusters.MediaPlayback.Attributes.ActiveAudioTrack,
//                        "{\"id\":\"audio1\",\"trackAttributes\":{\"languageCode\":\"en\",\"displayName\":\"English\"}}");
//        reportAttributeChange(
//                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.ActiveAudioTrack);
        break;
      case ATTR_TRACKS_SUBTITLE:
        // Example subtitle tracks
//        String subtitleTracks = "[{\"id\":\"sub1\",\"trackAttributes\":{\"languageCode\":\"en\",\"displayName\":\"English\"}}, {\"id\":\"sub2\",\"trackAttributes\":{\"languageCode\":\"es\",\"displayName\":\"Spanish\"}}]";
//        AttributeHolder.getInstance()
//                .setAttributeValue(
//                        Clusters.MediaPlayback.Id,
//                        Clusters.MediaPlayback.Attributes.AvailableTextTracks,
//                        subtitleTracks);
//        reportAttributeChange(
//                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.AvailableTextTracks);
//        AttributeHolder.getInstance()
//                .setAttributeValue(
//                        Clusters.MediaPlayback.Id,
//                        Clusters.MediaPlayback.Attributes.ActiveTextTrack,
//                        "{\"id\":\"sub1\",\"trackAttributes\":{\"languageCode\":\"en\",\"displayName\":\"English\"}}");
//        reportAttributeChange(
//                Clusters.MediaPlayback.Id, Clusters.MediaPlayback.Attributes.ActiveTextTrack);
        break;
    }
  }

  private void reportAttributeChange(final int clusterId, final int attributeId) {
    executorService.execute(
            new Runnable() {
              @Override
              public void run() {
                MatterAgentClient client = MatterAgentClient.getInstance();
                client.reportAttributeChange(clusterId, attributeId);
              }
            });
  }
}
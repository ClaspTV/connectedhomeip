/*
 *   Copyright (c) 2024 Project CHIP Authors
 *   All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.matter.casting;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chip.casting.R;
import com.matter.casting.core.CastingApp;
import com.matter.casting.core.CastingPlayer;
import com.matter.casting.core.Endpoint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.fragment.app.Fragment;
import chip.devicecontroller.ChipClusters;
import chip.devicecontroller.ChipStructs;

/**
 * A {@link Fragment} to test MediaPlayback cluster functionality including commands
 * and attributes.
 */
public class MediaPlaybackTestFragment extends Fragment {
    private static final String TAG = MediaPlaybackTestFragment.class.getSimpleName();
    private static final int DEFAULT_ENDPOINT_ID_FOR_CGP_FLOW = 1;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private final CastingPlayer selectedCastingPlayer;
    private final boolean useCommissionerGeneratedPasscode;

    private TextView statusTextView;
    private ChipClusters.MediaPlaybackCluster mediaPlaybackCluster;
    private Handler mainHandler;

    // Current state values
    private int currentState = -1;
    private Long startTime = null;
    private Long duration = null;
    private Float playbackSpeed = null;
    private ChipStructs.MediaPlaybackClusterPlaybackPositionStruct sampledPosition = null;
    private Long seekRangeStart = null;
    private Long seekRangeEnd = null;

    public MediaPlaybackTestFragment(
            CastingPlayer selectedCastingPlayer, boolean useCommissionerGeneratedPasscode) {
        this.selectedCastingPlayer = selectedCastingPlayer;
        this.useCommissionerGeneratedPasscode = useCommissionerGeneratedPasscode;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @param selectedCastingPlayer CastingPlayer that the casting app connected to.
     * @param useCommissionerGeneratedPasscode Boolean indicating whether this CastingPlayer was
     *     commissioned using the Commissioner-Generated Passcode (CGP) commissioning flow.
     * @return A new instance of fragment MediaPlaybackTestFragment.
     */
    public static MediaPlaybackTestFragment newInstance(
            CastingPlayer selectedCastingPlayer, boolean useCommissionerGeneratedPasscode) {
        return new MediaPlaybackTestFragment(
                selectedCastingPlayer, useCommissionerGeneratedPasscode);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_media_playback_test, container, false);

        // Initialize the status text view
        statusTextView = view.findViewById(R.id.status_text_view);

//        // Get the endpoint with MediaPlayback cluster
        Endpoint endpoint;
//        if (useCommissionerGeneratedPasscode) {
//            endpoint = EndpointSelectorExample.selectEndpointById(
//                    selectedCastingPlayer, DEFAULT_ENDPOINT_ID_FOR_CGP_FLOW);
//        } else {
//            endpoint = EndpointSelectorExample.selectFirstEndpointByVID(selectedCastingPlayer);
//        }

        endpoint = EndpointSelectorExample.selectFirstEndpointByVID(selectedCastingPlayer);
        if (endpoint == null) {
            logError("No suitable endpoint found on CastingPlayer");
            return view;
        }

        // Get the MediaPlaybackCluster
        mediaPlaybackCluster = endpoint.getCluster(ChipClusters.MediaPlaybackCluster.class);
        if (mediaPlaybackCluster == null) {
            logError("Could not get MediaPlaybackCluster for endpoint with ID: " + endpoint.getId());
            return view;
        }

        // Set up all button click listeners
        setupButtonListeners(view);

        return view;
    }

    private void setupButtonListeners(View view) {
        // Attribute section buttons
        view.findViewById(R.id.subscribe_current_state_button).setOnClickListener(v -> subscribeCurrentState());
        view.findViewById(R.id.read_attributes_button).setOnClickListener(v -> readAllAttributes());

        // Media controls
        view.findViewById(R.id.play_button).setOnClickListener(v -> sendPlayCommand());
        view.findViewById(R.id.pause_button).setOnClickListener(v -> sendPauseCommand());
        view.findViewById(R.id.stop_button).setOnClickListener(v -> sendStopCommand());
        view.findViewById(R.id.start_over_button).setOnClickListener(v -> sendStartOverCommand());
        view.findViewById(R.id.previous_button).setOnClickListener(v -> sendPreviousCommand());
        view.findViewById(R.id.next_button).setOnClickListener(v -> sendNextCommand());
        view.findViewById(R.id.rewind_button).setOnClickListener(v -> sendRewindCommand());
        view.findViewById(R.id.fast_forward_button).setOnClickListener(v -> sendFastForwardCommand());
        view.findViewById(R.id.skip_forward_button).setOnClickListener(v -> sendSkipForwardCommand(10000L));
        view.findViewById(R.id.skip_backward_button).setOnClickListener(v -> sendSkipBackwardCommand(10000L));
        view.findViewById(R.id.seek_button).setOnClickListener(v -> sendSeekCommand(30000L));

        // Shutdown subscriptions button
        view.findViewById(R.id.shutdown_subscriptions_button).setOnClickListener(v -> {
            logInfo("Shutting down all subscriptions");
            CastingApp.getInstance().shutdownAllSubscriptions();
        });
    }

    private void logInfo(String message) {
        Log.d(TAG, message);
        appendToLog("INFO: " + message);
    }

    private void logError(String message) {
        Log.e(TAG, message);
        appendToLog("ERROR: " + message);
    }

    private void appendToLog(String message) {
        mainHandler.post(() -> {
            String timeStamp = DATE_FORMAT.format(new Date());
            String currentText = statusTextView.getText().toString();

            // If this is the first message, clear the initial text
            if (currentText.equals(getString(R.string.status_initial_text))) {
                currentText = "";
            }

            // Add the new log message
            String newText = "[" + timeStamp + "] " + message + "\n\n" + currentText;

            // Limit log size to prevent performance issues
            if (newText.length() > 5000) {
                newText = newText.substring(0, 5000);
            }

            statusTextView.setText(newText);
        });
    }

    // Subscribe to CurrentState attribute
    private void subscribeCurrentState() {
        logInfo("Subscribing to CurrentState attribute");

        mediaPlaybackCluster.subscribeCurrentStateAttribute(
                new ChipClusters.IntegerAttributeCallback() {
                    @Override
                    public void onSuccess(int value) {
                        currentState = value;
                        String stateString = getStateString(value);
                        logInfo("CurrentState update: " + value + " (" + stateString + ")");
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("CurrentState subscription error: " + error);
                    }
                }, 0, 10); // min interval 0, max interval 10 seconds
    }

    // Read all relevant attributes
    private void readAllAttributes() {
        logInfo("Reading all MediaPlayback attributes...");

        // Read CurrentState
        mediaPlaybackCluster.readCurrentStateAttribute(
                new ChipClusters.IntegerAttributeCallback() {
                    @Override
                    public void onSuccess(int value) {
                        currentState = value;
                        String stateString = getStateString(value);
                        logInfo("CurrentState: " + value + " (" + stateString + ")");
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Failed to read CurrentState: " + error);
                    }
                });

        // Read StartTime
        mediaPlaybackCluster.readStartTimeAttribute(
                new ChipClusters.MediaPlaybackCluster.StartTimeAttributeCallback() {
                    @Override
                    public void onSuccess(Long value) {
                        startTime = value;
                        logInfo("StartTime: " + (value != null ? value : "null"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Failed to read StartTime: " + error);
                    }
                });

        // Read Duration
        mediaPlaybackCluster.readDurationAttribute(
                new ChipClusters.MediaPlaybackCluster.DurationAttributeCallback() {
                    @Override
                    public void onSuccess(Long value) {
                        duration = value;
                        logInfo("Duration: " + (value != null ? value + " ms" : "null"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Failed to read Duration: " + error);
                    }
                });

        // Read PlaybackSpeed
        mediaPlaybackCluster.readPlaybackSpeedAttribute(
                new ChipClusters.FloatAttributeCallback() {
                    @Override
                    public void onSuccess(float value) {
                        playbackSpeed = value;
                        logInfo("PlaybackSpeed: " + value + "x");
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Failed to read PlaybackSpeed: " + error);
                    }
                });

        // Read SampledPosition
        mediaPlaybackCluster.readSampledPositionAttribute(
                new ChipClusters.MediaPlaybackCluster.SampledPositionAttributeCallback() {
                    @Override
                    public void onSuccess(ChipStructs.MediaPlaybackClusterPlaybackPositionStruct value) {
                        sampledPosition = value;
                        if (value != null) {
                            logInfo("SampledPosition: updatedAt=" + value.updatedAt +
                                    ", position=" + (value.position != null ? value.position + " ms" : "null"));
                        } else {
                            logInfo("SampledPosition: null");
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Failed to read SampledPosition: " + error);
                    }
                });

        // Read SeekRangeStart
        mediaPlaybackCluster.readSeekRangeStartAttribute(
                new ChipClusters.MediaPlaybackCluster.SeekRangeStartAttributeCallback() {
                    @Override
                    public void onSuccess(Long value) {
                        seekRangeStart = value;
                        logInfo("SeekRangeStart: " + (value != null ? value + " ms" : "null"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Failed to read SeekRangeStart: " + error);
                    }
                });

        // Read SeekRangeEnd
        mediaPlaybackCluster.readSeekRangeEndAttribute(
                new ChipClusters.MediaPlaybackCluster.SeekRangeEndAttributeCallback() {
                    @Override
                    public void onSuccess(Long value) {
                        seekRangeEnd = value;
                        logInfo("SeekRangeEnd: " + (value != null ? value + " ms" : "null"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Failed to read SeekRangeEnd: " + error);
                    }
                });

        // Read Available Audio Tracks (if any)
        mediaPlaybackCluster.readAvailableAudioTracksAttribute(
                new ChipClusters.MediaPlaybackCluster.AvailableAudioTracksAttributeCallback() {
                    @Override
                    public void onSuccess(List<ChipStructs.MediaPlaybackClusterTrackStruct> value) {
                        if (value != null && !value.isEmpty()) {
                            logInfo("AvailableAudioTracks count: " + value.size());
                            for (int i = 0; i < value.size(); i++) {
                                ChipStructs.MediaPlaybackClusterTrackStruct track = value.get(i);
                                logInfo("  Track " + i + ": id=" + track.id);
                            }
                        } else {
                            logInfo("AvailableAudioTracks: " + (value == null ? "null" : "empty"));
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Failed to read AvailableAudioTracks: " + error);
                    }
                });
        // Read Available Text Tracks (if any)
        mediaPlaybackCluster.readAvailableTextTracksAttribute(
                new ChipClusters.MediaPlaybackCluster.AvailableTextTracksAttributeCallback() {
                    @Override
                    public void onSuccess(List<ChipStructs.MediaPlaybackClusterTrackStruct> value) {
                        if (value != null && !value.isEmpty()) {
                            logInfo("AvailableTextTracks count: " + value.size());
                            for (int i = 0; i < value.size(); i++) {
                                ChipStructs.MediaPlaybackClusterTrackStruct track = value.get(i);
                                logInfo("  Track " + i + ": id=" + track.id);
                            }
                        } else {
                            logInfo("AvailableTextTracks: " + (value == null ? "null" : "empty"));
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Failed to read AvailableTextTracks: " + error);
                    }
                });

        // Read Active Audio Track (if any)
        mediaPlaybackCluster.readActiveAudioTrackAttribute(
                new ChipClusters.MediaPlaybackCluster.ActiveAudioTrackAttributeCallback() {
                    @Override
                    public void onSuccess(ChipStructs.MediaPlaybackClusterTrackStruct value) {
                        if (value != null) {
                            logInfo("ActiveAudioTrack: id=" + value.id);
                        } else {
                            logInfo("ActiveAudioTrack: null");
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Failed to read ActiveAudioTrack: " + error);
                    }
                });

        // Read Active Text Track (if any)
        mediaPlaybackCluster.readActiveTextTrackAttribute(
                new ChipClusters.MediaPlaybackCluster.ActiveTextTrackAttributeCallback() {
                    @Override
                    public void onSuccess(ChipStructs.MediaPlaybackClusterTrackStruct value) {
                        if (value != null) {
                            logInfo("ActiveTextTrack: id=" + value.id);
                        } else {
                            logInfo("ActiveTextTrack: null");
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Failed to read ActiveTextTrack: " + error);
                    }
                });
    }

    // Play command
    private void sendPlayCommand() {
        logInfo("Sending Play command...");
        mediaPlaybackCluster.play(
                new ChipClusters.MediaPlaybackCluster.PlaybackResponseCallback() {
                    @Override
                    public void onSuccess(Integer status, java.util.Optional<String> data) {
                        logInfo("Play command response: status=" + status +
                                ", data=" + (data.isPresent() ? data.get() : "empty"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Play command error: " + error);
                    }
                });
    }

    // Pause command
    private void sendPauseCommand() {
        logInfo("Sending Pause command...");
        mediaPlaybackCluster.pause(
                new ChipClusters.MediaPlaybackCluster.PlaybackResponseCallback() {
                    @Override
                    public void onSuccess(Integer status, java.util.Optional<String> data) {
                        logInfo("Pause command response: status=" + status +
                                ", data=" + (data.isPresent() ? data.get() : "empty"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Pause command error: " + error);
                    }
                });
    }

    // Stop command
    private void sendStopCommand() {
        logInfo("Sending Stop command...");
        mediaPlaybackCluster.stop(
                new ChipClusters.MediaPlaybackCluster.PlaybackResponseCallback() {
                    @Override
                    public void onSuccess(Integer status, java.util.Optional<String> data) {
                        logInfo("Stop command response: status=" + status +
                                ", data=" + (data.isPresent() ? data.get() : "empty"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Stop command error: " + error);
                    }
                });
    }

    // StartOver command
    private void sendStartOverCommand() {
        logInfo("Sending StartOver command...");
        mediaPlaybackCluster.startOver(
                new ChipClusters.MediaPlaybackCluster.PlaybackResponseCallback() {
                    @Override
                    public void onSuccess(Integer status, java.util.Optional<String> data) {
                        logInfo("StartOver command response: status=" + status +
                                ", data=" + (data.isPresent() ? data.get() : "empty"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("StartOver command error: " + error);
                    }
                });
    }

    // Previous command
    private void sendPreviousCommand() {
        logInfo("Sending Previous command...");
        mediaPlaybackCluster.previous(
                new ChipClusters.MediaPlaybackCluster.PlaybackResponseCallback() {
                    @Override
                    public void onSuccess(Integer status, java.util.Optional<String> data) {
                        logInfo("Previous command response: status=" + status +
                                ", data=" + (data.isPresent() ? data.get() : "empty"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Previous command error: " + error);
                    }
                });
    }

    // Next command
    private void sendNextCommand() {
        logInfo("Sending Next command...");
        mediaPlaybackCluster.next(
                new ChipClusters.MediaPlaybackCluster.PlaybackResponseCallback() {
                    @Override
                    public void onSuccess(Integer status, java.util.Optional<String> data) {
                        logInfo("Next command response: status=" + status +
                                ", data=" + (data.isPresent() ? data.get() : "empty"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Next command error: " + error);
                    }
                });
    }

    // Rewind command
    private void sendRewindCommand() {
        logInfo("Sending Rewind command...");
        mediaPlaybackCluster.rewind(
                new ChipClusters.MediaPlaybackCluster.PlaybackResponseCallback() {
                    @Override
                    public void onSuccess(Integer status, java.util.Optional<String> data) {
                        logInfo("Rewind command response: status=" + status +
                                ", data=" + (data.isPresent() ? data.get() : "empty"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Rewind command error: " + error);
                    }
                },
                java.util.Optional.of(true)); // audioAdvanceUnmuted = true
    }

    // FastForward command
    private void sendFastForwardCommand() {
        logInfo("Sending FastForward command...");
        mediaPlaybackCluster.fastForward(
                new ChipClusters.MediaPlaybackCluster.PlaybackResponseCallback() {
                    @Override
                    public void onSuccess(Integer status, java.util.Optional<String> data) {
                        logInfo("FastForward command response: status=" + status +
                                ", data=" + (data.isPresent() ? data.get() : "empty"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("FastForward command error: " + error);
                    }
                },
                java.util.Optional.of(true)); // audioAdvanceUnmuted = true
    }

    // SkipForward command
    private void sendSkipForwardCommand(Long deltaPositionMilliseconds) {
        logInfo("Sending SkipForward command (delta=" + deltaPositionMilliseconds + "ms)...");
        mediaPlaybackCluster.skipForward(
                new ChipClusters.MediaPlaybackCluster.PlaybackResponseCallback() {
                    @Override
                    public void onSuccess(Integer status, java.util.Optional<String> data) {
                        logInfo("SkipForward command response: status=" + status +
                                ", data=" + (data.isPresent() ? data.get() : "empty"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("SkipForward command error: " + error);
                    }
                },
                deltaPositionMilliseconds);
    }

    // SkipBackward command
    private void sendSkipBackwardCommand(Long deltaPositionMilliseconds) {
        logInfo("Sending SkipBackward command (delta=" + deltaPositionMilliseconds + "ms)...");
        mediaPlaybackCluster.skipBackward(
                new ChipClusters.MediaPlaybackCluster.PlaybackResponseCallback() {
                    @Override
                    public void onSuccess(Integer status, java.util.Optional<String> data) {
                        logInfo("SkipBackward command response: status=" + status +
                                ", data=" + (data.isPresent() ? data.get() : "empty"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("SkipBackward command error: " + error);
                    }
                },
                deltaPositionMilliseconds);
    }

    // Seek command
    private void sendSeekCommand(Long position) {
        logInfo("Sending Seek command (position=" + position + "ms)...");
        mediaPlaybackCluster.seek(
                new ChipClusters.MediaPlaybackCluster.PlaybackResponseCallback() {
                    @Override
                    public void onSuccess(Integer status, java.util.Optional<String> data) {
                        logInfo("Seek command response: status=" + status +
                                ", data=" + (data.isPresent() ? data.get() : "empty"));
                    }

                    @Override
                    public void onError(Exception error) {
                        logError("Seek command error: " + error);
                    }
                },
                position);
    }

    // Helper method to convert state value to string
    private String getStateString(int state) {
        switch (state) {
            case 0: return "Playing";
            case 1: return "Paused";
            case 2: return "NotPlaying";
            case 3: return "Buffering";
            default: return "Unknown (" + state + ")";
        }
    }
}
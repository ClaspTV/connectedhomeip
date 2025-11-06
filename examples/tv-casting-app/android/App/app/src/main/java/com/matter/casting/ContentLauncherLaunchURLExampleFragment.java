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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import chip.devicecontroller.ChipClusters;
import chip.devicecontroller.ChipStructs;
import com.chip.casting.R;
import com.matter.casting.core.CastingPlayer;
import com.matter.casting.core.Endpoint;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Optional;

/** A {@link Fragment} to send Content Launcher LaunchURL command using the TV Casting App. */
public class ContentLauncherLaunchURLExampleFragment extends Fragment {
  private static final String TAG = ContentLauncherLaunchURLExampleFragment.class.getSimpleName();
  private static final Integer SAMPLE_ENDPOINT_VID = 65521;

  private final CastingPlayer selectedCastingPlayer;
  private final boolean useCommissionerGeneratedPasscode;

  private View.OnClickListener launchUrlButtonClickListener;
  private View.OnClickListener launchContentButtonClickListener;

  public ContentLauncherLaunchURLExampleFragment(
      CastingPlayer selectedCastingPlayer, boolean useCommissionerGeneratedPasscode) {
    this.selectedCastingPlayer = selectedCastingPlayer;
    this.useCommissionerGeneratedPasscode = useCommissionerGeneratedPasscode;
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @param selectedCastingPlayer CastingPlayer that the casting app connected to.
   * @param useCommissionerGeneratedPasscode Boolean indicating whether this CastingPlayer was
   *     commissioned using the Commissioner-Generated Passcode (CGP) commissioning flow.
   * @return A new instance of fragment ContentLauncherLaunchURLExampleFragment.
   */
  public static ContentLauncherLaunchURLExampleFragment newInstance(
      CastingPlayer selectedCastingPlayer, Boolean useCommissionerGeneratedPasscode) {
    return new ContentLauncherLaunchURLExampleFragment(
        selectedCastingPlayer, useCommissionerGeneratedPasscode);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    this.launchUrlButtonClickListener =
        v -> {
          Endpoint endpoint =
              EndpointSelectorExample.selectFirstEndpointByVID(selectedCastingPlayer);
          if (endpoint == null) {
            Log.e(TAG, "No Endpoint with sample vendorID found on CastingPlayer");
            return;
          }

          EditText contentUrlEditText = getView().findViewById(R.id.contentUrlEditText);
          String contentUrl = contentUrlEditText.getText().toString();
          EditText contentDisplayStringEditText =
              getView().findViewById(R.id.contentDisplayStringEditText);
          String contentDisplayString = contentDisplayStringEditText.getText().toString();

          // get ChipClusters.ContentLauncherCluster from the endpoint
          ChipClusters.ContentLauncherCluster cluster =
              endpoint.getCluster(ChipClusters.ContentLauncherCluster.class);
          if (cluster == null) {
            Log.e(
                TAG,
                "Could not get ContentLauncherCluster for endpoint with ID: " + endpoint.getId());
            return;
          }

          // call launchURL on the cluster object while passing in a
          // ChipClusters.ContentLauncherCluster.LauncherResponseCallback and request parameters
          cluster.launchURL(
              new ChipClusters.ContentLauncherCluster.LauncherResponseCallback() {
                @Override
                public void onSuccess(Integer status, Optional<String> data) {
                  Log.d(TAG, "LaunchURL success. Status: " + status + ", Data: " + data);
                  new Handler(Looper.getMainLooper())
                      .post(
                          () -> {
                            TextView launcherResult = getView().findViewById(R.id.launcherResult);
                            launcherResult.setText(
                                "LaunchURL result\nStatus: " + status + ", Data: " + data);
                          });
                }

                @Override
                public void onError(Exception error) {
                  Log.e(TAG, "LaunchURL failure " + error);
                  new Handler(Looper.getMainLooper())
                      .post(
                          () -> {
                            TextView launcherResult = getView().findViewById(R.id.launcherResult);
                            launcherResult.setText("LaunchURL result\nError: " + error);
                          });
                }
              },
              contentUrl,
              Optional.of(contentDisplayString),
              Optional.empty());
        };
    return inflater.inflate(R.layout.fragment_matter_content_launcher_launch_url, container, false);
  }

    // Add this method to create a ContentSearch struct from VideoInfo fields
    private ChipStructs.ContentLauncherClusterContentSearchStruct createContentSearch(String videoGuid) {
        // Create a parameter structure to identify the content
        ArrayList<ChipStructs.ContentLauncherClusterParameterStruct> parameters = new ArrayList<>();

        // Parameter 1: GUID (type 0 = ID)
        ChipStructs.ContentLauncherClusterParameterStruct guidParam =
                new ChipStructs.ContentLauncherClusterParameterStruct(
                        0, // type = ID
                        videoGuid, // value = the GUID
                        java.util.Optional.empty() // No external IDs
                );
        parameters.add(guidParam);

        // Sample data for additional metadata that would come from VideoInfo
        try {
            // Parameter 2: Title (type 1 = Name)
            ChipStructs.ContentLauncherClusterParameterStruct titleParam =
                    new ChipStructs.ContentLauncherClusterParameterStruct(
                            1, // type = Name
                            "Sample Video Title", // value = title
                            java.util.Optional.empty() // No external IDs
                    );
            parameters.add(titleParam);

            // Parameter 3: Content Type (type 2 = Type)
            ChipStructs.ContentLauncherClusterParameterStruct typeParam =
                    new ChipStructs.ContentLauncherClusterParameterStruct(
                            2, // type = Type
                            "video/mp4", // value = content type
                            java.util.Optional.empty() // No external IDs
                    );
            parameters.add(typeParam);

            // Create externalIDs for URL parameter
            ArrayList<ChipStructs.ContentLauncherClusterAdditionalInfoStruct> externalIDs =
                    new ArrayList<>();

            // Add video URL as external ID
            ChipStructs.ContentLauncherClusterAdditionalInfoStruct urlInfo =
                    new ChipStructs.ContentLauncherClusterAdditionalInfoStruct(
                            "url",
                            "https://example.com/videos/sample.mp4"
                    );
            externalIDs.add(urlInfo);

            // Add image URL as external ID
            ChipStructs.ContentLauncherClusterAdditionalInfoStruct imageInfo =
                    new ChipStructs.ContentLauncherClusterAdditionalInfoStruct(
                            "imageUrl",
                            "https://example.com/images/sample.jpg"
                    );
            externalIDs.add(imageInfo);

            // Add subtitle as external ID
            ChipStructs.ContentLauncherClusterAdditionalInfoStruct subtitleInfo =
                    new ChipStructs.ContentLauncherClusterAdditionalInfoStruct(
                            "subtitle",
                            "Sample Subtitle"
                    );
            externalIDs.add(subtitleInfo);

            // Add description as external ID
            ChipStructs.ContentLauncherClusterAdditionalInfoStruct descriptionInfo =
                    new ChipStructs.ContentLauncherClusterAdditionalInfoStruct(
                            "description",
                            "This is a sample video description"
                    );
            externalIDs.add(descriptionInfo);

            // Add luid as external ID
            ChipStructs.ContentLauncherClusterAdditionalInfoStruct luidInfo =
                    new ChipStructs.ContentLauncherClusterAdditionalInfoStruct(
                            "luid",
                            "local_unique_id_123"
                    );
            externalIDs.add(luidInfo);

            // Add isLive flag as external ID
            ChipStructs.ContentLauncherClusterAdditionalInfoStruct isLiveInfo =
                    new ChipStructs.ContentLauncherClusterAdditionalInfoStruct(
                            "isLive",
                            "false"
                    );
            externalIDs.add(isLiveInfo);

            // Add protocolType as external ID
            ChipStructs.ContentLauncherClusterAdditionalInfoStruct protocolInfo =
                    new ChipStructs.ContentLauncherClusterAdditionalInfoStruct(
                            "protocolType",
                            "HLS"
                    );
            externalIDs.add(protocolInfo);

            // Add DRM information as external IDs
            ChipStructs.ContentLauncherClusterAdditionalInfoStruct drmTypeInfo =
                    new ChipStructs.ContentLauncherClusterAdditionalInfoStruct(
                            "drmType",
                            "widevine"
                    );
            externalIDs.add(drmTypeInfo);

            ChipStructs.ContentLauncherClusterAdditionalInfoStruct drmLicenseInfo =
                    new ChipStructs.ContentLauncherClusterAdditionalInfoStruct(
                            "drmLicenseURL",
                            "https://example.com/license"
                    );
            externalIDs.add(drmLicenseInfo);

            ChipStructs.ContentLauncherClusterAdditionalInfoStruct drmCustomDataInfo =
                    new ChipStructs.ContentLauncherClusterAdditionalInfoStruct(
                            "drmCustomData",
                            "custom_drm_data_here"
                    );
            externalIDs.add(drmCustomDataInfo);

            // Add customMetadata JSON as external ID
            JSONObject customMetadataObj = new JSONObject();
            customMetadataObj.put("provider", "Sample Provider");
            customMetadataObj.put("genre", "Action");
            customMetadataObj.put("year", 2024);

            ChipStructs.ContentLauncherClusterAdditionalInfoStruct customMetadataInfo =
                    new ChipStructs.ContentLauncherClusterAdditionalInfoStruct(
                            "customMetadata",
                            customMetadataObj.toString()
                    );
            externalIDs.add(customMetadataInfo);

            // Add customStreamInfo JSON as external ID
            JSONObject customStreamInfoObj = new JSONObject();
            customStreamInfoObj.put("bitrate", 5000000);
            customStreamInfoObj.put("resolution", "1080p");
            customStreamInfoObj.put("codec", "h264");

            ChipStructs.ContentLauncherClusterAdditionalInfoStruct customStreamInfo =
                    new ChipStructs.ContentLauncherClusterAdditionalInfoStruct(
                            "customStreamInfo",
                            customStreamInfoObj.toString()
                    );
            externalIDs.add(customStreamInfo);

            // Parameter 4: Additional Info (type 3 = Source)
            ChipStructs.ContentLauncherClusterParameterStruct sourceParam =
                    new ChipStructs.ContentLauncherClusterParameterStruct(
                            3, // type = Source
                            "metadata", // value = source identifier
                            java.util.Optional.of(externalIDs) // Include all metadata
                    );
            parameters.add(sourceParam);
        } catch (Exception e) {
            Log.e(TAG, "Error creating content search parameters", e);
        }

        // Create the final content search struct
        return new ChipStructs.ContentLauncherClusterContentSearchStruct(parameters);
    }

    // Add this method to create PlaybackPreferences struct
    private java.util.Optional<ChipStructs.ContentLauncherClusterPlaybackPreferencesStruct> createPlaybackPreferences() {
        try {
            // Create a simple text track preference
            ArrayList<Integer> textTrackTypes = new ArrayList<>();
            textTrackTypes.add(0); // 0 = Default
            textTrackTypes.add(1); // 1 = Subtitle
            ChipStructs.ContentLauncherClusterTrackPreferenceStruct textTrack =
                    new ChipStructs.ContentLauncherClusterTrackPreferenceStruct(
                            "en", // language code
                            java.util.Optional.of(textTrackTypes),
                            0 // display name
                    );

            // Create audio tracks preferences
            ArrayList<ChipStructs.ContentLauncherClusterTrackPreferenceStruct> audioTracks =
                    new ArrayList<>();

            // Add English audio track
            ArrayList<Integer> englishAudioTypes = new ArrayList<>();
            englishAudioTypes.add(0); // 0 = Default
            englishAudioTypes.add(1); // 1 = Audio
            ChipStructs.ContentLauncherClusterTrackPreferenceStruct englishAudio =
                    new ChipStructs.ContentLauncherClusterTrackPreferenceStruct(
                            "en", // language code
                            java.util.Optional.of(englishAudioTypes),
                            0
                    );
            audioTracks.add(englishAudio);

            // Create the playback preferences struct
            ChipStructs.ContentLauncherClusterPlaybackPreferencesStruct preferences =
                    new ChipStructs.ContentLauncherClusterPlaybackPreferencesStruct(
                            15000L, // playback position in milliseconds
                            textTrack, // text track preference
                            java.util.Optional.of(audioTracks) // audio tracks preferences
                    );

            return java.util.Optional.of(preferences);
        } catch (Exception e) {
            Log.e(TAG, "Error creating playback preferences", e);
            return java.util.Optional.empty();
        }
    }


    @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "ContentLauncherLaunchURLExampleFragment.onViewCreated called");
        this.launchContentButtonClickListener =
                v -> {
                    Endpoint endpoint = EndpointSelectorExample.selectFirstEndpointByVID(selectedCastingPlayer);
                    if (endpoint == null) {
                        Log.e(TAG, "No Endpoint with sample vendorID found on CastingPlayer");
                        return;
                    }

                    // Get UI values
                    EditText videoGuidEditText = getView().findViewById(R.id.videoGuidEditText);
                    String videoGuid = videoGuidEditText.getText().toString();
                    CheckBox autoPlayCheckBox = getView().findViewById(R.id.autoPlayCheckBox);
                    Boolean autoPlay = autoPlayCheckBox.isChecked();

                    // Get ContentLauncherCluster from the endpoint
                    ChipClusters.ContentLauncherCluster cluster =
                            endpoint.getCluster(ChipClusters.ContentLauncherCluster.class);
                    if (cluster == null) {
                        Log.e(
                                TAG,
                                "Could not get ContentLauncherCluster for endpoint with ID: " + endpoint.getId());
                        return;
                    }

                    // Create search parameters from VideoInfo data
                    ChipStructs.ContentLauncherClusterContentSearchStruct search = createContentSearch(videoGuid);

                    // Create playback preferences
                    java.util.Optional<ChipStructs.ContentLauncherClusterPlaybackPreferencesStruct> playbackPreferences =
                            createPlaybackPreferences();

                    // Call launchContent
                    cluster.launchContent(
                            new ChipClusters.ContentLauncherCluster.LauncherResponseCallback() {
                                @Override
                                public void onSuccess(Integer status, Optional<String> responseData) {
                                    Log.d(TAG, "LaunchContent success. Status: " + status + ", Data: " + responseData);
                                    new Handler(Looper.getMainLooper())
                                            .post(
                                                    () -> {
                                                        TextView launcherResult = getView().findViewById(R.id.launcherResult);
                                                        launcherResult.setText(
                                                                "LaunchContent result\nStatus: " + status + ", Data: " + responseData);
                                                    });
                                }

                                @Override
                                public void onError(Exception error) {
                                    Log.e(TAG, "LaunchContent failure " + error);
                                    new Handler(Looper.getMainLooper())
                                            .post(
                                                    () -> {
                                                        TextView launcherResult = getView().findViewById(R.id.launcherResult);
                                                        launcherResult.setText("LaunchContent result\nError: " + error);
                                                    });
                                }
                            },
                            search,
                            autoPlay,
                            Optional.empty(), // CANNOT add big objects here. We will encounter a "Encode Error: src/lib/core/TLVWriter.cpp:802: CHIP Error 0x00000019: Buffer too small" error.
                            playbackPreferences,
                            Optional.of(true)); // Use current context
                };

// In the onViewCreated method, add this line to set the click listener for the new button:
        getView().findViewById(R.id.launchContentButton).setOnClickListener(launchContentButtonClickListener);
    getView().findViewById(R.id.launchUrlButton).setOnClickListener(launchUrlButtonClickListener);
  }
}

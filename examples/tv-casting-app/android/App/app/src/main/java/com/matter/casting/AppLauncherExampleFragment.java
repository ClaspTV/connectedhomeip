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
import android.widget.EditText;
import android.widget.TextView;

import com.chip.casting.R;
import com.matter.casting.core.CastingApp;
import com.matter.casting.core.CastingPlayer;
import com.matter.casting.core.Endpoint;

import java.util.List;
import java.util.Optional;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import chip.devicecontroller.ChipClusters;
import chip.devicecontroller.ChipStructs;

/** A {@link Fragment} to send Content Launcher LaunchURL command using the TV Casting App. */
public class AppLauncherExampleFragment extends Fragment {
  private static final String TAG = AppLauncherExampleFragment.class.getSimpleName();
  private static final Integer SAMPLE_ENDPOINT_VID = 65521;
  private static final int DEFAULT_ENDPOINT_ID_FOR_CGP_FLOW = 1;

  private final CastingPlayer selectedCastingPlayer;
  private final boolean useCommissionerGeneratedPasscode;

  private View.OnClickListener launchAppButtonClickListener;
  private View.OnClickListener getAppStatusButtonClickListener;
  private View.OnClickListener subscribeAppStatusButtonClickListener;
    String applicationValue = "NOT_FOUND";
    String statusValue = "NOT_FOUND";

  public AppLauncherExampleFragment(
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
  public static AppLauncherExampleFragment newInstance(
      CastingPlayer selectedCastingPlayer, Boolean useCommissionerGeneratedPasscode) {
    return new AppLauncherExampleFragment(
        selectedCastingPlayer, useCommissionerGeneratedPasscode);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    this.launchAppButtonClickListener =
        v -> {
          Endpoint endpoint;
          if (useCommissionerGeneratedPasscode) {
            // For the example Commissioner-Generated passcode commissioning flow, run demo
            // interactions with the Endpoint with ID DEFAULT_ENDPOINT_ID_FOR_CGP_FLOW = 1. For this
            // flow, we commissioned with the Target Content Application with Vendor ID 1111. Since
            // this target content application does not report its Endpoint's Vendor IDs, we find
            // the desired endpoint based on the Endpoint ID. See
            // connectedhomeip/examples/tv-app/tv-common/include/AppTv.h.
            endpoint =
                EndpointSelectorExample.selectEndpointById(
                    selectedCastingPlayer, DEFAULT_ENDPOINT_ID_FOR_CGP_FLOW);
          } else {
            endpoint = EndpointSelectorExample.selectFirstEndpointByVID(selectedCastingPlayer);
          }
          if (endpoint == null) {
            Log.e(TAG, "No Endpoint with sample vendorID found on CastingPlayer");
            return;
          }

          EditText contentAppVendorIdEditText = getView().findViewById(R.id.contentAppVendorIdEditText);
          int vendorId = Integer.parseInt(contentAppVendorIdEditText.getText().toString());
          EditText contentAppApplicationIdEditText =
              getView().findViewById(R.id.contentAppApplicationIdEditText);
          String contentAppApplicationId = contentAppApplicationIdEditText.getText().toString();

          // get ChipClusters.ApplicationLauncherCluster from the endpoint
          ChipClusters.ApplicationLauncherCluster cluster =
              endpoint.getCluster(ChipClusters.ApplicationLauncherCluster.class);
          if (cluster == null) {
            Log.e(
                TAG,
                "Could not get ApplicationLauncherCluster for endpoint with ID: " + endpoint.getId());
            return;
          }

          // call launchApp on the cluster object while passing in a
          // ChipClusters.ApplicationLauncherCluster.LauncherResponseCallback and request parameters
            ChipStructs.ApplicationLauncherClusterApplicationStruct
                applicationStruct =
                    new ChipStructs.ApplicationLauncherClusterApplicationStruct(
                        vendorId, contentAppApplicationId);
          cluster.launchApp(
              new ChipClusters.ApplicationLauncherCluster.LauncherResponseCallback() {
                  @Override
                  public void onSuccess(Integer status, Optional<byte[]> data) {
                      Log.d(TAG, "LaunchApp success. Status: " + status + ", Data: " + data);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView launcherResult = getView().findViewById(R.id.launcherResult);
                                          launcherResult.setText(
                                                  "LaunchApp result\nStatus: " + status + ", Data: " + data);
                                      });
                  }

                @Override
                public void onError(Exception error) {
                  Log.e(TAG, "LaunchApp failure " + error);
                  new Handler(Looper.getMainLooper())
                      .post(
                          () -> {
                            TextView launcherResult = getView().findViewById(R.id.launcherResult);
                            launcherResult.setText("LaunchApp result\nError: " + error);
                          });
                }
              },
                  Optional.of(applicationStruct),
              Optional.empty());
        };
//    this.getAppStatusButtonClickListener =
//            v -> {
//                Endpoint endpoint;
//                endpoint = EndpointSelectorExample.selectEndpointById(
//                                selectedCastingPlayer, DEFAULT_ENDPOINT_ID_FOR_CGP_FLOW);
//                if (endpoint == null) {
//                    Log.e(TAG, "No Endpoint with sample vendorID found on CastingPlayer");
//                    return;
//                }
//
//                // get ChipClusters.ApplicationLauncherCluster from the endpoint
//                ChipClusters.ApplicationLauncherCluster cluster =
//                        endpoint.getCluster(ChipClusters.ApplicationLauncherCluster.class);
//                if (cluster == null) {
//                    Log.e(
//                            TAG,
//                            "Could not get ApplicationLauncherCluster for endpoint with ID: " + endpoint.getId());
//                    return;
//                }
//
//                cluster.readCurrentAppAttribute(new ChipClusters.ApplicationLauncherCluster.CurrentAppAttributeCallback() {
//
//                    @Override
//                    public void onSuccess(ChipStructs.ApplicationLauncherClusterApplicationEPStruct value) {
//                        Log.d(TAG, "ReadCurrentApp success. Value: " + value);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText(
//                                                    "ReadCurrentApp result\nValue: " + value);
//                                        });
//                    }
//
//                    @Override
//                    public void onError(Exception error) {
//                        Log.e(TAG, "ReadCurrentApp failure " + error);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText("ReadCurrentApp result\nError: " + error);
//                                        });
//                    }
//
//                });
//
//                // Read AcceptedCommandList
//                cluster.readAcceptedCommandListAttribute(new ChipClusters.ApplicationLauncherCluster.AcceptedCommandListAttributeCallback() {
//                    @Override
//                    public void onSuccess(List<Long> value) {
//                        Log.d(TAG, "ReadAcceptedCommandList success. Value: " + value);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText(
//                                                    "ReadAcceptedCommandList result\nValue: " + value);
//                                        });
//                    }
//
//                    @Override
//                    public void onError(Exception error) {
//                        Log.e(TAG, "ReadAcceptedCommandList failure " + error);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText("ReadAcceptedCommandList result\nError: " + error);
//                                        });
//                    }
//                });
//
//// Read CatalogList
//                cluster.readCatalogListAttribute(new ChipClusters.ApplicationLauncherCluster.CatalogListAttributeCallback() {
//                    @Override
//                    public void onSuccess(List<Integer> value) {
//                        Log.d(TAG, "ReadCatalogList success. Value: " + value);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText(
//                                                    "ReadCatalogList result\nValue: " + value);
//                                        });
//                    }
//
//                    @Override
//                    public void onError(Exception error) {
//                        Log.e(TAG, "ReadCatalogList failure " + error);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText("ReadCatalogList result\nError: " + error);
//                                        });
//                    }
//                });
//
//// Read GeneratedCommandList
//                cluster.readGeneratedCommandListAttribute(new ChipClusters.ApplicationLauncherCluster.GeneratedCommandListAttributeCallback() {
//                    @Override
//                    public void onSuccess(List<Long> value) {
//                        Log.d(TAG, "ReadGeneratedCommandList success. Value: " + value);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText(
//                                                    "ReadGeneratedCommandList result\nValue: " + value);
//                                        });
//                    }
//
//                    @Override
//                    public void onError(Exception error) {
//                        Log.e(TAG, "ReadGeneratedCommandList failure " + error);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText("ReadGeneratedCommandList result\nError: " + error);
//                                        });
//                    }
//                });
//
//// Read AttributeList
//                cluster.readAttributeListAttribute(new ChipClusters.ApplicationLauncherCluster.AttributeListAttributeCallback() {
//                    @Override
//                    public void onSuccess(List<Long> value) {
//                        Log.d(TAG, "ReadAttributeList success. Value: " + value);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText(
//                                                    "ReadAttributeList result\nValue: " + value);
//                                        });
//                    }
//
//                    @Override
//                    public void onError(Exception error) {
//                        Log.e(TAG, "ReadAttributeList failure " + error);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText("ReadAttributeList result\nError: " + error);
//                                        });
//                    }
//                });
//
//// Read EventList
//                cluster.readEventListAttribute(new ChipClusters.ApplicationLauncherCluster.EventListAttributeCallback() {
//                    @Override
//                    public void onSuccess(List<Long> value) {
//                        Log.d(TAG, "ReadEventList success. Value: " + value);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText(
//                                                    "ReadEventList result\nValue: " + value);
//                                        });
//                    }
//
//                    @Override
//                    public void onError(Exception error) {
//                        Log.e(TAG, "ReadEventList failure " + error);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText("ReadEventList result\nError: " + error);
//                                        });
//                    }
//                });
//
//// Read FeatureMap
//                cluster.readFeatureMapAttribute(new ChipClusters.LongAttributeCallback() {
//                    @Override
//                    public void onSuccess(long value) {
//                        Log.d(TAG, "ReadFeatureMap success. Value: " + value);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText(
//                                                    "ReadFeatureMap result\nValue: " + value);
//                                        });
//                    }
//
//                    @Override
//                    public void onError(Exception error) {
//                        Log.e(TAG, "ReadFeatureMap failure " + error);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText("ReadFeatureMap result\nError: " + error);
//                                        });
//                    }
//                });
//
//// Read ClusterRevision
//                cluster.readClusterRevisionAttribute(new ChipClusters.IntegerAttributeCallback() {
//                    @Override
//                    public void onSuccess(int value) {
//                        Log.d(TAG, "ReadClusterRevision success. Value: " + value);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText(
//                                                    "ReadClusterRevision result\nValue: " + value);
//                                        });
//                    }
//
//                    @Override
//                    public void onError(Exception error) {
//                        Log.e(TAG, "ReadClusterRevision failure " + error);
//                        new Handler(Looper.getMainLooper())
//                                .post(
//                                        () -> {
//                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
//                                            getAppStatusResult.setText("ReadClusterRevision result\nError: " + error);
//                                        });
//                    }
//                });
//            };

      getAppStatusButtonClickListener = v -> {
          Endpoint contentAppEndpoint = EndpointSelectorExample.selectFirstEndpointByVID(selectedCastingPlayer);
            if (contentAppEndpoint == null) {
                Log.e(TAG, "No Endpoint with sample vendorID found on CastingPlayer");
                return;
            }

            ChipClusters.ApplicationBasicCluster basicCluster =
                    contentAppEndpoint.getCluster(ChipClusters.ApplicationBasicCluster.class);
            if (basicCluster == null) {
                Log.e(
                        TAG,
                        "Could not get ApplicationBasicCluster for endpoint with ID: " + contentAppEndpoint.getId());
                return;
            }
            basicCluster.readApplicationAttribute(new ChipClusters.ApplicationBasicCluster.ApplicationAttributeCallback() {

                @Override
                public void onSuccess(ChipStructs.ApplicationBasicClusterApplicationStruct value) {
                    Log.d(TAG, "ReadApplication success. Value: " + value);
                    new Handler(Looper.getMainLooper())
                            .post(
                                    () -> {
                                        applicationValue = value.toString();
                                        TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
                                        getAppStatusResult.setText("ReadApplication result\napplication: " + applicationValue + " status: " + statusValue);
                                    });
                }

                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "ReadApplication failure " + error);
                    new Handler(Looper.getMainLooper())
                            .post(() -> {
                                TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
                                getAppStatusResult.setText("ReadApplication result\nError: " + error + " application: " + applicationValue + " status: " + statusValue);
                            });
                }

            });
            basicCluster.readStatusAttribute(new ChipClusters.IntegerAttributeCallback() {
                @Override
                public void onSuccess(int value) {
                    Log.d(TAG, "ReadStatus success. Value: " + value);
                    new Handler(Looper.getMainLooper())
                                .post(
                                        () -> {
                                            statusValue = String.valueOf(value);
                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
                                            getAppStatusResult.setText("ReadStatus result\napplication: " + applicationValue + " status: " + statusValue);
                                        });
                }

                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "ReadStatus failure " + error);
                                            new Handler(Looper.getMainLooper())
                                .post(() -> {
                                            TextView getAppStatusResult = getView().findViewById(R.id.getAppStatusResult);
                                            getAppStatusResult.setText("ReadStatus result\nError: " + error + " application: " + applicationValue + " status: " + statusValue);
                                        });
                }
            });
      };

      subscribeAppStatusButtonClickListener = new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              Endpoint endpoint;
              if (useCommissionerGeneratedPasscode) {
                  endpoint = EndpointSelectorExample.selectEndpointById(
                          selectedCastingPlayer, DEFAULT_ENDPOINT_ID_FOR_CGP_FLOW);
              } else {
                  endpoint = EndpointSelectorExample.selectFirstEndpointByVID(selectedCastingPlayer);
              }
              if (endpoint == null) {
                  Log.e(TAG, "No Endpoint with sample vendorID found on CastingPlayer");
                  return;
              }

              // get ChipClusters.ApplicationLauncherCluster from the endpoint
              ChipClusters.ApplicationLauncherCluster cluster =
                      endpoint.getCluster(ChipClusters.ApplicationLauncherCluster.class);
              if (cluster == null) {
                  Log.e(TAG, "Could not get ApplicationLauncherCluster for endpoint with ID: " + endpoint.getId());
                  return;
              }

              // Subscribe to CurrentApp
              cluster.subscribeCurrentAppAttribute(new ChipClusters.ApplicationLauncherCluster.CurrentAppAttributeCallback() {
                  @Override
                  public void onSuccess(ChipStructs.ApplicationLauncherClusterApplicationEPStruct value) {
                      Log.d(TAG, "SubscribeCurrentApp success. Value: " + value);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText(
                                                  "SubscribeCurrentApp result\nValue: " + value);
                                      });
                  }

                  @Override
                  public void onError(Exception error) {
                      Log.e(TAG, "SubscribeCurrentApp failure " + error);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText("SubscribeCurrentApp result\nError: " + error);
                                      });
                  }
              }, 0, 1); // min interval 0, max interval 1 second

              // Subscribe to CatalogList
              cluster.subscribeCatalogListAttribute(new ChipClusters.ApplicationLauncherCluster.CatalogListAttributeCallback() {
                  @Override
                  public void onSuccess(List<Integer> value) {
                      Log.d(TAG, "SubscribeCatalogList success. Value: " + value);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText(
                                                  "SubscribeCatalogList result\nValue: " + value);
                                      });
                  }

                  @Override
                  public void onError(Exception error) {
                      Log.e(TAG, "SubscribeCatalogList failure " + error);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText("SubscribeCatalogList result\nError: " + error);
                                      });
                  }
              }, 0, 1);

              // Subscribe to AcceptedCommandList
              cluster.subscribeAcceptedCommandListAttribute(new ChipClusters.ApplicationLauncherCluster.AcceptedCommandListAttributeCallback() {
                  @Override
                  public void onSuccess(List<Long> value) {
                      Log.d(TAG, "SubscribeAcceptedCommandList success. Value: " + value);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText(
                                                  "SubscribeAcceptedCommandList result\nValue: " + value);
                                      });
                  }

                  @Override
                  public void onError(Exception error) {
                      Log.e(TAG, "SubscribeAcceptedCommandList failure " + error);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText("SubscribeAcceptedCommandList result\nError: " + error);
                                      });
                  }
              }, 0, 1);

              // Subscribe to GeneratedCommandList
              cluster.subscribeGeneratedCommandListAttribute(new ChipClusters.ApplicationLauncherCluster.GeneratedCommandListAttributeCallback() {
                  @Override
                  public void onSuccess(List<Long> value) {
                      Log.d(TAG, "SubscribeGeneratedCommandList success. Value: " + value);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText(
                                                  "SubscribeGeneratedCommandList result\nValue: " + value);
                                      });
                  }

                  @Override
                  public void onError(Exception error) {
                      Log.e(TAG, "SubscribeGeneratedCommandList failure " + error);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText("SubscribeGeneratedCommandList result\nError: " + error);
                                      });
                  }
              }, 0, 1);

              // Subscribe to AttributeList
              cluster.subscribeAttributeListAttribute(new ChipClusters.ApplicationLauncherCluster.AttributeListAttributeCallback() {
                  @Override
                  public void onSuccess(List<Long> value) {
                      Log.d(TAG, "SubscribeAttributeList success. Value: " + value);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText(
                                                  "SubscribeAttributeList result\nValue: " + value);
                                      });
                  }

                  @Override
                  public void onError(Exception error) {
                      Log.e(TAG, "SubscribeAttributeList failure " + error);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText("SubscribeAttributeList result\nError: " + error);
                                      });
                  }
              }, 0, 1);

              // Subscribe to EventList
//              cluster.subscribeEventListAttribute(new ChipClusters.ApplicationLauncherCluster.EventListAttributeCallback() {
//                  @Override
//                  public void onSuccess(List<Long> value) {
//                      Log.d(TAG, "SubscribeEventList success. Value: " + value);
//                      new Handler(Looper.getMainLooper())
//                              .post(
//                                      () -> {
//                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
//                                          subscribeAppStatusResult.setText(
//                                                  "SubscribeEventList result\nValue: " + value);
//                                      });
//                  }
//
//                  @Override
//                  public void onError(Exception error) {
//                      Log.e(TAG, "SubscribeEventList failure " + error);
//                      new Handler(Looper.getMainLooper())
//                              .post(
//                                      () -> {
//                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
//                                          subscribeAppStatusResult.setText("SubscribeEventList result\nError: " + error);
//                                      });
//                  }
//              }, 0, 1);

              // Subscribe to FeatureMap
              cluster.subscribeFeatureMapAttribute(new ChipClusters.LongAttributeCallback() {
                  @Override
                  public void onSuccess(long value) {
                      Log.d(TAG, "SubscribeFeatureMap success. Value: " + value);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText(
                                                  "SubscribeFeatureMap result\nValue: " + value);
                                      });
                  }

                  @Override
                  public void onError(Exception error) {
                      Log.e(TAG, "SubscribeFeatureMap failure " + error);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText("SubscribeFeatureMap result\nError: " + error);
                                      });
                  }
              }, 0, 1);

              // Subscribe to ClusterRevision
              cluster.subscribeClusterRevisionAttribute(new ChipClusters.IntegerAttributeCallback() {
                  @Override
                  public void onSuccess(int value) {
                      Log.d(TAG, "SubscribeClusterRevision success. Value: " + value);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText(
                                                  "SubscribeClusterRevision result\nValue: " + value);
                                      });
                  }

                  @Override
                  public void onError(Exception error) {
                      Log.e(TAG, "SubscribeClusterRevision failure " + error);
                      new Handler(Looper.getMainLooper())
                              .post(
                                      () -> {
                                          TextView subscribeAppStatusResult = getView().findViewById(R.id.subscribeAppStatusResult);
                                          subscribeAppStatusResult.setText("SubscribeClusterRevision result\nError: " + error);
                                      });
                  }
              }, 0, 1);
          }
      };
    return inflater.inflate(R.layout.fragment_matter_app_launcher, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "ContentLauncherLaunchURLExampleFragment.onViewCreated called");
    getView().findViewById(R.id.launchAppButton).setOnClickListener(launchAppButtonClickListener);
    getView().findViewById(R.id.getAppStatusButton).setOnClickListener(getAppStatusButtonClickListener);
    getView().findViewById(R.id.subscribeAppStatusButton).setOnClickListener(subscribeAppStatusButtonClickListener);
  }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        CastingApp.getInstance().shutdownAllSubscriptions();
    }
}

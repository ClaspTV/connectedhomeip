package com.matter.casting;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import com.chip.casting.R;
import com.matter.casting.core.CastingPlayer;
import com.matter.casting.core.Endpoint;
import java.util.List;

import chip.devicecontroller.ChipClusters;
import chip.devicecontroller.ChipStructs;

/** A utility that selects an endpoint based on some criterion */
public class EndpointSelectorExample {
  private static final String TAG = EndpointSelectorExample.class.getSimpleName();
  private static final Integer SAMPLE_ENDPOINT_VID = 65521;

  /**
   * Returns the first Endpoint in the list of Endpoints associated with the selectedCastingPlayer
   * whose VendorID matches the EndpointSelectorExample.SAMPLE_ENDPOINT_VID
   */
  public static Endpoint selectFirstEndpointByVID(CastingPlayer selectedCastingPlayer) {
    Endpoint endpoint = null;
    if (selectedCastingPlayer != null) {
      List<Endpoint> endpoints = selectedCastingPlayer.getEndpoints();
      if (endpoints == null) {
        Log.e(TAG, "selectFirstEndpointByVID() No Endpoints found on CastingPlayer");
      } else {
        endpoint =
            endpoints
                .stream()
                .filter(e -> SAMPLE_ENDPOINT_VID.equals(e.getVendorId()))
                .findFirst()
                .orElse(null);
      }
    }
    return endpoint;
  }

  /**
   * Returns the Endpoint with the desired endpoint Id in the list of Endpoints associated with the
   * selectedCastingPlayer.
   */
  public static Endpoint selectEndpointById(
      CastingPlayer selectedCastingPlayer, int desiredEndpointId) {
    Endpoint endpoint = null;
    if (selectedCastingPlayer != null) {
      List<Endpoint> endpoints = selectedCastingPlayer.getEndpoints();
      if (endpoints == null) {
        Log.e(TAG, "selectEndpointById() No Endpoints found on CastingPlayer");
      } else {
        endpoint =
            endpoints.stream().filter(e -> desiredEndpointId == e.getId()).findFirst().orElse(null);
      }
    }
    return endpoint;
  }


  public static void printEndPoints(CastingPlayer targetCastingPlayer) {

    Log.i(
            TAG,
            "Enumerating endpoints: " +
                    (targetCastingPlayer != null ? targetCastingPlayer.getEndpoints() : "null")
    );

    List<Endpoint> endPoints = targetCastingPlayer != null ? targetCastingPlayer.getEndpoints() : null;
    if (endPoints != null) {
      for (Endpoint endPoint : endPoints) {

        Log.i(
                TAG,
                "Getting details for Endpoint: " + endPoint.getId() + ", " +
                        "VendorId: " + endPoint.getVendorId() + ", " +
                        "ProductId: " + endPoint.getProductId()
        );
        // get ApplicationBasicCluster on each endpoint and print application.applicationID
        ChipClusters.ApplicationBasicCluster basicCluster =
                endPoint.getCluster(ChipClusters.ApplicationBasicCluster.class);
        if (basicCluster != null) {
          // This is not working. It results in a failure callback for all the endpoints. Also, results in commissioning failure.
//          basicCluster.readApplicationAttribute(new ChipClusters.ApplicationBasicCluster.ApplicationAttributeCallback() {
//
//            @Override
//            public void onSuccess(ChipStructs.ApplicationBasicClusterApplicationStruct value) {
//              Log.d(TAG, "ReadApplication success. Value: " + value);
//              new Handler(Looper.getMainLooper())
//                      .post(
//                              () -> {
//                                Log.d(TAG, "applicationValue of endpoint: Endpoint: " + endPoint.getId() + ", " +
//                                        " VendorId: " + endPoint.getVendorId() + ", " +
//                                        " ProductId: " + endPoint.getProductId() + " is " + value.toString());
//                              });
//            }
//
//            @Override
//            public void onError(Exception error) {
//              Log.e(TAG, "ReadApplication failure " + error);
//              new Handler(Looper.getMainLooper())
//                      .post(() -> {
//                        Log.e(TAG, "Failed to read applicationValue of endpoint: Endpoint: " + endPoint.getId() + ", " +
//                                " VendorId: " + endPoint.getVendorId() + ", " +
//                                " ProductId: " + endPoint.getProductId());
//                      });
//            }
//          });
//
//          basicCluster.readStatusAttribute(new ChipClusters.IntegerAttributeCallback() {
//            @Override
//            public void onSuccess(int value) {
//              Log.d(TAG, "ReadStatus success. Value: " + value);
//              new Handler(Looper.getMainLooper())
//                      .post(
//                              () -> {
//                                Log.d(TAG, "statusValue of endpoint: Endpoint: " + endPoint.getId() + ", " +
//                                        " VendorId: " + endPoint.getVendorId() + ", " +
//                                        " ProductId: " + endPoint.getProductId() + " is " + value);
//                              });
//            }
//
//            @Override
//            public void onError(Exception error) {
//              Log.e(TAG, "ReadStatus failure " + error);
//              new Handler(Looper.getMainLooper())
//                      .post(() -> {
//                        Log.e(TAG, "Failed to read statusValue of endpoint: Endpoint: " + endPoint.getId() + ", " +
//                                " VendorId: " + endPoint.getVendorId() + ", " +
//                                " ProductId: " + endPoint.getProductId());
//                      });
//            }
//          });
//
//          // read application name: This is working. We are able to see that the application name is com.example.contentapp for endpoint 4. For the rest of the endpoints, we it results in a failure.
            basicCluster.readApplicationNameAttribute(new ChipClusters.CharStringAttributeCallback() {
                @Override
                public void onSuccess(String value) {
                Log.d(TAG, "ReadApplicationName success. Value: " + value);
                new Handler(Looper.getMainLooper())
                        .post(
                                () -> {
                                    Log.d(TAG, "applicationName of endpoint: Endpoint: " + endPoint.getId() + ", " +
                                            " VendorId: " + endPoint.getVendorId() + ", " +
                                            " ProductId: " + endPoint.getProductId() + " is " + value);
                                });
                }

                @Override
                public void onError(Exception error) {
                Log.e(TAG, "ReadApplicationName failure " + error);
                new Handler(Looper.getMainLooper())
                        .post(() -> {
                            Log.e(TAG, "Failed to read applicationName of endpoint: Endpoint: " + endPoint.getId() + ", " +
                                    " VendorId: " + endPoint.getVendorId() + ", " +
                                    " ProductId: " + endPoint.getProductId());
                        });
                }
            });
        } else {
            Log.e(TAG, "Could not get ApplicationBasicCluster for endpoint with ID: " + endPoint.getId());
        }
      }
    } else {
      Log.i(TAG, "CastingPlayer endpoints: null");
    }
  }
}

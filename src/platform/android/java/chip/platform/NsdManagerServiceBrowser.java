/*
 *   Copyright (c) 2021 Project CHIP Authors
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
 *
 */
package chip.platform;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class NsdManagerServiceBrowser implements ServiceBrowser {
  private static final String TAG = NsdManagerServiceBrowser.class.getSimpleName();
  private static final long BROWSE_SERVICE_TIMEOUT = 5000;
  private final NsdManager nsdManager;
  private MulticastLock multicastLock;
  private Handler mainThreadHandler;
  private final long timeout;

  private HashMap<Long, NsdManagerDiscovery> callbackMap;

  public NsdManagerServiceBrowser(Context context) {
    this(context, BROWSE_SERVICE_TIMEOUT);
  }

  /**
   * @param context application context
   * @param timeout Timeout value in case there is no response after calling browse
   */
  public NsdManagerServiceBrowser(Context context, long timeout) {
    this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    this.mainThreadHandler = new Handler(Looper.getMainLooper());

    this.multicastLock =
        ((WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE))
            .createMulticastLock("chipBrowseMulticastLock");
    this.multicastLock.setReferenceCounted(true);
    callbackMap = new HashMap<>();
    this.timeout = timeout;
  }

  @Override
  public void browse(
      final String serviceType,
      final long callbackHandle,
      final long contextHandle,
      final ChipMdnsCallback chipMdnsCallback) {
    startDiscover(serviceType, callbackHandle, contextHandle, chipMdnsCallback, false);
  }

  @Override
  public void browseContinuous(
      final String serviceType,
      final long callbackHandle,
      final long contextHandle,
      final ChipMdnsCallback chipMdnsCallback) {
    startDiscover(serviceType, callbackHandle, contextHandle, chipMdnsCallback, true);
  }

  public void startDiscover(
      final String serviceType,
      final long callbackHandle,
      final long contextHandle,
      final ChipMdnsCallback chipMdnsCallback,
      final boolean continuous) {
    if (callbackMap.containsKey(callbackHandle)) {
      Log.w(TAG, "Starting service discovery failed. Invalid callbackHandle: " + callbackHandle);
      return;
    }

    NsdManagerDiscovery discovery =
        new NsdManagerDiscovery(serviceType, callbackHandle, contextHandle, continuous);
    multicastLock.acquire();
    discovery.setChipMdnsCallback(chipMdnsCallback);

    if (!continuous) {
      Runnable timeoutRunnable =
          new Runnable() {
            @Override
            public void run() {
              Log.i(
                  TAG,
                  "Browse for service '"
                      + serviceType
                      + "' expired after timeout: "
                      + timeout
                      + " ms");
              stopDiscover(callbackHandle);
            }
          };
      discovery.setTimeoutRunnable(timeoutRunnable);
      mainThreadHandler.postDelayed(timeoutRunnable, timeout);
    }

    Log.d(
        TAG,
        "Starting service discovery for '"
            + serviceType
            + "' with callbackHandle: "
            + callbackHandle
            + ", continuous: "
            + continuous);

    this.nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discovery);
    callbackMap.put(callbackHandle, discovery);
  }

  public void stopDiscover(final long callbackHandle) {
    Log.d(TAG, "Stopping service discovery with callbackHandle: " + callbackHandle);
    if (!callbackMap.containsKey(callbackHandle)) {
      Log.w(TAG, "Stopping service discovery failed. Callback handle not found.");
      return;
    }

    NsdManagerDiscovery discovery = callbackMap.remove(callbackHandle);
    if (multicastLock.isHeld()) {
      multicastLock.release();
    }

    if (discovery.timeoutRunnable != null) {
      Log.d(TAG, "Canceling scheduled browse timeout runnable for '" + discovery.serviceType + "'");
      mainThreadHandler.removeCallbacks(discovery.timeoutRunnable);
    }

    try {
      this.nsdManager.stopServiceDiscovery(discovery);
    } catch (Exception e) {
      Log.e(TAG, "Exception in stopDiscover " + e, e);
    }
  }

  public class NsdManagerDiscovery implements NsdManager.DiscoveryListener {
    private String serviceType;
    private long callbackHandle;
    private long contextHandle;
    private final boolean continuous;
    private final Set<String> serviceNames = new LinkedHashSet<>();
    private ChipMdnsCallback chipMdnsCallback;
    private Runnable timeoutRunnable;

    public NsdManagerDiscovery(
        String serviceType, long callbackHandle, long contextHandle, boolean continuous) {
      this.serviceType = serviceType;
      this.callbackHandle = callbackHandle;
      this.contextHandle = contextHandle;
      this.continuous = continuous;
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
      Log.w(TAG, "Failed to start discovery service '" + serviceType + "': " + errorCode);
      handleBrowseStop(errorCode);
    }

    @Override
    public void onDiscoveryStarted(String serviceType) {
      Log.i(TAG, "Started service '" + serviceType + "'");
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
      Log.i(TAG, "Found service '" + serviceInfo.getServiceName() + "'");
      boolean isNewService = serviceNames.add(serviceInfo.getServiceName());
      if (continuous && isNewService) {
        chipMdnsCallback.handleServiceBrowseAdd(
            serviceInfo.getServiceName(), serviceType, callbackHandle, contextHandle);
      }
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
      Log.i(
          TAG,
          "Lost service '" + (serviceInfo != null ? serviceInfo.getServiceName() : "null") + "'");
      if (serviceInfo == null) {
        return;
      }

      boolean removed = serviceNames.remove(serviceInfo.getServiceName());
      Log.i(TAG, "Remove List: " + removed);
      if (continuous && removed) {
        chipMdnsCallback.handleServiceBrowseRemove(
            serviceInfo.getServiceName(), serviceType, callbackHandle, contextHandle);
      }
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
      Log.w(TAG, "Failed to stop discovery service '" + serviceType + "': " + errorCode);
      handleBrowseStop(errorCode);
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
      Log.w(TAG, "Successfully stopped discovery service '" + serviceType);
      new Handler(Looper.getMainLooper())
          .post(
              () -> {
                if (continuous) {
                  this.handleBrowseStop(0);
                } else {
                  this.handleServiceBrowse(chipMdnsCallback);
                }
              });
    }

    public void handleServiceBrowse(ChipMdnsCallback chipMdnsCallback) {
      chipMdnsCallback.handleServiceBrowse(
          serviceNames.toArray(new String[serviceNames.size()]),
          serviceType,
          callbackHandle,
          contextHandle);
    }

    public void handleBrowseStop(int errorCode) {
      if (chipMdnsCallback != null) {
        chipMdnsCallback.handleServiceBrowseStop(callbackHandle, contextHandle, errorCode);
      }
    }

    public void setChipMdnsCallback(final ChipMdnsCallback chipMdnsCallback) {
      this.chipMdnsCallback = chipMdnsCallback;
    }

    public void setTimeoutRunnable(final Runnable timeoutRunnable) {
      this.timeoutRunnable = timeoutRunnable;
    }
  }
}

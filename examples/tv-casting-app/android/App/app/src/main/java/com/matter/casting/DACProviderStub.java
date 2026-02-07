/*
 *   Copyright (c) 2023 Project CHIP Authors
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

import android.util.Base64;
import android.util.Log;
import com.matter.casting.support.DACProvider;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

public class DACProviderStub implements DACProvider {

  private static final String TAG = DACProviderStub.class.getSimpleName();

  // DAC
  private String kDevelopmentDAC_Cert_FFF1_8001 =
      "MIIB6jCCAZGgAwIBAgIQaDU122CP07+DspBkvmPDqDAKBggqhkjOPQQDAjBRMRIwEAYDVQQDDAlWaXpiZWVQQUkxFDASBgorBgEEAYKifAIBDAQxMzgxMQ8wDQYDVQQKDAZWaXpiZWUxFDASBgorBgEEAYKifAICDAQxMDAzMB4XDTI2MDIwNTIyMTc0NFoXDTMxMDIwMzIzMTc0NFowTDEUMBIGCisGAQQBgqJ8AgEMBDEzODExFDASBgorBgEEAYKifAICDAQxMDAzMR4wHAYDVQQDDBVWaXpiZWUgQ2FzdGluZyBEQUMwMDEwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQrppA9Oe3eAc49yXUvem8UFWMG0Yhixjzzor0rEBmTJ6JuhKHBLu1Dli4CbVvqsUp47KM9sGVadFhnS1nIiakSo1AwTjAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFO1d1S8/tmRcQD3i+YvpFNsGqxqPMB0GA1UdDgQWBBRnmXkWdXxAL2il4cA7udJSuPXU2TAKBggqhkjOPQQDAgNHADBEAiAxERPpEY0hXQNCpA+2ja8e9UjL7kd30ji/vY/ohMMdAAIgK+0Hh8rOPbK02dPzH+vxJwEhRRwmXpivozwB/JWBEAw=";

  // Private Key
  private String kDevelopmentDAC_PrivateKey_FFF1_8001 =
      "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgzDNNJYyFzx3Fim6uIY1BQOAhdgeApXCjPEXrtw9NDZShRANCAAQrppA9Oe3eAc49yXUvem8UFWMG0Yhixjzzor0rEBmTJ6JuhKHBLu1Dli4CbVvqsUp47KM9sGVadFhnS1nIiakS";

  private String kDevelopmentDAC_PublicKey_FFF1_8001 =
      "BEY6xpNCkQoOVYj8b/Vrtj5i7M7LFI99TrA+5VJgFBV2fRalxmP3k+SRIyYLgpenzX58/HsxaznZjpDSk3dzjoI=";

  // PAI
  private String KPAI_FFF1_8000_Cert_Array =
      "MIIB3zCCAYWgAwIBAgIQc1bDqUKbQ7C7+ZDScMT3fjAKBggqhkjOPQQDAjAqMRIwEAYDVQQDDAlQcmltZSBQQUExFDASBgorBgEEAYKifAIBDAQxMzgxMB4XDTI2MDIwNTE4NTEzNFoXDTMyMDEwMTAwMDAwMFowUTESMBAGA1UEAwwJVml6YmVlUEFJMRQwEgYKKwYBBAGConwCAQwEMTM4MTEPMA0GA1UECgwGVml6YmVlMRQwEgYKKwYBBAGConwCAgwEMTAwMzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABAZCjOTpXbYnltzGxfuJq/0fjuqjnvXd85jVgFJ2DO3SbkI2VmhksvO9tRBHNkVKTTaERU9vHdZsP95f82bXdv+jZjBkMBIGA1UdEwEB/wQIMAYBAf8CAQAwHwYDVR0jBBgwFoAU57mRu5ye0GGny5vxiGRKFWPCTiswHQYDVR0OBBYEFO1d1S8/tmRcQD3i+YvpFNsGqxqPMA4GA1UdDwEB/wQEAwIBBjAKBggqhkjOPQQDAgNIADBFAiBLj4xF6VgaQtPqPGGtAg/PrBJoaJ/lypHtgxVK9UooBQIhAJNKrVmVr61sMjCwG7hTi6Iu4LvZyy5UJj/JscYapJPf";

  /**
   * format_version = 1 vendor_id = 0xFFF1 product_id_array = [ 0x8000,0x8001...0x8063]
   * device_type_id = 0x1234 certificate_id = "ZIG20141ZB330001-24" security_level = 0
   * security_information = 0 version_number = 0x2694 certification_type = 0 dac_origin_vendor_id is
   * not present dac_origin_product_id is not present
   */
  private String kCertificationDeclaration =
      "MIHtBgkqhkiG9w0BBwKggd8wgdwCAQMxDTALBglghkgBZQMEAgEwSgYJKoZIhvcNAQcBoD0EOxUkAAElARkWNgIEARgkAyksBBNDU0EyNjAwM1NXQzYwMjQxLU0xJAUAJAYAJAcBJAgCJQmBEyUKAxAYMXwwegIBA4AU/jQ/lZlHdjth7kU5ExM4SU/mfY4wCwYJYIZIAWUDBAIBMAoGCCqGSM49BAMCBEYwRAIgFGM0e+1xZtw7HIFqAOQxe2JOJ0LvlK1ccwBWxrBwNQQCIBKLXDPC6EQnnxa7hTNiSBaaED19gAjrPIbYTRiroR9s";

  @Override
  public byte[] GetCertificationDeclaration() {
    return Base64.decode(kCertificationDeclaration, Base64.DEFAULT);
  }

  @Override
  public byte[] GetFirmwareInformation() {
    return new byte[0];
  }

  @Override
  public byte[] GetDeviceAttestationCert() {
    return Base64.decode(kDevelopmentDAC_Cert_FFF1_8001, Base64.DEFAULT);
  }

  @Override
  public byte[] GetProductAttestationIntermediateCert() {
    return Base64.decode(KPAI_FFF1_8000_Cert_Array, Base64.DEFAULT);
  }

  @Override
  public byte[] SignWithDeviceAttestationKey(byte[] message) {

    try {
      byte[] privateKeyBytes = Base64.decode(kDevelopmentDAC_PrivateKey_FFF1_8001, Base64.DEFAULT);

      PrivateKey privateKey = null;
      KeyFactory keyFactory = KeyFactory.getInstance("EC");
      // the format can be determined by the header in the private key file:
      // -----BEGIN PRIVATE KEY----- - PKCS#8 format
      // -----BEGIN EC PRIVATE KEY----- - SEC1/traditional EC format
      try {
        // Try PKCS#8 format first.
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        privateKey = keyFactory.generatePrivate(keySpec);
      } catch (java.security.spec.InvalidKeySpecException e) {
          // Create a fresh KeyFactory instance for SEC1 format attempt.
          // This avoids any potential state issues from the failed PKCS#8 attempt.
          keyFactory = KeyFactory.getInstance("EC");
        // Fallback to SEC1 format.
        AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("EC");
        algorithmParameters.init(new ECGenParameterSpec("BggqhkjOPQMBBw=="));
        ECParameterSpec parameterSpec = algorithmParameters.getParameterSpec(ECParameterSpec.class);
        ECPrivateKeySpec ecPrivateKeySpec =
            new ECPrivateKeySpec(new BigInteger(1, privateKeyBytes), parameterSpec);
        privateKey = keyFactory.generatePrivate(ecPrivateKeySpec);
      }

      Signature signature = Signature.getInstance("SHA256withECDSA");
      signature.initSign(privateKey);

      signature.update(message);

      return signature.sign();

    } catch (Exception e) {
      Log.e(TAG, "SignWithDeviceAttestationKey failed", e);
      return null;
    }
  }
}

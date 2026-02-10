/**
 *
 *    Copyright (c) 2023 Project CHIP Authors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import Foundation
import Security
import os.log

// This class needs to be imlemented by the client.
class MCAppParametersDataSource : NSObject, MCDataSource
{    
    let Log = Logger(subsystem: "com.matter.casting",
                     category: "MCAppParametersDataSource")
    
    // Dummy values for demonstration only.
    private var commissionableData: MCCommissionableData = MCCommissionableData(
        passcode: 20202021,
        discriminator: 3874,
        // Default to the minimum PBKDF iterations (1,000) for this example implementation. For TV devices and TV casting app production
        // implementations, you should use a higher number of PBKDF iterations to enhance security. The default minimum iterations are
        // not sufficient against brute-force and rainbow table attacks. Increasing the number of iterations will increase the
        // computational time required to derive the key. This can slow down the authentication process, especially on devices with
        // limited processing power like a Raspberry Pi 4. For a production implementation, you should measure the actual performance on
        // the target device.
        //
        // 1,000 - Hypothetical key derivation time: ~20 milliseconds (ms).
        // 100,000 - Hypothetical key derivation time: ~2 seconds.
        spake2pIterationCount: 1000,
        spake2pVerifier: nil,
        spake2pSalt: nil
    )

    /**
    * This function needs to be implemented by the client in use cases where the MCCommissionableData needs to be updated
    * post-initialization. For example, when the Commissioner-Generated Passcode feature is used.
    */
    func update(_ newCommissionableData: MCCommissionableData) {
        Log.info("MCAppParametersDataSource.update() - Before update, passcode: \(self.commissionableData.passcode)")
        self.commissionableData = newCommissionableData
        Log.info("MCAppParametersDataSource.update() - After update, passcode: \(self.commissionableData.passcode)")
    }
    
    func clientQueue() -> DispatchQueue {
        return DispatchQueue.main;
    }
    
    func castingAppDidReceiveRequestForRotatingDeviceIdUniqueId(_ sender: Any) -> Data {
        // dummy value, with at least 16 bytes (ConfigurationManager::kMinRotatingDeviceIDUniqueIDLength), for demonstration only
        return "0123456789ABCDEF".data(using: .utf8)!
    }
    
    func castingAppDidReceiveRequestForCommissionableData(_ sender: Any) -> MCCommissionableData {
        Log.info("MCAppParametersDataSource castingAppDidReceiveRequestForCommissionableData()")
        return commissionableData
    }
    
    // DAC
    let kDevelopmentDAC_Cert_FFF1_8001: Data = Data(base64Encoded: "MIIB6jCCAZGgAwIBAgIQaDU122CP07+DspBkvmPDqDAKBggqhkjOPQQDAjBRMRIwEAYDVQQDDAlWaXpiZWVQQUkxFDASBgorBgEEAYKifAIBDAQxMzgxMQ8wDQYDVQQKDAZWaXpiZWUxFDASBgorBgEEAYKifAICDAQxMDAzMB4XDTI2MDIwNTIyMTc0NFoXDTMxMDIwMzIzMTc0NFowTDEUMBIGCisGAQQBgqJ8AgEMBDEzODExFDASBgorBgEEAYKifAICDAQxMDAzMR4wHAYDVQQDDBVWaXpiZWUgQ2FzdGluZyBEQUMwMDEwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQrppA9Oe3eAc49yXUvem8UFWMG0Yhixjzzor0rEBmTJ6JuhKHBLu1Dli4CbVvqsUp47KM9sGVadFhnS1nIiakSo1AwTjAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFO1d1S8/tmRcQD3i+YvpFNsGqxqPMB0GA1UdDgQWBBRnmXkWdXxAL2il4cA7udJSuPXU2TAKBggqhkjOPQQDAgNHADBEAiAxERPpEY0hXQNCpA+2ja8e9UjL7kd30ji/vY/ohMMdAAIgK+0Hh8rOPbK02dPzH+vxJwEhRRwmXpivozwB/JWBEAw=")!;
    
    // Private Key - raw 32-byte EC scalar (extracted from PKCS#8 DER)
    let kDevelopmentDAC_PrivateKey_FFF1_8001: Data = Data(base64Encoded: "zDNNJYyFzx3Fim6uIY1BQOAhdgeApXCjPEXrtw9NDZQ=")!;

    // Public Key - 65-byte uncompressed P-256 point (matching the new DAC cert)
    let kDevelopmentDAC_PublicKey_FFF1_8001: Data = Data(base64Encoded: "BCumkD057d4Bzj3JdS96bxQVYwbRiGLGPPOivSsQGZMnom6EocEu7UOWLgJtW+qxSnjsoz2wZVp0WGdLWciJqRI=")!;
    
    // PAI
    let KPAI_FFF1_8000_Cert_Array: Data = Data(base64Encoded: "MIIB3zCCAYWgAwIBAgIQc1bDqUKbQ7C7+ZDScMT3fjAKBggqhkjOPQQDAjAqMRIwEAYDVQQDDAlQcmltZSBQQUExFDASBgorBgEEAYKifAIBDAQxMzgxMB4XDTI2MDIwNTE4NTEzNFoXDTMyMDEwMTAwMDAwMFowUTESMBAGA1UEAwwJVml6YmVlUEFJMRQwEgYKKwYBBAGConwCAQwEMTM4MTEPMA0GA1UECgwGVml6YmVlMRQwEgYKKwYBBAGConwCAgwEMTAwMzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABAZCjOTpXbYnltzGxfuJq/0fjuqjnvXd85jVgFJ2DO3SbkI2VmhksvO9tRBHNkVKTTaERU9vHdZsP95f82bXdv+jZjBkMBIGA1UdEwEB/wQIMAYBAf8CAQAwHwYDVR0jBBgwFoAU57mRu5ye0GGny5vxiGRKFWPCTiswHQYDVR0OBBYEFO1d1S8/tmRcQD3i+YvpFNsGqxqPMA4GA1UdDwEB/wQEAwIBBjAKBggqhkjOPQQDAgNIADBFAiBLj4xF6VgaQtPqPGGtAg/PrBJoaJ/lypHtgxVK9UooBQIhAJNKrVmVr61sMjCwG7hTi6Iu4LvZyy5UJj/JscYapJPf")!;
    
    let kCertificationDeclaration: Data = Data(base64Encoded: "MIHtBgkqhkiG9w0BBwKggd8wgdwCAQMxDTALBglghkgBZQMEAgEwSgYJKoZIhvcNAQcBoD0EOxUkAAElARkWNgIEARgkAyksBBNDU0EyNjAwM1NXQzYwMjQxLU0xJAUAJAYAJAcBJAgCJQmBEyUKAxAYMXwwegIBA4AU/jQ/lZlHdjth7kU5ExM4SU/mfY4wCwYJYIZIAWUDBAIBMAoGCCqGSM49BAMCBEYwRAIgFGM0e+1xZtw7HIFqAOQxe2JOJ0LvlK1ccwBWxrBwNQQCIBKLXDPC6EQnnxa7hTNiSBaaED19gAjrPIbYTRiroR9s")!;
    
    func castingAppDidReceiveRequestForDeviceAttestationCredentials(_ sender: Any) -> MCDeviceAttestationCredentials {
        return MCDeviceAttestationCredentials(
            certificationDeclaration: kCertificationDeclaration,
            firmwareInformation: Data(),
            deviceAttestationCert: kDevelopmentDAC_Cert_FFF1_8001,
            productAttestationIntermediateCert: KPAI_FFF1_8000_Cert_Array)
    }
    
    func castingApp(_ sender: Any, didReceiveRequestToSignCertificateRequest csrData: Data, outRawSignature: AutoreleasingUnsafeMutablePointer<NSData>) -> MatterError {
        Log.info("castingApp didReceiveRequestToSignCertificateRequest")

        // get the private SecKey
        var privateKeyData = Data()
        privateKeyData.append(kDevelopmentDAC_PublicKey_FFF1_8001);
        privateKeyData.append(kDevelopmentDAC_PrivateKey_FFF1_8001);
        let privateSecKey: SecKey = SecKeyCreateWithData(privateKeyData as NSData,
                                    [
                                        kSecAttrKeyType: kSecAttrKeyTypeECSECPrimeRandom,
                                        kSecAttrKeyClass: kSecAttrKeyClassPrivate,
                                        kSecAttrKeySizeInBits: 256
                                    ] as NSDictionary, nil)!
        
        // sign csrData to get asn1SignatureData
        var error: Unmanaged<CFError>?
        let asn1SignatureData: CFData? = SecKeyCreateSignature(privateSecKey, .ecdsaSignatureMessageX962SHA256, csrData as CFData, &error)
        if(error != nil)
        {
            Log.error("Failed to sign message. Error: \(String(describing: error))")
            return MATTER_ERROR_INVALID_ARGUMENT
        }
        else if (asn1SignatureData == nil)
        {
            Log.error("Failed to sign message. asn1SignatureData is nil")
            return MATTER_ERROR_INVALID_ARGUMENT
        }
        
        // convert ASN.1 DER signature to SEC1 raw format
        return MCCryptoUtils.ecdsaAsn1SignatureToRaw(withFeLengthBytes: 32,
                                                    asn1Signature: asn1SignatureData!,
                                                         outRawSignature: &outRawSignature.pointee)
    }
}

// This class is a singleton
class MCInitializationExample {
    static let shared = MCInitializationExample()

    let Log = Logger(subsystem: "com.matter.casting",
                     category: "MCInitializationExample")

    // We store the client defined instance of the MCAppParametersDataSource passed to CastingApp.initialize().
    // MCAppParametersDataSource may need to be updated by the client in case of the Casting 
    // Player/Commissioner-Generated passcode commissioning flow.
    private var appParametersDataSource: MCAppParametersDataSource?

    private init() {
        // Private initialization to ensure just one instance is created.
    }

    func initialize() -> Error? {
        if let castingApp = MCCastingApp.getSharedInstance()
        {
            Log.info("MCInitializationExample.initialize() calling MCCastingApp.initializeWithDataSource()")

            let dataSource = MCAppParametersDataSource()
            appParametersDataSource = dataSource

            return castingApp.initialize(with: dataSource)
        }
        else
        {
            return NSError(domain: "com.matter.casting", code: Int(MATTER_ERROR_INCORRECT_STATE.code))
        }
    }

    // Getter method for the stored instance of MCAppParametersDataSource
    func getAppParametersDataSource() -> MCAppParametersDataSource? {
        Log.info("MCInitializationExample.getAppParametersDataSource()")
        return appParametersDataSource
    }
}

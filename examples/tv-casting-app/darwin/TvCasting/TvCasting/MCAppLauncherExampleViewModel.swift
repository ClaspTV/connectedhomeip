/**
 *
 *    Copyright (c) 2024 Project CHIP Authors
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
import os.log

class MCAppLauncherExampleViewModel: ObservableObject {
    let Log = Logger(subsystem: "com.matter.casting",
                     category: "MCAppLauncherExampleViewModel")

    @Published var launchAppStatus: String?
    @Published var appStatusResult: String?
    @Published var subscribeResult: String?

    let sampleEndpointVid: Int = 65521
    let defaultEndpointIdForCGPFlow: Int = 1

    // MARK: - Launch App

    func launchApp(castingPlayer: MCCastingPlayer, useCommissionerGeneratedPasscode: Bool, vendorId: UInt16, applicationId: String) {
        self.Log.info("MCAppLauncherExampleViewModel.launchApp()")
        castingPlayer.logAllEndpoints()

        guard let endpoint = selectEndpoint(castingPlayer: castingPlayer, useCommissionerGeneratedPasscode: useCommissionerGeneratedPasscode) else {
            DispatchQueue.main.async { self.launchAppStatus = "No suitable endpoint found" }
            return
        }

        if !endpoint.hasCluster(MCEndpointClusterTypeApplicationLauncher) {
            DispatchQueue.main.async { self.launchAppStatus = "No ApplicationLauncher cluster on endpoint" }
            return
        }

        let cluster = endpoint.cluster(for: MCEndpointClusterTypeApplicationLauncher) as! MCApplicationLauncherCluster

        guard let launchAppCommand = cluster.launchAppCommand() else {
            DispatchQueue.main.async { self.launchAppStatus = "LaunchApp command not supported on cluster" }
            return
        }

        // Build the ApplicationStruct
        let appStruct = MCApplicationLauncherClusterApplicationStruct()
        appStruct.catalogVendorID = NSNumber(value: vendorId)
        appStruct.applicationID = applicationId

        // Build the request params
        let request = MCApplicationLauncherClusterLaunchAppParams()
        request.application = appStruct

        launchAppCommand.invoke(request, context: nil, completion: { context, err, response in
            DispatchQueue.main.async {
                if err == nil {
                    self.Log.info("LaunchApp success with \(String(describing: response))")
                    self.launchAppStatus = "LaunchApp success\nStatus: \(String(describing: response?.status)), Data: \(String(describing: response?.data))"
                } else {
                    self.Log.error("LaunchApp failure with \(String(describing: err))")
                    self.launchAppStatus = "LaunchApp failure: \(String(describing: err))"
                }
            }
        }, timedInvokeTimeoutMs: 5000)
    }

    // MARK: - Get App Status (Read ApplicationBasic application + status attributes)

    func getAppStatus(castingPlayer: MCCastingPlayer) {
        self.Log.info("MCAppLauncherExampleViewModel.getAppStatus()")

        guard let endpoint = MCEndpointSelector.selectEndpoint(from: castingPlayer, sampleEndpointVid: sampleEndpointVid) else {
            DispatchQueue.main.async { self.appStatusResult = "No suitable endpoint found" }
            return
        }

        if !endpoint.hasCluster(MCEndpointClusterTypeApplicationBasic) {
            DispatchQueue.main.async { self.appStatusResult = "No ApplicationBasic cluster on endpoint" }
            return
        }

        let cluster = endpoint.cluster(for: MCEndpointClusterTypeApplicationBasic) as! MCApplicationBasicCluster

        var applicationValue = "NOT_FOUND"
        var statusValue = "NOT_FOUND"

        // Read application attribute
        if let applicationAttribute = cluster.applicationAttribute() {
            applicationAttribute.read(nil) { context, before, after, err in
                DispatchQueue.main.async {
                    if err == nil {
                        applicationValue = String(describing: after)
                        self.appStatusResult = "application: \(applicationValue)\nstatus: \(statusValue)"
                    } else {
                        self.appStatusResult = "ReadApplication error: \(String(describing: err))"
                    }
                }
            }
        }

        // Read status attribute
        if let statusAttribute = cluster.statusAttribute() {
            statusAttribute.read(nil) { context, before, after, err in
                DispatchQueue.main.async {
                    if err == nil {
                        statusValue = String(describing: after)
                        self.appStatusResult = "application: \(applicationValue)\nstatus: \(statusValue)"
                    } else {
                        self.appStatusResult = "ReadStatus error: \(String(describing: err))"
                    }
                }
            }
        }
    }

    // MARK: - Subscribe to ApplicationLauncher attributes

    func subscribeAppStatus(castingPlayer: MCCastingPlayer, useCommissionerGeneratedPasscode: Bool) {
        self.Log.info("MCAppLauncherExampleViewModel.subscribeAppStatus()")

        guard let endpoint = selectEndpoint(castingPlayer: castingPlayer, useCommissionerGeneratedPasscode: useCommissionerGeneratedPasscode) else {
            DispatchQueue.main.async { self.subscribeResult = "No suitable endpoint found" }
            return
        }

        if !endpoint.hasCluster(MCEndpointClusterTypeApplicationLauncher) {
            DispatchQueue.main.async { self.subscribeResult = "No ApplicationLauncher cluster on endpoint" }
            return
        }

        let cluster = endpoint.cluster(for: MCEndpointClusterTypeApplicationLauncher) as! MCApplicationLauncherCluster

        // Subscribe to CurrentApp
        if let currentAppAttribute = cluster.currentAppAttribute() {
            currentAppAttribute.subscribe(nil, completion: { context, before, after, err in
                DispatchQueue.main.async {
                    if err == nil {
                        self.subscribeResult = "CurrentApp: \(String(describing: after))"
                    } else {
                        self.subscribeResult = "CurrentApp subscribe error: \(String(describing: err))"
                    }
                }
            }, minInterval: 0, maxInterval: 1)
        }

        // Subscribe to CatalogList
        if let catalogListAttribute = cluster.catalogListAttribute() {
            catalogListAttribute.subscribe(nil, completion: { context, before, after, err in
                DispatchQueue.main.async {
                    if err == nil {
                        self.Log.info("CatalogList: \(String(describing: after))")
                    } else {
                        self.Log.error("CatalogList subscribe error: \(String(describing: err))")
                    }
                }
            }, minInterval: 0, maxInterval: 1)
        }
    }

    // MARK: - Shutdown subscriptions

    func shutdownSubscriptions() {
        if let castingApp = MCCastingApp.getSharedInstance() {
            castingApp.shutdownAllSubscriptions()
        }
    }

    // MARK: - Endpoint selection helper

    private func selectEndpoint(castingPlayer: MCCastingPlayer, useCommissionerGeneratedPasscode: Bool) -> MCEndpoint? {
        if useCommissionerGeneratedPasscode {
            // For CGP flow, select by endpoint ID = 1
            return castingPlayer.endpoints().first(where: { $0.identifier().intValue == defaultEndpointIdForCGPFlow })
        } else {
            return MCEndpointSelector.selectEndpoint(from: castingPlayer, sampleEndpointVid: sampleEndpointVid)
        }
    }
}

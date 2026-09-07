/**
 *
 *    Copyright (c) 2020-2022 Project CHIP Authors
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

import SwiftUI
import os.log

@main
struct TvCastingApp: App {
    let Log = Logger(subsystem: "com.matter.casting",
                     category: "TvCastingApp")

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear(perform: {
                    let err: Error? = MCInitializationExample.shared.initialize()
                    if err != nil
                    {
                        self.Log.error("MCCastingApp initialization failed \(err)")
                    }
                })
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.willResignActiveNotification)) { _ in
                    self.Log.info("TvCastingApp: UIApplication.willResignActiveNotification")
                    if let castingApp = MCCastingApp.getSharedInstance()
                    {
                        castingApp.stop(completionBlock: { (err : Error?) -> () in
                            if err != nil
                            {
                                self.Log.error("MCCastingApp stop failed \(err)")
                            }
                        })
                    }
                }
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
                    self.Log.info("TvCastingApp: UIApplication.didBecomeActiveNotification")
                    if let castingApp = MCCastingApp.getSharedInstance()
                    {
                        castingApp.start(completionBlock: { (err : Error?) -> () in
                            if err != nil
                            {
                                self.Log.error("MCCastingApp start failed \(err)")
                            }
                        })
                    }
                }
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.didEnterBackgroundNotification)) { _ in
                    self.Log.info("TvCastingApp: UIApplication.didEnterBackgroundNotification")
                }
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.didFinishLaunchingNotification)) { _ in
                    self.Log.info("TvCastingApp: UIApplication.didFinishLaunchingNotification")
                }
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
                    self.Log.info("TvCastingApp: UIApplication.willEnterForegroundNotification")
                }
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.willTerminateNotification)) { _ in
                    self.Log.info("TvCastingApp: UIApplication.willTerminateNotification")
                }
        }
    }
}

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

import SwiftUI

struct MCAppLauncherExampleView: View {
    @StateObject var viewModel = MCAppLauncherExampleViewModel()

    var selectedCastingPlayer: MCCastingPlayer?
    var useCommissionerGeneratedPasscode: Bool

    @State private var vendorId: String = "65521"
    @State private var applicationId: String = "exampleid"

    init(_selectedCastingPlayer: MCCastingPlayer?, _useCommissionerGeneratedPasscode: Bool) {
        self.selectedCastingPlayer = _selectedCastingPlayer
        self.useCommissionerGeneratedPasscode = _useCommissionerGeneratedPasscode
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {

                // -- Launch App section --
                Text("Launch App").font(.headline)

                HStack {
                    Text("Vendor ID")
                    TextField("65521", text: $vendorId)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .keyboardType(.numberPad)
                }

                HStack {
                    Text("Application ID")
                    TextField("exampleid", text: $applicationId)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .disableAutocorrection(true)
                        .textInputAutocapitalization(.never)
                }

                Button("Launch App") {
                    let vid = UInt16(vendorId) ?? 65521
                    viewModel.launchApp(
                        castingPlayer: selectedCastingPlayer!,
                        useCommissionerGeneratedPasscode: useCommissionerGeneratedPasscode,
                        vendorId: vid,
                        applicationId: applicationId
                    )
                }
                .frame(width: 300, height: 30)
                .background(Color.blue)
                .foregroundColor(Color.white)
                .cornerRadius(4)
                .border(Color.black, width: 1)

                Text(viewModel.launchAppStatus ?? "").font(.caption)

                Divider()

                // -- Get App Status section --
                Text("Get App Status").font(.headline)

                Button("Get App Status") {
                    viewModel.getAppStatus(castingPlayer: selectedCastingPlayer!)
                }
                .frame(width: 300, height: 30)
                .background(Color.blue)
                .foregroundColor(Color.white)
                .cornerRadius(4)
                .border(Color.black, width: 1)

                Text(viewModel.appStatusResult ?? "").font(.caption)

                Divider()

                // -- Subscribe App Status section --
                Text("Subscribe App Status").font(.headline)

                Button("Subscribe App Status") {
                    viewModel.subscribeAppStatus(
                        castingPlayer: selectedCastingPlayer!,
                        useCommissionerGeneratedPasscode: useCommissionerGeneratedPasscode
                    )
                }
                .frame(width: 300, height: 30)
                .background(Color.blue)
                .foregroundColor(Color.white)
                .cornerRadius(4)
                .border(Color.black, width: 1)

                Text(viewModel.subscribeResult ?? "").font(.caption)
            }
            .padding()
        }
        .navigationTitle("App Launcher")
        .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity, alignment: .top)
        .onDisappear {
            viewModel.shutdownSubscriptions()
        }
    }
}

struct MCAppLauncherExampleView_Previews: PreviewProvider {
    static var previews: some View {
        MCAppLauncherExampleView(_selectedCastingPlayer: nil, _useCommissionerGeneratedPasscode: false)
    }
}

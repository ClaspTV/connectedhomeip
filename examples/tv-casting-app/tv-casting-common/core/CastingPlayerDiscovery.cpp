/*
 *
 *    Copyright (c) 2023 Project CHIP Authors
 *    All rights reserved.
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

#include "CastingPlayerDiscovery.h"

namespace matter {
namespace casting {
namespace core {

using namespace chip::System;
using namespace chip::Controller;
using namespace chip::Dnssd;

namespace {

CastingPlayerAttributes BuildCastingPlayerAttributes(const chip::Dnssd::CommissionNodeData & nodeData)
{
    CastingPlayerAttributes attributes;
    snprintf(attributes.id, kIdMaxLength + 1, "%s%u", nodeData.hostName, nodeData.port);

    chip::Platform::CopyString(attributes.deviceName, chip::Dnssd::kMaxDeviceNameLen + 1, nodeData.deviceName);
    chip::Platform::CopyString(attributes.hostName, chip::Dnssd::kHostNameMaxLength + 1, nodeData.hostName);
    chip::Platform::CopyString(attributes.instanceName, chip::Dnssd::Commission::kInstanceNameMaxLength + 1, nodeData.instanceName);

    attributes.numIPs = static_cast<unsigned int>(nodeData.numIPs);
    for (unsigned j = 0; j < attributes.numIPs; j++)
    {
        attributes.ipAddresses[j] = nodeData.ipAddress[j];
    }
    attributes.interfaceId                           = nodeData.interfaceId;
    attributes.port                                  = nodeData.port;
    attributes.productId                             = nodeData.productId;
    attributes.vendorId                              = nodeData.vendorId;
    attributes.deviceType                            = nodeData.deviceType;
    attributes.supportsCommissionerGeneratedPasscode = nodeData.supportsCommissionerGeneratedPasscode;
    return attributes;
}

bool MatchesNodeIdentity(const memory::Strong<CastingPlayer> & castingPlayer, const chip::Dnssd::CommissionNodeData & nodeData)
{
    if (nodeData.instanceName[0] != '\0' && strlen(castingPlayer->GetInstanceName()) != 0)
    {
        return strcmp(castingPlayer->GetInstanceName(), nodeData.instanceName) == 0;
    }

    return strcmp(castingPlayer->GetHostName(), nodeData.hostName) == 0 && castingPlayer->GetPort() == nodeData.port;
}

} // namespace

CastingPlayerDiscovery * CastingPlayerDiscovery::_castingPlayerDiscovery = nullptr;

CastingPlayerDiscovery::CastingPlayerDiscovery() {}

CastingPlayerDiscovery * CastingPlayerDiscovery::GetInstance()
{
    if (_castingPlayerDiscovery == nullptr)
    {
        _castingPlayerDiscovery = new CastingPlayerDiscovery();
    }
    return _castingPlayerDiscovery;
}

CHIP_ERROR CastingPlayerDiscovery::StartDiscovery(uint32_t deviceTypeFilter, chip::Dnssd::DiscoveryMode mode)
{
    ChipLogProgress(Discovery, "CastingPlayerDiscovery::StartDiscovery() called");
    VerifyOrReturnError(mState == DISCOVERY_READY, CHIP_ERROR_INCORRECT_STATE);

    mCommissionableNodeController.RegisterDeviceDiscoveryDelegate(&mDelegate);

    if (deviceTypeFilter > 0)
    {
        ReturnErrorOnFailure(mCommissionableNodeController.DiscoverCommissioners(
            DiscoveryFilter(DiscoveryFilterType::kDeviceType, deviceTypeFilter), mode));
    }
    else
    {
        ReturnErrorOnFailure(mCommissionableNodeController.DiscoverCommissioners(DiscoveryFilter(), mode));
    }

    mState = DISCOVERY_RUNNING;
    return CHIP_NO_ERROR;
}

CHIP_ERROR CastingPlayerDiscovery::StopDiscovery()
{
    ChipLogProgress(Discovery, "CastingPlayerDiscovery::StopDiscovery() mCastingPlayers: %u, mCastingPlayersInternal: %u",
                    static_cast<unsigned int>(mCastingPlayers.size()), static_cast<unsigned int>(mCastingPlayersInternal.size()));
    VerifyOrReturnError(mState == DISCOVERY_RUNNING, CHIP_ERROR_INCORRECT_STATE);
    ReturnErrorOnFailure(mCommissionableNodeController.StopDiscovery());

    // Copy mCastingPlayers to mCastingPlayersInternal
    mCastingPlayersInternal = std::vector<memory::Strong<CastingPlayer>>(mCastingPlayers);

    // Clear mCastingPlayers of all CastingPlayers
    mCastingPlayers.clear();
    mState = DISCOVERY_READY;

    return CHIP_NO_ERROR;
}

void CastingPlayerDiscovery::ClearDisconnectedCastingPlayersInternal()
{
    ChipLogProgress(Discovery, "CastingPlayerDiscovery::ClearDisconnectedCastingPlayersInternal() mCastingPlayersInternal: %u",
                    static_cast<unsigned int>(mCastingPlayersInternal.size()));
    // Only clear the CastingPlayers in mCastingPlayersInternal with ConnectionState == CASTING_PLAYER_NOT_CONNECTED
    for (auto it = mCastingPlayersInternal.begin(); it != mCastingPlayersInternal.end();)
    {
        auto & player = *it;
        if (player->GetConnectionState() == CASTING_PLAYER_NOT_CONNECTED)
        {
            ChipLogProgress(
                Discovery,
                "CastingPlayerDiscovery::ClearDisconnectedCastingPlayersInternal() Removing disconnected CastingPlayer: %s "
                "with reference count: %lu",
                player->GetDeviceName(), player.use_count());
            it = mCastingPlayersInternal.erase(it);
        }
        else
        {
            ++it; // Move to the next element if the current one is not removed
        }
    }
}

void CastingPlayerDiscovery::ClearCastingPlayersInternal()
{
    ChipLogProgress(Discovery, "CastingPlayerDiscovery::ClearCastingPlayersInternal()");
    mCastingPlayersInternal.clear();
}

void DeviceDiscoveryDelegateImpl::OnDiscoveredDevice(const chip::Dnssd::CommissionNodeData & nodeData)
{
    ChipLogProgress(Discovery,
                    "DeviceDiscoveryDelegateImpl::OnDiscoveredDevice() instanceName='%s' deviceName='%s' port=%u",
                    nodeData.instanceName, nodeData.deviceName, nodeData.port);
    VerifyOrReturn(mClientDelegate != nullptr,
                   ChipLogError(Discovery, "DeviceDiscoveryDelegateImpl::OnDiscoveredDevice mClientDelegate is a nullptr"));

    CastingPlayerAttributes attributes = BuildCastingPlayerAttributes(nodeData);
    std::vector<memory::Strong<CastingPlayer>> castingPlayers = CastingPlayerDiscovery::GetInstance()->GetCastingPlayers();

    if (!castingPlayers.empty())
    {
        auto it = std::find_if(castingPlayers.begin(), castingPlayers.end(), [&nodeData](const memory::Strong<CastingPlayer> & castingPlayer) {
            return MatchesNodeIdentity(castingPlayer, nodeData);
        });

        if (it != castingPlayers.end())
        {
            (*it)->UpdateFromAttributes(attributes);
            (*it)->SetActive(true);
            ChipLogProgress(AppServer, "Updated Casting Player");
            mClientDelegate->HandleOnUpdated(*it);
            return;
        }
    }

    memory::Strong<CastingPlayer> player = std::make_shared<CastingPlayer>(attributes);
    player->SetActive(true);
    castingPlayers.push_back(player);
    CastingPlayerDiscovery::GetInstance()->mCastingPlayers = castingPlayers;
    mClientDelegate->HandleOnAdded(player);
}

void DeviceDiscoveryDelegateImpl::OnRemovedDevice(const chip::Dnssd::CommissionNodeData & nodeData)
{
    ChipLogProgress(Discovery,
                    "DeviceDiscoveryDelegateImpl::OnRemovedDevice() instanceName='%s' deviceName='%s' port=%u",
                    nodeData.instanceName, nodeData.deviceName, nodeData.port);
    VerifyOrReturn(mClientDelegate != nullptr,
                   ChipLogError(Discovery, "DeviceDiscoveryDelegateImpl::OnRemovedDevice mClientDelegate is a nullptr"));

    std::vector<memory::Strong<CastingPlayer>> castingPlayers = CastingPlayerDiscovery::GetInstance()->GetCastingPlayers();
    auto it = std::find_if(castingPlayers.begin(), castingPlayers.end(), [&nodeData](const memory::Strong<CastingPlayer> & castingPlayer) {
        return MatchesNodeIdentity(castingPlayer, nodeData);
    });

    VerifyOrReturn(it != castingPlayers.end(),
                   ChipLogProgress(Discovery, "DeviceDiscoveryDelegateImpl::OnRemovedDevice() no matching CastingPlayer found"));

    memory::Strong<CastingPlayer> player = *it;

    if (player->GetConnectionState() == CASTING_PLAYER_CONNECTING)
    {
        ChipLogProgress(Discovery,
                        "DeviceDiscoveryDelegateImpl::OnRemovedDevice() suppressed removal, commissioning in progress");
        return;
    }

    player->SetActive(false);
    castingPlayers.erase(it);
    CastingPlayerDiscovery::GetInstance()->mCastingPlayers = castingPlayers;
    mClientDelegate->HandleOnRemoved(player);
}

}; // namespace core
}; // namespace casting
}; // namespace matter

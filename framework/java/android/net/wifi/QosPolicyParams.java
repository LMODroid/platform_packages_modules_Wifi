/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.wifi;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.DscpPolicy;
import android.net.MacAddress;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;

/**
 * Parameters for QoS policies requested by system applications.
 * @hide
 */
@SystemApi
public final class QosPolicyParams implements Parcelable {
    private static final String TAG = "QosPolicyParams";

    /**
     * Indicates that the policy does not specify a DSCP value.
     */
    public static final int DSCP_ANY = -1;

    /**
     * Indicates that the policy does not specify a protocol.
     */
    public static final int PROTOCOL_ANY = DscpPolicy.PROTOCOL_ANY;

    /**
     * Policy should match packets using the TCP protocol.
     */
    public static final int PROTOCOL_TCP = 6;

    /**
     * Policy should match packets using the UDP protocol.
     */
    public static final int PROTOCOL_UDP = 17;

    /**
     * Policy should match packets using the ESP protocol.
     */
    public static final int PROTOCOL_ESP = 50;

    /** @hide */
    @IntDef(prefix = { "PROTOCOL_" }, value = {
            PROTOCOL_ANY,
            PROTOCOL_TCP,
            PROTOCOL_UDP,
            PROTOCOL_ESP
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Protocol {}

    /**
     * Policy should match packets in the uplink direction.
     */
    public static final int DIRECTION_UPLINK = 0;

    /**
     * Policy should match packets in the downlink direction.
     */
    public static final int DIRECTION_DOWNLINK = 1;


    /** @hide */
    @IntDef(prefix = { "DIRECTION_" }, value = {
            DIRECTION_UPLINK,
            DIRECTION_DOWNLINK,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Direction {}

    /**
     * Indicates that the policy does not specify a User Priority.
     */
    public static final int USER_PRIORITY_ANY = -1;

    /**
     * Policy should be assigned a low background priority.
     */
    public static final int USER_PRIORITY_BACKGROUND_LOW = 1;

    /**
     * Policy should be assigned a high background priority.
     */
    public static final int USER_PRIORITY_BACKGROUND_HIGH = 2;

    /**
     * Policy should be assigned a low best-effort priority.
     */
    public static final int USER_PRIORITY_BEST_EFFORT_LOW = 0;

    /**
     * Policy should be assigned a high best-effort priority.
     */
    public static final int USER_PRIORITY_BEST_EFFORT_HIGH = 3;

    /**
     * Policy should be assigned a low video priority.
     */
    public static final int USER_PRIORITY_VIDEO_LOW = 4;

    /**
     * Policy should be assigned a high video priority.
     */
    public static final int USER_PRIORITY_VIDEO_HIGH = 5;

    /**
     * Policy should be assigned a low voice priority.
     */
    public static final int USER_PRIORITY_VOICE_LOW = 6;

    /**
     * Policy should be assigned a high voice priority.
     */
    public static final int USER_PRIORITY_VOICE_HIGH = 7;

    /** @hide */
    @IntDef(prefix = { "USER_PRIORITY_" }, value = {
            USER_PRIORITY_ANY,
            USER_PRIORITY_BACKGROUND_LOW,
            USER_PRIORITY_BACKGROUND_HIGH,
            USER_PRIORITY_BEST_EFFORT_LOW,
            USER_PRIORITY_BEST_EFFORT_HIGH,
            USER_PRIORITY_VIDEO_LOW,
            USER_PRIORITY_VIDEO_HIGH,
            USER_PRIORITY_VOICE_LOW,
            USER_PRIORITY_VOICE_HIGH,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface UserPriority {}

    /**
     * Unique policy ID. See {@link Builder#Builder(int, int)} for more information.
     */
    private final int mPolicyId;

    /**
     * Translated policy ID. Should only be set by the Wi-Fi service.
     * @hide
     */
    private int mTranslatedPolicyId;

    // QoS DSCP marking. See {@link Builder#setDscp(int)} for more information.
    private final int mDscp;

    // User priority to apply to packets matching the policy. Only applicable to downlink requests.
    private final int mUserPriority;

    // Source address.
    private final @Nullable MacAddress mSrcAddr;

    // Destination address.
    private final @Nullable MacAddress mDstAddr;

    // Source port.
    private final int mSrcPort;

    // IP protocol that the policy requires.
    private final @Protocol int mProtocol;

    // Destination port range. Inclusive range.
    private final @Nullable int[] mDstPortRange;

    // Direction of traffic stream.
    private final @Direction int mDirection;

    private QosPolicyParams(int policyId, int dscp, @UserPriority int userPriority,
            @Nullable MacAddress srcAddr, @Nullable MacAddress dstAddr, int srcPort,
            @Protocol int protocol, @Nullable int[] dstPortRange, @Direction int direction) {
        this.mPolicyId = policyId;
        this.mDscp = dscp;
        this.mUserPriority = userPriority;
        this.mSrcAddr = srcAddr;
        this.mDstAddr = dstAddr;
        this.mSrcPort = srcPort;
        this.mProtocol = protocol;
        this.mDstPortRange = dstPortRange;
        this.mDirection = direction;
    }

    /**
     * Validate the parameters in this instance.
     *
     * @return true if all parameters are valid, false otherwise
     * @hide
     */
    public boolean validate() {
        if (mPolicyId < 1 || mPolicyId > 255) {
            Log.e(TAG, "Policy ID not in valid range: " + mPolicyId);
            return false;
        }
        if (mDscp < DSCP_ANY || mDscp > 63) {
            Log.e(TAG, "DSCP value not in valid range: " + mDscp);
            return false;
        }
        if (mUserPriority < USER_PRIORITY_ANY || mUserPriority > USER_PRIORITY_VOICE_HIGH) {
            Log.e(TAG, "User priority not in valid range: " + mUserPriority);
            return false;
        }
        if (mSrcPort < DscpPolicy.SOURCE_PORT_ANY || mSrcPort > 65535) {
            Log.e(TAG, "Source port not in valid range: " + mSrcPort);
            return false;
        }
        if (mDstPortRange != null && (mDstPortRange[0] < 0 || mDstPortRange[0] > 65535
                || mDstPortRange[1] < 0 || mDstPortRange[1] > 65535)) {
            Log.e(TAG, "Dst port range value not valid. start="
                    + mDstPortRange[0] + ", end=" + mDstPortRange[1]);
            return false;
        }
        if (!(mDirection == DIRECTION_UPLINK || mDirection == DIRECTION_DOWNLINK)) {
            Log.e(TAG, "Invalid direction enum: " + mDirection);
            return false;
        }

        // Check DSCP and User Priority based on direction
        if (mDirection == DIRECTION_UPLINK && mDscp == DSCP_ANY) {
            Log.e(TAG, "DSCP must be provided for uplink requests");
            return false;
        }
        if (mDirection == DIRECTION_DOWNLINK && mUserPriority == USER_PRIORITY_ANY) {
            Log.e(TAG, "User priority must be provided for downlink requests");
            return false;
        }
        return true;
    }

    /**
     * Set the translated policy ID for this policy.
     *
     * Note: Translated policy IDs should only be set by the Wi-Fi service.
     * @hide
     */
    public void setTranslatedPolicyId(int translatedPolicyId) {
        mTranslatedPolicyId = translatedPolicyId;
    }

    /**
     * Get the ID for this policy.
     *
     * See {@link Builder#Builder(int, int)} for more information.
     */
    public int getPolicyId() {
        return mPolicyId;
    }

    /**
     * Get the translated ID for this policy.
     *
     * See {@link #setTranslatedPolicyId} for more information.
     * @hide
     */
    public int getTranslatedPolicyId() {
        return mTranslatedPolicyId;
    }


    /**
     * Get the DSCP value for this policy.
     *
     * See {@link Builder#setDscp(int)} for more information.
     *
     * @return DSCP value, or {@link #DSCP_ANY} if not assigned.
     */
    public int getDscp() {
        return mDscp;
    }

    /**
     * Get the User Priority (UP) for this policy.
     *
     * See {@link Builder#setUserPriority(int)} for more information.
     *
     * @return User Priority value, or {@link #USER_PRIORITY_ANY} if not assigned.
     */
    public @UserPriority int getUserPriority() {
        return mUserPriority;
    }

    /**
     * Get the source address for this policy.
     *
     * See {@link Builder#setSourceAddress(MacAddress)} for more information.
     *
     * @return source address, or null if not assigned.
     */
    public @Nullable MacAddress getSourceAddress() {
        return mSrcAddr;
    }

    /**
     * Get the destination address for this policy.
     *
     * See {@link Builder#setDestinationAddress(MacAddress)} for more information.
     *
     * @return destination address, or null if not assigned.
     */
    public @Nullable MacAddress getDestinationAddress() {
        return mDstAddr;
    }

    /**
     * Get the source port for this policy.
     *
     * See {@link Builder#setSourcePort(int)} for more information.
     *
     * @return source port, or {@link DscpPolicy#SOURCE_PORT_ANY} if not assigned.
     */
    public int getSourcePort() {
        return mSrcPort;
    }

    /**
     * Get the protocol for this policy.
     *
     * See {@link Builder#setProtocol(int)} for more information.
     *
     * @return protocol, or {@link #PROTOCOL_ANY} if not assigned.
     */
    public @Protocol int getProtocol() {
        return mProtocol;
    }

    /**
     * Get the destination port range for this policy.
     *
     * See {@link Builder#setDestinationPortRange(int, int)} for more information.
     *
     * @return destination port range, or null if not assigned.
     */
    public @Nullable int[] getDestinationPortRange() {
        return mDstPortRange;
    }

    /**
     * Get the direction for this policy.
     *
     * See {@link Builder#Builder(int, int)} for more information.
     */
    public @Direction int getDirection() {
        return mDirection;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QosPolicyParams that = (QosPolicyParams) o;
        return mPolicyId == that.mPolicyId
                && mDscp == that.mDscp
                && mUserPriority == that.mUserPriority
                && mSrcAddr.equals(that.mSrcAddr)
                && mDstAddr.equals(that.mDstAddr)
                && mSrcPort == that.mSrcPort
                && mProtocol == that.mProtocol
                && Arrays.equals(mDstPortRange, that.mDstPortRange)
                && mDirection == that.mDirection;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPolicyId, mDscp, mUserPriority, mSrcAddr, mDstAddr, mSrcPort,
                mProtocol, Arrays.hashCode(mDstPortRange), mDirection);
    }

    @Override
    public String toString() {
        return "{policyId=" + mPolicyId + ", "
                + "dscp=" + mDscp + ", "
                + "userPriority=" + mUserPriority + ", "
                + "srcAddr=" + mSrcAddr + ", "
                + "dstAddr=" + mDstAddr + ", "
                + "srcPort=" + mSrcPort + ", "
                + "protocol=" + mProtocol + ", "
                + "dstPortRange=" + Arrays.toString(mDstPortRange) + ", "
                + "direction=" + mDirection + "}";
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mPolicyId);
        dest.writeInt(mDscp);
        dest.writeInt(mUserPriority);
        dest.writeParcelable(mSrcAddr, 0);
        dest.writeParcelable(mDstAddr, 0);
        dest.writeInt(mSrcPort);
        dest.writeInt(mProtocol);
        dest.writeIntArray(mDstPortRange);
        dest.writeInt(mDirection);
    }

    /** @hide */
    QosPolicyParams(@NonNull Parcel in) {
        this.mPolicyId = in.readInt();
        this.mDscp = in.readInt();
        this.mUserPriority = in.readInt();
        this.mSrcAddr = in.readParcelable(MacAddress.class.getClassLoader());
        this.mDstAddr = in.readParcelable(MacAddress.class.getClassLoader());
        this.mSrcPort = in.readInt();
        this.mProtocol = in.readInt();
        this.mDstPortRange = in.createIntArray();
        this.mDirection = in.readInt();
    }

    public static final @NonNull Parcelable.Creator<QosPolicyParams> CREATOR =
            new Parcelable.Creator<QosPolicyParams>() {
                @Override
                public QosPolicyParams createFromParcel(Parcel in) {
                    return new QosPolicyParams(in);
                }

                @Override
                public QosPolicyParams[] newArray(int size) {
                    return new QosPolicyParams[size];
                }
            };

    /**
     * Builder for {@link QosPolicyParams}.
     */
    public static final class Builder {
        private final int mPolicyId;
        private final @Direction int mDirection;
        private @Nullable MacAddress mSrcAddr;
        private @Nullable MacAddress mDstAddr;
        private int mDscp = DSCP_ANY;
        private @UserPriority int mUserPriority = USER_PRIORITY_ANY;
        private int mSrcPort = DscpPolicy.SOURCE_PORT_ANY;
        private int mProtocol = PROTOCOL_ANY;
        private @Nullable int[] mDstPortRange;

        /**
         * Constructor for {@link Builder}.
         *
         * @param policyId Unique ID to identify this policy. Each requesting application is
         *                 responsible for maintaining policy IDs unique for that app. IDs must be
         *                 in the range 1 <= policyId <= 255.
         *
         *                 In the case where a policy with an existing ID is created, the new policy
         *                 will be rejected. To update an existing policy, remove the existing one
         *                 before sending the new one.
         * @param direction Whether this policy applies to the uplink or downlink direction.
         */
        public Builder(int policyId, @Direction int direction) {
            mPolicyId = policyId;
            mDirection = direction;
        }

        /**
         * Specifies that this policy matches packets with the provided source address.
         */
        public @NonNull Builder setSourceAddress(@NonNull MacAddress value) {
            Objects.requireNonNull(value, "Source address cannot be null");
            mSrcAddr = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the provided destination address.
         */
        public @NonNull Builder setDestinationAddress(@NonNull MacAddress value) {
            Objects.requireNonNull(value, "Destination address cannot be null");
            mDstAddr = value;
            return this;
        }

        /**
         * Specifies the DSCP value. For uplink requests, this value will be applied to packets
         * that match the classifier. For downlink requests, this will be part of the classifier.
         */
        public @NonNull Builder setDscp(int value) {
            mDscp = value;
            return this;
        }

        /**
         * Specifies that the provided User Priority should be applied to packets that
         * match this classifier. Only applicable to downlink requests.
         */
        public @NonNull Builder setUserPriority(@UserPriority int value) {
            mUserPriority = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the provided source port.
         */
        public @NonNull Builder setSourcePort(int value) {
            mSrcPort = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the provided protocol.
         */
        public @NonNull Builder setProtocol(@Protocol int value) {
            mProtocol = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the provided destination port range.
         */
        public @NonNull Builder setDestinationPortRange(int start, int end) {
            mDstPortRange = new int[]{start, end};
            return this;
        }

        /**
         * Construct a QosPolicyParams object with the specified parameters.
         */
        public @NonNull QosPolicyParams build() {
            QosPolicyParams params = new QosPolicyParams(mPolicyId, mDscp, mUserPriority, mSrcAddr,
                    mDstAddr, mSrcPort, mProtocol, mDstPortRange, mDirection);
            if (!params.validate()) {
                throw new IllegalArgumentException("Provided parameters are invalid");
            }
            return params;
        }
    }
}

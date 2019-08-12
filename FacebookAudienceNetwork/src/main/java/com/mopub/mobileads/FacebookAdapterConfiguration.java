package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.BidderTokenProvider;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.facebookaudiencenetwork.BuildConfig;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class FacebookAdapterConfiguration extends BaseAdapterConfiguration {

    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;

    private AtomicReference<String> tokenReference = new AtomicReference<>(null);
    private AtomicBoolean isComputingToken = new AtomicBoolean(false);

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        refreshBidderToken(context);
        return tokenReference.get();
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        final String adapterVersion = getAdapterVersion();
        return adapterVersion.substring(0, adapterVersion.lastIndexOf('.'));
    }

    @Override
    public void initializeNetwork(@NonNull final Context context,
                                  @Nullable final Map<String, String> configuration,
                                  @NonNull final OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);
        tokenReference.set(BidderTokenProvider.getBidderToken(context));

        synchronized (FacebookAdapterConfiguration.class) {
            try {
                AudienceNetworkAds.buildInitSettings(context)
                        .withMediationService("MOPUB_" + MoPub.SDK_VERSION + ":" + ADAPTER_VERSION)
                        .initialize();
            } catch (Throwable t) {
                MoPubLog.log(
                        CUSTOM_WITH_THROWABLE,
                        "Initializing Facebook Audience Network" + " has encountered an exception.",
                        t);
            }
        }
        listener.onNetworkInitializationFinished(this.getClass(),
                MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
    }

    private void refreshBidderToken(final Context context) {
        if (isComputingToken.compareAndSet(false, true)) {
            try {
                String token = BidderTokenProvider.getBidderToken(context);
                if(token != null) {
                    tokenReference.set(token);
                }
                isComputingToken.set(false);
                return;
            } catch (Throwable t) {
                return;
            }
        }
    }
}

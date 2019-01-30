package com.mopub.mobileads;

import android.app.Application;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.BidderTokenProvider;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class FacebookAdapterConfiguration extends BaseAdapterConfiguration {

    private static final String ADAPTER_VERSION = "5.1.0.1";
    private static final String MOPUB_NETWORK_NAME = "facebook";

    private static AtomicReference<String> tokenReference = new AtomicReference<>();
    private static AtomicBoolean isComputingToken = new AtomicBoolean(false);

    static {
        initializeBidderToken();
    }

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        String token = tokenReference.get();
        refreshBidderToken(context);
        return token;
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

        boolean networkInitializationSucceeded = false;

        synchronized (FacebookAdapterConfiguration.class) {
            try {
                AudienceNetworkAds.initialize(context);
                networkInitializationSucceeded = true;
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing Facebook Audience Network" +
                        " has encountered an exception.", e);
            }
        }

        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(this.getClass(),
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(this.getClass(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    private static void initializeBidderToken() {
        try {
            final Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            final Method method = activityThreadClass.getMethod("currentApplication");
            Context context = (Application) method.invoke(null, (Object[]) null);

            refreshBidderToken(context);
        } catch (Throwable e) {
            // No-op
        }
    }

    private static void refreshBidderToken(final Context context) {
        if (isComputingToken.compareAndSet(false, true)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    tokenReference.set(BidderTokenProvider.getBidderToken(context));
                    isComputingToken.set(false);
                }
            }).start();
        }
    }
}

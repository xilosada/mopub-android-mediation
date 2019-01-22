package com.mopub.mobileads;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.facebook.ads.BidderTokenProvider;
import com.mopub.common.MoPubAdvancedBidder;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Include this class to use advanced bidding from Facebook.
 */
public class FacebookAdvancedBidder implements MoPubAdvancedBidder {

    static {
        initializeBidderToken();
    }

    private static AtomicReference<String> tokenReference = new AtomicReference<>();
    private static AtomicBoolean isComputingToken = new AtomicBoolean(false);

    @Override
    public String getToken(@NonNull final Context context) {
        String token = tokenReference.get();
        refreshBidderToken(context);
        return token;
    }

    @Override
    public String getCreativeNetworkName() {
        return "facebook";
    }

    private static void initializeBidderToken() {
        try {
            final Class<?> activityThreadClass =
                    Class.forName("android.app.ActivityThread");
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

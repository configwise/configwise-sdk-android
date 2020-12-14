package io.configwise.android.sdk_example;

import android.app.Application;

import io.configwise.sdk.ConfigWiseSDK;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        initConfigWiseSdk();
    }

    private void initConfigWiseSdk() {
        ConfigWiseSDK.initialize(new ConfigWiseSDK.Builder(this)
                .sdkVariant(ConfigWiseSDK.SdkVariant.B2C)
                .companyAuthToken("YOUR_COMPANY_AUTH_TOKEN")
                .dbAccessPeriod(1 * 60 * 60 * 1000) // (msec) 1 hr. Set 0 if you wish always to request data from server DB (ignoring locally cached queries)
                .lightEstimateEnabled(true)
                .debugLogging(false)
                .debug3d(false)
        );
    }

}

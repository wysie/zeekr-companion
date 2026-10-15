package com.kooo.evcam.ui;

import android.content.Context;
import android.os.Build;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;
import com.kooo.evcam.zeekr.VehicleIdentity;

/** One presentation name for builtin profiles; legacy profile IDs remain stable. */
public final class VehicleLabels {
    private VehicleLabels() {}

    public static String name(Context context) {
        VehicleIdentity.Model model = VehicleIdentity.resolve(new AppConfig(context).getVehicleNameMode(),
                Build.MODEL, Build.DEVICE, Build.PRODUCT);
        return context.getString(model == VehicleIdentity.Model.ZEEKR_009 ? R.string.vehicle_name_009
                : model == VehicleIdentity.Model.ZEEKR_7X ? R.string.vehicle_name_7x : R.string.vehicle_name_generic);
    }

    public static String profile(Context context, boolean cabins) {
        return context.getString(cabins ? R.string.vehicle_profile_cabins : R.string.vehicle_profile_surround,
                name(context));
    }
}

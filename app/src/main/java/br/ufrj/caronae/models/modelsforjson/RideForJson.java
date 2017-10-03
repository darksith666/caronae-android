package br.ufrj.caronae.models.modelsforjson;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

import br.ufrj.caronae.App;
import br.ufrj.caronae.models.Ride;
import br.ufrj.caronae.models.User;

public class RideForJson extends Ride implements Parcelable {
    private User driver;
    private List<User> riders;

    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
    }

    public List<User> getRiders() {
        return riders;
    }

    public void setRiders(List<User> riders) {
        this.riders = riders;
    }

    public RideForJson(Parcel in) {
        String[] data = new String[12];
        in.readStringArray(data);

        zone = data[0];
        neighborhood = data[1];
        place = data[2];
        route = data[3];
        date = data[4];
        slots = data[5];
        time = data[6];
        hub = data[7];
        description = data[8];
        weekDays = data[9];
        repeatsUntil = data[10];
        routineId = data[11];

        int[] intData = new int[4];
        in.readIntArray(intData);
        going = intData[0] == 1;
        routine = intData[1] == 1;
        dbId = intData[2];
        availableSlots = intData[3];

        driver = in.readParcelable(User.class.getClassLoader());
        //noinspection unchecked
        riders = in.readArrayList(User.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStringArray(new String[]{zone,
                neighborhood,
                place,
                route,
                date,
                slots,
                time,
                hub,
                description,
                weekDays,
                repeatsUntil,
                routineId});
        parcel.writeIntArray(new int[]{
                going ? 1 : 0,
                routine ? 1 : 0,
                dbId,
                availableSlots
        });
        parcel.writeParcelable(driver, 0);
        parcel.writeList(riders);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public RideForJson createFromParcel(Parcel in) {
            return new RideForJson(in);
        }

        public RideForJson[] newArray(int size) {
            return new RideForJson[size];
        }
    };
}

package br.ufrj.caronae.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;

import java.io.IOException;
import java.util.List;

import br.ufrj.caronae.App;
import br.ufrj.caronae.models.ActiveRideId;
import br.ufrj.caronae.models.ChatMessageReceived;
import br.ufrj.caronae.models.Ride;
import br.ufrj.caronae.models.modelsforjson.TokenForJson;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LogOut extends AsyncTask<Void, Void, Void> {
    @Override
    protected void onPreExecute() {
        App.getNetworkService().saveGcmToken(new TokenForJson(""), new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                Log.i("saveGcmToken", "gcm token cleared");
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("saveGcmToken", error.getMessage());
            }
        });
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        App.clearUser();

        App.removePref(App.USER_PREF_KEY);
        App.removePref(App.LAST_RIDE_OFFER_PREF_KEY);
        App.removePref(App.LAST_RIDE_SEARCH_FILTERS_PREF_KEY);
        App.removePref(App.TOKEN_PREF_KEY);
        App.removePref(App.NOTIFICATIONS_ON_PREF_KEY);

        List<ActiveRideId> activeRideIds = ActiveRideId.listAll(ActiveRideId.class);
        if (!activeRideIds.isEmpty()) {
            GcmPubSub pubSub = GcmPubSub.getInstance(App.inst());
            for (ActiveRideId ari : activeRideIds) {
                try {
                    pubSub.unsubscribe(App.getUserGcmToken(), "/topics/" + ari.getRideId());
                    Log.i("logOut", "unsubscribed from ride " + ari.getRideId());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Ride.deleteAll(Ride.class);
        ActiveRideId.deleteAll(ActiveRideId.class);
        ChatMessageReceived.deleteAll(ChatMessageReceived.class);

        return null;
    }
}
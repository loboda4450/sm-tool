package com.example.smtool.ui.home;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.smtool.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    private String json = null;

    private Boolean access = false;

    private Boolean credentialsWrong = false;

    private Boolean urlWrong = false;

    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getContext()));
        String ip = sharedPreferences.getString("ip", "");
        String port = sharedPreferences.getString("port", "");
        String login = sharedPreferences.getString("login", "");
        String passwd = sharedPreferences.getString("password", "");

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        TableLayout tl = (TableLayout) root.findViewById(R.id.home_container);

        if (login != "" && passwd != "") {
            Toast.makeText(getActivity(), "Trying to login", Toast.LENGTH_SHORT).show();
            while (!access) {
                try {
                    RequestBody requestbody = new FormBody.Builder()
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url("http://" + ip + ":" + port + "/smtool/api/v0.1/verify?login="+login+"&passwd="+passwd)
                            .post(requestbody)
                            .build();

                    client.newCall(request).enqueue(new Callback() {

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            Log.e("Status code: ", String.valueOf(response.code()));
                            access = true;
                            if (!response.isSuccessful()) {
                                credentialsWrong = true;
                            }
                            response.close();
                        }

                        @SuppressLint("ShowToast")
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            Log.e("Trying to login", String.valueOf(e)); //Crashing app on wrong IP or PORT, need to skip getting data and let user enter preferences.
                            urlWrong = true;
                            access = true;
                        }
                    });
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            access = true;
            credentialsWrong = true;
            //TODO:Show dialog to enter username and password
        }

        TextView tv;

        if (!urlWrong && !credentialsWrong) {
            TableRow tr;
            Toast.makeText(getActivity(), "Getting data from server", Toast.LENGTH_SHORT).show();

            try {
                doGetRequest("http://" + ip + ":" + port + "/smtool/api/v0.1/platform");
                do {
                    Thread.sleep(100);
                    Log.v("Log1", "Waiting for data from server");
                } while (json == null);
                JSONObject sysInfo = new JSONObject(json.trim());
                Iterator<String> keys = sysInfo.keys();

                while (keys.hasNext()) {
                    String key = keys.next();
                    Log.v(key, String.valueOf(sysInfo.get(key)));
                    tr = new TableRow(getActivity());
                    tl.setPadding(16, 16, 16, 16);
                    tl.addView(tr);
                    tv = new TextView(getActivity());
                    tv.setText(key + ": ");
                    tv.setTypeface(null, Typeface.BOLD);
                    tr.addView(tv);
                    tr = new TableRow(getActivity());
                    tl.addView(tr);
                    tv = new TextView(getActivity());
                    tv.setMinLines(2);
                    tv.setMaxLines(3);
                    tv.setText(String.valueOf(sysInfo.get(key)));
                    tr.addView(tv);
                }
            } catch (IOException | JSONException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            tv = new TextView(getActivity());
            if (urlWrong) tv.setText("Wrong IP or PORT");
            if (credentialsWrong) tv.setText("Wrong Username or Password");
            tl.addView(tv);
        }

        return root;
    }

    void doGetRequest(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        Log.e("Error getting access to infos", e.toString());
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        json = Objects.requireNonNull(response.body()).string();
                        Log.v("Got infos", json);
                        response.close();
                    }
                });
    }
}
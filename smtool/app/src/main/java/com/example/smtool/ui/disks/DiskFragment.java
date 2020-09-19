package com.example.smtool.ui.disks;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.smtool.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
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

import static android.widget.Toast.LENGTH_LONG;

public class DiskFragment extends Fragment {

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

        final View root = inflater.inflate(R.layout.fragment_disks, container, false);

        final TableLayout tl = (TableLayout) root.findViewById(R.id.disks_container);

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
            TableLayout tl1;
            TableRow tr, tr1;
            ProgressBar pb;
            Toast.makeText(getActivity(), "Getting data from server", Toast.LENGTH_SHORT).show();

            try {
                doGetRequest("http://" + ip + ":" + port + "/smtool/api/v0.1/disks_info");
                do {
                    Thread.sleep(100);
                    Log.v("Log1", "Waiting for data from server");
                } while (json == null);

                JSONObject jObj = new JSONObject(json);
                JSONArray jsonArry = jObj.getJSONArray("disks");
                StringBuilder keyIndex = new StringBuilder();
                int keyIdentifier;
                int max, min, used;
                for (int i = 0; i < jsonArry.length(); i++) {
                    keyIdentifier = 0;
                    JSONObject obj = jsonArry.getJSONObject(i);
                    Iterator<String> keys = obj.keys();
                    tl.setPadding(16, 16, 16, 16);
                    tr = new TableRow(getActivity());
                    tl.addView(tr);
                    while (keys.hasNext()) {
                        String key = keys.next();
                        keyIndex.append(", ").append(key);
                        Log.v(key, obj.getString(key));
                        tr1 = new TableRow(getActivity());
                        tl.addView(tr1);
                        tl1 = new TableLayout(getActivity());
                        tr1.addView(tl1);
                        tr1 = new TableRow(getActivity());
                        tl1.addView(tr1);
                        tv = new TextView(getActivity());
                        tv.setText(key + ": ");
                        tv.setTypeface(null, Typeface.BOLD);
                        if (keyIdentifier != 0) {
                            tv.setPadding(50, 0, 0, 0);
                            tv.setMinLines(2);
                        } else {
                            tv.setMinLines(1);
                        }
                        tr1.addView(tv);
                        tr = new TableRow(getActivity());
                        tl.addView(tr);
                        tv = new TextView(getActivity());
                        tv.setMaxLines(3);
                        tv.setText(obj.getString(key));
                        tr1.addView(tv);
                        keyIdentifier++;
                    }
                    if (keyIndex.toString().contains("Used") && keyIndex.toString().contains("Total Size")) {
                        max = Math.round(Float.parseFloat(obj.getString("Total Size").replaceAll("[a-z]|[A-Z]", "")));
                        min = 0;
                        used = Math.round(Float.parseFloat(obj.getString("Used").replaceAll("[a-z]|[A-Z]", "")));
                        pb = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleHorizontal);
                        pb.setMax(max);
                        pb.setMin(min);
                        pb.setProgress(used);
                        pb.setPadding(50, 0, 0, 50);
                        tr.addView(pb);
                    }
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
                    @SuppressLint("ShowToast")
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        Toast.makeText(getActivity(), "Could not connect to server using your IP and PORT", LENGTH_LONG);
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
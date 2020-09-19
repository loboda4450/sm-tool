package com.example.smtool.ui.swap_graphs;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
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
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SwapGraphsFragment extends Fragment {

    private String json = null;

    private Boolean access = false;

    private Boolean credentialsWrong = false;

    private Boolean urlWrong = false;

    @SuppressLint({"SetTextI18n", "ShowToast"})
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getContext()));
        String ip = sharedPreferences.getString("ip", "");
        String port = sharedPreferences.getString("port", "");
        String login = sharedPreferences.getString("login", "");
        String passwd = sharedPreferences.getString("password", "");

        View root = inflater.inflate(R.layout.fragment_graphs, container, false);

        TableLayout tl = root.findViewById(R.id.graph_container);

        if (login != "" && passwd != "") {
            Toast.makeText(getActivity(), "Trying to login", Toast.LENGTH_SHORT).show();
            while (!access) {
                try {
                    RequestBody requestbody = new FormBody.Builder()
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
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
                doGetRequest("http://" + ip + ":" + port + "/smtool/api/v0.1/swap_info");
                do {
                    Thread.sleep(100);
                    Log.v("Log1", "Waiting for data from server");
                } while (json == null);

                JSONObject jObj = new JSONObject(json);
                JSONArray jsonArry = jObj.getJSONArray("Collected data");
                DataPoint[] points = new DataPoint[jsonArry.length()];
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                GraphView graph = tl.findViewById(R.id.graph);
                for (int i = 0; i < jsonArry.length(); i++) {
                    JSONObject obj = jsonArry.getJSONObject(i);
                    tl.setPadding(16, 16, 16, 16);
                    points[i] = new DataPoint(formatter.parse(obj.getString("timestamp")), Float.parseFloat(obj.getString("usedswap")));
                }
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(points);
//                graph.getViewport().setScrollable(true); // enables horizontal scrolling
//                graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling //TODO:Fix Vertical labels in scrolling and zooming mode
                graph.addSeries(series);
                DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(getContext());
                graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity(), dateFormat));
                graph.getGridLabelRenderer().setNumVerticalLabels(6);
                graph.getGridLabelRenderer().setNumHorizontalLabels(3);
                graph.getViewport().setXAxisBoundsManual(true);
                graph.getGridLabelRenderer().setHumanRounding(false);


            } catch (IOException | JSONException | InterruptedException | ParseException e) {
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
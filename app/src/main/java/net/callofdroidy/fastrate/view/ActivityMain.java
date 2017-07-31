package net.callofdroidy.fastrate.view;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import net.callofdroidy.fastrate.R;
import net.callofdroidy.fastrate.utils.ServiceGenerator;
import net.callofdroidy.fastrate.webapi.CurrencyRateService;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityMain extends AppCompatActivity implements TextWatcher{
    private static final String TAG = "ActivityMain";
    private static final DecimalFormat df = new DecimalFormat("0.#####");
    private static final String COL_QUOTE_CURRENCY = "QuoteCurrency";
    private static final String COL_QUOTE_VALUE = "QuoteValue";

    EditText etInput;
    Spinner spinner;
    GridView gvConvertResult;

    Handler handler;
    Runnable filterTask;

    JSONObject savedRates;

    int currentSpinnerItemPosition;

    SimpleAdapter gridViewAdapter;

    List<Map<String, String>> dataSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        handler = new Handler();
        filterTask = new Runnable() {
            @Override
            public void run() {
                dataSet.clear();
                dataSet.addAll(convertRates(savedRates, spinner.getItemAtPosition(currentSpinnerItemPosition).toString(), etInput.getText().toString()));
                gridViewAdapter.notifyDataSetChanged();
            }
        };

        // check the last timestamp when updating rates from fixer.io
        long lastUpdateTime = getSharedPreferences("last_update_time", 0).getLong("time", 0);
        if(System.currentTimeMillis() - lastUpdateTime > 30 * 1000){
            // if more than 30 minutes(or new installed) pull data from fixer.io
            CurrencyRateService rateService = ServiceGenerator.createService(CurrencyRateService.class);
            Call<String> apiResponse = rateService.getLatestRates();
            apiResponse.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try{
                        JSONObject responseInJson = new JSONObject(response.body());
                        JSONObject rates = responseInJson.getJSONObject("rates");
                        rates.put("EUR", 1.0);
                        getSharedPreferences("data", 0).edit().putString("rates", rates.toString()).apply();
                        getSharedPreferences("last_update_time", 0).edit().putLong("time", System.currentTimeMillis()).apply();

                        savedRates = rates;
                        initStatus();
                    }catch (JSONException e){
                        Log.e(TAG, "onCreate: " + e.toString());
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(ActivityMain.this, "Fail to get rates data", Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            // directly read local data
            savedRates = getSavedRates();
            initStatus();
        }
    }

    private void initViews(){
        etInput = (EditText) findViewById(R.id.et_input);
        etInput.addTextChangedListener(this);
        spinner = (Spinner) findViewById(R.id.spinner_base_currency);
        gvConvertResult = (GridView) findViewById(R.id.gv_convert_result);
        try{
            Field popup = Spinner.class.getDeclaredField("mPopup");
            popup.setAccessible(true);
            android.widget.ListPopupWindow popupWindow = (android.widget.ListPopupWindow) popup.get(spinner);
            // Set popupWindow height to 150dp
            popupWindow.setHeight(Math.round(getResources().getDisplayMetrics().density) * 150);
        }catch (NoClassDefFoundError|ClassCastException|NoSuchFieldException|IllegalAccessException e){
            Log.e(TAG, "onCreate: " + e.toString());
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                dataSet.clear();
                dataSet.addAll(convertRates(savedRates, spinner.getItemAtPosition(pos).toString(), etInput.getText().toString()));
                //dataSet = convertRates(savedRates, spinner.getItemAtPosition(pos).toString(), etInput.getText().toString());
                currentSpinnerItemPosition = pos;
                gridViewAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void initStatus(){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ActivityMain.this, android.R.layout.simple_spinner_item, getCurrencyList(savedRates));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        currentSpinnerItemPosition = 0;

        dataSet = convertRates(savedRates, spinner.getItemAtPosition(currentSpinnerItemPosition).toString(), "");
        gridViewAdapter = new SimpleAdapter(
                ActivityMain.this,
                dataSet,
                R.layout.grid_cell,
                new String[] {COL_QUOTE_CURRENCY, COL_QUOTE_VALUE},
                new int[] {R.id.tv_quote_currency, R.id.tv_quote_value}
        );
        gvConvertResult.setAdapter(gridViewAdapter);
    }

    public JSONObject getSavedRates(){
        try{
            return new JSONObject(getSharedPreferences("data", 0).getString("rates", ""));
        }catch (JSONException e){
            Log.e(TAG, "onCreate: " + e.toString());
            return null;
        }
    }

    // in case fixer.io add or delete any currency in their list, get the latest currency list
    // every time there's a new result
    public List<String> getCurrencyList(JSONObject rates){
        //check null
        List<String> list = new ArrayList<>();
        Iterator<String> keys = rates.keys();
        while (keys.hasNext())
            list.add(keys.next());
        return list;
    }

    public List<Map<String, String>> convertRates(JSONObject rates, String baseCurrency, String value){
        List<Map<String, String>> list = new ArrayList<>();
        if(value.equals("")){
            Iterator<String> keys = rates.keys();
            while(keys.hasNext()) {
                Map<String, String> map = new HashMap<>();
                map.put(COL_QUOTE_CURRENCY, keys.next());
                map.put(COL_QUOTE_VALUE, "");
                list.add(map);
            }
        }else {
            df.setRoundingMode(RoundingMode.CEILING);
            Iterator<String> keys = rates.keys();
            while(keys.hasNext()){
                String key = keys.next();
                double convertedValue = 0;
                try{
                    convertedValue = rates.getDouble(key) / rates.getDouble(baseCurrency) * Double.parseDouble(value);
                }catch (JSONException e){
                    Log.e(TAG, "convertRates: " + e.toString());
                }
                Map<String, String> map = new HashMap<>();
                map.put(COL_QUOTE_CURRENCY, key);
                map.put(COL_QUOTE_VALUE, df.format(convertedValue));
                list.add(map);
            }
        }
        return list;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        handler.removeCallbacks(filterTask);
        handler.postDelayed(filterTask, 1000); // do the conversion when the user stops input longer than 1 second
    }
}

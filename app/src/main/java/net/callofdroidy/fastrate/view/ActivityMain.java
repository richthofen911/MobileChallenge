package net.callofdroidy.fastrate.view;

import android.content.Context;
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
import net.callofdroidy.fastrate.presenter.PresenterMain;

import org.json.JSONObject;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static net.callofdroidy.fastrate.utils.Constants.COL_QUOTE_CURRENCY;
import static net.callofdroidy.fastrate.utils.Constants.COL_QUOTE_VALUE;

public class ActivityMain extends AppCompatActivity implements ViewActMain, TextWatcher{
    private static final String TAG = "ActivityMain";

    EditText etInput;
    Spinner spinner;
    GridView gvConvertResult;

    Handler handler;

    int currentSpinnerItemPosition;

    SimpleAdapter gridViewAdapter;

    List<Map<String, String>> dataSet;

    private PresenterMain presenterMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        presenterMain = new PresenterMain(this);

        initViews();

        presenterMain.getRates(new PresenterMain.GetRatesCallback() {
            @Override
            public void onGetRates(JSONObject rates) {
                initStatus(rates);
            }
        });
    }

    @Override
    public Context getViewContext(){
        return this;
    }

    public void initViews(){
        etInput = (EditText) findViewById(R.id.et_input);
        etInput.addTextChangedListener(this);
        gvConvertResult = (GridView) findViewById(R.id.gv_convert_result);
        spinner = (Spinner) findViewById(R.id.spinner_base_currency);
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
            public void onItemSelected(AdapterView<?> adapterView, View view, final int pos, long id) {
                presenterMain.getRates(new PresenterMain.GetRatesCallback() {
                    @Override
                    public void onGetRates(JSONObject rates) {
                        dataSet.clear();
                        dataSet.addAll(presenterMain.convertRates(rates, spinner.getItemAtPosition(pos).toString(), etInput.getText().toString()));
                        currentSpinnerItemPosition = pos;
                        gridViewAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void showErrMsg(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void initStatus(final JSONObject rates){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ActivityMain.this, android.R.layout.simple_spinner_item, getCurrencyList(rates));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        currentSpinnerItemPosition = 0;

        dataSet = presenterMain.convertRates(rates, spinner.getItemAtPosition(currentSpinnerItemPosition).toString(), "");
        gridViewAdapter = new SimpleAdapter(
                ActivityMain.this,
                dataSet,
                R.layout.grid_cell,
                new String[] {COL_QUOTE_CURRENCY, COL_QUOTE_VALUE},
                new int[] {R.id.tv_quote_currency, R.id.tv_quote_value}
        );
        gvConvertResult.setAdapter(gridViewAdapter);
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

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        // do the conversion when the user stops input longer than 1 second
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dataSet.clear();
                presenterMain.getRates(new PresenterMain.GetRatesCallback() {
                    @Override
                    public void onGetRates(JSONObject rates) {
                        dataSet.addAll(presenterMain.convertRates(rates, spinner.getItemAtPosition(currentSpinnerItemPosition).toString(), etInput.getText().toString()));
                    }
                });
                gridViewAdapter.notifyDataSetChanged();
            }
        }, 1000);
    }
}

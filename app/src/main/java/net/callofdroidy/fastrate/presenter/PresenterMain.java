package net.callofdroidy.fastrate.presenter;

import android.util.Log;

import net.callofdroidy.fastrate.utils.ServiceGenerator;
import net.callofdroidy.fastrate.view.ViewActMain;
import net.callofdroidy.fastrate.webapi.CurrencyRateService;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static net.callofdroidy.fastrate.utils.Constants.COL_QUOTE_CURRENCY;
import static net.callofdroidy.fastrate.utils.Constants.COL_QUOTE_VALUE;

/**
 * Created by yli on 28/08/17.
 */

public class PresenterMain {
    private static final String TAG = "PresenterMain";
    private static final DecimalFormat df = new DecimalFormat("0.#####");

    private ViewActMain viewActMain;

    public PresenterMain(ViewActMain viewActMain){
        this.viewActMain = viewActMain;
    }

    public interface GetRatesCallback{
        void onGetRates(JSONObject rates);
    }

    public void getRates(final GetRatesCallback getRatesCallback){
        long lastUpdateTime = viewActMain.getViewContext().getSharedPreferences("info", 0).getLong("last_update_time", 0);
        // get rates from server when more than 30 minutes
        if(System.currentTimeMillis() - lastUpdateTime > 30 * 1000){
            ServiceGenerator.createService(CurrencyRateService.class).getLatestRates().enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try{
                        JSONObject responseInJson = new JSONObject(response.body());
                        JSONObject rates = responseInJson.getJSONObject("rates");
                        rates.put("EUR", 1.0);
                        viewActMain.getViewContext().getSharedPreferences("info", 0).edit()
                                .putString("rates", rates.toString())
                                .putLong("last_update_time", System.currentTimeMillis())
                                .apply();
                        getRatesCallback.onGetRates(rates);
                    }catch (JSONException e){
                        Log.e(TAG, "onResponse: " + e.toString());
                        viewActMain.showErrMsg(e.toString());
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    viewActMain.showErrMsg(t.getMessage());
                }
            });
        }else {
            // get local saved rates
            try{
                getRatesCallback.onGetRates(new JSONObject(viewActMain.getViewContext().getSharedPreferences("info", 0).getString("rates", "")));
            }catch (JSONException e){
                Log.e(TAG, "onCreate: " + e.toString());
                viewActMain.showErrMsg(e.toString());
            }
        }
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
            Iterator<String> keys = rates.keys();
            while(keys.hasNext()){
                String key = keys.next();

                BigDecimal convertedValue = new BigDecimal(0);
                try{
                    convertedValue = new BigDecimal(rates.getDouble(key)).divide(new BigDecimal(rates.getDouble(baseCurrency)), 5, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(value));
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
}

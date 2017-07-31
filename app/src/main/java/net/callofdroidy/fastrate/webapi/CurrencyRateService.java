package net.callofdroidy.fastrate.webapi;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by Richthofen80 on 7/25/2017.
 */

public interface CurrencyRateService {
    @GET("/latest")
    Call<String> getLatestRates();
}

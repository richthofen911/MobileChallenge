package net.callofdroidy.fastrate.webapi;

import io.reactivex.Observable;
import retrofit2.http.GET;

/**
 * Created by Richthofen80 on 7/25/2017.
 */

public interface CurrencyRateService {
    @GET("/latest")
    Observable<String> getLatestRates();
}

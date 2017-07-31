package net.callofdroidy.fastrate.utils;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Created by Richthofen80 on 7/25/2017.
 */

public class ServiceGenerator {
    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    private static Retrofit.Builder builder = new Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create());

    public static <S> S createService(Class<S> serviceClass){
        return builder.client(httpClient.build()).build().create(serviceClass);
    }
}

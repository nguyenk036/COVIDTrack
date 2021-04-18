package com.kevinnguyenle.covidtracker;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * CallbackFuture - customized callback that utilizes CompleteableFuture to assure data is returned
 *                  before continuing tasks
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class CallbackFuture extends CompletableFuture<Response> implements Callback {
    public void onResponse(Call call, Response response) {
        super.complete(response);
    }
    public void onFailure(Call call, IOException e){
        e.printStackTrace();
    }
}

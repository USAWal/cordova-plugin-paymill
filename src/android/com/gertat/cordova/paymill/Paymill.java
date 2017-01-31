package com.gertat.cordova.paymill;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import com.paymill.android.service.*;
import com.paymill.android.listener.*;
import com.paymill.android.factory.*;
import com.paymill.android.api.*;

import android.app.Application;
import android.app.Activity;
import android.content.res.Resources;

import android.util.Log;
import java.util.LinkedList;

public class Paymill extends CordovaPlugin {

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if(action.equals("processTransaction")) {
      this.processTransaction(args, callbackContext);
    } else {
      return false;
    }

    return true;
  }

  @Override
  protected void pluginInitialize() {
    super.pluginInitialize();
    if(PMManager.isInit()) {
      return;
    }

    Activity activity = cordova.getActivity();
    Application application = activity.getApplication();
    String packageName = application.getPackageName();
    Resources resources = application.getResources();

    String  paymillPublicKey = resources.getString(resources.getIdentifier("paymill_public_key", "string", packageName));
    String  paymillTestMode = resources.getString(resources.getIdentifier("paymill_test_mode", "string", packageName));
    PMManager.init(activity.getApplicationContext(), paymillTestMode.equals("OFF") ? PMService.ServiceMode.LIVE : PMService.ServiceMode.TEST, paymillPublicKey, null, null);

    Log.d(LOG_TAG, "Paymill is initialized now.");
  }

  @Override
  public void onDestroy() {
    while(!TRANSACTION_LISTENERS.isEmpty()) {
      PMManager.removeListener(TRANSACTION_LISTENERS.remove());
    }
  }

  private void processTransaction(JSONArray args, final CallbackContext callbackContext) {
    Activity activity = cordova.getActivity();

    Log.d(LOG_TAG, args.toString());

    try {
      String cardholder = args.getString(0);
      String number = args.getString(1);
      String exp_month = args.getString(2);
      String exp_year = args.getString(3);
      String cvc = args.getString(4);

      String currency = args.getString(5);
      int amount_int = args.getInt(6);

      PMPaymentMethod method = PMFactory.genCardPayment(cardholder, number, exp_month, exp_year, cvc);
      PMPaymentParams params = PMFactory.genPaymentParams(currency, amount_int, null);
      PMTransListener listener = new PMTransListener() {
        public void onTransactionFailed(PMError error) {
          Log.e(LOG_TAG, "Error: " + error.getMessage());
          String userMessage = error.getType().name();
          if(PMError.Type.WRONG_PARAMS.equals(error.getType())) {
            userMessage += " (" + error.getMessage() + ")";
          }
          callbackContext.error(userMessage);
          PMManager.removeListener(this);
        }

        public void onTransaction(Transaction transaction) {
          JSONArray response = new JSONArray();
          response
            .put(transaction.getId());
          callbackContext.success(response);
          PMManager.removeListener(this);
        }
      };
      PMManager.addListener(listener);
      TRANSACTION_LISTENERS.add(listener);
      PMManager.transaction(activity.getApplicationContext(), method, params, false);
    } catch(JSONException error) {
      callbackContext.error(error.getLocalizedMessage());
    }
  }

  private final LinkedList<PMTransListener> TRANSACTION_LISTENERS = new LinkedList<PMTransListener>();
  private final static String LOG_TAG = "Paymill";

}

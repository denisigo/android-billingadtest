package com.denisigo.billingadtest;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.denisigo.billingadtest.billingutil.IabHelper;
import com.denisigo.billingadtest.billingutil.IabResult;
import com.denisigo.billingadtest.billingutil.Inventory;
import com.denisigo.billingadtest.billingutil.Purchase;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	// Item name for premium status
	private static final String SKU_PREMIUM = "premium";
	// Flag set to true when we have premium status
	private boolean mIsPremium = false;

	// Advertising instance
	private AdView mAdView;
	// In-app Billing helper
	private IabHelper mAbHelper;
	// Advertising placement layout
	private RelativeLayout mAdLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mAdLayout = (RelativeLayout) findViewById(R.id.adplacement);
		
		// your app's base64 encoded public key
		// TODO: place here your base64 encoded key
		String base64EncodedPublicKey = "";

		// Create in-app billing helper
		mAbHelper = new IabHelper(this, base64EncodedPublicKey);
		// and start setup. If setup is successfull, query inventory we already own
		mAbHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				
				if (!result.isSuccess()) {
					return;
				}

				mAbHelper.queryInventoryAsync(mGotInventoryListener);
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mAbHelper != null)
			mAbHelper.dispose();
		mAbHelper = null;
	}

	public void onBtUpgradeClick(View view) {
		mAbHelper.launchPurchaseFlow(this, SKU_PREMIUM, 0,
				mPurchaseFinishedListener, "");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (mAbHelper == null)
			return;

		// Pass on the activity result to the helper for handling
		if (!mAbHelper.handleActivityResult(requestCode, resultCode, data)) {
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			super.onActivityResult(requestCode, resultCode, data);
		} else {
		}
	}

	/**
	 * Listener that is called when we finish purchase flow.
	 */
	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			
			if (result.isFailure()) {
				return;
			}
			
			// Purchase was successfull, set premium flag and update interface
			if (purchase.getSku().equals(SKU_PREMIUM)) {
				mIsPremium = true;
				updateInterface();
			}
		}
	};

	/** 
	 * Listener that's called when we finish querying the items and
	 * subscriptions we own
	 */
	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result,
				Inventory inventory) {

			// Have we been disposed of in the meantime? If so, quit.
			if (mAbHelper == null)
				return;

			// Is it a failure?
			if (result.isFailure()) {
				return;
			}

			// Do we have the premium upgrade?
			Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
			mIsPremium = premiumPurchase != null;
			updateInterface();
		}
	};

	/**
	 * Updates interface
	 */
	private void updateInterface() {

		Button btUpgrade = (Button) findViewById(R.id.btUpgrade);
		if (mIsPremium) {
			btUpgrade.setText(R.string.you_have_premium_status);
			btUpgrade.setEnabled(false);
			displayAd(false);
		} else {
			btUpgrade.setEnabled(true);
			displayAd(true);
		}
	}

	/**
	 * Displays or hides the advertising
	 * 
	 * @param state
	 */
	private void displayAd(boolean state) {

		if (state) {
			if (mAdView == null) {
				
				// Google has dropped Google Play Services support for Froyo
				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
					mAdLayout.setVisibility(View.VISIBLE);

					mAdView = new AdView(this);
					mAdView.setAdUnitId(getResources().getString(
							R.string.ad_unit_id));
					mAdView.setAdSize(AdSize.BANNER);

					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
							RelativeLayout.LayoutParams.MATCH_PARENT,
							RelativeLayout.LayoutParams.WRAP_CONTENT);
					mAdLayout.addView(mAdView, params);
					mAdView.loadAd(new AdRequest.Builder().build());
				}
			}
		} else {

			mAdLayout.setVisibility(View.GONE);
			if (mAdView != null) {
				mAdView.destroy();
				mAdView = null;
			}

		}
	}
}

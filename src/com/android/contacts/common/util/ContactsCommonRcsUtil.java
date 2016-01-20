/*
 * Copyright (c) 2015 pci-suntektech Technologies, Inc.  All Rights Reserved.
 * pci-suntektech Technologies Proprietary and Confidential.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.android.contacts.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Log;
import com.android.contacts.common.list.DefaultContactListAdapter;
import com.suntek.mway.rcs.client.api.richscreen.RichScreenApi;
import com.suntek.rcs.ui.common.RcsLog;
import com.suntek.mway.rcs.client.api.support.SupportApi;

public class ContactsCommonRcsUtil {

    public static final String TAG = "ContactsCommonRcsUtil";

    public static final String RCS_CAPABILITY_CHANGED = "rcs_capability_changed";

    public static final String RCS_CAPABILITY_CHANGED_CONTACT_ID =
        "rcs_capability_changed_contact_id";

    public static final String RCS_CAPABILITY_CHANGED_VALUE = "rcs_capability_changed_value";

    public static final String RCS_CAPABILITY_MIMETYPE = "vnd.android.cursor.item/capability";

    // User requst to update enhance screen
    public static final String UPDATE_ENHANCE_SCREEN_PHONE_EVENT = "933 10 12000";

    private static int DEFAULT_NUMBER_LENGTH = 11;

    public static final HashMap<Long, Boolean> RCS_CAPABILITY_MAP = new HashMap<Long, Boolean>();

    public static final HashMap<Long, Boolean> RCS_CAPABILITY_MAP_CACHE =
        new HashMap<Long, Boolean>();

    private static SupportApi mSupportApi = SupportApi.getInstance();

    public static boolean isRcsSupported() {
        return mSupportApi.isRcsSupported();
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

    public static void loadRcsCapabilityOfContacts(final Context context,
            final DefaultContactListAdapter adapter) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ContentResolver resolver = context.getContentResolver();
                Cursor c = resolver.query(ContactsContract.Data.CONTENT_URI,
                new String[] {
                    ContactsContract.Data.DATA1
                }, Data.MIMETYPE + " = ?" + " and " +
                ContactsContract.Data.DATA2 + " = ?", new String[] {
                        RCS_CAPABILITY_MIMETYPE, String.valueOf(1)
                }, null);
                try {
                    if (c != null && c.moveToFirst()) {
                        do {
                            Long contactId = c.getLong(0);
                            RCS_CAPABILITY_MAP.put(contactId, true);
                        } while (c.moveToNext());
                    }
                } finally {
                    if(null != c){
                        c.close();
                    }
                }
                return null;
            }

            protected void onPostExecute(Void result) {
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        }.execute();
    }

    private static boolean isWifiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            ConnectivityManager connManager = (ConnectivityManager)context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiInfo.isConnected();
        } else {
            return false;
        }
    }

    public static String getFormatNumber(String number){
        if(null == number){
            return "";
        }
        number = number.replaceAll("-", "");
        number = number.replaceAll(" ", "");
        number = number.replaceAll(",", "");
        int numberLen = number.length();
        if(numberLen > DEFAULT_NUMBER_LENGTH){
            number = number.substring(numberLen - DEFAULT_NUMBER_LENGTH, numberLen);
        }
        return number;
    }

    public static void updateAllEnhanceScreeen(final Context context) {
        if (!isWifiEnabled(context))
            return;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> phoneNumberList = new ArrayList<String>();
                Cursor phonecursor = context.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[] {
                            Phone.NUMBER
                        }, null, null, null);
                try {
                    if (phonecursor != null && phonecursor.moveToFirst()) {
                        do {
                            String phoneNumber = phonecursor.getString(0);
                            phoneNumberList.add(getFormatNumber(phoneNumber));
                        } while (phonecursor.moveToNext());
                    }
                } finally {
                    if (null != phonecursor) {
                        phonecursor.close();
                    }
                }
                try {
                    for (String aPhoneNumber : phoneNumberList) {
                        if (!TextUtils.isEmpty(aPhoneNumber)) {
                            RcsLog.d("Phone Number is: " + aPhoneNumber);
                            RcsLog.d("Calling downloadRichScrnObj for " + aPhoneNumber);
                            RichScreenApi.getInstance().downloadRichScrnObj(aPhoneNumber,
                                    UPDATE_ENHANCE_SCREEN_PHONE_EVENT);
                        }
                    }
                } catch (Exception e) {
                    RcsLog.e("e.printStackTrace() " + e.toString());
                    return;
                }
            }
        });
        t.start();
    }
}

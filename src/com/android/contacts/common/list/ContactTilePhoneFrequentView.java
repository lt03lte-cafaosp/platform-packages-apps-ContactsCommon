/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contacts.common.list;

import android.content.Context;
import android.content.Intent;
import android.telephony.MSimTelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

import com.android.contacts.common.CallUtil;
import com.android.contacts.common.MoreContactUtils;
import com.android.contacts.common.R;
import com.android.contacts.common.list.ContactTileAdapter.ContactEntry;
import com.android.contacts.common.util.ViewUtil;
import com.android.internal.telephony.MSimConstants;

/**
 * A dark version of the {@link com.android.contacts.common.list.ContactTileView} that is used in Dialtacts
 * for frequently called contacts. Slightly different behavior from superclass...
 * when you tap it, you want to call the frequently-called number for the
 * contact, even if that is not the default number for that contact.
 */
public class ContactTilePhoneFrequentView extends ContactTileView {
    private String mPhoneNumberString;

    private View mSecondaryActionView;

    private View divider_sub1;
    private View layoutSub1;
    private ImageView callButtonSub1;
    private ImageView callIconSub1;

    private View divider_sub2;
    private View layoutSub2;
    private ImageView callButtonSub2;
    private ImageView callIconSub2;

    public ContactTilePhoneFrequentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ViewStub viewStub = ((ViewStub) findViewById(R.id.secondary_action_stub));
        mSecondaryActionView = viewStub.inflate();
        mSecondaryActionView.setVisibility(View.VISIBLE);

        divider_sub1 = mSecondaryActionView.findViewById(R.id.divider_sub1);
        divider_sub1.setBackgroundResource(R.drawable.ic_divider_dashed_holo_dark);
        layoutSub1 = mSecondaryActionView.findViewById(R.id.layout_sub1);
        callButtonSub1 = (ImageView) mSecondaryActionView.findViewById(R.id.call_button_sub1);
        callButtonSub1.setImageResource(R.drawable.ic_ab_dialer_holo_dark);
        callIconSub1 = (ImageView) mSecondaryActionView.findViewById(R.id.call_icon_sub1);

        divider_sub2 = mSecondaryActionView.findViewById(R.id.divider_sub2);
        divider_sub2.setBackgroundResource(R.drawable.ic_divider_dashed_holo_dark);
        layoutSub2 = mSecondaryActionView.findViewById(R.id.layout_sub2);
        callButtonSub2 = (ImageView) mSecondaryActionView.findViewById(R.id.call_button_sub2);
        callButtonSub2.setImageResource(R.drawable.ic_ab_dialer_holo_dark);
        callIconSub2 = (ImageView) mSecondaryActionView.findViewById(R.id.call_icon_sub2);

        MoreContactUtils.controlCallIconDisplay(mContext, layoutSub1, callButtonSub1, callIconSub1,
                layoutSub2, callButtonSub2, callIconSub2, divider_sub1, divider_sub2, -1);
        setSecondaryListener(callButtonSub1, MSimConstants.SUB1);
        setSecondaryListener(callButtonSub2, MSimConstants.SUB2);
    }

    private void setSecondaryListener(ImageView imageView, final int subscription) {
        if (MoreContactUtils.isMultiSimEnable(subscription)) {
            imageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TextUtils.isEmpty(mPhoneNumberString) && mListener != null) {
                        // Copy "superclass.createClickListener" implementation
                        mListener.onContactSelected(getLookupUri(), MoreContactUtils
                                .getTargetRectFromView(mContext,
                                        ContactTilePhoneFrequentView.this));
                    } else {
                        Intent intent = CallUtil.getCallIntent(mPhoneNumberString);
                        if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
                            intent.putExtra(MSimConstants.SUBSCRIPTION_KEY, subscription);
                            intent.putExtra(MoreContactUtils.DIAL_WIDGET_SWITCHED, subscription);
                        }
                        mContext.startActivity(intent);
                    }
                }
            });
        }
    }

    @Override
    protected boolean isDarkTheme() {
        return true;
    }

    @Override
    protected int getApproximateImageSize() {
        return ViewUtil.getConstantPreLayoutWidth(getQuickContact());
    }

    @Override
    public void loadFromContact(ContactEntry entry) {
        super.loadFromContact(entry);
        mPhoneNumberString = null; // ... in case we're reusing the view
        if (entry != null) {
            // Grab the phone-number to call directly... see {@link onClick()}
            mPhoneNumberString = entry.phoneNumber;
        }
    }

    @Override
    protected OnClickListener createClickListener() {
        // disable createClickListener, remove "onContactSelected" case to "setSecondaryListener"
        // method.
        return null;
    }
}

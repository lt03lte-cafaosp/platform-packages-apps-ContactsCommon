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
import android.provider.ContactsContract.QuickContact;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.QuickContactBadge;

import com.android.contacts.common.MoreContactUtils;
import com.android.contacts.common.R;

/**
 * Displays the contact's picture overlayed with their name
 * in a perfect square. It also has an additional touch target for a secondary action.
 */
public class ContactTilePhoneStarredView extends ContactTileView {
    private ImageButton mSecondaryButton;

    public ContactTilePhoneStarredView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSecondaryButton = (ImageButton) findViewById(R.id.contact_tile_secondary_button);
        mSecondaryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, getLookupUri());
                // Secondary target will be visible only from phone's favorite screen, then
                // we want to launch it as a separate People task.
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(MoreContactUtils.IS_FROM_DAILER, true);
                getContext().startActivity(intent);
            }
        });
    }

    @Override
    protected OnClickListener createClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener == null) {
                    return;
                }

                if (MoreContactUtils.getEnabledSimCount() > 1) {
                    QuickContact.showQuickContact(mContext, new QuickContactBadge(mContext),
                            getLookupUri(), QuickContact.MODE_LARGE, null);
                } else {
                    mListener.onContactSelected(getLookupUri(), MoreContactUtils
                            .getTargetRectFromView(mContext, ContactTilePhoneStarredView.this));
                }
            }
        };
    }

    @Override
    protected boolean isDarkTheme() {
        return true;
    }

    @Override
    protected int getApproximateImageSize() {
        // The picture is the full size of the tile (minus some padding, but we can be generous)
        return mListener.getApproximateTileWidth();
    }
}

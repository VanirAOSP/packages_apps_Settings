/*
 * Copyright 2014 Exodus
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

package com.android.settings.vanir.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.vanir.ScreenRecorderSettings;

/*
 * Screen recorder activity
 */

public class ScreenRecordActivity extends Activity implements DialogInterface.OnCancelListener {

    public ScreenRecordActivity() {
        super();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        final Context mContext = this;
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setTitle(com.android.internal.R.string.screen_recorder);
        alert.setMessage(R.string.screen_recorder_dialog_message);
        alert.setPositiveButton(mContext.getResources().getString(com.android.internal.R.string.record),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent intent = new Intent();
                intent.setAction("com.android.vanir.RECORD_SCREEN");
                mContext.sendBroadcast(intent);
                finish();
            }
        });
        alert.setNegativeButton(mContext.getResources().getString(R.string.cancel_action),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        });

        alert.show();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        dialog.dismiss();
        dialog.cancel();
        dialog = null;
        this.finish();
    }
}

/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.auth.login;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.R;

public abstract class AssistantActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_assistant);
        findViewById(R.id.next_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNextButtonClicked();
            }
        });
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackButtonClicked();
            }
        });
    }

    public void setNextButtonText(@StringRes int res) {
        setNextButtonText(getText(res));
    }

    public void setNextButtonText(CharSequence text) {
        if (text == null) {
            findViewById(R.id.next_button).setVisibility(View.GONE);
        } else {
            findViewById(R.id.next_button).setVisibility(View.VISIBLE);
            ((Button) findViewById(R.id.next_button)).setText(text);
        }
    }

    public void setBackButtonText(@StringRes int res) {
        setBackButtonText(getText(res));
    }

    public void setBackButtonText(CharSequence text) {
        if (text == null) {
            findViewById(R.id.back_button).setVisibility(View.GONE);
        } else {
            findViewById(R.id.back_button).setVisibility(View.VISIBLE);
            ((Button) findViewById(R.id.back_button)).setText(text);
        }
    }

    protected void onNextButtonClicked() {

    }

    protected void onBackButtonClicked() {

    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        ((TextView) findViewById(R.id.title)).setText(title);
    }
}

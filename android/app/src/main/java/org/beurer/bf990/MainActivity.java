package org.beurer.bf990;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView message = new TextView(this);
        message.setPadding(32, 32, 32, 32);
        message.setText("BF990 app setup: measurement reception is not implemented yet.");
        setContentView(message);
    }
}

package com.slash.batterychargelimit.settings;

import android.content.Context;
import android.preference.DialogPreference;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.slash.batterychargelimit.ControlFile;
import com.slash.batterychargelimit.R;
import com.slash.batterychargelimit.SharedMethods;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
@Keep
public class ControlFilePreference extends DialogPreference {
    private static final String TAG = ControlFilePreference.class.getSimpleName();
    private List<ControlFile> ctrlFiles = Collections.emptyList();

    public ControlFilePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPositiveButtonText(null);
        ctrlFiles = SharedMethods.getCtrlFiles(context);
    }

    @SuppressWarnings("unused")
    @Keep
    public ControlFilePreference(Context context) {
        this(context, null);
    }

    public class ControlFileAdapter extends ArrayAdapter<ControlFile> {
        private ViewHolder holder;
        private List<ControlFile> data;
        private Context context;

        private class ViewHolder {
            RadioButton label;
            TextView details;
            TextView experimental;
        }

        ControlFileAdapter(List<ControlFile> data, Context context) {
            super(context, R.layout.cf_row, data);
            this.data = data;
            this.context = context;
        }

        @Override
        public @NonNull View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final ViewHolder h;
            ControlFile cf = data.get(position);

            if (convertView == null) {
                h = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.cf_row, parent, false);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        h.label.performClick();
                    }
                });
                h.label = (RadioButton) convertView.findViewById(R.id.cf_label);
                h.label.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (v.isEnabled()) {
                            SharedMethods.setCtrlFile(getContext(), (ControlFile) v.getTag());
                            ControlFilePreference.this.getDialog().dismiss();
                        }
                    }
                });
                h.details = (TextView) convertView.findViewById(R.id.cf_details);
                h.experimental = (TextView) convertView.findViewById(R.id.cf_experimental);
                convertView.setTag(h);
            } else {
                h = (ViewHolder) convertView.getTag();
            }

            h.label.setEnabled(cf.isValid());
            h.label.setText(cf.getLabel());
            h.label.setTag(cf);
            h.label.setChecked(cf.getFile().equals(getPersistedString(null)));
            h.details.setText(cf.getDetails());
            h.experimental.setVisibility(cf.isExperimental() ? View.VISIBLE : View.INVISIBLE);

            return convertView;
        }
    }

    @Override
    protected View onCreateDialogView() {
        ListView v = new ListView(getContext());
        v.setAdapter(new ControlFileAdapter(ctrlFiles, getContext()));
        return v;
    }
}
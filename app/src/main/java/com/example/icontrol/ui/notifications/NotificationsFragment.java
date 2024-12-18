package com.example.icontrol.ui.notifications;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.icontrol.R;
public class NotificationsFragment extends Fragment{
    public Button notifications_button_github;
    public Button notifications_button_license;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView=inflater.inflate(R.layout.fragment_notifications,container,false );
        notifications_button_github = rootView.findViewById(R.id.notifications_button_github);
        notifications_button_license = rootView.findViewById(R.id.notifications_button_license);
        notifications_button_github.setOnClickListener(v -> {
            Intent intent=new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/MingchenDai/iControl"));
            startActivity(intent);
        });
        notifications_button_license.setOnClickListener(v -> {
            Intent intent=new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.gnu.org/licenses/gpl-3.0.en.html#license-text"));
            startActivity(intent);
        });
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
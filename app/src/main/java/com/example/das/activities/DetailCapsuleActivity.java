package com.example.das.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.example.das.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.appbar.AppBarLayout;
import java.util.ArrayList;
import java.util.List;

public class DetailCapsuleActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private List<String> imagenes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_capsule);

        // Obtener datos del Intent
        imagenes = getIntent().getStringArrayListExtra("imagenes");

        setupToolbar();
        setupViewPager();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);
        ImagePagerAdapter adapter = new ImagePagerAdapter(this, imagenes);
        viewPager.setAdapter(adapter);
    }

    public static class ImagePagerAdapter extends FragmentStateAdapter {

        private final List<String> imagenes;

        public ImagePagerAdapter(@NonNull FragmentActivity fragmentActivity, List<String> imagenes) {
            super(fragmentActivity);
            this.imagenes = imagenes;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ImageFragment.newInstance(imagenes.get(position));
        }

        @Override
        public int getItemCount() {
            return imagenes.size();
        }
    }

    public static class ImageFragment extends Fragment {

        private String imageUrl;

        public static ImageFragment newInstance(String imageUrl) {
            ImageFragment fragment = new ImageFragment();
            Bundle args = new Bundle();
            args.putString("image_url", imageUrl);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_image, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            ImageView imageView = view.findViewById(R.id.full_image);

            if (getArguments() != null) {
                imageUrl = getArguments().getString("image_url");
                Glide.with(this)
                        .load(Uri.parse(imageUrl))
                        .into(imageView);
            }
        }
    }
}
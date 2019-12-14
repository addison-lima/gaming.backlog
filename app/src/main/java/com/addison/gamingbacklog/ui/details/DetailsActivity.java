package com.addison.gamingbacklog.ui.details;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.addison.gamingbacklog.GamingBacklogApplication;
import com.addison.gamingbacklog.databinding.ActivityDetailsBinding;
import com.addison.gamingbacklog.repository.Repository;
import com.addison.gamingbacklog.repository.database.GameEntry;
import com.addison.gamingbacklog.repository.service.RequestStatus;
import com.addison.gamingbacklog.repository.service.models.Video;
import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.View;
import android.widget.Toast;

import com.addison.gamingbacklog.R;

import java.util.Calendar;
import java.util.List;

public class DetailsActivity extends AppCompatActivity implements VideosAdapter.VideosAdapterOnClickHandler {

    public static final String EXTRA_GAME = "game";

    private ActivityDetailsBinding mActivityDetailsBinding;
    private VideosAdapter mVideosAdapter;

    private Tracker mTracker;
    private GameEntry mGameEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivityDetailsBinding = DataBindingUtil.setContentView(this, R.layout.activity_details);
        mActivityDetailsBinding.setLifecycleOwner(this);

        GameUi gameUi = null;

        if (getIntent().hasExtra(EXTRA_GAME)) {
            gameUi = getIntent().getParcelableExtra(EXTRA_GAME);
        }

        if (gameUi != null) {
            populateUi(gameUi);
        } else {
            Toast.makeText(this, getString(R.string.details_error),
                    Toast.LENGTH_LONG).show();
            finish();
        }

        mActivityDetailsBinding.fab.hide();
        mActivityDetailsBinding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGameEntry != null) {
                    //TODO remove from Library
                } else {
                    //TODO add to Library
                }
            }
        });

        mVideosAdapter = new VideosAdapter(this, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mActivityDetailsBinding.contentDetails.rvVideos.setLayoutManager(linearLayoutManager);
        mActivityDetailsBinding.contentDetails.rvVideos.setAdapter(mVideosAdapter);

        initializeTracker();

        initializeAds();
    }

    @Override
    public void onResume() {
        super.onResume();
        trackScreen("onResume Details");
    }

    @Override
    public void onClick(Video video) {
        try {
            Intent appIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(video.getAppIntent()));
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(video.getWebIntent()));
            startActivity(webIntent);
        }
    }

    private void initializeTracker() {
        GamingBacklogApplication application = (GamingBacklogApplication) getApplication();
        mTracker = application.getDefaultTracker();
    }

    private void trackScreen(String screenName) {
        if (mTracker != null) {
            mTracker.setScreenName(screenName);
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    private void initializeAds() {
        MobileAds.initialize(this);
        AdRequest adRequest = new AdRequest.Builder().build();
        mActivityDetailsBinding.contentDetails.adView.loadAd(adRequest);
    }

    private void populateUi(GameUi gameUi) {
        if (gameUi.getCoverUrl() != null) {
            Glide.with(this.getApplicationContext())
                    .load(gameUi.getCoverUrl())
                    .centerCrop()
                    .into(mActivityDetailsBinding.contentDetails.ivGameCover);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(gameUi.getFirstReleaseDate() * 1000);
        mActivityDetailsBinding.contentDetails.tvGameName.setText(
                gameUi.getName());
        mActivityDetailsBinding.contentDetails.tvGameRelease.setText(
                String.valueOf(calendar.get(Calendar.YEAR)));
        mActivityDetailsBinding.contentDetails.tvGameSummary.setText(
                gameUi.getSummary());

        retrieveGameFromLibraryAndVideos(gameUi.getId());
    }

    private void retrieveGameFromLibraryAndVideos(Integer gameId) {
        Repository repository = Repository.getInstance(getApplicationContext());
        repository.retrieveGameVideos(gameId);
        repository.getRequestVideosStatus().observe(this, getRequestVideosStatusObserver());
        repository.getGameVideosList().observe(this, getGameVideosListObserver());
        repository.getGame(gameId).observe(this, getGameObserver());
    }

    private Observer<RequestStatus> getRequestVideosStatusObserver() {
        return new Observer<RequestStatus>() {
            @Override
            public void onChanged(RequestStatus requestStatus) {
                if (requestStatus != null) {
                    updateUi(requestStatus.getRequestState());
                }
            }
        };
    }

    private Observer<List<Video>> getGameVideosListObserver() {
        return new Observer<List<Video>>() {
            @Override
            public void onChanged(List<Video> videos) {
                mVideosAdapter.setData(videos);
            }
        };
    }

    private Observer<GameEntry> getGameObserver() {
        return new Observer<GameEntry>() {
            @Override
            public void onChanged(GameEntry gameEntry) {
                mGameEntry = gameEntry;
                mActivityDetailsBinding.fab.show();
            }
        };
    }

    private void updateUi(RequestStatus.RequestState requestState) {
        mActivityDetailsBinding.contentDetails.tvVideos
                .setVisibility(requestState.equals(RequestStatus.RequestState.FAILURE)
                        ? View.VISIBLE : View.INVISIBLE);
        mActivityDetailsBinding.contentDetails.pbLoadingIndicator
                .setVisibility(requestState.equals(RequestStatus.RequestState.LOADING)
                        ? View.VISIBLE : View.INVISIBLE);
        mActivityDetailsBinding.contentDetails.rvVideos
                .setVisibility(requestState.equals(RequestStatus.RequestState.SUCCESS)
                        ? View.VISIBLE : View.INVISIBLE);
    }
}

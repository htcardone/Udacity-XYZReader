package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetail-Fragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;

    //private View mPhotoContainerView;
    private AppBarLayout mAppBar;
    private CollapsingToolbarLayout mCollapsingToolbar;
    private Toolbar mToolbar;
    private ImageView mPhotoView;
    private View mMetaBarView;
    private TextView mTitleView;
    private TextView mBylineView;
    private RecyclerView mRecyclerView;
    private FloatingActionButton mFloatingActionButton;

    private BodyAdapter mAdapter;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        //Log.d(TAG, "onActivityCreated");
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mAppBar = mRootView.findViewById(R.id.appbar);
        mMetaBarView = mRootView.findViewById(R.id.meta_bar);
        mTitleView = mRootView.findViewById(R.id.article_title);
        mBylineView = mRootView.findViewById(R.id.article_byline);
        mRecyclerView = mRootView.findViewById(R.id.article_recycler_view);
        mToolbar = mRootView.findViewById(R.id.toolbar);
        mCollapsingToolbar = mRootView.findViewById(R.id.collapsing_toolbar);
        mPhotoView = mRootView.findViewById(R.id.photo);
        mFloatingActionButton = mRootView.findViewById(R.id.share_fab);

        getActivityCast().setSupportActionBar(mToolbar);
        ActionBar actionBar = getActivityCast().getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPhotoView.setTransitionName(getString(R.string.transition_photo) + "_" + mItemId);
        }

        mAdapter = new BodyAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));

        // Hiding FAB when the Toolbar is not visible
        mAppBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                verticalOffset = -1 * verticalOffset;
                int difference = appBarLayout.getTotalScrollRange() - mToolbar.getHeight();

                if (verticalOffset == difference) {
                    ViewCompat.animate(mFloatingActionButton).scaleX(1).scaleY(1).start();
                }
                else if (verticalOffset > difference) {
                    ViewCompat.animate(mFloatingActionButton).scaleX(0).scaleY(0).start();
                }
            }
        });

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();
        return mRootView;
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        mBylineView.setMovementMethod(new LinkMovementMethod());

        if (mCursor != null) {
            Log.d(TAG, "bindViews " + mCursor.getString(ArticleLoader.Query.TITLE));
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);

            String title = mCursor.getString(ArticleLoader.Query.TITLE);
            mTitleView.setText(title);
            mCollapsingToolbar.setTitle(title);

            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                mBylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                mBylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }

            List<String> lines = Arrays.asList(mCursor
                    .getString(ArticleLoader.Query.BODY)
                    .replaceAll("(\r\n\r\n)", "<br />")
                    .replaceAll("(\r\n)", " ")
                    .split("<br />"));

            mAdapter.replaceData(lines);

            Picasso.with(getActivity())
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .placeholder(R.drawable.empty_detail)
                    .into(mPhotoView, new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap bitmap = ((BitmapDrawable) mPhotoView.getDrawable()).getBitmap();
                            Palette p = Palette.generate(bitmap, 12);
                            int mutedColor = p.getMutedColor(getResources().getColor(R.color.theme_primary));
                            int darkMutedColor = p.getDarkMutedColor(getResources().getColor(R.color.theme_primary_dark));

                            mMetaBarView.setBackgroundColor(mutedColor);
                            mToolbar.setBackgroundColor(mutedColor);
                            mCollapsingToolbar.setContentScrimColor(mutedColor);
                            mCollapsingToolbar.setStatusBarScrimColor(darkMutedColor);
                            mFloatingActionButton.setBackgroundTintList(ColorStateList.valueOf(darkMutedColor));
                        }

                        @Override
                        public void onError() {

                        }
                    });

        } else {
            mRootView.setVisibility(View.INVISIBLE);
            mTitleView.setText("N/A");
            mBylineView.setText("N/A" );
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewLine;

        public ViewHolder(View view) {
            super(view);
            textViewLine = view.findViewById(R.id.article_line);
        }
    }

    private class BodyAdapter extends RecyclerView.Adapter<ViewHolder> {
        private List<String> dataset;

        public BodyAdapter() {
            this.dataset = new ArrayList<>();
            dataset.add("N/A");
        }

        public void replaceData(List<String> dataset) {
            this.dataset = dataset;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivityCast()
                    .getLayoutInflater()
                    .inflate(R.layout.line_item_detail, parent, false);
            final ViewHolder viewHolder = new ViewHolder(view);

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.textViewLine.setText(Html.fromHtml(dataset.get(position)));
        }

        @Override
        public int getItemCount() {
            return dataset.size();
        }
    }
}

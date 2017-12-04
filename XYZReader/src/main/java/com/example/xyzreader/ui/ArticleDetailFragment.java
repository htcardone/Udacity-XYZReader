package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    //private ObservableScrollView mScrollView;
    //private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ColorDrawable mStatusBarColorDrawable;

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

    private int mTopInset;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;

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

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
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
        mRootView = inflater.inflate(R.layout.fragment_article_detail_temp, container, false);

        mAppBar = mRootView.findViewById(R.id.appbar);
        mMetaBarView = mRootView.findViewById(R.id.meta_bar);
        mTitleView = mRootView.findViewById(R.id.article_title);
        mBylineView = mRootView.findViewById(R.id.article_byline);
        mRecyclerView = mRootView.findViewById(R.id.article_recycler_view);
        mToolbar = mRootView.findViewById(R.id.toolbar);
        mCollapsingToolbar = mRootView.findViewById(R.id.collapsing_toolbar);
        mPhotoView = mRootView.findViewById(R.id.photo);
        mFloatingActionButton = mRootView.findViewById(R.id.share_fab);

        //mPhotoContainerView = mRootView.findViewById(R.id.photo_container);
        /*mDrawInsetsFrameLayout = (DrawInsetsFrameLayout)
                mRootView.findViewById(R.id.draw_insets_frame_layout);
        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                mTopInset = insets.top;
            }
        });*/

        /*mScrollView = (ObservableScrollView) mRootView.findViewById(R.id.scrollview);
        mScrollView.setCallbacks(new ObservableScrollView.Callbacks() {
            @Override
            public void onScrollChanged() {
                mScrollY = mScrollView.getScrollY();
                getActivityCast().onUpButtonFloorChanged(mItemId, ArticleDetailFragment.this);
                //mPhotoContainerView.setTranslationY((int) (mScrollY - mScrollY / PARALLAX_FACTOR));
                updateStatusBar();
            }
        });*/

        getActivityCast().setSupportActionBar(mToolbar);
        ActionBar actionBar = getActivityCast().getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("");
        }

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


        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));

        mStatusBarColorDrawable = new ColorDrawable(0);

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
        updateStatusBar();
        return mRootView;
    }

    private void updateStatusBar() {
        /*int color = 0;
        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mDarkMutedColor) * 0.9),
                    (int) (Color.green(mDarkMutedColor) * 0.9),
                    (int) (Color.blue(mDarkMutedColor) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);*/
        //mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
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


        //mBylineView.setMovementMethod(new LinkMovementMethod());

        //mBodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
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

            mRecyclerView.setAdapter(new BodyAdapter(lines));

            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.generate(bitmap, 12);
                                int mutedColor = p.getMutedColor(getResources().getColor(R.color.theme_primary));
                                int darkMutedColor = p.getDarkMutedColor(getResources().getColor(R.color.theme_primary_dark));
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                mMetaBarView.setBackgroundColor(mutedColor);
                                mToolbar.setBackgroundColor(mutedColor);
                                mCollapsingToolbar.setContentScrimColor(mutedColor);
                                mCollapsingToolbar.setStatusBarScrimColor(darkMutedColor);
                                mFloatingActionButton.setBackgroundTintList(ColorStateList.valueOf(darkMutedColor));
                                updateStatusBar();
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });

            //Log.d(TAG, "mBodyView setText");
            //mBodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")));
        } else {
            mRootView.setVisibility(View.GONE);
            //mTitleView.setText("N/A");
            //mBylineView.setText("N/A" );
            //mBodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(TAG, "onCreateLoader");
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished");
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
        Log.d(TAG, "onLoaderReset");
        mCursor = null;
        bindViews();
    }





    /*public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }*/

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewLine;

        public ViewHolder(View view) {
            super(view);
            textViewLine = view.findViewById(R.id.article_line);
        }
    }

    private class BodyAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final List<String> dataset;

        public BodyAdapter(List<String> lines) {
            dataset = lines;
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
            Log.d("BodyAdapter", dataset.get(position));
            holder.textViewLine.setText(Html.fromHtml(dataset.get(position)));
        }

        @Override
        public int getItemCount() {
            return dataset.size();
        }
    }
}

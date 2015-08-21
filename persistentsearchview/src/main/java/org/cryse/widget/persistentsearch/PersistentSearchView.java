package org.cryse.widget.persistentsearch;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

@SuppressWarnings("unused")
public class PersistentSearchView extends RevealViewGroup {
    public static final int VOICE_RECOGNITION_CODE = 8185102;
    final static double COS_45 = Math.cos(Math.toRadians(45));
    private static final int[] RES_IDS_ACTION_BAR_SIZE = { R.attr.actionBarSize };
    private static final int DURATION_REVEAL_OPEN = 400;
    private static final int DURATION_REVEAL_CLOSE = 300;
    private static final int DURATION_HOME_BUTTON = 300;
    private static final int DURATION_LAYOUT_TRANSITION = 100;
    private HomeButton.IconState mHomeButtonCloseIconState;
    private HomeButton.IconState mHomeButtonOpenIconState;
    private HomeButton.IconState mHomeButtonSearchIconState;
    private SearchViewState mCurrentState;
    private SearchViewState mLastState;
    private DisplayMode mDisplayMode;
    private int mCardVerticalPadding;
    private int mCardHorizontalPadding;
    private int mCardHeight;
    private int mCustomToolbarHeight;
    private int mSearchCardElevation;
    private int mFromX, mFromY;
    // Views
    private LogoView mLogoView;
    private CardView mSearchCardView;
    private HomeButton mHomeButton;
    private EditText mSearchEditText;
    private ListView mSuggestionListView;
    private ImageView mMicButton;
    private SearchListener mSearchListener;
    private HomeButtonListener mHomeButtonListener;
    private FrameLayout mRootLayout;
    private VoiceRecognitionDelegate mVoiceRecognitionDelegate;
    private boolean mAvoidTriggerTextWatcher;
    private boolean mIsMic;
    private int mSearchTextColor;
    private int mArrorButtonColor;
    private Drawable mLogoDrawable;
    private int mSearchEditTextColor;
    private String mSearchEditTextHint;
    private int mSearchEditTextHintColor;
    private SearchSuggestionsBuilder mSuggestionBuilder;
    private SearchItemAdapter mSearchItemAdapter;
    private ArrayList<SearchItem> mSearchSuggestions;
    public PersistentSearchView(Context context) {
        super(context);
        init(null);
    }
    public PersistentSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public PersistentSearchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    static float calculateVerticalPadding(CardView cardView) {
        float maxShadowSize = cardView.getMaxCardElevation();
        float cornerRadius = cardView.getRadius();
        boolean addPaddingForCorners = cardView.getPreventCornerOverlap();

        if (addPaddingForCorners) {
            return (float) (maxShadowSize * 1.5f + (1 - COS_45) * cornerRadius);
        } else {
            return maxShadowSize * 1.5f;
        }
    }

    static float calculateHorizontalPadding(CardView cardView) {
        float maxShadowSize = cardView.getMaxCardElevation();
        float cornerRadius = cardView.getRadius();
        boolean addPaddingForCorners = cardView.getPreventCornerOverlap();
        if (addPaddingForCorners) {
            return (float) (maxShadowSize + (1 - COS_45) * cornerRadius);
        } else {
            return maxShadowSize;
        }
    }

    /** Calculates the Toolbar height in pixels. */
    static int calculateToolbarSize(Context context) {
        if (context == null) {
            return 0;
        }

        Resources.Theme curTheme = context.getTheme();
        if (curTheme == null) {
            return 0;
        }

        TypedArray att = curTheme.obtainStyledAttributes(RES_IDS_ACTION_BAR_SIZE);
        if (att == null) {
            return 0;
        }

        float size = att.getDimension(0, 0);
        att.recycle();
        return (int)size;
    }

    private void init(AttributeSet attrs) {
        LayoutInflater.from(getContext()).inflate(R.layout.layout_searchview, this, true);
        if (attrs != null) {
            TypedArray attrsValue = getContext().obtainStyledAttributes(attrs,
                    R.styleable.PersistentSearchView);
            mDisplayMode = DisplayMode.fromInt(attrsValue.getInt(R.styleable.PersistentSearchView_persistentSV_displayMode, DisplayMode.MENUITEM.toInt()));
            mSearchCardElevation = attrsValue.getDimensionPixelSize(R.styleable.PersistentSearchView_persistentSV_searchCardElevation, -1);
            mSearchTextColor = attrsValue.getColor(R.styleable.PersistentSearchView_persistentSV_searchTextColor, Color.BLACK);
            mLogoDrawable = attrsValue.getDrawable(R.styleable.PersistentSearchView_persistentSV_logoDrawable);
            mSearchEditTextColor = attrsValue.getColor(R.styleable.PersistentSearchView_persistentSV_editTextColor, Color.BLACK);
            mSearchEditTextHint = attrsValue.getString(R.styleable.PersistentSearchView_persistentSV_editHintText);
            mSearchEditTextHintColor = attrsValue.getColor(R.styleable.PersistentSearchView_persistentSV_editHintTextColor, Color.BLACK);
            mArrorButtonColor = attrsValue.getColor(R.styleable.PersistentSearchView_persistentSV_homeButtonColor, Color.BLACK);
            mCustomToolbarHeight = attrsValue.getDimensionPixelSize(R.styleable.PersistentSearchView_persistentSV_customToolbarHeight, calculateToolbarSize(getContext()));
            attrsValue.recycle();
        }

        if (mSearchCardElevation < 0) {
            mSearchCardElevation = getContext().getResources().getDimensionPixelSize(R.dimen.search_card_default_card_elevation);
        }

        mCardHeight = getResources().getDimensionPixelSize(R.dimen.search_card_height);
        mCardVerticalPadding = (mCustomToolbarHeight - mCardHeight) / 2;

        switch (mDisplayMode) {
            case MENUITEM:
            default:
                mCardHorizontalPadding = getResources().getDimensionPixelSize(R.dimen.search_card_visible_padding_menu_item_mode);
                if(mCardVerticalPadding > mCardHorizontalPadding)
                    mCardHorizontalPadding = mCardVerticalPadding;
                mHomeButtonCloseIconState = HomeButton.IconState.ARROW;
                mHomeButtonOpenIconState = HomeButton.IconState.ARROW;
                setCurrentState(SearchViewState.MENUITEM);
                break;
            case TOOLBAR_DRAWER:
                mHomeButtonCloseIconState = HomeButton.IconState.BURGER;
                mHomeButtonOpenIconState = HomeButton.IconState.ARROW;
                mCardHorizontalPadding = getResources().getDimensionPixelSize(R.dimen.search_card_visible_padding_toolbar_mode);
                setCurrentState(SearchViewState.TOOLBAR_DRAWER);
                break;
            case TOOLBAR_BACKARROW:
                mHomeButtonCloseIconState = HomeButton.IconState.ARROW;
                mHomeButtonOpenIconState = HomeButton.IconState.ARROW;
                mCardHorizontalPadding = getResources().getDimensionPixelSize(R.dimen.search_card_visible_padding_toolbar_mode);
                setCurrentState(SearchViewState.TOOLBAR_BACKARROW);
                break;
        }
        mHomeButtonSearchIconState = HomeButton.IconState.ARROW;

        bindViews();
        setValuesToViews();

        this.mIsMic = true;
        mSearchSuggestions = new ArrayList<>();
        mSearchItemAdapter = new SearchItemAdapter(getContext(), mSearchSuggestions);
        mSuggestionListView.setAdapter(mSearchItemAdapter);

        setUpLayoutTransition();
        setUpListeners();
    }

    private void bindViews() {
        this.mSearchCardView = (CardView) findViewById(R.id.cardview_search);
        this.mHomeButton = (HomeButton) findViewById(R.id.button_home);
        this.mLogoView = (LogoView) findViewById(R.id.logoview);
        this.mSearchEditText = (EditText) findViewById(R.id.edittext_search);
        this.mSuggestionListView = (ListView) findViewById(R.id.listview_suggestions);
        this.mMicButton = (ImageView) findViewById(R.id.button_mic);
    }

    private void setValuesToViews() {
        this.mSearchCardView.setCardElevation(mSearchCardElevation);
        this.mSearchCardView.setMaxCardElevation(mSearchCardElevation);
        this.mHomeButton.setArrowDrawableColor(mArrorButtonColor);
        this.mHomeButton.setState(mHomeButtonCloseIconState);
        this.mHomeButton.setAnimationDuration(DURATION_HOME_BUTTON);
        this.mSearchEditText.setTextColor(mSearchEditTextColor);
        this.mSearchEditText.setHint(mSearchEditTextHint);
        this.mSearchEditText.setHintTextColor(mSearchEditTextHintColor);
        if (mLogoDrawable != null) {
            this.mLogoView.setLogo(mLogoDrawable);
        }
        this.mLogoView.setTextColor(mSearchTextColor);
    }

    private void setUpListeners() {
        mHomeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentState == SearchViewState.EDITING) {
                    cancelEditing();
                } else if (mCurrentState == SearchViewState.SEARCH && mDisplayMode == DisplayMode.TOOLBAR_BACKARROW) {
                    if (mHomeButtonListener != null)
                        mHomeButtonListener.onHomeButtonClick();
                } else if (mCurrentState == SearchViewState.SEARCH) {
                    stateFromSearchToNormal();
                } else {
                    if (mHomeButtonListener != null)
                        mHomeButtonListener.onHomeButtonClick();
                }
            }

        });
        mLogoView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mCurrentState == SearchViewState.TOOLBAR_DRAWER) {
                    stateFromToolbarToEditing();
                } else if (mCurrentState == SearchViewState.TOOLBAR_BACKARROW) {
                    stateFromToolbarToEditing();
                } else if (mCurrentState == SearchViewState.SEARCH) {
                    stateFromSearchToEditing();
                }
            }

        });
        mSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    clearSuggestions();
                    search();
                    return true;
                }
                return false;
            }
        });
        mSearchEditText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    clearSuggestions();
                    search();
                    return true;
                }
                return false;
            }
        });
        micStateChanged();
        mMicButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                micClick();
            }
        });
        mSearchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if (!mAvoidTriggerTextWatcher) {
                    if (s.length() > 0) {
                        micStateChanged(false);
                        mMicButton.setImageDrawable(
                                ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_clear_black, null)
                        );
                        buildSearchSuggestions(getSearchText());
                    } else {
                        micStateChanged(true);
                        mMicButton.setImageDrawable(
                                ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_mic_black, null)
                        );
                        buildEmptySearchSuggestions();
                    }
                }
                if (mSearchListener != null)
                    mSearchListener.onSearchTermChanged(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

            }

        });

    }

    private void setUpLayoutTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            RelativeLayout searchRoot = (RelativeLayout) findViewById(R.id.search_root);
            LayoutTransition layoutTransition = new LayoutTransition();
            layoutTransition.setDuration(DURATION_LAYOUT_TRANSITION);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                // layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
                layoutTransition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
                layoutTransition.setStartDelay(LayoutTransition.CHANGING, 0);
            }
            layoutTransition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
            mSearchCardView.setLayoutTransition(layoutTransition);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int totalHeight = 0;
        int searchCardWidth;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                if (i == 0 && child instanceof CardView) {
                    CardView searchCard = (CardView) child;
                    int horizontalPadding = (int) Math.ceil(calculateHorizontalPadding(searchCard));
                    int verticalPadding = (int) Math.ceil(calculateVerticalPadding(searchCard));
                    // searchCardWidth = widthSize - 2 * mCardVisiblePadding + horizontalPadding * 2;
                    int searchCardLeft = mCardHorizontalPadding - horizontalPadding;
                    // searchCardTop = mCardVisiblePadding - verticalPadding;
                    searchCardWidth = widthSize - searchCardLeft * 2;
                    int cardWidthSpec = MeasureSpec.makeMeasureSpec(searchCardWidth, MeasureSpec.EXACTLY);
                    // int cardHeightSpec = MeasureSpec.makeMeasureSpec(searchCardHeight, MeasureSpec.EXACTLY);
                    measureChild(child, cardWidthSpec, heightMeasureSpec);
                    int childMeasuredHeight = child.getMeasuredHeight();
                    int childMeasuredWidth = child.getMeasuredWidth();
                    int childHeight = childMeasuredHeight - verticalPadding * 2;
                    totalHeight = totalHeight + childHeight + mCardVerticalPadding * 2;
                }
            }
        }
        setMeasuredDimension(widthSize, totalHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        int searchViewWidth = r - l;
        int searchViewHeight = b - t;
        int searchCardLeft;
        int searchCardTop;
        int searchCardRight;
        int searchCardBottom;
        int searchCardWidth;
        int searchCardHeight;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (i == 0 && child instanceof CardView) {
                CardView searchCard = (CardView) child;
                int horizontalPadding = (int) Math.ceil(calculateHorizontalPadding(searchCard));
                int verticalPadding = (int) Math.ceil(calculateVerticalPadding(searchCard));
                searchCardLeft = mCardHorizontalPadding - horizontalPadding;
                searchCardTop = mCardVerticalPadding - verticalPadding;
                searchCardWidth = searchViewWidth - searchCardLeft * 2;
                searchCardHeight = child.getMeasuredHeight();
                searchCardRight = searchCardLeft + searchCardWidth;
                searchCardBottom = searchCardTop + searchCardHeight;
                child.layout(searchCardLeft, searchCardTop, searchCardRight, searchCardBottom);
            }
        }
    }

    private void revealFromMenuItem(View menuItemView, int desireRevealWidth) {
        setVisibility(View.VISIBLE);
        if (menuItemView != null) {
            int[] location = new int[2];
            menuItemView.getLocationInWindow(location);
            int menuItemWidth = menuItemView.getWidth();
            this.mFromX = location[0] + menuItemWidth / 2;
            this.mFromY = location[1];
            revealFrom(mFromX, mFromY, desireRevealWidth);
        }
    }

    private void hideCircularlyToMenuItem() {
        if (mFromX == 0 || mFromY == 0) {
            mFromX = getRight();
            mFromY = getTop();
        }
        hideCircularly(mFromX, mFromY);
    }

    /***
     * Hide the PersistentSearchView using the circle animation. Can be called regardless of result list length
     */
    private void hideCircularly(int x, int y) {

        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 96,
                r.getDisplayMetrics());
        int finalRadius = (int) Math.max(this.getMeasuredWidth() * 1.5, px);

        SupportAnimator animator = ViewAnimationUtils.createCircularReveal(
                mSearchCardView, x, y, 0, finalRadius);
        animator = animator.reverse();
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(DURATION_REVEAL_CLOSE);
        animator.start();
        animator.addListener(new SupportAnimator.AnimatorListener() {

            @Override
            public void onAnimationStart() {

            }

            @Override
            public void onAnimationEnd() {
                setVisibility(View.GONE);
                closeSearchInternal();
                // closeSearch();
            }

            @Override
            public void onAnimationCancel() {

            }

            @Override
            public void onAnimationRepeat() {

            }

        });
    }

    private void hideCircularly() {
        hideCircularly(getLeft() + getRight(), getTop());
    }

    public boolean getSearchOpen() {
        return getVisibility() == VISIBLE && (mCurrentState == SearchViewState.SEARCH || mCurrentState == SearchViewState.EDITING);
    }

    /***
     * Hide the search suggestions manually
     */
    public void hideSuggestions() {
        this.mSearchEditText.setVisibility(View.GONE);
        this.mSuggestionListView.setVisibility(View.GONE);
    }

    private boolean isMicEnabled() {
        return mVoiceRecognitionDelegate != null;
    }

    private void micStateChanged() {
        mMicButton.setVisibility((!mIsMic || isMicEnabled()) ? VISIBLE : INVISIBLE);
    }

    private void micStateChanged(boolean isMic) {
        this.mIsMic = isMic;
        micStateChanged();
    }

    /***
     * Mandatory method for the onClick event
     */
    public void micClick() {
        if (!mIsMic) {
            setSearchString("", false);
        } else {
            if (mVoiceRecognitionDelegate != null)
                mVoiceRecognitionDelegate.onStartVoiceRecognition();
        }
    }

    /***
     * Populate the PersistentSearchView with words, in an ArrayList. Used by the voice input
     *
     * @param matches Matches
     */
    public void populateEditText(ArrayList<String> matches) {
        String text = matches.get(0).trim();
        populateEditText(text);
    }

    /***
     * Populate the PersistentSearchView with search query
     *
     * @param query Matches
     */
    public void populateEditText(String query) {
        String text = query.trim();
        setSearchString(text, true);
        search();
    }

    /***
     * Set whether the menu button should be shown. Particularly useful for apps that adapt to screen sizes
     *
     * @param visibility Whether to show
     */

    public void setHomeButtonVisibility(int visibility) {
        this.mHomeButton.setVisibility(visibility);
    }

    /***
     * Set the menu listener
     *
     * @param homeButtonListener MenuListener
     */
    public void setHomeButtonListener(HomeButtonListener homeButtonListener) {
        this.mHomeButtonListener = homeButtonListener;
    }

    /***
     * Set the search listener
     *
     * @param listener SearchListener
     */
    public void setSearchListener(SearchListener listener) {
        this.mSearchListener = listener;
    }

    /***
     * Set the text color of the logo
     *
     * @param color logo text color
     */
    public void setLogoTextColor(int color) {
        mLogoView.setTextColor(color);
    }

    /***
     * Get the PersistentSearchView's current text
     *
     * @return Text
     */
    public String getSearchText() {
        return mSearchEditText.getText().toString();
    }

    public void clearSuggestions() {
        mSearchItemAdapter.clear();
    }

    /***
     * Set the PersistentSearchView's current text manually
     *
     * @param text                    Text
     * @param avoidTriggerTextWatcher avoid trigger TextWatcher(TextChangedListener)
     */
    public void setSearchString(String text, boolean avoidTriggerTextWatcher) {
        if (avoidTriggerTextWatcher)
            mAvoidTriggerTextWatcher = true;
        mSearchEditText.setText("");
        mSearchEditText.append(text);
        mAvoidTriggerTextWatcher = false;
    }

    private void buildEmptySearchSuggestions() {
        if (mSuggestionBuilder != null) {
            mSearchSuggestions.clear();
            Collection<SearchItem> suggestions = mSuggestionBuilder.buildEmptySearchSuggestion(10);
            if (suggestions != null && suggestions.size() > 0) {
                mSearchSuggestions.addAll(suggestions);
            }
            mSearchItemAdapter.notifyDataSetChanged();
        }
    }

    private void buildSearchSuggestions(String query) {
        if (mSuggestionBuilder != null) {
            mSearchSuggestions.clear();
            Collection<SearchItem> suggestions = mSuggestionBuilder.buildSearchSuggestion(10, query);
            if (suggestions != null && suggestions.size() > 0) {
                mSearchSuggestions.addAll(suggestions);
            }
            mSearchItemAdapter.notifyDataSetChanged();
        }
    }

    private void revealFrom(float x, float y, int desireRevealWidth) {
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 96,
                r.getDisplayMetrics());

        int measuredHeight = getMeasuredWidth();
        int finalRadius = (int) Math.max(Math.max(measuredHeight, px), desireRevealWidth);

        SupportAnimator animator = ViewAnimationUtils.createCircularReveal(
                mSearchCardView, (int) x, (int) y, 0, finalRadius);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(DURATION_REVEAL_OPEN);
        animator.addListener(new SupportAnimator.AnimatorListener() {

            @Override
            public void onAnimationCancel() {

            }

            @Override
            public void onAnimationEnd() {
                // show search view here
                openSearchInternal(true);
            }

            @Override
            public void onAnimationRepeat() {

            }

            @Override
            public void onAnimationStart() {

            }

        });
        animator.start();
    }

    private void search() {
        String searchTerm = getSearchText();
        if (!TextUtils.isEmpty(searchTerm)) {
            setLogoTextInt(searchTerm);
            stateFromEditingToSearch();
            if (mSearchListener != null)
                mSearchListener.onSearch(searchTerm);
        } else {
            stateFromEditingToNormal();
        }
    }

    private void openSearchInternal(Boolean openKeyboard) {
        this.mHomeButton.animateState(mHomeButtonOpenIconState);
        this.mLogoView.setVisibility(View.GONE);
        this.mSearchEditText.setVisibility(View.VISIBLE);
        mSearchEditText.requestFocus();
        this.mSuggestionListView.setVisibility(View.VISIBLE);
        mSuggestionListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                SearchItem result = mSearchSuggestions.get(arg2);
                setSearchString(result.getValue(), true);
                search();
            }

        });
        String currentSearchText = getSearchText();
        if (currentSearchText.length() > 0) {
            buildSearchSuggestions(currentSearchText);
        } else {
            buildEmptySearchSuggestions();
        }

        if (mSearchListener != null)
            mSearchListener.onSearchEditOpened();
        if (getSearchText().length() > 0) {
            micStateChanged(false);
            mMicButton.setImageDrawable(
                    ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_clear_black, null));
        }
        if (openKeyboard) {
            InputMethodManager inputMethodManager = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInputFromWindow(
                    getApplicationWindowToken(),
                    InputMethodManager.SHOW_FORCED, 0);
        }
    }

    private void closeSearchInternal() {
        if (mCurrentState == SearchViewState.SEARCH) {
            this.mHomeButton.animateState(mHomeButtonSearchIconState);
        } else {
            this.mHomeButton.animateState(mHomeButtonCloseIconState);
        }
        this.mLogoView.setVisibility(View.VISIBLE);
        this.mSearchEditText.setVisibility(View.GONE);
        // if(mDisplayMode == DISPLAY_MODE_AS_TOOLBAR) {
        mSuggestionListView.setVisibility(View.GONE);
        // }
        // this.mSuggestionListView.setVisibility(View.GONE);
        /*if (mTintView != null && mRootLayout != null) {
            mRootLayout.removeView(mTintView);
        }*/
        if (mSearchListener != null)
            mSearchListener.onSearchEditClosed();
        micStateChanged(true);
        mMicButton.setImageDrawable(
                ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_mic_black, null));
        InputMethodManager inputMethodManager = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getApplicationWindowToken(),
                0);
    }

    public boolean isEditing() {
        return mCurrentState == SearchViewState.EDITING;
    }

    public boolean isSearching() {
        return mCurrentState == SearchViewState.EDITING || mCurrentState == SearchViewState.SEARCH;
    }

    private void setLogoTextInt(String text) {
        mLogoView.setText(text);
    }

    public void setHomeButtonOpenIconState(HomeButton.IconState homeButtonOpenIconState) {
        this.mHomeButtonOpenIconState = homeButtonOpenIconState;
    }

    public void setHomeButtonCloseIconState(HomeButton.IconState homeButtonCloseIconState) {
        this.mHomeButtonCloseIconState = homeButtonCloseIconState;
    }

    public void setSuggestionBuilder(SearchSuggestionsBuilder suggestionBuilder) {
        this.mSuggestionBuilder = suggestionBuilder;
    }

    private void stateFromToolbarToEditing() {
        openSearchInternal(true);
        setCurrentState(SearchViewState.EDITING);
    }

    private void stateFromMenuItemToEditing() {
        setCurrentState(SearchViewState.EDITING);
    }

    private void stateFromSearchToEditing() {
        openSearchInternal(true);
        setCurrentState(SearchViewState.EDITING);
    }

    private void stateFromEditingToNormal() {
        if (mDisplayMode == DisplayMode.TOOLBAR_DRAWER) {
            setCurrentState(SearchViewState.TOOLBAR_DRAWER);
            setSearchString("", false);
            closeSearchInternal();
        } else if (mDisplayMode == DisplayMode.MENUITEM) {
            setCurrentState(SearchViewState.MENUITEM);
            setSearchString("", false);
            hideCircularlyToMenuItem();
        } else if (mDisplayMode == DisplayMode.TOOLBAR_BACKARROW) {
            setCurrentState(SearchViewState.TOOLBAR_BACKARROW);
            setSearchString("", false);
            closeSearchInternal();
        }
        if (mSearchListener != null)
            mSearchListener.onSearchExit();
    }

    private void stateFromEditingToSearch() {
        setCurrentState(SearchViewState.SEARCH);
        closeSearchInternal();
    }

    private void stateToSearch(String query) {
        populateEditText(query);
        setCurrentState(SearchViewState.SEARCH);
        closeSearchInternal();
    }

    private void stateFromSearchToNormal() {
        setLogoTextInt("");
        setSearchString("", true);
        if (mDisplayMode == DisplayMode.TOOLBAR_DRAWER) {
            setCurrentState(SearchViewState.TOOLBAR_DRAWER);
            closeSearchInternal();
        } else if (mDisplayMode == DisplayMode.MENUITEM) {
            setCurrentState(SearchViewState.MENUITEM);
            hideCircularlyToMenuItem();
        } else if (mDisplayMode == DisplayMode.TOOLBAR_BACKARROW) {
            setCurrentState(SearchViewState.TOOLBAR_DRAWER);
            closeSearchInternal();
        }
        if (mSearchListener != null)
            mSearchListener.onSearchExit();
    }

    private void setCurrentState(SearchViewState state) {
        mLastState = mCurrentState;
        mCurrentState = state;
    }

    public void openSearch() {
        if (mCurrentState == SearchViewState.TOOLBAR_DRAWER) {
            stateFromToolbarToEditing();
        } else if (mCurrentState == SearchViewState.MENUITEM) {
            stateFromMenuItemToEditing();
        } else if (mCurrentState == SearchViewState.SEARCH) {
            stateFromSearchToEditing();
        }
    }

    public void openSearch(View menuItemView, int desireRevealWidth) {
        if (mCurrentState == SearchViewState.MENUITEM) {
            revealFromMenuItem(menuItemView, desireRevealWidth);
            stateFromMenuItemToEditing();
        }
    }

    public void openSearch(View menuItemView) {
        if (mCurrentState == SearchViewState.MENUITEM) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = metrics.widthPixels;
            revealFromMenuItem(menuItemView, width);
            stateFromMenuItemToEditing();
        }
    }

    public void openSearch(String query) {
        stateToSearch(query);
    }

    public void closeSearch() {
        if (mCurrentState == SearchViewState.EDITING) {
            stateFromEditingToNormal();
        } else if (mCurrentState == SearchViewState.SEARCH) {
            stateFromSearchToNormal();
        }
    }

    public void cancelEditing() {
        if (mLastState == SearchViewState.SEARCH)
            stateFromEditingToSearch();
        else
            stateFromEditingToNormal();
    }

    public void setVoiceRecognitionDelegate(VoiceRecognitionDelegate delegate) {
        this.mVoiceRecognitionDelegate = delegate;
        micStateChanged();
    }

    public enum DisplayMode {
        MENUITEM(0), TOOLBAR_DRAWER(1), TOOLBAR_BACKARROW(2);
        int mode;

        DisplayMode(int mode) {
            this.mode = mode;
        }

        public static DisplayMode fromInt(int mode) {
            for (DisplayMode enumMode : values()) {
                if (enumMode.mode == mode) return enumMode;
            }
            throw new IllegalArgumentException();
        }

        public int toInt() {
            return mode;
        }
    }

    public enum SearchViewState {
        TOOLBAR_DRAWER, MENUITEM, EDITING, SEARCH, TOOLBAR_BACKARROW
    }

    public interface SearchListener {

        /**
         * Called when the clear button is pressed
         */
        void onSearchCleared();

        /**
         * Called when the PersistentSearchView's EditText text changes
         */
        void onSearchTermChanged(String term);

        /**
         * Called when search happens
         *
         * @param query search string
         */
        void onSearch(String query);

        /**
         * Called when search state change to SEARCH and EditText, Suggestions visible
         */
        void onSearchEditOpened();

        /**
         * Called when search state change from SEARCH and EditText, Suggestions gone
         */
        void onSearchEditClosed();

        /**
         * Called when search back to start state.
         */
        void onSearchExit();
    }

    public interface HomeButtonListener {
        /**
         * Called when the menu button is pressed
         */
        void onHomeButtonClick();
    }
}

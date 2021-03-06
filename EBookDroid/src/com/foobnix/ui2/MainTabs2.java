package com.foobnix.ui2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ebookdroid.ui.viewer.VerticalViewActivity;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Safe;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.pdf.SlidingTabLayout;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.AndroidWhatsNew;
import com.foobnix.pdf.info.ExportSettingsManager;
import com.foobnix.pdf.info.FontExtractor;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.widget.RecentBooksWidget;
import com.foobnix.pdf.info.widget.RecentUpates;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.pdf.search.activity.msg.MessegeBrightness;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.adapter.TabsAdapter2;
import com.foobnix.ui2.fragment.BookmarksFragment2;
import com.foobnix.ui2.fragment.BrowseFragment2;
import com.foobnix.ui2.fragment.OpdsFragment2;
import com.foobnix.ui2.fragment.PrefFragment2;
import com.foobnix.ui2.fragment.RecentFragment2;
import com.foobnix.ui2.fragment.SearchFragment2;
import com.foobnix.ui2.fragment.UIFragment;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class MainTabs2 extends AdsFragmentActivity {
    private static final String TAG = "MainTabs";
    public static final String EXTRA_EXIT = "EXTRA_EXIT";
    public static final String EXTRA_SHOW_TABS = "EXTRA_SHOW_TABS";
    public static String EXTRA_PAGE_NUMBER = "EXTRA_PAGE_NUMBER";
    public static String EXTRA_SEACH_TEXT = "EXTRA_SEACH_TEXT";
    ViewPager pager;
    List<UIFragment> tabFragments;

    public static volatile boolean isInStack;
    TabsAdapter2 adapter;

    ImageView imageMenu;
    View imageMenuParent, overlay;
    TextView toastBrightnessText;

    public boolean isEink = false;

    @Override
    protected void onNewIntent(final Intent intent) {
        LOG.d(TAG, "onNewIntent");
        isInStack = true;
        // testIntentHandler();
        if (intent.getBooleanExtra(EXTRA_EXIT, false)) {
            finish();
            return;
        }
        checkGoToPage(intent);

    }

    public void testIntentHandler() {
        if (getIntent().hasExtra(RecentBooksWidget.TEST_LOCALE)) {

            int pos = getIntent().getIntExtra(RecentBooksWidget.TEST_LOCALE_POS, -1);
            if (pos != -1) {
                pager.setCurrentItem(pos);

                pager.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        DocumentController.chooseFullScreen(MainTabs2.this, true);

                        if (getIntent().getBooleanExtra("id0", false)) {
                            ((SearchFragment2) tabFragments.get(0)).popupMenuTest();

                        }

                        if (getIntent().getBooleanExtra("id1", false)) {
                            ((SearchFragment2) tabFragments.get(0)).onTextRecive("Lewis");

                        }
                    }
                }, 100);

            } else {
                finish();
                startActivity(new Intent(this, MainTabs2.class));
            }

        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        testIntentHandler();
    }

    @Override
    protected void attachBaseContext(Context context) {
        if (AppState.MY_SYSTEM_LANG.equals(AppState.get().appLang) && AppState.get().appFontScale == 1.0f) {
            LOG.d("attachBaseContext skip");
            super.attachBaseContext(context);
        } else {
            LOG.d("attachBaseContext apply");
            super.attachBaseContext(MyContextWrapper.wrap(context));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AppState.get().isWhiteTheme) {
            setTheme(R.style.StyledIndicatorsWhite);
        } else {
            setTheme(R.style.StyledIndicatorsBlack);
        }
        super.onCreate(savedInstanceState);
        RecentUpates.updateAll(this);

        LOG.d(TAG, "onCreate");

        LOG.d("EXTRA_EXIT", EXTRA_EXIT);
        if (getIntent().getBooleanExtra(EXTRA_EXIT, false)) {
            finish();
            return;
        }

        isEink = Dips.isEInk(this);

        TintUtil.setStatusBarColor(this);
        DocumentController.doRotation(this);

        setContentView(R.layout.main_tabs);

        imageMenu = (ImageView) findViewById(R.id.imageMenu1);
        imageMenuParent = findViewById(R.id.imageParent1);
        imageMenuParent.setBackgroundColor(TintUtil.color);

        overlay = findViewById(R.id.overlay);

        toastBrightnessText = (TextView) findViewById(R.id.toastBrightnessText);
        toastBrightnessText.setVisibility(View.GONE);
        TintUtil.setDrawableTint(toastBrightnessText.getCompoundDrawables()[0], Color.WHITE);

        tabFragments = new ArrayList<UIFragment>();

        try {
            for (UITab tab : UITab.getOrdered(AppState.get().tabsOrder)) {
                if (tab.isVisible()) {
                    tabFragments.add(tab.getClazz().newInstance());
                }
            }
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(MainTabs2.this, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
            tabFragments.add(new SearchFragment2());
            tabFragments.add(new BrowseFragment2());
            tabFragments.add(new RecentFragment2());
            tabFragments.add(new BookmarksFragment2());
            tabFragments.add(new OpdsFragment2());
            tabFragments.add(new PrefFragment2());
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.left_drawer, new PrefFragment2()).commit();

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        imageMenu.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(Gravity.START))
                    drawerLayout.closeDrawer(Gravity.START, !AppState.get().isInkMode);
                else
                    drawerLayout.openDrawer(Gravity.START, !AppState.get().isInkMode);

            }
        });

        if (UITab.isShowPreferences()) {
            imageMenu.setVisibility(View.GONE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            imageMenu.setVisibility(View.VISIBLE);
        }

        // ((BrigtnessDraw)
        // findViewById(R.id.brigtnessProgressView)).setActivity(this);

        adapter = new TabsAdapter2(this, tabFragments);
        pager = (ViewPager)

        findViewById(R.id.pager);

        if (Android6.canWrite(this)) {
            pager.setAdapter(adapter);
        }

        pager.setOffscreenPageLimit(5);
        pager.addOnPageChangeListener(onPageChangeListener);

        drawerLayout.addDrawerListener(new DrawerListener() {

            @Override
            public void onDrawerStateChanged(int arg0) {
            }

            @Override
            public void onDrawerSlide(View arg0, float arg1) {

            }

            @Override
            public void onDrawerOpened(View arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onDrawerClosed(View arg0) {
                tabFragments.get(pager.getCurrentItem()).onSelectFragment();

            }
        });

        indicator = (SlidingTabLayout) findViewById(R.id.slidingTabs);
        indicator.setViewPager(pager);

        indicator.setDividerColors(getResources().getColor(R.color.tint_divider));
        indicator.setSelectedIndicatorColors(Color.WHITE);
        indicator.setBackgroundColor(TintUtil.color);

        if (AppState.get().isInkMode) {
            TintUtil.setTintImageNoAlpha(imageMenu, TintUtil.color);
            indicator.setSelectedIndicatorColors(TintUtil.color);
            indicator.setDividerColors(TintUtil.color);
            indicator.setBackgroundColor(Color.TRANSPARENT);
            imageMenuParent.setBackgroundColor(Color.TRANSPARENT);

        }

        Android6.checkPermissions(this);
        // Analytics.onStart(this);
        FontExtractor.extractFonts(this);

        List<String> actions = Arrays.asList("android.intent.action.PROCESS_TEXT", "android.intent.action.SEARCH", "android.intent.action.SEND");
        List<String> extras = Arrays.asList(Intent.EXTRA_PROCESS_TEXT_READONLY, Intent.EXTRA_PROCESS_TEXT, SearchManager.QUERY, Intent.EXTRA_TEXT);
        if (getIntent() != null && getIntent().getAction() != null) {
            if (actions.contains(getIntent().getAction())) {
                for (String extra : extras) {
                    final String text = getIntent().getStringExtra(extra);
                    if (TxtUtils.isNotEmpty(text)) {
                        AppState.get().lastA = null;
                        pager.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                ((SearchFragment2) tabFragments.get(0)).searchAndOrderExteral(text);
                            }
                        }, 250);
                        break;
                    }
                }

            }

        }

        boolean showTabs = getIntent().getBooleanExtra(EXTRA_SHOW_TABS, false);
        LOG.d("EXTRA_SHOW_TABS", showTabs, AppState.get().lastMode);
        if (showTabs == false && AppState.get().isOpenLastBook) {
            LOG.d("Open LAST book");
            final FileMeta meta = AppDB.get().getRecentLast();
            AppState.get().lastA = null;

            if (meta != null) {

                Safe.run(new Runnable() {

                    @Override
                    public void run() {
                        boolean isEasyMode = HorizontalViewActivity.class.getSimpleName().equals(AppState.get().lastMode);
                        Intent intent = new Intent(MainTabs2.this, isEasyMode ? HorizontalViewActivity.class : VerticalViewActivity.class);
                        intent.setData(Uri.fromFile(new File(meta.getPath())));
                        startActivity(intent);
                    }
                });
            }
        } else if (false && !AppState.get().isOpenLastBook) {
            LOG.d("Open book lastA", AppState.get().lastA);

            Safe.run(new Runnable() {

                @Override
                public void run() {
                    if (HorizontalViewActivity.class.getSimpleName().equals(AppState.get().lastA)) {

                        FileMeta meta = AppDB.get().getRecentLast();
                        if (meta != null) {
                            Intent intent = new Intent(MainTabs2.this, HorizontalViewActivity.class);
                            intent.setData(Uri.fromFile(new File(meta.getPath())));
                            startActivity(intent);
                            LOG.d("Start lasta", AppState.get().lastA);
                        }
                    } else if (VerticalViewActivity.class.getSimpleName().equals(AppState.get().lastA)) {
                        FileMeta meta = AppDB.get().getRecentLast();
                        if (meta != null) {
                            Intent intent = new Intent(MainTabs2.this, VerticalViewActivity.class);
                            intent.setData(Uri.fromFile(new File(meta.getPath())));
                            startActivity(intent);
                            LOG.d("Start lasta", AppState.get().lastA);
                        }

                    }

                }
            });

        }

        checkGoToPage(getIntent());

        try {
            AndroidWhatsNew.checkForNewBeta(this);
        } catch (Exception e) {
            LOG.e(e);
        }
    }


    @Subscribe
    public void onMessegeBrightness(MessegeBrightness msg) {
        BrightnessHelper.onMessegeBrightness(msg, toastBrightnessText, overlay);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int pos = intent.getIntExtra(EXTRA_PAGE_NUMBER, -1);
            if (pos != -1) {
                pager.setCurrentItem(pos);
            } else {
                if (AppState.get().isInkMode) {
                    TintUtil.setTintImageNoAlpha(imageMenu, TintUtil.color);
                    indicator.setSelectedIndicatorColors(TintUtil.color);
                    indicator.setDividerColors(TintUtil.color);
                    indicator.updateIcons(pager.getCurrentItem());
                } else {
                    indicator.setBackgroundColor(TintUtil.color);
                    imageMenuParent.setBackgroundColor(TintUtil.color);
                }
            }
        }

    };

    public void checkGoToPage(Intent intent) {
        int pos = intent.getIntExtra(EXTRA_PAGE_NUMBER, -1);
        if (pos != -1) {
            pager.setCurrentItem(pos);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LOG.d(TAG, "onResume");
        isInStack = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // DocumentController.chooseFullScreen(this, false);
        TintUtil.updateAll();
        AppState.get().lastA = MainTabs2.class.getSimpleName();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(UIFragment.INTENT_TINT_CHANGE));

        try {
            tabFragments.get(pager.getCurrentItem()).onSelectFragment();
        } catch (Exception e) {
            LOG.e(e);
        }
        adapter.notifyDataSetChanged();

        BrightnessHelper.applyBrigtness(this);
        BrightnessHelper.updateOverlay(overlay);

        EventBus.getDefault().unregister(this);
        EventBus.getDefault().register(this);

    };

    boolean isMyKey = false;

    @Override
    public boolean onKeyDown(int keyCode1, KeyEvent event) {
        if (!isEink) {
            return super.onKeyDown(keyCode1, event);
        }

        int keyCode = event.getKeyCode();
        if (keyCode == 0) {
            keyCode = event.getScanCode();
        }
        isMyKey = false;
        if (tabFragments.get(pager.getCurrentItem()).onKeyDown(keyCode)) {
            isMyKey = true;
            return true;
        }

        return super.onKeyDown(keyCode1, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!isEink) {
            return super.onKeyUp(keyCode, event);
        }

        if (isMyKey) {
            return true;
        }
        // TODO Auto-generated method stub
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        AppState.get().save(this);
        AppState.get().save(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        ImageLoader.getInstance().clearAllTasks();

    };

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOG.d(TAG, "onDestroy");
        if (pager != null) {
            try {
                pager.setAdapter(null);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        // Analytics.onStop(this);
        isInStack = false;
        CacheDir.ZipApp.removeCacheContent();
        ImageExtractor.clearErrors();
        ImageExtractor.clearCodeDocument();

        if (AppState.get().isAutomaticExport && Android6.canWrite(this)) {
            try {
                File file = new File(CacheZipUtils.SD_CARD_APP_DIR, Apps.getApplicationName(this) + "-" + Apps.getVersionName(this) + "-backup-export-all.JSON.txt");
                LOG.d("isAutomaticExport", file);
                ExportSettingsManager.getInstance(this).exportAll(file);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        String language = newConfig.locale.getLanguage();
        float fontScale = newConfig.fontScale;

        LOG.d("ContextWrapper ConfigChanged", language, fontScale);

        if (pager != null) {
            int currentItem = pager.getCurrentItem();
            pager.setAdapter(adapter);
            pager.setCurrentItem(currentItem);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Android6.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int pos) {
            tabFragments.get(pos).onSelectFragment();
            TempHolder.get().currentTab = pos;
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };
    private SlidingTabLayout indicator;

    @Override
    public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
        if (CloseAppDialog.checkLongPress(this, event)) {
            CloseAppDialog.show(this, closeActivityRunnable);
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onFinishActivity() {
        finish();
    }

    @Override
    public void onBackPressed() {
        if (isInterstialShown()) {
            onFinishActivity();
            return;
        }

        if (!tabFragments.isEmpty() && tabFragments.get(pager.getCurrentItem()).isBackPressed()) {
            return;
        }

        CloseAppDialog.show(this, closeActivityRunnable);
    }

    Runnable closeActivityRunnable = new Runnable() {

        @Override
        public void run() {
            showInterstial();
        }
    };

    public static void startActivity(Activity c, int tab) {
        AppState.get().lastA = null;
        final Intent intent = new Intent(c, MainTabs2.class);
        intent.putExtra(MainTabs2.EXTRA_SHOW_TABS, true);
        intent.putExtra(MainTabs2.EXTRA_PAGE_NUMBER, tab);
        c.startActivity(intent);
        c.overridePendingTransition(0, 0);

    }

    public static void closeApp(Context c) {
        // if (MainTabs2.isInStack) {
        Intent startMain = new Intent(c, MainTabs2.class);
        startMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startMain.putExtra(MainTabs2.EXTRA_EXIT, true);
        c.startActivity(startMain);
        // }
    }

}

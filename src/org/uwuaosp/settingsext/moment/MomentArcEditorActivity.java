/*
 * Copyright (C) 2026 The uwuAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uwuaosp.settingsext.moment;

import android.content.ClipData;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.google.android.material.appbar.AppBarLayout;

import org.uwuaosp.settingsext.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MomentArcEditorActivity extends CollapsingToolbarBaseActivity {
    private static final String EMPTY_TARGET = "empty:";
    private static final int SOURCE_SPAN_COUNT = 5;
    private static final String STATE_IS_LEFT = "is_left";
    private static final String STATE_SEARCH_QUERY = "search_query";
    private static final String STATE_EXPANDED_SECTION = "expanded_section";
    private static final String STATE_PENDING_KEY = "pending_key";

    private static final String ENTRY_PREFIX_APP = "app:";
    private static final String ENTRY_PREFIX_SHORTCUT = "shortcut:";
    private static final String ENTRY_FIELD_SEPARATOR = ":";

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final ArrayList<TargetInfo> mAllApps = new ArrayList<>();
    private final ArrayList<TargetInfo> mAllShortcuts = new ArrayList<>();
    private final ArrayList<TargetInfo> mFilteredApps = new ArrayList<>();
    private final ArrayList<TargetInfo> mFilteredShortcuts = new ArrayList<>();
    private final ArrayList<TargetInfo> mAssignedTargets = new ArrayList<>();

    private MomentArcLayout mPreview;
    private RecyclerView mAppsList;
    private RecyclerView mShortcutsList;
    private View mAppsHeader;
    private View mShortcutsHeader;
    private View mAppsContainer;
    private View mShortcutsContainer;
    private ImageView mAppsArrow;
    private ImageView mShortcutsArrow;
    private TextView mAppsEmpty;
    private TextView mShortcutsEmpty;
    private TargetAdapter mAppsAdapter;
    private TargetAdapter mShortcutsAdapter;

    @Nullable
    private TargetInfo mPendingTarget;
    @Nullable
    private MenuItem mSideToggleItem;
    @Nullable
    private String mRestoredPendingKey;
    @Nullable
    private AppBarLayout.OnOffsetChangedListener mAppBarLockListener;

    private boolean mIsLeftSide = true;
    private boolean mShortcutsUnavailable;
    private int mLoadGeneration;
    @NonNull
    private String mSearchQuery = "";
    @NonNull
    private Section mExpandedSection = Section.NONE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moment_arc_editor);
        setTitle(R.string.moment_arc_editor_title);

        if (savedInstanceState != null) {
            mIsLeftSide = savedInstanceState.getBoolean(STATE_IS_LEFT, true);
            mSearchQuery = savedInstanceState.getString(STATE_SEARCH_QUERY, "");
            mExpandedSection = Section.fromName(
                    savedInstanceState.getString(STATE_EXPANDED_SECTION));
            mRestoredPendingKey = savedInstanceState.getString(STATE_PENDING_KEY);
        }

        for (int i = 0; i < MomentArcLayout.TOTAL_CONFIGURABLE; i++) {
            mAssignedTargets.add(null);
        }

        bindViews();
        collapseAndLockAppBar();
        bindPreview();
        bindSourceLists();
        bindSectionHeaders();
        updateExpandedSections();
        updatePreview();
    }

    @Override
    protected void onStart() {
        super.onStart();
        collapseAndLockAppBar();
        loadData();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        collapseAndLockAppBar();
    }

    @Override
    protected void onDestroy() {
        AppBarLayout appBarLayout = getAppBarLayout();
        if (appBarLayout != null && mAppBarLockListener != null) {
            appBarLayout.removeOnOffsetChangedListener(mAppBarLockListener);
        }
        mExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IS_LEFT, mIsLeftSide);
        outState.putString(STATE_SEARCH_QUERY, mSearchQuery);
        outState.putString(STATE_EXPANDED_SECTION, mExpandedSection.name());
        if (mPendingTarget != null) {
            outState.putString(STATE_PENDING_KEY, mPendingTarget.getKey());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.moment_arc_editor, menu);

        MenuItem searchItem = menu.findItem(R.id.moment_arc_editor_search_menu);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.moment_arc_editor_search_title));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                applyPreviewPositionCompensation();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                mSearchQuery = "";
                applyFilter();
                applyPreviewPositionCompensation();
                return true;
            }
        });
        int searchTextId = searchView.getContext().getResources().getIdentifier(
                "search_src_text", "id", "android");
        TextView searchText = searchTextId != 0 ? searchView.findViewById(searchTextId) : null;
        if (searchText != null) {
            searchText.setTextCursorDrawable(searchText.getContext().getDrawable(
                    R.drawable.search_cursor));
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mSearchQuery = newText != null ? newText : "";
                applyFilter();
                return true;
            }
        });
        searchView.setOnCloseListener(() -> {
            if (!mSearchQuery.isEmpty()) {
                mSearchQuery = "";
                applyFilter();
            }
            return false;
        });
        if (!mSearchQuery.isEmpty()) {
            searchItem.expandActionView();
            searchView.setIconified(false);
            searchView.setQuery(mSearchQuery, false);
            searchView.clearFocus();
        } else {
            applyPreviewPositionCompensation();
        }

        mSideToggleItem = menu.findItem(R.id.moment_arc_editor_toggle_side_menu);
        updateSideToggleTitle();
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.moment_arc_editor_toggle_side_menu) {
            mIsLeftSide = !mIsLeftSide;
            mPreview.setLeftSide(mIsLeftSide);
            updateSideToggleTitle();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void bindViews() {
        mPreview = findViewById(R.id.moment_arc_editor_preview);
        mAppsHeader = findViewById(R.id.moment_arc_editor_apps_header);
        mShortcutsHeader = findViewById(R.id.moment_arc_editor_shortcuts_header);
        mAppsContainer = findViewById(R.id.moment_arc_editor_apps_container);
        mShortcutsContainer = findViewById(R.id.moment_arc_editor_shortcuts_container);
        mAppsArrow = findViewById(R.id.moment_arc_editor_apps_arrow);
        mShortcutsArrow = findViewById(R.id.moment_arc_editor_shortcuts_arrow);
        mAppsEmpty = findViewById(R.id.moment_arc_editor_apps_empty);
        mShortcutsEmpty = findViewById(R.id.moment_arc_editor_shortcuts_empty);
        mAppsList = findViewById(R.id.moment_arc_editor_apps_list);
        mShortcutsList = findViewById(R.id.moment_arc_editor_shortcuts_list);
    }

    private void collapseAndLockAppBar() {
        AppBarLayout appBarLayout = getAppBarLayout();
        if (appBarLayout == null) {
            return;
        }
        appBarLayout.setLiftOnScroll(false);
        appBarLayout.post(() -> appBarLayout.setExpanded(false, false));
        if (mAppBarLockListener == null) {
            mAppBarLockListener = (layout, verticalOffset) -> {
                if (verticalOffset != -layout.getTotalScrollRange()) {
                    layout.post(() -> layout.setExpanded(false, false));
                }
            };
            appBarLayout.addOnOffsetChangedListener(mAppBarLockListener);
        }
        if (!(appBarLayout.getLayoutParams() instanceof CoordinatorLayout.LayoutParams)) {
            return;
        }
        CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        AppBarLayout.Behavior behavior = params.getBehavior() instanceof AppBarLayout.Behavior
                ? (AppBarLayout.Behavior) params.getBehavior()
                : new AppBarLayout.Behavior();
        behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
            @Override
            public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                return false;
            }
        });
        params.setBehavior(behavior);
        appBarLayout.setLayoutParams(params);
    }

    private void bindPreview() {
        mPreview.setLeftSide(mIsLeftSide);
        mPreview.setOnSlotClickListener(this::onSlotClicked);
        mPreview.setOnSlotLongClickListener(this::onSlotLongClicked);
        mPreview.setOnSlotDropListener(this::onSlotDropped);
        mPreview.addOnLayoutChangeListener((v, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> applyPreviewPositionCompensation());
    }

    private void bindSourceLists() {
        mAppsAdapter = new TargetAdapter(target -> selectPendingTarget(target));
        mShortcutsAdapter = new TargetAdapter(target -> selectPendingTarget(target));

        mAppsList.setLayoutManager(new GridLayoutManager(this, SOURCE_SPAN_COUNT));
        mAppsList.setItemAnimator(null);
        mAppsList.setAdapter(mAppsAdapter);

        mShortcutsList.setLayoutManager(new GridLayoutManager(this, SOURCE_SPAN_COUNT));
        mShortcutsList.setItemAnimator(null);
        mShortcutsList.setAdapter(mShortcutsAdapter);
    }

    private void bindSectionHeaders() {
        mAppsHeader.setOnClickListener(v -> toggleSection(Section.APPS));
        mShortcutsHeader.setOnClickListener(v -> toggleSection(Section.SHORTCUTS));
    }

    private void loadData() {
        final int loadGeneration = ++mLoadGeneration;
        mExecutor.execute(() -> {
            LoadedData loadedData = buildLoadedData();
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed() || loadGeneration != mLoadGeneration) {
                    return;
                }
                mAllApps.clear();
                mAllApps.addAll(loadedData.apps);
                mAllShortcuts.clear();
                mAllShortcuts.addAll(loadedData.shortcuts);
                for (int i = 0; i < MomentArcLayout.TOTAL_CONFIGURABLE; i++) {
                    mAssignedTargets.set(i, loadedData.assignedTargets.get(i));
                }
                mShortcutsUnavailable = loadedData.shortcutsUnavailable;
                if (mPendingTarget != null) {
                    mPendingTarget = findTargetByKey(mPendingTarget.getKey());
                } else if (mRestoredPendingKey != null) {
                    mPendingTarget = findTargetByKey(mRestoredPendingKey);
                    mRestoredPendingKey = null;
                }
                applyFilter();
                updatePreview();
            });
        });
    }

    @NonNull
    private LoadedData buildLoadedData() {
        PackageManager packageManager = getPackageManager();
        LauncherApps launcherApps = getSystemService(LauncherApps.class);

        ArrayList<TargetInfo> apps = loadLaunchableApps(packageManager);
        Map<String, String> appLabels = new LinkedHashMap<>();
        Map<String, TargetInfo> allTargetsByKey = new LinkedHashMap<>();
        for (TargetInfo target : apps) {
            appLabels.put(target.getPackageName(), target.getTitle());
            allTargetsByKey.put(target.getKey(), target);
        }

        ShortcutLoadResult shortcutLoadResult = loadShortcuts(launcherApps, packageManager, appLabels);
        for (TargetInfo target : shortcutLoadResult.shortcuts) {
            allTargetsByKey.put(target.getKey(), target);
        }

        ArrayList<TargetInfo> assignedTargets = new ArrayList<>();
        int count = 0;

        List<String> innerTargets = MomentArcSettings.getInnerRingTargets(this);
        for (String rawTarget : innerTargets) {
            if (count >= MomentArcLayout.INNER_CONFIGURABLE) {
                break;
            }
            if (EMPTY_TARGET.equals(rawTarget)) {
                assignedTargets.add(null);
                count++;
                continue;
            }
            ParsedTarget parsedTarget = parseTarget(rawTarget);
            if (parsedTarget == null) {
                assignedTargets.add(null);
                count++;
                continue;
            }
            TargetInfo resolved = allTargetsByKey.get(parsedTarget.getKey());
            if (resolved == null) {
                resolved = parsedTarget.resolve(packageManager, launcherApps, appLabels,
                        getResources().getDisplayMetrics().densityDpi);
            }
            if (resolved != null) {
                assignedTargets.add(resolved);
            } else {
                assignedTargets.add(null);
            }
            count++;
        }
        while (assignedTargets.size() < MomentArcLayout.INNER_CONFIGURABLE) {
            assignedTargets.add(null);
        }

        List<String> outerTargets = MomentArcSettings.getOuterRingTargets(this);
        count = 0;
        for (String rawTarget : outerTargets) {
            if (count >= MomentArcLayout.OUTER_CONFIGURABLE) {
                break;
            }
            if (EMPTY_TARGET.equals(rawTarget)) {
                assignedTargets.add(null);
                count++;
                continue;
            }
            ParsedTarget parsedTarget = parseTarget(rawTarget);
            if (parsedTarget == null) {
                assignedTargets.add(null);
                count++;
                continue;
            }
            TargetInfo resolved = allTargetsByKey.get(parsedTarget.getKey());
            if (resolved == null) {
                resolved = parsedTarget.resolve(packageManager, launcherApps, appLabels,
                        getResources().getDisplayMetrics().densityDpi);
            }
            if (resolved != null) {
                assignedTargets.add(resolved);
            } else {
                assignedTargets.add(null);
            }
            count++;
        }
        while (assignedTargets.size() < MomentArcLayout.TOTAL_CONFIGURABLE) {
            assignedTargets.add(null);
        }

        return new LoadedData(apps, shortcutLoadResult.shortcuts, assignedTargets,
                shortcutLoadResult.unavailable);
    }

    @NonNull
    private ArrayList<TargetInfo> loadLaunchableApps(@NonNull PackageManager packageManager) {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(
                intent, PackageManager.GET_META_DATA | PackageManager.MATCH_ALL);
        LinkedHashMap<String, TargetInfo> deduped = new LinkedHashMap<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo == null || resolveInfo.activityInfo.packageName == null) {
                continue;
            }
            String packageName = resolveInfo.activityInfo.packageName;
            if (deduped.containsKey(packageName)) {
                continue;
            }
            Drawable icon = resolveInfo.loadIcon(packageManager);
            if (icon == null) {
                continue;
            }
            CharSequence label = resolveInfo.loadLabel(packageManager);
            String title = label != null ? label.toString() : packageName;
            deduped.put(packageName, TargetInfo.app(title, packageName, packageName, icon));
        }
        ArrayList<TargetInfo> apps = new ArrayList<>(deduped.values());
        apps.sort(Comparator.comparing(TargetInfo::getTitle, String.CASE_INSENSITIVE_ORDER));
        return apps;
    }

    @NonNull
    private ShortcutLoadResult loadShortcuts(@Nullable LauncherApps launcherApps,
            @NonNull PackageManager packageManager, @NonNull Map<String, String> appLabels) {
        if (launcherApps == null) {
            return new ShortcutLoadResult(new ArrayList<>(), true);
        }

        try {
            List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(
                    new LauncherApps.ShortcutQuery().setQueryFlags(
                            LauncherApps.ShortcutQuery.FLAG_MATCH_ALL_KINDS_WITH_ALL_PINNED),
                    UserHandle.of(UserHandle.myUserId()));
            if (shortcuts == null || shortcuts.isEmpty()) {
                shortcuts = launcherApps.getShortcuts(
                        new LauncherApps.ShortcutQuery().setQueryFlags(
                                LauncherApps.ShortcutQuery.FLAG_MATCH_ALL_KINDS),
                        UserHandle.of(UserHandle.myUserId()));
            }

            LinkedHashMap<String, TargetInfo> deduped = new LinkedHashMap<>();
            if (shortcuts != null) {
                for (ShortcutInfo shortcutInfo : shortcuts) {
                    if (shortcutInfo == null || shortcutInfo.getPackage() == null
                            || shortcutInfo.getId() == null || !shortcutInfo.isEnabled()) {
                        continue;
                    }
                    String packageName = shortcutInfo.getPackage();
                    String shortcutId = shortcutInfo.getId();
                    int userId = shortcutInfo.getUserHandle() != null
                            ? shortcutInfo.getUserHandle().getIdentifier()
                            : UserHandle.myUserId();
                    String key = buildShortcutKey(userId, packageName, shortcutId);
                    if (deduped.containsKey(key)) {
                        continue;
                    }
                    Drawable icon = launcherApps.getShortcutBadgedIconDrawable(
                            shortcutInfo, getResources().getDisplayMetrics().densityDpi);
                    if (icon == null) {
                        try {
                            icon = packageManager.getApplicationIcon(packageName);
                        } catch (PackageManager.NameNotFoundException ignored) {
                        }
                    }
                    if (icon == null) {
                        continue;
                    }
                    String title = getShortcutTitle(shortcutInfo, shortcutId);
                    String summary = appLabels.containsKey(packageName)
                            ? appLabels.get(packageName) : packageName;
                    deduped.put(key, TargetInfo.shortcut(
                            title, summary, packageName, icon, shortcutId, userId));
                }
            }

            ArrayList<TargetInfo> shortcutTargets = new ArrayList<>(deduped.values());
            shortcutTargets.sort(Comparator
                    .comparing(TargetInfo::getTitle, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(TargetInfo::getSummary, String.CASE_INSENSITIVE_ORDER));
            return new ShortcutLoadResult(shortcutTargets, false);
        } catch (RuntimeException e) {
            return new ShortcutLoadResult(new ArrayList<>(), true);
        }
    }

    @NonNull
    private String getShortcutTitle(@NonNull ShortcutInfo shortcutInfo, @NonNull String fallbackId) {
        CharSequence shortLabel = shortcutInfo.getShortLabel();
        if (shortLabel != null && shortLabel.length() > 0) {
            return shortLabel.toString();
        }
        CharSequence longLabel = shortcutInfo.getLongLabel();
        if (longLabel != null && longLabel.length() > 0) {
            return longLabel.toString();
        }
        return fallbackId;
    }

    private void toggleSection(@NonNull Section section) {
        if (mExpandedSection == section) {
            mExpandedSection = Section.NONE;
        } else {
            mExpandedSection = section;
        }
        updateExpandedSections();
    }

    private void updateExpandedSections() {
        boolean appsExpanded = mExpandedSection == Section.APPS;
        boolean shortcutsExpanded = mExpandedSection == Section.SHORTCUTS;
        mAppsContainer.setVisibility(appsExpanded ? View.VISIBLE : View.GONE);
        mShortcutsContainer.setVisibility(shortcutsExpanded ? View.VISIBLE : View.GONE);
        mAppsArrow.animate().rotation(appsExpanded ? 180f : 0f).setDuration(180).start();
        mShortcutsArrow.animate().rotation(shortcutsExpanded ? 180f : 0f).setDuration(180).start();
    }

    private void applyFilter() {
        mFilteredApps.clear();
        mFilteredShortcuts.clear();
        for (TargetInfo target : mAllApps) {
            if (target.matchesQuery(mSearchQuery)) {
                mFilteredApps.add(target);
            }
        }
        for (TargetInfo target : mAllShortcuts) {
            if (target.matchesQuery(mSearchQuery)) {
                mFilteredShortcuts.add(target);
            }
        }
        mAppsAdapter.submitList(mFilteredApps, getPendingKey());
        mShortcutsAdapter.submitList(mFilteredShortcuts, getPendingKey());
        mAppsEmpty.setVisibility(mFilteredApps.isEmpty() ? View.VISIBLE : View.GONE);
        mShortcutsEmpty.setVisibility(mFilteredShortcuts.isEmpty() ? View.VISIBLE : View.GONE);
        mShortcutsEmpty.setText(mShortcutsUnavailable
                ? R.string.moment_arc_editor_shortcuts_unavailable
                : R.string.moment_arc_editor_empty_shortcuts);
    }

    private void selectPendingTarget(@NonNull TargetInfo target) {
        if (target.getKey().equals(getPendingKey())) {
            mPendingTarget = null;
        } else {
            mPendingTarget = target;
        }
        refreshSelectionState();
    }

    private void refreshSelectionState() {
        String pendingKey = getPendingKey();
        mAppsAdapter.updateSelection(pendingKey);
        mShortcutsAdapter.updateSelection(pendingKey);
    }

    private void onSlotClicked(int slotIndex) {
        if (mPendingTarget == null) {
            return;
        }
        placeTarget(slotIndex, mPendingTarget);
    }

    private boolean onSlotLongClicked(int slotIndex) {
        int targetIndex = slotToTargetIndex(slotIndex);
        if (targetIndex < 0 || targetIndex >= mAssignedTargets.size()) {
            return false;
        }
        if (mAssignedTargets.get(targetIndex) == null) {
            return false;
        }
        mAssignedTargets.set(targetIndex, null);
        persistAssignedTargets();
        updatePreview();
        return true;
    }

    private boolean onSlotDropped(int slotIndex, @Nullable Object payload) {
        if (!(payload instanceof TargetInfo)) {
            return false;
        }
        placeTarget(slotIndex, (TargetInfo) payload);
        return true;
    }

    private void placeTarget(int slotIndex, @NonNull TargetInfo target) {
        int targetIndex = slotToTargetIndex(slotIndex);
        if (targetIndex < 0 || targetIndex >= mAssignedTargets.size()) {
            return;
        }
        int existingIndex = indexOfAssignedTarget(target.getKey());
        if (existingIndex >= 0 && existingIndex != targetIndex) {
            mAssignedTargets.set(existingIndex, null);
        }
        mAssignedTargets.set(targetIndex, target);
        mPendingTarget = null;
        persistAssignedTargets();
        updatePreview();
        refreshSelectionState();
    }

    private int slotToTargetIndex(int slotIndex) {
        if (slotIndex < MomentArcLayout.INNER_CONFIGURABLE) {
            return slotIndex;
        }
        return slotIndex - 1;
    }

    private int indexOfAssignedTarget(@NonNull String key) {
        for (int i = 0; i < mAssignedTargets.size(); i++) {
            TargetInfo assigned = mAssignedTargets.get(i);
            if (assigned != null && key.equals(assigned.getKey())) {
                return i;
            }
        }
        return -1;
    }

    private void updatePreview() {
        ArrayList<MomentArcLayout.SlotItem> slotItems = new ArrayList<>();
        for (int i = 0; i < MomentArcLayout.INNER_CONFIGURABLE; i++) {
            TargetInfo target = mAssignedTargets.get(i);
            if (target == null || target.getIcon() == null) {
                slotItems.add(MomentArcLayout.SlotItem.empty(
                        getString(R.string.moment_arc_editor_slot_empty)));
            } else {
                slotItems.add(MomentArcLayout.SlotItem.filled(
                        target.getIcon(),
                        getString(R.string.moment_arc_editor_slot_filled, target.getTitle())));
            }
        }
        slotItems.add(MomentArcLayout.SlotItem.moreApps(
                getDrawable(R.drawable.ic_moment_arc_all_apps),
                getString(R.string.moment_arc_editor_more_apps)));
        for (int i = MomentArcLayout.INNER_CONFIGURABLE;
                i < MomentArcLayout.TOTAL_CONFIGURABLE; i++) {
            TargetInfo target = mAssignedTargets.get(i);
            if (target == null || target.getIcon() == null) {
                slotItems.add(MomentArcLayout.SlotItem.empty(
                        getString(R.string.moment_arc_editor_slot_empty)));
            } else {
                slotItems.add(MomentArcLayout.SlotItem.filled(
                        target.getIcon(),
                        getString(R.string.moment_arc_editor_slot_filled, target.getTitle())));
            }
        }
        mPreview.setSlots(slotItems);
    }

    private void persistAssignedTargets() {
        ArrayList<String> innerTargets = new ArrayList<>();
        for (int i = 0; i < MomentArcLayout.INNER_CONFIGURABLE; i++) {
            TargetInfo target = mAssignedTargets.get(i);
            innerTargets.add(target != null ? target.toStoredValue() : EMPTY_TARGET);
        }
        MomentArcSettings.saveInnerRingTargets(this, innerTargets);

        ArrayList<String> outerTargets = new ArrayList<>();
        for (int i = MomentArcLayout.INNER_CONFIGURABLE;
                i < MomentArcLayout.TOTAL_CONFIGURABLE; i++) {
            TargetInfo target = mAssignedTargets.get(i);
            outerTargets.add(target != null ? target.toStoredValue() : EMPTY_TARGET);
        }
        MomentArcSettings.saveOuterRingTargets(this, outerTargets);
    }

    private void updateSideToggleTitle() {
        if (mSideToggleItem != null) {
            mSideToggleItem.setTitle(mIsLeftSide
                    ? R.string.moment_arc_editor_right_side
                    : R.string.moment_arc_editor_left_side);
        }
    }

    private void applyPreviewPositionCompensation() {
        mPreview.animate()
                .translationY(0f)
                .setDuration(180)
                .start();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Nullable
    private String getPendingKey() {
        return mPendingTarget != null ? mPendingTarget.getKey() : null;
    }

    @Nullable
    private TargetInfo findTargetByKey(@Nullable String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        for (TargetInfo target : mAllApps) {
            if (key.equals(target.getKey())) {
                return target;
            }
        }
        for (TargetInfo target : mAllShortcuts) {
            if (key.equals(target.getKey())) {
                return target;
            }
        }
        return null;
    }

    @Nullable
    private ParsedTarget parseTarget(@Nullable String rawEntry) {
        if (rawEntry == null) {
            return null;
        }
        String entry = rawEntry.trim();
        if (entry.isEmpty()) {
            return null;
        }
        if (!entry.startsWith(ENTRY_PREFIX_APP) && !entry.startsWith(ENTRY_PREFIX_SHORTCUT)) {
            return ParsedTarget.app(entry);
        }
        if (entry.startsWith(ENTRY_PREFIX_APP)) {
            String packageName = Uri.decode(entry.substring(ENTRY_PREFIX_APP.length())).trim();
            return packageName.isEmpty() ? null : ParsedTarget.app(packageName);
        }
        return ParsedTarget.shortcut(entry);
    }

    @NonNull
    private static String buildShortcutKey(int userId, @NonNull String packageName,
            @NonNull String shortcutId) {
        return ENTRY_PREFIX_SHORTCUT + userId + ENTRY_FIELD_SEPARATOR
                + packageName + ENTRY_FIELD_SEPARATOR + shortcutId;
    }

    private enum Section {
        NONE,
        APPS,
        SHORTCUTS;

        @NonNull
        static Section fromName(@Nullable String name) {
            if (name == null) {
                return NONE;
            }
            for (Section section : values()) {
                if (section.name().equals(name)) {
                    return section;
                }
            }
            return NONE;
        }
    }

    private static final class LoadedData {
        @NonNull
        final List<TargetInfo> apps;
        @NonNull
        final List<TargetInfo> shortcuts;
        @NonNull
        final List<TargetInfo> assignedTargets;
        final boolean shortcutsUnavailable;

        LoadedData(@NonNull List<TargetInfo> apps, @NonNull List<TargetInfo> shortcuts,
                @NonNull List<TargetInfo> assignedTargets, boolean shortcutsUnavailable) {
            this.apps = apps;
            this.shortcuts = shortcuts;
            this.assignedTargets = assignedTargets;
            this.shortcutsUnavailable = shortcutsUnavailable;
        }
    }

    private static final class ShortcutLoadResult {
        @NonNull
        final ArrayList<TargetInfo> shortcuts;
        final boolean unavailable;

        ShortcutLoadResult(@NonNull ArrayList<TargetInfo> shortcuts, boolean unavailable) {
            this.shortcuts = shortcuts;
            this.unavailable = unavailable;
        }
    }

    private static final class ParsedTarget {
        private final boolean mIsShortcut;
        @NonNull
        private final String mPackageName;
        @Nullable
        private final String mShortcutId;
        private final int mUserId;

        private ParsedTarget(boolean isShortcut, @NonNull String packageName,
                @Nullable String shortcutId, int userId) {
            mIsShortcut = isShortcut;
            mPackageName = packageName;
            mShortcutId = shortcutId;
            mUserId = userId;
        }

        @NonNull
        static ParsedTarget app(@NonNull String packageName) {
            return new ParsedTarget(false, packageName, null, UserHandle.myUserId());
        }

        @Nullable
        static ParsedTarget shortcut(@NonNull String entry) {
            String[] parts = entry.split(ENTRY_FIELD_SEPARATOR, 4);
            if (parts.length < 3 || parts.length > 4) {
                return null;
            }
            boolean hasExplicitUserId = parts.length == 4;
            Integer userId = hasExplicitUserId
                    ? tryParseInt(parts[1])
                    : UserHandle.myUserId();
            String packageName = Uri.decode(parts[hasExplicitUserId ? 2 : 1]).trim();
            String shortcutId = Uri.decode(parts[hasExplicitUserId ? 3 : 2]).trim();
            if (userId == null || packageName.isEmpty() || shortcutId.isEmpty()) {
                return null;
            }
            return new ParsedTarget(true, packageName, shortcutId, userId);
        }

        @NonNull
        String getKey() {
            return mIsShortcut
                    ? buildShortcutKey(mUserId, mPackageName, mShortcutId)
                    : mPackageName;
        }

        @Nullable
        TargetInfo resolve(@NonNull PackageManager packageManager,
                @Nullable LauncherApps launcherApps, @NonNull Map<String, String> appLabels,
                int densityDpi) {
            if (!mIsShortcut) {
                try {
                    Drawable icon = packageManager.getApplicationIcon(mPackageName);
                    CharSequence label = packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(mPackageName, 0));
                    String title = label != null ? label.toString() : mPackageName;
                    return TargetInfo.app(title, mPackageName, mPackageName, icon);
                } catch (PackageManager.NameNotFoundException e) {
                    return null;
                }
            }
            if (mShortcutId == null) {
                return null;
            }
            try {
                List<ShortcutInfo> shortcuts = null;
                if (launcherApps != null) {
                    shortcuts = launcherApps.getShortcuts(
                            new LauncherApps.ShortcutQuery()
                                    .setPackage(mPackageName)
                                    .setShortcutIds(java.util.Collections.singletonList(mShortcutId))
                                    .setQueryFlags(
                                            LauncherApps.ShortcutQuery.FLAG_MATCH_ALL_KINDS_WITH_ALL_PINNED),
                            UserHandle.of(mUserId));
                    if (shortcuts == null || shortcuts.isEmpty()) {
                        shortcuts = launcherApps.getShortcuts(
                                new LauncherApps.ShortcutQuery()
                                        .setPackage(mPackageName)
                                        .setShortcutIds(java.util.Collections.singletonList(mShortcutId))
                                        .setQueryFlags(
                                                LauncherApps.ShortcutQuery.FLAG_MATCH_ALL_KINDS),
                                UserHandle.of(mUserId));
                    }
                }
                ShortcutInfo shortcutInfo = shortcuts != null && !shortcuts.isEmpty()
                        ? shortcuts.get(0) : null;
                Drawable icon = shortcutInfo != null && launcherApps != null
                        ? launcherApps.getShortcutBadgedIconDrawable(shortcutInfo, densityDpi)
                        : null;
                if (icon == null) {
                    icon = packageManager.getApplicationIcon(mPackageName);
                }
                String title = shortcutInfo != null
                        ? getShortcutStaticTitle(shortcutInfo, mShortcutId)
                        : mShortcutId;
                String summary = appLabels.containsKey(mPackageName)
                        ? appLabels.get(mPackageName) : mPackageName;
                return TargetInfo.shortcut(title, summary, mPackageName, icon, mShortcutId, mUserId);
            } catch (RuntimeException | PackageManager.NameNotFoundException e) {
                try {
                    Drawable fallbackIcon = packageManager.getApplicationIcon(mPackageName);
                    String summary = appLabels.containsKey(mPackageName)
                            ? appLabels.get(mPackageName) : mPackageName;
                    return TargetInfo.shortcut(
                            mShortcutId,
                            summary,
                            mPackageName,
                            fallbackIcon,
                            mShortcutId,
                            mUserId);
                } catch (PackageManager.NameNotFoundException ignored) {
                    return null;
                }
            }
        }

        @Nullable
        private static Integer tryParseInt(@Nullable String value) {
            try {
                return value == null ? null : Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @NonNull
        private static String getShortcutStaticTitle(@NonNull ShortcutInfo shortcutInfo,
                @NonNull String fallbackId) {
            CharSequence shortLabel = shortcutInfo.getShortLabel();
            if (shortLabel != null && shortLabel.length() > 0) {
                return shortLabel.toString();
            }
            CharSequence longLabel = shortcutInfo.getLongLabel();
            if (longLabel != null && longLabel.length() > 0) {
                return longLabel.toString();
            }
            return fallbackId;
        }
    }

    private static final class TargetInfo {
        private final boolean mIsShortcut;
        @NonNull
        private final String mTitle;
        @NonNull
        private final String mSummary;
        @NonNull
        private final String mPackageName;
        @Nullable
        private final Drawable mIcon;
        @Nullable
        private final String mShortcutId;
        private final int mUserId;

        private TargetInfo(boolean isShortcut, @NonNull String title, @NonNull String summary,
                @NonNull String packageName, @Nullable Drawable icon,
                @Nullable String shortcutId, int userId) {
            mIsShortcut = isShortcut;
            mTitle = title;
            mSummary = summary;
            mPackageName = packageName;
            mIcon = icon;
            mShortcutId = shortcutId;
            mUserId = userId;
        }

        @NonNull
        static TargetInfo app(@NonNull String title, @NonNull String summary,
                @NonNull String packageName, @Nullable Drawable icon) {
            return new TargetInfo(false, title, summary, packageName, icon, null,
                    UserHandle.myUserId());
        }

        @NonNull
        static TargetInfo shortcut(@NonNull String title, @NonNull String summary,
                @NonNull String packageName, @Nullable Drawable icon,
                @NonNull String shortcutId, int userId) {
            return new TargetInfo(true, title, summary, packageName, icon, shortcutId, userId);
        }

        @NonNull
        String getKey() {
            return mIsShortcut ? buildShortcutKey(mUserId, mPackageName, mShortcutId) : mPackageName;
        }

        @NonNull
        String getTitle() {
            return mTitle;
        }

        @NonNull
        String getSummary() {
            return mSummary;
        }

        @NonNull
        String getPackageName() {
            return mPackageName;
        }

        @Nullable
        Drawable getIcon() {
            return mIcon;
        }

        boolean matchesQuery(@Nullable String query) {
            if (query == null || query.trim().isEmpty()) {
                return true;
            }
            String normalized = query.trim().toLowerCase();
            if (mTitle.toLowerCase().contains(normalized)
                    || mSummary.toLowerCase().contains(normalized)
                    || mPackageName.toLowerCase().contains(normalized)) {
                return true;
            }
            return mShortcutId != null && mShortcutId.toLowerCase().contains(normalized);
        }

        @NonNull
        String toStoredValue() {
            if (!mIsShortcut) {
                return mPackageName;
            }
            return ENTRY_PREFIX_SHORTCUT + mUserId + ENTRY_FIELD_SEPARATOR
                    + Uri.encode(mPackageName) + ENTRY_FIELD_SEPARATOR
                    + Uri.encode(mShortcutId);
        }
    }

    private final class TargetAdapter extends RecyclerView.Adapter<TargetViewHolder> {
        private final ArrayList<TargetInfo> mItems = new ArrayList<>();
        private final TargetSelectionListener mSelectionListener;
        @Nullable
        private String mSelectedKey;

        TargetAdapter(@NonNull TargetSelectionListener selectionListener) {
            mSelectionListener = selectionListener;
        }

        @NonNull
        @Override
        public TargetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_moment_arc_target, parent, false);
            return new TargetViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TargetViewHolder holder, int position) {
            holder.bind(mItems.get(position), mSelectionListener, mSelectedKey);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        void submitList(@NonNull List<TargetInfo> items, @Nullable String selectedKey) {
            mItems.clear();
            mItems.addAll(items);
            mSelectedKey = selectedKey;
            notifyDataSetChanged();
        }

        void updateSelection(@Nullable String selectedKey) {
            if ((mSelectedKey == null && selectedKey == null)
                    || (mSelectedKey != null && mSelectedKey.equals(selectedKey))) {
                return;
            }
            mSelectedKey = selectedKey;
            notifyDataSetChanged();
        }
    }

    private final class TargetViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIcon;
        private final TextView mTitle;
        private final View mIndicator;

        TargetViewHolder(@NonNull View itemView) {
            super(itemView);
            mIcon = itemView.findViewById(R.id.target_icon);
            mTitle = itemView.findViewById(R.id.target_title);
            mIndicator = itemView.findViewById(R.id.target_indicator);
        }

        void bind(@NonNull TargetInfo target, @NonNull TargetSelectionListener selectionListener,
                @Nullable String selectedKey) {
            boolean isSelected = target.getKey().equals(selectedKey);
            mIcon.setImageDrawable(target.getIcon());
            mIcon.setContentDescription(target.getTitle());
            mTitle.setText(target.getTitle());
            mTitle.setTextColor(getColor(isSelected
                    ? R.color.settingslib_materialColorPrimary
                    : R.color.settingslib_materialColorOnSurface));
            mIndicator.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
            itemView.setActivated(isSelected);
            itemView.setOnClickListener(v -> selectionListener.onTargetSelected(target));
            itemView.setOnLongClickListener(v -> {
                ClipData clipData = ClipData.newPlainText(target.getTitle(), target.getKey());
                v.startDragAndDrop(clipData, new View.DragShadowBuilder(v), target, 0);
                return true;
            });
        }
    }

    private interface TargetSelectionListener {
        void onTargetSelected(@NonNull TargetInfo target);
    }
}

package com.shashi.castlematic.features.user_management;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class UserManagementPagerAdapter extends FragmentStateAdapter {

    private final boolean canAddUser;

    public UserManagementPagerAdapter(@NonNull FragmentActivity fragmentActivity, boolean canAddUser) {
        super(fragmentActivity);
        this.canAddUser = canAddUser;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new AddDriverFragment();
        } else {
            return new AddUserFragment();
        }
    }

    @Override
    public int getItemCount() {
        // Show both tabs if user can add users (super admin), otherwise only show driver tab
        return canAddUser ? 2 : 1;
    }
}

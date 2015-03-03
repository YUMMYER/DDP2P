/* Copyright (C) 2014,2015 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
Florida Tech, Human Decision Support Systems Laboratory
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation; either the current version of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. */
/* ------------------------------------------------------------------------- */

package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingUpdate extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
		getActivity().getActionBar().setTitle("Update Setting");
		View v = inflater.inflate(R.layout.setting_update, null);

		ViewPager pager = (ViewPager) v.findViewById(R.id.setting_update_viewpager);
		pager.setAdapter(new SettingUpdateAdapter(getActivity().getSupportFragmentManager()));
		return v;
	}

	private class SettingUpdateAdapter extends FragmentPagerAdapter {

		public SettingUpdateAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);

		}

		@Override
		public Fragment getItem(int pos) {
			
			 switch (pos) { 
			 case 0: 
				 return KnownTester.newInstance(); 
			 case 1: 
				 return UsedTester.newInstance();
		     }
			 
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}

	}
}

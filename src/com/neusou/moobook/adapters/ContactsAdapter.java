package com.neusou.moobook.adapters;

import android.app.Activity;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.widget.Filter;
import android.widget.FilterQueryProvider;
import android.widget.Filterable;

import com.neusou.moobook.controller.BaseListViewFactory;
import com.neusou.moobook.controller.CursorListAdapter;

public class ContactsAdapter extends CursorListAdapter implements Filterable{
	static final String LOG_TAG = "ContactsAdapter";
	
	FilterQueryProvider filterQueryProvider;
	Cursor mNoFilterData;
	DataSetObserver mDataSetObserver = new DataSetObserver() {
		@Override
		public void onChanged() {		
			super.onChanged();
			notifyDataSetChanged();
		}
	}; 
	
	public DataSetObserver getObserver(){
		 return mDataSetObserver;		
	}
	
	public ContactsAdapter(Activity ctx, BaseListViewFactory<Cursor> factory,
			int primaryKeyIndex) {
		super(ctx, factory, primaryKeyIndex);
		
		mFilter = new MyFilter();
	}
			
	public void setFilterQueryProvider(FilterQueryProvider filterQueryProvider) {
		this.filterQueryProvider = filterQueryProvider;
	}
	
	public FilterQueryProvider getFilterQueryProvider() {
		return filterQueryProvider;
	}
	
	class MyFilter extends Filter
	{
		Cursor resultCursor = null;
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			mNoFilterData = mData;			
			FilterResults result = new FilterResults();
			
			Object values = null;			
			int numRows = 0;
			
			if(constraint != null && filterQueryProvider != null){
				resultCursor = filterQueryProvider.runQuery(constraint);
				if(resultCursor != null){
					numRows = resultCursor.getCount();
					values = resultCursor;
				}
			}
			
			result.count = numRows;
			result.values = values;
			
			return result;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			if(constraint != null && results.count > 0){
				mData = (Cursor) results.values;
			}
			else{
				mData = mNoFilterData;
			}			
		}
		
	}

	Filter mFilter;
	
	@Override
	public Filter getFilter() {
		return mFilter;
	}
	
	
	
}
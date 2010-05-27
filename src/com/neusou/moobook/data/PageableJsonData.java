package com.neusou.moobook.data;

import org.json.JSONArray;
import org.json.JSONException;

import com.neusou.moobook.adapters.PageableDataStore;

public class PageableJsonData implements PageableDataStore<JSONArray>{
		JSONArray data;
		public void set(JSONArray data){
			this.data =data;
		}
		
		@Override
		public void clear() {
			data = null;
		}
		
		public JSONArray get(int index) {
			return data;			
		}
		
		@Override
		public Object getAt(int index) {
			if(data == null){
				return null;
			}		
			try {
				return data.get(index);
			} catch (JSONException e) {
				
			}	
			
			return null;
			
		}

		@Override
		public int size() {
			if(data == null){
				return 0;
			}
			return data.length();
		}
		
	};
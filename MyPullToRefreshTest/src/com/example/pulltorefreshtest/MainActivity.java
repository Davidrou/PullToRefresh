package com.example.pulltorefreshtest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends Activity {

	ArrayAdapter<String> adapter;
	ListView lv;
	String[] items = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L" };
	MyRefreshableView refreshableView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
	}
  
	void initView(){
		lv=(ListView)findViewById(R.id.list_view);
		refreshableView=(MyRefreshableView)findViewById(R.id.refreshView);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
		lv.setAdapter(adapter);
		refreshableView.setOnRefreshListener(new MyRefreshableView.PullToRefreshListener() {
			@Override
			public void onRefresh() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}
 
}

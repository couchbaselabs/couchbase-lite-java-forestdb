package com.couchbase.cbforest_java_test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClickButton(View view) {
        Log.i(TAG, "[onClickButton(View)] START");
        try{
        if(view.getId() == R.id.button_run_database_test)
            testDatabase();
        else if(view.getId() == R.id.button_run_versioneddocument_test)
            testVersionedDocument();
        else if(view.getId() == R.id.button_run_collatable_test)
            testCollatable();
        else if(view.getId() == R.id.button_run_index_test)
            testIndex();
        else if(view.getId() == R.id.button_run_mapreduce_test)
            testMapReduce();
        else if(view.getId() == R.id.button_run_all_test)
            testAll();
        } catch (Exception e) {
            Log.e(TAG, "Exceptin", e);
            e.printStackTrace();
        }
        Log.i(TAG, "[onClickButton(View)] END");
    }

    public void testAll() throws Exception {
        for(int i = 0; i < 2; i++) {

            testDatabase();
            testVersionedDocument();
            testIndex();
            testMapReduce();
            testCollatable();

        }
    }
    public void testCollatable() throws Exception {
        CollatableTest test = new CollatableTest(this);
        test.test();
    }

    public void testDatabase() throws Exception {
        DatabaseTest test = new DatabaseTest(this);
        test.test();
    }

    public void testVersionedDocument() throws Exception {
        VersionedDocumentTest test = new VersionedDocumentTest(this);
        test.test();
    }

    public void testIndex() throws Exception {
        IndexTest test = new IndexTest(this);
        test.test();
    }

    public void testMapReduce() throws Exception {
        MapReduceTest test = new MapReduceTest(this);
        test.test();
    }

}

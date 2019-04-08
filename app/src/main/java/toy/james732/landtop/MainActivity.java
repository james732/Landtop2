package toy.james732.landtop;

import android.app.AlertDialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import androidx.appcompat.app.AppCompatActivity;

class DownloadPhoneTask extends AsyncTask<File, Integer, AllPhones>
{
    AllPhones allPhones;
    private MainActivity mainActivity;

    DownloadPhoneTask(MainActivity mainActivity) {
        super();
        this.mainActivity = mainActivity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(AllPhones allPhones) {
        if (allPhones.getResult() == null) {
            mainActivity.descriptText.setText("無法取得資料，網路可能有問題");
        } else {
            switch (allPhones.getResult()) {
                case NoOldData:
                    mainActivity.show_toast("初次執行");
                    break;
                case Updated:
                    mainActivity.show_toast("網頁有更新");
                    mainActivity.showType = PhoneItem.Type.None;
                    break;
                case NoUpdated:
                    mainActivity.show_toast("網頁沒有更新");
                    break;
                case Error:
                    mainActivity.show_toast("發生錯誤");
                    break;
            }
            /* currentShow = allPhones; */
            this.allPhones = allPhones;
            mainActivity.updateList();
            mainActivity.adapter.notifyDataSetChanged();
            mainActivity.isBusy = false;
        }
    }

    @Override
    protected AllPhones doInBackground(File... params) {
        return new AllPhones(params[0]);
    }
}

public class MainActivity extends AppCompatActivity {
    static final String LOG_TAG = "LANDTOP";
    static final String LOG_TAG_EXCEPTION = "LANDTOP_EXCEPTION";

    ArrayList<HashMap<String, Object>> listForView = new ArrayList<>();
    ArrayList<PhoneItem> currentShow;
    SimpleAdapter adapter;

    boolean isBusy;
    TextView descriptText;

    PhoneItem.Type showType = PhoneItem.Type.Phone;
    PhoneCompany showCompany = null;

    DownloadPhoneTask downloadPhoneTask;

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } else {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ListView listView = findViewById(R.id.myListView);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        descriptText = findViewById(R.id.descript_text);

        if (isConnected()) {
            descriptText.setText("忙碌中請稍後......");
            isBusy = true;

            adapter = new SimpleAdapter(
                    getApplicationContext(),
                    listForView,
                    R.layout.list,
                    new String[]{ "PhoneName", "PhonePrice" },
                    new int[] { R.id.textView1, R.id.textView2 });
            listView.setAdapter(adapter);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                    PhoneItem selectItem = currentShow.get(position);
                    AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                    b.setTitle(selectItem.name);
                    b.setMessage(selectItem.getAllPriceString());
                    b.create().show();
            });

            listView.setOnItemLongClickListener((adapterView, view, position, l) -> {
                    view.setSelected(true);
                    return true;
            });

            findViewById(R.id.forRefOnly).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("關於*的說明")
                            .setMessage("價格有*代表該價錢為估算而來。估算方式為：其他手機的空機價-其他手機的補助價+該手機的補助價。")
                            .show();
                }
            });

            downloadPhoneTask = new DownloadPhoneTask(this);
            downloadPhoneTask.execute(getFilesDir());
        } else {
            descriptText.setText("無網路連線...");
        }
    } /* onCreate */

    @Override
    protected void onStop() {
        super.onStop();
    }

    void updateList() {
        StringBuilder sb = new StringBuilder();
        ArrayList<PhoneItem> tmpList;

        if (showType != PhoneItem.Type.None) {
            if (showCompany == null) {
                tmpList = downloadPhoneTask.allPhones.getByType(showType);
                sb.append("所有廠牌");
            }
            else {
                tmpList = downloadPhoneTask.allPhones.getByCompany(showCompany, showType);
                sb.append(showCompany.name);
            }

            sb.append(" 的 ");
            if (showType == PhoneItem.Type.Phone) {
                sb.append("手機");
            } else if (showType == PhoneItem.Type.Tablet) {
                sb.append("平板");
            } else {
                sb.append("其他");
            }

            if (currentShow == null)
                currentShow = new ArrayList<>();
            else
                currentShow.clear();

            currentShow = new ArrayList<>(tmpList);
        } else {
            sb.append("價格有變");
            currentShow = new ArrayList<>(downloadPhoneTask.allPhones.getDiffList());
        }
        Collections.sort(currentShow, (PhoneItem lhs, PhoneItem rhs) ->
            Integer.compare(rhs.getFinalPrice(), lhs.getFinalPrice()));

        listForView.clear();

        for (PhoneItem pp : currentShow) {
            HashMap<String, Object> hashMap = new HashMap<>();

            if (showCompany == null)
                hashMap.put("PhoneName", pp.company.name + " " + pp.name);
            else
                hashMap.put("PhoneName", pp.name);

            hashMap.put("PhonePrice", pp.getPriceString());
            listForView.add(hashMap);
        }
        adapter.notifyDataSetChanged();
        descriptText.setText(sb.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (isBusy)
            return true;

        int id = item.getItemId();
        item.setChecked(true);

        switch (id) {
            case R.id.show_all_comp:
                showCompany = null;
                updateList();
                return true;

            case R.id.show_apple:
                showCompany = PhoneCompany.Apple;
                updateList();
                return true;

            case R.id.show_asus:
                showCompany = PhoneCompany.ASUS;
                updateList();
                return true;

            case R.id.show_htc:
                showCompany = PhoneCompany.HTC;
                updateList();
                return true;

            case R.id.show_lg:
                showCompany = PhoneCompany.LG;
                updateList();
                return true;

            case R.id.show_samsung:
                showCompany = PhoneCompany.SAMSUNG;
                updateList();
                return true;

            case R.id.show_sony:
                showCompany = PhoneCompany.Sony;
                updateList();
                return true;

            case R.id.show_huawei:
                showCompany = PhoneCompany.HUAWEI;
                updateList();
                return true;

            case R.id.show_oppo:
                showCompany = PhoneCompany.OPPO;
                updateList();
                return true;

            case R.id.show_mi:
                showCompany = PhoneCompany.MI;
                updateList();
                return true;

            case R.id.show_moto:
                showCompany = PhoneCompany.MOTO;
                updateList();
                return true;

            case R.id.show_vivo:
                showCompany = PhoneCompany.VIVO;
                updateList();
                return true;

            case R.id.show_nokia:
                showCompany = PhoneCompany.NOKIA;
                updateList();
                return true;

//            case R.id.show_infocus:
//                showCompany = PhoneCompany.INFOCUS;
//                updateList();
//                return true;
//
//            case R.id.show_google:
//                showCompany = PhoneCompany.Google;
//                updateList();
//                return true;
//
//            case R.id.show_blackberry:
//                showCompany = PhoneCompany.Blackberry;
//                updateList();
//                return true;
//
//            case R.id.show_oneplus:
//                showCompany = PhoneCompany.OnePlus;
//                updateList();
//                return true;

            case R.id.show_pad:
                showType = PhoneItem.Type.Tablet;
                updateList();
                return true;

            case R.id.show_phone:
                showType = PhoneItem.Type.Phone;
                updateList();
                return true;

            case R.id.show_money_diff:
                showType = PhoneItem.Type.None;
                updateList();
                return true;

            case R.id.show_others:
                showType = PhoneItem.Type.Others;
                updateList();
                return true;

//            case R.id.show_delete:
//                updateList();
//                return true;

            case R.id.history_size:
                showHistorySize();
                return true;

            case R.id.history_clear:
                deleteHistory();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void show_toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    void showHistorySize() {
        AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
        b.setTitle("歷史檔案大小");
        b.setMessage(downloadPhoneTask.allPhones.getHistorySizeString());
        b.show();
    }

    void deleteHistory() {
        if (downloadPhoneTask.allPhones.deleteHistory()) {
            show_toast("歷史檔案已刪除");
        } else {
            show_toast("歷史檔案不存在");
        }
    }
}
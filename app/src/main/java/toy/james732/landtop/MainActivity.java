package toy.james732.landtop;

import android.app.AlertDialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Date;

class PhonePriceHistory {
    PhonePriceHistory(int p, Date d) {
        price = p;
        date = d;
    }

    void update_price_today(int price, Date today) {
        if (date.equals(today)) {
            if (price < this.price) {
                this.price = price;
            }
        }
    }

    int price;
    Date date;
}

class PhoneItem {
    String companyName;
    String phoneName;
    Type type;
    ArrayList<PhonePriceHistory> priceHistories;

    enum Type {
        None,
        Phone,
        Tablet,
        Others,
        Deleted,
    }

    PhoneItem(String company, String phone, int price, Date today) {
        companyName = company;
        phoneName = phone;

        priceHistories = new ArrayList<>();
        priceHistories.add(new PhonePriceHistory(price, today));

        type = check_type();
    }

    @Override
    public String toString() {
        return phoneName;
    }

    private PhonePriceHistory get_final_history() {
        return priceHistories.get(priceHistories.size() - 1);
    }

    int get_final_price() {
        return get_final_history().price;
    }

    Date get_final_date() {
        return get_final_history().date;
    }

    String getPriceString() {
        if (get_final_price() != -1) {
            StringBuilder sb = new StringBuilder("$ ");
            sb.append(get_final_price());

            if (priceHistories.size() > 1) {
                int prevPrice = priceHistories.get(0).price;
                sb.append(" ");

                for (int i = 1; i < priceHistories.size(); i++) {
                    int currPrice = priceHistories.get(i).price;

                    if (prevPrice == currPrice) {
                        sb.append("－");
                    } else {
                        if (prevPrice < currPrice) {
                            sb.append("↗");
                        } else {
                            sb.append("↘");
                        }
                    }

                    prevPrice = currPrice;
                }

            }
            return sb.toString();
        } else {
            return "已下架";
        }

    }

    private Type check_type() {
        String upperStr = phoneName.toUpperCase();

        if (upperStr.contains("PADFONE")) {
            return Type.Phone;
        } else if (upperStr.contains("TAB")
                || upperStr.contains("PAD")
                || upperStr.contains("TABLET")
                || upperStr.contains("NOKIA N1")) {
            return Type.Tablet;
        } else if (upperStr.contains("WATCH")
                || upperStr.contains("ＷATCH")
                || upperStr.contains("R100")
                || upperStr.contains("R105")
                || upperStr.contains("VR")) {
            return Type.Others;
        } else {
            return Type.Phone;
        }
    }

    void check_and_update_price(int price, Date today) {
        get_final_history().update_price_today(price, today);
    }
}

public class MainActivity extends AppCompatActivity {
    private final String LOG_TAG = "LANDTOP";
    private final String ALL_PHONE_FILE_NAME = "landtop_V2.txt";

    /* 儲存所有手機，包括歷史的記錄 */
    private HashMap<String, PhoneItem> all_phone_items_map = new HashMap<>();

    /* 差異記錄 */
    private ArrayList<PhoneItem> diff_list = new ArrayList<>();

    private Handler uiHandler = new Handler();

    private HashMap<String, PhoneCompany> compsMap = new HashMap<>();
    ArrayList<HashMap<String, Object>> listForView = new ArrayList<>();
    ArrayList<PhoneItem> all_phones;
    ArrayList<PhoneItem> all_tablet;
    ArrayList<PhoneItem> all_others;
    ArrayList<PhoneItem> all_items;
    ArrayList<PhoneItem> all_deleted;
    ArrayList<PhoneItem> currentShow;
    SimpleAdapter adapter;

    boolean is_busy;
    TextView descript_text;

    PhoneItem.Type showType = PhoneItem.Type.Phone;
    String showCompany = "";

    /* read old data from json file to 'all_phone_items_map' list */
    void read_old_data() {
        File path = getFilesDir();
        File file = new File(path, ALL_PHONE_FILE_NAME);

        if (file.exists() && !file.isDirectory()) {
            PhoneItem[] item_array = null;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String json = sb.toString();
                item_array = new Gson().fromJson(
                        json, PhoneItem[].class);

                for (PhoneItem item : item_array) {
                    all_phone_items_map.put(item.phoneName, item);
                }
            } catch (IOException e) {
                Log.d(LOG_TAG, "read_old_data : " + e.getMessage());
            }
        }
    }

    void save_all_data() {
        File path = getFilesDir();
        File file = new File(path, ALL_PHONE_FILE_NAME);

        if (file.exists())
            file.delete();

        PhoneItem[] item_array = all_phone_items_map.values().toArray(
                new PhoneItem[all_phone_items_map.values().size()]
        );

        try {
            String json = new Gson().toJson(item_array);
            FileOutputStream o = new FileOutputStream(file);
            o.write(json.getBytes());
            o.close();
        } catch (IOException e) {
            Log.d(LOG_TAG, "save_all_data : " + e.getMessage());
        }
    }

    void show_history_size() {
        File path = getFilesDir();
        File file = new File(path, ALL_PHONE_FILE_NAME);

        AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
        b.setTitle("歷史檔案大小");

        if (file.exists()) {
            long size = file.length();

            b.setMessage(new StringBuilder()
                .append(size)
                .append(" byte\n")
                .append(Double.toString(size / 1024.0))
                .append(" KB\n")
                .append(Double.toString(size / 1024.0 / 1024.0))
                .append(" MB\n").toString());
        } else {
            b.setMessage("歷史檔案不存在");
        }
        b.show();
    }

    void delete_history() {
        File path = getFilesDir();
        File file = new File(path, ALL_PHONE_FILE_NAME);

        if (file.exists()) {
            file.delete();
            Toast.makeText(this, "歷史資訊已刪除", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "沒有歷史資訊", Toast.LENGTH_LONG).show();
        }
    }

    private boolean is_connected() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ListView listView = (ListView)findViewById(R.id.myListView);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        descript_text = (TextView)findViewById(R.id.descript_text);

        if (is_connected()) {
            descript_text.setText("忙碌中請稍後......");
            is_busy = true;

            HandlerThread thread = new HandlerThread("jsoup");
            thread.start();

            adapter = new SimpleAdapter(
                    getApplicationContext(),
                    listForView,
                    R.layout.list,
                    new String[]{ "PhoneName", "PhonePrice" },
                    new int[] { R.id.textView1, R.id.textView2 });
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new ListView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    PhoneItem selectItem = currentShow.get(position);

                    AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                    b.setTitle(selectItem.phoneName);

                    StringBuilder sb = new StringBuilder();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

                    for (PhonePriceHistory history : selectItem.priceHistories) {
                        sb.append(sdf.format(history.date));
                        sb.append(" $ ");
                        sb.append(history.price);
                        sb.append("\n");
                    }

                    b.setMessage(sb.toString());
                    b.create().show();
                }
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
                    view.setSelected(true);
                    return true;
                }
            });

        /* 另一個thread負責抓網頁與處理資訊 */
            Handler threadHandler = new Handler(thread.getLooper());
            threadHandler.post(new Runnable() {
                @Override
                public void run() {
                    parse();
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            currentShow = all_phones;
                            updateList();
                            adapter.notifyDataSetChanged();
                            is_busy = false;
                        }
                    });
                } /* run() */
            }); /* threadHandler.post */
        } else {
            descript_text.setText("無網路連線...");
        }
    } /* onCreate */

    @Override
    protected void onStop() {
        super.onStop();
    }

    Document get_and_parse(String url) throws IOException, InterruptedException {
        Connection.Response response = null;
        while (true) {
            response = Jsoup.connect(url).execute();
            if (response.statusCode() == 200)
                return response.parse();
            Thread.sleep(1000);
        }
    }

    int parse_money_string(String money) {
        if (money.equals("特價中"))
            return -1;

        if (money.startsWith("$ "))
            money = money.replace("$ ", "");
        else if (money.startsWith("$"))
            money = money.replace("$", "");

        try {
            return Integer.parseInt(money);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void parse_products(ArrayList<PhoneItem> phoneList,
                                HashMap<String, PhoneItem> phoneMap,
                                Date today) throws IOException, InterruptedException {
        Document doc = get_and_parse("http://www.landtop.com.tw/products.php?types=1");
        Elements companies = doc.getElementsByTag("table");

        for (int i = 0; i < companies.size(); i++) {
            Element company = companies.get(i);
            Elements phones = company.getElementsByTag("tr");

            String companyPic = phones.get(0).child(0).child(1).attr("src");
            PhoneCompany phoneCompany = PhoneCompany.getCompanyFromPicture(companyPic);
            compsMap.put(phoneCompany.companyName, phoneCompany);

            for (int j = 1; j < phones.size(); j++) {
                Element phone = phones.get(j);
                String name = phone.child(0).child(0).child(0).text();
                String money = phone.child(1).text();

                int money_value = parse_money_string(money);
                if (money_value == -1)
                    continue;

                if (phoneMap.containsKey(name)) {
                    phoneMap.get(name).check_and_update_price(money_value, today);
                } else {
                    PhoneItem pi = new PhoneItem(
                            phoneCompany.companyName,
                            name,
                            money_value,
                            today
                    );

                    phoneList.add(pi);
                    phoneMap.put(name, pi);
                }
            }
        }
    }

    private void parse_top50(ArrayList<PhoneItem> phoneList,
                             HashMap<String, PhoneItem> phoneMap,
                             Date today) throws IOException, InterruptedException {
        Document doc = get_and_parse("http://www.landtop.com.tw/landtop50_new.php");
        Elements companies = doc.getElementsByTag("table");
        int reduce_diff = -1;

        /* 每家手機是一個 table */
        for (int i = 0; i < companies.size(); i++) {
            /* 這家廠商的每一隻 */
            Elements phones = companies.get(i).getElementsByTag("tr");

            /* 第一格是廠商的資訊 */
            /* <tr><td><img src="..."> */
            String companyPic = phones.get(0).child(0).child(0).attr("src");

            PhoneCompany phoneCompany = PhoneCompany.getCompanyFromPicture(companyPic);
            if (!compsMap.containsKey(phoneCompany.companyName)) {
                compsMap.put(phoneCompany.companyName, phoneCompany);
            }

            for (int j = 1; j < phones.size(); j++) {
                Element phone = phones.get(j);
                    /* <tr>
                    *  <td><h5><a>phone name</a></h5></td>
                    *  <td>單機價</td>
                    *  </tr>
                    **/
                String name  = phone.child(0).child(0).child(0).text();
                String money = phone.child(1).text();
                String reduce = phone.child(2).text();

                int money_value = parse_money_string(money);
                int reduce_value = parse_money_string(reduce);

                if (reduce_diff == -1) {
                    if (money_value != -1 && reduce_value != -1)
                        reduce_diff = money_value - reduce_value;
                }

                if (money_value == -1 && reduce_value != 0 && reduce_diff != -1) {
                    money_value = reduce_value + reduce_diff;
                }
                if (money_value == -1)
                    continue;

                if (phoneMap.containsKey(name)) {
                    /* 已經存在這一筆資料 */
                    phoneMap.get(name).check_and_update_price(money_value, today);
                } else {
                    PhoneItem pi = new PhoneItem(
                            phoneCompany.companyName,
                            name,
                            money_value,
                            today
                    );

                    phoneList.add(pi);
                }
            }
        }
    }

    private void parse() {
        Date today = Calendar.getInstance().getTime();
        ArrayList<PhoneItem> phoneList = new ArrayList<>();

        compsMap.clear();

        HashMap<String, PhoneItem> phoneMap = new HashMap<>();

        try {
            parse_products(phoneList, phoneMap, today);
            parse_top50(phoneList, phoneMap, today);
            updateNewPhone(phoneList, today);
        } catch (Exception e) {
            Log.d(LOG_TAG, "Exception in Parse: " + e.getMessage());
            Log.d(LOG_TAG, Log.getStackTraceString(e));
        }
    } /* parse() */

    private void update_new_phones(ArrayList<PhoneItem> phoneList, Date today) {
        if (all_phone_items_map.size() != 0) {
            HashMap<String, PhoneItem> new_phone_map = new HashMap<>();

            for (PhoneItem new_item : phoneList) {
                String name = new_item.phoneName;

                if (all_phone_items_map.containsKey(name)) {
                    PhoneItem old_item;
                    /* 舊有的手機，比較價格 */
                    old_item = all_phone_items_map.get(name);
                    int prev_price = old_item.get_final_price();
                    int now_price = new_item.get_final_price();

                    if (prev_price != now_price) {
                        /* 價格改變了，把新的價格-日期的項目加入old_item */
                        PhonePriceHistory newPriceItem = new_item.priceHistories.get(0);
                        old_item.priceHistories.add(newPriceItem);
                        diff_list.add(old_item);
                    }
                    new_phone_map.put(name, old_item);

                    /* 舊有的，新的也有的，先移除掉 */
                    all_phone_items_map.remove(name);
                } else {
                    /* 新上架 */
                    diff_list.add(new_item);
                    new_phone_map.put(name, new_item);
                }
            }

            /* 處理已經下架的手機們 */
            for (String s : all_phone_items_map.keySet()) {
                PhoneItem item = all_phone_items_map.get(s);

                if (item.get_final_price() != -1) {
                    item.priceHistories.add(new PhonePriceHistory(-1, today));
                    new_phone_map.put(s, item);
                    diff_list.add(item);
                }
            }

            for (String s : all_phone_items_map.keySet()) {
                PhoneItem item = all_phone_items_map.get(s);
                if (item.get_final_price() == -1) {
                    all_deleted.add(item);
                }
            }

            all_phone_items_map = new_phone_map;

            if (diff_list.size() != 0) {
                Toast.makeText(this, "網頁有更新", Toast.LENGTH_LONG).show();
                save_all_data();

                /* 如果網頁有更新，就只先顯示有改變的列表 */
                showType = PhoneItem.Type.None;
            } else {
                Toast.makeText(this, "網頁沒有更新", Toast.LENGTH_LONG).show();
            }
        } else /* if (all_phone_items_map.size() == 0) */ {
            Toast.makeText(this, "初次執行", Toast.LENGTH_LONG).show();
            for (PhoneItem pi : phoneList) {
                all_phone_items_map.put(pi.phoneName, pi);
            }
            save_all_data();
        }
    }

    private void updateNewPhone(ArrayList<PhoneItem> phoneList, Date today) {
        read_old_data();

        /* 分類 */
        all_phones  = new ArrayList<>();
        all_tablet  = new ArrayList<>();
        all_others  = new ArrayList<>();
        all_items   = new ArrayList<>();
        all_deleted = new ArrayList<>();

        update_new_phones(phoneList, today);

        for (PhoneItem pi : all_phone_items_map.values()) {
            switch (pi.type) {
                case Phone:
                    all_phones.add(pi);
                    break;

                case Tablet:
                    all_tablet.add(pi);
                    break;

                case Others:
                    all_others.add(pi);
                    break;
            }
            all_items.add(pi);

            if (compsMap.containsKey(pi.companyName))
                compsMap.get(pi.companyName).phones.add(pi);
            else
                compsMap.get(PhoneCompany.UNKNOWN).phones.add(pi);
        }
    }

    private int IntegerCompare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    private void updateList() {
        StringBuilder sb = new StringBuilder();
        ArrayList<PhoneItem> tmpList;
        boolean sort_by_money = true;

        if (showType == PhoneItem.Type.Deleted) {
            sb.append("已下架: ");
            sb.append(all_deleted.size());
            currentShow = new ArrayList<>(all_deleted);
            sort_by_money = false;
        } else if (showType != PhoneItem.Type.None) {
            if (compsMap.containsKey(showCompany)) {
                tmpList = compsMap.get(showCompany).phones;
                sb.append(showCompany);
            }  else {
                tmpList = all_items;
                sb.append("所有廠牌");
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

            for (PhoneItem pi : tmpList) {
                if (pi.type == showType) {
                    currentShow.add(pi);
                }
            }
        } else {
            sb.append("價格有變");
            currentShow = new ArrayList<>(diff_list);
        }
        if (sort_by_money) {
            Collections.sort(currentShow, new Comparator<PhoneItem>() {
                @Override
                public int compare(PhoneItem lhs, PhoneItem rhs) {
                    return IntegerCompare(rhs.get_final_price(), lhs.get_final_price());
                }
            });
        } else {
            Collections.sort(currentShow, new Comparator<PhoneItem>() {
                @Override
                public int compare(PhoneItem lhs, PhoneItem rhs) {
                    return lhs.get_final_date().after(rhs.get_final_date()) ? 1 : -1;
                }
            });
        }

        listForView.clear();

        for (PhoneItem pp : currentShow) {
            HashMap<String, Object> hashMap = new HashMap<>();

            if (showCompany.equals(""))
                hashMap.put("PhoneName", pp.companyName + " " + pp.phoneName);
            else
                hashMap.put("PhoneName", pp.phoneName);

            hashMap.put("PhonePrice", pp.getPriceString());
            listForView.add(hashMap);
        }

        adapter.notifyDataSetChanged();
        descript_text.setText(sb.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
/*
        MenuItem item = menu.findItem(R.id.show_money_diff);

        if (diff_list.isEmpty()) {
            item.setEnabled(false);
        } else {
            if (currentShow == diff_list) {
                item.setChecked(true);
            }
        }
*/
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (is_busy)
            return true;

        int id = item.getItemId();
        item.setChecked(true);

        switch (id) {
            case R.id.show_all_comp:
                showCompany = "";
                updateList();
                return true;

            case R.id.show_apple:
                showCompany = PhoneCompany.APPLE;
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
                showCompany = PhoneCompany.SONY;
                updateList();
                return true;

            case R.id.show_infocus:
                showCompany = PhoneCompany.INFOCUS;
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

            case R.id.show_delete:
                showType = PhoneItem.Type.Deleted;
                updateList();
                return true;

            case R.id.history_size:
                show_history_size();
                return true;

            case R.id.history_clear:
                delete_history();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

class PhoneCompany {
    String companyName;
    ArrayList<PhoneItem> phones = new ArrayList<>();

    static final String APPLE = "Apple";
    static final String SONY = "Sony";
    static final String GSMART = "GSmart";
    static final String HTC = "HTC";
    static final String INHON = "Inhon";
    static final String BENTEN = "Benten";
    static final String HUAWEI = "HUAWEI";
    static final String ASUS = "ASUS";
    static final String SAMSUNG = "SAMSUNG";
    static final String INFOCUS = "InFocus";
    static final String NOKIA = "NOKIA";
    static final String OPPO = "OPPO";
    static final String LG = "LG";
    static final String MTO = "MTO";
    static final String COOLPAD = "COOLPAD";
    static final String MI = "MI";
    static final String TWM = "TWM";
    static final String SHARP = "SHARP";
    static final String MEITU = "美圖";
    static final String MOTO = "MOTO";
    static final String SUGAR = "SUGAR";
    static final String UNKNOWN = "UNKNOWN";

    /* 地標圖示與公司名稱的map */
    private static HashMap<String, String> companyPicMap = new HashMap<>();
    static  {
        companyPicMap.put("images/prodpt/844pSa.jpg", APPLE);
        companyPicMap.put("images/prodpt/051k76.jpg", SONY);
        companyPicMap.put("images/prodpt/097SwZ.jpg", GSMART);
        companyPicMap.put("images/prodpt/149vbE.jpg", HTC);
        companyPicMap.put("images/prodpt/167sKc.jpg", INHON);
        companyPicMap.put("images/prodpt/414ZPQ.jpg", BENTEN);
        companyPicMap.put("images/prodpt/502Fc1.jpg", HUAWEI);
        companyPicMap.put("images/prodpt/586Q6N.jpg", ASUS);
        companyPicMap.put("images/prodpt/593uAM.jpg", SAMSUNG);
        companyPicMap.put("images/prodpt/697qw3.jpg", INFOCUS);
        companyPicMap.put("images/prodpt/751lXt.jpg", NOKIA);
        companyPicMap.put("images/prodpt/881IQv.jpg", OPPO);
        companyPicMap.put("images/prodpt/888Pyt.jpg", LG);
        companyPicMap.put("images/prodpt/957X0d.jpg", MTO);
        companyPicMap.put("images/prodpt/886lV7.jpg", COOLPAD);
        companyPicMap.put("images/prodpt/734wY8.gif", MI);
        companyPicMap.put("images/prodpt/084DtY.jpg", TWM);
        companyPicMap.put("images/prodpt/319uKq.jpg", SHARP);
        companyPicMap.put("images/prodpt/741o3d.jpg", MEITU);
        companyPicMap.put("images/prodpt/965q1b.jpg", MOTO);
        companyPicMap.put("images/prodpt/7606_L.jpg", SUGAR);
    };

    static private PhoneCompany UnknownCompany = new PhoneCompany();
    static {
        UnknownCompany.companyName = UNKNOWN;
    }

    static PhoneCompany getCompanyFromPicture(String pic_name) {
        if (companyPicMap.containsKey(pic_name)) {
            PhoneCompany comp = new PhoneCompany();
            comp.companyName = companyPicMap.get(pic_name);;
            return comp;
        } else {
            return UnknownCompany;
        }
    }
}
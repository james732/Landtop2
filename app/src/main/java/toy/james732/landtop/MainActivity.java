package toy.james732.landtop;

import android.app.AlertDialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
    PhoneCompany company;
    String name;
    Type type;
    ArrayList<PhonePriceHistory> priceHistories;

    enum Type {
        None,
        Phone,
        Tablet,
        Others,
        Deleted,
    }

    PhoneItem(PhoneCompany company, String name, int price, Date today) {
        this.company = company;
        this.name = name;

        priceHistories = new ArrayList<>();
        priceHistories.add(new PhonePriceHistory(price, today));

        type = check_type();
    }

    @Override
    public String toString() {
        return name;
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
        String upperStr = name.toUpperCase();

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

abstract class ParserBase {
    ArrayList<PhoneItem> phone_list = new ArrayList<>();
    private HashMap<String, PhoneItem> phone_map = new HashMap<>();
    private Date today;

    ParserBase(Date today) {
        this.today = today;
    }

    abstract ArrayList<PhoneItem> parse() throws IOException, InterruptedException;

    protected Document get_and_parse(String url) throws IOException, InterruptedException {
        Connection.Response response = null;
        while (true) {
            response = Jsoup.connect(url).execute();
            if (response.statusCode() == 200)
                return response.parse();
            Thread.sleep(1000);
        }
    }

    protected void check_add_phone(String name,
                                 int money_value,
                                 PhoneCompany phone_company) {
        if (phone_map.containsKey(name)) {
            phone_map.get(name).check_and_update_price(money_value, today);
        } else {
            PhoneItem pi = new PhoneItem(
                    phone_company,
                    name,
                    money_value,
                    today
            );

            phone_list.add(pi);
            phone_map.put(name, pi);
        }
    }
}

class ExpansysParser extends ParserBase {
    public ExpansysParser(Date today) {
        super(today);
    }

    @Override
    ArrayList<PhoneItem> parse() throws IOException, InterruptedException {
        parse_expansys();
        return phone_list;
    }

    private void parse_expansys_doc(Document doc) {
        Elements products = doc.select("div.productGrid").select("ul");

        for (int i = 0; i < products.size(); i++) {
            String name = products.get(i).select("li.title").text();
            String price = products.get(i).select("li.price").text();

            int first_space = name.indexOf(' ');
            String company_name = name.substring(0, first_space);
            String phone_name = name.substring(first_space + 1);

            if (company_name.equals("B-Stock")) {
                first_space = phone_name.indexOf(' ');
                company_name = phone_name.substring(0, first_space);
                phone_name = phone_name.substring(first_space + 1) + " [B-Stock]";
            }

            PhoneCompany phone_company = PhoneCompany.get_company_by_name(company_name);

            int price_value = 0;
            try {
                String s = price.replace("NT$", "").replace(",", "");
                price_value = Integer.parseInt(s);
            } catch (NumberFormatException e) {

            }
            check_add_phone(phone_name, price_value, phone_company);
        }
    }

    private int get_expansys_phone_pages() throws IOException, InterruptedException {
        Document doc = get_and_parse("http://www.expansys-tw.com/mobile-phones/");
        String s = doc.select("li.showing").text();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '頁')
                break;
            sb.append(c);
        }
        parse_expansys_doc(doc);
        return Integer.parseInt(sb.toString());
    }

    private void parse_expansys_page(int page) throws IOException, InterruptedException {
        Document doc = get_and_parse("http://www.expansys-tw.com/mobile-phones/?page=" + String.valueOf(page));
        parse_expansys_doc(doc);
    }

    private void parse_expansys() throws IOException, InterruptedException {
        int pages = get_expansys_phone_pages();
        for (int i = 0; i < pages; i++) {
            parse_expansys_page(i + 1);
        }
    }
}

class LandtopParser extends ParserBase {
    LandtopParser(Date today) {
        super(today);
    }

    @Override
    ArrayList<PhoneItem> parse() throws IOException, InterruptedException {
        parse_products();
        parse_top50();
        return phone_list;
    }

    private int parse_money_string(String money) {
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

    private void parse_products() throws IOException, InterruptedException {
        Document doc = get_and_parse("http://www.landtop.com.tw/products.php?types=1");
        Elements companies = doc.getElementsByTag("table");

        for (int i = 0; i < companies.size(); i++) {
            Element company = companies.get(i);
            Elements phones = company.getElementsByTag("tr");

            String company_pic = phones.get(0).child(0).child(1).attr("src");
            PhoneCompany phone_company = PhoneCompany.getCompanyFromPicture(company_pic);

            for (int j = 1; j < phones.size(); j++) {
                Element phone = phones.get(j);
                String name = phone.child(0).child(0).child(0).text();
                String money = phone.child(1).text();

                int money_value = parse_money_string(money);
                if (money_value == -1)
                    continue;

                check_add_phone(name, money_value, phone_company);
            }
        }
    }

    private void parse_top50() throws IOException, InterruptedException {
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

                check_add_phone(name, money_value, phoneCompany);
            }
        }
    }
}

class AllPhones
{
    enum CompareResult {
        NoOldData,
        NoUpdated,
        Updated,
        Error,
    }

    private final String ALL_PHONE_FILE_NAME = "landtop_V2.txt";
    private File file_path;

    private CompareResult result;

    private Date today = Calendar.getInstance().getTime();
    private ArrayList<PhoneItem> all_items = new ArrayList<>();
    private ArrayList<PhoneItem> diff_list = new ArrayList<>();

    private ArrayList<PhoneItem> get_by_type(ArrayList<PhoneItem> items, PhoneItem.Type type) {
        if (items == null)
            return null;

        ArrayList<PhoneItem> ret_items = new ArrayList<>();

        for (PhoneItem pi : items) {
            if (pi.type == type) {
                ret_items.add(pi);
            }
        }

        return ret_items;
    }

    public ArrayList<PhoneItem> get_by_type(PhoneItem.Type type) {
        return get_by_type(all_items, type);
    }

    public ArrayList<PhoneItem> get_all_items() {
        return all_items;
    }

    public ArrayList<PhoneItem> get_by_company(PhoneCompany company, PhoneItem.Type type) {
        ArrayList<PhoneItem> item;

        if (company_phone_map.containsKey(company))
            item = company_phone_map.get(company);
        else
            return null;

        return get_by_type(item, type);
    }

    public ArrayList<PhoneItem> get_diff_list() { return diff_list; }

    /* 儲存所有手機，包括歷史的記錄 */
    private HashMap<String, PhoneItem> all_phone_items_map = new HashMap<>();

    private HashMap<PhoneCompany, ArrayList<PhoneItem>> company_phone_map = new HashMap<>();

    public AllPhones(File file_path)
    {
        ArrayList<PhoneItem> new_phone_items = null;
        this.file_path = file_path;

        ParserBase parser = new LandtopParser(today);
        // ParserBase parser = new ExpansysParser(today);
        try {
            new_phone_items = parser.parse();
            read_and_update(new_phone_items);
        } catch (Exception e) {

        }
    }

    public CompareResult get_result() {
        return result;
    }

    public long get_history_size() {
        File file = new File(file_path, ALL_PHONE_FILE_NAME);
        if (file.exists())
            return file.length();
        else
            return -1;
    }

    boolean delete_history() {
        File file = new File(file_path, ALL_PHONE_FILE_NAME);

        if (file.exists()) {
            file.delete();
            return true;
        } else {
            return false;
        }
    }
    
    /* read old data from json file to 'all_phone_items_map' list */
    private void read_old_data() {
        File file = new File(file_path, ALL_PHONE_FILE_NAME);

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
                    all_phone_items_map.put(item.name, item);
                }
            } catch (IOException e) {
            }
        }
    }

    private void save_all_data() {
        File file = new File(file_path, ALL_PHONE_FILE_NAME);

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
        }
    }

    private void compare_old_phones(ArrayList<PhoneItem> phoneList) {
        if (all_phone_items_map.size() != 0) {
            HashMap<String, PhoneItem> new_phone_map = new HashMap<>();

            for (PhoneItem new_item : phoneList) {
                String name = new_item.name;

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
                    item.type = PhoneItem.Type.Deleted;
                }
            }

            all_phone_items_map = new_phone_map;

            if (diff_list.size() != 0) {
                result = CompareResult.Updated;
                save_all_data();
            } else {
                result = CompareResult.NoUpdated;
            }
        } else /* if (all_phone_items_map.size() == 0) */ {
            result = CompareResult.NoOldData;
            for (PhoneItem pi : phoneList) {
                all_phone_items_map.put(pi.name, pi);
            }
            save_all_data();
        }
    }

    private void read_and_update(ArrayList<PhoneItem> phoneList) {
        read_old_data();
        compare_old_phones(phoneList);

        for (PhoneItem pi : all_phone_items_map.values()) {
            all_items.add(pi);

            if (!company_phone_map.containsKey(pi.company))
                company_phone_map.put(pi.company, new ArrayList<PhoneItem>());
            company_phone_map.get(pi.company).add(pi);
        }
    }
}

public class MainActivity extends AppCompatActivity {
    private final String LOG_TAG = "LANDTOP";

    private Handler uiHandler = new Handler();

    ArrayList<HashMap<String, Object>> listForView = new ArrayList<>();
    ArrayList<PhoneItem> current_show;
    SimpleAdapter adapter;

    boolean is_busy;
    TextView descript_text;

    PhoneItem.Type show_type = PhoneItem.Type.Phone;
    PhoneCompany show_company = null;

    AllPhones all_phones;

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
                    PhoneItem selectItem = current_show.get(position);

                    AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                    b.setTitle(selectItem.name);

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
            HandlerThread thread = new HandlerThread("jsoup");
            thread.start();

            Handler threadHandler = new Handler(thread.getLooper());
            threadHandler.post(new Runnable() {
                @Override
                public void run() {
                    all_phones = new AllPhones(getFilesDir());
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            switch (all_phones.get_result()) {
                                case NoOldData:
                                    show_toast("初次執行");
                                    break;
                                case Updated:
                                    show_toast("網頁有更新");
                                    show_type = PhoneItem.Type.None;
                                    break;
                                case NoUpdated:
                                    show_toast("網頁沒有更新");
                                    break;
                                case Error:
                                    show_toast("發生錯誤");
                                    break;
                            }

                            /* currentShow = all_phones; */
                            update_list();
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

    private int IntegerCompare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    private void update_list() {
        StringBuilder sb = new StringBuilder();
        ArrayList<PhoneItem> tmp_list;
        boolean sort_by_money = true;

        if (show_type == PhoneItem.Type.Deleted) {
            sb.append("已下架: ");
            tmp_list = all_phones.get_by_type(PhoneItem.Type.Deleted);
            sb.append(tmp_list.size());
            current_show = new ArrayList<>(tmp_list);
            sort_by_money = false;
        } else if (show_type != PhoneItem.Type.None) {
            if (show_company == null) {
                tmp_list = all_phones.get_by_type(show_type);
                sb.append("所有廠牌");
            }
            else {
                tmp_list = all_phones.get_by_company(show_company, show_type);
                sb.append(show_company.name);
            }

            sb.append(" 的 ");
            if (show_type == PhoneItem.Type.Phone) {
                sb.append("手機");
            } else if (show_type == PhoneItem.Type.Tablet) {
                sb.append("平板");
            } else {
                sb.append("其他");
            }

            if (current_show == null)
                current_show = new ArrayList<>();
            else
                current_show.clear();

            current_show = new ArrayList<>(tmp_list);
        } else {
            sb.append("價格有變");
            current_show = new ArrayList<>(all_phones.get_diff_list());
        }
        if (sort_by_money) {
            Collections.sort(current_show, new Comparator<PhoneItem>() {
                @Override
                public int compare(PhoneItem lhs, PhoneItem rhs) {
                    return IntegerCompare(rhs.get_final_price(), lhs.get_final_price());
                }
            });
        } else {
            Collections.sort(current_show, new Comparator<PhoneItem>() {
                @Override
                public int compare(PhoneItem lhs, PhoneItem rhs) {
                    return lhs.get_final_date().after(rhs.get_final_date()) ? 1 : -1;
                }
            });
        }

        listForView.clear();

        for (PhoneItem pp : current_show) {
            HashMap<String, Object> hashMap = new HashMap<>();

            if (show_company == null)
                hashMap.put("PhoneName", pp.company.name + " " + pp.name);
            else
                hashMap.put("PhoneName", pp.name);

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
        super.onCreateOptionsMenu(menu);
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
        return true;
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
                show_company = null;
                update_list();
                return true;

            case R.id.show_apple:
                show_company = PhoneCompany.Apple;
                update_list();
                return true;

            case R.id.show_asus:
                show_company = PhoneCompany.ASUS;
                update_list();
                return true;

            case R.id.show_htc:
                show_company = PhoneCompany.HTC;
                update_list();
                return true;

            case R.id.show_lg:
                show_company = PhoneCompany.LG;
                update_list();
                return true;

            case R.id.show_samsung:
                show_company = PhoneCompany.SAMSUNG;
                update_list();
                return true;

            case R.id.show_sony:
                show_company = PhoneCompany.Sony;
                update_list();
                return true;

            case R.id.show_infocus:
                show_company = PhoneCompany.INFOCUS;
                update_list();
                return true;

            case R.id.show_huawei:
                show_company = PhoneCompany.HUAWEI;
                update_list();
                return true;

            case R.id.show_oppo:
                show_company = PhoneCompany.OPPO;
                update_list();
                return true;

            case R.id.show_mi:
                show_company = PhoneCompany.MI;
                update_list();
                return true;

            case R.id.show_moto:
                show_company = PhoneCompany.MOTO;
                update_list();
                return true;

            case R.id.show_google:
                show_company = PhoneCompany.Google;
                update_list();
                return true;

            case R.id.show_blackberry:
                show_company = PhoneCompany.Blackberry;
                update_list();
                return true;

            case R.id.show_oneplus:
                show_company = PhoneCompany.OnePlus;
                update_list();
                return true;

            case R.id.show_pad:
                show_type = PhoneItem.Type.Tablet;
                update_list();
                return true;

            case R.id.show_phone:
                show_type = PhoneItem.Type.Phone;
                update_list();
                return true;

            case R.id.show_money_diff:
                show_type = PhoneItem.Type.None;
                update_list();
                return true;

            case R.id.show_others:
                show_type = PhoneItem.Type.Others;
                update_list();
                return true;

            case R.id.show_delete:
                show_type = PhoneItem.Type.Deleted;
                update_list();
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

    void show_toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    void show_history_size() {
        AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
        b.setTitle("歷史檔案大小");
        long size = all_phones.get_history_size();

        if (size != -1) {
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
        if (all_phones.delete_history()) {
            show_toast("歷史檔案已刪除");
        } else {
            show_toast("歷史檔案不存在");
        }
    }
}

class PhoneCompany {
    private enum PhoneCountry {
        US,
        China,
        Japan,
        Korea,
        Taiwan,
        Unknown,
        Other,
    }

    String name;
    private PhoneCountry country;

    static PhoneCompany Apple = new PhoneCompany("Apple", PhoneCountry.US);
    static PhoneCompany ASUS = new PhoneCompany("ASUS", PhoneCountry.Taiwan);
    static PhoneCompany Benten = new PhoneCompany("Benten", PhoneCountry.Taiwan);
    static PhoneCompany Blackberry = new PhoneCompany("blackberry", PhoneCountry.US);
    static PhoneCompany Cat = new PhoneCompany("Cat", PhoneCountry.US);
    static PhoneCompany coolpad = new PhoneCompany("COOLPAD", PhoneCountry.China);
    static PhoneCompany GSmart = new PhoneCompany("GSmart", PhoneCountry.Taiwan); /* 技嘉 */
    static PhoneCompany Google = new PhoneCompany("Google", PhoneCountry.US);
    static PhoneCompany HTC = new PhoneCompany("HTC", PhoneCountry.Taiwan);
    static PhoneCompany HUAWEI = new PhoneCompany("HUAWEI", PhoneCountry.China);
    static PhoneCompany INFOCUS = new PhoneCompany("InFocus", PhoneCountry.Taiwan);
    static PhoneCompany INHON = new PhoneCompany("Inhon", PhoneCountry.Taiwan); /* 頂新 */
    static PhoneCompany LG = new PhoneCompany("LG", PhoneCountry.Korea);
    static PhoneCompany MEITU = new PhoneCompany("美圖", PhoneCountry.China);
    static PhoneCompany MI = new PhoneCompany("小米", PhoneCountry.China);
    static PhoneCompany MOTO = new PhoneCompany("MOTO", PhoneCountry.China);
    static PhoneCompany MTO = new PhoneCompany("MTO", PhoneCountry.Taiwan);
    static PhoneCompany NOKIA = new PhoneCompany("NOKIA", PhoneCountry.Other);
    static PhoneCompany OnePlus = new PhoneCompany("OnePlus", PhoneCountry.China);
    static PhoneCompany Onkyo = new PhoneCompany("Onkyo", PhoneCountry.Japan);
    static PhoneCompany OPPO = new PhoneCompany("OPPO", PhoneCountry.China);
    static PhoneCompany SAMSUNG = new PhoneCompany("SAMSUNG", PhoneCountry.Korea);
    static PhoneCompany SHARP = new PhoneCompany("SHARP", PhoneCountry.Taiwan);
    static PhoneCompany Sony = new PhoneCompany("Sony", PhoneCountry.Japan);
    static PhoneCompany SUGAR = new PhoneCompany("SUGAR", PhoneCountry.China);
    static PhoneCompany TWM = new PhoneCompany("TWM", PhoneCountry.Taiwan);
    static PhoneCompany Unknown = new PhoneCompany("UNKNOWN", PhoneCountry.Unknown);

    private PhoneCompany(String name, PhoneCountry country) {
        this.name = name;
        this.country = country;
    }

    /* 地標圖示與公司名稱的map */
    private static HashMap<String, PhoneCompany> company_pic_map = new HashMap<>();
    static  {
        company_pic_map.put("images/prodpt/844pSa.jpg", Apple);
        company_pic_map.put("images/prodpt/051k76.jpg", Sony);
        company_pic_map.put("images/prodpt/097SwZ.jpg", GSmart);
        company_pic_map.put("images/prodpt/149vbE.jpg", HTC);
        company_pic_map.put("images/prodpt/167sKc.jpg", INHON);
        company_pic_map.put("images/prodpt/414ZPQ.jpg", Benten);
        company_pic_map.put("images/prodpt/502Fc1.jpg", HUAWEI);
        company_pic_map.put("images/prodpt/586Q6N.jpg", ASUS);
        company_pic_map.put("images/prodpt/593uAM.jpg", SAMSUNG);
        company_pic_map.put("images/prodpt/697qw3.jpg", INFOCUS);
        company_pic_map.put("images/prodpt/751lXt.jpg", NOKIA);
        company_pic_map.put("images/prodpt/881IQv.jpg", OPPO);
        company_pic_map.put("images/prodpt/888Pyt.jpg", LG);
        company_pic_map.put("images/prodpt/957X0d.jpg", MTO);
        company_pic_map.put("images/prodpt/886lV7.jpg", coolpad);
        company_pic_map.put("images/prodpt/734wY8.gif", MI);
        company_pic_map.put("images/prodpt/084DtY.jpg", TWM);
        company_pic_map.put("images/prodpt/319uKq.jpg", SHARP);
        company_pic_map.put("images/prodpt/741o3d.jpg", MEITU);
        company_pic_map.put("images/prodpt/965q1b.jpg", MOTO);
        company_pic_map.put("images/prodpt/7606_L.jpg", SUGAR);
    }

    private static HashMap<String, PhoneCompany> company_name_map = new HashMap<>();
    static {
        company_name_map.put("apple", Apple);
        company_name_map.put("asus", ASUS);
        company_name_map.put("benten", Benten);
        company_name_map.put("blackberry", Blackberry);
        company_name_map.put("CAT®", Cat);
        company_name_map.put("coolpad", coolpad);
        company_name_map.put("gsmart", GSmart);
        company_name_map.put("google", Google);
        company_name_map.put("htc", HTC);
        company_name_map.put("huawei", HUAWEI);
        company_name_map.put("infocus", INFOCUS);
        company_name_map.put("inhon", INHON);
        company_name_map.put("lg", LG);
        company_name_map.put("meitu", MEITU);
        company_name_map.put("xiaomi", MI);
        company_name_map.put("motorola", MOTO);
        company_name_map.put("mto", MTO);
        company_name_map.put("nokia", NOKIA);
        company_name_map.put("oneplus", OnePlus);
        company_name_map.put("onkyo", Onkyo);
        company_name_map.put("oppo", OPPO);
        company_name_map.put("samsung", SAMSUNG);
        company_name_map.put("shape", SHARP);
        company_name_map.put("sony", Sony);
        company_name_map.put("sugar", SUGAR);
        company_name_map.put("twm", TWM);
    }

    static public PhoneCompany getCompanyFromPicture(String pic_name) {
        if (company_pic_map.containsKey(pic_name)) {
            return company_pic_map.get(pic_name);
        } else {
            return Unknown;
        }
    }

    static public PhoneCompany get_company_by_name(String name) {
        name = name.toLowerCase().trim();
        if (company_name_map.containsKey(name))
            return company_name_map.get(name);
        else {

            return Unknown;
        }
    }
}
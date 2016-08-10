package toy.james732.landtop;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Handler uiHandler = new Handler();
    private ListView listView;
    private HandlerThread thread;
    private Handler threadHandler;

    private HashMap<PhoneCompany.CompanyEnum, PhoneCompany> compsMap = new HashMap<PhoneCompany.CompanyEnum, PhoneCompany>();
    ArrayList<HashMap<String, Object>> listForView = new ArrayList<HashMap<String, Object>>();
    ArrayList<PhonePrice> allPhones = new ArrayList<PhonePrice>();
    ArrayList<PhonePrice> currentShow;
    SimpleAdapter adapter;

    enum ShowType {
        All,
        Phone,
        Tablet,
        Others,
        MoneyDiff
    }

    ShowType showType = ShowType.Phone;

    enum SortOrder {
        ByWeb,
        ByMoney,
        ByDiff
    }

    SortOrder sortOrder = SortOrder.ByMoney;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView)findViewById(R.id.myListView);

        thread = new HandlerThread("jsoup");
        thread.start();

        adapter = new SimpleAdapter(
                getApplicationContext(),
                listForView,
                R.layout.list,
                new String[]{ "PhoneName", "PhonePrice" },
                new int[] { R.id.textView1, R.id.textView2 });
        listView.setAdapter(adapter);

        threadHandler = new Handler(thread.getLooper());
        threadHandler.post(new Runnable() {
            @Override
            public void run() {
                parse();

                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        currentShow = allPhones;
                        updateList();
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private Boolean isPad(String s) {
        String upperStr = s.toUpperCase();

        if (upperStr.contains("PADFONE")) {
            return false;
        }
        else if (upperStr.contains("TAB") || upperStr.contains("PAD") || upperStr.contains("TABLET")) {
            return true;
        }
        else if (upperStr.contains("NOKIA N1")) {
            return true;
        }
        else {
            return false;
        }
    }

    private Boolean isOthers(String s) {
        String upperStr = s.toUpperCase();

        if (upperStr.contains("WATCH")) {
            return true;
        }
        else if (upperStr.contains("R100")) {
            return true;
        }
        else {
            return false;
        }
    }

    private void parse() {
        Connection.Response response = null;

        allPhones.clear();
        compsMap.clear();

        HashMap<String, Integer> namePriceMap = new HashMap<String, Integer>();
        StringBuilder sb = new StringBuilder();

        try {
            while (response == null || response.statusCode() != 200) {
                response = Jsoup.connect("http://www.landtop.com.tw/products.php?types=1").timeout(3000).execute();
                Thread.sleep(1000);
            }

            Document doc = response.parse();
            Elements companies = doc.getElementsByTag("table");

            int phoneSn = 0;

            for (int i = 0; i < companies.size(); i++) {
                Element company = companies.get(i);
                Elements phones = company.getElementsByTag("tr");

                String companyPic = phones.get(0).child(0).child(1).attr("src");
                PhoneCompany phoneCompany = PhoneCompany.getCompanyFromPicture(companyPic);
                compsMap.put(phoneCompany.company, phoneCompany);

                for (int j = 1; j < phones.size(); j++) {
                    Element phone = phones.get(j);
                    String name = phone.child(0).child(0).child(0).text();
                    String money = phone.child(1).text().substring(1);

                    sb.append(name);
                    sb.append(money);

                    PhonePrice pp = new PhonePrice(phoneCompany, name, money, phoneSn);

                    if (isPad(name)) {
                        pp.type = PhonePrice.Type.Tablet;
                    } else if (isOthers(name)) {
                        pp.type = PhonePrice.Type.Others;
                    }
                    else  {
                        pp.type = PhonePrice.Type.Phone;
                    }

                    phoneCompany.phones.add(pp);
                    allPhones.add(pp);
                    phoneSn++;

                    namePriceMap.put(pp.name, pp.price);
                }
            }
            checkPrevPrice(namePriceMap, stringToHash(sb.toString()));
        } catch (Exception e) {
            Log.d("PHONE", "Exception in Parse: " + e.getMessage());
        }
    }

    HashMap<String, Double> getFromJson(String s) throws IOException {
        HashMap<String, Double> m = new HashMap<String, Double>();

        File path = getFilesDir();
        File file = new File(path, s + ".txt");

        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String json = sb.toString();
        m = new Gson().fromJson(json, m.getClass());

        return m;
    }

    void saveToJson(HashMap<String, Integer> m, String s) throws IOException {
        File path = getFilesDir();
        File file = new File(path, s + ".txt");

        String json = new Gson().toJson(m);

        path.mkdirs();
        FileOutputStream o = new FileOutputStream(file);
        o.write(json.getBytes());
        o.close();
    }

    private void checkAllPrice() throws IOException {
        HashMap<String, Double> prevNamePriceMap = getFromJson("PrevPrev");

        for (PhonePrice p : allPhones) {
            if (prevNamePriceMap.containsKey(p.name)) {
                p.prev_price = prevNamePriceMap.get(p.name).intValue();
            }
        }
    }

    private String stringToHash(String str) throws NoSuchAlgorithmException {
        StringBuilder sb = new StringBuilder();

        MessageDigest md5 = MessageDigest.getInstance("SHA-1");
        md5.update(str.getBytes());
        byte[] digest = md5.digest();

        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }

        return sb.toString();
    }

    private void updateJsonFiles(HashMap<String, Integer> m) throws IOException {
        File path = getFilesDir();
        File prevFile = new File(path, "Prev.txt");
        File prevPrevFile = new File(path, "PrevPrev.txt");

        prevPrevFile.deleteOnExit();
        prevFile.renameTo(new File(path, "PrevPrev.txt"));

        saveToJson(m, "Prev");
    }

    private void checkPrevPrice(HashMap<String, Integer> namePriceMap, String currentHash)
            throws IOException  {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        String prevHash = sharedPref.getString("PREV_HASH", "");
        String prevPrevHash = sharedPref.getString("PREV_PREV_HASH", "");

        Log.d("PHONE", "Current Hash: " + currentHash);
        Log.d("PHONE", "Prev Hash: " + prevHash);
        Log.d("PHONE", "PrevPrev Hash: " + prevPrevHash);

        if (prevHash.equals("")) {
            // 初次執行
            Toast.makeText(this, "初次執行", Toast.LENGTH_LONG).show();

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("PREV_HASH", currentHash);
            editor.commit();

            saveToJson(namePriceMap, "Prev");
        } else if (prevHash.equals(currentHash)) {
            if (!prevPrevHash.equals("")) {
                Toast.makeText(this, "網頁沒有更新，有舊資料可比較", Toast.LENGTH_LONG).show();
                checkAllPrice();
            }
            else {
                Toast.makeText(this, "網頁沒有更新，也沒有舊資料可比較", Toast.LENGTH_LONG).show();
            }
        } else {
            // 網頁已更新
            Toast.makeText(this, "網頁有更新", Toast.LENGTH_LONG).show();

            prevPrevHash = prevHash;
            prevHash = currentHash;

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("PREV_HASH", prevHash);
            editor.putString("PREV_PREV_HASH", prevPrevHash);
            editor.commit();

            updateJsonFiles(namePriceMap);

            checkAllPrice();
        }
    }

    private int IntegerCompare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    private void updateList() {
        listForView.clear();

        if (sortOrder == SortOrder.ByMoney) {
            Collections.sort(currentShow, new Comparator<PhonePrice>() {
                @Override
                public int compare(PhonePrice lhs, PhonePrice rhs) {
                    return IntegerCompare(rhs.price, lhs.price);
                }
            });
        }
        else if (sortOrder == SortOrder.ByDiff) {
            Collections.sort(currentShow, new Comparator<PhonePrice>() {
                @Override
                public int compare(PhonePrice lhs, PhonePrice rhs) {
                    return IntegerCompare(lhs.getPriceDiff(), rhs.getPriceDiff());
                }
            });
        }
        else {
            Collections.sort(currentShow, new Comparator<PhonePrice>() {
                @Override
                public int compare(PhonePrice lhs, PhonePrice rhs) {
                    return IntegerCompare(lhs.id, rhs.id);
                }
            });
        }

        for (PhonePrice pp : currentShow) {

            if (showType == ShowType.Phone && pp.type != PhonePrice.Type.Phone) {
                continue;
            }
            else if (showType == ShowType.Tablet && pp.type != PhonePrice.Type.Tablet) {
                continue;
            }
            else if (showType == ShowType.Others && pp.type != PhonePrice.Type.Others) {
                continue;
            }
            else if (showType == ShowType.MoneyDiff
                    && pp.getPriceStatus() == PhonePrice.PriceStatus.Same) {
                continue;
            }

            HashMap<String, Object> hashMap = new HashMap<String, Object>();
            hashMap.put("PhoneName", pp);
            hashMap.put("PhonePrice", pp.getPriceString());
            listForView.add(hashMap);
        }

        adapter.notifyDataSetChanged();
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

        item.setChecked(true);

        switch (id) {
            case R.id.sort_by_money:
                sortOrder = SortOrder.ByMoney;
                updateList();
                return true;

            case R.id.sort_by_web_order:
                sortOrder = SortOrder.ByWeb;
                updateList();
                return true;

            case R.id.sort_by_moneydiff:
                sortOrder = SortOrder.ByDiff;
                updateList();
                return true;

            case R.id.show_all:
                currentShow = allPhones;
                updateList();
                return true;

            case R.id.show_apple:
                return showCompany(PhoneCompany.CompanyEnum.Apple);

            case R.id.show_asus:
                return showCompany(PhoneCompany.CompanyEnum.ASUS);

            case R.id.show_htc:
                return showCompany(PhoneCompany.CompanyEnum.hTC);

            case R.id.show_lg:
                return showCompany(PhoneCompany.CompanyEnum.LG);

            case R.id.show_samsung:
                return showCompany(PhoneCompany.CompanyEnum.SAMSUNG);

            case R.id.show_sony:
                return showCompany(PhoneCompany.CompanyEnum.Sony);

            case R.id.show_infocus:
                return showCompany(PhoneCompany.CompanyEnum.InFocus);

            case R.id.show_huawei:
                return showCompany(PhoneCompany.CompanyEnum.HUAWEI);

            case R.id.show_oppo:
                return showCompany(PhoneCompany.CompanyEnum.oppo);

            case R.id.show_mi:
                return showCompany(PhoneCompany.CompanyEnum.MI);

            case R.id.show_twm:
                return showCompany(PhoneCompany.CompanyEnum.TWM);

            case R.id.show_pad_phone:
                showType = ShowType.All;
                updateList();
                return true;

            case R.id.show_pad:
                showType = ShowType.Tablet;
                updateList();
                return true;

            case R.id.show_phone:
                showType = ShowType.Phone;
                updateList();
                return true;

            case R.id.show_money_diff:
                showType = ShowType.MoneyDiff;
                updateList();
                return true;

            case R.id.show_others:
                showType = ShowType.Others;
                updateList();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean showCompany(PhoneCompany.CompanyEnum c) {
        currentShow = compsMap.get(c).phones;
        updateList();
        return true;
    }
}

class PhonePrice {
    public PhonePrice(PhoneCompany c, String n, String p, int i) {
        name = c.company + " " + n;
        try {
            price = Integer.parseInt(p);
        } catch (NumberFormatException e) {
            price = Integer.MIN_VALUE;
        }
        id = i;
        prev_price = 0;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getPriceString() {
        if (price == Integer.MAX_VALUE || price == Integer.MIN_VALUE) {
            return "特價中";
        }
        else {
            StringBuilder sb = new StringBuilder();
            sb.append("$ ");
            sb.append(price);

            if (prev_price != 0) {
                sb.append(" ");

                int diff = prev_price - price;
                if (diff != 0) {
                    if (diff > 0) {
                        sb.append(" ↘ $");
                    }
                    else {
                        sb.append(" ↗ $");
                        diff = -diff;
                    }
                    sb.append(diff);
                }
                else {
                    sb.append(" ー");
                }
            }
            else {
                sb.append(" 新");
            }

            return sb.toString();
        }
    }

    public PriceStatus getPriceStatus() {
        if (prev_price == 0) {
            return PriceStatus.New;
        }
        else if (price == prev_price) {
            return PriceStatus.Same;
        }
        else if (price > prev_price) {
            return PriceStatus.Down;
        }
        else {
            return PriceStatus.Up;
        }
    }

    public int getPriceDiff() {
        if (getPriceStatus() == PriceStatus.New) {
            return price;
        }
        else {
            return price - prev_price;
        }
    }

    public String name;
    public int id;
    public int price;
    public int prev_price;

    public enum Type {
        Phone,
        Tablet,
        Others,
    }

    public enum PriceStatus {
        New,
        Same,
        Up,
        Down
    }

    public Type type;
}

class PhoneCompany {
    public String companyName;
    public ArrayList<PhonePrice> phones = new ArrayList<PhonePrice>();

    public enum CompanyEnum {
        Apple,
        Sony,
        GSmart,
        hTC,
        Inhon,
        Benten,
        HUAWEI,
        ASUS,
        SAMSUNG,
        InFocus,
        NOKIA,
        oppo,
        LG,
        MTO,
        COOLPAD,
        MI,
        TWM,
        SHARP,
        UNKNOWN,
    }

    public CompanyEnum company;

    private static HashMap<String, CompanyEnum> companyPicMap = new HashMap<String, CompanyEnum>();

    static  {
        companyPicMap.put("images/prodpt/844pSa.jpg", CompanyEnum.Apple);
        companyPicMap.put("images/prodpt/051k76.jpg", CompanyEnum.Sony);
        companyPicMap.put("images/prodpt/097SwZ.jpg", CompanyEnum.GSmart);
        companyPicMap.put("images/prodpt/149vbE.jpg", CompanyEnum.hTC);
        companyPicMap.put("images/prodpt/167sKc.jpg", CompanyEnum.Inhon);
        companyPicMap.put("images/prodpt/414ZPQ.jpg", CompanyEnum.Benten);
        companyPicMap.put("images/prodpt/502Fc1.jpg", CompanyEnum.HUAWEI);
        companyPicMap.put("images/prodpt/586Q6N.jpg", CompanyEnum.ASUS);
        companyPicMap.put("images/prodpt/593uAM.jpg", CompanyEnum.SAMSUNG);
        companyPicMap.put("images/prodpt/697qw3.jpg", CompanyEnum.InFocus);
        companyPicMap.put("images/prodpt/751lXt.jpg", CompanyEnum.NOKIA);
        companyPicMap.put("images/prodpt/881IQv.jpg", CompanyEnum.oppo);
        companyPicMap.put("images/prodpt/888Pyt.jpg", CompanyEnum.LG);
        companyPicMap.put("images/prodpt/957X0d.jpg", CompanyEnum.MTO);
        companyPicMap.put("images/prodpt/886lV7.jpg", CompanyEnum.COOLPAD);
        companyPicMap.put("images/prodpt/734wY8.gif", CompanyEnum.MI);
        companyPicMap.put("images/prodpt/084DtY.jpg", CompanyEnum.TWM);
        companyPicMap.put("images/prodpt/319uKq.jpg", CompanyEnum.SHARP);
    };

    static public PhoneCompany getCompanyFromPicture(String name) {
        PhoneCompany comp = new PhoneCompany();

        CompanyEnum ce = companyPicMap.get(name);

        if (ce != null) {
            comp.company = ce;
        }
        else {
            comp.company = CompanyEnum.UNKNOWN;
        }

        comp.companyName = comp.company.toString();

        return comp;
    }
}
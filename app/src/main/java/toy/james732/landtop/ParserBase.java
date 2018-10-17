package toy.james732.landtop;

import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

abstract class ParserBase {
    ArrayList<PhoneItem> phoneList = new ArrayList<>();
    private HashMap<String, PhoneItem> phoneMap = new HashMap<>();
    private Date today;

    ParserBase(Date today) {
        this.today = today;
    }

    abstract ArrayList<PhoneItem> parse() throws IOException, InterruptedException;

    /* 把指定URL的網頁抓下來，並回傳Document */
    Document getAndParse(String url) throws IOException, InterruptedException {
        Connection.Response response = null;
        while (true) {
            Log.d(MainActivity.LOG_TAG, "Downloading " + url);
            response = Jsoup.connect(url).execute();
            if (response.statusCode() == 200) {
                Log.d(MainActivity.LOG_TAG, "response.statusCode() == 200");
                return response.parse();
            }
            Thread.sleep(1000);
        }
    }

    void checkAddPhone(String name, int money, PhoneCompany company) {
        checkAddPhone(name, money, company, false);
    }

    void checkAddPhone(String name, int money, PhoneCompany company, boolean isEstimated) {
        if (phoneMap.containsKey(name)) {
            /* 如果已經有同名的手機，則update它的價格 */
            phoneMap.get(name).checkAndUpdatePrice(money, today, isEstimated);
        } else {
            PhoneItem pi = new PhoneItem(company, name, money, today, isEstimated);
            phoneList.add(pi);
            phoneMap.put(name, pi);
        }
    }
}
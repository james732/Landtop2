package toy.james732.landtop;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

class LandtopParser extends ParserBase {
    LandtopParser(Date today) {
        super(today);
    }

    @Override
    ArrayList<PhoneItem> parse() throws IOException, InterruptedException {
        parseProducts();
        parseTop50();
        return phoneList;
    }

    private int parseMoneyString(String money) {
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

    private void parseProducts() throws IOException, InterruptedException {
        Document doc = getAndParse("http://www.landtop.com.tw/products.php?types=1");
        Elements companies = doc.getElementsByTag("table");

        for (int i = 0; i < companies.size(); i++) {
            Element company = companies.get(i);
            Elements phones = company.getElementsByTag("tr");

            String company_pic = phones.get(0).child(0).child(1).attr("src");
            PhoneCompany phoneCompany = PhoneCompany.getCompanyFromPicture(company_pic);
            String companyName = phoneCompany.name;

            for (int j = 1; j < phones.size(); j++) {
                Element phone = phones.get(j);
                String name = phone.child(0).child(0).child(0).text();
                String money = phone.child(1).text();

                if (name.toLowerCase().startsWith(companyName.toLowerCase()) && name.charAt(companyName.length())== ' ')
                    name = name.substring(companyName.length() + 1);

                int moneyValue = parseMoneyString(money);
                if (moneyValue == -1)
                    continue;

                checkAddPhone(name, moneyValue, phoneCompany);
            }
        }
    }

    private void parseTop50() throws IOException, InterruptedException {
        Document doc = getAndParse("http://www.landtop.com.tw/landtop50_new.php");
        Elements companies = doc.getElementsByTag("table");
        int reduceDiff = -1;

        /* 每家手機是一個 table */
        for (int i = 0; i < companies.size(); i++) {
            /* 這家廠商的每一隻 */
            Elements phones = companies.get(i).getElementsByTag("tr");

            /* 第一格是廠商的資訊 */
            /* <tr><td><img src="..."> */
            String companyPic = phones.get(0).child(0).child(0).attr("src");

            PhoneCompany phoneCompany = PhoneCompany.getCompanyFromPicture(companyPic);
            String companyName = phoneCompany.name;

            for (int j = 1; j < phones.size(); j++) {
                Element phone = phones.get(j);
                /* <tr>
                 *  <td><h5><a>phone name</a></h5></td>
                 *  <td>單機價</td>
                 *  </tr>
                 **/
                String name = phone.child(0).child(0).child(0).text();
                String moneyString = phone.child(1).text();
                String reduceString = phone.child(2).text();
                boolean isEstimated = false;

                if (name.startsWith(companyName) && name.charAt(companyName.length()) == ' ')
                    name = name.substring(companyName.length() + 1);

                int money = parseMoneyString(moneyString);
                int reduce = parseMoneyString(reduceString);

                if (reduceDiff == -1)
                    if (money != -1 && reduce != -1)
                        reduceDiff = money - reduce;

                if (money == -1 && reduce != 0 && reduceDiff != -1) {
                    money = reduce + reduceDiff;
                    isEstimated = true;
                }
                if (money == -1)
                    continue;

                checkAddPhone(name, money, phoneCompany, isEstimated);
            }
        }
    }
}

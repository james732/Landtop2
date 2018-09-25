package toy.james732.landtop;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

class ExpansysParser extends ParserBase {
    public ExpansysParser(Date today) {
        super(today);
    }

    @Override
    ArrayList<PhoneItem> parse() throws IOException, InterruptedException {
        parse_expansys();
        return phoneList;
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

            PhoneCompany phone_company = PhoneCompany.getCompanyByName(company_name);

            int price_value = 0;
            try {
                String s = price.replace("NT$", "").replace(",", "");
                price_value = Integer.parseInt(s);
            } catch (NumberFormatException e) {

            }
            checkAddPhone(phone_name, price_value, phone_company);
        }
    }

    private int get_expansys_phone_pages() throws IOException, InterruptedException {
        Document doc = getAndParse("http://www.expansys-tw.com/mobile-phones/");
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
        Document doc = getAndParse("http://www.expansys-tw.com/mobile-phones/?page=" + String.valueOf(page));
        parse_expansys_doc(doc);
    }

    private void parse_expansys() throws IOException, InterruptedException {
        int pages = get_expansys_phone_pages();
        for (int i = 0; i < pages; i++) {
            parse_expansys_page(i + 1);
        }
    }
}

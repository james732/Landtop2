package toy.james732.landtop;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

class PhoneItem {
    PhoneCompany company;
    String name;
    Type type;

    private ArrayList<PhonePriceHistory> priceHistories;

    enum Type {
        None,
        Phone,
        Tablet,
        Others,
    }

    PhoneItem(PhoneCompany company, String name, int price, Date today, boolean isEstimated) {
        this.company = company;
        this.name = name;

        priceHistories = new ArrayList<>();
        priceHistories.add(new PhonePriceHistory(price, today, isEstimated));

        type = check_type();
    }

    @Override
    public String toString() { return name; }

    private PhonePriceHistory getFinalHistory() {
        return priceHistories.get(priceHistories.size() - 1);
    }

    int getFinalPrice() { return getFinalHistory().price; }

    Date getFinalDate() { return getFinalHistory().date; }

    private boolean getFinalPriceIsEstimated() { return getFinalHistory().isEstimated; }

    String getAllPriceString() {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

        for (PhonePriceHistory history : priceHistories) {
            sb.append(sdf.format(history.date));
            sb.append(" $ ");
            sb.append(history.price);
            if (history.isEstimated)
                sb.append("*");
            sb.append("\n");
        }

        return sb.toString();
    }

    String getPriceString() {
        if (getFinalPrice() != -1) {
            StringBuilder sb = new StringBuilder("$ ");
            sb.append(getFinalPrice());

            if (getFinalPriceIsEstimated())
                sb.append("*");

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

    void checkAndUpdatePrice(int price, Date today, boolean isEstimated) {
        getFinalHistory().updatePriceToday(price, today, isEstimated);
    }

    void addNewPriceFromAnother(PhoneItem item) {
        if (getFinalPrice() == -1)
            priceHistories.remove(priceHistories.size() - 1);
        if (getFinalPrice() != item.priceHistories.get(0).price)
            priceHistories.add(item.priceHistories.get(0));
    }

    boolean isAvailable() {
        return priceHistories.get(priceHistories.size()).price != -1;
    }

    void notAvailable(Date today) {
        priceHistories.add(new PhonePriceHistory(-1, today, false));
    }
}

package toy.james732.landtop;

import java.util.Date;

class PhonePriceHistory {
    PhonePriceHistory(int p, Date d, boolean isEstimated) {
        this.price = p;
        this.date = d;
        this.isEstimated = isEstimated;
    }

    void updatePriceToday(int price, Date today, boolean isEstimated) {
        if (date.equals(today)) {
            if (price < this.price) {
                this.price = price;
                this.isEstimated = isEstimated;
            }
        }
    }

    int price;
    Date date;
    boolean isEstimated;
}

package toy.james732.landtop;

import android.provider.ContactsContract;

import java.util.HashMap;

class PhoneCompany {
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PhoneCompany) {
            PhoneCompany company = (PhoneCompany)obj;
            return name.equals(company.name);
        }
        else {
            return false;
        }
    }

    private enum PhoneCountry {
        US,
        China,
        Japan,
        Korea,
        Taiwan,
        Unknown,
        Other,
    }

    @Override
    public String toString() {
        return name;
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
    static PhoneCompany Koobee = new PhoneCompany("Koobee", PhoneCountry.China); /* 酷比 */
    static PhoneCompany LG = new PhoneCompany("LG", PhoneCountry.Korea);
    static PhoneCompany MEITU = new PhoneCompany("美圖", PhoneCountry.China);
    static PhoneCompany MI = new PhoneCompany("小米", PhoneCountry.China);
    static PhoneCompany MOTO = new PhoneCompany("moto", PhoneCountry.China);
    static PhoneCompany MTO = new PhoneCompany("MTO", PhoneCountry.Taiwan);
    static PhoneCompany NOKIA = new PhoneCompany("Nokia", PhoneCountry.Other);
    static PhoneCompany OnePlus = new PhoneCompany("OnePlus", PhoneCountry.China);
    static PhoneCompany Onkyo = new PhoneCompany("Onkyo", PhoneCountry.Japan);
    static PhoneCompany OPPO = new PhoneCompany("OPPO", PhoneCountry.China);
    static PhoneCompany PANASONIC = new PhoneCompany("Panasonic", PhoneCountry.Japan);
    static PhoneCompany SAMSUNG = new PhoneCompany("SAMSUNG", PhoneCountry.Korea);
    static PhoneCompany SHARP = new PhoneCompany("SHARP", PhoneCountry.Taiwan);
    static PhoneCompany Sony = new PhoneCompany("Sony", PhoneCountry.Japan);
    static PhoneCompany SUGAR = new PhoneCompany("SUGAR", PhoneCountry.China);
    static PhoneCompany TWM = new PhoneCompany("TWM", PhoneCountry.Taiwan);
    static PhoneCompany VIVO = new PhoneCompany("VIVO", PhoneCountry.China);
    static PhoneCompany Hugiga = new PhoneCompany("Hugiga", PhoneCountry.Taiwan);
    static PhoneCompany Unknown = new PhoneCompany("UNKNOWN", PhoneCountry.Unknown);

    private PhoneCompany(String name, PhoneCountry country) {
        this.name = name;
        this.country = country;
    }

    /* 地標圖示與公司名稱的map */
    private static HashMap<String, PhoneCompany> companyPicMap = new HashMap<>();
    static  {
        companyPicMap.put("images/prodpt/844pSa.jpg", Apple);
        companyPicMap.put("images/prodpt/051k76.jpg", Sony);
        companyPicMap.put("images/prodpt/097SwZ.jpg", GSmart);
        companyPicMap.put("images/prodpt/149vbE.jpg", HTC);
        companyPicMap.put("images/prodpt/167sKc.jpg", INHON);
        companyPicMap.put("images/prodpt/414ZPQ.jpg", Benten);
        companyPicMap.put("images/prodpt/502Fc1.jpg", HUAWEI);
        companyPicMap.put("images/prodpt/586Q6N.jpg", ASUS);
        companyPicMap.put("images/prodpt/593uAM.jpg", SAMSUNG);
        companyPicMap.put("images/prodpt/697qw3.jpg", INFOCUS);
        companyPicMap.put("images/prodpt/751lXt.jpg", NOKIA);
        companyPicMap.put("images/prodpt/881IQv.jpg", OPPO);
        companyPicMap.put("images/prodpt/888Pyt.jpg", LG);
        companyPicMap.put("images/prodpt/957X0d.jpg", MTO);
        companyPicMap.put("images/prodpt/886lV7.jpg", coolpad);
        companyPicMap.put("images/prodpt/734wY8.gif", MI);
        companyPicMap.put("images/prodpt/084DtY.jpg", TWM);
        companyPicMap.put("images/prodpt/319uKq.jpg", SHARP);
        companyPicMap.put("images/prodpt/741o3d.jpg", MEITU);
        companyPicMap.put("images/prodpt/965q1b.jpg", MOTO);
        companyPicMap.put("images/prodpt/7606_L.jpg", SUGAR);
        companyPicMap.put("images/prodpt/7283AU.jpg", VIVO);
        companyPicMap.put("images/prodpt/642Js_.gif", Hugiga);
        companyPicMap.put("images/prodpt/246Alk.jpg", PANASONIC);
        companyPicMap.put("images/prodpt/5461db.jpg", Koobee);
    }

    private static HashMap<String, PhoneCompany> companyNameMap = new HashMap<>();
    static {
        companyNameMap.put("apple", Apple);
        companyNameMap.put("asus", ASUS);
        companyNameMap.put("benten", Benten);
        companyNameMap.put("blackberry", Blackberry);
        companyNameMap.put("CAT®", Cat);
        companyNameMap.put("coolpad", coolpad);
        companyNameMap.put("gsmart", GSmart);
        companyNameMap.put("google", Google);
        companyNameMap.put("htc", HTC);
        companyNameMap.put("huawei", HUAWEI);
        companyNameMap.put("infocus", INFOCUS);
        companyNameMap.put("inhon", INHON);
        companyNameMap.put("lg", LG);
        companyNameMap.put("meitu", MEITU);
        companyNameMap.put("xiaomi", MI);
        companyNameMap.put("motorola", MOTO);
        companyNameMap.put("mto", MTO);
        companyNameMap.put("nokia", NOKIA);
        companyNameMap.put("oneplus", OnePlus);
        companyNameMap.put("onkyo", Onkyo);
        companyNameMap.put("oppo", OPPO);
        companyNameMap.put("samsung", SAMSUNG);
        companyNameMap.put("shape", SHARP);
        companyNameMap.put("sony", Sony);
        companyNameMap.put("sugar", SUGAR);
        companyNameMap.put("twm", TWM);
        companyNameMap.put("VIVO", VIVO);
    }

    static PhoneCompany getCompanyFromPicture(String pic_name) {
        if (companyPicMap.containsKey(pic_name)) {
            return companyPicMap.get(pic_name);
        } else {
            return Unknown;
        }
    }

    static PhoneCompany getCompanyByName(String name) {
        name = name.toLowerCase().trim();
        if (companyNameMap.containsKey(name))
            return companyNameMap.get(name);
        else {
            return Unknown;
        }
    }
}
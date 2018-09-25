package toy.james732.landtop;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

class AllPhones
{
    enum CompareResult {
        NoOldData,
        NoUpdated,
        Updated,
        Error,
    }

    /* 儲存所有手機，包括歷史的記錄 */
    private HashMap<String, PhoneItem> allPhoneItemsMap = new HashMap<>();
    private HashMap<PhoneCompany, ArrayList<PhoneItem>> companyPhoneMap = new HashMap<>();

    private final String ALL_PHONE_FILE_NAME = "landtop_V3.txt";
    private File filePath;

    private CompareResult result;

    private Date today = Calendar.getInstance().getTime();
    private ArrayList<PhoneItem> allItems = new ArrayList<>();
    private ArrayList<PhoneItem> diffList = new ArrayList<>();

    private ArrayList<PhoneItem> getByType(ArrayList<PhoneItem> items, PhoneItem.Type type) {
        if (items == null)
            return null;

        ArrayList<PhoneItem> retItems = new ArrayList<>();

        for (PhoneItem pi : items) {
            if (pi.type == type) {
                retItems.add(pi);
            }
        }

        return retItems;
    }

    ArrayList<PhoneItem> getByType(PhoneItem.Type type) {
        return getByType(allItems, type);
    }

    ArrayList<PhoneItem> getByCompany(PhoneCompany company, PhoneItem.Type type) {
        ArrayList<PhoneItem> item;

        if (companyPhoneMap.containsKey(company))
            item = companyPhoneMap.get(company);
        else
            return null;

        return getByType(item, type);
    }

    ArrayList<PhoneItem> getDiffList() { return diffList; }

    AllPhones(File filePath)
    {
        this.filePath = filePath;

        ParserBase parser = new LandtopParser(today);
        // ParserBase parser = new ExpansysParser(today);
        try {
            readAndUpdate(parser.parse());
        } catch (Exception e) {
        }
    }

    CompareResult getResult() { return result; }

    private long getHistorySize() {
        File file = new File(filePath, ALL_PHONE_FILE_NAME);
        if (file.exists())
            return file.length();
        else
            return -1;
    }

    String getHistorySizeString() {
        long size = getHistorySize();
        if (size == -1) {
            return "歷史檔案不存在";
        } else {
            return new StringBuilder()
                    .append(size)
                    .append(" byte\n")
                    .append(Double.toString(size / 1024.0))
                    .append(" KB\n")
                    .append(Double.toString(size / 1024.0 / 1024.0))
                    .append(" MB\n").toString();
        }
    }

    boolean deleteHistory() {
        File file = new File(filePath, ALL_PHONE_FILE_NAME);

        if (file.exists()) {
            file.delete();
            return true;
        } else {
            return false;
        }
    }

    private void readAndUpdate(ArrayList<PhoneItem> parsedList) {
        readOldData();
        compareOldPhones(parsedList);

        for (PhoneItem pi : allPhoneItemsMap.values()) {
            allItems.add(pi);

            if (!companyPhoneMap.containsKey(pi.company))
                companyPhoneMap.put(pi.company, new ArrayList<>());
            companyPhoneMap.get(pi.company).add(pi);
        }
    }

    /* 從json讀出來的是list，再以name為key，object為value，存至allPhoneItemsMap */
    private void readOldData() {
        File file = new File(filePath, ALL_PHONE_FILE_NAME);

        if (file.exists() && !file.isDirectory()) {
            PhoneItem[] itemArray = null;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String json = sb.toString();
                itemArray = new Gson().fromJson(
                        json, PhoneItem[].class);

                for (PhoneItem item : itemArray) {
                    allPhoneItemsMap.put(item.name, item);
                }
            } catch (IOException e) {
            }
        }
    }

    private void saveAllData() {
        File file = new File(filePath, ALL_PHONE_FILE_NAME);

        if (file.exists())
            file.delete();

        PhoneItem[] item_array = allPhoneItemsMap.values().toArray(
                new PhoneItem[allPhoneItemsMap.values().size()]
        );

        try {
            String json = new Gson().toJson(item_array);
            FileOutputStream o = new FileOutputStream(file);
            o.write(json.getBytes());
            o.close();
        } catch (IOException e) {
        }
    }

    private void compareOldPhones(ArrayList<PhoneItem> parsedList) {
        if (allPhoneItemsMap.size() != 0) { /* 如果有舊資料 */
            HashMap<String, PhoneItem> newPhoneMap = new HashMap<>();

            for (PhoneItem newItem : parsedList) { /* 對於網頁讀到的每一項 */
                String name = newItem.name;

                if (allPhoneItemsMap.containsKey(name)) {
                    PhoneItem oldItem;
                    /* 舊有的手機，比較價格 */
                    oldItem = allPhoneItemsMap.get(name);
                    int prevPrice = oldItem.getFinalPrice();
                    int nowPrice = newItem.getFinalPrice();

                    if (prevPrice != nowPrice) { /* 價格改變了 */
                        oldItem.addNewPriceFromAnother(newItem);
                        diffList.add(oldItem);
                    }
                    newPhoneMap.put(name, oldItem);

                    /* 舊有的，新的也有的，先移除掉 */
                    allPhoneItemsMap.remove(name);
                } else {
                    /* 新上架 */
                    diffList.add(newItem);
                    newPhoneMap.put(name, newItem);
                }
            }

            /* 處理已經下架的手機們 */
            for (String s : allPhoneItemsMap.keySet()) {
                PhoneItem item = allPhoneItemsMap.get(s);

                if (item.getFinalPrice() != -1) {
                    item.notAvailable(today);
                    newPhoneMap.put(s, item);
                    diffList.add(item);
                }
            }

            allPhoneItemsMap = newPhoneMap;

            if (diffList.size() != 0) {
                result = CompareResult.Updated;
                saveAllData();
            } else {
                result = CompareResult.NoUpdated;
            }
        } else /* if (all_phone_items_map.size() == 0) */ {
            result = CompareResult.NoOldData;
            for (PhoneItem pi : parsedList) {
                allPhoneItemsMap.put(pi.name, pi);
            }
            saveAllData();
        }
    }

    String[] getAllCompany() {
        String[] items = new String[companyPhoneMap.keySet().size()];
        int i = 0;
        for (PhoneCompany p : companyPhoneMap.keySet())
            items[i++] = p.name;
        return items;
    }
}
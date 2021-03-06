package uhh_praktikum_fea.tools;

import java.util.*;

public class MapUtility {
    /**
     * Accepts a map and returns LinkedHashMap sorted by values from biggest to smallest.
     *
     * @param map map to be sorted
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        Map<K, V> result = new LinkedHashMap<>();

        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
}

package com.swapps.mp3player.comparator;

import com.swapps.mp3player.Item;

import java.util.Comparator;

/**
 * ファイル名順に並べ替え
 */
public class NameComparator implements Comparator {
    public int compare(Object object1, Object object2) {
        Item item1 = (Item) object1;
        Item item2 = (Item) object2;

        return item1.getName().compareTo(item2.getName());
    }
}

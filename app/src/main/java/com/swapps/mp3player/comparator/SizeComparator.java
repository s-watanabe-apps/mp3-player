package com.swapps.mp3player.comparator;

import com.swapps.mp3player.Item;
import java.util.Comparator;

/**
 * サイズの大きい順に並べ替え
 */
public class SizeComparator implements Comparator {
    public int compare(Object object1, Object object2) {
        Item item1 = (Item) object1;
        Item item2 = (Item) object2;

        if(item1.getSize() < item2.getSize()){
            return 1;
        }else if(item1.getSize() > item2.getSize()){
            return -1;
        }else{
            return 0;
        }
    }
}

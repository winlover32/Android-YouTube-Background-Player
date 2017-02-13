package com.smedic.tubtub.interfaces;

import com.smedic.tubtub.model.ItemType;

/**
 * Created by smedic on 9.2.17..
 */

public interface ItemEventsListener {
    void onShareClicked(ItemType type, String itemId);
}

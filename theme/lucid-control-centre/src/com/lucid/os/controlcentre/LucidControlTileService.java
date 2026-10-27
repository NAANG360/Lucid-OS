package com.lucid.os.controlcentre;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public final class LucidControlTileService extends TileService {
    @Override public void onStartListening() {
        super.onStartListening();
        Tile tile=getQsTile();
        if(tile!=null){tile.setLabel("Lucid");tile.setSubtitle("Control Centre");tile.setState(Tile.STATE_INACTIVE);tile.updateTile();}
    }
    @Override public void onClick() {
        super.onClick();
        Intent i=new Intent(this,MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityAndCollapse(i);
    }
}
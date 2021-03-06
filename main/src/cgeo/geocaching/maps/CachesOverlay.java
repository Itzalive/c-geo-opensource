package cgeo.geocaching.maps;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgeodetail;
import cgeo.geocaching.cgeonavigate;
import cgeo.geocaching.cgeopopup;
import cgeo.geocaching.cgeowaypoint;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.ItemizedOverlayImpl;
import cgeo.geocaching.maps.interfaces.MapFactory;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;

import org.apache.commons.lang3.StringUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.location.Location;
import android.text.Html;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CachesOverlay extends AbstractItemizedOverlay implements GeneralOverlay {

    private List<CachesOverlayItemImpl> items = new ArrayList<CachesOverlayItemImpl>();
    private Context context = null;
    private Boolean fromDetail = false;
    private boolean displayCircles = false;
    private ProgressDialog waitDialog = null;
    private Point center = new Point();
    private Point left = new Point();
    private Paint blockedCircle = null;
    private PaintFlagsDrawFilter setfil = null;
    private PaintFlagsDrawFilter remfil = null;
    private cgSettings settings;
    private MapFactory mapFactory = null;

    public CachesOverlay(cgSettings settingsIn, ItemizedOverlayImpl ovlImpl, Context contextIn, Boolean fromDetailIn) {
        super(ovlImpl);

        populate();
        settings = settingsIn;

        context = contextIn;
        fromDetail = fromDetailIn;

        mapFactory = settings.getMapFactory();
    }

    public void updateItems(CachesOverlayItemImpl item) {
        List<CachesOverlayItemImpl> itemsPre = new ArrayList<CachesOverlayItemImpl>();
        itemsPre.add(item);

        updateItems(itemsPre);
    }

    public void updateItems(List<CachesOverlayItemImpl> itemsPre) {
        if (itemsPre == null) {
            return;
        }

        for (CachesOverlayItemImpl item : itemsPre) {
            item.setMarker(boundCenterBottom(item.getMarker(0)));
        }

        // ensure no interference between the draw and content changing routines
        getOverlayImpl().lock();
        try {
            items = new ArrayList<CachesOverlayItemImpl>(itemsPre);

            setLastFocusedItemIndex(-1); // to reset tap during data change
            populate();
        } finally {
            getOverlayImpl().unlock();
        }
    }

    public boolean getCircles() {
        return displayCircles;
    }

    public void switchCircles() {
        displayCircles = !displayCircles;
    }

    @Override
    public void draw(Canvas canvas, MapViewImpl mapView, boolean shadow) {

        drawInternal(canvas, mapView.getMapProjection());

        super.draw(canvas, mapView, false);
    }

    @Override
    public void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            MapProjectionImpl projection, byte drawZoomLevel) {

        drawInternal(canvas, projection);

        super.drawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
    }

    private void drawInternal(Canvas canvas, MapProjectionImpl projection) {

        // prevent content changes
        getOverlayImpl().lock();
        try {
            if (displayCircles) {
                if (blockedCircle == null) {
                    blockedCircle = new Paint();
                    blockedCircle.setAntiAlias(true);
                    blockedCircle.setStrokeWidth(1.0f);
                    blockedCircle.setARGB(127, 0, 0, 0);
                    blockedCircle.setPathEffect(new DashPathEffect(new float[] { 3, 2 }, 0));
                }

                if (setfil == null)
                    setfil = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
                if (remfil == null)
                    remfil = new PaintFlagsDrawFilter(Paint.FILTER_BITMAP_FLAG, 0);

                canvas.setDrawFilter(setfil);

                for (CachesOverlayItemImpl item : items) {
                    final cgCoord itemCoord = item.getCoord();
                    float[] result = new float[1];

                    Location.distanceBetween(itemCoord.coords.getLatitude(), itemCoord.coords.getLongitude(),
                            itemCoord.coords.getLatitude(), itemCoord.coords.getLongitude() + 1, result);
                    final float longitudeLineDistance = result[0];

                    GeoPointImpl itemGeo = mapFactory.getGeoPointBase(itemCoord.coords);

                    final Geopoint leftCoords = new Geopoint(itemCoord.coords.getLatitude(),
                            itemCoord.coords.getLongitude() - 161 / longitudeLineDistance);
                    GeoPointImpl leftGeo = mapFactory.getGeoPointBase(leftCoords);

                    projection.toPixels(itemGeo, center);
                    projection.toPixels(leftGeo, left);
                    int radius = center.x - left.x;

                    final String type = item.getType();
                    if (type == null || "multi".equals(type) || "mystery".equals(type) || "virtual".equals(type)) {
                        blockedCircle.setColor(0x66000000);
                        blockedCircle.setStyle(Style.STROKE);
                        canvas.drawCircle(center.x, center.y, radius, blockedCircle);
                    } else {
                        blockedCircle.setColor(0x66BB0000);
                        blockedCircle.setStyle(Style.STROKE);
                        canvas.drawCircle(center.x, center.y, radius, blockedCircle);

                        blockedCircle.setColor(0x44BB0000);
                        blockedCircle.setStyle(Style.FILL);
                        canvas.drawCircle(center.x, center.y, radius, blockedCircle);
                    }
                }
                canvas.setDrawFilter(remfil);
            }
        } finally {
            getOverlayImpl().unlock();
        }
    }

    @Override
    public boolean onTap(int index) {

        try {
            if (items.size() <= index) {
                return false;
            }

            if (waitDialog == null) {
                waitDialog = new ProgressDialog(context);
                waitDialog.setMessage("loading details...");
                waitDialog.setCancelable(false);
            }
            waitDialog.show();

            CachesOverlayItemImpl item = null;

            // prevent concurrent changes
            getOverlayImpl().lock();
            try {
                if (index < items.size()) {
                    item = items.get(index);
                }
            } finally {
                getOverlayImpl().unlock();
            }

            if (item == null) {
                return false;
            }

            cgCoord coordinate = item.getCoord();

            if (StringUtils.isNotBlank(coordinate.type) && coordinate.type.equalsIgnoreCase("cache") && StringUtils.isNotBlank(coordinate.geocode)) {
                Intent popupIntent = new Intent(context, cgeopopup.class);

                popupIntent.putExtra("fromdetail", fromDetail);
                popupIntent.putExtra("geocode", coordinate.geocode);

                context.startActivity(popupIntent);
            } else if (coordinate.type != null && coordinate.type.equalsIgnoreCase("waypoint") && coordinate.id != null && coordinate.id > 0) {
                Intent popupIntent = new Intent(context, cgeowaypoint.class);

                popupIntent.putExtra("waypoint", coordinate.id);
                popupIntent.putExtra("geocode", coordinate.geocode);

                context.startActivity(popupIntent);
            } else {
                waitDialog.dismiss();
                return false;
            }

            waitDialog.dismiss();
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgMapOverlay.onTap: " + e.toString());
        }

        return false;
    }

    @Override
    public CachesOverlayItemImpl createItem(int index) {
        try {
            return items.get(index);
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgMapOverlay.createItem: " + e.toString());
        }

        return null;
    }

    @Override
    public int size() {
        try {
            return items.size();
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgMapOverlay.size: " + e.toString());
        }

        return 0;
    }

    public void infoDialog(int index) {

        final CachesOverlayItemImpl item = items.get(index);
        final cgCoord coordinate = item.getCoord();

        if (coordinate == null) {
            Log.e(cgSettings.tag, "cgMapOverlay:infoDialog: No coordinates given");
            return;
        }

        try {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setCancelable(true);

            if (coordinate.type.equalsIgnoreCase("cache")) {
                dialog.setTitle("cache");

                String cacheType;
                if (cgBase.cacheTypesInv.containsKey(coordinate.typeSpec)) {
                    cacheType = cgBase.cacheTypesInv.get(coordinate.typeSpec);
                } else {
                    cacheType = cgBase.cacheTypesInv.get("mystery");
                }

                dialog.setMessage(Html.fromHtml(item.getTitle()) + "\n\ngeocode: " + coordinate.geocode.toUpperCase() + "\ntype: " + cacheType);
                if (fromDetail == false) {
                    dialog.setPositiveButton("detail", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            Intent cachesIntent = new Intent(context, cgeodetail.class);
                            cachesIntent.putExtra("geocode", coordinate.geocode.toUpperCase());
                            context.startActivity(cachesIntent);

                            dialog.cancel();
                        }
                    });
                } else {
                    dialog.setPositiveButton("navigate", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            cgeonavigate navigateActivity = new cgeonavigate();

                            cgeonavigate.coordinates.clear();
                            cgeonavigate.coordinates.add(coordinate);

                            Intent navigateIntent = new Intent(context, navigateActivity.getClass());
                            navigateIntent.putExtra("latitude", coordinate.coords.getLatitude());
                            navigateIntent.putExtra("longitude", coordinate.coords.getLongitude());
                            navigateIntent.putExtra("geocode", coordinate.geocode.toUpperCase());
                            context.startActivity(navigateIntent);
                            dialog.cancel();
                        }
                    });
                }
            } else {
                dialog.setTitle("waypoint");

                String waypointType;
                if (cgBase.cacheTypesInv.containsKey(coordinate.typeSpec)) {
                    waypointType = cgBase.waypointTypes.get(coordinate.typeSpec);
                } else {
                    waypointType = cgBase.waypointTypes.get("waypoint");
                }

                dialog.setMessage(Html.fromHtml(item.getTitle()) + "\n\ntype: " + waypointType);
                dialog.setPositiveButton("navigate", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        cgeonavigate navigateActivity = new cgeonavigate();

                        cgeonavigate.coordinates.clear();
                        cgeonavigate.coordinates.add(coordinate);

                        Intent navigateIntent = new Intent(context, navigateActivity.getClass());
                        navigateIntent.putExtra("latitude", coordinate.coords.getLatitude());
                        navigateIntent.putExtra("longitude", coordinate.coords.getLongitude());
                        navigateIntent.putExtra("geocode", coordinate.name);

                        context.startActivity(navigateIntent);
                        dialog.cancel();
                    }
                });
            }

            dialog.setNegativeButton("dismiss", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            AlertDialog alert = dialog.create();
            alert.show();
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgMapOverlay.infoDialog: " + e.toString());
        }
    }
}

package cgeo.geocaching;

import cgeo.geocaching.activity.IAbstractActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.Spannable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal c:geo representation of a "cache"
 */
public class cgCache implements ICache {

    public Long updated = null;
    public Long detailedUpdate = null;
    public Long visitedDate = null;
    public Integer reason = 0;
    public Boolean detailed = false;
    /**
     * Code of the cache like GCABCD
     */
    public String geocode = "";
    public String cacheId = "";
    public String guid = "";
    public String type = "";
    public String name = "";
    public Spannable nameSp = null;
    public String owner = "";
    public String ownerReal = "";
    public Date hidden = null;
    public String hint = "";
    public CacheSize size = null;
    public Float difficulty = Float.valueOf(0);
    public Float terrain = Float.valueOf(0);
    public Float direction = null;
    public Float distance = null;
    public String latlon = "";
    public String latitudeString = "";
    public String longitudeString = "";
    public String location = "";
    public Geopoint coords = null;
    public boolean reliableLatLon = false;
    public Double elevation = null;
    public String personalNote = null;
    public String shortdesc = "";
    public String description = "";
    public boolean disabled = false;
    public boolean archived = false;
    public boolean members = false;
    public boolean found = false;
    public boolean favourite = false;
    public boolean own = false;
    public Integer favouriteCnt = null;
    public Float rating = null;
    public Integer votes = null;
    public Float myVote = null;
    public int inventoryItems = 0;
    public boolean onWatchlist = false;
    public List<String> attributes = null;
    public List<cgWaypoint> waypoints = null;
    public List<cgImage> spoilers = null;
    public List<cgLog> logs = null;
    public List<cgTrackable> inventory = null;
    public Map<Integer, Integer> logCounts = new HashMap<Integer, Integer>();
    public boolean logOffline = false;
    // temporary values
    public boolean statusChecked = false;
    public boolean statusCheckedView = false;
    public String directionImg = null;

    /**
     * Gather missing information from another cache object.
     *
     * @param other
     *            the other version, or null if non-existent
     */
    public void gatherMissingFrom(final cgCache other) {
        if (other == null) {
            return;
        }

        updated = System.currentTimeMillis();
        if (detailed == false && other.detailed) {
            detailed = true;
            detailedUpdate = updated;
        }

        if (visitedDate == null || visitedDate == 0) {
            visitedDate = other.visitedDate;
        }
        if (reason == null || reason == 0) {
            reason = other.reason;
        }
        if (StringUtils.isBlank(geocode)) {
            geocode = other.geocode;
        }
        if (StringUtils.isBlank(cacheId)) {
            cacheId = other.cacheId;
        }
        if (StringUtils.isBlank(guid)) {
            guid = other.guid;
        }
        if (StringUtils.isBlank(type)) {
            type = other.type;
        }
        if (StringUtils.isBlank(name)) {
            name = other.name;
        }
        if (StringUtils.isBlank(nameSp)) {
            nameSp = other.nameSp;
        }
        if (StringUtils.isBlank(owner)) {
            owner = other.owner;
        }
        if (StringUtils.isBlank(ownerReal)) {
            ownerReal = other.ownerReal;
        }
        if (hidden == null) {
            hidden = other.hidden;
        }
        if (StringUtils.isBlank(hint)) {
            hint = other.hint;
        }
        if (size == null) {
            size = other.size;
        }
        if (difficulty == null || difficulty == 0) {
            difficulty = other.difficulty;
        }
        if (terrain == null || terrain == 0) {
            terrain = other.terrain;
        }
        if (direction == null) {
            direction = other.direction;
        }
        if (distance == null) {
            distance = other.distance;
        }
        if (StringUtils.isBlank(latlon)) {
            latlon = other.latlon;
        }
        if (StringUtils.isBlank(latitudeString)) {
            latitudeString = other.latitudeString;
        }
        if (StringUtils.isBlank(longitudeString)) {
            longitudeString = other.longitudeString;
        }
        if (StringUtils.isBlank(location)) {
            location = other.location;
        }
        if (coords == null) {
            coords = other.coords;
        }
        if (elevation == null) {
            elevation = other.elevation;
        }
        if (StringUtils.isNotBlank(personalNote)) {
            personalNote = other.personalNote;
        }
        if (StringUtils.isBlank(shortdesc)) {
            shortdesc = other.shortdesc;
        }
        if (StringUtils.isBlank(description)) {
            description = other.description;
        }
        if (favouriteCnt == null) {
            favouriteCnt = other.favouriteCnt;
        }
        if (rating == null) {
            rating = other.rating;
        }
        if (votes == null) {
            votes = other.votes;
        }
        if (myVote == null) {
            myVote = other.myVote;
        }
        if (attributes == null) {
            attributes = other.attributes;
        }
        if (waypoints == null) {
            waypoints = other.waypoints;
        }
        cgWaypoint.mergeWayPoints(waypoints, other.waypoints);
        if (spoilers == null) {
            spoilers = other.spoilers;
        }
        if (inventory == null) {
            // If inventoryItems is 0, it can mean both
            // "don't know" or "0 items". Since we cannot distinguish
            // them here, only populate inventoryItems from
            // old data when we have to do it for inventory.
            inventory = other.inventory;
            inventoryItems = other.inventoryItems;
        }
        if (logs == null || logs.isEmpty()) { // keep last known logs if none
            logs = other.logs;
        }
    }

    public boolean hasTrackables() {
        return inventoryItems > 0;
    }

    public boolean canBeAddedToCalendar() {
        // is event type?
        if (!isEventCache()) {
            return false;
        }
        // has event date set?
        if (hidden == null) {
            return false;
        }
        // is in future?
        Date today = new Date();
        today.setHours(0);
        today.setMinutes(0);
        today.setSeconds(0);
        if (hidden.compareTo(today) <= 0) {
            return false;
        }
        return true;
    }

    /**
     * checks if a page contains the guid of a cache
     *
     * @param cache
     *            the cache to look for
     * @param page
     *            the page to search in
     *
     * @return true: page contains guid of cache, false: otherwise
     */
    boolean isGuidContainedInPage(final String page) {
        // check if the guid of the cache is anywhere in the page
        if (StringUtils.isBlank(guid)) {
            return false;
        }
        Pattern patternOk = Pattern.compile(guid, Pattern.CASE_INSENSITIVE);
        Matcher matcherOk = patternOk.matcher(page);
        if (matcherOk.find()) {
            Log.i(cgSettings.tag, "cgCache.isGuidContainedInPage: guid '" + guid + "' found");
            return true;
        } else {
            Log.i(cgSettings.tag, "cgCache.isGuidContainedInPage: guid '" + guid + "' not found");
            return false;
        }
    }

    public boolean isEventCache() {
        return "event".equalsIgnoreCase(type) || "mega".equalsIgnoreCase(type) || "cito".equalsIgnoreCase(type);
    }

    public boolean logVisit(IAbstractActivity fromActivity) {
        if (StringUtils.isBlank(cacheId)) {
            fromActivity.showToast(((Activity) fromActivity).getResources().getString(R.string.err_cannot_log_visit));
            return true;
        }
        Intent logVisitIntent = new Intent((Activity) fromActivity, cgeovisit.class);
        logVisitIntent.putExtra(cgeovisit.EXTRAS_ID, cacheId);
        logVisitIntent.putExtra(cgeovisit.EXTRAS_GEOCODE, geocode.toUpperCase());
        logVisitIntent.putExtra(cgeovisit.EXTRAS_FOUND, found);

        ((Activity) fromActivity).startActivity(logVisitIntent);

        return true;
    }

    public boolean logOffline(final IAbstractActivity fromActivity, final int logType, final cgSettings settings, final cgBase base) {
        String log = "";
        if (StringUtils.isNotBlank(settings.getSignature())
                && settings.signatureAutoinsert) {
            log = LogTemplateProvider.applyTemplates(settings.getSignature(), base, true);
        }
        logOffline(fromActivity, log, Calendar.getInstance(), logType);
        return true;
    }

    void logOffline(final IAbstractActivity fromActivity, final String log, Calendar date, final int logType) {
        if (logType <= 0) {
            return;
        }
        cgeoapplication app = (cgeoapplication) ((Activity) fromActivity).getApplication();
        final boolean status = app.saveLogOffline(geocode, date.getTime(), logType, log);

        Resources res = ((Activity) fromActivity).getResources();
        if (status) {
            fromActivity.showToast(res.getString(R.string.info_log_saved));
            app.saveVisitDate(geocode);
        } else {
            fromActivity.showToast(res.getString(R.string.err_log_post_failed));
        }
    }

    public List<Integer> getPossibleLogTypes(cgSettings settings) {
        boolean isOwner = owner != null && owner.equalsIgnoreCase(settings.getUsername());
        List<Integer> types = new ArrayList<Integer>();
        if ("event".equals(type) || "mega".equals(type) || "cito".equals(type) || "lostfound".equals(type)) {
            types.add(cgBase.LOG_WILL_ATTEND);
            types.add(cgBase.LOG_NOTE);
            types.add(cgBase.LOG_ATTENDED);
            types.add(cgBase.LOG_NEEDS_ARCHIVE);
            if (isOwner) {
                types.add(cgBase.LOG_ANNOUNCEMENT);
            }
        } else if ("webcam".equals(type)) {
            types.add(cgBase.LOG_WEBCAM_PHOTO_TAKEN);
            types.add(cgBase.LOG_DIDNT_FIND_IT);
            types.add(cgBase.LOG_NOTE);
            types.add(cgBase.LOG_NEEDS_ARCHIVE);
            types.add(cgBase.LOG_NEEDS_MAINTENANCE);
        } else {
            types.add(cgBase.LOG_FOUND_IT);
            types.add(cgBase.LOG_DIDNT_FIND_IT);
            types.add(cgBase.LOG_NOTE);
            types.add(cgBase.LOG_NEEDS_ARCHIVE);
            types.add(cgBase.LOG_NEEDS_MAINTENANCE);
        }
        if (isOwner) {
            types.add(cgBase.LOG_OWNER_MAINTENANCE);
            types.add(cgBase.LOG_TEMP_DISABLE_LISTING);
            types.add(cgBase.LOG_ENABLE_LISTING);
            types.add(cgBase.LOG_ARCHIVE);
            types.remove(Integer.valueOf(cgBase.LOG_UPDATE_COORDINATES));
        }
        return types;
    }

    public void openInBrowser(Activity fromActivity) {
        fromActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getCacheUrl())));
    }

    private String getCacheUrl() {
        return getConnector().getCacheUrl(this);
    }

    private IConnector getConnector() {
        return ConnectorFactory.getConnector(this);
    }

    public boolean canOpenInBrowser() {
        return getCacheUrl() != null;
    }

    public boolean supportsRefresh() {
        return getConnector().supportsRefreshCache(this);
    }

    public boolean supportsWatchList() {
        return getConnector().supportsWatchList();
    }

    public boolean supportsLogging() {
        return getConnector().supportsLogging();
    }

    @Override
    public Float getDifficulty() {
        return difficulty;
    }

    @Override
    public String getGeocode() {
        return geocode;
    }

    @Override
    public String getLatitude() {
        return latitudeString;
    }

    @Override
    public String getLongitude() {
        return longitudeString;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public CacheSize getSize() {
        return size;
    }

    @Override
    public Float getTerrain() {
        return terrain;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public boolean isArchived() {
        return archived;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public boolean isMembersOnly() {
        return members;
    }

    @Override
    public boolean isOwn() {
        return own;
    }

    @Override
    public String getOwnerReal() {
        return ownerReal;
    }

    @Override
    public String getHint() {
        return hint;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getShortDescription() {
        return shortdesc;
    }

    @Override
    public String getName() {
        return name;
    }

}

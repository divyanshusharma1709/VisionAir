package com.airquality.VisionAir;


public class cpcbCenterList {

    String mlocationName;
    double mlat, mlon;
    cpcbCenterList(String locationName, double lat, double lon) {
        mlocationName = locationName;
        mlat = lat;
        mlon = lon;
    }


    String getMlocationName() {
        return mlocationName;
    }

    double getMlat() {
        return mlat;
    }

    double getMlon() {
        return mlon;
    }

}

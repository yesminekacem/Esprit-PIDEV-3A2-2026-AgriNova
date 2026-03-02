package tn.esprit.marketplace.controller;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class DeliveryMapDialogController {

    @FXML private WebView mapWebView;
    @FXML private Label coordsLabel;
    @FXML private TextArea addressArea;
    @FXML private Button confirmBtn;

    private double selectedLatitude = 36.8065;
    private double selectedLongitude = 10.1815;
    private String selectedAddress = "";
    private boolean confirmed = false;

    private Double initialLat = null;
    private Double initialLng = null;

    public void setInitialLocation(double lat, double lng) {
        this.initialLat = lat;
        this.initialLng = lng;
    }

    @FXML
    public void initialize() {
        confirmBtn.setDisable(true);

        WebEngine engine = mapWebView.getEngine();
        engine.loadContent(buildHtml());

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                confirmBtn.setDisable(false);

                if (initialLat != null && initialLng != null) {
                    engine.executeScript("window.setInitialLocation(" + initialLat + "," + initialLng + ");");
                }

                updateFromMap();
            }
        });
    }

    private void updateFromMap() {
        Object result = mapWebView.getEngine()
                .executeScript("window.getSelectedLocation && window.getSelectedLocation();");

        if (result instanceof JSObject js) {
            Object lat = js.getMember("lat");
            Object lng = js.getMember("lng");
            Object addr = js.getMember("address");

            if (lat instanceof Number) selectedLatitude = ((Number) lat).doubleValue();
            if (lng instanceof Number) selectedLongitude = ((Number) lng).doubleValue();
            if (addr != null) selectedAddress = String.valueOf(addr);
        }

        coordsLabel.setText(String.format("Latitude: %.6f, Longitude: %.6f", selectedLatitude, selectedLongitude));
        addressArea.setText(selectedAddress == null ? "" : selectedAddress);
    }

    @FXML
    private void handleConfirm() {
        updateFromMap();
        confirmed = true;
        ((Stage) confirmBtn.getScene().getWindow()).close();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        ((Stage) confirmBtn.getScene().getWindow()).close();
    }

    public boolean isConfirmed() { return confirmed; }
    public double getSelectedLatitude() { return selectedLatitude; }
    public double getSelectedLongitude() { return selectedLongitude; }

    public String getSelectedAddress() {
        if (selectedAddress != null && !selectedAddress.isBlank()) return selectedAddress;
        return String.format("Lat: %.6f, Lng: %.6f", selectedLatitude, selectedLongitude);
    }

    private String buildHtml() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
              html, body, #map { height: 100%; margin: 0; }
            </style>
        </head>
        <body>
        <div id="map"></div>

        <script>
          let map, marker;
          const tunis = [36.8065, 10.1815];

          map = L.map('map').setView(tunis, 16);
          L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
            { attribution: '© OpenStreetMap contributors', maxZoom: 19 }).addTo(map);

          function setData(lat, lng, address) {
            document.body.dataset.lat = String(lat);
            document.body.dataset.lng = String(lng);
            document.body.dataset.address = address || '';
          }

          async function reverse(lat, lng) {
            try {
              const url =
                        'https://nominatim.openstreetmap.org/reverse?format=jsonv2' +
                        '&lat=' + lat +
                        '&lon=' + lng +
                        '&accept-language=en';

              const r = await fetch(url, { headers: { 'Accept': 'application/json' } });
              const data = await r.json();
              setData(lat, lng, data.display_name || '');
            } catch(e) {
              setData(lat, lng, '');
            }
          }

          function placeMarker(lat, lng) {
            if (!marker) {
              marker = L.marker([lat, lng]).addTo(map).bindPopup('Delivery location');
            } else {
              marker.setLatLng([lat, lng]);
            }
            reverse(lat, lng);
          }

          map.on('click', function(e) {
            placeMarker(e.latlng.lat, e.latlng.lng);
          });

          window.setInitialLocation = function(lat, lng) {
            map.setView([lat, lng], 18);
            placeMarker(lat, lng);
          };

          // Only auto-geolocate if we don't already have an initial location
          if (!document.body.dataset.lat || !document.body.dataset.lng) {
            navigator.geolocation.getCurrentPosition(
              (pos) => {
                const { latitude, longitude } = pos.coords;
                map.setView([latitude, longitude], 18);
                placeMarker(latitude, longitude);
              },
              () => placeMarker(tunis[0], tunis[1])
            );
          }

          window.getSelectedLocation = function() {
            const lat = parseFloat(document.body.dataset.lat || '36.8065');
            const lng = parseFloat(document.body.dataset.lng || '10.1815');
            const address = document.body.dataset.address || '';
            return { lat, lng, address };
          };
        </script>

        </body>
        </html>
        """;
    }
}

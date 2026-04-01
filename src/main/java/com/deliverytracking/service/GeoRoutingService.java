package com.deliverytracking.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Spring Framework Imports
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.deliverytracking.entity.Hub;
import com.deliverytracking.repository.HubRepository;

import lombok.RequiredArgsConstructor;
//
//@Service
//@RequiredArgsConstructor
//public class GeoRoutingService {
//
//    private final HubRepository hubRepository;
//    private final RestTemplate restTemplate;
//
//    // Step 1: Geocode an address → lat/lng using Nominatim (free, no key needed)
//    public double[] geocodeAddress(String address) {
//       try {
//    	   String url = "https://nominatim.openstreetmap.org/search?q="
//                   + URLEncoder.encode(address, StandardCharsets.UTF_8)
//                   + "&format=json&limit=1";
//
//           HttpHeaders headers = new HttpHeaders();
//           headers.set("User-Agent", "DeliveryTrackingSystem/1.0");
//           HttpEntity<String> entity = new HttpEntity<>(headers);
//
//           ResponseEntity<List> response = restTemplate.exchange(
//               url, HttpMethod.GET, entity, List.class);
//
//           Map<String, Object> result = (Map<String, Object>) response.getBody().get(0);
//           double lat = Double.parseDouble((String) result.get("lat"));
//           double lon = Double.parseDouble((String) result.get("lon"));
//           return new double[]{lat, lon};
//       }catch (Exception e) {
//    	    throw new RuntimeException(
//    	            "Could not find location: '" + address + "'. " +
//    	            "Please enter a more specific address.");
//    	    }
//    }
//
//    // Step 2: Find nearest hub using Haversine formula (no external API needed)
//    public Hub findNearestHub(double lat, double lng) {
//        List<Hub> allHubs = hubRepository.findByActiveTrue(); // only active hubs
//        if (allHubs.isEmpty()) {
//            throw new RuntimeException("No active hubs configured in the system.");
//        }
////        return allHubs.stream()
////            .min(Comparator.comparingDouble(h ->
////                haversine(lat, lng, h.getLatitude(), h.getLongitude())))
////            .orElseThrow();
//        
//        Hub nearest= allHubs.stream()
//                .min(Comparator.comparingDouble(h ->
//                    haversine(lat, lng, h.getLatitude(), h.getLongitude())))
//                .orElseThrow();
//        
//        double distance = haversine(lat, lng,
//                nearest.getLatitude(), nearest.getLongitude());
//
//            if (distance > 200) { // 200 km threshold
//                throw new RuntimeException(
//                    "No hub available within 200km of this location. " +
//                    "Service not available in this area yet.");
//            }
//            return nearest;
//    }
//    // Step 3: Find intermediate hubs between origin and destination
//    // Returns ordered list: [originHub, ...intermediateHubs..., destinationHub]
////    public List<Hub> buildHubRoute(Hub originHub, Hub destinationHub) {
////        List<Hub> allHubs = hubRepository.findAll();
////        List<Hub> candidates = new ArrayList<>();
////
////        // Filter hubs that lie roughly between origin and destination
////        // Using bounding box approach
////        double minLat = Math.min(originHub.getLatitude(), destinationHub.getLatitude()) - 0.5;
////        double maxLat = Math.max(originHub.getLatitude(), destinationHub.getLatitude()) + 0.5;
////        double minLng = Math.min(originHub.getLongitude(), destinationHub.getLongitude()) - 0.5;
////        double maxLng = Math.max(originHub.getLongitude(), destinationHub.getLongitude()) + 0.5;
////
////        for (Hub h : allHubs) {
////            if (h.getId().equals(originHub.getId()) || h.getId().equals(destinationHub.getId()))
////                continue;
////            if (h.getLatitude() >= minLat && h.getLatitude() <= maxLat
////                    && h.getLongitude() >= minLng && h.getLongitude() <= maxLng) {
////                candidates.add(h);
////            }
////        }
////
////        // Use OSRM to get optimal road order for all waypoints (free, no key needed)
////        List<Hub> allWaypoints = new ArrayList<>();
////        allWaypoints.add(originHub);
////        allWaypoints.addAll(candidates);
////        allWaypoints.add(destinationHub);
////
////        return orderHubsByOSRM(allWaypoints, originHub, destinationHub);
////    }
//    
//    public List<Hub> buildHubRoute(Hub originHub, Hub destinationHub) {
//        List<Hub> allHubs = hubRepository.findAll();
//        List<Hub> candidates = new ArrayList<>();
//
//        double totalDist = haversine(
//            originHub.getLatitude(), originHub.getLongitude(),
//            destinationHub.getLatitude(), destinationHub.getLongitude()
//        );
//
//        // Scale detour tolerance based on route length
//        double detourThreshold;
//        if (totalDist < 400) {
//            detourThreshold = totalDist * 0.10;
//        } else if (totalDist < 800) {
//            detourThreshold = totalDist * 0.15;
//        } else {
//            detourThreshold = totalDist * 0.25;
//        }
//
//        // Scale min useful distance based on route length
//        double minUsefulDistance = Math.max(80, totalDist * 0.15);
//
//        for (Hub h : allHubs) {
//            if (h.getId().equals(originHub.getId()) || h.getId().equals(destinationHub.getId()))
//                continue;
//
//            double distFromOrigin = haversine(
//                originHub.getLatitude(), originHub.getLongitude(),
//                h.getLatitude(), h.getLongitude()
//            );
//            double distToDestination = haversine(
//                h.getLatitude(), h.getLongitude(),
//                destinationHub.getLatitude(), destinationHub.getLongitude()
//            );
//
//            // Skip hubs too close to either endpoint
//            if (distFromOrigin < minUsefulDistance || distToDestination < minUsefulDistance) {
//                continue;
//            }
//
//            // Ellipse check — hub must lie along the corridor
//            if ((distFromOrigin + distToDestination) <= (totalDist + detourThreshold)) {
//                candidates.add(h);
//            }
//        }
//
//        List<Hub> allWaypoints = new ArrayList<>();
//        allWaypoints.add(originHub);
//        allWaypoints.addAll(candidates);
//        allWaypoints.add(destinationHub);
//
//        return orderHubsByOSRM(allWaypoints, originHub, destinationHub);
//    }
//
//    // OSRM Trip API — orders waypoints optimally along real roads
//    private List<Hub> orderHubsByOSRM(List<Hub> hubs, Hub origin, Hub destination) {
//        if (hubs.size() <= 2) return hubs; // no intermediates, skip API call
//
//        StringBuilder coords = new StringBuilder();
//        for (Hub h : hubs) {
//            coords.append(h.getLongitude()).append(",").append(h.getLatitude()).append(";");
//        }
//        String coordStr = coords.toString().replaceAll(";$", "");
//
//        // source=first,destination=last keeps origin and destination fixed
//        String url = "http://router.project-osrm.org/trip/v1/driving/"
//                + coordStr + "?source=first&destination=last&roundtrip=false";
//
//        try {
//            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
//            List<Map<String, Object>> waypoints = (List<Map<String, Object>>) response.get("waypoints");
//
//            // OSRM returns waypoints with waypoint_index = optimized position
//            Map<Integer, Hub> indexToHub = new HashMap<>();
//            for (int i = 0; i < waypoints.size(); i++) {
//                int waypointIndex = (int) waypoints.get(i).get("waypoint_index");
//                indexToHub.put(waypointIndex, hubs.get(i));
//            }
//
//            List<Hub> ordered = new ArrayList<>();
//            for (int i = 0; i < indexToHub.size(); i++) {
//                ordered.add(indexToHub.get(i));
//            }
//            return ordered;
//        } catch (Exception e) {
//            // Fallback: sort by distance from origin if OSRM fails
//            return hubs.stream()
//                .sorted(Comparator.comparingDouble(h ->
//                    haversine(origin.getLatitude(), origin.getLongitude(), h.getLatitude(), h.getLongitude())))
//                .collect(Collectors.toList());
//        }
//    }
//
//    // Haversine formula — distance in km between two lat/lng points
//    private double haversine(double lat1, double lng1, double lat2, double lng2) {
//        double R = 6371;
//        double dLat = Math.toRadians(lat2 - lat1);
//        double dLng = Math.toRadians(lng2 - lng1);
//        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
//                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
//                 * Math.sin(dLng/2) * Math.sin(dLng/2);
//        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
//    }
//}

@Service
@RequiredArgsConstructor
public class GeoRoutingService {

    private final HubRepository hubRepository;
    private final RestTemplate restTemplate;

    // Step 1: Geocode an address → lat/lng using Nominatim
    public double[] geocodeAddress(String address) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?q="
                    + URLEncoder.encode(address, StandardCharsets.UTF_8)
                    + "&format=json&limit=1";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "DeliveryTrackingSystem/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, List.class);

            Map<String, Object> result = (Map<String, Object>) response.getBody().get(0);
            double lat = Double.parseDouble((String) result.get("lat"));
            double lon = Double.parseDouble((String) result.get("lon"));
            return new double[]{lat, lon};
        } catch (Exception e) {
            throw new RuntimeException(
                "Could not find location: '" + address + "'. " +
                "Please enter a more specific address.");
        }
    }

    // Step 2: Find nearest hub using Haversine formula
//    public Hub findNearestHub(double lat, double lng) {
//        List<Hub> allHubs = hubRepository.findByActiveTrue();
//        if (allHubs.isEmpty()) {
//            throw new RuntimeException("No active hubs configured in the system.");
//        }
//
//        Hub nearest = allHubs.stream()
//                .min(Comparator.comparingDouble(h ->
//                    haversine(lat, lng, h.getLatitude(), h.getLongitude())))
//                .orElseThrow();
//
//        double distance = haversine(lat, lng, nearest.getLatitude(), nearest.getLongitude());
//        if (distance > 200) {
//            throw new RuntimeException(
//                "No hub available within 200km of this location. " +
//                "Service not available in this area yet.");
//        }
//        return nearest;
//    }
    
    public Hub findNearestHub(double lat, double lng) {
        List<Hub> allHubs = hubRepository.findByActiveTrue();
        if (allHubs.isEmpty()) {
            throw new RuntimeException("No active hubs configured in the system.");
        }

        // ADD THIS: log all hubs and their distances
        allHubs.forEach(h -> {
            double dist = haversine(lat, lng, h.getLatitude(), h.getLongitude());
//            System.out.println("Hub: " + h.getName() + 
//                               " | Lat: " + h.getLatitude() + 
//                               " | Lng: " + h.getLongitude() + 
//                               " | Distance: " + dist + " km");
        });

        Hub nearest = allHubs.stream()
                .min(Comparator.comparingDouble(h ->
                    haversine(lat, lng, h.getLatitude(), h.getLongitude())))
                .orElseThrow();

//        System.out.println(">>> Nearest hub selected: " + nearest.getName() + 
//                           " | Lat: " + nearest.getLatitude() + 
//                           " | Lng: " + nearest.getLongitude());

        double distance = haversine(lat, lng, nearest.getLatitude(), nearest.getLongitude());
        if (distance > 200) {
            throw new RuntimeException(
                "No hub available within 200km of this location. " +
                "Service not available in this area yet.");
        }
        return nearest;
    }

    // Step 3: Build optimal hub route from origin to destination
    public List<Hub> buildHubRoute(Hub originHub, Hub destinationHub) {

        if (originHub.getId().equals(destinationHub.getId())) {
            return List.of(originHub);
        }

        List<Hub> allHubs = hubRepository.findByActiveTrue();
        List<Hub> candidates = new ArrayList<>();

        double totalDist = haversine(
            originHub.getLatitude(), originHub.getLongitude(),
            destinationHub.getLatitude(), destinationHub.getLongitude()
        );

        // Strict detour threshold — only hubs nearly on the straight line
        double detourThreshold = totalDist * 0.08; // 8% max detour for all route lengths

        // Min useful distance — hub must be meaningfully far from both endpoints
        double minUsefulDistance = Math.max(100, totalDist * 0.20);

        for (Hub h : allHubs) {
            if (h.getId().equals(originHub.getId()) || h.getId().equals(destinationHub.getId()))
                continue;

            double distFromOrigin = haversine(
                originHub.getLatitude(), originHub.getLongitude(),
                h.getLatitude(), h.getLongitude()
            );
            double distToDestination = haversine(
                h.getLatitude(), h.getLongitude(),
                destinationHub.getLatitude(), destinationHub.getLongitude()
            );

            // Rule 1: Must be meaningfully far from both endpoints
            if (distFromOrigin < minUsefulDistance || distToDestination < minUsefulDistance)
                continue;

            // Rule 2: Must not be farther than total route distance
            if (distFromOrigin > totalDist || distToDestination > totalDist)
                continue;

            // Rule 3: Must be in forward direction from origin toward destination
            double vecOriginDestLat = destinationHub.getLatitude() - originHub.getLatitude();
            double vecOriginDestLng = destinationHub.getLongitude() - originHub.getLongitude();
            double vecOriginHubLat  = h.getLatitude()  - originHub.getLatitude();
            double vecOriginHubLng  = h.getLongitude() - originHub.getLongitude();
            double dotProduct = (vecOriginHubLat * vecOriginDestLat)
                              + (vecOriginHubLng * vecOriginDestLng);
            if (dotProduct <= 0) continue;

            // Rule 4: Must not be past the destination
            double vecDestOriginLat = originHub.getLatitude()  - destinationHub.getLatitude();
            double vecDestOriginLng = originHub.getLongitude() - destinationHub.getLongitude();
            double vecDestHubLat    = h.getLatitude()  - destinationHub.getLatitude();
            double vecDestHubLng    = h.getLongitude() - destinationHub.getLongitude();
            double dotProduct2 = (vecDestHubLat * vecDestOriginLat)
                               + (vecDestHubLng * vecDestOriginLng);
            if (dotProduct2 <= 0) continue;

            // Rule 5: Strict ellipse check
            if ((distFromOrigin + distToDestination) > (totalDist + detourThreshold))
                continue;

            candidates.add(h);
        }

        // Rule 6: Cap max intermediate hubs based on route distance
        int maxIntermediates = getMaxIntermediates(totalDist);

        if (candidates.size() > maxIntermediates) {
            // Keep only the best hubs — those closest to the straight-line path
            candidates = candidates.stream()
                .sorted(Comparator.comparingDouble(h -> {
                    double dFromOrigin = haversine(
                        originHub.getLatitude(), originHub.getLongitude(),
                        h.getLatitude(), h.getLongitude()
                    );
                    double dToDestination = haversine(
                        h.getLatitude(), h.getLongitude(),
                        destinationHub.getLatitude(), destinationHub.getLongitude()
                    );
                    // Score = how much detour this hub adds (lower = better)
                    return (dFromOrigin + dToDestination) - totalDist;
                }))
                .limit(maxIntermediates)
                .collect(Collectors.toList());
        }

        List<Hub> allWaypoints = new ArrayList<>();
        allWaypoints.add(originHub);
        allWaypoints.addAll(candidates);
        allWaypoints.add(destinationHub);

        return orderHubsByOSRMRoute(allWaypoints, originHub, destinationHub);
    }

    // Max intermediate hubs based on route length
    private int getMaxIntermediates(double totalDist) {
        if (totalDist < 300)  return 0; // short route — no intermediates
        if (totalDist < 600)  return 1;
        if (totalDist < 1000) return 2;
        if (totalDist < 1500) return 3; // Ludhiana→Mumbai ~1400km → max 3 intermediates
        return 4;                        // very long routes
    }

    // OSRM Route API — orders waypoints along actual road path
    // Unlike Trip API, Route API respects start→end direction
    private List<Hub> orderHubsByOSRMRoute(List<Hub> hubs, Hub origin, Hub destination) {
        if (hubs.size() <= 2) return hubs;

        StringBuilder coords = new StringBuilder();
        for (Hub h : hubs) {
            coords.append(h.getLongitude()).append(",").append(h.getLatitude()).append(";");
        }
        String coordStr = coords.toString().replaceAll(";$", "");

        // Route API with geometries to get actual road order
        String url = "http://router.project-osrm.org/route/v1/driving/"
                + coordStr
                + "?overview=false&steps=false";

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            String code = (String) response.get("code");

            if (!"Ok".equals(code)) {
                // OSRM returned an error — fall back to haversine sort
                return fallbackSort(hubs, origin);
            }

            List<Map<String, Object>> waypoints =
                (List<Map<String, Object>>) response.get("waypoints");

            // OSRM Route API waypoints are returned in INPUT ORDER
            // but each has a "waypoint_index" showing its position in the route
            // We sort hubs by their waypoint_index to get road order
            List<Map.Entry<Integer, Hub>> indexed = new ArrayList<>();
            for (int i = 0; i < waypoints.size(); i++) {
                // waypoint_index in Route API = position in the matched route
                int waypointIndex = (int) waypoints.get(i).get("waypoint_index");
                indexed.add(Map.entry(waypointIndex, hubs.get(i)));
            }

            indexed.sort(Comparator.comparingInt(Map.Entry::getKey));

            List<Hub> ordered = indexed.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

            // Guarantee origin is first and destination is last
            if (!ordered.get(0).getId().equals(origin.getId())) {
                ordered.remove(origin);
                ordered.add(0, origin);
            }
            if (!ordered.get(ordered.size() - 1).getId().equals(destination.getId())) {
                ordered.remove(destination);
                ordered.add(destination);
            }

            return ordered;

        } catch (Exception e) {
            return fallbackSort(hubs, origin);
        }
    }

    // Fallback: sort by cumulative haversine distance from origin
    private List<Hub> fallbackSort(List<Hub> hubs, Hub origin) {
        List<Hub> sorted = hubs.stream()
            .filter(h -> !h.getId().equals(origin.getId()))
            .sorted(Comparator.comparingDouble(h ->
                haversine(origin.getLatitude(), origin.getLongitude(),
                          h.getLatitude(), h.getLongitude())))
            .collect(Collectors.toList());

        sorted.add(0, origin);
        return sorted;
    }

    // Haversine formula — distance in km between two lat/lng points
    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
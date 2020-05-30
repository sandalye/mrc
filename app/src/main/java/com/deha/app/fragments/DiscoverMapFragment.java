package com.deha.app.fragments;

import android.app.ProgressDialog;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.deha.app.MainActivity;
import com.deha.app.R;
import com.deha.app.databinding.FragmentDiscoverMapBinding;
import com.deha.app.model.RequestModel;
import com.deha.app.model.UserModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager;
import com.mapbox.mapboxsdk.style.sources.VectorSource;
import com.tomergoldst.tooltips.ToolTip;
import com.tomergoldst.tooltips.ToolTipsManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import flipagram.assetcopylib.AssetCopier;

public class DiscoverMapFragment extends Fragment /*implements OnMapReadyCallback */{

  public static final String MAP_DATA_KEY = "HAS_SAVED_LOC";

  private FragmentDiscoverMapBinding binding;
  private ProgressDialog progressDialog;

  private FusedLocationProviderClient fusedLocationClient;
  private LocationCallback locationCallback;

  private MapboxMap map;
  private MapView mapView;
  private MarkerViewManager markerViewManager;
//  private MapBoxOfflineTileProvider provider;
  private RequestModel data;

  public static DiscoverMapFragment newInstance(String data) {
    DiscoverMapFragment fragment = new DiscoverMapFragment();
    Bundle args = new Bundle();
    if (data != null) {
      args.putString(MAP_DATA_KEY, data);
    }

    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    Mapbox.getInstance(getContext(), "sk.eyJ1IjoiaGFsbG9nYSIsImEiOiJja2FzYm51enEwZG9xMnptbzc5aW54bmYxIn0.g7HZ3Ghy841If8ohc31WbA");
    final String dataString = getArguments().getString(MAP_DATA_KEY);
    data = RequestModel.fromJson(dataString);
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_discover_map, container, false);
    binding.setLifecycleOwner(this);

    mapView = binding.mapView;
//    SupportMapFragment mapFrag = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.discover_map);
//    mapFrag.getMapAsync(this);
    setActions();
    progressDialog = ProgressDialog.show(getContext(), "Yükleniyor", "Lütfen bekleyiniz...");
    setupMapBox(savedInstanceState);
    return binding.getRoot();
  }

  private void setupMapBox(@Nullable Bundle savedInstanceState) {
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(mapboxMap -> {
      map = mapboxMap;
      mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/halloga/ckatmublk3ia81iqhjkdwgpph"), style -> setupMap(style));
    });
    setupOfflineMaps();
  }

  private void setupOfflineMaps() {
    OfflineManager offlineManager = OfflineManager.getInstance(getContext());
    LatLngBounds latLngBounds = new LatLngBounds.Builder()
        .include(new LatLng(41.269, 29.3399)) // Northeast
        .include(new LatLng(40.8397, 28.4926)) // Southwest
        .build();
  }

  private void showTooltip() {
    final ToolTipsManager toolTipsManager = new ToolTipsManager();
    ToolTip.Builder builder = new ToolTip.Builder(getContext(), binding.buttonHelp, binding.parent, "Yardıma mı ihtiyacınız var?", ToolTip.POSITION_ABOVE);
    builder.setGravity(ToolTip.GRAVITY_CENTER);
    builder.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
    toolTipsManager.show(builder.build());
  }

  private void setActions() {
    binding.buttonHelp.setOnClickListener(v -> {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage("Yardım isteği göndermek ister misiniz?")
          .setPositiveButton("Evet", (dialog, id) -> {
          })
          .setNegativeButton("Hayır", (dialog, id) -> {
          });
      // Create the AlertDialog object and return it
      builder.create().show();

    });
  }

  private void setupMap(Style style) {
    final Handler handler = new Handler();
    handler.post(() -> {
      showMyLocation(style);

      fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(Location location) {
          CameraPosition position = new CameraPosition.Builder()
              .target(new LatLng(location.getLatitude(), location.getLongitude()))
              .zoom(15)
              .build();
          map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 500);
        }
      });

      setMarkers();
      showTooltip();
      hideProgress();
    });
  }

  private void showMyLocation(Style style) {
    LocationComponentOptions locationComponentOptions = LocationComponentOptions.builder(getContext()).build();
    LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions
        .builder(getContext(), style)
        .locationComponentOptions(locationComponentOptions)
        .build();

    LocationComponent locationComponent = map.getLocationComponent();
    locationComponent.activateLocationComponent(locationComponentActivationOptions);
    locationComponent.setCameraMode(CameraMode.TRACKING);
    locationComponent.setLocationComponentEnabled(true);
  }

  private void hideProgress() {
    final Handler handler = new Handler(Looper.getMainLooper());
//    handler.post(()-> progressDialog.dismiss());
  }

  private void setMarkers() {
    markerViewManager = new MarkerViewManager(mapView, map);
    UserModel help = data.getPositiveList().get(0);
    addMarker(help,R.drawable.help);
    UserModel help2 = data.getPositiveList().get(1);
    addMarker(help2, R.drawable.help);
    UserModel med = data.getPositiveList().get(2);
    addMarker(med, R.drawable.medical);
    UserModel wifi = data.getPositiveList().get(3);
    addMarker(wifi, R.drawable.wifi);
  }

  private void addMarker(UserModel userModel, int iconRes) {
    IconFactory iconFactory = IconFactory.getInstance(getContext());
    Icon icon = iconFactory.fromResource(iconRes);
    map.addMarker(new MarkerOptions().position(new LatLng(userModel.getLatitude(), userModel.getLongitude())).icon(icon));
  }

  @Override
  public void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  public void onDestroyView() {
    markerViewManager.onDestroy();
    mapView.onDestroy();
    super.onDestroyView();
  }
}

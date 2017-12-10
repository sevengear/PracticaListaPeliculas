package org.upv.movie.list.netflix.activity;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.upv.movie.list.netflix.model.Lista;
import org.upv.movie.list.netflix.adapters.ListaAdapter;
import org.upv.movie.list.netflix.utils.ListasVector;
import org.upv.movie.list.netflix.R;
import org.upv.movie.list.netflix.adapters.RecyclerItemClickListener;
import org.upv.movie.list.netflix.model.User;
import org.upv.movie.list.netflix.utils.RateMyApp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import static org.upv.movie.list.netflix.activity.PerfilActivity.USERS;
import static org.upv.movie.list.netflix.activity.PerfilActivity.USERS_KEY_USERS;
import static org.upv.movie.list.netflix.activity.PerfilActivity.USER_LOGIN_PREFERENCES;
import static org.upv.movie.list.netflix.activity.PerfilActivity.USER_LOGIN_PREFERENCES_KEY_USER;

/**
 * Created by Lionel on 07/11/2017.
 */

public class ListasActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int ACTUALIZAR_PERFIL = 10000;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView username;
    private ImageView userfoto;

    // Publicidad

    private AdView adView;

    // InApp Billing
    private IInAppBillingService serviceBilling;
    private ServiceConnection serviceConnection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listas);

        // Puntuar mi aplicación
        new RateMyApp(this).app_launched();

        // Banner
        adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        serviceConectInAppBilling();

        // Inicializar los elementos (ejemplo)
        ListasVector listasVector = new ListasVector();
        listasVector.anyade(new Lista(R.drawable.ic_fav, getString(R.string.LA_favorites), getString(R.string.LA_favorites_desc)));
        listasVector.anyade(new Lista(R.drawable.ic_star, getString(R.string.LA_best), getString(R.string.LA_best_desc)));


        RecyclerView recycler = findViewById(R.id.recycler);

        RecyclerView.LayoutManager lManager = new LinearLayoutManager(this);

        recycler.setLayoutManager(lManager);

        RecyclerView.Adapter adapter = new ListaAdapter(listasVector);
        recycler.setAdapter(adapter);

        recycler.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Intent intent = new Intent(ListasActivity.this, MovieListActivity.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(ListasActivity.this).toBundle());
                } else {
                    startActivity(intent);
                }
            }
        }));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, getString(R.string.LA_fab_press), Snackbar.LENGTH_LONG).show();
            }
        });

        // Toolbar
        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Navigation Drawer
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        setNavigationPerfil();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_1) {
            // Editar perfil
            Intent intent = new Intent(this, PerfilActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, new Pair<>(findViewById(R.id.navUserFoto), getString(R.string.shared_photo_perfil)));
            ActivityCompat.startActivityForResult(this, intent, ACTUALIZAR_PERFIL, options.toBundle());
        } else if (id == R.id.nav_remove_advertising) {
            comprarProducto();
        }
// else if (id == R.id.nav_consulta_inapps_disponibles) {
//            getInAppInformationOfProducts();
//        } else if (id == R.id.nav_remove_advertising_manual) {
//            setAds(showInterticial );
//
//            showInterticial=!showInterticial;
//        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void checkPurchasedInAppProducts() {
        Bundle ownedItemsInApp = null;
        if (serviceBilling != null) {
            try {
                ownedItemsInApp = serviceBilling.getPurchases(3, getPackageName(), "inapp", null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            int response = ownedItemsInApp.getInt("RESPONSE_CODE");
            System.out.println(response);
            if (response == 0) {
                ArrayList<String> ownedSkus = ownedItemsInApp.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                ArrayList<String> purchaseDataList = ownedItemsInApp.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                ArrayList<String> signatureList = ownedItemsInApp.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
                String continuationToken = ownedItemsInApp.getString("INAPP_CONTINUATION_TOKEN");
                for (int i = 0; i < purchaseDataList.size(); ++i) {
                    String purchaseData = purchaseDataList.get(i);
                    String signature = signatureList.get(i);
                    String sku = ownedSkus.get(i);
                    System.out.println("Inapp Purchase data: " + purchaseData);
                    System.out.println("Inapp Signature: " + signature);
                    System.out.println("Inapp Sku: " + sku);
                    if (sku.equals(ID_ARTICULO)) {
//                        Toast.makeText(this, "Inapp comprado: " + sku + "el dia " + purchaseData, Toast.LENGTH_LONG).show();
                        // Quitar publicidad
//                        removeAdvertising();
                        setAds(false);
                    } else {
                        setAds(true);
                    }

                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void setNavigationPerfil() {
        // Navigation drawer user/foto
        User user = readUserFromPreferences();
        String email = user.getMail();
        int photo = user.getDEFAULT_PHOTO();
        username = navigationView.getHeaderView(0).findViewById(R.id.navUsername);
        username.setText(email);
        userfoto = navigationView.getHeaderView(0).findViewById(R.id.navUserFoto);
        userfoto.setImageResource(photo);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTUALIZAR_PERFIL && resultCode == RESULT_OK) {
            User user = readUserFromPreferences();
            username = navigationView.getHeaderView(0).findViewById(R.id.navUsername);
            username.setText(user.getMail());

            userfoto = navigationView.getHeaderView(0).findViewById(R.id.navUserFoto);
            userfoto.setImageResource(user.getDEFAULT_PHOTO());
        }

        switch (requestCode) {
            case INAPP_BILLING: {
                int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
                String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
                String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
                if (resultCode == RESULT_OK) {
                    try {
                        JSONObject jo = new JSONObject(purchaseData);
                        String sku = jo.getString("productId");
                        String developerPayload = jo.getString("developerPayload");
                        String purchaseToken = jo.getString("purchaseToken");
                        if (sku.equals(ID_ARTICULO)) {
                            Toast.makeText(this, R.string.compra_completada, Toast.LENGTH_LONG).show();
//                            removeAvertising();
                            setAds(false);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void getInAppInformationOfProducts() {
        ArrayList<String> skuList = new ArrayList<String>();
        skuList.add(ID_ARTICULO);
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
        Bundle skuDetails;
        ArrayList<String> responseList;
        try {
            skuDetails = serviceBilling.getSkuDetails(3, getPackageName(), "inapp", querySkus);
            int response = skuDetails.getInt("RESPONSE_CODE");
            if (response == 0) {
                responseList = skuDetails.getStringArrayList("DETAILS_LIST");
                assert responseList != null;
                for (String thisResponse : responseList) {
                    JSONObject object = new JSONObject(thisResponse);
                    String ref = object.getString("productId");
                    System.out.println("InApp Reference: " + ref);
                    String price = object.getString("price");
                    System.out.println("InApp Price: " + price);
                }
            }
        } catch (RemoteException | JSONException e) {
            e.printStackTrace();
        }
    }

    private User readUserFromPreferences() {
        User userAux;
        User user = null;
        SharedPreferences prefsLogin = getSharedPreferences(USER_LOGIN_PREFERENCES, Context.MODE_PRIVATE);
        String userLogged = prefsLogin.getString(USER_LOGIN_PREFERENCES_KEY_USER, "");
        SharedPreferences prefs = getSharedPreferences(USERS, Context.MODE_PRIVATE);
        Set userList = prefs.getStringSet(USERS_KEY_USERS, null);
        Gson gson = new Gson();
        Iterator<String> userListIterator = userList.iterator();
        while (userListIterator.hasNext()) {
            userAux = gson.fromJson(userListIterator.next(), User.class);
            if (userLogged.equals(userAux.getUsername())) {
                user = userAux;
                break;
            }
        }
        return user;
    }

    // In-App Billing
    public void serviceConectInAppBilling() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                serviceBilling = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                serviceBilling = IInAppBillingService.Stub.asInterface(service);
                checkPurchasedInAppProducts();
            }
        };
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void comprarProducto() {
        if (serviceBilling != null) {
            Bundle buyIntentBundle = null;
            try {
                buyIntentBundle = serviceBilling.getBuyIntent(3, getPackageName(), ID_ARTICULO, "inapp", developerPayLoad);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            try {
                if (pendingIntent != null) {
                    startIntentSenderForResult(pendingIntent.getIntentSender(), INAPP_BILLING, new Intent(), 0, 0, 0);
                }
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "InApp Billing service not available", Toast.LENGTH_LONG).show();
        }
    }

    private final String ID_ARTICULO = "org.upv.movie.list.netflix.removeadvertising";
    private final int INAPP_BILLING = 1;
    private final String developerPayLoad = "clave de seguridad";
    public static boolean showInterticial = true;

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }

    private void setAds(Boolean adsEnabled) {
        if (adsEnabled) {
            showInterticial = true;
            adView.setVisibility(View.VISIBLE);
            navigationView.getMenu().findItem(R.id.nav_remove_advertising).setVisible(true);
        } else

        {
            showInterticial = false;
            adView.setVisibility(View.GONE);
            navigationView.getMenu().findItem(R.id.nav_remove_advertising).setVisible(false);
        }
    }

}

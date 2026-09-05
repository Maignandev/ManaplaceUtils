package com.devkit.utils;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.MediaUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

@DesignerComponent(
        version = 16,
        description = "Extension ManaplaceUtils - corrections: clavier flottant, auto-grow, galerie/permissions, barre de navigation, réutilisation multiple sans conflit parent/dialogue + détails produits + badges génériques.",
        category = ComponentCategory.EXTENSION,
        nonVisible = true
)
@SimpleObject(external = true)
@UsesPermissions(
        permissionNames =
                "android.permission.READ_EXTERNAL_STORAGE," +
                "android.permission.READ_MEDIA_IMAGES," +
                "android.permission.INTERNET"
)
public class ManaplaceUtils extends AndroidNonvisibleComponent implements ActivityResultListener {

    private final Context context;
    private final Activity activity;
    private final Form monForm;
    private final int requestCode;

    private Dialog activeAlphaDialog;
    private View activeDialogContentView;

    private Typeface customTypeface = Typeface.DEFAULT;
    private int radioButtonColor = Color.parseColor("#C01A1A1B");

    // =========================================================================
    // WEBSOCKET (TEMPS RÉEL)
    // =========================================================================

    private Socket wsSocket;
    private OutputStream wsOutput;
    private volatile boolean wsRunning = false;

    // =========================================================================
    // 0. BARRE DE NAVIGATION FLOTTANTE
    // =========================================================================

    private boolean dejaInitialise = false;
    private int tailleIconeDp = 26;

    private final List<String> idsEnAttente = new ArrayList<>();
    private final List<String> iconesEnAttente = new ArrayList<>();
    private final List<String> iconTypesEnAttente = new ArrayList<>();
    private final List<View> vuesIcones = new ArrayList<>();
    private final List<View> vuesCercles = new ArrayList<>();
    private final List<String> idsFinaux = new ArrayList<>();

    private String idSelectionne = null;
    private View navBarView = null;

    private Typeface navBarIconFont = null;

    // =========================================================================
    // BADGES GÉNÉRIQUES
    // =========================================================================
    //
    // Le badge est placé directement dans la racine de l'activité.
    // Le composant d'origine n'est donc PAS modifié et garde exactement
    // sa taille, son parent et son comportement.
    //
    // Cela permet d'utiliser les badges sur :
    // CardView
    // Button
    // Image
    // Arrangement
    // Label
    // etc.
    //
    // =========================================================================

    private final Map<View, TextView> badges = new HashMap<>();
    private final Map<View, Integer> badgeCounts = new HashMap<>();
    private final Map<View, String> badgeTexts = new HashMap<>();
    private final Map<View, Integer> badgeColors = new HashMap<>();
    private final Map<View, Integer> badgeTextColors = new HashMap<>();
    private final Map<View, Boolean> badgeVisibility = new HashMap<>();

    private ViewTreeObserver.OnGlobalLayoutListener badgeLayoutListener;
    private boolean badgeListenerInstalled = false;

    private int defaultBadgeColor = Color.parseColor("#E53935");
    private int defaultBadgeTextColor = Color.WHITE;

    // =========================================================================
    // DÉTAILS PRODUIT
    // =========================================================================

    private String productDetailsEndpoint = "";

    private String currentProductUid = "";
    private String currentProductDescription = "";
    private int currentProductStock = 0;

    private String currentProductImage2 = "";
    private String currentProductImage3 = "";
    private String currentProductImage4 = "";
    private String currentProductImage5 = "";

    private boolean currentProductDeliveryIncluded = false;

    private boolean productDetailsLoading = false;

    // =========================================================================
    // NOTIFIER PERSONNALISÉ
    // =========================================================================

    private View activeNotifierView = null;

    // =========================================================================
    // MENU DÉROULANT
    // =========================================================================

    private final List<String> dropdownIdsEnAttente = new ArrayList<>();
    private final List<String> dropdownIconsEnAttente = new ArrayList<>();
    private final List<String> dropdownIconTypesEnAttente = new ArrayList<>();
    private final List<String> dropdownTextsEnAttente = new ArrayList<>();
    private android.widget.PopupWindow activeDropdownPopup = null;
    private Typeface dropdownIconFont = null;

    // =========================================================================
    // CARTES DE CONVERSATION
    // =========================================================================

    private final Map<String, View> conversationCards = new HashMap<>();
    private final Map<String, TextView> conversationSubtitles = new HashMap<>();
    private final Map<String, Boolean> conversationSelected = new HashMap<>();
    private ViewGroup conversationListContainer = null;
    private boolean conversationSelectionModeActive = false;

    private int conversationCardBackgroundColor = Color.parseColor("#F5F5F5");
    private int conversationCardSelectedColor = Color.parseColor("#E8F0DC");
    private int conversationNewMessageColor = Color.parseColor("#1A1A1B");
    private int conversationLastMessageColor = Color.parseColor("#8A8A8E");

    // =========================================================================
    // ÉCOUTE DU CHAMP DE SAISIE DE CHAT
    // =========================================================================

    public ManaplaceUtils(ComponentContainer container) {
        super(container.$form());

        this.context = container.$context();
        this.activity = (Activity) container.$context();
        this.monForm = container.$form();
        this.requestCode = this.form.registerForActivityResult(this);
    }

    private float dpToPx(int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    // =========================================================================
    // BARRE DE NAVIGATION
    // =========================================================================

    @SimpleFunction(
            description = "Ajoute une icône (fichier image) à la barre de navigation. À appeler une fois par icône, avant NavBarInitialize."
    )
    public void NavBarAdd(String id, String icon) {

        if (idsEnAttente.contains(id)) {
            NavBarError("Id déjà utilisé: " + id);
            return;
        }

        idsEnAttente.add(id);
        iconesEnAttente.add(icon);
        iconTypesEnAttente.add("image");
    }

    @SimpleFunction(
            description = "Ajoute une icône basée sur un caractère de police (ex: Phosphor, Material Symbols) à la barre de navigation. Utilise NavBarSetIconFont avant pour définir la police. À appeler avant NavBarInitialize."
    )
    public void NavBarAddTextIcon(String id, String unicodeChar) {

        if (idsEnAttente.contains(id)) {
            NavBarError("Id déjà utilisé: " + id);
            return;
        }

        idsEnAttente.add(id);
        iconesEnAttente.add(unicodeChar);
        iconTypesEnAttente.add("font");
    }

    @SimpleFunction(
            description = "Définit la police (.ttf/.otf) utilisée pour les icônes ajoutées via NavBarAddTextIcon. À appeler avant NavBarInitialize."
    )
    public void NavBarSetIconFont(String fontPath) {

        try {

            if (fontPath == null || fontPath.trim().isEmpty()) {
                navBarIconFont = null;
                return;
            }

            if (fontPath.startsWith("/")) {
                navBarIconFont = Typeface.createFromFile(new File(fontPath));
            } else {
                navBarIconFont = Typeface.createFromAsset(context.getAssets(), fontPath);
            }

        } catch (Exception e) {
            navBarIconFont = null;
            NavBarError("NavBarSetIconFont: " + e.getMessage());
        }
    }

    @SimpleFunction(
            description = "Construit et affiche la barre flottante avec toutes les icônes ajoutées via NavBarAdd."
    )
    public void NavBarInitialize(
            final int margeBas,
            final double largeurPourcent,
            final double hauteurPourcent) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {

                    FrameLayout root =
                            (FrameLayout) activity.findViewById(android.R.id.content);

                    if (root == null) {
                        NavBarError("Écran racine introuvable");
                        return;
                    }

                    if (navBarView != null &&
                            root.indexOfChild(navBarView) != -1) {

                        root.removeView(navBarView);
                    }

                    vuesIcones.clear();
                    vuesCercles.clear();
                    idsFinaux.clear();
                    idSelectionne = null;

                    if (idsEnAttente.isEmpty()) {

                        NavBarError(
                                "Aucune icône ajoutée — appelle NavBarAdd avant NavBarInitialize"
                        );

                        return;
                    }

                    DisplayMetrics metrics =
                            activity.getResources().getDisplayMetrics();

                    int largeurFinale =
                            (largeurPourcent > 0)
                                    ? (int) (
                                    metrics.widthPixels *
                                            (largeurPourcent / 100.0)
                            )
                                    : ViewGroup.LayoutParams.WRAP_CONTENT;

                    int hauteurFinale =
                            (hauteurPourcent > 0)
                                    ? (int) (
                                    metrics.heightPixels *
                                            (hauteurPourcent / 100.0)
                            )
                                    : (int) dpToPx(64);

                    LinearLayout bar = new LinearLayout(activity);

                    bar.setOrientation(LinearLayout.HORIZONTAL);
                    bar.setGravity(Gravity.CENTER);
                    bar.setWeightSum(idsEnAttente.size());
                    bar.setElevation(dpToPx(8));

                    GradientDrawable fond = new GradientDrawable();

                    fond.setColor(Color.WHITE);
                    fond.setCornerRadius(dpToPx(30));

                    bar.setBackground(fond);

                    for (int i = 0;
                         i < idsEnAttente.size();
                         i++) {

                        final String tabId =
                                idsEnAttente.get(i);

                        String iconValue =
                                iconesEnAttente.get(i);

                        String iconType =
                                (i < iconTypesEnAttente.size())
                                        ? iconTypesEnAttente.get(i)
                                        : "image";

                        FrameLayout conteneur =
                                new FrameLayout(activity);

                        conteneur.setLayoutParams(
                                new LinearLayout.LayoutParams(
                                        0,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        1f
                                )
                        );

                        View cercle =
                                new View(activity);

                        GradientDrawable fondCercle =
                                new GradientDrawable();

                        fondCercle.setShape(
                                GradientDrawable.OVAL
                        );

                        fondCercle.setColor(
                                Color.argb(
                                        30,
                                        0,
                                        0,
                                        0
                                )
                        );

                        cercle.setBackground(fondCercle);
                        cercle.setAlpha(0f);

                        conteneur.addView(
                                cercle,
                                new FrameLayout.LayoutParams(
                                        (int) dpToPx(46),
                                        (int) dpToPx(46),
                                        Gravity.CENTER
                                )
                        );

                        int taillePx =
                                (int) dpToPx(
                                        tailleIconeDp
                                );

                        final View iconeFinale;

                        if ("font".equals(iconType)) {

                            TextView iconTv = new TextView(activity);

                            iconTv.setText(iconValue);
                            iconTv.setGravity(Gravity.CENTER);
                            iconTv.setIncludeFontPadding(false);

                            if (navBarIconFont != null) {
                                iconTv.setTypeface(navBarIconFont);
                            }

                            iconTv.setTextColor(
                                    Color.rgb(150, 150, 150)
                            );

                            iconTv.setTextSize(
                                    TypedValue.COMPLEX_UNIT_PX,
                                    (float) taillePx
                            );

                            conteneur.addView(
                                    iconTv,
                                    new FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                            Gravity.CENTER
                                    )
                            );

                            iconeFinale = iconTv;

                        } else {

                            ImageView img = new ImageView(activity);

                            img.setAdjustViewBounds(true);

                            try {

                                Drawable d =
                                        MediaUtil.getBitmapDrawable(
                                                monForm,
                                                iconValue
                                        );

                                img.setImageDrawable(d);

                                img.setColorFilter(
                                        new PorterDuffColorFilter(
                                                Color.rgb(
                                                        150,
                                                        150,
                                                        150
                                                ),
                                                PorterDuff.Mode.SRC_IN
                                        )
                                );

                            } catch (IOException e) {

                                NavBarError(
                                        "Icône introuvable: " +
                                                iconValue
                                );
                            }

                            conteneur.addView(
                                    img,
                                    new FrameLayout.LayoutParams(
                                            taillePx,
                                            taillePx,
                                            Gravity.CENTER
                                    )
                            );

                            iconeFinale = img;
                        }

                        final View cercleFinal = cercle;

                        conteneur.setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        SelectionnerOnglet(
                                                tabId,
                                                cercleFinal,
                                                iconeFinale
                                        );
                                    }
                                }
                        );

                        vuesIcones.add(iconeFinale);
                        vuesCercles.add(cercle);
                        idsFinaux.add(tabId);

                        bar.addView(conteneur);
                    }

                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(
                                    largeurFinale,
                                    hauteurFinale
                            );

                    params.gravity =
                            Gravity.BOTTOM |
                                    Gravity.CENTER_HORIZONTAL;

                    params.setMargins(
                            0,
                            0,
                            0,
                            (int) dpToPx(margeBas)
                    );

                    root.addView(bar, params);

                    navBarView = bar;

                    dejaInitialise = true;

                    // Les badges existants sont repositionnés.
                    updateAllBadges();

                } catch (Exception e) {

                    NavBarError(
                            "NavBarInitialize: " +
                                    e.getMessage()
                    );
                }
            }
        });
    }

    @SimpleFunction(
            description = "Ajuste la taille de toutes les icônes de la barre en dp, avec une transition animée."
    )
    public void NavBarSetIconSize(final int tailleDp) {

        final int ancienneTailleDp =
                tailleIconeDp;

        tailleIconeDp = tailleDp;

        if (vuesIcones.isEmpty())
            return;

        try {

            final float ancienPx =
                    dpToPx(
                            ancienneTailleDp
                    );

            final float nouveauPx =
                    dpToPx(tailleDp);

            ValueAnimator anim =
                    ValueAnimator.ofFloat(
                            ancienPx,
                            nouveauPx
                    );

            anim.setDuration(220);

            anim.setInterpolator(
                    new DecelerateInterpolator()
            );

            anim.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {

                        @Override
                        public void onAnimationUpdate(
                                ValueAnimator animation) {

                            int taillePx =
                                    (int)
                                            (float)
                                                    animation
                                                            .getAnimatedValue();

                            for (View iv :
                                    vuesIcones) {

                                if (iv instanceof TextView) {

                                    ((TextView) iv).setTextSize(
                                            TypedValue.COMPLEX_UNIT_PX,
                                            (float) taillePx
                                    );

                                } else {

                                    ViewGroup.LayoutParams p =
                                            iv.getLayoutParams();

                                    if (p != null) {

                                        p.width = taillePx;
                                        p.height = taillePx;

                                        iv.setLayoutParams(p);
                                    }
                                }
                            }

                            updateAllBadges();
                        }
                    }
            );

            anim.start();

        } catch (Exception e) {

            NavBarError(
                    "Erreur NavBarSetIconSize: " +
                            e.getMessage()
            );
        }
    }

    @SimpleFunction(
            description = "Sélectionne un onglet de la barre par code, sans clic."
    )
    public void NavBarSelect(String id) {

        int index =
                idsFinaux.indexOf(id);

        if (index < 0) {

            NavBarError(
                    "Id introuvable pour NavBarSelect: " +
                            id
            );

            return;
        }

        SelectionnerOnglet(
                id,
                vuesCercles.get(index),
                vuesIcones.get(index)
        );
    }

    @SimpleFunction(
            description = "Affiche ou masque la barre de navigation."
    )
    public void NavBarSetVisible(
            final boolean visible) {

        if (navBarView == null) {

            NavBarError(
                    "NavBarSetVisible: la barre n'est pas encore initialisée."
            );

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        navBarView.setVisibility(
                                visible
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        updateAllBadges();
                    }
                }
        );
    }

    // =========================================================================
    // BADGE SUR LA NAVBAR
    // =========================================================================

    @SimpleFunction(
            description = "Ajoute ou modifie un badge numérique sur une icône de la barre de navigation."
    )
    public void NavBarSetBadge(
            final String id,
            final int count) {

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        int index =
                                idsFinaux.indexOf(id);

                        if (index < 0) {

                            NavBarError(
                                    "Badge: id NavBar introuvable: " +
                                            id
                            );

                            return;
                        }

                        View target =
                                vuesIcones.get(index);

                        setBadgeInternal(
                                target,
                                count,
                                null
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Affiche un texte personnalisé comme badge sur une icône de la barre."
    )
    public void NavBarSetBadgeText(
            final String id,
            final String text) {

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        int index =
                                idsFinaux.indexOf(id);

                        if (index < 0) {

                            NavBarError(
                                    "Badge: id NavBar introuvable: " +
                                            id
                            );

                            return;
                        }

                        setBadgeInternal(
                                vuesIcones.get(index),
                                0,
                                text
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Supprime le badge d'une icône de la barre de navigation."
    )
    public void NavBarClearBadge(final String id) {

        final int index = idsFinaux.indexOf(id);

        if (index < 0) {
            NavBarError("Badge: id NavBar introuvable: " + id);
            return;
        }

        final View target = vuesIcones.get(index);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                removeBadgeInternal(target);
            }
        });
    }

    private void SelectionnerOnglet(
            String id,
            View cercle,
            View img) {

        try {

            if (id.equals(idSelectionne))
                return;

            if (idSelectionne != null) {

                int ancienIndex =
                        idsFinaux.indexOf(
                                idSelectionne
                        );

                if (ancienIndex >= 0) {

                    animerOnglet(
                            vuesCercles.get(ancienIndex),
                            vuesIcones.get(ancienIndex),
                            false
                    );
                }
            }

            animerOnglet(
                    cercle,
                    img,
                    true
            );

            idSelectionne = id;

            OnSelected(id);

        } catch (Exception e) {

            NavBarError(
                    "Erreur de sélection: " +
                            e.getMessage()
            );
        }
    }

    private void animerOnglet(
            final View cercle,
            final View img,
            boolean selectionne) {

        float alphaCible =
                selectionne
                        ? 1f
                        : 0f;

        ValueAnimator anim =
                ValueAnimator.ofFloat(
                        cercle.getAlpha(),
                        alphaCible
                );

        anim.setDuration(220);

        anim.setInterpolator(
                new DecelerateInterpolator()
        );

        anim.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(
                            ValueAnimator animation) {

                        float val =
                                (float)
                                        animation
                                                .getAnimatedValue();

                        cercle.setAlpha(val);

                        int couleur =
                                melangerCouleurs(
                                        Color.rgb(
                                                150,
                                                150,
                                                150
                                        ),
                                        Color.rgb(
                                                20,
                                                20,
                                                20
                                        ),
                                        val
                                );

                        if (img instanceof ImageView) {

                            ((ImageView) img).setColorFilter(
                                    new PorterDuffColorFilter(
                                            couleur,
                                            PorterDuff.Mode.SRC_IN
                                    )
                            );

                        } else if (img instanceof TextView) {

                            ((TextView) img).setTextColor(couleur);
                        }
                    }
                }
        );

        anim.start();
    }

    private int melangerCouleurs(
            int c1,
            int c2,
            float ratio) {

        int r =
                (int)
                        (
                                Color.red(c1) +
                                        ratio *
                                                (
                                                        Color.red(c2) -
                                                                Color.red(c1)
                                                )
                        );

        int g =
                (int)
                        (
                                Color.green(c1) +
                                        ratio *
                                                (
                                                        Color.green(c2) -
                                                                Color.green(c1)
                                                )
                        );

        int b =
                (int)
                        (
                                Color.blue(c1) +
                                        ratio *
                                                (
                                                        Color.blue(c2) -
                                                                Color.blue(c1)
                                                )
                        );

        return Color.rgb(r, g, b);
    }

    // =========================================================================
    // SYSTÈME DE BADGES GÉNÉRIQUE
    // =========================================================================

    @SimpleFunction(
            description = "Ajoute un badge numérique à n'importe quel composant Android."
    )
    public void AddBadge(
            final AndroidViewComponent component,
            final int count) {

        if (component == null ||
                component.getView() == null) {

            OnError(
                    "AddBadge: composant invalide."
            );

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        setBadgeInternal(
                                component.getView(),
                                count,
                                null
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Définit le nombre affiché par un badge existant."
    )
    public void SetBadgeCount(
            final AndroidViewComponent component,
            final int count) {

        if (component == null ||
                component.getView() == null) {

            OnError(
                    "SetBadgeCount: composant invalide."
            );

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        setBadgeInternal(
                                component.getView(),
                                count,
                                null
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Ajoute ou modifie un badge avec un texte personnalisé."
    )
    public void SetBadgeText(
            final AndroidViewComponent component,
            final String text) {

        if (component == null ||
                component.getView() == null) {

            OnError(
                    "SetBadgeText: composant invalide."
            );

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        setBadgeInternal(
                                component.getView(),
                                0,
                                text
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Change la couleur du badge."
    )
    public void SetBadgeColor(
            final AndroidViewComponent component,
            final int color) {

        if (component == null ||
                component.getView() == null) {

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        View target =
                                component.getView();

                        badgeColors.put(
                                target,
                                color
                        );

                        TextView badge =
                                badges.get(target);

                        if (badge != null) {

                            applyBadgeBackground(
                                    badge,
                                    color
                            );
                        }
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Change la couleur du texte du badge."
    )
    public void SetBadgeTextColor(
            final AndroidViewComponent component,
            final int color) {

        if (component == null ||
                component.getView() == null) {

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        View target =
                                component.getView();

                        badgeTextColors.put(
                                target,
                                color
                        );

                        TextView badge =
                                badges.get(target);

                        if (badge != null) {
                            badge.setTextColor(color);
                        }
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Affiche ou masque le badge d'un composant."
    )
    public void SetBadgeVisible(
            final AndroidViewComponent component,
            final boolean visible) {

        if (component == null ||
                component.getView() == null) {

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        View target =
                                component.getView();

                        badgeVisibility.put(
                                target,
                                visible
                        );

                        TextView badge =
                                badges.get(target);

                        if (badge != null) {

                            badge.setVisibility(
                                    visible
                                            ? View.VISIBLE
                                            : View.GONE
                            );

                            updateBadgePosition(
                                    target,
                                    badge
                            );
                        }
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Supprime complètement le badge d'un composant."
    )
    public void RemoveBadge(
            final AndroidViewComponent component) {

        if (component == null ||
                component.getView() == null) {

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        removeBadgeInternal(
                                component.getView()
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Définit la couleur par défaut utilisée pour les nouveaux badges."
    )
    public void SetDefaultBadgeColor(int color) {

        defaultBadgeColor = color;
    }

    @SimpleFunction(
            description = "Définit la couleur de texte par défaut utilisée pour les nouveaux badges."
    )
    public void SetDefaultBadgeTextColor(int color) {

        defaultBadgeTextColor = color;
    }

    private void setBadgeInternal(
            final View target,
            final int count,
            final String customText) {

        if (target == null)
            return;

        FrameLayout root =
                (FrameLayout)
                        activity.findViewById(
                                android.R.id.content
                        );

        if (root == null) {

            OnError(
                    "Badge: racine de l'activité introuvable."
            );

            return;
        }

        TextView badge =
                badges.get(target);

        if (badge == null) {

            badge = new TextView(activity);

            badge.setGravity(
                    Gravity.CENTER
            );

            badge.setIncludeFontPadding(false);

            badge.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );

            badge.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    10
            );

            badge.setTextColor(
                    defaultBadgeTextColor
            );

            badge.setMinWidth(
                    (int) dpToPx(18)
            );

            badge.setMinHeight(
                    (int) dpToPx(18)
            );

            badge.setPadding(
                    (int) dpToPx(4),
                    0,
                    (int) dpToPx(4),
                    0
            );

            applyBadgeBackground(
                    badge,
                    defaultBadgeColor
            );

            FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            (int) dpToPx(20)
                    );

            params.gravity =
                    Gravity.TOP |
                            Gravity.LEFT;

            root.addView(
                    badge,
                    params
            );

            badges.put(
                    target,
                    badge
            );

            badgeColors.put(
                    target,
                    defaultBadgeColor
            );

            badgeTextColors.put(
                    target,
                    defaultBadgeTextColor
            );

            badgeVisibility.put(
                    target,
                    true
            );

            installBadgeLayoutListener();
        }

        if (customText != null) {

            String text =
                    customText.trim();

            badgeTexts.put(
                    target,
                    text
            );

            badge.setText(text);

        } else {

            badgeCounts.put(
                    target,
                    count
            );

            String displayText;

            if (count <= 0) {

                displayText = "";

            } else if (count > 99) {

                displayText = "99+";

            } else {

                displayText =
                        String.valueOf(count);
            }

            badgeTexts.put(
                    target,
                    displayText
            );

            badge.setText(displayText);
        }

        int textColor =
                badgeTextColors.containsKey(target)
                        ? badgeTextColors.get(target)
                        : defaultBadgeTextColor;

        badge.setTextColor(textColor);

        int badgeColor =
                badgeColors.containsKey(target)
                        ? badgeColors.get(target)
                        : defaultBadgeColor;

        applyBadgeBackground(
                badge,
                badgeColor
        );

        boolean visible =
                badgeVisibility.containsKey(target)
                        ? badgeVisibility.get(target)
                        : true;

        if (customText != null) {
            visible =
                    !customText.trim().isEmpty();
        } else {
            visible =
                    count > 0;
        }

        badgeVisibility.put(
                target,
                visible
        );

        badge.setVisibility(
                visible
                        ? View.VISIBLE
                        : View.GONE
        );

        updateBadgeSize(
                target,
                badge
        );

        updateBadgePosition(
                target,
                badge
        );
    }

    private void applyBadgeBackground(
            TextView badge,
            int color) {

        GradientDrawable background =
                new GradientDrawable();

        background.setShape(
                GradientDrawable.OVAL
        );

        background.setColor(color);

        badge.setBackground(
                background
        );
    }

    private void updateBadgeSize(
            View target,
            TextView badge) {

        String text =
                badge.getText() == null
                        ? ""
                        : badge.getText().toString();

        int minWidth;

        if (text.length() >= 3) {

            minWidth =
                    (int) dpToPx(28);

        } else if (text.length() == 2) {

            minWidth =
                    (int) dpToPx(23);

        } else {

            minWidth =
                    (int) dpToPx(20);
        }

        int height =
                (int) dpToPx(20);

        ViewGroup.LayoutParams oldParams =
                badge.getLayoutParams();

        if (oldParams instanceof FrameLayout.LayoutParams) {

            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams)
                            oldParams;

            params.width =
                    minWidth;

            params.height =
                    height;

            badge.setLayoutParams(params);
        }
    }

    private void updateBadgePosition(
            View target,
            View badge) {

        if (target == null ||
                badge == null) {

            return;
        }

        if (target.getWindowToken() == null ||
                !target.isAttachedToWindow()) {

            badge.setVisibility(
                    View.GONE
            );

            return;
        }

        FrameLayout root =
                (FrameLayout)
                        activity.findViewById(
                                android.R.id.content
                        );

        if (root == null)
            return;

        int[] targetLocation =
                new int[2];

        int[] rootLocation =
                new int[2];

        target.getLocationOnScreen(
                targetLocation
        );

        root.getLocationOnScreen(
                rootLocation
        );

        int targetLeft =
                targetLocation[0] -
                        rootLocation[0];

        int targetTop =
                targetLocation[1] -
                        rootLocation[1];

        int targetWidth =
                target.getWidth();

        int badgeWidth =
                badge.getWidth();

        if (badgeWidth <= 0) {

            badgeWidth =
                    (int) dpToPx(20);
        }

        int badgeHeight =
                badge.getHeight();

        if (badgeHeight <= 0) {

            badgeHeight =
                    (int) dpToPx(20);
        }

        // Position en haut à droite.
        // Une partie du badge dépasse volontairement du composant.
        int left =
                targetLeft +
                        targetWidth -
                        badgeWidth / 2;

        int top =
                targetTop -
                        badgeHeight / 2;

        FrameLayout.LayoutParams params;

        if (badge.getLayoutParams()
                instanceof FrameLayout.LayoutParams) {

            params =
                    (FrameLayout.LayoutParams)
                            badge.getLayoutParams();

        } else {

            params =
                    new FrameLayout.LayoutParams(
                            badgeWidth,
                            badgeHeight
                    );
        }

        params.leftMargin =
                left;

        params.topMargin =
                top;

        params.gravity =
                Gravity.TOP |
                        Gravity.LEFT;

        badge.setLayoutParams(params);
    }

    private void updateAllBadges() {

        if (badges.isEmpty())
            return;

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        for (
                                Map.Entry<View, TextView> entry :
                                badges.entrySet()
                        ) {

                            View target =
                                    entry.getKey();

                            TextView badge =
                                    entry.getValue();

                            if (target != null &&
                                    badge != null) {

                                updateBadgePosition(
                                        target,
                                        badge
                                );
                            }
                        }
                    }
                }
        );
    }

    private void removeBadgeInternal(
            View target) {

        if (target == null)
            return;

        TextView badge =
                badges.remove(target);

        badgeCounts.remove(target);
        badgeTexts.remove(target);
        badgeColors.remove(target);
        badgeTextColors.remove(target);
        badgeVisibility.remove(target);

        if (badge != null) {

            ViewGroup parent =
                    (ViewGroup)
                            badge.getParent();

            if (parent != null) {
                parent.removeView(badge);
            }
        }

        if (badges.isEmpty()) {
            uninstallBadgeLayoutListener();
        }
    }

    private void installBadgeLayoutListener() {

        if (badgeListenerInstalled)
            return;

        final View rootView =
                activity.getWindow()
                        .getDecorView()
                        .getRootView();

        badgeLayoutListener =
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {

                        updateAllBadges();
                    }
                };

        rootView.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        badgeLayoutListener
                );

        badgeListenerInstalled = true;
    }

    private void uninstallBadgeLayoutListener() {

        if (!badgeListenerInstalled)
            return;

        try {

            View rootView =
                    activity.getWindow()
                            .getDecorView()
                            .getRootView();

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.JELLY_BEAN) {

                rootView.getViewTreeObserver()
                        .removeOnGlobalLayoutListener(
                                badgeLayoutListener
                        );

            } else {

                rootView.getViewTreeObserver()
                        .removeGlobalOnLayoutListener(
                                badgeLayoutListener
                        );
            }

        } catch (Exception ignored) {
        }

        badgeListenerInstalled = false;
        badgeLayoutListener = null;
    }

    // =========================================================================
    // POLICE PERSONNALISÉE
    // =========================================================================

    @SimpleFunction(
            description = "Charge une police personnalisée .ttf ou .otf."
    )
    public void LoadCustomFont(String fontPath) {

        try {

            if (fontPath == null ||
                    fontPath.trim().isEmpty()) {

                customTypeface =
                        Typeface.DEFAULT;

                return;
            }

            if (fontPath.startsWith("/")) {

                customTypeface =
                        Typeface.createFromFile(
                                new File(fontPath)
                        );

            } else {

                customTypeface =
                        Typeface.createFromAsset(
                                context.getAssets(),
                                fontPath
                        );
            }

        } catch (Exception e) {

            e.printStackTrace();

            customTypeface =
                    Typeface.DEFAULT;
        }
    }

    @SimpleFunction(
            description = "Définit la couleur des boutons radio."
    )
    public void SetRadioButtonColor(int color) {

        radioButtonColor = color;
    }

    // =========================================================================
    // UTILITAIRES IMAGES
    // =========================================================================

    private ViewGroup getRealLayout(
            AndroidViewComponent component) {

        if (component == null)
            return null;

        View view =
                component.getView();

        if (view instanceof ViewGroup) {

            ViewGroup vg =
                    (ViewGroup) view;

            if (vg.getChildCount() > 0 &&
                    vg.getChildAt(0)
                            instanceof ViewGroup) {

                return (ViewGroup)
                        vg.getChildAt(0);
            }

            return vg;
        }

        return null;
    }

    private void runOnUi(
            Runnable runnable) {

        activity.runOnUiThread(
                runnable
        );
    }

    private void loadImageAsync(
            final ImageView imageView,
            final String imagePath) {

        if (imagePath == null ||
                imagePath.trim().isEmpty())
            return;

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        Bitmap bmp = null;
                        InputStream input = null;
                        HttpURLConnection conn = null;

                        try {

                            if (imagePath.startsWith(
                                    "http://"
                            ) ||
                                    imagePath.startsWith(
                                            "https://"
                                    )) {

                                URL url =
                                        new URL(imagePath);

                                conn =
                                        (HttpURLConnection)
                                                url.openConnection();

                                conn.setConnectTimeout(
                                        15000
                                );

                                conn.setReadTimeout(
                                        15000
                                );

                                conn.setDoInput(true);

                                conn.connect();

                                input =
                                        conn.getInputStream();

                                bmp =
                                        BitmapFactory
                                                .decodeStream(
                                                        input
                                                );

                            } else if (
                                    imagePath.startsWith(
                                            "content://"
                                    )
                            ) {

                                input =
                                        context
                                                .getContentResolver()
                                                .openInputStream(
                                                        Uri.parse(
                                                                imagePath
                                                        )
                                                );

                                if (input != null) {

                                    bmp =
                                            BitmapFactory
                                                    .decodeStream(
                                                            input
                                                    );
                                }

                            } else {

                                try {

                                    input =
                                            context
                                                    .getAssets()
                                                    .open(
                                                            imagePath
                                                    );

                                    bmp =
                                            BitmapFactory
                                                    .decodeStream(
                                                            input
                                                    );

                                } catch (Exception assetError) {

                                    try {

                                        bmp =
                                                MediaUtil
                                                        .getBitmapDrawable(
                                                                monForm,
                                                                imagePath
                                                        )
                                                        .getBitmap();

                                    } catch (Exception mediaError) {

                                        File file =
                                                new File(
                                                        imagePath
                                                );

                                        if (file.exists()) {

                                            input =
                                                    new FileInputStream(
                                                            file
                                                    );

                                            bmp =
                                                    BitmapFactory
                                                            .decodeStream(
                                                                    input
                                                            );
                                        }
                                    }
                                }
                            }

                        } catch (Exception e) {

                            e.printStackTrace();

                        } finally {

                            if (input != null) {

                                try {
                                    input.close();
                                } catch (Exception ignored) {}
                            }

                            if (conn != null) {
                                conn.disconnect();
                            }
                        }

                        final Bitmap finalBmp =
                                bmp;

                        if (finalBmp != null) {

                            activity.runOnUiThread(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            if (imageView
                                                    .getWindowToken()
                                                    != null ||
                                                    imageView
                                                            .isAttachedToWindow()
                                            ) {

                                                imageView
                                                        .setImageBitmap(
                                                                finalBmp
                                                        );
                                            }
                                        }
                                    }
                            );
                        }
                    }
                }
        );
    }

    // =========================================================================
    // 1. MOTEUR DE CHAT DYNAMIQUE NATIF
    // =========================================================================

    @SimpleFunction(
            description = "Ajoute une bulle de chat avec un petit avatar rond."
    )
    public void AddChatBubble(
            final AndroidViewComponent chatContainer,
            final String messageText,
            final String timeText,
            final String avatarUrl,
            final String senderUid,
            final boolean isMe,
            final int bubbleColor,
            final int textColor) {

        runOnUi(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            ViewGroup targetLayout =
                                    getRealLayout(
                                            chatContainer
                                    );

                            if (targetLayout == null)
                                return;

                            int screenWidth =
                                    activity
                                            .getResources()
                                            .getDisplayMetrics()
                                            .widthPixels;

                            LinearLayout row =
                                    new LinearLayout(
                                            context
                                    );

                            row.setOrientation(
                                    LinearLayout.HORIZONTAL
                            );

                            row.setGravity(
                                    isMe
                                            ? Gravity.END
                                            : Gravity.START
                            );

                            LinearLayout.LayoutParams rowParams =
                                    new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    );

                            rowParams.setMargins(
                                    (int) dpToPx(8),
                                    (int) dpToPx(4),
                                    (int) dpToPx(8),
                                    (int) dpToPx(4)
                            );

                            row.setLayoutParams(
                                    rowParams
                            );

                            int avatarSizePx =
                                    (int) dpToPx(32);

                            CardView avatarCard =
                                    new CardView(context);

                            LinearLayout.LayoutParams avatarParams =
                                    new LinearLayout.LayoutParams(
                                            avatarSizePx,
                                            avatarSizePx
                                    );

                            avatarParams.gravity =
                                    Gravity.CENTER_VERTICAL;

                            avatarParams.setMargins(
                                    (int) dpToPx(6),
                                    0,
                                    (int) dpToPx(6),
                                    0
                            );

                            avatarCard.setLayoutParams(
                                    avatarParams
                            );

                            avatarCard.setRadius(
                                    avatarSizePx / 2f
                            );

                            avatarCard.setCardElevation(0f);
                            avatarCard.setMaxCardElevation(0f);

                            avatarCard.setCardBackgroundColor(
                                    Color.parseColor(
                                            "#E0E0E0"
                                    )
                            );

                            ImageView avatarImg =
                                    new ImageView(context);

                            avatarImg.setLayoutParams(
                                    new ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                            );

                            avatarImg.setScaleType(
                                    ImageView.ScaleType.CENTER_INSIDE
                            );

                            avatarImg.setPadding(
                                    (int) dpToPx(4),
                                    (int) dpToPx(4),
                                    (int) dpToPx(4),
                                    (int) dpToPx(4)
                            );

                            if (avatarUrl != null &&
                                    !avatarUrl.isEmpty()) {

                                loadImageAsync(
                                        avatarImg,
                                        avatarUrl
                                );
                            }

                            avatarCard.addView(
                                    avatarImg
                            );

                            avatarCard.setOnClickListener(
                                    new View.OnClickListener() {

                                        @Override
                                        public void onClick(View v) {

                                            OnAvatarClick(
                                                    senderUid,
                                                    isMe
                                            );
                                        }
                                    }
                            );

                            LinearLayout bubble =
                                    new LinearLayout(context);

                            bubble.setOrientation(
                                    LinearLayout.VERTICAL
                            );

                            bubble.setPadding(
                                    (int) dpToPx(16),
                                    (int) dpToPx(10),
                                    (int) dpToPx(16),
                                    (int) dpToPx(10)
                            );

                            GradientDrawable bg =
                                    new GradientDrawable();

                            bg.setShape(
                                    GradientDrawable.RECTANGLE
                            );

                            bg.setColor(
                                    bubbleColor
                            );

                            bg.setCornerRadius(
                                    dpToPx(22)
                            );

                            bubble.setBackground(bg);

                            int maxBubbleWidth =
                                    (int)
                                            (screenWidth * 0.72);

                            LinearLayout.LayoutParams bubbleParams =
                                    new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    );

                            bubble.setLayoutParams(
                                    bubbleParams
                            );

                            TextView msgTv =
                                    new TextView(context);

                            msgTv.setText(
                                    messageText
                            );

                            msgTv.setTextColor(
                                    textColor
                            );

                            msgTv.setTextSize(15);

                            msgTv.setMaxWidth(
                                    maxBubbleWidth
                            );

                            msgTv.setLayoutParams(
                                    new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                            );

                            if (customTypeface != null) {

                                msgTv.setTypeface(
                                        customTypeface
                                );
                            }

                            bubble.addView(msgTv);

                            if (timeText != null &&
                                    !timeText.isEmpty()) {

                                TextView timeTv =
                                        new TextView(context);

                                timeTv.setText(
                                        timeText
                                );

                                timeTv.setTextColor(
                                        Color.argb(
                                                180,
                                                Color.red(textColor),
                                                Color.green(textColor),
                                                Color.blue(textColor)
                                        )
                                );

                                timeTv.setTextSize(10);

                                LinearLayout.LayoutParams timeParams =
                                        new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                        );

                                timeParams.gravity =
                                        Gravity.END;

                                timeParams.setMargins(
                                        0,
                                        (int) dpToPx(2),
                                        0,
                                        0
                                );

                                timeTv.setLayoutParams(
                                        timeParams
                                );

                                if (customTypeface != null) {

                                    timeTv.setTypeface(
                                            customTypeface
                                    );
                                }

                                bubble.addView(
                                        timeTv
                                );
                            }

                            if (isMe) {

                                row.addView(bubble);
                                row.addView(avatarCard);

                            } else {

                                row.addView(avatarCard);
                                row.addView(bubble);
                            }

                            targetLayout.addView(row);

                            ScrollToBottom(
                                    chatContainer
                            );

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Fait défiler le ScrollArrangement jusqu'au tout dernier message."
    )
    public void ScrollToBottom(
            final AndroidViewComponent scrollContainer) {

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        View view =
                                scrollContainer.getView();

                        if (view instanceof ScrollView) {

                            final ScrollView scrollView =
                                    (ScrollView) view;

                            scrollView.post(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            scrollView.fullScroll(
                                                    View.FOCUS_DOWN
                                            );
                                        }
                                    }
                            );
                        }
                    }
                }
        );
    }

    // =========================================================================
    // 2. SAISIE FLOTTANTE & CLAVIER
    // =========================================================================

    @SimpleFunction(
            description = "Attache la zone de saisie au-dessus du clavier de manière flottante."
    )
    public void AttachFloatingInputWithDynamicHeight(
            final Object inputContainer,
            final Object editTextComponent,
            final int maxHeightPx) {

        if (!(inputContainer instanceof AndroidViewComponent))
            return;

        final View containerView =
                ((AndroidViewComponent)
                        inputContainer)
                        .getView();

        if (containerView == null)
            return;

        final View rootView =
                activity.getWindow()
                        .getDecorView()
                        .getRootView();

        rootView.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {

                            @Override
                            public void onGlobalLayout() {

                                Rect r = new Rect();

                                rootView
                                        .getWindowVisibleDisplayFrame(
                                                r
                                        );

                                int screenHeight =
                                        rootView
                                                .getRootView()
                                                .getHeight();

                                int keypadHeight =
                                        screenHeight -
                                                r.bottom;

                                if (keypadHeight >
                                        screenHeight * 0.15) {

                                    containerView
                                            .setTranslationY(
                                                    -keypadHeight
                                            );

                                } else {

                                    containerView
                                            .setTranslationY(0);
                                }
                            }
                        }
                );
    }

    @SimpleFunction(
            description = "Force le conteneur à s'agrandir dynamiquement quand le TextBox multiligne à l'intérieur grandit."
    )
    public void EnableAutoGrowWithText(
            final AndroidViewComponent cardContainer,
            final AndroidViewComponent editTextComponent) {

        if (cardContainer == null ||
                editTextComponent == null)
            return;

        final View containerView =
                cardContainer.getView();

        final View editView =
                editTextComponent.getView();

        if (containerView == null ||
                !(editView instanceof EditText))
            return;

        ((EditText) editView)
                .addTextChangedListener(
                        new TextWatcher() {

                            @Override
                            public void beforeTextChanged(
                                    CharSequence s,
                                    int start,
                                    int count,
                                    int after) {
                            }

                            @Override
                            public void onTextChanged(
                                    CharSequence s,
                                    int start,
                                    int before,
                                    int count) {
                            }

                            @Override
                            public void afterTextChanged(
                                    Editable s) {

                                containerView
                                        .requestLayout();

                                View parent =
                                        (View)
                                                containerView
                                                        .getParent();

                                if (parent != null) {
                                    parent.requestLayout();
                                }
                            }
                        }
                );
    }

    // =========================================================================
    // 3. CATALOGUE DE PRODUITS 2x2 NATIF
    // =========================================================================

    @SimpleFunction(
            description = "Construit la grille de produits depuis un JSON sans élévation."
    )
    public void BuildProductGridFromJson(
            final AndroidViewComponent scrollContainer,
            final String jsonData) {

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            final JSONArray array =
                                    new JSONArray(jsonData);

                            final int screenWidth =
                                    activity
                                            .getResources()
                                            .getDisplayMetrics()
                                            .widthPixels;

                            final int screenHeight =
                                    activity
                                            .getResources()
                                            .getDisplayMetrics()
                                            .heightPixels;

                            runOnUi(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            try {

                                                ViewGroup targetLayout =
                                                        getRealLayout(
                                                                scrollContainer
                                                        );

                                                if (targetLayout == null)
                                                    return;

                                                targetLayout
                                                        .removeAllViews();

                                                int cardWidth =
                                                        (int)
                                                                (
                                                                        screenWidth *
                                                                                0.44
                                                                );

                                                int cardHeight =
                                                        (int)
                                                                (
                                                                        screenHeight *
                                                                                0.28
                                                                );

                                                LinearLayout currentRow =
                                                        null;

                                                for (
                                                        int i = 0;
                                                        i < array.length();
                                                        i++
                                                ) {

                                                    JSONObject item =
                                                            array.getJSONObject(
                                                                    i
                                                            );

                                                    final String uid =
                                                            item.optString(
                                                                    "uid",
                                                                    String.valueOf(i)
                                                            );

                                                    String imageStr =
                                                            item.optString(
                                                                    "image",
                                                                    ""
                                                            );

                                                    String titleStr =
                                                            item.optString(
                                                                    "title",
                                                                    ""
                                                            );

                                                    String priceStr =
                                                            item.optString(
                                                                    "price",
                                                                    ""
                                                            );

                                                    final String productJson =
                                                            item.toString();

                                                    if (i % 2 == 0) {

                                                        currentRow =
                                                                new LinearLayout(
                                                                        context
                                                                );

                                                        currentRow
                                                                .setOrientation(
                                                                        LinearLayout.HORIZONTAL
                                                                );

                                                        currentRow
                                                                .setGravity(
                                                                        Gravity.CENTER_HORIZONTAL
                                                                );

                                                        LinearLayout.LayoutParams rowParams =
                                                                new LinearLayout.LayoutParams(
                                                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                                                );

                                                        rowParams.setMargins(
                                                                0,
                                                                8,
                                                                0,
                                                                8
                                                        );

                                                        currentRow
                                                                .setLayoutParams(
                                                                        rowParams
                                                                );

                                                        targetLayout
                                                                .addView(
                                                                        currentRow
                                                                );
                                                    }

                                                    CardView card =
                                                            new CardView(
                                                                    context
                                                            );

                                                    LinearLayout.LayoutParams cardParams =
                                                            new LinearLayout.LayoutParams(
                                                                    cardWidth,
                                                                    cardHeight
                                                            );

                                                    cardParams.setMargins(
                                                            10,
                                                            8,
                                                            10,
                                                            8
                                                    );

                                                    card.setLayoutParams(
                                                            cardParams
                                                    );

                                                    card.setRadius(20f);

                                                    card.setCardBackgroundColor(
                                                            Color.WHITE
                                                    );

                                                    card.setCardElevation(
                                                            0f
                                                    );

                                                    card.setMaxCardElevation(
                                                            0f
                                                    );

                                                    if (Build.VERSION.SDK_INT >=
                                                            Build.VERSION_CODES.LOLLIPOP) {

                                                        card.setOutlineProvider(
                                                                null
                                                        );
                                                    }

                                                    LinearLayout inner =
                                                            new LinearLayout(
                                                                    context
                                                            );

                                                    inner.setOrientation(
                                                            LinearLayout.VERTICAL
                                                    );

                                                    inner.setBackgroundColor(
                                                            Color.WHITE
                                                    );

                                                    inner.setLayoutParams(
                                                            new LinearLayout.LayoutParams(
                                                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                                                    LinearLayout.LayoutParams.MATCH_PARENT
                                                            )
                                                    );

                                                    ImageView img =
                                                            new ImageView(
                                                                    context
                                                            );

                                                    LinearLayout.LayoutParams imgParams =
                                                            new LinearLayout.LayoutParams(
                                                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                                                    0,
                                                                    1.0f
                                                            );

                                                    img.setLayoutParams(
                                                            imgParams
                                                    );

                                                    img.setScaleType(
                                                            ImageView.ScaleType.CENTER_CROP
                                                    );

                                                    img.setBackgroundColor(
                                                            Color.parseColor(
                                                                    "#F5F5F5"
                                                            )
                                                    );

                                                    loadImageAsync(
                                                            img,
                                                            imageStr
                                                    );

                                                    inner.addView(img);

                                                    TextView titleTv =
                                                            new TextView(
                                                                    context
                                                            );

                                                    titleTv.setText(
                                                            titleStr
                                                    );

                                                    titleTv.setTextColor(
                                                            Color.BLACK
                                                    );

                                                    titleTv.setTextSize(
                                                            13
                                                    );

                                                    titleTv.setMaxLines(
                                                            2
                                                    );

                                                    titleTv.setPadding(
                                                            14,
                                                            8,
                                                            14,
                                                            0
                                                    );

                                                    if (customTypeface != null) {

                                                        titleTv.setTypeface(
                                                                customTypeface
                                                        );
                                                    }

                                                    inner.addView(
                                                            titleTv
                                                    );

                                                    TextView priceTv =
                                                            new TextView(
                                                                    context
                                                            );

                                                    priceTv.setText(
                                                            priceStr
                                                    );

                                                    priceTv.setTextColor(
                                                            Color.BLACK
                                                    );

                                                    priceTv.setTextSize(
                                                            14
                                                    );

                                                    priceTv.setTypeface(
                                                            null,
                                                            Typeface.BOLD
                                                    );

                                                    priceTv.setPadding(
                                                            14,
                                                            2,
                                                            14,
                                                            12
                                                    );

                                                    if (customTypeface != null) {

                                                        priceTv.setTypeface(
                                                                customTypeface,
                                                                Typeface.BOLD
                                                        );
                                                    }

                                                    inner.addView(
                                                            priceTv
                                                    );

                                                    card.addView(
                                                            inner
                                                    );

                                                    card.setOnClickListener(
                                                            new View.OnClickListener() {

                                                                @Override
                                                                public void onClick(
                                                                        View v) {

                                                                    OnProductCardClick(
                                                                            uid,
                                                                            productJson
                                                                    );
                                                                }
                                                            }
                                                    );

                                                    if (currentRow != null) {

                                                        currentRow.addView(
                                                                card
                                                        );
                                                    }
                                                }

                                            } catch (Exception e) {

                                                e.printStackTrace();
                                            }
                                        }
                                    }
                            );

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    // =========================================================================
    // 3B. LISTE DYNAMIQUE DE CATÉGORIES
    // =========================================================================

    @SimpleFunction(
            description = "Génère la liste des catégories/sous-catégories depuis un JSON."
    )
    public void BuildCategoryListFromJson(
            final AndroidViewComponent listContainer,
            final String categoriesJson) {

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            final JSONArray mainArray =
                                    new JSONArray(
                                            categoriesJson
                                    );

                            runOnUi(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            try {

                                                ViewGroup target =
                                                        getRealLayout(
                                                                listContainer
                                                        );

                                                if (target == null)
                                                    return;

                                                target.removeAllViews();

                                                RadioGroup group =
                                                        new RadioGroup(
                                                                activity
                                                        );

                                                group.setOrientation(
                                                        LinearLayout.VERTICAL
                                                );

                                                ColorStateList radioColors =
                                                        ColorStateList.valueOf(
                                                                radioButtonColor
                                                        );

                                                for (
                                                        int i = 0;
                                                        i < mainArray.length();
                                                        i++
                                                ) {

                                                    JSONObject category =
                                                            mainArray
                                                                    .getJSONObject(
                                                                            i
                                                                    );

                                                    String categoryName =
                                                            category.optString(
                                                                    "title",
                                                                    ""
                                                            );

                                                    JSONArray subCategories =
                                                            category.optJSONArray(
                                                                    "subcategories"
                                                            );

                                                    TextView header =
                                                            new TextView(
                                                                    activity
                                                            );

                                                    header.setText(
                                                            ">  " +
                                                                    categoryName
                                                    );

                                                    header.setTextColor(
                                                            Color.parseColor(
                                                                    "#E91A1A1B"
                                                            )
                                                    );

                                                    header.setTextSize(
                                                            18
                                                    );

                                                    header.setTypeface(
                                                            customTypeface,
                                                            Typeface.BOLD
                                                    );

                                                    header.setPadding(
                                                            0,
                                                            (int) dpToPx(16),
                                                            0,
                                                            (int) dpToPx(8)
                                                    );

                                                    group.addView(
                                                            header
                                                    );

                                                    if (subCategories != null) {

                                                        for (
                                                                int j = 0;
                                                                j < subCategories.length();
                                                                j++
                                                        ) {

                                                            JSONObject sub =
                                                                    subCategories
                                                                            .getJSONObject(
                                                                                    j
                                                                            );

                                                            final String id =
                                                                    sub.optString(
                                                                            "id",
                                                                            ""
                                                                    );

                                                            final String title =
                                                                    sub.optString(
                                                                            "title",
                                                                            ""
                                                                    );

                                                            RadioButton button =
                                                                    new RadioButton(
                                                                            activity
                                                                    );

                                                            button.setId(
                                                                    View.generateViewId()
                                                            );

                                                            button.setText(
                                                                    title
                                                            );

                                                            button.setTextColor(
                                                                    Color.parseColor(
                                                                            "#C01A1A1B"
                                                                    )
                                                            );

                                                            button.setTextSize(
                                                                    13
                                                            );

                                                            if (Build.VERSION.SDK_INT >=
                                                                    Build.VERSION_CODES.LOLLIPOP) {

                                                                button.setButtonTintList(
                                                                        radioColors
                                                                );
                                                            }

                                                            if (customTypeface != null) {

                                                                button.setTypeface(
                                                                        customTypeface
                                                                );
                                                            }

                                                            button.setPadding(
                                                                    (int) dpToPx(8),
                                                                    (int) dpToPx(12),
                                                                    (int) dpToPx(8),
                                                                    (int) dpToPx(12)
                                                            );

                                                            button.setOnClickListener(
                                                                    new View.OnClickListener() {

                                                                        @Override
                                                                        public void onClick(
                                                                                View v) {

                                                                            OnCategorySelected(
                                                                                    id,
                                                                                    title
                                                                            );
                                                                        }
                                                                    }
                                                            );

                                                            group.addView(
                                                                    button
                                                            );

                                                            View divider =
                                                                    new View(
                                                                            activity
                                                                    );

                                                            divider.setLayoutParams(
                                                                    new LinearLayout.LayoutParams(
                                                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                                                            (int) dpToPx(1)
                                                                    )
                                                            );

                                                            divider.setBackgroundColor(
                                                                    Color.parseColor(
                                                                            "#F0F0F0"
                                                                    )
                                                            );

                                                            group.addView(
                                                                    divider
                                                            );
                                                        }
                                                    }
                                                }

                                                target.addView(
                                                        group
                                                );

                                            } catch (Exception e) {

                                                e.printStackTrace();
                                            }
                                        }
                                    }
                            );

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    // =========================================================================
    // 4. EFFETS VISUELS
    // =========================================================================

    @SimpleFunction(
            description = "Applique un dégradé de couleur sur un composant."
    )
    public void SetGradientBackground(
            final AndroidViewComponent component,
            final int startColor,
            final int endColor,
            final String orientation) {

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            GradientDrawable.Orientation gradOrientation =
                                    GradientDrawable
                                            .Orientation
                                            .TOP_BOTTOM;

                            if ("LEFT_RIGHT"
                                    .equalsIgnoreCase(
                                            orientation
                                    )) {

                                gradOrientation =
                                        GradientDrawable
                                                .Orientation
                                                .LEFT_RIGHT;
                            }

                            GradientDrawable gd =
                                    new GradientDrawable(
                                            gradOrientation,
                                            new int[]{
                                                    startColor,
                                                    endColor
                                            }
                                    );

                            gd.setCornerRadius(0f);

                            component.getView()
                                    .setBackground(gd);

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    // =========================================================================
    // 5. DIALOGUE TRANSPARENT, NOTIFICATION & SON
    // =========================================================================

    @SimpleFunction(
            description = "Affiche un composant sous forme de dialogue transparent. Détache automatiquement le composant de son ancien parent."
    )
    public void ShowAlphaDialog(
            final AndroidViewComponent dialogContentLayout,
            final boolean cancelable) {

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            DismissAlphaDialog();

                            if (dialogContentLayout == null ||
                                    dialogContentLayout.getView() == null) {

                                return;
                            }

                            final View contentView =
                                    dialogContentLayout.getView();

                            if (contentView.getParent() != null) {

                                ((ViewGroup)
                                        contentView.getParent())
                                        .removeView(
                                                contentView
                                        );
                            }

                            contentView.setVisibility(
                                    View.VISIBLE
                            );

                            activeDialogContentView =
                                    contentView;

                            activeAlphaDialog =
                                    new Dialog(activity);

                            activeAlphaDialog
                                    .requestWindowFeature(
                                            Window.FEATURE_NO_TITLE
                                    );

                            activeAlphaDialog.setContentView(
                                    contentView
                            );

                            if (activeAlphaDialog
                                    .getWindow() != null) {

                                activeAlphaDialog
                                        .getWindow()
                                        .setBackgroundDrawable(
                                                new ColorDrawable(
                                                        Color.TRANSPARENT
                                                )
                                        );

                                activeAlphaDialog
                                        .getWindow()
                                        .setLayout(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                        );
                            }

                            activeAlphaDialog.setCancelable(
                                    cancelable
                            );

                            activeAlphaDialog.setOnDismissListener(
                                    new DialogInterface.OnDismissListener() {

                                        @Override
                                        public void onDismiss(
                                                DialogInterface dialog) {

                                            if (contentView
                                                    .getParent() != null) {

                                                ((ViewGroup)
                                                        contentView
                                                                .getParent())
                                                        .removeView(
                                                                contentView
                                                        );
                                            }

                                            if (activeDialogContentView ==
                                                    contentView) {

                                                activeDialogContentView =
                                                        null;
                                            }
                                        }
                                    }
                            );

                            activeAlphaDialog.show();

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Ferme le dialogue Alpha et libère son conteneur."
    )
    public void DismissAlphaDialog() {

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            if (activeAlphaDialog != null) {

                                if (activeAlphaDialog.isShowing()) {

                                    activeAlphaDialog.dismiss();
                                }

                                activeAlphaDialog = null;
                            }

                            if (activeDialogContentView != null) {

                                if (activeDialogContentView
                                        .getParent() != null) {

                                    ((ViewGroup)
                                            activeDialogContentView
                                                    .getParent())
                                            .removeView(
                                                    activeDialogContentView
                                            );
                                }

                                activeDialogContentView = null;
                            }

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Notification personnalisée temporaire."
    )
    public void CustomNotifier(
            final AndroidViewComponent customLayout,
            final int durationMs) {

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        ShowAlphaDialog(
                                customLayout,
                                true
                        );

                        new Handler(
                                Looper.getMainLooper()
                        ).postDelayed(
                                new Runnable() {

                                    @Override
                                    public void run() {

                                        DismissAlphaDialog();
                                    }
                                },
                                durationMs
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Joue un son personnalisé."
    )
    public void PlayCustomSound(
            final String fileNameOrPath) {

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        MediaPlayer mediaPlayer =
                                null;

                        try {

                            mediaPlayer =
                                    new MediaPlayer();

                            mediaPlayer
                                    .setAudioAttributes(
                                            new AudioAttributes.Builder()
                                                    .setContentType(
                                                            AudioAttributes
                                                                    .CONTENT_TYPE_SONIFICATION
                                                    )
                                                    .setUsage(
                                                            AudioAttributes
                                                                    .USAGE_ASSISTANCE_SONIFICATION
                                                    )
                                                    .build()
                                    );

                            if (fileNameOrPath.startsWith("/")) {

                                mediaPlayer.setDataSource(
                                        fileNameOrPath
                                );

                            } else {

                                android.content.res.AssetFileDescriptor afd =
                                        context.getAssets()
                                                .openFd(
                                                        fileNameOrPath
                                                );

                                mediaPlayer.setDataSource(
                                        afd.getFileDescriptor(),
                                        afd.getStartOffset(),
                                        afd.getLength()
                                );

                                afd.close();
                            }

                            mediaPlayer.prepare();

                            mediaPlayer.setOnCompletionListener(
                                    new MediaPlayer.OnCompletionListener() {

                                        @Override
                                        public void onCompletion(
                                                MediaPlayer mp) {

                                            mp.release();
                                        }
                                    }
                            );

                            mediaPlayer.start();

                        } catch (Exception e) {

                            e.printStackTrace();

                            if (mediaPlayer != null) {
                                mediaPlayer.release();
                            }
                        }
                    }
                }
        );
    }

    // =========================================================================
    // 6. GALERIE D'IMAGES & COMPRESSION
    // =========================================================================

    @SimpleFunction(
            description = "Ouvre la galerie d'images native, avec demande de permission."
    )
    public void OpenPhotoPicker() {

        String permission;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            permission =
                    "android.permission.READ_MEDIA_IMAGES";

        } else {

            permission =
                    "android.permission.READ_EXTERNAL_STORAGE";
        }

        form.askPermission(
                permission,
                new PermissionResultHandler() {

                    @Override
                    public void HandlePermissionResponse(
                            String permissionName,
                            boolean granted) {

                        if (!granted) {

                            OnError(
                                    "Permission refusée."
                            );

                            return;
                        }

                        activity.runOnUiThread(
                                new Runnable() {

                                    @Override
                                    public void run() {

                                        try {

                                            Intent intent =
                                                    new Intent(
                                                            Intent.ACTION_PICK
                                                    );

                                            intent.setType(
                                                    "image/*"
                                            );

                                            form.startActivityForResult(
                                                    intent,
                                                    requestCode
                                            );

                                        } catch (Exception e) {

                                            OnError(
                                                    "OpenPhotoPicker: " +
                                                            e.getMessage()
                                            );
                                        }
                                    }
                                }
                        );
                    }
                }
        );
    }

    @Override
    public void resultReturned(
            int receivedRequestCode,
            int resultCode,
            Intent data) {

        if (receivedRequestCode != requestCode)
            return;

        if (resultCode == Activity.RESULT_OK && data != null) {

            Uri selectedImageUri = data.getData();

            if (selectedImageUri != null) {

                OnPhotoPicked(selectedImageUri.toString());

            } else {

                OnError("Aucune image sélectionnée (URI nulle).");
            }

        } else {

            OnError(
                    "Sélection annulée ou échouée (resultCode: " + resultCode + ")"
            );
        }
    }

    @SimpleFunction(
            description = "Compresse une image sans surcharger la mémoire."
    )
    public String CompressImage(
            String imagePath,
            int quality,
            int maxWidth) {

        try {

            BitmapFactory.Options options =
                    new BitmapFactory.Options();

            options.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(
                    imagePath,
                    options
            );

            if (options.outWidth <= 0 ||
                    options.outHeight <= 0) {

                return imagePath;
            }

            int srcWidth =
                    options.outWidth;

            int inSampleSize = 1;

            if (srcWidth > maxWidth) {

                inSampleSize =
                        Math.round(
                                (float) srcWidth /
                                        (float) maxWidth
                        );
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize =
                    inSampleSize;

            Bitmap bitmap =
                    BitmapFactory.decodeFile(
                            imagePath,
                            options
                    );

            if (bitmap == null)
                return imagePath;

            File outputFile =
                    new File(
                            context.getCacheDir(),
                            "comp_" +
                                    System.currentTimeMillis() +
                                    ".jpg"
                    );

            FileOutputStream out =
                    null;

            try {

                out =
                        new FileOutputStream(
                                outputFile
                        );

                bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        quality,
                        out
                );

                out.flush();

            } finally {

                if (out != null)
                    out.close();

                bitmap.recycle();
            }

            return outputFile.getAbsolutePath();

        } catch (Exception e) {

            e.printStackTrace();

            return imagePath;
        }
    }

    // =========================================================================
    // 7. DÉTAILS PRODUIT
    // =========================================================================

    @SimpleFunction(
            description = "Définit l'URL du serveur utilisée pour demander les détails d'un produit."
    )
    public void SetProductDetailsEndpoint(
            String endpointUrl) {

        if (endpointUrl == null) {

            productDetailsEndpoint = "";

        } else {

            productDetailsEndpoint =
                    endpointUrl.trim();
        }
    }

    @SimpleFunction(
            description = "Demande au serveur les détails complets d'un produit à partir de son productUid."
    )
    public void RequestProductDetails(
            final String productUid) {

        if (productUid == null ||
                productUid.trim().isEmpty()) {

            OnProductDetailsError(
                    "productUid vide."
            );

            return;
        }

        if (productDetailsEndpoint == null ||
                productDetailsEndpoint.trim().isEmpty()) {

            OnProductDetailsError(
                    "Endpoint des détails produit non défini. Utilise SetProductDetailsEndpoint."
            );

            return;
        }

        final String uid =
                productUid.trim();

        productDetailsLoading = true;

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        HttpURLConnection conn =
                                null;

                        try {

                            URL url =
                                    new URL(
                                            productDetailsEndpoint
                                    );

                            conn =
                                    (HttpURLConnection)
                                            url.openConnection();

                            conn.setConnectTimeout(
                                    15000
                            );

                            conn.setReadTimeout(
                                    15000
                            );

                            conn.setRequestMethod(
                                    "POST"
                            );

                            conn.setDoOutput(true);

                            conn.setRequestProperty(
                                    "Content-Type",
                                    "application/json; charset=utf-8"
                            );

                            conn.setRequestProperty(
                                    "Accept",
                                    "application/json"
                            );

                            JSONObject request =
                                    new JSONObject();

                            request.put(
                                    "action",
                                    "get_product_details"
                            );

                            request.put(
                                    "productUid",
                                    uid
                            );

                            OutputStream os =
                                    conn.getOutputStream();

                            os.write(
                                    request.toString()
                                            .getBytes("UTF-8")
                            );

                            os.flush();
                            os.close();

                            final int responseCode =
                                    conn.getResponseCode();

                            InputStream is =
                                    (responseCode >= 200 &&
                                            responseCode < 400)
                                            ? conn.getInputStream()
                                            : conn.getErrorStream();

                            final String responseContent =
                                    lireFlux(is);

                            if (responseCode < 200 ||
                                    responseCode >= 400) {

                                activity.runOnUiThread(
                                        new Runnable() {

                                            @Override
                                            public void run() {

                                                productDetailsLoading =
                                                        false;

                                                OnProductDetailsError(
                                                        "Erreur serveur HTTP " +
                                                                responseCode +
                                                                ": " +
                                                                responseContent
                                                );
                                            }
                                        }
                                );

                                return;
                            }

                            activity.runOnUiThread(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            traiterProductDetailsResponse(
                                                    uid,
                                                    responseContent
                                            );
                                        }
                                    }
                            );

                        } catch (
                                final Exception e) {

                            activity.runOnUiThread(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            productDetailsLoading =
                                                    false;

                                            OnProductDetailsError(
                                                    "RequestProductDetails: " +
                                                            e.getMessage()
                                            );
                                        }
                                    }
                            );

                        } finally {

                            if (conn != null) {
                                conn.disconnect();
                            }
                        }
                    }
                }
        );
    }

    private void traiterProductDetailsResponse(
            String requestedUid,
            String responseContent) {

        try {

            JSONObject response =
                    new JSONObject(
                            responseContent
                    );

            JSONObject product =
                    response;

            JSONObject nestedProduct =
                    response.optJSONObject(
                            "product"
                    );

            if (nestedProduct != null) {

                product =
                        nestedProduct;

            } else {

                JSONObject data =
                        response.optJSONObject(
                                "data"
                        );

                if (data != null) {
                    product = data;
                }
            }

            currentProductUid =
                    product.optString(
                            "productUid",
                            product.optString(
                                    "uid",
                                    requestedUid
                            )
                    );

            currentProductDescription =
                    product.optString(
                            "description",
                            ""
                    );

            if (product.has("stock")) {

                currentProductStock =
                        product.optInt(
                                "stock",
                                0
                        );

            } else {

                currentProductStock =
                        product.optInt(
                                "quantity",
                                0
                        );
            }

            currentProductImage2 =
                    product.optString(
                            "image2",
                            ""
                    );

            currentProductImage3 =
                    product.optString(
                            "image3",
                            ""
                    );

            currentProductImage4 =
                    product.optString(
                            "image4",
                            ""
                    );

            currentProductImage5 =
                    product.optString(
                            "image5",
                            ""
                    );

            currentProductDeliveryIncluded =
                    product.optBoolean(
                            "deliveryIncluded",
                            false
                    );

            productDetailsLoading =
                    false;

            OnProductDetailsReceived(
                    currentProductUid,
                    currentProductDescription,
                    currentProductStock,
                    currentProductImage2,
                    currentProductImage3,
                    currentProductImage4,
                    currentProductImage5,
                    currentProductDeliveryIncluded
            );

        } catch (Exception e) {

            productDetailsLoading =
                    false;

            OnProductDetailsError(
                    "JSON détails produit invalide: " +
                            e.getMessage()
            );
        }
    }

    @SimpleFunction(
            description = "Retourne l'identifiant du dernier produit dont les détails ont été reçus."
    )
    public String GetProductDetailsUid() {

        return currentProductUid;
    }

    @SimpleFunction(
            description = "Retourne la description du dernier produit chargé."
    )
    public String GetProductDescription() {

        return currentProductDescription;
    }

    @SimpleFunction(
            description = "Retourne le stock ou la quantité disponible du dernier produit chargé."
    )
    public int GetProductStock() {

        return currentProductStock;
    }

    @SimpleFunction(
            description = "Retourne la deuxième image du produit."
    )
    public String GetProductImage2() {

        return currentProductImage2;
    }

    @SimpleFunction(
            description = "Retourne la troisième image du produit."
    )
    public String GetProductImage3() {

        return currentProductImage3;
    }

    @SimpleFunction(
            description = "Retourne la quatrième image du produit."
    )
    public String GetProductImage4() {

        return currentProductImage4;
    }

    @SimpleFunction(
            description = "Retourne la cinquième image du produit."
    )
    public String GetProductImage5() {

        return currentProductImage5;
    }

    @SimpleFunction(
            description = "Retourne vrai si la livraison est incluse dans le produit."
    )
    public boolean GetProductDeliveryIncluded() {

        return currentProductDeliveryIncluded;
    }

    @SimpleFunction(
            description = "Indique si une demande de détails produit est actuellement en cours."
    )
    public boolean IsProductDetailsLoading() {

        return productDetailsLoading;
    }

    @SimpleFunction(
            description = "Efface les détails produit actuellement mémorisés."
    )
    public void ClearProductDetails() {

        currentProductUid = "";
        currentProductDescription = "";
        currentProductStock = 0;

        currentProductImage2 = "";
        currentProductImage3 = "";
        currentProductImage4 = "";
        currentProductImage5 = "";

        currentProductDeliveryIncluded =
                false;

        productDetailsLoading =
                false;
    }

    @SimpleFunction(
            description = "Construit un JSON produit à partir des derniers détails reçus."
    )
    public String BuildProductDetailsJson() {

        JSONObject o =
                new JSONObject();

        try {

            o.put(
                    "uid",
                    currentProductUid
            );

            o.put(
                    "description",
                    currentProductDescription
            );

            o.put(
                    "stock",
                    currentProductStock
            );

            o.put(
                    "image2",
                    currentProductImage2
            );

            o.put(
                    "image3",
                    currentProductImage3
            );

            o.put(
                    "image4",
                    currentProductImage4
            );

            o.put(
                    "image5",
                    currentProductImage5
            );

            o.put(
                    "deliveryIncluded",
                    currentProductDeliveryIncluded
            );

        } catch (Exception ignored) {}

        return o.toString();
    }

    // =========================================================================
    // 8. NOTIFIER PERSONNALISÉ (style toast)
    // =========================================================================

    @SimpleFunction(
            description = "Affiche un notifier personnalisé (icône + texte) en haut ou en bas de l'écran, façon toast. position: \"top\" ou \"bottom\"."
    )
    public void ShowNotifier(
            final String iconPath,
            final String message,
            final int heightDp,
            final String position,
            final int durationMs) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {

                    FrameLayout root =
                            (FrameLayout) activity.findViewById(android.R.id.content);

                    if (root == null) return;

                    if (activeNotifierView != null &&
                            activeNotifierView.getParent() != null) {

                        ((ViewGroup) activeNotifierView.getParent())
                                .removeView(activeNotifierView);

                        activeNotifierView = null;
                    }

                    LinearLayout notifier = new LinearLayout(activity);
                    notifier.setOrientation(LinearLayout.HORIZONTAL);
                    notifier.setGravity(Gravity.CENTER_VERTICAL);
                    notifier.setPadding(
                            (int) dpToPx(16),
                            0,
                            (int) dpToPx(16),
                            0
                    );

                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.parseColor("#2B2B2B"));
                    bg.setCornerRadius(dpToPx(heightDp) / 2f);
                    notifier.setBackground(bg);

                    if (iconPath != null && !iconPath.trim().isEmpty()) {

                        ImageView iconView = new ImageView(activity);

                        try {
                            Drawable d = MediaUtil.getBitmapDrawable(monForm, iconPath);
                            iconView.setImageDrawable(d);
                        } catch (Exception ignored) {}

                        int iconSize = (int) dpToPx(heightDp - 12);

                        LinearLayout.LayoutParams iconParams =
                                new LinearLayout.LayoutParams(iconSize, iconSize);

                        iconParams.setMargins(0, 0, (int) dpToPx(10), 0);

                        iconView.setLayoutParams(iconParams);

                        notifier.addView(iconView);
                    }

                    TextView textView = new TextView(activity);
                    textView.setText(message);
                    textView.setTextColor(Color.WHITE);
                    textView.setTextSize(14);

                    if (customTypeface != null) {
                        textView.setTypeface(customTypeface);
                    }

                    notifier.addView(textView);

                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            (int) dpToPx(heightDp)
                    );

                    boolean isTop = "top".equalsIgnoreCase(position);

                    params.gravity = (isTop ? Gravity.TOP : Gravity.BOTTOM)
                            | Gravity.CENTER_HORIZONTAL;

                    params.setMargins(0, (int) dpToPx(24), 0, (int) dpToPx(24));

                    root.addView(notifier, params);

                    activeNotifierView = notifier;

                    if (durationMs > 0) {

                        final View notifierFinal = notifier;

                        new Handler(Looper.getMainLooper()).postDelayed(
                                new Runnable() {
                                    @Override
                                    public void run() {

                                        if (notifierFinal.getParent() != null) {
                                            ((ViewGroup) notifierFinal.getParent())
                                                    .removeView(notifierFinal);
                                        }

                                        if (activeNotifierView == notifierFinal) {
                                            activeNotifierView = null;
                                        }
                                    }
                                },
                                durationMs
                        );
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SimpleFunction(description = "Masque immédiatement le notifier actuellement affiché.")
    public void DismissNotifier() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activeNotifierView != null) {
                    if (activeNotifierView.getParent() != null) {
                        ((ViewGroup) activeNotifierView.getParent())
                                .removeView(activeNotifierView);
                    }
                    activeNotifierView = null;
                }
            }
        });
    }

    // =========================================================================
    // 9. MENU DÉROULANT ANCRÉ
    // =========================================================================

    @SimpleFunction(description = "Ajoute un élément au menu déroulant avec une icône image (fichier). À appeler avant DropdownMenuShow.")
    public void DropdownMenuAddItem(String id, String icon, String text) {
        dropdownIdsEnAttente.add(id);
        dropdownIconsEnAttente.add(icon);
        dropdownIconTypesEnAttente.add("image");
        dropdownTextsEnAttente.add(text);
    }

    @SimpleFunction(description = "Ajoute un élément au menu déroulant avec une icône de police (ex: Phosphor). Utilise DropdownMenuSetIconFont avant pour définir la police. À appeler avant DropdownMenuShow.")
    public void DropdownMenuAddTextIcon(String id, String unicodeChar, String text) {
        dropdownIdsEnAttente.add(id);
        dropdownIconsEnAttente.add(unicodeChar);
        dropdownIconTypesEnAttente.add("font");
        dropdownTextsEnAttente.add(text);
    }

    @SimpleFunction(description = "Définit la police (.ttf/.otf) utilisée pour les icônes de type police du menu déroulant. À appeler avant DropdownMenuShow.")
    public void DropdownMenuSetIconFont(String fontPath) {
        try {
            if (fontPath == null || fontPath.trim().isEmpty()) {
                dropdownIconFont = null;
                return;
            }
            if (fontPath.startsWith("/")) {
                dropdownIconFont = Typeface.createFromFile(new File(fontPath));
            } else {
                dropdownIconFont = Typeface.createFromAsset(context.getAssets(), fontPath);
            }
        } catch (Exception e) {
            dropdownIconFont = null;
            OnError("DropdownMenuSetIconFont: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Vide la liste des éléments du menu déroulant en attente.")
    public void DropdownMenuClear() {
        dropdownIdsEnAttente.clear();
        dropdownIconsEnAttente.clear();
        dropdownIconTypesEnAttente.clear();
        dropdownTextsEnAttente.clear();
    }

    @SimpleFunction(description = "Affiche le menu déroulant ancré près du composant donné (ex: un bouton 3 points).")
    public void DropdownMenuShow(final AndroidViewComponent anchorComponent) {

        if (anchorComponent == null || anchorComponent.getView() == null) {
            OnError("DropdownMenuShow: composant ancre invalide.");
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {

                    if (activeDropdownPopup != null && activeDropdownPopup.isShowing()) {
                        activeDropdownPopup.dismiss();
                    }

                    LinearLayout menu = new LinearLayout(activity);
                    menu.setOrientation(LinearLayout.VERTICAL);
                    menu.setPadding(
                            (int) dpToPx(4),
                            (int) dpToPx(4),
                            (int) dpToPx(4),
                            (int) dpToPx(4)
                    );

                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.WHITE);
                    bg.setCornerRadius(dpToPx(14));
                    menu.setBackground(bg);
                    menu.setElevation(dpToPx(4));

                    for (int i = 0; i < dropdownIdsEnAttente.size(); i++) {

                        final String itemId = dropdownIdsEnAttente.get(i);
                        String itemIcon = dropdownIconsEnAttente.get(i);
                        String itemText = dropdownTextsEnAttente.get(i);
                        String itemIconType =
                                (i < dropdownIconTypesEnAttente.size())
                                        ? dropdownIconTypesEnAttente.get(i)
                                        : "image";

                        LinearLayout row = new LinearLayout(activity);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setGravity(Gravity.CENTER_VERTICAL);
                        row.setPadding(
                                (int) dpToPx(14),
                                (int) dpToPx(12),
                                (int) dpToPx(20),
                                (int) dpToPx(12)
                        );

                        if (itemIcon != null && !itemIcon.trim().isEmpty()) {

                            if ("font".equals(itemIconType)) {

                                TextView iconTv = new TextView(activity);
                                iconTv.setText(itemIcon);
                                iconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                                iconTv.setTextColor(Color.parseColor("#1A1A1B"));

                                if (dropdownIconFont != null) {
                                    iconTv.setTypeface(dropdownIconFont);
                                }

                                LinearLayout.LayoutParams iconParams =
                                        new LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                        );

                                iconParams.setMargins(0, 0, (int) dpToPx(14), 0);

                                iconTv.setLayoutParams(iconParams);

                                row.addView(iconTv);

                            } else {

                                ImageView iconView = new ImageView(activity);

                                try {
                                    Drawable d = MediaUtil.getBitmapDrawable(monForm, itemIcon);
                                    iconView.setImageDrawable(d);
                                } catch (Exception ignored) {}

                                LinearLayout.LayoutParams iconParams =
                                        new LinearLayout.LayoutParams(
                                                (int) dpToPx(22),
                                                (int) dpToPx(22)
                                        );

                                iconParams.setMargins(0, 0, (int) dpToPx(14), 0);

                                iconView.setLayoutParams(iconParams);

                                row.addView(iconView);
                            }
                        }

                        TextView textView = new TextView(activity);
                        textView.setText(itemText);
                        textView.setTextSize(15);
                        textView.setTextColor(Color.parseColor("#1A1A1B"));

                        if (customTypeface != null) {
                            textView.setTypeface(customTypeface);
                        }

                        row.addView(textView);

                        row.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (activeDropdownPopup != null) {
                                    activeDropdownPopup.dismiss();
                                }
                                OnDropdownItemClick(itemId);
                            }
                        });

                        menu.addView(row);
                    }

                    final android.widget.PopupWindow popup = new android.widget.PopupWindow(
                            menu,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            true
                    );

                    popup.setOutsideTouchable(true);
                    popup.setElevation(dpToPx(4));

                    activeDropdownPopup = popup;

                    popup.showAsDropDown(anchorComponent.getView(), 0, (int) dpToPx(4));

                    // Vide automatiquement la liste en attente pour éviter
                    // toute duplication si DropdownMenuAddItem est rappelé
                    // avant un prochain DropdownMenuShow sans Clear explicite.
                    dropdownIdsEnAttente.clear();
                    dropdownIconsEnAttente.clear();
                    dropdownIconTypesEnAttente.clear();
                    dropdownTextsEnAttente.clear();

                } catch (Exception e) {
                    OnError("DropdownMenuShow: " + e.getMessage());
                }
            }
        });
    }

    @SimpleEvent(description = "Déclenché lorsqu'un élément du menu déroulant est cliqué.")
    public void OnDropdownItemClick(String id) {
        EventDispatcher.dispatchEvent(this, "OnDropdownItemClick", id);
    }

    // =========================================================================
    // 10. CARTES DE CONVERSATION (liste type messagerie)
    // =========================================================================

    @SimpleFunction(description = "Définit les couleurs des cartes de conversation (fond normal, fond sélectionné, texte nouveau message, texte dernier message).")
    public void SetConversationCardColors(
            int backgroundColor,
            int selectedColor,
            int newMessageColor,
            int lastMessageColor) {

        conversationCardBackgroundColor = backgroundColor;
        conversationCardSelectedColor = selectedColor;
        conversationNewMessageColor = newMessageColor;
        conversationLastMessageColor = lastMessageColor;
    }

    @SimpleFunction(description = "Ajoute ou met à jour une carte de conversation. productImage/productTitle proviennent directement d'un composant Image.Picture et Label.Text (pas besoin de JSON).")
    public void AddOrUpdateConversationCard(
            final AndroidViewComponent listContainer,
            final String conversationUid,
            final String productImage,
            final String productTitle,
            final String lastMessage,
            final boolean isNewMessage,
            final String dateText) {

        if (listContainer == null || listContainer.getView() == null) {
            OnError("AddOrUpdateConversationCard: conteneur invalide.");
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {

                    ViewGroup targetLayout = getRealLayout(listContainer);

                    if (targetLayout == null) {
                        targetLayout = (ViewGroup) listContainer.getView();
                    }

                    conversationListContainer = targetLayout;

                    View existingCard = conversationCards.get(conversationUid);

                    if (existingCard != null) {

                        TextView subtitle = conversationSubtitles.get(conversationUid);

                        if (subtitle != null) {

                            String displayText =
                                    (lastMessage == null || lastMessage.trim().isEmpty())
                                            ? "Négociation en cours"
                                            : lastMessage;

                            subtitle.setText(displayText);

                            if (isNewMessage) {
                                subtitle.setTypeface(
                                        customTypeface != null ? customTypeface : Typeface.DEFAULT,
                                        Typeface.BOLD
                                );
                                subtitle.setTextColor(conversationNewMessageColor);
                            } else {
                                subtitle.setTypeface(
                                        customTypeface != null ? customTypeface : Typeface.DEFAULT
                                );
                                subtitle.setTextColor(conversationLastMessageColor);
                            }
                        }

                        return;
                    }

                    buildConversationCard(
                            targetLayout,
                            conversationUid,
                            productImage,
                            productTitle,
                            lastMessage,
                            isNewMessage,
                            dateText
                    );

                } catch (Exception e) {
                    OnError("AddOrUpdateConversationCard: " + e.getMessage());
                }
            }
        });
    }

    private void buildConversationCard(
            ViewGroup targetLayout,
            final String conversationUid,
            String productImage,
            String productTitle,
            String lastMessage,
            boolean isNewMessage,
            String dateText) {

        CardView card = new CardView(context);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        cardParams.setMargins(
                (int) dpToPx(12),
                (int) dpToPx(6),
                (int) dpToPx(12),
                (int) dpToPx(6)
        );

        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(16));
        card.setCardElevation(0f);
        card.setMaxCardElevation(0f);
        card.setCardBackgroundColor(conversationCardBackgroundColor);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(
                (int) dpToPx(14),
                (int) dpToPx(12),
                (int) dpToPx(14),
                (int) dpToPx(12)
        );

        int logoSize = (int) dpToPx(48);

        CardView logoCard = new CardView(context);

        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(logoSize, logoSize);

        logoParams.setMargins(0, 0, (int) dpToPx(12), 0);

        logoCard.setLayoutParams(logoParams);
        logoCard.setRadius(logoSize / 2f);
        logoCard.setCardElevation(0f);
        logoCard.setMaxCardElevation(0f);
        logoCard.setCardBackgroundColor(Color.parseColor("#F0F0F0"));

        ImageView logoImg = new ImageView(context);
        logoImg.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        logoImg.setScaleType(ImageView.ScaleType.CENTER_CROP);

        loadImageAsync(logoImg, productImage);

        logoCard.addView(logoImg);
        row.addView(logoCard);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleTv = new TextView(context);
        titleTv.setText(productTitle);
        titleTv.setTextColor(Color.parseColor("#1A1A1B"));
        titleTv.setTextSize(15);
        titleTv.setTypeface(customTypeface != null ? customTypeface : Typeface.DEFAULT, Typeface.BOLD);
        titleTv.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView dateTv = new TextView(context);
        dateTv.setText(dateText);
        dateTv.setTextColor(Color.parseColor("#8A8A8E"));
        dateTv.setTextSize(11);

        titleRow.addView(titleTv);
        titleRow.addView(dateTv);

        String displayText =
                (lastMessage == null || lastMessage.trim().isEmpty())
                        ? "Négociation en cours"
                        : lastMessage;

        TextView subtitleTv = new TextView(context);
        subtitleTv.setText(displayText);
        subtitleTv.setTextSize(13);
        subtitleTv.setMaxLines(1);
        subtitleTv.setEllipsize(android.text.TextUtils.TruncateAt.END);

        if (isNewMessage) {
            subtitleTv.setTypeface(customTypeface != null ? customTypeface : Typeface.DEFAULT, Typeface.BOLD);
            subtitleTv.setTextColor(conversationNewMessageColor);
        } else {
            subtitleTv.setTypeface(customTypeface != null ? customTypeface : Typeface.DEFAULT);
            subtitleTv.setTextColor(conversationLastMessageColor);
        }

        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        subtitleParams.setMargins(0, (int) dpToPx(3), 0, 0);

        subtitleTv.setLayoutParams(subtitleParams);

        textCol.addView(titleRow);
        textCol.addView(subtitleTv);

        row.addView(textCol);
        card.addView(row);

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (conversationSelectionModeActive) {

                    toggleConversationSelection(conversationUid);

                } else {

                    OnConversationCardClick(conversationUid);
                }
            }
        });

        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if (!conversationSelectionModeActive) {
                    conversationSelectionModeActive = true;
                }

                toggleConversationSelection(conversationUid);

                return true;
            }
        });

        targetLayout.addView(card);

        conversationCards.put(conversationUid, card);
        conversationSubtitles.put(conversationUid, subtitleTv);
        conversationSelected.put(conversationUid, false);
    }

    private void toggleConversationSelection(String conversationUid) {

        View card = conversationCards.get(conversationUid);

        if (card == null || !(card instanceof CardView)) return;

        boolean nowSelected =
                !(conversationSelected.containsKey(conversationUid)
                        && conversationSelected.get(conversationUid));

        conversationSelected.put(conversationUid, nowSelected);

        ((CardView) card).setCardBackgroundColor(
                nowSelected ? conversationCardSelectedColor : conversationCardBackgroundColor
        );

        int count = 0;

        for (Boolean sel : conversationSelected.values()) {
            if (sel != null && sel) count++;
        }

        if (count == 0) {
            conversationSelectionModeActive = false;
        }

        OnConversationSelectionChanged(count);
    }

    @SimpleFunction(description = "Annule le mode sélection des conversations.")
    public void CancelConversationSelection() {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                conversationSelectionModeActive = false;

                for (Map.Entry<String, View> entry : conversationCards.entrySet()) {

                    conversationSelected.put(entry.getKey(), false);

                    if (entry.getValue() instanceof CardView) {
                        ((CardView) entry.getValue())
                                .setCardBackgroundColor(conversationCardBackgroundColor);
                    }
                }

                OnConversationSelectionChanged(0);
            }
        });
    }

    @SimpleFunction(description = "Indique si le mode sélection des conversations est actif.")
    public boolean IsConversationSelectionModeActive() {
        return conversationSelectionModeActive;
    }

    @SimpleFunction(description = "Supprime toutes les cartes de conversation actuellement sélectionnées.")
    public void DeleteSelectedConversationCards() {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                List<String> toRemove = new ArrayList<>();

                for (Map.Entry<String, Boolean> entry : conversationSelected.entrySet()) {
                    if (entry.getValue() != null && entry.getValue()) {
                        toRemove.add(entry.getKey());
                    }
                }

                for (String uid : toRemove) {
                    removeConversationCardInternal(uid);
                }

                conversationSelectionModeActive = false;

                OnConversationSelectionChanged(0);
            }
        });
    }

    @SimpleFunction(description = "Supprime une carte de conversation précise par son UID.")
    public void DeleteConversationCard(final String conversationUid) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                removeConversationCardInternal(conversationUid);
            }
        });
    }

    private void removeConversationCardInternal(String conversationUid) {

        View card = conversationCards.remove(conversationUid);
        conversationSubtitles.remove(conversationUid);
        conversationSelected.remove(conversationUid);

        if (card != null && card.getParent() != null) {
            ((ViewGroup) card.getParent()).removeView(card);
        }
    }

    @SimpleEvent(description = "Déclenché lors du clic sur une carte de conversation (hors mode sélection).")
    public void OnConversationCardClick(String conversationUid) {
        EventDispatcher.dispatchEvent(this, "OnConversationCardClick", conversationUid);
    }

    @SimpleEvent(description = "Déclenché quand le nombre de cartes sélectionnées change.")
    public void OnConversationSelectionChanged(int selectedCount) {
        EventDispatcher.dispatchEvent(this, "OnConversationSelectionChanged", selectedCount);
    }

    // =========================================================================
    // 11. ÉCOUTE DU CHAMP DE SAISIE DE CHAT
    // =========================================================================

    @SimpleFunction(description = "Écoute un TextBox de saisie de message. Déclenche OnChatMessageSent quand l'utilisateur appuie sur Entrée/Envoyer, et vide le champ automatiquement.")
    public void EnableChatInputListener(final AndroidViewComponent editTextComponent) {

        if (editTextComponent == null || editTextComponent.getView() == null) {
            OnError("EnableChatInputListener: composant invalide.");
            return;
        }

        View view = editTextComponent.getView();

        if (!(view instanceof EditText)) {
            OnError("EnableChatInputListener: le composant n'est pas un TextBox.");
            return;
        }

        final EditText editText = (EditText) view;

        editText.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(android.widget.TextView v, int actionId, android.view.KeyEvent event) {

                boolean isSendAction =
                        actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND
                                || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                                || (event != null
                                && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                                && event.getAction() == android.view.KeyEvent.ACTION_DOWN);

                if (isSendAction) {

                    String message = editText.getText() == null
                            ? ""
                            : editText.getText().toString().trim();

                    if (!message.isEmpty()) {

                        editText.setText("");

                        OnChatMessageSent(message);
                    }

                    return true;
                }

                return false;
            }
        });
    }

    @SimpleEvent(description = "Déclenché quand l'utilisateur envoie un message depuis le champ écouté par EnableChatInputListener.")
    public void OnChatMessageSent(String message) {
        EventDispatcher.dispatchEvent(this, "OnChatMessageSent", message);
    }

    // =========================================================================
    // 12. BULLE PHOTO DANS LE CHAT
    // =========================================================================

    @SimpleFunction(description = "Ajoute une bulle de chat contenant une photo, avec le même style et positionnement que AddChatBubble.")
    public void AddChatImageBubble(
            final AndroidViewComponent chatContainer,
            final String imagePath,
            final String timeText,
            final String avatarUrl,
            final String senderUid,
            final boolean isMe,
            final int bubbleColor) {

        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {

                    ViewGroup targetLayout = getRealLayout(chatContainer);
                    if (targetLayout == null) return;

                    int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;

                    LinearLayout row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(isMe ? Gravity.END : Gravity.START);

                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    rowParams.setMargins((int) dpToPx(8), (int) dpToPx(4), (int) dpToPx(8), (int) dpToPx(4));
                    row.setLayoutParams(rowParams);

                    int avatarSizePx = (int) dpToPx(32);
                    CardView avatarCard = new CardView(context);
                    LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(avatarSizePx, avatarSizePx);
                    avatarParams.gravity = Gravity.CENTER_VERTICAL;
                    avatarParams.setMargins((int) dpToPx(6), 0, (int) dpToPx(6), 0);
                    avatarCard.setLayoutParams(avatarParams);
                    avatarCard.setRadius(avatarSizePx / 2f);
                    avatarCard.setCardElevation(0f);
                    avatarCard.setMaxCardElevation(0f);
                    avatarCard.setCardBackgroundColor(Color.parseColor("#E0E0E0"));

                    ImageView avatarImg = new ImageView(context);
                    avatarImg.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    avatarImg.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    avatarImg.setPadding((int) dpToPx(4), (int) dpToPx(4), (int) dpToPx(4), (int) dpToPx(4));

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        loadImageAsync(avatarImg, avatarUrl);
                    }

                    avatarCard.addView(avatarImg);
                    avatarCard.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            OnAvatarClick(senderUid, isMe);
                        }
                    });

                    LinearLayout bubble = new LinearLayout(context);
                    bubble.setOrientation(LinearLayout.VERTICAL);
                    bubble.setPadding((int) dpToPx(6), (int) dpToPx(6), (int) dpToPx(6), (int) dpToPx(6));

                    GradientDrawable bg = new GradientDrawable();
                    bg.setShape(GradientDrawable.RECTANGLE);
                    bg.setColor(bubbleColor);
                    bg.setCornerRadius(dpToPx(18));
                    bubble.setBackground(bg);

                    int maxBubbleWidth = (int) (screenWidth * 0.6);

                    LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                            maxBubbleWidth,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    bubble.setLayoutParams(bubbleParams);

                    ImageView photoView = new ImageView(context);

                    LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            (int) dpToPx(180)
                    );

                    photoView.setLayoutParams(photoParams);
                    photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    GradientDrawable photoBg = new GradientDrawable();
                    photoBg.setCornerRadius(dpToPx(12));
                    photoBg.setColor(Color.parseColor("#F0F0F0"));
                    photoView.setBackground(photoBg);
                    photoView.setClipToOutline(true);

                    loadImageAsync(photoView, imagePath);

                    bubble.addView(photoView);

                    if (timeText != null && !timeText.isEmpty()) {
                        TextView timeTv = new TextView(context);
                        timeTv.setText(timeText);
                        timeTv.setTextColor(Color.argb(180, 60, 60, 60));
                        timeTv.setTextSize(10);

                        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        timeParams.gravity = Gravity.END;
                        timeParams.setMargins(0, (int) dpToPx(4), (int) dpToPx(4), 0);
                        timeTv.setLayoutParams(timeParams);

                        if (customTypeface != null) {
                            timeTv.setTypeface(customTypeface);
                        }

                        bubble.addView(timeTv);
                    }

                    if (isMe) {
                        row.addView(bubble);
                        row.addView(avatarCard);
                    } else {
                        row.addView(avatarCard);
                        row.addView(bubble);
                    }

                    targetLayout.addView(row);

                    ScrollToBottom(chatContainer);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // 13. UTILITAIRE JSON
    // =========================================================================

    @SimpleFunction(description = "Extrait la valeur d'un champ texte depuis une chaîne JSON. Retourne une chaîne vide si le champ est absent ou si le JSON est invalide.")
    public String ExtractJsonField(String jsonString, String fieldName) {
        try {
            JSONObject o = new JSONObject(jsonString);
            return o.optString(fieldName, "");
        } catch (Exception e) {
            return "";
        }
    }

    // =========================================================================
    // WEBSOCKET
    // =========================================================================

    @SimpleFunction(
            description = "Ouvre une connexion WebSocket permanente vers le serveur."
    )
    public void ConnectWebSocket(
            final String url) {

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            URI uri =
                                    URI.create(url);

                            String scheme =
                                    uri.getScheme() == null
                                            ? "ws"
                                            : uri.getScheme();

                            boolean secure =
                                    scheme.equalsIgnoreCase(
                                            "wss"
                                    );

                            String host =
                                    uri.getHost();

                            int port =
                                    uri.getPort() != -1
                                            ? uri.getPort()
                                            : (secure
                                            ? 443
                                            : 80);

                            String path =
                                    (
                                            uri.getRawPath() == null ||
                                                    uri.getRawPath().isEmpty()
                                    )
                                            ? "/"
                                            : uri.getRawPath();

                            if (uri.getRawQuery() != null) {

                                path +=
                                        "?" +
                                                uri.getRawQuery();
                            }

                            if (secure) {

                                wsSocket =
                                        SSLSocketFactory
                                                .getDefault()
                                                .createSocket(
                                                        host,
                                                        port
                                                );

                            } else {

                                wsSocket =
                                        new Socket(
                                                host,
                                                port
                                        );
                            }

                            wsOutput =
                                    wsSocket.getOutputStream();

                            InputStream input =
                                    wsSocket.getInputStream();

                            byte[] keyBytes =
                                    new byte[16];

                            new SecureRandom()
                                    .nextBytes(
                                            keyBytes
                                    );

                            String wsKey =
                                    Base64.encodeToString(
                                            keyBytes,
                                            Base64.NO_WRAP
                                    );

                            String request =
                                    "GET " +
                                            path +
                                            " HTTP/1.1\r\n" +
                                            "Host: " +
                                            host +
                                            "\r\n" +
                                            "Upgrade: websocket\r\n" +
                                            "Connection: Upgrade\r\n" +
                                            "Sec-WebSocket-Key: " +
                                            wsKey +
                                            "\r\n" +
                                            "Sec-WebSocket-Version: 13\r\n" +
                                            "\r\n";

                            wsOutput.write(
                                    request.getBytes(
                                            "UTF-8"
                                    )
                            );

                            wsOutput.flush();

                            boolean handshakeOk =
                                    false;

                            String line;
                            boolean firstLine = true;

                            while (
                                    (line =
                                            wsReadLine(
                                                    input
                                            )) != null &&
                                            !line.isEmpty()
                            ) {

                                if (firstLine) {

                                    if (line.contains("101")) {
                                        handshakeOk = true;
                                    }

                                    firstLine = false;
                                }
                            }

                            if (!handshakeOk) {

                                activity.runOnUiThread(
                                        new Runnable() {

                                            @Override
                                            public void run() {

                                                OnError(
                                                        "ConnectWebSocket: handshake refusé par le serveur."
                                                );
                                            }
                                        }
                                );

                                return;
                            }

                            wsRunning = true;

                            activity.runOnUiThread(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            OnWebSocketConnected();
                                        }
                                    }
                            );

                            wsReadLoop(input);

                        } catch (
                                final Exception e) {

                            activity.runOnUiThread(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            OnError(
                                                    "ConnectWebSocket: " +
                                                            e.getMessage()
                                            );
                                        }
                                    }
                            );
                        }
                    }
                }
        );
    }

    private String wsReadLine(
            InputStream input)
            throws IOException {

        StringBuilder sb =
                new StringBuilder();

        int b;

        while (
                (b = input.read()) != -1
        ) {

            if (b == '\r')
                continue;

            if (b == '\n')
                break;

            sb.append(
                    (char) b
            );
        }

        return sb.toString();
    }

    private void wsReadLoop(
            InputStream input) {

        try {

            while (wsRunning) {

                int b0 =
                        input.read();

                if (b0 == -1)
                    break;

                int b1 =
                        input.read();

                if (b1 == -1)
                    break;

                int opcode =
                        b0 & 0x0F;

                long payloadLen =
                        b1 & 0x7F;

                if (payloadLen == 126) {

                    payloadLen =
                            ((input.read() & 0xFF) << 8) |
                                    (input.read() & 0xFF);

                } else if (payloadLen == 127) {

                    payloadLen = 0;

                    for (int i = 0; i < 8; i++) {

                        payloadLen =
                                (payloadLen << 8) |
                                        (input.read() & 0xFF);
                    }
                }

                byte[] payload =
                        new byte[(int) payloadLen];

                int readTotal = 0;

                while (
                        readTotal <
                                payload.length
                ) {

                    int r =
                            input.read(
                                    payload,
                                    readTotal,
                                    payload.length -
                                            readTotal
                            );

                    if (r == -1)
                        break;

                    readTotal += r;
                }

                if (opcode == 0x8) {

                    wsRunning = false;
                    break;

                } else if (opcode == 0x9) {

                    wsSendFrame(
                            0xA,
                            payload
                    );

                } else if (
                        opcode == 0x1 ||
                                opcode == 0x0
                ) {

                    final String message =
                            new String(
                                    payload,
                                    "UTF-8"
                            );

                    activity.runOnUiThread(
                            new Runnable() {

                                @Override
                                public void run() {

                                    OnWebSocketMessageReceived(
                                            message
                                    );
                                }
                            }
                    );
                }
            }

        } catch (Exception e) {

            if (wsRunning) {

                activity.runOnUiThread(
                        new Runnable() {

                            @Override
                            public void run() {

                                OnError(
                                        "WebSocket lecture: erreur de connexion."
                                );
                            }
                        }
                );
            }

        } finally {

            wsRunning = false;

            activity.runOnUiThread(
                    new Runnable() {

                        @Override
                        public void run() {

                            OnWebSocketDisconnected();
                        }
                    }
            );
        }
    }

    private synchronized void wsSendFrame(
            int opcode,
            byte[] payload) {

        try {

            if (wsOutput == null)
                return;

            byte[] mask =
                    new byte[4];

            new SecureRandom()
                    .nextBytes(mask);

            byte[] masked =
                    new byte[payload.length];

            for (
                    int i = 0;
                    i < payload.length;
                    i++
            ) {

                masked[i] =
                        (byte)
                                (
                                        payload[i] ^
                                                mask[i % 4]
                                );
            }

            java.io.ByteArrayOutputStream frame =
                    new java.io.ByteArrayOutputStream();

            frame.write(
                    0x80 | opcode
            );

            int len =
                    payload.length;

            if (len <= 125) {

                frame.write(
                        0x80 | len
                );

            } else if (len <= 65535) {

                frame.write(
                        0x80 | 126
                );

                frame.write(
                        (len >> 8) & 0xFF
                );

                frame.write(
                        len & 0xFF
                );

            } else {

                frame.write(
                        0x80 | 127
                );

                for (
                        int i = 7;
                        i >= 0;
                        i--
                ) {

                    frame.write(
                            (int)
                                    (
                                            (len >>
                                                    (8 * i)) &
                                                    0xFF
                                    )
                    );
                }
            }

            frame.write(mask);
            frame.write(masked);

            wsOutput.write(
                    frame.toByteArray()
            );

            wsOutput.flush();

        } catch (Exception e) {

            activity.runOnUiThread(
                    new Runnable() {

                        @Override
                        public void run() {

                            OnError(
                                    "Envoi WebSocket échoué."
                            );
                        }
                    }
            );
        }
    }

    @SimpleFunction(
            description = "Envoie des données JSON au serveur via la connexion WebSocket ouverte."
    )
    public void SendWebSocketMessage(
            final String json) {

        if (!wsRunning) {

            OnError(
                    "SendWebSocketMessage: connexion WebSocket fermée."
            );

            return;
        }

        try {

            wsSendFrame(
                    0x1,
                    json.getBytes("UTF-8")
            );

        } catch (Exception e) {

            OnError(
                    "SendWebSocketMessage: " +
                            e.getMessage()
            );
        }
    }

    @SimpleFunction(
            description = "Ferme la connexion WebSocket."
    )
    public void DisconnectWebSocket() {

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            wsRunning = false;

                            if (wsSocket != null) {

                                wsSocket.close();
                                wsSocket = null;
                            }

                        } catch (Exception e) {
                            // silencieux
                        }
                    }
                }
        );
    }

    // =========================================================================
    // 8. REQUÊTES SERVEUR
    // =========================================================================

    @SimpleFunction(
            description = "Envoie une requête HTTPS au serveur."
    )
    public void CallServerRequest(
            final String endpointUrl,
            final String method,
            final String headersJson,
            final String bodyJson) {

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        HttpURLConnection conn =
                                null;

                        try {

                            URL url =
                                    new URL(
                                            endpointUrl
                                    );

                            conn =
                                    (HttpURLConnection)
                                            url.openConnection();

                            conn.setConnectTimeout(
                                    15000
                            );

                            conn.setReadTimeout(
                                    15000
                            );

                            conn.setRequestMethod(
                                    "POST".equalsIgnoreCase(
                                            method
                                    )
                                            ? "POST"
                                            : "GET"
                            );

                            if (headersJson != null &&
                                    !headersJson.isEmpty()) {

                                JSONObject headers =
                                        new JSONObject(
                                                headersJson
                                        );

                                Iterator<String> keys =
                                        headers.keys();

                                while (keys.hasNext()) {

                                    String key =
                                            keys.next();

                                    conn.setRequestProperty(
                                            key,
                                            headers.getString(
                                                    key
                                            )
                                    );
                                }
                            }

                            if ("POST".equalsIgnoreCase(
                                    method
                            )) {

                                conn.setDoOutput(true);

                                conn.setRequestProperty(
                                        "Content-Type",
                                        "application/json; charset=utf-8"
                                );

                                if (bodyJson != null) {

                                    OutputStream os =
                                            conn.getOutputStream();

                                    os.write(
                                            bodyJson.getBytes(
                                                    "UTF-8"
                                            )
                                    );

                                    os.flush();
                                    os.close();
                                }
                            }

                            final int responseCode =
                                    conn.getResponseCode();

                            InputStream is =
                                    (responseCode >= 200 &&
                                            responseCode < 400)
                                            ? conn.getInputStream()
                                            : conn.getErrorStream();

                            final String responseContent =
                                    lireFlux(is);

                            activity.runOnUiThread(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            OnServerResponse(
                                                    responseCode,
                                                    responseContent
                                            );
                                        }
                                    }
                            );

                        } catch (
                                final Exception e) {

                            activity.runOnUiThread(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            OnServerResponse(
                                                    500,
                                                    e.getMessage()
                                            );
                                        }
                                    }
                            );

                        } finally {

                            if (conn != null)
                                conn.disconnect();
                        }
                    }
                }
        );
    }

    private String lireFlux(
            InputStream is)
            throws IOException {

        if (is == null)
            return "";

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                is,
                                "UTF-8"
                        )
                );

        StringBuilder sb =
                new StringBuilder();

        String ligne;

        while (
                (ligne =
                        reader.readLine()) != null
        ) {

            sb.append(ligne);
        }

        reader.close();

        return sb.toString();
    }

    // =========================================================================
    // 9. ÉVÉNEMENTS KODULAR
    // =========================================================================

    @SimpleEvent(
            description = "Déclenché quand l'utilisateur touche une icône de la barre de navigation."
    )
    public void OnSelected(String id) {

        EventDispatcher.dispatchEvent(
                this,
                "OnSelected",
                id
        );
    }

    @SimpleEvent(
            description = "Déclenché en cas de problème avec la barre de navigation."
    )
    public void NavBarError(String message) {

        EventDispatcher.dispatchEvent(
                this,
                "NavBarError",
                message
        );
    }

    @SimpleEvent(
            description = "Déclenché lors du clic sur une carte produit. Renvoie l'UID et le JSON complet du produit."
    )
    public void OnProductCardClick(
            String productUid,
            String productJson) {

        EventDispatcher.dispatchEvent(
                this,
                "OnProductCardClick",
                productUid,
                productJson
        );
    }

    @SimpleEvent(
            description = "Déclenché lorsque tous les détails du produit ont été récupérés."
    )
    public void OnProductDetailsReceived(
            String productUid,
            String description,
            int stock,
            String image2,
            String image3,
            String image4,
            String image5,
            boolean deliveryIncluded) {

        EventDispatcher.dispatchEvent(
                this,
                "OnProductDetailsReceived",
                productUid,
                description,
                stock,
                image2,
                image3,
                image4,
                image5,
                deliveryIncluded
        );
    }

    @SimpleEvent(
            description = "Déclenché lorsqu'une erreur survient pendant la récupération des détails produit."
    )
    public void OnProductDetailsError(
            String message) {

        EventDispatcher.dispatchEvent(
                this,
                "OnProductDetailsError",
                message
        );
    }

    @SimpleEvent(
            description = "Déclenché lors du choix d'une catégorie. Renvoie l'ID et le Nom."
    )
    public void OnCategorySelected(
            String categoryId,
            String categoryTitle) {

        EventDispatcher.dispatchEvent(
                this,
                "OnCategorySelected",
                categoryId,
                categoryTitle
        );
    }

    @SimpleEvent(
            description = "Déclenché lors du clic sur l'avatar du message."
    )
    public void OnAvatarClick(
            String senderUid,
            boolean isMe) {

        EventDispatcher.dispatchEvent(
                this,
                "OnAvatarClick",
                senderUid,
                isMe
        );
    }

    @SimpleEvent(
            description = "Déclenché après sélection d'une image."
    )
    public void OnPhotoPicked(
            String imageUri) {

        EventDispatcher.dispatchEvent(
                this,
                "OnPhotoPicked",
                imageUri
        );
    }

    @SimpleEvent(
            description = "Déclenché après réponse du serveur."
    )
    public void OnServerResponse(
            int responseCode,
            String responseContent) {

        EventDispatcher.dispatchEvent(
                this,
                "OnServerResponse",
                responseCode,
                responseContent
        );
    }

    @SimpleEvent(
            description = "Déclenché en cas de problème."
    )
    public void OnError(String message) {

        EventDispatcher.dispatchEvent(
                this,
                "OnError",
                message
        );
    }

    @SimpleEvent(
            description = "Déclenché quand la connexion WebSocket est établie avec le serveur."
    )
    public void OnWebSocketConnected() {

        EventDispatcher.dispatchEvent(
                this,
                "OnWebSocketConnected"
        );
    }

    @SimpleEvent(
            description = "Déclenché quand la connexion WebSocket est fermée."
    )
    public void OnWebSocketDisconnected() {

        EventDispatcher.dispatchEvent(
                this,
                "OnWebSocketDisconnected"
        );
    }

    @SimpleEvent(
            description = "Déclenché à chaque réception d'un message JSON poussé par le serveur via WebSocket."
    )
    public void OnWebSocketMessageReceived(
            String json) {

        EventDispatcher.dispatchEvent(
                this,
                "OnWebSocketMessageReceived",
                json
        );
    }
}

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

import android.content.SharedPreferences;
import android.os.Environment;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.PopupWindow;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;

import android.content.ClipData;
import android.graphics.Outline;
import android.text.InputFilter;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.HorizontalScrollView;
import java.io.DataOutputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.text.InputType;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListPopupWindow;

import android.content.ContentValues;
import android.provider.MediaStore;
import android.view.MotionEvent;

import android.app.AlertDialog;
import java.math.RoundingMode;
import java.text.Normalizer;

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
        version = 17,
        description = "Extension ManaplaceUtils - corrections: clavier flottant, auto-grow, galerie/permissions, barre de navigation, réutilisation multiple sans conflit parent/dialogue + détails produits + badges génériques + page boutique vendeur/acheteur (header logo, barre de statut, menu infos, compteur, chargement serveur, suppression produit) + pages Modifier la boutique, Prix (HTG / USD, livraison, total) et État et variantes (dialogue, couleur avec pastilles, taille) + page détails produit (images défilantes, prix, stock et livraison, quantité, vendeur, partage) + page « Créer ma boutique » (champs à libellé animé, vérification du nom, catégories, logo, envoi, statut vendeur) + formulaire « Ajouter un produit » (multimédia 5 photos, titre, lignes cliquables, stock, validation, brouillon, envoi multipart).",
        category = ComponentCategory.EXTENSION,
        nonVisible = true
)
@SimpleObject(external = true)
@UsesPermissions(
        permissionNames =
                "android.permission.READ_EXTERNAL_STORAGE," +
                "android.permission.READ_MEDIA_IMAGES," +
                "android.permission.INTERNET," +
                "android.permission.POST_NOTIFICATIONS"
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
    private final java.util.Map<String, View> ongletArrangements = new java.util.HashMap<>();

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
        this.productPickRequestCode = this.form.registerForActivityResult(this);
        this.shopLogoRequestCode = this.form.registerForActivityResult(this);
        this.editLogoRequestCode = this.form.registerForActivityResult(this);
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
                    ongletArrangements.clear();
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

    @SimpleFunction(description = "Lie un id de la barre de navigation à l'arrangement qu'il doit afficher. Appelle une fois par onglet (après NavBarAdd, avant ou après NavBarInitialize). Une fois lié, l'extension affiche/masque automatiquement le bon arrangement à chaque clic ou sélection d'onglet.")
    public void NavBarLinkArrangement(final String id, final AndroidViewComponent arrangement) {
        if (arrangement == null) {
            NavBarError("NavBarLinkArrangement: arrangement invalide pour id " + id);
            return;
        }
        View v = arrangement.getView();
        if (v == null) {
            NavBarError("NavBarLinkArrangement: vue introuvable pour id " + id);
            return;
        }
        ongletArrangements.put(id, v);
    }

    @SimpleFunction(description = "Restaure le dernier onglet actif sauvegardé (affiche le bon arrangement et surligne la bonne icône). À appeler dans Screen.Initialize, après NavBarInitialize et NavBarLinkArrangement pour chaque onglet. defaultId est utilisé si aucun onglet n'a encore été sauvegardé.")
    public void NavBarRestoreLastTab(final String defaultId) {
        try {
            String idSauvegarde = chatPrefs().getString("navbar_dernier_id", defaultId);

            if (idsFinaux.indexOf(idSauvegarde) < 0) {
                idSauvegarde = defaultId;
            }

            idSelectionne = null;
            NavBarSelect(idSauvegarde);

        } catch (Exception e) {
            NavBarError("NavBarRestoreLastTab: " + e.getMessage());
        }
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

            afficherArrangementPourId(id);
            chatPrefs().edit().putString("navbar_dernier_id", id).apply();

            OnSelected(id);

        } catch (Exception e) {

            NavBarError(
                    "Erreur de sélection: " +
                            e.getMessage()
            );
        }
    }

    private void afficherArrangementPourId(String id) {
        if (ongletArrangements.isEmpty()) return;
        for (java.util.Map.Entry<String, View> entree : ongletArrangements.entrySet()) {
            View v = entree.getValue();
            if (v == null) continue;
            v.setVisibility(entree.getKey().equals(id) ? View.VISIBLE : View.GONE);
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
            description = "Charge une police personnalisée .ttf ou .otf (fichier des Assets, chemin complet ou dossier Companion). Elle est aussi utilisée par la page boutique et par le formulaire « Ajouter un produit » (mis à jour s'il est déjà affiché) ; sans police chargée, la page boutique utilise Manrope-Medium.ttf si ce fichier est dans les Assets. À appeler avant BuildShopHome."
    )
    public void LoadCustomFont(String fontPath) {

        if (fontPath == null || fontPath.trim().isEmpty()) {
            customTypeface = Typeface.DEFAULT;
            applyFonts();
            return;
        }

        Typeface t = tryLoadTypeface(fontPath.trim());

        if (t == null) {
            customTypeface = Typeface.DEFAULT;
            OnError("LoadCustomFont: police introuvable (" + fontPath
                    + "). Mets le fichier dans les Assets.");
        } else {
            customTypeface = t;
        }

        applyFonts();
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

                            attachMessageExtras(targetLayout, row, bubble, null, messageText, "", isMe,
                                    Color.argb(180, Color.red(textColor), Color.green(textColor), Color.blue(textColor)));
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

                                                    final String uid = item.optString("uid", item.optString("id", String.valueOf(i)));

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

                                                    String priceStr = item.has("price_display")
                                                            ? item.optString("price_display", "")
                                                            : item.optString("price", "");

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

                                                OnError("BuildProductGridFromJson: " + e.getMessage());
                                            }
                                        }
                                    }
                            );

                        } catch (Exception e) {

                            final String msgGrille = "BuildProductGridFromJson: JSON invalide ou illisible (" + e.getMessage() + ")";
                            runOnUi(new Runnable() {
                                @Override
                                public void run() {
                                    OnError(msgGrille);
                                }
                            });
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

        if (receivedRequestCode == productPickRequestCode) {
            productPhotosResult(receivedRequestCode, resultCode, data);
            return;
        }

        if (receivedRequestCode == shopLogoRequestCode) {
            shopLogoResult(receivedRequestCode, resultCode, data);
            return;
        }

        if (receivedRequestCode == editLogoRequestCode) {
            shopEditLogoResult(receivedRequestCode, resultCode, data);
            return;
        }

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

    @SimpleFunction(description = "Affiche un petit message flottant temporaire (pastille sombre arrondie, icône Phosphor facultative + texte). Sa taille s'adapte à la longueur du texte. fontPath : fichier de police Phosphor (ex: Phosphor-Bold.ttf) ; mets \"\" pour n'afficher aucune icône. unicodeChar : le caractère de l'icône. iconColor : couleur de l'icône. position : \"top\" ou \"bottom\". offsetPercent : distance par rapport au bord choisi, en % de la hauteur de l'écran (0 = au bord ; en bas, monte-le pour qu'il ne soit pas caché par la barre de navigation, ex: 12). durationMs : durée d'affichage (0 = reste jusqu'à DismissNotifier). Un seul notifier à la fois : un nouveau remplace l'ancien.")
    public void ShowNotifier(
            final String fontPath,
            final String unicodeChar,
            final int iconColor,
            final String message,
            final String position,
            final float offsetPercent,
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

                    android.util.DisplayMetrics dm = activity.getResources().getDisplayMetrics();
                    float offset = Math.max(0f, Math.min(offsetPercent, 80f));
                    int offsetPx = Math.round(dm.heightPixels * (offset / 100f));
                    boolean isTop = "top".equalsIgnoreCase(position);

                    LinearLayout notifier = new LinearLayout(activity);
                    notifier.setOrientation(LinearLayout.HORIZONTAL);
                    notifier.setGravity(Gravity.CENTER_VERTICAL);
                    notifier.setPadding(
                            (int) dpToPx(16),
                            (int) dpToPx(10),
                            (int) dpToPx(16),
                            (int) dpToPx(10)
                    );

                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.argb(0xED, 0x1A, 0x1A, 0x1B));
                    bg.setCornerRadius(dpToPx(24));
                    notifier.setBackground(bg);

                    int iconWidthPx = 0;
                    if (fontPath != null && !fontPath.trim().isEmpty()
                            && unicodeChar != null && !unicodeChar.isEmpty()) {
                        Typeface iconFont = null;
                        try {
                            if (fontPath.startsWith("/")) {
                                iconFont = Typeface.createFromFile(new File(fontPath));
                            } else {
                                iconFont = Typeface.createFromAsset(context.getAssets(), fontPath);
                            }
                        } catch (Exception ignored) {
                        }
                        if (iconFont == null) {
                            OnError("ShowNotifier: police introuvable (" + fontPath + ").");
                        } else {
                            TextView iconView = new TextView(activity);
                            iconView.setText(unicodeChar);
                            iconView.setTypeface(iconFont);
                            iconView.setTextColor(iconColor);
                            iconView.setTextSize(22);
                            iconView.setIncludeFontPadding(false);
                            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            );
                            iconParams.setMargins(0, 0, (int) dpToPx(10), 0);
                            iconView.setLayoutParams(iconParams);
                            notifier.addView(iconView);
                            iconWidthPx = (int) dpToPx(34);
                        }
                    }

                    TextView textView = new TextView(activity);
                    textView.setText(message);
                    textView.setTextColor(Color.WHITE);
                    textView.setTextSize(14);
                    if (customTypeface != null) {
                        textView.setTypeface(customTypeface);
                    }
                    int maxTextWidth = (int) (dm.widthPixels * 0.9f) - (int) dpToPx(32) - iconWidthPx;
                    textView.setMaxWidth(Math.max(maxTextWidth, (int) dpToPx(80)));
                    notifier.addView(textView);

                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    params.gravity = (isTop ? Gravity.TOP : Gravity.BOTTOM)
                            | Gravity.CENTER_HORIZONTAL;
                    int base = (int) dpToPx(16);
                    if (isTop) {
                        params.setMargins(0, base + offsetPx, 0, 0);
                    } else {
                        params.setMargins(0, 0, 0, base + offsetPx);
                    }

                    notifier.setAlpha(0f);
                    root.addView(notifier, params);
                    notifier.animate().alpha(1f).setDuration(180).start();
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
                    OnError("ShowNotifier: " + e.getMessage());
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

    // =========================================================================
    // 12bis. PERSISTANCE DES CARTES DE CONVERSATION (partage entre écrans, ex: Inbox -> Home)
    // =========================================================================

    @SimpleFunction(description = "Sauvegarde (ou met à jour) les infos d'une carte de conversation pour qu'elles soient disponibles sur un autre écran (ex: depuis Inbox vers Home) via LoadAllConversationCards.")
    public void SaveConversationSummary(
            String conversationUid,
            String productImage,
            String productTitle,
            String lastMessage,
            boolean isNewMessage,
            String dateText) {
        try {
            // Si l'image ou le titre sont vides, on garde ceux déjà sauvegardés pour cette conversation.
            String imageFinale = productImage == null ? "" : productImage;
            String titreFinal = productTitle == null ? "" : productTitle;
            if (imageFinale.isEmpty() || titreFinal.isEmpty()) {
                String ancien = chatPrefs().getString("conv_" + conversationUid, null);
                if (ancien != null) {
                    JSONObject anciennesInfos = new JSONObject(ancien);
                    if (imageFinale.isEmpty()) imageFinale = anciennesInfos.optString("productImage", "");
                    if (titreFinal.isEmpty()) titreFinal = anciennesInfos.optString("productTitle", "");
                }
            }
            JSONObject obj = new JSONObject();
            obj.put("productImage", imageFinale);
            obj.put("productTitle", titreFinal);
            obj.put("lastMessage", lastMessage == null ? "" : lastMessage);
            obj.put("isNewMessage", isNewMessage);
            obj.put("dateText", dateText == null ? "" : dateText);

            android.content.SharedPreferences.Editor editor = chatPrefs().edit();
            editor.putString("conv_" + conversationUid, obj.toString());

            String listeActuelle = chatPrefs().getString("conv_uid_list", "[]");
            JSONArray anciensUid = new JSONArray(listeActuelle);
            JSONArray nouvelleListe = new JSONArray();
            nouvelleListe.put(conversationUid);
            for (int i = 0; i < anciensUid.length(); i++) {
                String uid = anciensUid.getString(i);
                if (!uid.equals(conversationUid)) {
                    nouvelleListe.put(uid);
                }
            }
            editor.putString("conv_uid_list", nouvelleListe.toString());
            editor.apply();

            if (chatSyncActive()) {
                JSONObject corps = new JSONObject();
                corps.put("action", "save_summary");
                corps.put("userUid", chatPrefs().getString(SYNC_KEY_UID, ""));
                corps.put("conversationUid", conversationUid);
                corps.put("summary", obj);
                pushToServer(corps.toString());
            }

        } catch (Exception e) {
            OnError("SaveConversationSummary: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Recharge toutes les cartes de conversation sauvegardées avec SaveConversationSummary et les affiche dans listContainer, de la plus récente à la plus ancienne.")
    public void LoadAllConversationCards(final AndroidViewComponent listContainer) {
        try {
            String listeActuelle = chatPrefs().getString("conv_uid_list", "[]");
            JSONArray uidList = new JSONArray(listeActuelle);

            for (int i = 0; i < uidList.length(); i++) {
                String uid = uidList.getString(i);
                String dataJson = chatPrefs().getString("conv_" + uid, null);
                if (dataJson == null) {
                    continue;
                }

                JSONObject obj = new JSONObject(dataJson);

                AddOrUpdateConversationCard(
                        listContainer,
                        uid,
                        obj.optString("productImage", ""),
                        obj.optString("productTitle", ""),
                        obj.optString("lastMessage", ""),
                        obj.optBoolean("isNewMessage", false),
                        obj.optString("dateText", "")
                );
            }
        } catch (Exception e) {
            OnError("LoadAllConversationCards: " + e.getMessage());
        }
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
        purgeConversationData(conversationUid);
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

                    final String imagePathPourAgrandissement = imagePath;
                    photoView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openFullscreenImage(imagePathPourAgrandissement);
                        }
                    });

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

                    attachMessageExtras(targetLayout, row, bubble, photoView, "", imagePath, isMe, Color.argb(180, 60, 60, 60));
                    targetLayout.addView(row);

                    ScrollToBottom(chatContainer);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // 12ter. AFFICHAGE IMAGE EN PLEIN ÉCRAN (clic sur une bulle image du chat)
    // =========================================================================

    private void openFullscreenImage(final String imagePath) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    FrameLayout root = (FrameLayout) activity.findViewById(android.R.id.content);
                    if (root == null) return;

                    final FrameLayout overlay = new FrameLayout(activity);
                    overlay.setBackgroundColor(Color.BLACK);
                    overlay.setLayoutParams(new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));

                    ImageView fullImage = new ImageView(activity);
                    fullImage.setLayoutParams(new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    fullImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    loadImageAsync(fullImage, imagePath);

                    overlay.addView(fullImage);

                    TextView closeBtn = new TextView(activity);
                    closeBtn.setText("\u2715");
                    closeBtn.setTextColor(Color.WHITE);
                    closeBtn.setTextSize(22);
                    closeBtn.setPadding((int) dpToPx(14), (int) dpToPx(14), (int) dpToPx(14), (int) dpToPx(14));

                    FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    closeParams.gravity = Gravity.TOP | Gravity.END;
                    closeParams.setMargins(0, (int) dpToPx(28), (int) dpToPx(12), 0);
                    closeBtn.setLayoutParams(closeParams);

                    overlay.addView(closeBtn);

                    View.OnClickListener fermer = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (overlay.getParent() != null) {
                                ((ViewGroup) overlay.getParent()).removeView(overlay);
                            }
                        }
                    };

                    overlay.setOnClickListener(fermer);
                    closeBtn.setOnClickListener(fermer);

                    root.addView(overlay);

                } catch (Exception e) {
                    OnError("openFullscreenImage: " + e.getMessage());
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
    // 14. HISTORIQUE DE CHAT (texte + image, stockage interne)
    // =========================================================================

    private static final String CHAT_PREFS = "ManaplaceChatHistory";

    private android.content.SharedPreferences chatPrefs() {
        return context.getSharedPreferences(CHAT_PREFS, Context.MODE_PRIVATE);
    }

    // Retourne le libellé du séparateur de date pour un instant donné :
    // "Aujourd'hui", "Hier", ou "d MMM" (ex: "1 oct.") sinon.
    private String formatDateLabel(long millis) {
        java.util.Calendar calMessage = java.util.Calendar.getInstance();
        calMessage.setTimeInMillis(millis);

        java.util.Calendar calAujourdhui = java.util.Calendar.getInstance();

        java.util.Calendar calHier = (java.util.Calendar) calAujourdhui.clone();
        calHier.add(java.util.Calendar.DAY_OF_YEAR, -1);

        boolean estAujourdhui =
                calAujourdhui.get(java.util.Calendar.YEAR) == calMessage.get(java.util.Calendar.YEAR)
                        && calAujourdhui.get(java.util.Calendar.DAY_OF_YEAR) == calMessage.get(java.util.Calendar.DAY_OF_YEAR);

        boolean estHier =
                calHier.get(java.util.Calendar.YEAR) == calMessage.get(java.util.Calendar.YEAR)
                        && calHier.get(java.util.Calendar.DAY_OF_YEAR) == calMessage.get(java.util.Calendar.DAY_OF_YEAR);

        if (estAujourdhui) {
            return "Aujourd'hui";
        } else if (estHier) {
            return "Hier";
        } else {
            boolean memeAnnee = calAujourdhui.get(java.util.Calendar.YEAR) == calMessage.get(java.util.Calendar.YEAR);
            String format = memeAnnee ? "d MMM" : "d MMM yyyy";
            return new java.text.SimpleDateFormat(format, java.util.Locale.FRENCH).format(new java.util.Date(millis));
        }
    }

    // Insère une petite étiquette de date centrée (ex: "Hier") dans la conversation,
    // au-dessus du premier message d'un nouveau jour.
    private void addDateDividerInternal(final AndroidViewComponent chatContainer, final String label) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup targetLayout = getRealLayout(chatContainer);
                    if (targetLayout == null) return;

                    LinearLayout row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER);

                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    rowParams.setMargins(0, (int) dpToPx(12), 0, (int) dpToPx(8));
                    row.setLayoutParams(rowParams);

                    TextView labelView = new TextView(context);
                    labelView.setText(label);
                    labelView.setTextColor(Color.parseColor("#9E9E9E"));
                    labelView.setTextSize(12);
                    labelView.setTypeface(
                            customTypeface != null ? customTypeface : labelView.getTypeface(),
                            android.graphics.Typeface.BOLD
                    );

                    row.addView(labelView);
                    row.setTag("date_divider");
                    targetLayout.addView(row);

                } catch (Exception e) {
                    OnError("addDateDivider: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Ajoute un message (texte OU image, laisser l'autre vide) à l'historique de la conversation donnée, et le sauvegarde automatiquement.")
    public void SaveChatMessage(String conversationTag, String text, String imagePath, boolean isMe) {
        try {
            String key = "hist_" + conversationTag;
            String jsonActuel = chatPrefs().getString(key, "[]");
            JSONArray liste = new JSONArray(jsonActuel);

            long maintenant = System.currentTimeMillis();

            JSONObject nouveau = new JSONObject();
            nouveau.put("text", text == null ? "" : text);
            nouveau.put("image", imagePath == null ? "" : imagePath);
            nouveau.put("time", new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(maintenant)));
            nouveau.put("timestamp", maintenant);
            nouveau.put("isMe", isMe);

            liste.put(nouveau);

            chatPrefs().edit().putString(key, liste.toString()).apply();

            if (chatSyncActive()) {
                pushToServer(buildSaveMessageBody(conversationTag, nouveau));
            }

        } catch (Exception e) {
            OnError("SaveChatMessage: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Recharge tout l'historique d'une conversation et affiche directement toutes les bulles (texte et image) dans chatContainer, dans l'ordre chronologique.")
    public void LoadChatHistory(
            final AndroidViewComponent chatContainer,
            final String conversationTag,
            final String myAvatarUrl,
            final String otherAvatarUrl,
            final String myUid,
            final int bubbleColorMe,
            final int textColorMe,
            final int bubbleColorOther,
            final int textColorOther) {
        try {
            String key = "hist_" + conversationTag;
            String jsonActuel = chatPrefs().getString(key, "[]");
            JSONArray liste = new JSONArray(jsonActuel);

            int dernierJourAffiche = -1;
            long dernierMsgMoi = -1;

            for (int i = 0; i < liste.length(); i++) {
                JSONObject item = liste.getJSONObject(i);
                String text = item.optString("text", "");
                String image = item.optString("image", "");
                long timestamp = item.optLong("timestamp", -1);
                String time = (timestamp > 0)
                        ? new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(timestamp))
                        : item.optString("time", "");
                boolean isMe = item.optBoolean("isMe", false);
                if (isMe && timestamp > dernierMsgMoi) dernierMsgMoi = timestamp;

                if (timestamp > 0) {
                    java.util.Calendar calMsg = java.util.Calendar.getInstance();
                    calMsg.setTimeInMillis(timestamp);
                    int jourCle = calMsg.get(java.util.Calendar.YEAR) * 1000 + calMsg.get(java.util.Calendar.DAY_OF_YEAR);

                    if (jourCle != dernierJourAffiche) {
                        addDateDividerInternal(chatContainer, formatDateLabel(timestamp));
                        dernierJourAffiche = jourCle;
                    }
                }

                String avatar = isMe ? myAvatarUrl : otherAvatarUrl;
                String uid = isMe ? myUid : conversationTag;
                int bColor = isMe ? bubbleColorMe : bubbleColorOther;
                int tColor = isMe ? textColorMe : textColorOther;

                if (!text.isEmpty()) {
                    AddChatBubble(chatContainer, text, time, avatar, uid, isMe, bColor, tColor);
                } else if (!image.isEmpty()) {
                    AddChatImageBubble(chatContainer, image, time, avatar, uid, isMe, bColor);
                }
            }

            appliquerEtatVu(chatContainer, conversationTag, dernierMsgMoi);
        } catch (Exception e) {
            OnError("LoadChatHistory: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Efface tout l'historique sauvegardé d'une conversation donnée.")
    public void ClearChatHistory(String conversationTag) {
        chatPrefs().edit().remove("hist_" + conversationTag).apply();
    }

    // =========================================================================
    // 12quinquies. CARTE DE PANIER (photo + titre + prix + quantité)
    // =========================================================================

    private String selectedCartProductUid = "";

    @SimpleEvent(description = "L'utilisateur a touché une carte du panier. Utilise productUid pour ouvrir l'écran Détail Produit.")
    public void OnCartCardDetailsClick(String productUid) {
        EventDispatcher.dispatchEvent(this, "OnCartCardDetailsClick", productUid);
    }

    @SimpleEvent(description = "Appui long sur une carte du panier (pour proposer la suppression). L'uid est aussi disponible avec GetSelectedCartProductUid.")
    public void OnCartCardLongClick(String productUid) {
        EventDispatcher.dispatchEvent(this, "OnCartCardLongClick", productUid);
    }

    @SimpleFunction(description = "Retourne l'uid du produit de la dernière carte du panier touchée (clic ou appui long). Pratique après une boîte de confirmation, sans variable globale.")
    public String GetSelectedCartProductUid() {
        return selectedCartProductUid;
    }

    @SimpleFunction(description = "Construit une carte par produit du panier (photo, titre, prix, quantité « Qnt. N ») dans le conteneur. cartJson : résultat de Ecommerce.GetCartJson. Un clic sur la carte déclenche OnCartCardDetailsClick, un appui long déclenche OnCartCardLongClick. Ce bloc vide d'abord le conteneur : rappelle-le après chaque changement du panier.")
    public void BuildCartFromJson(
            final AndroidViewComponent listContainer,
            final String cartJson) {
        if (listContainer == null || listContainer.getView() == null) {
            OnError("BuildCartFromJson: conteneur invalide.");
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup target = getRealLayout(listContainer);
                    if (target == null) {
                        target = (ViewGroup) listContainer.getView();
                    }
                    target.removeAllViews();

                    JSONArray items = new JSONArray(
                            cartJson == null || cartJson.trim().isEmpty() ? "[]" : cartJson);

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) continue;
                        String uid = item.optString("product_uid",
                                item.optString("uid", item.optString("id", "")));
                        if (uid.isEmpty()) continue;
                        String price = item.has("price_display")
                                ? item.optString("price_display", "")
                                : item.optString("price", "");
                        target.addView(buildCartCard(
                                uid,
                                firstImageOf(item),
                                item.optString("title", ""),
                                price,
                                item.optInt("quantity", 0)));
                    }
                } catch (Exception e) {
                    OnError("BuildCartFromJson: " + e.getMessage());
                }
            }
        });
    }

    // Une seule image, même si le serveur envoie une liste.
    private String firstImageOf(JSONObject item) {
        JSONArray arr = item.optJSONArray("images");
        if (arr != null && arr.length() > 0 && !arr.optString(0, "").isEmpty()) {
            return arr.optString(0, "");
        }
        Object o = item.opt("image");
        if (o instanceof JSONArray) {
            return ((JSONArray) o).optString(0, "");
        }
        String t = item.optString("image", "").trim();
        if (t.startsWith("[")) {
            try {
                JSONArray a = new JSONArray(t);
                if (a.length() > 0) return a.optString(0, "");
            } catch (Exception ignored) {
            }
        }
        if (!t.startsWith("data:")) {
            String low = t.toLowerCase(java.util.Locale.US);
            int i = low.indexOf(",http");
            if (i < 0) i = low.indexOf(", http");
            if (i > 0) return t.substring(0, i).trim();
        }
        return t;
    }

    private CardView buildCartCard(
            final String productUid,
            String image,
            String title,
            String price,
            int quantity) {

        // Réglages du design (tailles en sp / dp)
        final int titleSizeSp = 20;
        final int priceSizeSp = 16;
        final int qtySizeSp = 16;
        int textBlack = Color.parseColor("#1A1A1B");

        // Carte : toute la largeur du conteneur (comme la carte de conversation)
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
        card.setRadius(dpToPx(20));
        card.setCardElevation(0f);
        card.setMaxCardElevation(0f);
        card.setCardBackgroundColor(Color.parseColor("#F5F5F5"));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(
                (int) dpToPx(12),
                (int) dpToPx(12),
                (int) dpToPx(14),
                (int) dpToPx(12)
        );

        // Tuile blanche arrondie avec la photo
        int tileSize = (int) dpToPx(90);
        CardView tile = new CardView(context);
        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(tileSize, tileSize);
        tileParams.setMargins(0, 0, (int) dpToPx(14), 0);
        tile.setLayoutParams(tileParams);
        tile.setRadius(dpToPx(16));
        tile.setCardElevation(0f);
        tile.setMaxCardElevation(0f);
        tile.setCardBackgroundColor(Color.WHITE);

        ImageView photo = new ImageView(context);
        photo.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        photo.setPadding((int) dpToPx(10), (int) dpToPx(10), (int) dpToPx(10), (int) dpToPx(10));
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadImageAsync(photo, image);
        tile.addView(photo);
        row.addView(tile);

        // Colonne de droite
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleTv = new TextView(context);
        titleTv.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        titleTv.setText(title == null || title.trim().isEmpty() ? "Produit" : title);
        titleTv.setTextColor(textBlack);
        titleTv.setTextSize(titleSizeSp);
        titleTv.setMaxLines(1);
        titleTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleTv.setTypeface(customTypeface != null ? customTypeface : Typeface.DEFAULT);
        topRow.addView(titleTv);

        TextView priceTv = new TextView(context);
        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        priceParams.setMargins((int) dpToPx(10), 0, 0, 0);
        priceTv.setLayoutParams(priceParams);
        priceTv.setText(price == null ? "" : price);
        priceTv.setTextColor(textBlack);
        priceTv.setTextSize(priceSizeSp);
        priceTv.setMaxLines(1);
        priceTv.setTypeface(customTypeface != null ? customTypeface : Typeface.DEFAULT, Typeface.BOLD);
        topRow.addView(priceTv);
        column.addView(topRow);

        // Pastille « Qnt. N » : contour fin, coins arrondis, fond transparent
        TextView qtyTv = new TextView(context);
        LinearLayout.LayoutParams qtyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        qtyParams.setMargins(0, (int) dpToPx(8), 0, 0);
        qtyTv.setLayoutParams(qtyParams);
        qtyTv.setText("Qnt. " + Math.max(0, quantity));
        qtyTv.setTextColor(textBlack);
        qtyTv.setTextSize(qtySizeSp);
        qtyTv.setTypeface(customTypeface != null ? customTypeface : Typeface.DEFAULT);
        qtyTv.setPadding((int) dpToPx(20), (int) dpToPx(4), (int) dpToPx(20), (int) dpToPx(4));
        GradientDrawable qtyBg = new GradientDrawable();
        qtyBg.setColor(Color.TRANSPARENT);
        qtyBg.setStroke((int) dpToPx(1), Color.parseColor("#BDBDC2"));
        qtyBg.setCornerRadius(dpToPx(12));
        qtyTv.setBackground(qtyBg);
        column.addView(qtyTv);

        row.addView(column);
        card.addView(row);

        // Clic sur la carte -> Détail Produit ; appui long -> suppression
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedCartProductUid = productUid;
                OnCartCardDetailsClick(productUid);
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                selectedCartProductUid = productUid;
                OnCartCardLongClick(productUid);
                return true;
            }
        });

        return card;
    }

    // =========================================================================
    // 12quater. SUPPRESSION, STATUT ENVOYÉ/VU, NOTIFICATIONS, CARTE PLEINE LARGEUR
    // =========================================================================

    private static class BubbleTag {
        final int id;
        final String key;
        final boolean isMe;
        final TextView status;

        BubbleTag(int id, String key, boolean isMe, TextView status) {
            this.id = id;
            this.key = key;
            this.isMe = isMe;
            this.status = status;
        }
    }

    private int nextBubbleId = 1;
    private int nextNotificationId = 1000;
    private final Map<Integer, java.lang.ref.WeakReference<View>> bubbleRows =
            new HashMap<Integer, java.lang.ref.WeakReference<View>>();
    private final Map<View, java.lang.ref.WeakReference<TextView>> lastSentStatus =
            new java.util.WeakHashMap<View, java.lang.ref.WeakReference<TextView>>();

    // Identité d'un message : expéditeur + texte (ou [img] pour une photo).
    private String bubbleKey(boolean isMe, String text, String image) {
        String t = text == null ? "" : text;
        if (t.isEmpty()) {
            if (image == null || image.isEmpty()) return null;
            t = "[img]";
        }
        return (isMe ? "1|" : "0|") + t;
    }

    // Appelé à la création de chaque bulle : identifiant, appui long, et statut « Envoyé » sous mes messages.
    private void attachMessageExtras(ViewGroup targetLayout, ViewGroup row, View bubble, View extraTarget,
                                     String text, String image, boolean isMe, int statusColor) {
        final int id = nextBubbleId++;
        String key = bubbleKey(isMe, text, image);
        if (key == null) key = isMe ? "1|" : "0|";

        TextView statusTv = null;
        if (isMe && bubble instanceof LinearLayout) {
            statusTv = new TextView(context);
            statusTv.setText("Envoyé");
            statusTv.setTextColor(statusColor);
            statusTv.setTextSize(10);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            sp.gravity = Gravity.END;
            statusTv.setLayoutParams(sp);
            if (customTypeface != null) {
                statusTv.setTypeface(customTypeface);
            }
            ((LinearLayout) bubble).addView(statusTv);
            java.lang.ref.WeakReference<TextView> prev = lastSentStatus.get(targetLayout);
            if (prev != null && prev.get() != null) {
                prev.get().setVisibility(View.GONE);
            }
            lastSentStatus.put(targetLayout, new java.lang.ref.WeakReference<TextView>(statusTv));
        }

        row.setTag(new BubbleTag(id, key, isMe, statusTv));
        bubbleRows.put(id, new java.lang.ref.WeakReference<View>(row));

        final String textFinal = text == null ? "" : text;
        final boolean isMeFinal = isMe;
        View.OnLongClickListener longClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                OnChatBubbleLongClick(id, textFinal, isMeFinal);
                return true;
            }
        };
        bubble.setOnLongClickListener(longClick);
        if (extraTarget != null) {
            extraTarget.setOnLongClickListener(longClick);
        }
    }

    @SimpleEvent(description = "Appui long sur une bulle de message. messageId sert ensuite à DeleteChatMessage.")
    public void OnChatBubbleLongClick(int messageId, String messageText, boolean isMe) {
        EventDispatcher.dispatchEvent(this, "OnChatBubbleLongClick", messageId, messageText, isMe);
    }

    @SimpleEvent(description = "Un message a été supprimé (écran et sauvegarde).")
    public void OnChatMessageDeleted(String conversationTag, int messageId) {
        EventDispatcher.dispatchEvent(this, "OnChatMessageDeleted", conversationTag, messageId);
    }

    @SimpleEvent(description = "Une conversation a été supprimée (carte, messages et sauvegarde). Utilise-la pour prévenir ton serveur si besoin.")
    public void OnConversationDeleted(String conversationUid) {
        EventDispatcher.dispatchEvent(this, "OnConversationDeleted", conversationUid);
    }

    @SimpleFunction(description = "Supprime un message de l'écran et de la sauvegarde. messageId vient de OnChatBubbleLongClick. Si la synchronisation automatique est active, le serveur est prévenu (action delete_message).")
    public void DeleteChatMessage(final String conversationTag, final int messageId) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    java.lang.ref.WeakReference<View> ref = bubbleRows.get(messageId);
                    View row = ref == null ? null : ref.get();
                    if (row == null || !(row.getTag() instanceof BubbleTag)) {
                        OnError("DeleteChatMessage: message introuvable (messageId inconnu).");
                        return;
                    }
                    BubbleTag tag = (BubbleTag) row.getTag();
                    ViewGroup parent = (ViewGroup) row.getParent();

                    int ordinal = 0;
                    int position = -1;
                    if (parent != null) {
                        for (int i = 0; i < parent.getChildCount(); i++) {
                            View v = parent.getChildAt(i);
                            if (v == row) {
                                position = i;
                                break;
                            }
                            if (v.getTag() instanceof BubbleTag
                                    && ((BubbleTag) v.getTag()).key.equals(tag.key)) {
                                ordinal++;
                            }
                        }
                    }

                    JSONObject supprime = removeFromHistory(conversationTag, tag.key, ordinal);

                    if (parent != null) {
                        java.lang.ref.WeakReference<TextView> st = lastSentStatus.get(parent);
                        boolean portaitStatut = tag.status != null && st != null && st.get() == tag.status;
                        View avant = position > 0 ? parent.getChildAt(position - 1) : null;
                        View apres = (position >= 0 && position + 1 < parent.getChildCount())
                                ? parent.getChildAt(position + 1) : null;

                        parent.removeView(row);

                        if (avant != null && "date_divider".equals(avant.getTag())
                                && (apres == null || "date_divider".equals(apres.getTag()))) {
                            parent.removeView(avant);
                        }

                        if (portaitStatut) {
                            lastSentStatus.remove(parent);
                            for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                                View v = parent.getChildAt(i);
                                if (v.getTag() instanceof BubbleTag) {
                                    BubbleTag t2 = (BubbleTag) v.getTag();
                                    if (t2.isMe && t2.status != null) {
                                        t2.status.setVisibility(View.VISIBLE);
                                        lastSentStatus.put(parent, new java.lang.ref.WeakReference<TextView>(t2.status));
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    bubbleRows.remove(messageId);
                    refreshSummaryAfterDelete(conversationTag);

                    if (supprime != null && chatSyncActive()) {
                        String myUid = chatPrefs().getString(SYNC_KEY_UID, "");
                        JSONObject corps = new JSONObject();
                        corps.put("action", "delete_message");
                        corps.put("userUid", myUid);
                        corps.put("conversationUid", conversationTag);
                        corps.put("message", messageForServer(supprime, conversationTag, myUid));
                        pushToServer(corps.toString());
                    }
                    OnChatMessageDeleted(conversationTag, messageId);
                } catch (Exception e) {
                    OnError("DeleteChatMessage: " + e.getMessage());
                }
            }
        });
    }

    // Retire du stockage le n-ième message qui correspond à la clé. Retourne le message retiré (ou null).
    private JSONObject removeFromHistory(String conversationTag, String key, int ordinal) throws Exception {
        String hk = "hist_" + conversationTag;
        JSONArray liste = new JSONArray(chatPrefs().getString(hk, "[]"));
        int compte = 0;
        for (int i = 0; i < liste.length(); i++) {
            JSONObject m = liste.getJSONObject(i);
            String k = bubbleKey(m.optBoolean("isMe", false), m.optString("text", ""), m.optString("image", ""));
            if (k == null || !k.equals(key)) continue;
            if (compte == ordinal) {
                JSONArray nouvelle = new JSONArray();
                for (int j = 0; j < liste.length(); j++) {
                    if (j != i) nouvelle.put(liste.get(j));
                }
                chatPrefs().edit().putString(hk, nouvelle.toString()).apply();
                return m;
            }
            compte++;
        }
        return null;
    }

    // Met à jour le dernier message affiché sur la carte après une suppression de message.
    private void refreshSummaryAfterDelete(String conversationTag) {
        try {
            String ancien = chatPrefs().getString("conv_" + conversationTag, null);
            if (ancien == null) return;
            JSONArray liste = new JSONArray(chatPrefs().getString("hist_" + conversationTag, "[]"));
            String dernier = "";
            if (liste.length() > 0) {
                JSONObject m = liste.getJSONObject(liste.length() - 1);
                dernier = m.optString("text", "");
                if (dernier.isEmpty() && !m.optString("image", "").isEmpty()) {
                    dernier = "\uD83D\uDCF7 Photo";
                }
            }
            JSONObject s = new JSONObject(ancien);
            s.put("lastMessage", dernier);
            chatPrefs().edit().putString("conv_" + conversationTag, s.toString()).apply();
        } catch (Exception e) {
            OnError("Mise à jour de la carte: " + e.getMessage());
        }
    }

    // Suppression complète d'une conversation : carte, messages, état « vu » et, si la synchro est active, le serveur.
    private void purgeConversationData(String conversationUid) {
        try {
            android.content.SharedPreferences p = chatPrefs();
            JSONArray liste = new JSONArray(p.getString("conv_uid_list", "[]"));
            JSONArray nouvelle = new JSONArray();
            for (int i = 0; i < liste.length(); i++) {
                String u = liste.getString(i);
                if (!u.equals(conversationUid)) nouvelle.put(u);
            }
            p.edit()
                    .remove("conv_" + conversationUid)
                    .remove("hist_" + conversationUid)
                    .remove("seen_" + conversationUid)
                    .putString("conv_uid_list", nouvelle.toString())
                    .apply();
            if (chatSyncActive()) {
                JSONObject corps = new JSONObject();
                corps.put("action", "delete_chat");
                corps.put("userUid", p.getString(SYNC_KEY_UID, ""));
                corps.put("conversationUid", conversationUid);
                pushToServer(corps.toString());
            }
            OnConversationDeleted(conversationUid);
        } catch (Exception e) {
            OnError("Suppression de la conversation: " + e.getMessage());
        }
    }

    // ----- Statut « Vu » -----

    @SimpleFunction(description = "Construit le JSON d'accusé de lecture (type seen) à envoyer avec SendWebSocketMessage quand l'utilisateur ouvre une conversation. to : l'uid de la personne qui a écrit. uid : ton uid.")
    public String BuildSeenJson(String to, String uid) {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "seen");
            o.put("to", to == null ? "" : to);
            o.put("uid", uid == null ? "" : uid);
            return o.toString();
        } catch (Exception e) {
            OnError("BuildSeenJson: " + e.getMessage());
            return "";
        }
    }

    @SimpleFunction(description = "Marque comme « Vu » le dernier message que tu as envoyé dans cette conversation : met à jour l'affichage du chat ouvert et sauvegarde l'état. À appeler quand tu reçois un accusé de lecture pour la conversation ouverte.")
    public void MarkLastMessageSeen(final AndroidViewComponent chatContainer, final String conversationTag) {
        chatPrefs().edit().putLong("seen_" + conversationTag, System.currentTimeMillis()).apply();
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup layout = getRealLayout(chatContainer);
                    if (layout == null) return;
                    java.lang.ref.WeakReference<TextView> ref = lastSentStatus.get(layout);
                    if (ref != null && ref.get() != null) {
                        ref.get().setText("Vu");
                    }
                } catch (Exception e) {
                    OnError("MarkLastMessageSeen: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Sauvegarde seulement l'état « Vu » d'une conversation (sans toucher à l'écran). À utiliser quand l'accusé de lecture arrive alors que cette conversation n'est pas ouverte.")
    public void SaveConversationSeen(String conversationTag) {
        chatPrefs().edit().putLong("seen_" + conversationTag, System.currentTimeMillis()).apply();
    }

    private void appliquerEtatVu(final AndroidViewComponent chatContainer, final String conversationTag, final long dernierMsgMoi) {
        if (dernierMsgMoi <= 0) return;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    long vuA = chatPrefs().getLong("seen_" + conversationTag, 0);
                    if (vuA < dernierMsgMoi) return;
                    ViewGroup layout = getRealLayout(chatContainer);
                    if (layout == null) return;
                    java.lang.ref.WeakReference<TextView> ref = lastSentStatus.get(layout);
                    if (ref != null && ref.get() != null) {
                        ref.get().setText("Vu");
                    }
                } catch (Exception e) {
                    OnError("appliquerEtatVu: " + e.getMessage());
                }
            }
        });
    }

    // ----- Notifications -----

    @SimpleFunction(description = "Joue le son de notification par défaut du téléphone.")
    public void PlayNotificationSound() {
        try {
            android.net.Uri uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            android.media.Ringtone r = android.media.RingtoneManager.getRingtone(context, uri);
            if (r != null) {
                r.play();
            }
        } catch (Exception e) {
            OnError("PlayNotificationSound: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Affiche une notification Android (barre d'état). playSound : avec ou sans le son de notification. Un appui sur la notification ouvre l'application. Fonctionne tant que l'application est en vie (au premier plan ou en arrière-plan) ; sur Android 13+ appelle d'abord RequestNotificationPermission.")
    public void ShowNotification(String title, String message, boolean playSound) {
        try {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) {
                OnError("ShowNotification: service de notification indisponible.");
                return;
            }
            String channelId = playSound ? "manaplace_messages" : "manaplace_messages_silent";
            if (Build.VERSION.SDK_INT >= 26) {
                android.app.NotificationChannel ch = new android.app.NotificationChannel(
                        channelId,
                        playSound ? "Messages" : "Messages (silencieux)",
                        playSound ? android.app.NotificationManager.IMPORTANCE_HIGH
                                : android.app.NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(ch);
            }
            Intent lancement = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            android.app.PendingIntent pi = null;
            if (lancement != null) {
                int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= 23) {
                    flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
                }
                pi = android.app.PendingIntent.getActivity(context, 0, lancement, flags);
            }
            android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new android.app.Notification.Builder(context, channelId)
                    : new android.app.Notification.Builder(context);
            b.setSmallIcon(context.getApplicationInfo().icon);
            b.setContentTitle(title == null ? "" : title);
            b.setContentText(message == null ? "" : message);
            b.setAutoCancel(true);
            if (pi != null) {
                b.setContentIntent(pi);
            }
            if (Build.VERSION.SDK_INT < 26 && playSound) {
                b.setDefaults(android.app.Notification.DEFAULT_SOUND);
            }
            nm.notify(nextNotificationId++, b.build());
        } catch (Exception e) {
            OnError("ShowNotification: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Demande l'autorisation d'afficher des notifications (Android 13 et plus). Sur les versions plus anciennes, répond tout de suite « accordée ».")
    public void RequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            OnNotificationPermissionResult(true);
            return;
        }
        form.askPermission(
                "android.permission.POST_NOTIFICATIONS",
                new PermissionResultHandler() {
                    @Override
                    public void HandlePermissionResponse(String permissionName, final boolean granted) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                OnNotificationPermissionResult(granted);
                            }
                        });
                    }
                });
    }

    @SimpleEvent(description = "Résultat de la demande d'autorisation des notifications.")
    public void OnNotificationPermissionResult(boolean granted) {
        EventDispatcher.dispatchEvent(this, "OnNotificationPermissionResult", granted);
    }

    // ----- Carte pleine largeur -----

    private CardView findCardViewIn(View v, int profondeur) {
        if (v instanceof CardView) return (CardView) v;
        if (profondeur <= 0 || !(v instanceof ViewGroup)) return null;
        ViewGroup g = (ViewGroup) v;
        for (int i = 0; i < g.getChildCount(); i++) {
            CardView c = findCardViewIn(g.getChildAt(i), profondeur - 1);
            if (c != null) return c;
        }
        return null;
    }

    @SimpleFunction(description = "Force une carte (CardView) à occuper toute la largeur de son conteneur : supprime l'espace ajouté autour de la carte (ombre/compat padding). sideMarginDp : marge gauche et droite voulue (0 = bord à bord). À appeler dans Screen.Initialize.")
    public void FillCardWidth(final AndroidViewComponent card, final int sideMarginDp) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    View v = card == null ? null : card.getView();
                    if (v == null) {
                        OnError("FillCardWidth: composant invalide.");
                        return;
                    }
                    CardView cv = findCardViewIn(v, 2);
                    if (cv != null) {
                        cv.setUseCompatPadding(false);
                    }
                    int m = (int) dpToPx(Math.max(0, sideMarginDp));
                    View[] cibles = (cv != null && cv != v) ? new View[]{v, cv} : new View[]{v};
                    for (View cible : cibles) {
                        ViewGroup.LayoutParams lp = cible.getLayoutParams();
                        if (lp == null) continue;
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        if (lp instanceof ViewGroup.MarginLayoutParams) {
                            ((ViewGroup.MarginLayoutParams) lp).leftMargin = m;
                            ((ViewGroup.MarginLayoutParams) lp).rightMargin = m;
                        }
                        cible.setLayoutParams(lp);
                        cible.requestLayout();
                    }
                } catch (Exception e) {
                    OnError("FillCardWidth: " + e.getMessage());
                }
            }
        });
    }

    // =========================================================================
    // 12ter. MESSAGES JSON + SYNCHRONISATION DES DISCUSSIONS AVEC LE SERVEUR
    // Même uid = même historique sur n'importe quel téléphone.
    // =========================================================================

    private static final String SYNC_KEY_URL = "sync_url";
    private static final String SYNC_KEY_UID = "sync_uid";
    private static final String SYNC_KEY_TOKEN = "sync_token";
    private static final String SYNC_KEY_AUTO = "sync_auto";

    @SimpleFunction(description = "Construit le JSON d'un message pour SendWebSocketMessage (guillemets et retours à la ligne gérés). Clés : to, uid, text, productImage, productTitle.")
    public String BuildMessageJson(String to, String uid, String text, String productImage, String productTitle) {
        try {
            JSONObject o = new JSONObject();
            o.put("to", to == null ? "" : to);
            o.put("uid", uid == null ? "" : uid);
            o.put("text", text == null ? "" : text);
            o.put("productImage", productImage == null ? "" : productImage);
            o.put("productTitle", productTitle == null ? "" : productTitle);
            return o.toString();
        } catch (Exception e) {
            OnError("BuildMessageJson: " + e.getMessage());
            return "";
        }
    }

    @SimpleFunction(description = "Active la synchronisation des discussions avec ton serveur. endpointUrl : adresse unique (POST, champ action). myUid : uid de l'utilisateur connecté. authToken : jeton de connexion (peut être vide). uploadAutomatically : si vrai, chaque message et chaque carte sauvegardés sont aussi envoyés au serveur ; mets faux si ton serveur enregistre déjà les messages lui-même. Les réglages sont conservés dans le téléphone.")
    public void ConfigureChatSync(String endpointUrl, String myUid, String authToken, boolean uploadAutomatically) {
        if (endpointUrl == null || endpointUrl.trim().isEmpty() || myUid == null || myUid.trim().isEmpty()) {
            OnError("ConfigureChatSync: endpointUrl et myUid sont obligatoires.");
            return;
        }
        chatPrefs().edit()
                .putString(SYNC_KEY_URL, endpointUrl.trim())
                .putString(SYNC_KEY_UID, myUid.trim())
                .putString(SYNC_KEY_TOKEN, authToken == null ? "" : authToken.trim())
                .putBoolean(SYNC_KEY_AUTO, uploadAutomatically)
                .apply();
    }

    @SimpleFunction(description = "Désactive la synchronisation (à appeler à la déconnexion de l'utilisateur).")
    public void DisableChatSync() {
        chatPrefs().edit()
                .remove(SYNC_KEY_URL)
                .remove(SYNC_KEY_UID)
                .remove(SYNC_KEY_TOKEN)
                .remove(SYNC_KEY_AUTO)
                .apply();
    }

    @SimpleFunction(description = "Télécharge depuis le serveur toutes les discussions de l'utilisateur (action get_chats) et les fusionne avec celles du téléphone sans créer de doublons. Déclenche OnChatsRestored, puis appelle LoadAllConversationCards pour afficher les cartes.")
    public void RestoreChatsFromServer() {
        if (!chatSyncActive()) {
            OnError("RestoreChatsFromServer: appelle d'abord ConfigureChatSync.");
            return;
        }
        final String myUid = chatPrefs().getString(SYNC_KEY_UID, "");
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject req = new JSONObject();
                    req.put("action", "get_chats");
                    req.put("userUid", myUid);
                    String resp = chatSyncPost(req.toString()).trim();
                    JSONArray convs = resp.startsWith("[")
                            ? new JSONArray(resp)
                            : new JSONObject(resp).optJSONArray("conversations");
                    if (convs == null) {
                        throw new Exception("réponse sans liste \"conversations\"");
                    }
                    final int n = mergeConversationsFromServer(convs, myUid);
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            OnChatsRestored(n);
                        }
                    });
                } catch (final Exception e) {
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            OnError("RestoreChatsFromServer: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    @SimpleFunction(description = "Envoie au serveur tout l'historique du téléphone pour une conversation (action save_chat). Utile pour envoyer les anciennes discussions ou réessayer après un échec. Déclenche OnChatUploaded.")
    public void UploadChatHistory(final String conversationTag) {
        if (!chatSyncActive()) {
            OnError("UploadChatHistory: appelle d'abord ConfigureChatSync.");
            return;
        }
        final String myUid = chatPrefs().getString(SYNC_KEY_UID, "");
        final String histJson = chatPrefs().getString("hist_" + conversationTag, "[]");
        final String summaryJson = chatPrefs().getString("conv_" + conversationTag, "");
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray locaux = new JSONArray(histJson);
                    JSONArray messages = new JSONArray();
                    for (int i = 0; i < locaux.length(); i++) {
                        messages.put(messageForServer(locaux.getJSONObject(i), conversationTag, myUid));
                    }
                    JSONObject corps = new JSONObject();
                    corps.put("action", "save_chat");
                    corps.put("userUid", myUid);
                    corps.put("conversationUid", conversationTag);
                    corps.put("messages", messages);
                    if (!summaryJson.isEmpty()) {
                        corps.put("summary", new JSONObject(summaryJson));
                    }
                    chatSyncPost(corps.toString());
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            OnChatUploaded(conversationTag);
                        }
                    });
                } catch (final Exception e) {
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            OnError("UploadChatHistory: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    @SimpleEvent(description = "Les discussions du serveur ont été fusionnées avec celles du téléphone. conversationCount = nombre de conversations reçues. Appelle ensuite LoadAllConversationCards.")
    public void OnChatsRestored(int conversationCount) {
        EventDispatcher.dispatchEvent(this, "OnChatsRestored", conversationCount);
    }

    @SimpleEvent(description = "L'historique d'une conversation a été envoyé au serveur.")
    public void OnChatUploaded(String conversationTag) {
        EventDispatcher.dispatchEvent(this, "OnChatUploaded", conversationTag);
    }

    private boolean chatSyncActive() {
        android.content.SharedPreferences p = chatPrefs();
        return !p.getString(SYNC_KEY_URL, "").isEmpty()
                && !p.getString(SYNC_KEY_UID, "").isEmpty();
    }

    // Seules les images déjà hébergées (lien http) sont envoyées : un chemin local n'existe pas sur un autre téléphone.
    private JSONObject messageForServer(JSONObject local, String tag, String myUid) throws Exception {
        boolean isMe = local.optBoolean("isMe", false);
        String image = local.optString("image", "");
        JSONObject m = new JSONObject();
        m.put("senderUid", isMe ? myUid : tag);
        m.put("text", local.optString("text", ""));
        m.put("image", image.startsWith("http") ? image : "");
        m.put("timestamp", local.optLong("timestamp", 0));
        return m;
    }

    private String buildSaveMessageBody(String tag, JSONObject local) throws Exception {
        String myUid = chatPrefs().getString(SYNC_KEY_UID, "");
        JSONObject corps = new JSONObject();
        corps.put("action", "save_message");
        corps.put("userUid", myUid);
        corps.put("conversationUid", tag);
        corps.put("message", messageForServer(local, tag, myUid));
        return corps.toString();
    }

    // Envoi automatique en arrière-plan (uniquement si uploadAutomatically = vrai).
    private void pushToServer(final String body) {
        if (!chatPrefs().getBoolean(SYNC_KEY_AUTO, false)) {
            return;
        }
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    chatSyncPost(body);
                } catch (final Exception e) {
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            OnError("Synchronisation (envoi) : " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    private String chatSyncPost(String bodyJson) throws Exception {
        android.content.SharedPreferences p = chatPrefs();
        String endpoint = p.getString(SYNC_KEY_URL, "");
        String token = p.getString(SYNC_KEY_TOKEN, "");
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (!token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(bodyJson.getBytes("UTF-8"));
            os.flush();
            os.close();
            int code = conn.getResponseCode();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            StringBuilder sb = new StringBuilder();
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
            }
            if (code < 200 || code >= 300) {
                throw new Exception("serveur HTTP " + code + " " + sb);
            }
            return sb.toString();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // Même message = même expéditeur + même texte + horodatage à moins de 2 minutes d'écart.
    private boolean messageExists(java.util.ArrayList<JSONObject> msgs, long ts, boolean isMe, String text) {
        for (JSONObject m : msgs) {
            if (m.optBoolean("isMe", false) == isMe
                    && m.optString("text", "").equals(text)
                    && Math.abs(m.optLong("timestamp", 0) - ts) <= 120000) {
                return true;
            }
        }
        return false;
    }

    private int mergeConversationsFromServer(JSONArray convs, String myUid) throws Exception {
        android.content.SharedPreferences p = chatPrefs();
        android.content.SharedPreferences.Editor ed = p.edit();
        JSONArray listeUid = new JSONArray(p.getString("conv_uid_list", "[]"));
        int count = 0;

        for (int i = 0; i < convs.length(); i++) {
            JSONObject c = convs.optJSONObject(i);
            if (c == null) continue;
            String tag = c.optString("conversationUid", "");
            if (tag.isEmpty()) continue;

            java.util.ArrayList<JSONObject> msgs = new java.util.ArrayList<JSONObject>();
            JSONArray locaux = new JSONArray(p.getString("hist_" + tag, "[]"));
            for (int k = 0; k < locaux.length(); k++) {
                msgs.add(locaux.getJSONObject(k));
            }

            JSONArray distants = c.optJSONArray("messages");
            if (distants != null) {
                for (int k = 0; k < distants.length(); k++) {
                    JSONObject d = distants.optJSONObject(k);
                    if (d == null) continue;
                    boolean isMe = d.has("senderUid")
                            ? myUid.equals(d.optString("senderUid", ""))
                            : d.optBoolean("isMe", false);
                    long ts = d.optLong("timestamp", 0);
                    String text = d.optString("text", "");
                    String image = d.optString("image", "");
                    if (text.isEmpty() && image.isEmpty()) continue;
                    if (messageExists(msgs, ts, isMe, text)) continue;

                    JSONObject m = new JSONObject();
                    m.put("text", text);
                    m.put("image", image);
                    m.put("time", ts > 0
                            ? new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(ts))
                            : "");
                    m.put("timestamp", ts);
                    m.put("isMe", isMe);
                    msgs.add(m);
                }
            }

            java.util.Collections.sort(msgs, new java.util.Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject a, JSONObject b) {
                    long x = a.optLong("timestamp", 0);
                    long y = b.optLong("timestamp", 0);
                    return x < y ? -1 : (x > y ? 1 : 0);
                }
            });
            JSONArray sortie = new JSONArray();
            for (JSONObject m : msgs) {
                sortie.put(m);
            }
            ed.putString("hist_" + tag, sortie.toString());

            JSONObject s = c.optJSONObject("summary");
            if (s != null && !p.contains("conv_" + tag)) {
                JSONObject r = new JSONObject();
                r.put("productImage", s.optString("productImage", ""));
                r.put("productTitle", s.optString("productTitle", ""));
                r.put("lastMessage", s.optString("lastMessage", ""));
                r.put("isNewMessage", s.optBoolean("isNewMessage", false));
                r.put("dateText", s.optString("dateText", ""));
                ed.putString("conv_" + tag, r.toString());
            }

            boolean dejaListe = false;
            for (int k = 0; k < listeUid.length(); k++) {
                if (tag.equals(listeUid.getString(k))) {
                    dejaListe = true;
                    break;
                }
            }
            if (!dejaListe) {
                listeUid.put(tag);
            }
            count++;
        }

        ed.putString("conv_uid_list", listeUid.toString());
        ed.apply();
        return count;
    }

    // =========================================================================
    // 15. LECTURE JSON (extraction simple de clés, ex: messages WebSocket)
    // =========================================================================

    @SimpleFunction(description = "Extrait la valeur d'une clé depuis un texte JSON (objet). Retourne valueIfNotThere si la clé est absente ou si le JSON est invalide.")
    public String GetJsonValue(String json, String key, String valueIfNotThere) {
        try {
            JSONObject obj = new JSONObject(json);
            if (obj.has(key) && !obj.isNull(key)) {
                return obj.get(key).toString();
            }
        } catch (Exception e) {
            // JSON invalide ou clé absente : on retourne la valeur par défaut
        }
        return valueIfNotThere;
    }

    @SimpleFunction(description = "Vérifie si un texte JSON (objet) contient une clé donnée.")
    public boolean HasJsonKey(String json, String key) {
        try {
            JSONObject obj = new JSONObject(json);
            return obj.has(key) && !obj.isNull(key);
        } catch (Exception e) {
            return false;
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

    // =========================================================================
    // 16. BOUTIQUE VENDEUR / ACHETEUR (ex-Manatest 1/5)
    // =========================================================================

    // Mêmes préférences que ManaplaceUtils / création de boutique
    private static final String PREFS_NAME = "ManaplaceShop";
    private static final String PREF_CURRENT_UID = "current_uid";
    private static final String HEADER_TAG = "manatest_header";


    private Typeface shopIconFont;

    // Données de la boutique affichée
    private String shopUidValue = "";
    private boolean ownerFlag = false;
    private String shopNameValue = "";
    private String shopCategoryValue = "";
    private String shopAddressValue = "";
    private String shopPhoneValue = "";
    private String logoSourceValue = "";
    private String productsTitleOverride = "";
    private int articleCount = 0;
    private Bitmap logoBitmap;

    // Icônes (caractères Phosphor donnés par les blocs)
    private String iconBack = "";
    private String iconEdit = "";
    private String iconPerson = "";
    private String iconPhone = "";
    private String iconAddress = "";
    private String iconReport = "";

    // Couleurs déduites du logo (calculées sur fond blanc)
    private int topColor = Color.WHITE;
    private double topLum = 1.0;
    private double contentLum = 1.0;
    private int onColor = Color.parseColor("#1A1A1B");

    // Dimensions
    private int screenW;
    private int screenH;
    private int statusBarH;
    private int logoHeightPx;
    private int topBtnW;
    private int topBtnH;
    private int extraTop = 0;         // partie du logo qui passe sous la barre de statut
    private boolean edgeMode = false; // vrai : le contenu démarre sous la barre (transparente)

    // Vues
    private LinearLayout headerHolder;
    private ScrollView scrollView;
    private FrameLayout logoFrame;
    private LinearLayout.LayoutParams logoFrameParams;
    private FrameLayout contentLayer;
    private ImageView logoImage;
    private TextView nameTv;
    private TextView categoryTv;
    private TextView leftIconTv;
    private TextView personIconTv;
    private TextView titleTv;
    private TextView countTv;
    private View statusScrim;
    private View decorBackdrop;
    private PopupWindow infoPopup;
    private ViewTreeObserver.OnScrollChangedListener scrollListener;

    // Barre de statut d'origine
    private boolean barsSaved = false;
    private int origStatusColor;
    private int origSysUiFlags;

    // Police de la page boutique : celle de LoadCustomFont si elle existe,
    // sinon Manrope-Medium.ttf des Assets (si présent), sinon la police système.
    private Typeface shopDefaultFont = null;
    private boolean shopDefaultFontTried = false;

    private Typeface shopFont() {
        if (customTypeface != null && customTypeface != Typeface.DEFAULT) return customTypeface;
        if (!shopDefaultFontTried) {
            shopDefaultFontTried = true;
            shopDefaultFont = tryLoadTypeface("Manrope-Medium.ttf");
        }
        return shopDefaultFont;
    }

    // =========================================================================
    // OUTILS
    // =========================================================================

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, context.getResources().getDisplayMetrics());
    }

    private int ink(int alpha) {
        return Color.argb(alpha, 0x1A, 0x1A, 0x1B);
    }

    private void fail(final String message) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                OnError(message);
            }
        });
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(context);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        Typeface f = shopFont();
        if (f != null) t.setTypeface(f);
        return t;
    }

    private ScrollView findScrollView(View v, int depth) {
        if (v instanceof ScrollView) return (ScrollView) v;
        if (depth > 3 || !(v instanceof ViewGroup)) return null;
        ViewGroup g = (ViewGroup) v;
        for (int i = 0; i < g.getChildCount(); i++) {
            ScrollView s = findScrollView(g.getChildAt(i), depth + 1);
            if (s != null) return s;
        }
        return null;
    }

    private double luminance(int c) {
        return (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0;
    }

    private float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private int blend(int a, int b, float p) {
        return Color.rgb(
                (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * p),
                (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * p),
                (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * p));
    }

    private int statusBarHeight() {
        int id = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? context.getResources().getDimensionPixelSize(id) : dp(24);
    }

    private boolean isOwner(String uid) {
        String me = prefs().getString(PREF_CURRENT_UID, "");
        return uid != null && !uid.isEmpty() && uid.equals(me);
    }

    // =========================================================================
    // ÉVÉNEMENTS
    // =========================================================================

    @SimpleEvent(description = "Le vendeur propriétaire a touché « Ajouter un produit ».")
    public void OnAddProductClick() {
        EventDispatcher.dispatchEvent(this, "OnAddProductClick");
    }

    @SimpleEvent(description = "Le vendeur propriétaire a touché le bouton Édit (en haut à gauche). Ouvre ici la page d'édition de la boutique. shopUid : l'UID de la boutique.")
    public void OnEditShopClick(String shopUid) {
        EventDispatcher.dispatchEvent(this, "OnEditShopClick", shopUid);
    }

    @SimpleEvent(description = "Un visiteur a touché le bouton Retour (en haut à gauche). Ferme ici l'écran.")
    public void OnBackClick() {
        EventDispatcher.dispatchEvent(this, "OnBackClick");
    }

    @SimpleEvent(description = "L'utilisateur a touché le numéro de téléphone dans le menu déroulant. Lance ici l'appel avec ton composant PhoneCall.")
    public void OnShopPhoneClick(String phone) {
        EventDispatcher.dispatchEvent(this, "OnShopPhoneClick", phone);
    }

    @SimpleEvent(description = "L'utilisateur a touché l'adresse dans le menu déroulant. Ouvre ici la carte ou copie l'adresse.")
    public void OnShopAddressClick(String address) {
        EventDispatcher.dispatchEvent(this, "OnShopAddressClick", address);
    }

    @SimpleEvent(description = "Un visiteur a touché « Signaler la boutique ». shopUid : l'UID de la boutique signalée. Cette ligne n'existe pas pour le vendeur propriétaire.")
    public void OnReportShopClick(String shopUid) {
        EventDispatcher.dispatchEvent(this, "OnReportShopClick", shopUid);
    }

    @SimpleEvent(description = "Le serveur a supprimé le produit (ou il n'existait déjà plus). productUid : l'uid du produit supprimé. Ferme la page d'édition et reviens à la page boutique : sa grille et son compteur se rechargent avec LoadShopFromServer.")
    public void OnProductDeleted(String productUid) {
        EventDispatcher.dispatchEvent(this, "OnProductDeleted", productUid);
    }

    @SimpleEvent(description = "La suppression du produit a échoué. responseCode : code HTTP du serveur (0 si refusée par l'application ou erreur réseau). message : la réponse du serveur ou la raison.")
    public void OnProductDeleteFailed(int responseCode, String message) {
        EventDispatcher.dispatchEvent(this, "OnProductDeleteFailed", responseCode, message);
    }

    @SimpleEvent(description = "Les infos de la boutique sont arrivées du serveur et affichées (nom, catégorie, logo, adresse, téléphone).")
    public void OnShopInfoLoaded(String name, String category, String logo, String address, String phone) {
        EventDispatcher.dispatchEvent(this, "OnShopInfoLoaded", name, category, logo, address, phone);
    }

    @SimpleEvent(description = "Les produits de la boutique sont arrivés du serveur (tableau JSON). Branche productsJson sur ManaplaceUtils.BuildProductGridFromJson. Le compteur d'articles est déjà mis à jour.")
    public void OnShopProductsLoaded(String productsJson) {
        EventDispatcher.dispatchEvent(this, "OnShopProductsLoaded", productsJson);
    }

    @SimpleEvent(description = "Le logo du header est chargé et affiché.")
    public void OnShopLogoLoaded() {
        EventDispatcher.dispatchEvent(this, "OnShopLogoLoaded");
    }

    // =========================================================================
    // CONFIGURATION : POLICE, ICÔNES, UTILISATEUR, INFOS
    // =========================================================================

    private Typeface tryLoadTypeface(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        try {
            if (name.startsWith("/")) return Typeface.createFromFile(new File(name));
        } catch (Exception ignored) {
        }
        try {
            return Typeface.createFromAsset(context.getAssets(), name);
        } catch (Exception ignored) {
        }
        try {
            File f = new File(Environment.getExternalStorageDirectory(), "AppInventor/assets/" + name);
            if (f.exists()) return Typeface.createFromFile(f);
        } catch (Exception ignored) {
        }
        return null;
    }

    @SimpleFunction(description = "Définit la police d'icônes (ex: Phosphor-Bold.ttf dans les Assets) utilisée par les icônes de la page boutique et de la page détails produit. À appeler avant BuildShopHome / BuildProductPage, avec SetShopIcons / SetProductIcons.")
    public void SetShopIconFont(String fontPath) {
        if (fontPath == null || fontPath.trim().isEmpty()) {
            shopIconFont = null;
            return;
        }
        Typeface t = tryLoadTypeface(fontPath.trim());
        if (t == null) {
            OnError("SetShopIconFont: police introuvable (" + fontPath + "). Mets le fichier dans les Assets.");
        } else {
            shopIconFont = t;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                refreshIcons();
                refreshProductIcons();
            }
        });
    }

    @SimpleFunction(description = "Définit les icônes de la page par leur caractère Phosphor. back : flèche retour (visiteur). edit : crayon (vendeur). person : personne (menu des infos). phone : téléphone. address : localisation. report : signaler. Laisse un texte vide pour utiliser le symbole par défaut (haut de page) ou aucune icône (lignes du menu).")
    public void SetShopIcons(String back, String edit, String person,
                             String phone, String address, String report) {
        iconBack = back == null ? "" : back;
        iconEdit = edit == null ? "" : edit;
        iconPerson = person == null ? "" : person;
        iconPhone = phone == null ? "" : phone;
        iconAddress = address == null ? "" : address;
        iconReport = report == null ? "" : report;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                refreshIcons();
            }
        });
    }

    @SimpleFunction(description = "Mémorise l'utilisateur connecté (UID Firebase). À appeler une fois à la connexion : BuildShopHome et la page détails produit (IsProductOwner, OnSellerClick) s'en servent pour savoir si c'est ta boutique ou ton produit ; IsSeller et le formulaire « Créer ma boutique » s'en servent aussi.")
    public void SetShopUser(String uid) {
        prefs().edit().putString(PREF_CURRENT_UID, uid == null ? "" : uid.trim()).apply();
    }

    @SimpleFunction(description = "Retourne vrai si la boutique affichée appartient à l'utilisateur connecté (SetShopUser).")
    public boolean IsShopOwner() {
        return isOwner(shopUidValue);
    }

    @SimpleFunction(description = "Définit le nom de la boutique (grand texte centré du header) et sa catégorie (texte centré en haut).")
    public void SetShopHeader(final String name, final String category) {
        shopNameValue = name == null ? "" : name.trim();
        shopCategoryValue = category == null ? "" : category.trim();
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if (nameTv != null) nameTv.setText(shopNameValue);
                if (categoryTv != null) categoryTv.setText(shopCategoryValue);
            }
        });
    }

    @SimpleFunction(description = "Définit l'adresse et le numéro de téléphone affichés dans le menu déroulant de l'icône personne.")
    public void SetShopContact(String address, String phone) {
        shopAddressValue = address == null ? "" : address.trim();
        shopPhoneValue = phone == null ? "" : phone.trim();
    }

    @SimpleFunction(description = "Change le titre au-dessus de la grille. Par défaut : « Mes Produits » pour le vendeur, « Produits » pour un visiteur.")
    public void SetProductsTitle(final String title) {
        productsTitleOverride = title == null ? "" : title.trim();
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if (titleTv != null) titleTv.setText(productsTitle());
            }
        });
    }

    private String productsTitle() {
        if (!productsTitleOverride.isEmpty()) return productsTitleOverride;
        return ownerFlag ? "Mes Produits" : "Produits";
    }

    @SimpleFunction(description = "Retourne le nom de la boutique chargé.")
    public String GetShopName() {
        return shopNameValue;
    }

    @SimpleFunction(description = "Retourne la catégorie de la boutique chargée.")
    public String GetShopCategory() {
        return shopCategoryValue;
    }

    @SimpleFunction(description = "Retourne le lien ou le chemin du logo de la boutique chargé.")
    public String GetShopLogo() {
        return logoSourceValue;
    }

    @SimpleFunction(description = "Retourne l'adresse de la boutique chargée.")
    public String GetShopAddress() {
        return shopAddressValue;
    }

    @SimpleFunction(description = "Retourne le numéro de téléphone de la boutique chargé.")
    public String GetShopPhone() {
        return shopPhoneValue;
    }

    // =========================================================================
    // CONSTRUCTION DE LA PAGE
    // =========================================================================

    @SimpleFunction(description = "Construit la page boutique dans l'arrangement scrollable donné : le header (logo, nom, boutons, bouton Ajouter un produit, titre et compteur) se place tout en haut, la grille de ManaplaceUtils (BuildProductGridFromJson) se place dessous, et tout défile ensemble. shopUid : l'UID de la boutique à afficher (vide = ta propre boutique). Si c'est celui de l'utilisateur connecté (SetShopUser), la page est celle du vendeur : bouton Édit, bouton Ajouter un produit, pas de ligne Signaler. Sinon c'est un visiteur : bouton Retour et ligne Signaler la boutique.")
    public void BuildShopHome(final AndroidViewComponent scrollContainer, final String shopUid) {
        if (scrollContainer == null || scrollContainer.getView() == null) {
            OnError("BuildShopHome: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    buildShopHomeInternal(scrollContainer, shopUid);
                } catch (Exception e) {
                    OnError("BuildShopHome: " + e.getMessage());
                }
            }
        });
    }

    private void buildShopHomeInternal(AndroidViewComponent container, String uid) {
        ScrollView sc = findScrollView(container.getView(), 0);
        if (sc == null) {
            OnError("BuildShopHome: le conteneur doit être un arrangement vertical scrollable.");
            return;
        }
        if (sc.getChildCount() == 0 || !(sc.getChildAt(0) instanceof LinearLayout)
                || ((LinearLayout) sc.getChildAt(0)).getOrientation() != LinearLayout.VERTICAL) {
            OnError("BuildShopHome: contenu de l'arrangement scrollable inattendu.");
            return;
        }
        LinearLayout inner = (LinearLayout) sc.getChildAt(0);

        detachScrollListener();
        dismissInfoMenu();

        String me = prefs().getString(PREF_CURRENT_UID, "");
        shopUidValue = uid == null ? "" : uid.trim();
        if (shopUidValue.isEmpty()) shopUidValue = me; // pas d'uid donné : sa propre boutique
        ownerFlag = isOwner(shopUidValue);
        if (me.isEmpty()) {
            OnError("[diag] utilisateur inconnu : appelle SetShopUser avec un uid non vide avant BuildShopHome. "
                    + "La page est en mode visiteur (Édit et Ajouter un produit masqués).");
        }

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        screenH = dm.heightPixels;
        screenW = dm.widthPixels;
        statusBarH = statusBarHeight();
        logoHeightPx = (int) (screenH * 0.35);
        topBtnW = (int) (screenW * 0.12);   // cercle : largeur = hauteur
        topBtnH = topBtnW;
        int btnH = (int) (screenH * 0.06);
        int btnW = (int) (screenW * 0.95);
        int cardH = (int) (screenH * 0.05);
        int cardW = (int) (screenW * 0.30);

        // Retire un ancien header avant d'en poser un nouveau
        for (int i = inner.getChildCount() - 1; i >= 0; i--) {
            if (HEADER_TAG.equals(inner.getChildAt(i).getTag())) inner.removeViewAt(i);
        }

        LinearLayout holder = new LinearLayout(context);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setTag(HEADER_TAG);
        holder.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        holder.addView(buildLogoFrame());

        if (ownerFlag) {
            TextView btn = text("Ajouter un produit", 15, Color.WHITE);
            btn.setGravity(Gravity.CENTER);
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(Color.argb(0xED, 0x1A, 0x1A, 0x1B));
            btnBg.setCornerRadius(dp(10));
            btn.setBackground(btnBg);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(btnW, btnH);
            bp.gravity = Gravity.CENTER_HORIZONTAL;
            bp.topMargin = dp(10);
            btn.setLayoutParams(bp);
            btn.setClickable(true);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OnAddProductClick();
                }
            });
            holder.addView(btn);
        }

        // Ligne : titre à gauche, carte compteur à droite
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.topMargin = ownerFlag ? dp(6) : dp(10);
        rp.bottomMargin = dp(10);
        row.setLayoutParams(rp);
        row.setPadding(dp(20), 0, dp(15), 0);

        titleTv = text(productsTitle(), 24, ink(0xFF));
        Typeface titleFont = shopFont();
        titleTv.setTypeface(titleFont != null
                ? Typeface.create(titleFont, Typeface.BOLD) : Typeface.DEFAULT_BOLD);
        titleTv.setSingleLine(true);
        titleTv.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(titleTv, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        countTv = text(countLabel(articleCount), 12, ink(0x7E));
        countTv.setGravity(Gravity.CENTER);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#F5F5F5"));
        cardBg.setCornerRadius(dp(8));
        countTv.setBackground(cardBg);
        row.addView(countTv, new LinearLayout.LayoutParams(cardW, cardH));

        holder.addView(row);

        inner.addView(holder, 0);

        headerHolder = holder;
        scrollView = sc;

        saveSystemBars();
        applySystemBars();
        applyLogoToView();
        attachScrollListener();
        applyScroll();

        // Le Form peut réappliquer ses couleurs après Initialize, et la disposition finale
        // n'est connue qu'après le premier affichage : on détecte et on réapplique
        int[] delays = {150, 450, 1000};
        for (int i = 0; i < delays.length; i++) {
            holder.postDelayed(new Runnable() {
                @Override
                public void run() {
                    applySystemBars();
                    detectEdge();
                }
            }, delays[i]);
        }
        holder.postDelayed(new Runnable() {
            @Override
            public void run() {
                diagnose();
            }
        }, 1500);
        holder.postDelayed(new Runnable() {
            @Override
            public void run() {
                diagnose();
            }
        }, 3500);
    }

    private void diagnose() {
        if (headerHolder != null && headerHolder.getParent() == null) {
            OnError("[diag] le header a été supprimé du conteneur : BuildProductGridFromJson vide encore tout le conteneur. Donne-lui un arrangement séparé (non scrollable) à l'intérieur de l'arrangement scrollable.");
        }
    }

    // ---- boutons ronds du haut (Édit ou Retour à gauche, Personne à droite) ----

    private FrameLayout makeTopButton() {
        FrameLayout b = new FrameLayout(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(0x66, 0xD5, 0xD5, 0xD5));
        bg.setShape(GradientDrawable.OVAL);
        b.setBackground(bg);
        b.setClickable(true);
        return b;
    }

    // Icône Phosphor si un caractère est donné, sinon un symbole simple par défaut
    private TextView makeIconText(String icon, String fallback) {
        TextView t = new TextView(context);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, topBtnW * 0.5f);
        t.setTextColor(ink(0xFF));
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        applyIcon(t, icon, fallback);
        return t;
    }

    private void applyIcon(TextView t, String icon, String fallback) {
        if (icon != null && !icon.isEmpty()) {
            t.setText(icon);
            if (shopIconFont != null) t.setTypeface(shopIconFont);
        } else {
            t.setText(fallback);
            t.setTypeface(Typeface.DEFAULT);
        }
    }

    private void refreshIcons() {
        if (leftIconTv != null) {
            if (ownerFlag) applyIcon(leftIconTv, iconEdit, "✎");
            else applyIcon(leftIconTv, iconBack, "←");
        }
        if (personIconTv != null) applyIcon(personIconTv, iconPerson, "●");
    }

    private View buildLogoFrame() {
        logoFrame = new FrameLayout(context);
        logoFrameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, logoHeightPx + extraTop);
        logoFrame.setLayoutParams(logoFrameParams);
        logoFrame.setBackgroundColor(Color.WHITE);

        logoImage = new ImageView(context);
        logoImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logoFrame.addView(logoImage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Couche des textes : décalée de la hauteur de la barre de statut
        contentLayer = new FrameLayout(context);
        contentLayer.setPadding(0, extraTop, 0, 0);
        logoFrame.addView(contentLayer, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        nameTv = text(shopNameValue, 35, onColor);
        nameTv.setGravity(Gravity.CENTER);
        nameTv.setPadding(dp(26), 0, dp(26), 0);
        nameTv.setMaxLines(2);
        nameTv.setEllipsize(TextUtils.TruncateAt.END);
        contentLayer.addView(nameTv, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        // Catégorie : centrée en haut, à la hauteur des boutons
        categoryTv = text(shopCategoryValue, 15, onColor);
        categoryTv.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams cat = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, topBtnH, Gravity.TOP);
        cat.topMargin = dp(4);
        contentLayer.addView(categoryTv, cat);

        // Bouton de gauche : Édit (vendeur) ou Retour (visiteur)
        FrameLayout left = makeTopButton();
        leftIconTv = makeIconText(ownerFlag ? iconEdit : iconBack, ownerFlag ? "✎" : "←");
        left.addView(leftIconTv, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ownerFlag) OnEditShopClick(shopUidValue);
                else OnBackClick();
            }
        });
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                topBtnW, topBtnH, Gravity.TOP | Gravity.START);
        lp.leftMargin = dp(8);
        lp.topMargin = dp(4);
        contentLayer.addView(left, lp);

        // Bouton de droite : Personne (ouvre le menu des infos), pour tout le monde
        final FrameLayout right = makeTopButton();
        personIconTv = makeIconText(iconPerson, "●");
        right.addView(personIconTv, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfoMenu(right);
            }
        });
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(
                topBtnW, topBtnH, Gravity.TOP | Gravity.END);
        rp.rightMargin = dp(8);
        rp.topMargin = dp(4);
        contentLayer.addView(right, rp);

        return logoFrame;
    }

    private void updateLogoExtra() {
        if (logoFrame != null && logoFrameParams != null) {
            logoFrameParams.height = logoHeightPx + extraTop;
            logoFrame.setLayoutParams(logoFrameParams);
        }
        if (contentLayer != null) contentLayer.setPadding(0, extraTop, 0, 0);
    }

    // =========================================================================
    // MENU DÉROULANT DES INFOS (icône personne) : adresse, téléphone, signaler
    // =========================================================================

    private LinearLayout infoRow(String icon, String label, int textColor, final Runnable action) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(20), dp(12));

        if (icon != null && !icon.isEmpty()) {
            TextView iv = new TextView(context);
            iv.setText(icon);
            iv.setTextSize(18);
            iv.setTextColor(Color.parseColor("#1A1A1B"));
            if (shopIconFont != null) iv.setTypeface(shopIconFont);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ip.setMargins(0, 0, dp(14), 0);
            row.addView(iv, ip);
        }

        TextView tv = text(label, 15, textColor);
        tv.setMaxLines(2);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setMaxWidth((int) (screenW * 0.62));
        row.addView(tv);

        if (action != null) {
            row.setClickable(true);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissInfoMenu();
                    action.run();
                }
            });
        }
        return row;
    }

    private void showInfoMenu(View anchor) {
        try {
            dismissInfoMenu();

            LinearLayout menu = new LinearLayout(context);
            menu.setOrientation(LinearLayout.VERTICAL);
            menu.setPadding(dp(4), dp(4), dp(4), dp(4));
            menu.setMinimumWidth((int) (screenW * 0.60));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(14));
            menu.setBackground(bg);
            menu.setElevation((float) dp(4));

            // Adresse
            if (!shopAddressValue.isEmpty()) {
                final String address = shopAddressValue;
                menu.addView(infoRow(iconAddress, address, Color.parseColor("#1A1A1B"), new Runnable() {
                    @Override
                    public void run() {
                        OnShopAddressClick(address);
                    }
                }));
            } else {
                menu.addView(infoRow(iconAddress, "Adresse non renseignée", ink(0x7E), null));
            }

            // Téléphone
            if (!shopPhoneValue.isEmpty()) {
                final String phone = shopPhoneValue;
                menu.addView(infoRow(iconPhone, phone, Color.parseColor("#1A1A1B"), new Runnable() {
                    @Override
                    public void run() {
                        OnShopPhoneClick(phone);
                    }
                }));
            } else {
                menu.addView(infoRow(iconPhone, "Numéro non renseigné", ink(0x7E), null));
            }

            // Signaler : jamais sur sa propre boutique
            if (!ownerFlag) {
                View sep = new View(context);
                sep.setBackgroundColor(Color.parseColor("#EDEDED"));
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                sp.setMargins(dp(10), dp(2), dp(10), dp(2));
                menu.addView(sep, sp);
                menu.addView(infoRow(iconReport, "Signaler la boutique",
                        Color.parseColor("#1A1A1B"), new Runnable() {
                            @Override
                            public void run() {
                                OnReportShopClick(shopUidValue);
                            }
                        }));
            }

            PopupWindow popup = new PopupWindow(menu,
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popup.setOutsideTouchable(true);
            popup.setElevation((float) dp(4));
            infoPopup = popup;
            popup.showAsDropDown(anchor, 0, dp(4), Gravity.END);
        } catch (Exception e) {
            OnError("Menu des infos: " + e.getMessage());
        }
    }

    private void dismissInfoMenu() {
        try {
            if (infoPopup != null && infoPopup.isShowing()) infoPopup.dismiss();
        } catch (Exception ignored) {
        }
        infoPopup = null;
    }

    // =========================================================================
    // SERVEUR : INFOS ET PRODUITS DE LA BOUTIQUE
    // =========================================================================

    private String authHeader(String a) {
        String t = a == null ? "" : a.trim();
        if (t.isEmpty()) return "";
        if (t.indexOf(' ') >= 0) return t;   // déjà complet (ex: « Bearer xxx »)
        return "Bearer " + t;                // jeton seul
    }

    private String withUid(String url, String uid) throws Exception {
        String enc = URLEncoder.encode(uid, "UTF-8");
        String u = url.trim();
        if (u.contains("{uid}")) return u.replace("{uid}", enc);
        return u + (u.contains("?") ? "&" : "?") + "uid=" + enc;
    }

    private String httpGetText(String target, String auth) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(target).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);
            c.setRequestProperty("Accept", "application/json");
            if (auth != null && !auth.isEmpty()) {
                c.setRequestProperty("Authorization", auth);
            }
            int code = c.getResponseCode();
            if (code >= 400) throw new Exception("serveur " + code);
            return new String(readAllBytes(c.getInputStream()), "UTF-8");
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private String pick(JSONObject o, String... keys) {
        for (int i = 0; i < keys.length; i++) {
            if (o.has(keys[i]) && !o.isNull(keys[i])) {
                String v = o.optString(keys[i], "").trim();
                if (!v.isEmpty() && !"null".equals(v)) return v;
            }
        }
        return "";
    }

    // Le serveur peut répondre directement l'objet, ou l'envelopper dans data / shop / boutique
    private JSONObject shopObject(String body) throws Exception {
        String t = body == null ? "" : body.trim();
        if (t.isEmpty() || "null".equals(t)) return null;
        JSONObject o = new JSONObject(t);
        if (!pick(o, "name", "nom", "shopName", "boutique").isEmpty()) return o;
        String[] wrap = {"data", "shop", "boutique", "infos", "info"};
        for (int i = 0; i < wrap.length; i++) {
            JSONObject n = o.optJSONObject(wrap[i]);
            if (n != null) return n;
        }
        return o;
    }

    // Retourne toujours un tableau JSON de produits (gère aussi la forme { id: produit } de Firebase)
    private String normalizeProducts(String body) throws Exception {
        String t = body == null ? "" : body.trim();
        if (t.isEmpty() || "null".equals(t)) return "[]";
        if (t.startsWith("[")) return t;
        JSONObject o = new JSONObject(t);
        String[] keys = {"products", "produits", "items", "articles", "data"};
        for (int i = 0; i < keys.length; i++) {
            Object v = o.opt(keys[i]);
            if (v instanceof JSONArray) return v.toString();
            if (v instanceof JSONObject) {
                o = (JSONObject) v;
                break;
            }
        }
        JSONArray out = new JSONArray();
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            Object v = o.opt(k);
            if (v instanceof JSONObject) {
                JSONObject p = (JSONObject) v;
                if (!p.has("uid")) p.put("uid", k);
                out.put(p);
            }
        }
        return out.toString();
    }

    @SimpleFunction(description = "Charge la boutique depuis le serveur. Appelle urlInfos et urlProduits avec ?uid= (ou à la place de {uid} dans l'adresse) : l'UID de la boutique donné à BuildShopHome. Infos attendues en JSON : name, category, logo, address, phone. Les infos remplissent la page (nom, catégorie, logo, menu) et OnShopInfoLoaded se déclenche. Les produits mettent à jour le compteur et OnShopProductsLoaded donne le JSON à brancher sur BuildProductGridFromJson. authorization : jeton (avec ou sans « Bearer »), vide si inutile. Laisse une adresse vide pour ne pas l'appeler. À appeler après BuildShopHome.")
    public void LoadShopFromServer(final String urlInfos, final String urlProduits,
                                   final String authorization) {
        String uid = shopUidValue;
        if (uid.isEmpty()) uid = prefs().getString(PREF_CURRENT_UID, "");
        if (uid.isEmpty()) {
            OnError("LoadShopFromServer: uid inconnu. Appelle SetShopUser puis BuildShopHome avant.");
            return;
        }
        final String shopUid = uid;
        final String auth = authHeader(authorization);

        if (urlInfos != null && !urlInfos.trim().isEmpty()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject o = shopObject(httpGetText(withUid(urlInfos, shopUid), auth));
                        if (o == null) throw new Exception("infos de la boutique vides");
                        final String name = pick(o, "name", "nom", "shopName", "boutique", "title");
                        final String category = pick(o, "category", "categorie", "catégorie", "cat");
                        final String logo = pick(o, "logo", "photo", "image", "logoUrl", "logo_url", "avatar");
                        final String address = pick(o, "address", "adresse", "location");
                        final String phone = pick(o, "phone", "telephone", "téléphone", "tel", "phoneNumber", "numero");
                        runOnUi(new Runnable() {
                            @Override
                            public void run() {
                                applyShopInfo(name, category, logo, address, phone);
                            }
                        });
                    } catch (Exception e) {
                        fail("LoadShopFromServer (infos): " + e.getMessage());
                    }
                }
            }).start();
        }

        if (urlProduits != null && !urlProduits.trim().isEmpty()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String json = normalizeProducts(
                                httpGetText(withUid(urlProduits, shopUid), auth));
                        final int n = new JSONArray(json).length();
                        runOnUi(new Runnable() {
                            @Override
                            public void run() {
                                showCount(n);
                                OnShopProductsLoaded(json);
                            }
                        });
                    } catch (Exception e) {
                        fail("LoadShopFromServer (produits): " + e.getMessage());
                    }
                }
            }).start();
        }
    }

    private void applyShopInfo(String name, String category, String logo,
                               String address, String phone) {
        if (!name.isEmpty()) shopNameValue = name;
        if (!category.isEmpty()) shopCategoryValue = category;
        if (!address.isEmpty()) shopAddressValue = address;
        if (!phone.isEmpty()) shopPhoneValue = phone;
        if (nameTv != null) nameTv.setText(shopNameValue);
        if (categoryTv != null) categoryTv.setText(shopCategoryValue);
        if (!logo.isEmpty()) SetShopLogoSource(logo);
        OnShopInfoLoaded(shopNameValue, shopCategoryValue, logo, shopAddressValue, shopPhoneValue);
    }

    // =========================================================================
    // LOGO DU VENDEUR
    // =========================================================================

    @SimpleFunction(description = "Charge le logo du vendeur dans le header : lien http(s)://, chemin de fichier (file://...) ou nom d'un fichier des Assets. Les couleurs du texte et de la barre de statut s'adaptent au logo (blanc sur logo sombre, sombre sur logo clair).")
    public void SetShopLogoSource(final String source) {
        if (source == null || source.trim().isEmpty()) {
            OnError("SetShopLogoSource: source vide.");
            return;
        }
        final String src = source.trim();
        logoSourceValue = src;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int maxW = context.getResources().getDisplayMetrics().widthPixels;
                    Bitmap bmp = loadBitmap(src, maxW);
                    if (bmp == null) {
                        fail("SetShopLogoSource: logo introuvable ou illisible (" + src
                                + "). Utilise un lien https, un fichier de l'appareil ou un fichier des Assets.");
                        return;
                    }
                    final Bitmap result = bmp;
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            logoBitmap = result;
                            applyLogoToView();
                            applyScroll();
                            OnShopLogoLoaded();
                        }
                    });
                } catch (Exception e) {
                    fail("SetShopLogoSource: " + e.getMessage());
                }
            }
        }).start();
    }

    private Bitmap loadBitmap(String src, int maxW) throws Exception {
        if (src.startsWith("http://") || src.startsWith("https://")) {
            return downloadBitmap(src, maxW);
        }
        if (src.startsWith("file://")) {
            return decodeFile(src.substring(7), maxW);
        }
        if (src.startsWith("/")) {
            return decodeFile(src, maxW);
        }
        // Nom de fichier simple : Assets de l'application (ex: world-flag.png)
        try {
            InputStream is = context.getAssets().open(src);
            return decodeBytes(readAllBytes(is), maxW);
        } catch (Exception ignored) {
        }
        // Companion : dossier des Assets sur le téléphone
        File f = new File(Environment.getExternalStorageDirectory(), "AppInventor/assets/" + src);
        if (f.exists()) return decodeFile(f.getAbsolutePath(), maxW);
        return null;
    }

    private byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        int total = 0;
        while ((n = is.read(buf)) > 0) {
            total += n;
            if (total > 12 * 1024 * 1024) throw new Exception("réponse trop lourde");
            bos.write(buf, 0, n);
        }
        is.close();
        return bos.toByteArray();
    }

    private Bitmap decodeBytes(byte[] data, int maxW) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);
        if (o.outWidth <= 0) return null;
        o.inSampleSize = sampleFor(o.outWidth, maxW);
        o.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, o);
    }

    private Bitmap decodeFile(String path, int maxW) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        if (o.outWidth <= 0) return null;
        o.inSampleSize = sampleFor(o.outWidth, maxW);
        o.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, o);
    }

    private Bitmap downloadBitmap(String url, int maxW) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            int code = c.getResponseCode();
            if (code >= 400) throw new Exception("serveur " + code);
            return decodeBytes(readAllBytes(c.getInputStream()), maxW);
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private int sampleFor(int width, int maxW) {
        int s = 1;
        while (width / (s * 2) >= maxW) s *= 2;
        return s;
    }

    // ---- analyse des couleurs (les zones transparentes comptent comme du blanc) ----

    private int overWhite(int c) {
        int a = Color.alpha(c);
        return Color.rgb(
                (Color.red(c) * a + 255 * (255 - a)) / 255,
                (Color.green(c) * a + 255 * (255 - a)) / 255,
                (Color.blue(c) * a + 255 * (255 - a)) / 255);
    }

    private int avgOverWhite(Bitmap src, int w, int h) {
        Bitmap small = Bitmap.createScaledBitmap(src, w, h, true);
        long r = 0;
        long g = 0;
        long b = 0;
        int n = 0;
        for (int x = 0; x < small.getWidth(); x++) {
            for (int y = 0; y < small.getHeight(); y++) {
                int c = overWhite(small.getPixel(x, y));
                r += Color.red(c);
                g += Color.green(c);
                b += Color.blue(c);
                n++;
            }
        }
        if (small != src) small.recycle();
        if (n == 0) return Color.WHITE;
        return Color.rgb((int) (r / n), (int) (g / n), (int) (b / n));
    }

    // Bande du haut telle qu'elle est affichée (center-crop)
    private int topStripColor(Bitmap bmp) {
        try {
            int bw = bmp.getWidth();
            int bh = bmp.getHeight();
            int sw = context.getResources().getDisplayMetrics().widthPixels;
            float scale = Math.max((float) sw / bw, (float) logoHeightPx / bh);
            float visH = logoHeightPx / scale;
            int y = (int) Math.max(0f, (bh - visH) / 2f);
            int stripH = Math.max(1, (int) (visH * 0.06f));
            stripH = Math.min(stripH, bh - y);
            Bitmap strip = Bitmap.createBitmap(bmp, 0, y, bw, stripH);
            int c = avgOverWhite(strip, 8, 2);
            if (strip != bmp) strip.recycle();
            return c;
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    private void applyLogoToView() {
        if (logoImage == null) return;
        if (logoBitmap == null) {
            topColor = Color.WHITE;
            topLum = 1.0;
            contentLum = 1.0;
        } else {
            logoImage.setImageBitmap(logoBitmap);
            contentLum = luminance(avgOverWhite(logoBitmap, 16, 16));
            topColor = topStripColor(logoBitmap);
            topLum = luminance(topColor);
        }
        // Texte blanc sur logo sombre, texte sombre sur logo clair ou blanc
        onColor = contentLum > 0.6 ? ink(0xFF) : Color.WHITE;
        if (nameTv != null) nameTv.setTextColor(onColor);
        if (categoryTv != null) categoryTv.setTextColor(onColor);
    }

    // =========================================================================
    // BARRE DE STATUT TRANSPARENTE
    // =========================================================================

    private void saveSystemBars() {
        if (barsSaved) return;
        try {
            Window w = activity.getWindow();
            origStatusColor = w.getStatusBarColor();
            origSysUiFlags = w.getDecorView().getSystemUiVisibility();
            barsSaved = true;
        } catch (Exception ignored) {
        }
    }

    private void applySystemBars() {
        try {
            Window w = activity.getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.setStatusBarColor(Color.TRANSPARENT);
            View d = w.getDecorView();
            int f = d.getSystemUiVisibility();
            int nf = f | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            if (nf != f) d.setSystemUiVisibility(nf);
        } catch (Exception ignored) {
        }
    }

    private void setStatusIcons(boolean dark) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        try {
            View d = activity.getWindow().getDecorView();
            int f = d.getSystemUiVisibility();
            int nf = dark ? (f | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
                    : (f & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            if (nf != f) d.setSystemUiVisibility(nf);
        } catch (Exception ignored) {
        }
    }

    private FrameLayout contentRoot() {
        View v = activity.findViewById(android.R.id.content);
        return v instanceof FrameLayout ? (FrameLayout) v : null;
    }

    private void ensureScrim() {
        if (statusScrim != null || statusBarH <= 0) return;
        FrameLayout root = contentRoot();
        if (root == null) return;
        statusScrim = new View(context);
        statusScrim.setBackgroundColor(Color.WHITE);
        statusScrim.setAlpha(0f);
        statusScrim.setElevation((float) dp(2));
        root.addView(statusScrim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, statusBarH, Gravity.TOP));
    }

    // Vue posée tout en haut de la fenêtre, derrière les icônes de statut
    private void ensureDecorBackdrop() {
        if (statusBarH <= 0) return;
        try {
            ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            if (decorBackdrop != null && decorBackdrop.getParent() == decor) return;
            decorBackdrop = new View(context);
            decorBackdrop.setBackgroundColor(topColor);
            decorBackdrop.setClickable(false);
            decorBackdrop.setOutlineProvider(null);
            decorBackdrop.setElevation((float) dp(24));
            decor.addView(decorBackdrop, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, statusBarH, Gravity.TOP));
        } catch (Exception e) {
            decorBackdrop = null;
        }
    }

    private void removeDecorBackdrop() {
        if (decorBackdrop == null) return;
        ViewGroup p = (ViewGroup) decorBackdrop.getParent();
        if (p != null) p.removeView(decorBackdrop);
        decorBackdrop = null;
    }

    private void removeScrim() {
        if (statusScrim == null) return;
        ViewGroup p = (ViewGroup) statusScrim.getParent();
        if (p != null) p.removeView(statusScrim);
        statusScrim = null;
    }

    // Le contenu démarre-t-il sous la barre (transparente) ou en dessous d'elle ?
    private void detectEdge() {
        if (scrollView == null || headerHolder == null) return;
        int[] loc = new int[2];
        scrollView.getLocationOnScreen(loc);
        int top = loc[1];
        boolean newEdge;
        int newExtra;
        if (top < statusBarH - dp(2)) {
            newEdge = true;
            newExtra = Math.max(0, statusBarH - top);
        } else {
            newEdge = false;
            newExtra = 0;
        }
        if (newEdge != edgeMode || newExtra != extraTop) {
            edgeMode = newEdge;
            extraTop = newExtra;
            updateLogoExtra();
            if (edgeMode) {
                ensureScrim();
                removeDecorBackdrop();
            } else {
                removeScrim();
            }
        }
        applyScroll();
    }

    private void attachScrollListener() {
        if (scrollView == null) return;
        scrollListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                applyScroll();
            }
        };
        scrollView.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
    }

    private void detachScrollListener() {
        try {
            if (scrollView != null && scrollListener != null
                    && scrollView.getViewTreeObserver().isAlive()) {
                scrollView.getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
            }
        } catch (Exception ignored) {
        }
        scrollListener = null;
    }

    // Quand le logo quitte le haut de l'écran, la zone de la barre passe au blanc de la page
    private void applyScroll() {
        if (scrollView == null) return;
        int y = scrollView.getScrollY();
        float range = Math.max(2f * statusBarH, (float) dp(48));
        float p = clamp01((y - (logoHeightPx - range / 2f)) / range);

        if (edgeMode) {
            if (statusScrim != null) statusScrim.setAlpha(p);
        } else {
            // Le contenu démarre sous la barre : on peint la zone de la barre avec la couleur du logo
            ensureDecorBackdrop();
            if (decorBackdrop != null) {
                decorBackdrop.setBackgroundColor(blend(topColor, Color.WHITE, p));
            }
        }
        double bgLum = topLum * (1f - p) + p;
        setStatusIcons(bgLum > 0.6);
    }

    @SimpleFunction(description = "À appeler en quittant la page boutique : retire le header, ferme le menu et remet la barre de statut d'origine.")
    public void ReleaseShopHome() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                detachScrollListener();
                dismissInfoMenu();
                removeScrim();
                removeDecorBackdrop();
                if (headerHolder != null && headerHolder.getParent() instanceof ViewGroup) {
                    ((ViewGroup) headerHolder.getParent()).removeView(headerHolder);
                }
                headerHolder = null;
                if (barsSaved) {
                    try {
                        Window w = activity.getWindow();
                        w.setStatusBarColor(origStatusColor);
                        w.getDecorView().setSystemUiVisibility(origSysUiFlags);
                    } catch (Exception ignored) {
                    }
                    barsSaved = false;
                }
                edgeMode = false;
                extraTop = 0;
            }
        });
    }

    // =========================================================================
    // SUPPRESSION D'UN PRODUIT
    // =========================================================================

    private String deleteUrlValue = "";
    private String deleteAuthValue = "";
    private boolean deleting = false;

    @SimpleFunction(description = "Définit une fois l'adresse du serveur qui supprime un produit, et le jeton (avec ou sans « Bearer »). L'adresse peut contenir {uid} (uid du vendeur) et {productUid} (uid du produit), par exemple https://serveur/{uid}/products/{productUid}. Sans ces repères, ?uid=...&productUid=... est ajouté à la fin. La suppression est envoyée avec la méthode DELETE.")
    public void SetProductDeleteUrl(String url, String authorization) {
        deleteUrlValue = url == null ? "" : url.trim();
        deleteAuthValue = authHeader(authorization);
    }

    private boolean deleteBodyOk(String body) {
        try {
            JSONObject o = new JSONObject(body.trim());
            if (o.has("success")) return o.optBoolean("success");
            if (o.has("error")) return false;
            return true;
        } catch (Exception e) {
            return true; // réponse sans JSON : le code HTTP décide
        }
    }

    @SimpleFunction(description = "Supprime un produit sur le serveur. productUid : l'uid du produit (donné par ManaplaceUtils.OnProductCardClick). sellerUid : l'uid Firebase du vendeur, sous lequel se trouve le produit. Refusé si sellerUid n'est pas l'utilisateur connecté (SetShopUser). Résultat : OnProductDeleted ou OnProductDeleteFailed. Demande toujours une confirmation avant, la suppression est définitive. Appelle SetProductDeleteUrl avant.")
    public void DeleteProduct(final String productUid, final String sellerUid) {
        final String pUid = productUid == null ? "" : productUid.trim();
        final String sUid = sellerUid == null ? "" : sellerUid.trim();
        if (deleteUrlValue.isEmpty()) {
            OnError("DeleteProduct: appelle SetProductDeleteUrl avant.");
            return;
        }
        if (pUid.isEmpty() || sUid.isEmpty()) {
            OnError("DeleteProduct: l'uid du produit ou du vendeur est vide.");
            return;
        }
        if (!isOwner(sUid)) {
            OnProductDeleteFailed(0, "Suppression refusée : ce vendeur n'est pas l'utilisateur connecté (appelle SetShopUser).");
            return;
        }
        if (deleting) return;
        deleting = true;

        final String baseUrl = deleteUrlValue;
        final String auth = deleteAuthValue;
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection c = null;
                try {
                    String encSeller = URLEncoder.encode(sUid, "UTF-8");
                    String encProduct = URLEncoder.encode(pUid, "UTF-8");
                    String target;
                    if (baseUrl.contains("{uid}") || baseUrl.contains("{productUid}")) {
                        target = baseUrl.replace("{uid}", encSeller).replace("{productUid}", encProduct);
                    } else {
                        target = baseUrl + (baseUrl.contains("?") ? "&" : "?")
                                + "uid=" + encSeller + "&productUid=" + encProduct;
                    }
                    c = (HttpURLConnection) new URL(target).openConnection();
                    c.setRequestMethod("DELETE");
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(15000);
                    c.setRequestProperty("Accept", "application/json");
                    if (!auth.isEmpty()) c.setRequestProperty("Authorization", auth);

                    final int code = c.getResponseCode();
                    InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
                    final String body = is == null ? "" : new String(readAllBytes(is), "UTF-8");
                    // 404 : le produit n'existe déjà plus, le résultat voulu est atteint
                    final boolean ok = (code >= 200 && code < 300 && deleteBodyOk(body)) || code == 404;
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            deleting = false;
                            if (ok) {
                                showCount(Math.max(0, articleCount - 1));
                                OnProductDeleted(pUid);
                            } else {
                                OnProductDeleteFailed(code, body);
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            deleting = false;
                            OnProductDeleteFailed(0, "Réseau : " + e.getMessage());
                        }
                    });
                } finally {
                    if (c != null) c.disconnect();
                }
            }
        }).start();
    }

    // =========================================================================
    // COMPTEUR D'ARTICLES
    // =========================================================================

    private String countLabel(int n) {
        return n + (n > 1 ? " Articles" : " Article");
    }

    private void showCount(final int n) {
        articleCount = n;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if (countTv != null) countTv.setText(countLabel(n));
            }
        });
    }

    private int countProducts(String json) throws Exception {
        String t = json == null ? "" : json.trim();
        if (t.isEmpty()) return 0;
        if (t.startsWith("[")) return new JSONArray(t).length();
        JSONObject o = new JSONObject(t);
        String[] keys = {"products", "produits", "items", "articles", "data"};
        for (int i = 0; i < keys.length; i++) {
            Object v = o.opt(keys[i]);
            if (v instanceof JSONArray) return ((JSONArray) v).length();
            if (v instanceof JSONObject) return ((JSONObject) v).length();
        }
        return o.length();
    }

    @SimpleFunction(description = "Compte les produits du JSON de la grille (le même que BuildProductGridFromJson), affiche « N Article(s) » dans la petite carte et retourne N.")
    public int UpdateArticleCountFromJson(String jsonData) {
        try {
            int n = countProducts(jsonData);
            showCount(n);
            return n;
        } catch (Exception e) {
            OnError("UpdateArticleCountFromJson: JSON invalide (" + e.getMessage() + ")");
            return 0;
        }
    }

    @SimpleFunction(description = "Affiche directement un nombre d'articles dans la petite carte.")
    public void SetArticleCount(int count) {
        showCount(Math.max(0, count));
    }

    @SimpleFunction(description = "Retourne le nombre d'articles actuellement affiché.")
    public int GetArticleCount() {
        return articleCount;
    }

    // =========================================================================
    // 17. FORMULAIRE « AJOUTER UN PRODUIT » (ex-Manatest 2/5)
    //     Renommés pour éviter les conflits : OpenProductPhotoPicker (au lieu de
    //     OpenPhotoPicker) et OnProductPhotosPicked (au lieu de OnPhotoPicked).
    // =========================================================================

    private static final String PRODUCT_PREFS_NAME = "ManatestProductForm";
    private static final String PREF_DRAFT = "draft";

    private static final int MAX_TITLE = 80;
    private static final int MAX_DESCRIPTION = 1000;
    private static final int MAX_SHORT = 30;
    private static final int MAX_IMAGES = 5;
    private static final int MAX_STOCK = 999999;


    // Vraies valeurs (séparées de l'affichage) : clé du champ -> valeur
    private final Map<String, String> values = new LinkedHashMap<String, String>();

    // Photos en cours de sélection (pas encore validées) et état
    private final List<String> pending = new ArrayList<String>();
    private boolean dirty = false;
    private final Image[] boundImages = new Image[MAX_IMAGES];
    private final Map<String, Bitmap> thumbs = new HashMap<String, Bitmap>();
    private int productPickRequestCode;
    private int fileCounter = 0;

    private ViewGroup formContainer;
    private EditText titleEdit;
    private boolean updatingTitle = false;

    private Typeface iconFont;
    private String iconPlus = "";
    private String iconMinus = "";
    private String iconPhoto = "";
    private String iconStore = "";
    private String iconTruck = "";
    private int iconSize = 22;
    private Typeface customBoldFont;
    private String invalidKey = "";

    // =========================================================================
    // OUTILS INTERNES
    // =========================================================================

    private int col(String hex) {
        return Color.parseColor(hex);
    }

    // #1A1A1B + alpha C0 (écriture RRGGBBAA)
    private int cText() {
        return Color.argb(0xC0, 0x1A, 0x1A, 0x1B);
    }

    // #1A1A1B + alpha E9 (écriture RRGGBBAA)
    private int cHead() {
        return Color.argb(0xE9, 0x1A, 0x1A, 0x1B);
    }

    private boolean has(String key) {
        String v = values.get(key);
        return v != null && !v.isEmpty();
    }

    private String get(String key) {
        String v = values.get(key);
        return v == null ? "" : v;
    }

    // Accepte un vrai caractère Phosphor, ou un code hexadécimal (« e3d2 », « \ue3d2 », « U+E3D2 », « &#xE3D2; »)
    private String glyph(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.isEmpty()) return "";
        String h = t.replaceFirst("(?i)^(\\\\u|u\\+|&#x|0x)", "").replace(";", "");
        if (h.length() >= 4 && h.length() <= 6 && h.matches("[0-9a-fA-F]+")) {
            try {
                return new String(Character.toChars(Integer.parseInt(h, 16)));
            } catch (Exception ignored) {
            }
        }
        return t;
    }

    // Retrouve la vraie vue où ajouter les lignes (déroule les enveloppes d'arrangement / défilement)
    private ViewGroup realLayout(AndroidViewComponent component) {
        if (component == null) return null;
        View v = component.getView();
        for (int i = 0; i < 3; i++) {
            if (v instanceof ScrollView || v instanceof HorizontalScrollView) {
                ViewGroup sv = (ViewGroup) v;
                if (sv.getChildCount() > 0 && sv.getChildAt(0) instanceof ViewGroup) {
                    v = sv.getChildAt(0);
                    continue;
                }
                break;
            }
            if (v instanceof FrameLayout) {
                ViewGroup fl = (ViewGroup) v;
                if (fl.getChildCount() == 1 && fl.getChildAt(0) instanceof ViewGroup) {
                    v = fl.getChildAt(0);
                    continue;
                }
            }
            break;
        }
        return v instanceof ViewGroup ? (ViewGroup) v : null;
    }

    private Typeface loadFont(String fontPath) {
        return loadFontNamed(fontPath, "BuildProductForm");
    }

    private Typeface loadFontNamed(String fontPath, String who) {
        if (fontPath == null || fontPath.trim().isEmpty()) return null;
        Typeface t = tryLoadTypeface(fontPath.trim());
        if (t == null) {
            OnError(who + ": police introuvable (" + fontPath + ").");
        }
        return t;
    }

    // =========================================================================
    // ÉVÉNEMENTS
    // =========================================================================

    @SimpleEvent(description = "L'utilisateur a touché une ligne du formulaire. id : description, category, price, variants, condition, color, size, stock (bouton Modifier), address ou delivery. Ouvre le panneau de saisie correspondant. (Le titre se saisit directement dans le formulaire, et les photos s'ouvrent toutes seules.)")
    public void OnProductFormFieldClick(String id) {
        EventDispatcher.dispatchEvent(this, "OnProductFormFieldClick", id);
    }

    @SimpleEvent(description = "Déclenché après sélection : les photos sont validées (5 photos atteintes, ou bouton OK touché). photosJson : liste JSON de liens file:// ; count : nombre de photos.")
    public void OnProductPhotosPicked(String photosJson, int count) {
        EventDispatcher.dispatchEvent(this, "OnProductPhotosPicked", photosJson, count);
    }

    @SimpleEvent(description = "Le titre a changé pendant la saisie directe dans le formulaire.")
    public void OnProductTitleChanged(String title) {
        EventDispatcher.dispatchEvent(this, "OnProductTitleChanged", title);
    }

    @SimpleEvent(description = "Le stock a changé avec les boutons plus / moins.")
    public void OnProductStockChanged(int stock) {
        EventDispatcher.dispatchEvent(this, "OnProductStockChanged", stock);
    }

    @SimpleEvent(description = "Le serveur a répondu à SendProductForm. responseCode : code HTTP (200, 201 = succès), response : texte renvoyé par le serveur.")
    public void OnProductSent(int responseCode, String response) {
        EventDispatcher.dispatchEvent(this, "OnProductSent", responseCode, response);
    }

    @SimpleEvent(description = "Une valeur vient d'être enregistrée (SetProductFormValue ou boutons plus/moins du stock). fieldKey : le champ, value : la valeur enregistrée. Sert à afficher ou utiliser la réponse.")
    public void OnProductFormValueSaved(String fieldKey, String value) {
        EventDispatcher.dispatchEvent(this, "OnProductFormValueSaved", fieldKey, value);
    }

    @SimpleEvent(description = "ValidateProductForm a trouvé un champ invalide. La ligne concernée passe en rouge.")
    public void OnProductFormInvalid(String fieldKey, String message) {
        EventDispatcher.dispatchEvent(this, "OnProductFormInvalid", fieldKey, message);
    }

    // =========================================================================
    // CONSTRUCTION DU FORMULAIRE
    // =========================================================================

    @SimpleFunction(description = "Construit le formulaire « Ajouter un produit » dans l'arrangement donné. fontPath : police Phosphor (ex: Phosphor-Bold.ttf). plusChar : icône « + » . minusChar : icône « - » du stock. photoChar : icône photo. storeChar : icône adresse. truckChar : icône livraison. Caractère Phosphor ou code hexadécimal (ex: e3d2). Vide = pas d'icône. iconSize : taille des icônes UNIQUEMENT, le texte n'est pas touché (ex: 22).")
    public void BuildProductForm(
            final AndroidViewComponent container,
            final String fontPath,
            final String plusChar,
            final String minusChar,
            final String photoChar,
            final String storeChar,
            final String truckChar,
            final int iconSize) {
        if (container == null || container.getView() == null) {
            OnError("BuildProductForm: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup target = realLayout(container);
                    if (target == null) {
                        OnError("BuildProductForm: conteneur invalide.");
                        return;
                    }
                    formContainer = target;
                    iconFont = loadFont(fontPath);
                    iconPlus = glyph(plusChar);
                    iconMinus = glyph(minusChar);
                    iconPhoto = glyph(photoChar);
                    iconStore = glyph(storeChar);
                    iconTruck = glyph(truckChar);
                    ManaplaceUtils.this.iconSize = iconSize > 0 ? iconSize : 22;
                    loadPendingFromValues();
                    render();
                } catch (Exception e) {
                    OnError("BuildProductForm: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Charge (facultatif) la police des grands titres et du gras (ex: Manrope-Bold.ttf), pour le formulaire produit et le formulaire « Créer ma boutique ». Sans elle, les grands titres utilisent la police normale en gras.")
    public void LoadCustomBoldFont(String fontPath) {
        customBoldFont = loadFontNamed(fontPath, "LoadCustomBoldFont");
        applyFonts();
    }

    private void applyFonts() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if (titleEdit != null) {
                    Typeface f = shopFont();
                    titleEdit.setTypeface(f != null ? f : Typeface.DEFAULT);
                }
                if (formContainer != null) render();
                if (shopFormContainer != null && !IsSeller()) renderShopForm();
                rerenderAll();
            }
        });
    }

    private void render() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    if (formContainer == null) return;
                    formContainer.removeAllViews();

                    addMediaSection();
                    addTitleRow();
                    addDivider();
                    addIconTextRow("description", iconPlus, "Description du produit", 2);
                    addDivider();
                    addIconTextRow("category", iconPlus, "Choisir une catégorie", 1);
                    addDivider();
                    addPriceRow();
                    addSeparator();
                    addVariantsSection();
                    addSeparator();
                    addStockSection();
                    addSeparator();
                    addAddressSection();
                    addSeparator();
                    addDeliveryRow();
                    addSeparator();
                } catch (Exception e) {
                    OnError("render: " + e.getMessage());
                }
            }
        });
    }

    // ---- éléments de base ----

    private LinearLayout rowBase(int left, int top, int right, int bottom, boolean horizontal) {
        LinearLayout r = new LinearLayout(context);
        r.setOrientation(horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        r.setPadding(dp(left), dp(top), dp(right), dp(bottom));
        return r;
    }

    private TextView text(String s, int sp, int color, boolean bold, int maxLines) {
        TextView t = new TextView(context);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        Typeface cf = shopFont();
        if (bold) {
            if (customBoldFont != null) {
                t.setTypeface(customBoldFont);
            } else if (cf != null) {
                t.setTypeface(Typeface.create(cf, Typeface.BOLD));
            } else {
                t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            }
        } else if (cf != null) {
            t.setTypeface(cf);
        }
        if (maxLines > 0) {
            t.setMaxLines(maxLines);
            t.setEllipsize(TextUtils.TruncateAt.END);
        }
        return t;
    }

    private void weight(View v) {
        v.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    }

    private TextView icon(String ch, int sp, int color, int marginRightDp) {
        if (iconFont == null || ch == null || ch.isEmpty()) return null;
        TextView t = new TextView(context);
        t.setText(ch);
        t.setTypeface(iconFont);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) dp(sp));
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, dp(marginRightDp), 0);
        t.setLayoutParams(p);
        return t;
    }

    private void clickable(View v, final String key) {
        v.setClickable(true);
        try {
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground, tv, true)) {
                v.setBackgroundResource(tv.resourceId);
            }
        } catch (Exception ignored) {
        }
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                OnProductFormFieldClick(key);
            }
        });
    }

    private void addDivider() {
        View d = new View(context);
        d.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        d.setBackgroundColor(col("#EEEEEE"));
        formContainer.addView(d);
    }

    private void addSeparator() {
        View d = new View(context);
        d.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(8)));
        d.setBackgroundColor(col("#F2F2F2"));
        formContainer.addView(d);
    }

    private int valueColor(String key) {
        if (key.equals(invalidKey)) return col("#D93025");
        return has(key) ? cText() : col("#6E6E73");
    }

    private int iconColor() {
        return col("#1A1A1B");
    }

    // ---- section multimédia ----

    private Bitmap thumb(String path) {
        Bitmap cached = thumbs.get(path);
        if (cached != null) return cached;
        try {
            String p = path.startsWith("file://") ? path.substring(7) : path;
            if (!new File(p).exists()) return null;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(p, o);
            int s = 1;
            while ((o.outWidth / (s * 2)) >= 400 && (o.outHeight / (s * 2)) >= 400) {
                s *= 2;
            }
            o.inJustDecodeBounds = false;
            o.inSampleSize = s;
            Bitmap b = BitmapFactory.decodeFile(p, o);
            if (b != null) thumbs.put(path, b);
            return b;
        } catch (Throwable t) {
            return null;
        }
    }

    // Taille des tuiles photo : ~34 % de la largeur de l'écran (entre 100 et 180 dp)
    private int tileSize() {
        int w = context.getResources().getDisplayMetrics().widthPixels;
        return Math.max(dp(100), Math.min(dp(180), (int) (w * 0.34f)));
    }

    private LinearLayout tileBase(int fillColor, int strokeColor) {
        LinearLayout tile = new LinearLayout(context);
        tile.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileSize(), tileSize());
        lp.setMargins(0, 0, dp(12), 0);
        tile.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fillColor);
        bg.setCornerRadius(dp(20));
        if (strokeColor != 0) {
            bg.setStroke(dp(1), strokeColor);
        }
        tile.setBackground(bg);
        return tile;
    }

    private void addMediaSection() {
        LinearLayout head = rowBase(22, 16, 22, 10, true);
        head.addView(text("Contenu multimédia", 18, cHead(), true, 1));
        TextView count = text("  (" + pending.size() + "/" + MAX_IMAGES + ")", 18,
                col("#6E6E73"), false, 1);
        head.addView(count);

        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        head.addView(spacer);

        if (dirty) {
            TextView ok = text("OK", 18, Color.WHITE, true, 1);
            ok.setGravity(Gravity.CENTER);
            ok.setPadding(dp(22), dp(6), dp(22), dp(6));
            GradientDrawable okBg = new GradientDrawable();
            okBg.setColor(col("#3949AB"));
            okBg.setCornerRadius(dp(20));
            ok.setBackground(okBg);
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ConfirmProductPhotos();
                }
            });
            head.addView(ok);
        }
        formContainer.addView(head);

        HorizontalScrollView hs = new HorizontalScrollView(context);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout strip = new LinearLayout(context);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setPadding(dp(22), dp(4), dp(10), dp(4));

        View.OnClickListener pick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenProductPhotoPicker();
            }
        };

        if (pending.isEmpty()) {
            // Grande tuile grise avec l'icône photo
            LinearLayout ph = tileBase(col("#F5F5F5"), 0);
            TextView ic = icon(iconPhoto, iconSize + 12, col("#9A9AA0"), 0);
            if (ic != null) ph.addView(ic);
            ph.setOnClickListener(pick);
            strip.addView(ph);
        } else {
            for (int i = 0; i < pending.size(); i++) {
                strip.addView(filledTile(i, pending.get(i)));
            }
        }

        if (pending.size() < MAX_IMAGES) {
            // Petit carré blanc avec « + » au milieu
            LinearLayout plus = tileBase(Color.WHITE, col("#EEEEEE"));
            TextView ic = icon(iconPlus, iconSize + 6, col("#6E6E73"), 0);
            if (ic != null) {
                plus.addView(ic);
            } else {
                TextView fb = text("+", 28, col("#6E6E73"), false, 1);
                plus.addView(fb);
            }
            plus.setOnClickListener(pick);
            strip.addView(plus);
        }

        hs.addView(strip);
        formContainer.addView(hs);

        TextView hint = text(pending.isEmpty() ? "Ajouter des photos"
                : "Maintiens une photo pour la retirer",
                18, col("#6E6E73"), false, 1);
        hint.setGravity(Gravity.CENTER);
        hint.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        hint.setPadding(dp(16), dp(12), dp(16), dp(18));
        formContainer.addView(hint);
    }

    private View filledTile(final int index, String path) {
        ImageView iv = new ImageView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileSize(), tileSize());
        lp.setMargins(0, 0, dp(12), 0);
        iv.setLayoutParams(lp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackgroundColor(col("#F5F5F5"));
        iv.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(20));
            }
        });
        iv.setClipToOutline(true);
        Bitmap b = thumb(path);
        if (b != null) {
            iv.setImageBitmap(b);
        }
        iv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (index >= 0 && index < pending.size()) {
                    pending.remove(index);
                    dirty = true;
                    render();
                }
                return true;
            }
        });
        return iv;
    }

    // ---- ouverture du sélecteur de photos (jusqu'à 5) ----

    @SimpleFunction(description = "Ouvre la galerie d'images native (avec demande de permission) pour choisir des photos, 5 au maximum au total. Appelée automatiquement quand on touche une tuile photo. OnProductPhotosPicked se déclenche quand les 5 photos sont atteintes ou quand l'utilisateur touche OK.")
    public void OpenProductPhotoPicker() {
        if (MAX_IMAGES - pending.size() <= 0) {
            OnError("Maximum " + MAX_IMAGES + " photos.");
            return;
        }
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? "android.permission.READ_MEDIA_IMAGES"
                : "android.permission.READ_EXTERNAL_STORAGE";

        form.askPermission(permission, new PermissionResultHandler() {
            @Override
            public void HandlePermissionResponse(String permissionName, boolean granted) {
                if (!granted) {
                    OnError("Permission refusée.");
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                            i.setType("image/*");
                            i.addCategory(Intent.CATEGORY_OPENABLE);
                            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                            form.startActivityForResult(i, productPickRequestCode);
                        } catch (Exception e) {
                            OnError("OpenProductPhotoPicker: " + e.getMessage());
                        }
                    }
                });
            }
        });
    }

    // Appelée par resultReturned (aiguillage par code de retour)
    private void productPhotosResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != productPickRequestCode || resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        final List<Uri> uris = new ArrayList<Uri>();
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri u = clip.getItemAt(i).getUri();
                if (u != null) uris.add(u);
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        if (uris.isEmpty()) return;

        int remaining = MAX_IMAGES - pending.size();
        while (uris.size() > remaining) {
            uris.remove(uris.size() - 1);
        }
        if (remaining <= 0) {
            OnError("Maximum " + MAX_IMAGES + " photos.");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<String> done = new ArrayList<String>();
                for (int i = 0; i < uris.size(); i++) {
                    String p = copyToCache(uris.get(i));
                    if (p != null) done.add(p);
                }
                runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < done.size(); i++) {
                            if (pending.size() >= MAX_IMAGES) break;
                            pending.add(done.get(i));
                        }
                        dirty = true;
                        render();
                        if (pending.size() >= MAX_IMAGES) {
                            ConfirmProductPhotos();
                        }
                    }
                });
            }
        }).start();
    }

    private String copyToCache(Uri uri) {
        try {
            File dir = new File(context.getCacheDir(), "manatest_photos");
            dir.mkdirs();
            File f = new File(dir, "p_" + System.currentTimeMillis() + "_" + (fileCounter++) + ".jpg");
            InputStream in = context.getContentResolver().openInputStream(uri);
            if (in == null) return null;
            FileOutputStream out = new FileOutputStream(f);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            } finally {
                try { in.close(); } catch (Exception ignored) { }
                try { out.close(); } catch (Exception ignored) { }
            }
            return "file://" + f.getAbsolutePath();
        } catch (Exception e) {
            OnError("Photo: " + e.getMessage());
            return null;
        }
    }

    @SimpleFunction(description = "Valide les photos choisies (équivaut au bouton OK). Enregistre la liste, met à jour les composants Image reliés et déclenche OnProductPhotosPicked.")
    public void ConfirmProductPhotos() {
        try {
            JSONArray a = new JSONArray(pending);
            if (pending.isEmpty()) {
                values.remove("images");
            } else {
                values.put("images", a.toString());
            }
            dirty = false;
            saveDraft();
            bindImages();
            render();
            OnProductPhotosPicked(a.toString(), pending.size());
        } catch (Exception e) {
            OnError("ConfirmProductPhotos: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Relie jusqu'à 5 vrais composants Image Kodular : leur Picture est mise à jour avec les photos validées (image1 = première photo, etc.). Un composant vide est ignoré.")
    public void BindProductImages(Image image1, Image image2, Image image3, Image image4, Image image5) {
        boundImages[0] = image1;
        boundImages[1] = image2;
        boundImages[2] = image3;
        boundImages[3] = image4;
        boundImages[4] = image5;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                bindImages();
            }
        });
    }

    private void bindImages() {
        List<String> ok = confirmedImages();
        for (int i = 0; i < MAX_IMAGES; i++) {
            Image img = boundImages[i];
            if (img == null) continue;
            try {
                img.Picture(i < ok.size() ? ok.get(i) : "");
            } catch (Exception e) {
                OnError("BindProductImages: " + e.getMessage());
            }
        }
    }

    private List<String> confirmedImages() {
        List<String> l = new ArrayList<String>();
        try {
            if (has("images")) {
                JSONArray a = new JSONArray(get("images"));
                for (int i = 0; i < a.length() && i < MAX_IMAGES; i++) {
                    String s = a.optString(i, "");
                    if (!s.isEmpty()) l.add(s);
                }
            }
        } catch (Exception ignored) {
        }
        return l;
    }

    private void loadPendingFromValues() {
        pending.clear();
        pending.addAll(confirmedImages());
        dirty = false;
    }

    @SimpleFunction(description = "Retourne la liste JSON des photos validées (liens file://).")
    public String GetProductFormImages() {
        return new JSONArray(confirmedImages()).toString();
    }

    @SimpleFunction(description = "Retourne le lien de la photo validée à la position index (1 à 5), ou \"\".")
    public String GetProductFormImage(int index) {
        List<String> l = confirmedImages();
        return (index >= 1 && index <= l.size()) ? l.get(index - 1) : "";
    }

    @SimpleFunction(description = "Retourne le nombre de photos validées.")
    public int GetProductFormImageCount() {
        return confirmedImages().size();
    }

    // ---- titre : saisie directe ----

    private void createTitleEdit() {
        titleEdit = new EditText(context);
        titleEdit.setHint("Titre du produit");
        titleEdit.setHintTextColor(cText());   // #1A1A1B + alpha C0
        titleEdit.setTextSize(22);
        titleEdit.setTextColor(cHead());   // #1A1A1B + alpha E9
        Typeface titleFont = shopFont();
        titleEdit.setTypeface(titleFont != null ? titleFont : Typeface.DEFAULT);
        titleEdit.setBackground(null);
        titleEdit.setSingleLine(true);
        titleEdit.setPadding(0, dp(10), 0, dp(10));
        titleEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_TITLE)});
        titleEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        titleEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                if (updatingTitle) return;
                String v = e.toString().trim();
                if (v.isEmpty()) {
                    values.remove("title");
                } else {
                    values.put("title", v);
                }
                if (invalidKey.equals("title")) {
                    invalidKey = "";
                }
                titleEdit.setTextColor(cHead());
                saveDraft();
                OnProductTitleChanged(v);
            }
        });
        titleEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent ev) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    try {
                        InputMethodManager imm = (InputMethodManager)
                                context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    } catch (Exception ignored) {
                    }
                    v.clearFocus();
                    return true;
                }
                return false;
            }
        });
    }

    private void syncTitle() {
        if (titleEdit == null) return;
        String want = get("title");
        if (!titleEdit.getText().toString().trim().equals(want)) {
            updatingTitle = true;
            titleEdit.setText(want);
            titleEdit.setSelection(titleEdit.getText().length());
            updatingTitle = false;
        }
    }

    private void addTitleRow() {
        if (titleEdit == null) createTitleEdit();
        syncTitle();
        ViewGroup old = (ViewGroup) titleEdit.getParent();
        if (old != null) old.removeView(titleEdit);

        LinearLayout row = rowBase(22, 12, 16, 12, true);
        titleEdit.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        titleEdit.setTextColor(invalidKey.equals("title") ? col("#D93025") : cHead());
        row.addView(titleEdit);
        formContainer.addView(row);
    }

    // ---- lignes ----

    private void addIconTextRow(String key, String iconChar, String placeholder, int maxLines) {
        LinearLayout row = rowBase(22, 22, 16, 22, true);
        TextView ic = has(key) ? null : icon(iconChar, iconSize, iconColor(), 14);
        if (ic != null) {
            row.addView(ic);
        }
        int vc = valueColor(key);
        if (key.equals("category") && has(key) && !key.equals(invalidKey)) {
            vc = col("#6E3FC4");
        }
        TextView t = text(has(key) ? get(key) : placeholder, 18, vc, false, maxLines);
        weight(t);
        row.addView(t);
        clickable(row, key);
        formContainer.addView(row);
    }

    private void addPriceRow() {
        LinearLayout row = rowBase(22, 22, 16, 22, true);
        String shown = has("price") ? formatPrice(get("price")) : "0,00 HTG";
        TextView t = text(shown, 18, valueColor("price"), false, 1);
        weight(t);
        row.addView(t);
        clickable(row, "price");
        formContainer.addView(row);
    }

    private void addVariantsSection() {
        LinearLayout head = rowBase(22, 18, 16, 6, false);
        head.addView(text("État et variantes", 18, cHead(), true, 1));
        formContainer.addView(head);

        boolean any = has("condition") || has("color") || has("size");
        if (!any) {
            LinearLayout row = rowBase(22, 16, 16, 24, true);
            TextView ic = icon(iconPlus, iconSize, iconColor(), 14);
            if (ic != null) {
                row.addView(ic);
            }
            TextView t = text("Plus d'options (neuf, couleur...)", 18, col("#6E6E73"), false, 1);
            weight(t);
            row.addView(t);
            clickable(row, "variants");
            formContainer.addView(row);
            return;
        }

        String[][] lines = {
                {"condition", "État"},
                {"color", "Couleur"},
                {"size", "Taille"}
        };
        for (int i = 0; i < lines.length; i++) {
            String key = lines[i][0];
            LinearLayout row = rowBase(22, 8, 22, i == lines.length - 1 ? 16 : 8, true);
            TextView label = text(lines[i][1], 18, cText(), false, 1);
            weight(label);
            row.addView(label);
            TextView value = text(has(key) ? get(key) : "—", 18, valueColor(key), false, 1);
            value.setGravity(Gravity.END);
            value.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(value);
            clickable(row, key);
            formContainer.addView(row);
        }
    }

    // ---- stock ----

    private int currentStock() {
        try {
            return has("stock") ? Integer.parseInt(get("stock")) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void changeStock(int delta) {
        int n = Math.max(0, Math.min(MAX_STOCK, currentStock() + delta));
        values.put("stock", String.valueOf(n));
        if (invalidKey.equals("stock")) invalidKey = "";
        saveDraft();
        render();
        OnProductStockChanged(n);
        OnProductFormValueSaved("stock", String.valueOf(n));
    }

    private TextView stepButton(String glyphChar, String fallback, final int delta) {
        TextView b = new TextView(context);
        b.setGravity(Gravity.CENTER);
        if (iconFont != null && glyphChar != null && !glyphChar.isEmpty()) {
            b.setText(glyphChar);
            b.setTypeface(iconFont);
            b.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) dp(iconSize));
            b.setIncludeFontPadding(false);
        } else {
            b.setText(fallback);
            b.setTextSize(26);
            b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        b.setTextColor(iconColor());
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setClickable(true);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeStock(delta);
            }
        });
        return b;
    }

    private void addStockSection() {
        LinearLayout head = rowBase(22, 18, 22, 6, true);
        TextView title = text("Stock", 18, cHead(), true, 1);
        weight(title);
        head.addView(title);
        TextView modify = text("Modifier", 18, col("#3949AB"), false, 1);
        modify.setPadding(dp(8), dp(4), 0, dp(4));
        modify.setClickable(true);
        modify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnProductFormFieldClick("stock");
            }
        });
        head.addView(modify);
        formContainer.addView(head);

        LinearLayout row = rowBase(22, 6, 12, 18, true);
        TextView label = text("Disponible", 18, cText(), false, 1);
        weight(label);
        row.addView(label);

        row.addView(stepButton(iconMinus, "−", -1));

        TextView box = text(String.valueOf(currentStock()), 18, valueColor("stock"), false, 1);
        box.setGravity(Gravity.CENTER);
        box.setMinWidth(dp(90));
        box.setMinHeight(dp(42));
        box.setPadding(dp(16), dp(6), dp(16), dp(6));
        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(col("#F5F5F5"));
        boxBg.setCornerRadius(dp(12));
        box.setBackground(boxBg);
        row.addView(box);

        row.addView(stepButton(iconPlus, "+", 1));
        formContainer.addView(row);
    }

    // ---- adresse / livraison ----

    private void addAddressSection() {
        LinearLayout row = rowBase(22, 18, 22, 18, false);
        row.setGravity(Gravity.START);

        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView ic = icon(iconStore, iconSize, iconColor(), 14);
        if (ic != null) {
            head.addView(ic);
        }
        head.addView(text("Adresse", 18, cHead(), true, 1));
        row.addView(head);

        TextView value = text(has("address") ? get("address") : "Ajouter une adresse", 18,
                valueColor("address"), false, 2);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        vp.setMargins(0, dp(8), 0, 0);
        value.setLayoutParams(vp);
        row.addView(value);

        clickable(row, "address");
        formContainer.addView(row);
    }

    private void addDeliveryRow() {
        LinearLayout row = rowBase(22, 22, 22, 22, true);
        TextView ic = icon(iconTruck, iconSize, iconColor(), 14);
        if (ic != null) {
            row.addView(ic);
        }
        TextView label = text("Livraison", 18, cText(), false, 1);
        weight(label);
        row.addView(label);
        if (has("delivery")) {
            row.addView(text(get("delivery"), 18, valueColor("delivery"), false, 1));
        }
        clickable(row, "delivery");
        formContainer.addView(row);
    }

    // =========================================================================
    // VALEURS : SET / GET
    // =========================================================================

    @SimpleFunction(description = "Enregistre la valeur d'un champ et met à jour son affichage. fieldKey : title, description, category, category_id (non affiché), price, condition, color, size, stock, address, delivery, images (liste JSON de liens). Le texte long est coupé par « … » seulement à l'écran : la vraie valeur reste entière. Une valeur vide efface le champ.")
    public void SetProductFormValue(String fieldKey, String value) {
        String k = fieldKey == null ? "" : fieldKey.trim();
        String v = value == null ? "" : value.trim();
        if (k.isEmpty()) {
            OnError("SetProductFormValue: fieldKey vide.");
            return;
        }
        if (k.equals("price") && !v.isEmpty()) {
            String n = normalizePrice(v);
            if (n.isEmpty()) {
                OnError("SetProductFormValue: prix invalide (nombre supérieur à 0, 2 décimales maximum).");
                return;
            }
            v = n;
        }
        if (k.equals("stock") && !v.isEmpty()) {
            if (!v.matches("\\d{1,6}")) {
                OnError("SetProductFormValue: le stock doit être un nombre entier (0 ou plus).");
                return;
            }
            v = String.valueOf(Integer.parseInt(v));
        }
        if (k.equals("images") && !v.isEmpty()) {
            try {
                new JSONArray(v);
            } catch (Exception e) {
                OnError("SetProductFormValue: images doit être une liste JSON.");
                return;
            }
        }
        if (v.isEmpty()) {
            values.remove(k);
        } else {
            values.put(k, v);
        }
        if (k.equals(invalidKey)) {
            invalidKey = "";
        }
        if (k.equals("images")) {
            loadPendingFromValues();
            bindImages();
        }
        saveDraft();
        render();
        OnProductFormValueSaved(k, v);
    }

    @SimpleFunction(description = "Retourne la vraie valeur entière d'un champ (pas le texte coupé de l'écran). Sert à préremplir un panneau de saisie.")
    public String GetProductFormValue(String fieldKey) {
        return fieldKey == null ? "" : get(fieldKey.trim());
    }

    @SimpleFunction(description = "Retourne le stock actuel (nombre entier, 0 par défaut). Utile pour l'afficher ailleurs ou pour l'envoi au serveur.")
    public int GetProductFormStock() {
        return currentStock();
    }

    @SimpleFunction(description = "Retourne vrai si le texte saisi dans un panneau est différent de la valeur déjà enregistrée pour ce champ (modification non enregistrée). Sert au bouton × (annuler) pour décider d'afficher une alerte. Le prix et le stock sont comparés après mise en forme (« 198 » = « 198.00 »).")
    public boolean IsProductFieldModified(String fieldKey, String value) {
        String k = fieldKey == null ? "" : fieldKey.trim();
        String v = value == null ? "" : value.trim();
        if (k.equals("price") && !v.isEmpty()) {
            String n = normalizePrice(v);
            if (!n.isEmpty()) v = n;
        } else if (k.equals("stock") && v.matches("\\d{1,6}")) {
            v = String.valueOf(Integer.parseInt(v));
        }
        return !v.equals(get(k));
    }

    @SimpleFunction(description = "Retourne le produit complet en JSON, prêt à envoyer au serveur : title, description, category, category_id, price (nombre), currency (HTG), condition, color, size, stock (nombre), address, delivery, deliveryIncluded, images (liste), image, image2 à image5.")
    public String GetProductFormJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("title", get("title"));
            o.put("description", get("description"));
            o.put("category", get("category"));
            if (has("category_id")) {
                o.put("category_id", get("category_id"));
            }
            String p = get("price");
            o.put("price", p.isEmpty() ? 0 : new BigDecimal(p).doubleValue());
            o.put("currency", "HTG");
            o.put("condition", get("condition"));
            o.put("color", get("color"));
            o.put("size", get("size"));
            o.put("stock", currentStock());
            o.put("address", get("address"));
            o.put("delivery", get("delivery"));
            o.put("deliveryIncluded", isTruthy(get("delivery")));
            List<String> imgs = confirmedImages();
            o.put("images", new JSONArray(imgs));
            String[] names = {"image", "image2", "image3", "image4", "image5"};
            for (int i = 0; i < imgs.size() && i < names.length; i++) {
                o.put(names[i], imgs.get(i));
            }
            return o.toString();
        } catch (Exception e) {
            OnError("GetProductFormJson: " + e.getMessage());
            return "{}";
        }
    }

    private boolean isTruthy(String s) {
        String t = s == null ? "" : s.trim().toLowerCase(Locale.US);
        return t.equals("true") || t.equals("oui") || t.equals("yes") || t.equals("1")
                || t.equals("inclus") || t.equals("incluse") || t.equals("included");
    }

    // =========================================================================
    // VALIDATION
    // =========================================================================

    @SimpleFunction(description = "Vérifie la valeur d'un champ avant de l'enregistrer. Retourne \"\" si tout est bon, sinon le message d'erreur à afficher. Règles : titre obligatoire (80 caractères max), description (1000 max), catégorie obligatoire, prix supérieur à 0 (2 décimales max), stock entier, état/couleur/taille (30 max).")
    public String ValidateProductField(String fieldKey, String value) {
        String k = fieldKey == null ? "" : fieldKey.trim();
        String v = value == null ? "" : value.trim();
        int len = v.length();
        if (k.equals("title")) {
            if (len == 0) return "Le titre est obligatoire.";
            if (len > MAX_TITLE) return "Titre trop long : " + len + "/" + MAX_TITLE + ".";
        } else if (k.equals("description")) {
            if (len > MAX_DESCRIPTION) {
                return "Description trop longue : " + len + "/" + MAX_DESCRIPTION + ".";
            }
        } else if (k.equals("category")) {
            if (len == 0) return "Choisis une catégorie.";
        } else if (k.equals("price")) {
            if (normalizePrice(v).isEmpty()) {
                return "Prix invalide : entre un nombre supérieur à 0 (2 décimales maximum).";
            }
        } else if (k.equals("stock")) {
            if (!v.matches("\\d{1,6}")) return "Le stock doit être un nombre entier (0 ou plus).";
        } else if (k.equals("condition") || k.equals("color") || k.equals("size")) {
            if (len > MAX_SHORT) return "Trop long : " + len + "/" + MAX_SHORT + ".";
        }
        return "";
    }

    @SimpleFunction(description = "Retourne vrai si la valeur du champ est valide, faux sinon. À mettre directement dans un « si », sans comparaison. Pour lire le message d'erreur, utilise ValidateProductField.")
    public boolean IsProductFieldValid(String fieldKey, String value) {
        return ValidateProductField(fieldKey, value).isEmpty();
    }

    @SimpleFunction(description = "Vérifie tout le formulaire. Retourne vrai si tout est valide. Sinon retourne faux, la ligne concernée passe en rouge et OnProductFormInvalid est déclenché. À mettre directement dans un « si » avant l'envoi au serveur.")
    public boolean IsProductFormValid() {
        return ValidateProductForm().isEmpty();
    }

    @SimpleFunction(description = "Vérifie tout le formulaire avant l'envoi. Retourne \"\" si tout est bon, sinon le premier message d'erreur. La ligne concernée passe en rouge et OnProductFormInvalid est déclenché.")
    public String ValidateProductForm() {
        String[] order = {"title", "category", "price", "description", "stock",
                "condition", "color", "size"};
        for (int i = 0; i < order.length; i++) {
            String k = order[i];
            boolean optional = !(k.equals("title") || k.equals("category") || k.equals("price"));
            if (optional && !has(k)) continue;
            String msg = ValidateProductField(k, get(k));
            if (!msg.isEmpty()) {
                invalidKey = k;
                render();
                OnProductFormInvalid(k, msg);
                return msg;
            }
        }
        if (!invalidKey.isEmpty()) {
            invalidKey = "";
            render();
        }
        return "";
    }

    // =========================================================================
    // PRIX ET TEXTE
    // =========================================================================

    private String normalizePrice(String text) {
        if (text == null) return "";
        String t = text.trim().replace(" ", "").replace("\u00A0", "").replace(",", ".");
        if (!t.matches("\\d{1,12}(\\.\\d{1,2})?")) return "";
        BigDecimal bd = new BigDecimal(t);
        if (bd.signum() <= 0) return "";
        return bd.setScale(2).toPlainString();
    }

    private String formatPrice(String amount) {
        try {
            BigDecimal bd = new BigDecimal(amount);
            String s = String.format(Locale.US, "%,.2f", bd);
            String r = s.replace(",", " ").replace(".", ",");
            if (r.endsWith(",00")) r = r.substring(0, r.length() - 3);
            return r + " HTG";
        } catch (Exception e) {
            return amount + " HTG";
        }
    }

    @SimpleFunction(description = "Transforme un prix saisi (« 5000 », « 5 000,5 ») en nombre à 2 décimales (« 5000.50 »). Retourne \"\" si le format est invalide ou si le prix n'est pas supérieur à 0.")
    public String NormalizePrice(String text) {
        return normalizePrice(text);
    }

    @SimpleFunction(description = "Met un prix en forme pour l'affichage : « 5 000,50 HTG ».")
    public String FormatPriceDisplay(String amount) {
        String n = normalizePrice(amount);
        return n.isEmpty() ? "0,00 HTG" : formatPrice(n);
    }

    @SimpleFunction(description = "Coupe un texte à maxChars caractères en ajoutant « … » (pour l'affichage seulement).")
    public String ShortenText(String text, int maxChars) {
        String t = text == null ? "" : text;
        if (maxChars <= 0 || t.length() <= maxChars) return t;
        return t.substring(0, Math.max(0, maxChars - 1)).trim() + "…";
    }

    // =========================================================================
    // ENVOI AU SERVEUR (multipart/form-data)
    // =========================================================================

    private void mpField(DataOutputStream out, String boundary, String name, String value) throws Exception {
        out.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes("UTF-8"));
        out.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n".getBytes("UTF-8"));
        out.write((value == null ? "" : value).getBytes("UTF-8"));
        out.write("\r\n".getBytes("UTF-8"));
    }

    @SimpleFunction(description = "Envoie le produit complet à ton serveur en une seule requête POST multipart/form-data. Champs texte : title, description, category, category_id, price, currency, condition, color, size, stock, address, delivery, deliveryIncluded. Photos en fichiers : image, image2 à image5. authorization : valeur de l'en-tête Authorization (ex : Bearer abc123), vide si inutile. Le formulaire est vérifié avant l'envoi (OnProductFormInvalid si invalide). La réponse arrive dans OnProductSent.")
    public void SendProductForm(final String url, final String authorization) {
        if (url == null || url.trim().isEmpty()) {
            OnError("SendProductForm: adresse du serveur vide.");
            return;
        }
        if (dirty) {
            ConfirmProductPhotos();
        }
        if (!ValidateProductForm().isEmpty()) {
            return;
        }

        final Map<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("title", get("title"));
        fields.put("description", get("description"));
        fields.put("category", get("category"));
        if (has("category_id")) {
            fields.put("category_id", get("category_id"));
        }
        fields.put("price", get("price"));
        fields.put("currency", "HTG");
        fields.put("condition", get("condition"));
        fields.put("color", get("color"));
        fields.put("size", get("size"));
        fields.put("stock", String.valueOf(currentStock()));
        fields.put("address", get("address"));
        fields.put("delivery", get("delivery"));
        fields.put("deliveryIncluded", String.valueOf(isTruthy(get("delivery"))));
        final List<String> files = confirmedImages();
        final String target = url.trim();
        final String auth = authorization == null ? "" : authorization.trim();

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection c = null;
                try {
                    String boundary = "----Manatest" + System.currentTimeMillis();
                    c = (HttpURLConnection) new URL(target).openConnection();
                    c.setRequestMethod("POST");
                    c.setDoOutput(true);
                    c.setConnectTimeout(20000);
                    c.setReadTimeout(60000);
                    c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    if (!auth.isEmpty()) {
                        c.setRequestProperty("Authorization", auth);
                    }
                    DataOutputStream out = new DataOutputStream(c.getOutputStream());
                    for (Map.Entry<String, String> e : fields.entrySet()) {
                        mpField(out, boundary, e.getKey(), e.getValue());
                    }
                    String[] names = {"image", "image2", "image3", "image4", "image5"};
                    for (int i = 0; i < files.size() && i < names.length; i++) {
                        String path = files.get(i);
                        if (path.startsWith("file://")) path = path.substring(7);
                        File f = new File(path);
                        if (!f.exists()) continue;
                        out.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
                        out.write(("Content-Disposition: form-data; name=\"" + names[i]
                                + "\"; filename=\"photo" + (i + 1) + ".jpg\"\r\n").getBytes("UTF-8"));
                        out.write("Content-Type: image/jpeg\r\n\r\n".getBytes("UTF-8"));
                        FileInputStream in = new FileInputStream(f);
                        try {
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = in.read(buf)) > 0) {
                                out.write(buf, 0, n);
                            }
                        } finally {
                            try { in.close(); } catch (Exception ignored) { }
                        }
                        out.write("\r\n".getBytes("UTF-8"));
                    }
                    out.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
                    out.flush();
                    out.close();

                    final int code = c.getResponseCode();
                    InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    if (is != null) {
                        byte[] buf = new byte[4096];
                        int n;
                        while ((n = is.read(buf)) > 0) {
                            bos.write(buf, 0, n);
                        }
                        is.close();
                    }
                    final String body = bos.toString("UTF-8");
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            OnProductSent(code, body);
                        }
                    });
                } catch (final Exception e) {
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            OnError("SendProductForm: " + e.getMessage());
                        }
                    });
                } finally {
                    if (c != null) c.disconnect();
                }
            }
        }).start();
    }

    // =========================================================================
    // BROUILLON
    // =========================================================================

    private void saveDraft() {
        try {
            SharedPreferences p = context.getSharedPreferences(PRODUCT_PREFS_NAME, Context.MODE_PRIVATE);
            p.edit().putString(PREF_DRAFT, new JSONObject(values).toString()).apply();
        } catch (Exception e) {
            OnError("Brouillon: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Remet dans le formulaire le brouillon sauvegardé dans le téléphone (chaque modification le met à jour). Retourne vrai si un brouillon a été restauré. À appeler après BuildProductForm.")
    public boolean RestoreProductFormDraft() {
        try {
            SharedPreferences p = context.getSharedPreferences(PRODUCT_PREFS_NAME, Context.MODE_PRIVATE);
            String raw = p.getString(PREF_DRAFT, "");
            if (raw == null || raw.isEmpty()) return false;
            JSONObject o = new JSONObject(raw);
            values.clear();
            Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                String v = o.optString(k, "");
                if (!v.isEmpty()) {
                    values.put(k, v);
                }
            }
            loadPendingFromValues();
            bindImages();
            render();
            return !values.isEmpty();
        } catch (Exception e) {
            OnError("RestoreProductFormDraft: " + e.getMessage());
            return false;
        }
    }

    @SimpleFunction(description = "Efface toutes les valeurs, les photos et le brouillon, et remet le formulaire à zéro (pour un nouveau produit).")
    public void ClearProductForm() {
        values.clear();
        pending.clear();
        thumbs.clear();
        dirty = false;
        invalidKey = "";
        try {
            context.getSharedPreferences(PRODUCT_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().remove(PREF_DRAFT).apply();
        } catch (Exception ignored) {
        }
        bindImages();
        render();
    }

    // =========================================================================
    // 18. PAGE « CRÉER MA BOUTIQUE » (ex-Manatest 3/5)
    //     Renommés pour éviter les conflits : GetShopFormName / GetShopFormPhone /
    //     GetShopFormAddress / GetShopFormLogo (au lieu de GetShop...), cInput (au lieu
    //     de cText), renderShopForm, addShopSeparator, addShopAddressSection.
    // =========================================================================

    // Préférences : mêmes PREFS_NAME / PREF_CURRENT_UID que la boutique (section 16)
    private static final String PREF_SELLER_PREFIX = "is_seller_";

    private static final int MAX_NAME = 50;
    private static final int MAX_PHONE = 20;
    private static final int MAX_ADDRESS = 120;
    private static final int FIELD_HEIGHT_DP = 64;
    private static final long NAME_DEBOUNCE_MS = 450L;
    private static final int LOGO_MAX_WIDTH = 800;
    private static final int LOGO_QUALITY = 85;

    private static final int COLOR_FOCUS = Color.parseColor("#0055D4");
    private static final int COLOR_ERROR = Color.parseColor("#E53935");
    private static final int COLOR_OK = Color.parseColor("#2E7D32");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int shopLogoRequestCode;

    // Valeurs saisies (séparées de l'affichage : elles survivent à un nouveau rendu)
    private String shopName = "";
    private String shopCategory = "";
    private String shopPhone = "";
    private String shopAddress = "";
    private String logoPath = "";
    private final List<String> categories = new ArrayList<String>();

    private ViewGroup shopFormContainer;
    private boolean restoring = false;

    // Vues
    private FieldView nameField;
    private FieldView phoneField;
    private FieldView addressField;
    private FieldView catField;
    private TextView catValue;
    private TextView catError;
    private TextView nameStatus;
    private ImageView formLogoImage;
    private View logoGlyph;

    // Vérification du nom
    private String nameCheckUrl = "";
    private String nameCheckAuth = "";
    private int nameSeq = 0;
    private boolean nameChecking = false;
    private int nameState = 0; // 0 inconnu, 1 disponible, 2 déjà utilisé, 3 erreur réseau
    private boolean nameRequiredShown = false;
    private Runnable nameRunnable;

    // Envoi et statut vendeur
    private boolean submitting = false;
    private volatile boolean sellerListening = false;
    private Thread sellerThread;

    // =========================================================================
    // OUTILS INTERNES
    // =========================================================================

    private int cTitle() { return ink(0xC0); }   // grand titre et sous-descriptions
    private int cInput() { return ink(0xE9); }    // texte saisi / sélectionné
    private int cHint() { return ink(0x91); }    // libellé (hint)
    private int cStroke() { return ink(0x86); }  // contour au repos

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(context);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        Typeface cf = shopFont();
        if (bold) {
            if (customBoldFont != null) {
                t.setTypeface(customBoldFont);
            } else if (cf != null) {
                t.setTypeface(Typeface.create(cf, Typeface.BOLD));
            } else {
                t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            }
        } else if (cf != null) {
            t.setTypeface(cf);
        }
        return t;
    }

    private void hideKeyboard() {
        try {
            View v = activity.getCurrentFocus();
            if (v == null) v = shopFormContainer != null ? shopFormContainer : activeContainer;
            InputMethodManager imm =
                    (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && v != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } catch (Exception ignored) {
        }
    }

    private String stripFile(String p) {
        return (p != null && p.startsWith("file://")) ? p.substring(7) : p;
    }

    private String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        is.close();
        return bos.toString("UTF-8");
    }

    private String[] httpGet(String target, String auth) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(target).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            c.setRequestProperty("Accept", "application/json");
            if (auth != null && !auth.isEmpty()) {
                c.setRequestProperty("Authorization", auth);
            }
            int code = c.getResponseCode();
            InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
            return new String[]{String.valueOf(code), readAll(is)};
        } finally {
            if (c != null) c.disconnect();
        }
    }

    // =========================================================================
    // ÉVÉNEMENTS
    // =========================================================================

    @SimpleEvent(description = "Une catégorie vient d'être choisie dans le spinner. category : le nom choisi.")
    public void AfterCategorySelected(String category) {
        EventDispatcher.dispatchEvent(this, "AfterCategorySelected", category);
    }

    @SimpleEvent(description = "Le serveur a répondu à la vérification du nom. available : vrai si le nom est libre, faux s'il est déjà utilisé.")
    public void OnShopNameChecked(String name, boolean available) {
        EventDispatcher.dispatchEvent(this, "OnShopNameChecked", name, available);
    }

    @SimpleEvent(description = "Le formulaire est invalide. fieldKey : name ou category. message : le texte de l'erreur (aussi affiché en rouge à l'écran).")
    public void OnShopFormInvalid(String fieldKey, String message) {
        EventDispatcher.dispatchEvent(this, "OnShopFormInvalid", fieldKey, message);
    }

    @SimpleEvent(description = "Le logo a été choisi et compressé. logoPath : lien file:// de l'image.")
    public void OnShopLogoPicked(String logoPath) {
        EventDispatcher.dispatchEvent(this, "OnShopLogoPicked", logoPath);
    }

    @SimpleEvent(description = "Le serveur a accepté la création de la boutique (l'utilisateur est maintenant vendeur). responseCode : code HTTP, response : texte renvoyé.")
    public void OnShopCreated(int responseCode, String response) {
        EventDispatcher.dispatchEvent(this, "OnShopCreated", responseCode, response);
    }

    @SimpleEvent(description = "Le serveur a refusé la création de la boutique (ou erreur serveur). Le code 409 signale un nom déjà pris.")
    public void OnShopCreateFailed(int responseCode, String response) {
        EventDispatcher.dispatchEvent(this, "OnShopCreateFailed", responseCode, response);
    }

    @SimpleEvent(description = "Le serveur a répondu sur le statut vendeur. isSeller : vrai si l'utilisateur a déjà une boutique.")
    public void OnSellerStatusChecked(boolean isSeller) {
        EventDispatcher.dispatchEvent(this, "OnSellerStatusChecked", isSeller);
    }

    @SimpleEvent(description = "Le statut vendeur vient de changer (faux vers vrai à la création ou à la reconnexion). À utiliser pour masquer la page de création et l'onglet de la barre de navigation.")
    public void OnSellerStatusChanged(boolean isSeller) {
        EventDispatcher.dispatchEvent(this, "OnSellerStatusChanged", isSeller);
    }

    // =========================================================================
    // CONSTRUCTION DU FORMULAIRE
    // =========================================================================

    @SimpleFunction(description = "Construit le formulaire « Créer ma boutique » dans l'arrangement donné (utilise un arrangement vertical dédié : son contenu est remplacé). Si l'utilisateur est déjà vendeur, rien n'est affiché et OnSellerStatusChecked(vrai) est déclenché.")
    public void BuildShopForm(final AndroidViewComponent container) {
        if (container == null || container.getView() == null) {
            OnError("BuildShopForm: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup target = realLayout(container);
                    if (target == null) {
                        OnError("BuildShopForm: conteneur invalide.");
                        return;
                    }
                    shopFormContainer = target;
                    if (IsSeller()) {
                        OnSellerStatusChecked(true);
                        return;
                    }
                    renderShopForm();
                } catch (Exception e) {
                    OnError("BuildShopForm: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Définit la liste des catégories du spinner (pages Créer ma boutique et Modifier la boutique) : un tableau JSON (« [\"Mode\",\"Beauté\"] » ou objets avec name / title) ou des noms séparés par des virgules.")
    public void SetShopCategories(String categoriesList) {
        categories.clear();
        if (categoriesList == null || categoriesList.trim().isEmpty()) return;
        String t = categoriesList.trim();
        try {
            if (t.startsWith("[")) {
                JSONArray a = new JSONArray(t);
                for (int i = 0; i < a.length(); i++) {
                    Object o = a.get(i);
                    String s;
                    if (o instanceof JSONObject) {
                        JSONObject jo = (JSONObject) o;
                        s = jo.optString("name", jo.optString("title", jo.optString("label", "")));
                    } else {
                        s = String.valueOf(o);
                    }
                    s = s == null ? "" : s.trim();
                    if (!s.isEmpty()) categories.add(s);
                }
            } else {
                String[] parts = t.split(",");
                for (int i = 0; i < parts.length; i++) {
                    String s = parts[i].trim();
                    if (!s.isEmpty()) categories.add(s);
                }
            }
        } catch (Exception e) {
            OnError("SetShopCategories: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Définit l'adresse du serveur qui vérifie le nom en direct. La requête GET est envoyée à chaque pause de frappe avec ?name=... (ou à la place de {name} dans l'adresse). Réponse attendue en JSON : {\"available\": true} ou {\"exists\": true}. authorization : en-tête Authorization (vide si inutile). Sert aussi à la page Modifier la boutique : l'UID (SetShopUser) est joint à la requête (ou à la place de {uid} dans l'adresse), et le serveur doit ignorer la boutique de cet UID.")
    public void SetShopNameCheckUrl(String url, String authorization) {
        nameCheckUrl = url == null ? "" : url.trim();
        nameCheckAuth = authorization == null ? "" : authorization.trim();
        scheduleNameCheck();
        scheduleEditNameCheck();
    }

    private void renderShopForm() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    if (shopFormContainer == null) return;
                    shopFormContainer.removeAllViews();
                    addTitle();
                    addNameSection();
                    addShopSeparator();
                    addCategorySection();
                    addShopSeparator();
                    addPhoneSection();
                    addShopSeparator();
                    addShopAddressSection();
                    addLogoSection();
                } catch (Exception e) {
                    OnError("render: " + e.getMessage());
                }
            }
        });
    }

    // ---- éléments de base ----

    private LinearLayout section() {
        LinearLayout s = new LinearLayout(context);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        s.setPadding(dp(20), dp(16), dp(20), dp(16));
        return s;
    }

    private TextView desc(String s) {
        TextView t = text(s, 12, cTitle(), false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(12);
        p.leftMargin = dp(2);
        t.setLayoutParams(p);
        return t;
    }

    private void addShopSeparator() {
        View v = new View(context);
        v.setBackgroundColor(Color.parseColor("#F5F5F5"));
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(14)));
        shopFormContainer.addView(v);
    }

    private void addTitle() {
        TextView t = text("Configure ta boutique pour commencer à vendre", 25, cTitle(), false);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        t.setPadding(dp(20), dp(20), dp(20), dp(12));
        shopFormContainer.addView(t);
    }

    // ---- champ à libellé animé (nom, téléphone, adresse, spinner) ----

    private class FieldView {
        final FrameLayout box;
        final TextView label;
        final GradientDrawable bg;
        EditText edit;
        boolean floated = false;
        boolean focused = false;
        boolean error = false;

        FieldView(String hint) {
            box = new FrameLayout(context);
            box.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(FIELD_HEIGHT_DP)));
            bg = new GradientDrawable();
            bg.setColor(Color.TRANSPARENT);
            bg.setCornerRadius(dp(24));
            box.setBackground(bg);

            label = text(hint, 15, cHint(), false);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, dp(22),
                    Gravity.CENTER_VERTICAL | Gravity.START);
            lp.leftMargin = dp(20);
            label.setLayoutParams(lp);
            label.setGravity(Gravity.CENTER_VERTICAL);
            label.setSingleLine(true);
            label.setClickable(false);
            label.setPivotX(0f);
            label.setPivotY((float) dp(11));
            updateStroke();
        }

        // À appeler en dernier : le libellé passe au-dessus du contenu
        void attachLabel() {
            box.addView(label);
        }

        void updateStroke() {
            int w;
            int c;
            if (focused) {
                w = dp(2);
                c = COLOR_FOCUS;
            } else if (error) {
                w = dp(1);
                c = COLOR_ERROR;
            } else {
                w = dp(1);
                c = cStroke();
            }
            bg.setStroke(w, c);
        }

        void setFloated(boolean f, boolean animate) {
            if (animate && floated == f) return;
            floated = f;
            float ty = f ? -(float) dp(17) : 0f;
            float sc = f ? 0.8f : 1f;
            if (animate) {
                label.animate()
                        .translationY(ty)
                        .scaleX(sc)
                        .scaleY(sc)
                        .setDuration(160)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            } else {
                label.animate().cancel();
                label.setTranslationY(ty);
                label.setScaleX(sc);
                label.setScaleY(sc);
            }
        }
    }

    private FieldView buildEditField(final String key, String hint, int inputType,
                                     int maxLen, int imeAction, String initial) {
        final FieldView f = new FieldView(hint);
        final EditText e = new EditText(context);
        e.setBackground(null);
        e.setTextSize(15);
        e.setTextColor(cInput());
        e.setHintTextColor(cHint());
        Typeface ef = shopFont();
        e.setTypeface(ef != null ? ef : Typeface.DEFAULT);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setImeOptions(imeAction);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLen)});
        e.setPadding(dp(20), dp(24), dp(20), dp(6));
        e.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        f.edit = e;
        f.box.addView(e, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        f.attachLabel();

        restoring = true;
        e.setText(initial);
        restoring = false;
        f.setFloated(initial != null && !initial.isEmpty(), false);

        e.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                f.focused = hasFocus;
                f.updateStroke();
                f.setFloated(hasFocus || e.getText().length() > 0, true);
            }
        });

        e.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable ed) {
                if (restoring) return;
                String s = ed.toString();
                if (s.length() > 0 && !f.floated) f.setFloated(true, true);
                f.error = false;
                f.updateStroke();
                onFieldChanged(key, s);
            }
        });

        if (imeAction == EditorInfo.IME_ACTION_DONE) {
            e.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent ev) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        hideKeyboard();
                        e.clearFocus();
                        return true;
                    }
                    return false;
                }
            });
        }
        return f;
    }

    private void onFieldChanged(String key, String value) {
        if ("name".equals(key)) {
            shopName = value;
            nameRequiredShown = false;
            scheduleNameCheck();
        } else if ("phone".equals(key)) {
            shopPhone = value;
        } else if ("address".equals(key)) {
            shopAddress = value;
        }
    }

    // ---- sections ----

    private void addNameSection() {
        LinearLayout sec = section();
        nameField = buildEditField("name", "Nom de la boutique",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                MAX_NAME, EditorInfo.IME_ACTION_NEXT, shopName);
        sec.addView(nameField.box);
        sec.addView(desc("Le nom de votre boutique est le premier point de contact avec vos futurs clients"));

        nameStatus = text("", 12, COLOR_ERROR, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(6);
        p.leftMargin = dp(2);
        nameStatus.setLayoutParams(p);
        nameStatus.setVisibility(View.GONE);
        sec.addView(nameStatus);
        showNameStatus();
        shopFormContainer.addView(sec);
    }

    private void addPhoneSection() {
        LinearLayout sec = section();
        phoneField = buildEditField("phone", "Téléphone du magasin",
                InputType.TYPE_CLASS_PHONE,
                MAX_PHONE, EditorInfo.IME_ACTION_NEXT, shopPhone);
        sec.addView(phoneField.box);
        sec.addView(desc("Indiquez le numéro de téléphone officiel de votre établissement. Ce contact permet à vos clients de vous joindre facilement."));
        shopFormContainer.addView(sec);
    }

    private void addShopAddressSection() {
        LinearLayout sec = section();
        addressField = buildEditField("address", "Adresse du magasin",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                MAX_ADDRESS, EditorInfo.IME_ACTION_DONE, shopAddress);
        sec.addView(addressField.box);
        sec.addView(desc("Indiquez l'emplacement physique exact de votre commerce."));
        shopFormContainer.addView(sec);
    }

    // ---- spinner de catégories ----

    private class ArrowView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        ArrowView(Context c) {
            super(c);
            paint.setColor(cInput());
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            path.reset();
            path.moveTo(0f, 0f);
            path.lineTo(w, 0f);
            path.lineTo(w / 2f, h);
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    private void addCategorySection() {
        LinearLayout sec = section();
        catField = new FieldView("Catégorie");

        catValue = text(shopCategory, 15, cInput(), false);
        catValue.setSingleLine(true);
        catValue.setEllipsize(TextUtils.TruncateAt.END);
        catValue.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        catValue.setPadding(dp(20), dp(24), dp(48), dp(6));
        catField.box.addView(catValue, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(
                dp(14), dp(8), Gravity.END | Gravity.CENTER_VERTICAL);
        ap.rightMargin = dp(20);
        catField.box.addView(new ArrowView(context), ap);

        catField.attachLabel();
        catField.setFloated(!shopCategory.isEmpty(), false);
        catField.box.setClickable(true);
        catField.box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCategoryPopup();
            }
        });

        sec.addView(catField.box);
        sec.addView(desc("Sélectionnez le secteur d'activité qui correspond le mieux à vos produits"));

        catError = text("", 12, COLOR_ERROR, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(6);
        p.leftMargin = dp(2);
        catError.setLayoutParams(p);
        catError.setVisibility(View.GONE);
        sec.addView(catError);
        shopFormContainer.addView(sec);
    }

    private void clearEditFocus() {
        if (nameField != null && nameField.edit != null) nameField.edit.clearFocus();
        if (phoneField != null && phoneField.edit != null) phoneField.edit.clearFocus();
        if (addressField != null && addressField.edit != null) addressField.edit.clearFocus();
    }

    private void openCategoryPopup() {
        if (categories.isEmpty()) {
            OnError("Aucune catégorie : appelle SetShopCategories avant d'ouvrir le spinner.");
            return;
        }
        hideKeyboard();
        clearEditFocus();

        final ListPopupWindow pop = new ListPopupWindow(context);
        pop.setAnchorView(catField.box);
        pop.setModal(true);
        pop.setWidth(catField.box.getWidth());
        pop.setHeight(categories.size() <= 5
                ? ViewGroup.LayoutParams.WRAP_CONTENT
                : dp(5 * 48 + 8));
        pop.setVerticalOffset(dp(4));

        GradientDrawable pbg = new GradientDrawable();
        pbg.setColor(Color.WHITE);
        pbg.setCornerRadius(dp(16));
        pbg.setStroke(dp(1), cStroke());
        pop.setBackgroundDrawable(pbg);

        pop.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return categories.size();
            }

            @Override
            public Object getItem(int position) {
                return categories.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv;
                if (convertView instanceof TextView) {
                    tv = (TextView) convertView;
                } else {
                    tv = text("", 15, cInput(), false);
                    tv.setGravity(Gravity.CENTER_VERTICAL);
                    tv.setPadding(dp(20), 0, dp(20), 0);
                    tv.setSingleLine(true);
                    tv.setEllipsize(TextUtils.TruncateAt.END);
                    tv.setLayoutParams(new AbsListView.LayoutParams(
                            AbsListView.LayoutParams.MATCH_PARENT, dp(48)));
                }
                String item = categories.get(position);
                tv.setText(item);
                tv.setBackgroundColor(item.equals(shopCategory)
                        ? Color.parseColor("#F2F2F2") : Color.TRANSPARENT);
                return tv;
            }
        });

        pop.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectCategory(categories.get(position), true);
                pop.dismiss();
            }
        });

        pop.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                catField.focused = false;
                catField.updateStroke();
                if (shopCategory.isEmpty()) catField.setFloated(false, true);
            }
        });

        catField.focused = true;
        catField.updateStroke();
        catField.setFloated(true, true);
        pop.show();
    }

    private void selectCategory(String c, boolean fire) {
        shopCategory = c == null ? "" : c;
        if (catValue != null) catValue.setText(shopCategory);
        if (catField != null) {
            catField.error = false;
            catField.updateStroke();
            catField.setFloated(!shopCategory.isEmpty(), true);
        }
        if (catError != null) catError.setVisibility(View.GONE);
        if (fire) AfterCategorySelected(shopCategory);
    }

    // ---- logo ----

    private class PhotoGlyph extends View {
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        PhotoGlyph(Context c) {
            super(c);
            int grey = Color.parseColor("#C4C4C4");
            stroke.setColor(grey);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth((float) dp(4));
            stroke.setStrokeJoin(Paint.Join.ROUND);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            fill.setColor(grey);
            fill.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            float s = stroke.getStrokeWidth() / 2f;
            canvas.drawRoundRect(new RectF(s, s, w - s, h - s), w * 0.2f, w * 0.2f, stroke);
            canvas.drawCircle(w * 0.68f, h * 0.30f, w * 0.06f, fill);
            path.reset();
            path.moveTo(w * 0.12f, h * 0.78f);
            path.lineTo(w * 0.38f, h * 0.50f);
            path.lineTo(w * 0.55f, h * 0.68f);
            path.lineTo(w * 0.68f, h * 0.58f);
            path.lineTo(w * 0.88f, h * 0.78f);
            canvas.drawPath(path, stroke);
        }
    }

    private void addLogoSection() {
        LinearLayout sec = section();
        sec.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout outer = new FrameLayout(context);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(dp(158), dp(158));
        op.gravity = Gravity.CENTER_HORIZONTAL;
        op.topMargin = dp(20);
        op.bottomMargin = dp(20);
        outer.setLayoutParams(op);
        GradientDrawable obg = new GradientDrawable();
        obg.setColor(Color.WHITE);
        obg.setCornerRadius(dp(30));
        outer.setBackground(obg);
        outer.setElevation((float) dp(6));
        outer.setRotation(-4f);

        final int innerRadius = dp(26);
        FrameLayout inner = new FrameLayout(context);
        GradientDrawable ibg = new GradientDrawable();
        ibg.setColor(Color.parseColor("#E8E8E8"));
        ibg.setCornerRadius(innerRadius);
        inner.setBackground(ibg);
        inner.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), innerRadius);
            }
        });
        inner.setClipToOutline(true);
        outer.addView(inner, new FrameLayout.LayoutParams(dp(134), dp(134), Gravity.CENTER));

        logoGlyph = new PhotoGlyph(context);
        inner.addView(logoGlyph, new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER));

        formLogoImage = new ImageView(context);
        formLogoImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        formLogoImage.setVisibility(View.GONE);
        inner.addView(formLogoImage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        outer.setClickable(true);
        outer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenLogoPicker();
            }
        });

        sec.addView(outer);
        sec.addView(desc("Ajoutez votre logo pour finaliser l'apparence de votre magasin sur l'application"));
        shopFormContainer.addView(sec);
        updateLogoView();
    }

    private void updateLogoView() {
        if (formLogoImage == null || logoGlyph == null) return;
        if (logoPath.isEmpty()) {
            formLogoImage.setImageDrawable(null);
            formLogoImage.setVisibility(View.GONE);
            logoGlyph.setVisibility(View.VISIBLE);
            return;
        }
        Bitmap b = decodeSampled(stripFile(logoPath), 400);
        if (b != null) {
            formLogoImage.setImageBitmap(b);
            formLogoImage.setVisibility(View.VISIBLE);
            logoGlyph.setVisibility(View.GONE);
        }
    }

    private Bitmap decodeSampled(String path, int maxWidth) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            if (o.outWidth <= 0) return null;
            int sample = 1;
            while (o.outWidth / (sample * 2) >= maxWidth) sample *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            return BitmapFactory.decodeFile(path, o);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    // SÉLECTION DU LOGO (permission + galerie comme ManaplaceUtils,
    // copie dans le cache comme Manatest, puis compression)
    // =========================================================================

    @SimpleFunction(description = "Ouvre la galerie pour choisir le logo de la boutique (demande la permission si besoin). Appelée automatiquement au toucher de la tuile logo. OnShopLogoPicked se déclenche quand le logo est prêt.")
    public void OpenLogoPicker() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? "android.permission.READ_MEDIA_IMAGES"
                : "android.permission.READ_EXTERNAL_STORAGE";

        form.askPermission(permission, new PermissionResultHandler() {
            @Override
            public void HandlePermissionResponse(String permissionName, boolean granted) {
                if (!granted) {
                    OnError("Permission refusée.");
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent i = new Intent(Intent.ACTION_PICK);
                            i.setType("image/*");
                            form.startActivityForResult(i, shopLogoRequestCode);
                        } catch (Exception e) {
                            OnError("OpenLogoPicker: " + e.getMessage());
                        }
                    }
                });
            }
        });
    }

    // Appelée par resultReturned (aiguillage par code de retour)
    private void shopLogoResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != shopLogoRequestCode) return;
        if (resultCode != Activity.RESULT_OK || data == null) return; // annulation : pas une erreur
        final Uri uri = data.getData();
        if (uri == null) {
            OnError("Aucune image sélectionnée (URI nulle).");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String result = processLogo(uri);
                if (result == null) return;
                runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        logoPath = result;
                        updateLogoView();
                        OnShopLogoPicked(result);
                    }
                });
            }
        }).start();
    }

    private String processLogo(Uri uri) {
        File raw = null;
        try {
            File dir = new File(context.getCacheDir(), "manatest_logo");
            dir.mkdirs();
            long t = System.currentTimeMillis();
            raw = new File(dir, "raw_" + t);

            InputStream in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                fail("Logo: image illisible.");
                return null;
            }
            FileOutputStream out = new FileOutputStream(raw);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            } finally {
                try { in.close(); } catch (Exception ignored) { }
                try { out.close(); } catch (Exception ignored) { }
            }

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(raw.getAbsolutePath(), o);
            if (o.outWidth <= 0 || o.outHeight <= 0) {
                fail("Logo: ce fichier n'est pas une image.");
                return null;
            }
            int sample = 1;
            while (o.outWidth / (sample * 2) >= LOGO_MAX_WIDTH) sample *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            Bitmap bmp = BitmapFactory.decodeFile(raw.getAbsolutePath(), o);
            if (bmp == null) {
                fail("Logo: décodage impossible.");
                return null;
            }

            int degrees = 0;
            try {
                ExifInterface exif = new ExifInterface(raw.getAbsolutePath());
                int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
                if (ori == ExifInterface.ORIENTATION_ROTATE_90) degrees = 90;
                else if (ori == ExifInterface.ORIENTATION_ROTATE_180) degrees = 180;
                else if (ori == ExifInterface.ORIENTATION_ROTATE_270) degrees = 270;
            } catch (Exception ignored) {
            }
            if (degrees != 0) {
                Matrix m = new Matrix();
                m.postRotate(degrees);
                Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                if (rotated != bmp) {
                    bmp.recycle();
                    bmp = rotated;
                }
            }

            File dest = new File(dir, "logo_" + t + ".jpg");
            FileOutputStream fos = new FileOutputStream(dest);
            try {
                bmp.compress(Bitmap.CompressFormat.JPEG, LOGO_QUALITY, fos);
                fos.flush();
            } finally {
                try { fos.close(); } catch (Exception ignored) { }
                bmp.recycle();
            }
            return "file://" + dest.getAbsolutePath();
        } catch (Exception e) {
            fail("Logo: " + e.getMessage());
            return null;
        } finally {
            if (raw != null) raw.delete();
        }
    }

    // =========================================================================
    // VÉRIFICATION DU NOM EN DIRECT
    // =========================================================================

    private void scheduleNameCheck() {
        if (nameRunnable != null) handler.removeCallbacks(nameRunnable);
        nameSeq++;
        nameState = 0;
        nameChecking = false;
        showNameStatus();

        final String n = shopName == null ? "" : shopName.trim();
        if (n.isEmpty() || nameCheckUrl.isEmpty()) return;

        nameChecking = true;
        final int seq = nameSeq;
        nameRunnable = new Runnable() {
            @Override
            public void run() {
                runNameCheck(n, seq);
            }
        };
        handler.postDelayed(nameRunnable, NAME_DEBOUNCE_MS);
    }

    private String buildNameUrl(String name) throws Exception {
        String enc = URLEncoder.encode(name, "UTF-8");
        if (nameCheckUrl.contains("{name}")) {
            return nameCheckUrl.replace("{name}", enc);
        }
        return nameCheckUrl + (nameCheckUrl.contains("?") ? "&" : "?") + "name=" + enc;
    }

    private Boolean nameAvailableFromJson(JSONObject o) {
        if (o.has("available")) return Boolean.valueOf(o.optBoolean("available"));
        String[] taken = {"exists", "taken", "used", "isUsed", "already_used", "alreadyUsed"};
        for (int i = 0; i < taken.length; i++) {
            if (o.has(taken[i])) return Boolean.valueOf(!o.optBoolean(taken[i]));
        }
        JSONObject data = o.optJSONObject("data");
        if (data != null) return nameAvailableFromJson(data);
        return null;
    }

    private void runNameCheck(final String name, final int seq) {
        final String url = nameCheckUrl;
        final String auth = nameCheckAuth;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] r = httpGet(buildNameUrl(name), auth);
                    int code = Integer.parseInt(r[0]);
                    if (code >= 400) throw new Exception("serveur " + code);
                    Boolean avail = nameAvailableFromJson(new JSONObject(r[1].trim()));
                    if (avail == null) throw new Exception("réponse inattendue (available / exists manquant)");
                    final boolean available = avail.booleanValue();
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            if (seq != nameSeq) return;
                            nameChecking = false;
                            nameState = available ? 1 : 2;
                            showNameStatus();
                            OnShopNameChecked(name, available);
                        }
                    });
                } catch (final Exception e) {
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            if (seq != nameSeq) return;
                            nameChecking = false;
                            nameState = 3;
                            showNameStatus();
                            OnError("Vérification du nom: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void showNameStatus() {
        if (nameStatus == null) return;
        String msg = "";
        int color = COLOR_ERROR;
        if (nameRequiredShown) {
            msg = "Le nom de la boutique est obligatoire";
        } else if (nameState == 2) {
            msg = "Ce nom est déjà utilisé";
        } else if (nameState == 1 && shopName != null && !shopName.trim().isEmpty()) {
            msg = "Ce nom est disponible";
            color = COLOR_OK;
        }
        nameStatus.setTextColor(color);
        nameStatus.setText(msg);
        nameStatus.setVisibility(msg.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // =========================================================================
    // LECTURE DES SAISIES
    // =========================================================================

    @SimpleFunction(description = "Retourne la catégorie choisie dans le spinner (vide si aucune).")
    public String GetSelectedCategory() {
        return shopCategory == null ? "" : shopCategory;
    }

    @SimpleFunction(description = "Retourne le nom de la boutique saisi.")
    public String GetShopFormName() {
        return shopName == null ? "" : shopName.trim();
    }

    @SimpleFunction(description = "Retourne le téléphone du magasin saisi.")
    public String GetShopFormPhone() {
        return shopPhone == null ? "" : shopPhone.trim();
    }

    @SimpleFunction(description = "Retourne l'adresse du magasin saisie.")
    public String GetShopFormAddress() {
        return shopAddress == null ? "" : shopAddress.trim();
    }

    @SimpleFunction(description = "Retourne le lien file:// du logo choisi (vide si aucun).")
    public String GetShopFormLogo() {
        return logoPath == null ? "" : logoPath;
    }

    @SimpleFunction(description = "Retourne tout le formulaire en JSON : name, category, phone, address, logo, uid, formValid.")
    public String GetShopFormJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("name", GetShopFormName());
            o.put("category", GetSelectedCategory());
            o.put("phone", GetShopFormPhone());
            o.put("address", GetShopFormAddress());
            o.put("logo", GetShopFormLogo());
            o.put("uid", prefs().getString(PREF_CURRENT_UID, ""));
            o.put("formValid", firstError() == null);
            return o.toString();
        } catch (Exception e) {
            OnError("GetShopFormJson: " + e.getMessage());
            return "{}";
        }
    }

    // =========================================================================
    // VALIDATION (sécurité : nom et catégorie uniquement)
    // =========================================================================

    // Retourne {clé, message} ou null si le formulaire est valide
    private String[] firstError() {
        if (shopName == null || shopName.trim().isEmpty()) {
            return new String[]{"name", "Le nom de la boutique est obligatoire"};
        }
        if (nameState == 2) {
            return new String[]{"name", "Ce nom est déjà utilisé"};
        }
        if (nameChecking) {
            return new String[]{"name", "Vérification du nom en cours, patiente un instant"};
        }
        if (shopCategory == null || shopCategory.trim().isEmpty()) {
            return new String[]{"category", "Choisis une catégorie"};
        }
        return null;
    }

    @SimpleFunction(description = "Vérifie le formulaire. Retourne vrai si le nom (obligatoire, libre) et la catégorie (obligatoire) sont valides. Sinon retourne faux, affiche l'erreur en rouge sous le champ et déclenche OnShopFormInvalid. À mettre directement dans un « si ».")
    public boolean ValidateShopForm() {
        final String[] err = firstError();
        if (err == null) return true;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if ("name".equals(err[0])) {
                    if (nameField != null) {
                        nameField.error = true;
                        nameField.updateStroke();
                    }
                    if (shopName == null || shopName.trim().isEmpty()) {
                        nameRequiredShown = true;
                    }
                    showNameStatus();
                } else if ("category".equals(err[0])) {
                    if (catField != null) {
                        catField.error = true;
                        catField.updateStroke();
                    }
                    if (catError != null) {
                        catError.setText(err[1]);
                        catError.setVisibility(View.VISIBLE);
                    }
                }
                OnShopFormInvalid(err[0], err[1]);
            }
        });
        return false;
    }

    @SimpleFunction(description = "Retourne le premier message d'erreur du formulaire, ou \"\" si tout est valide (sans rien afficher).")
    public String GetShopFormError() {
        String[] err = firstError();
        return err == null ? "" : err[1];
    }

    // =========================================================================
    // ENVOI AU SERVEUR (multipart/form-data)
    // =========================================================================

    private boolean responseIsSuccess(String body) {
        try {
            JSONObject o = new JSONObject(body.trim());
            if (o.has("success")) return o.optBoolean("success");
            if (o.has("error")) return false;
            return true;
        } catch (Exception e) {
            return true; // 2xx sans JSON : succès
        }
    }

    @SimpleFunction(description = "Envoie la création de boutique au serveur (POST multipart). Champs : uid, name, category, phone, address, formValid=true, et le fichier logo. authorization : en-tête Authorization (ex : Bearer <jeton Firebase>). uid : l'UID Firebase de l'utilisateur, réutilisé par le serveur pour créer le nœud vendeur. Le formulaire est vérifié avant l'envoi. Réponse : OnShopCreated (succès, l'utilisateur devient vendeur pour toujours) ou OnShopCreateFailed.")
    public void SubmitShopForm(final String url, final String authorization, final String uid) {
        if (url == null || url.trim().isEmpty()) {
            OnError("SubmitShopForm: adresse du serveur vide.");
            return;
        }
        if (uid == null || uid.trim().isEmpty()) {
            OnError("SubmitShopForm: uid vide.");
            return;
        }
        if (submitting) return;
        if (!ValidateShopForm()) return;

        submitting = true;
        final String cleanUid = uid.trim();
        prefs().edit().putString(PREF_CURRENT_UID, cleanUid).apply();

        final Map<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("uid", cleanUid);
        fields.put("name", GetShopFormName());
        fields.put("category", GetSelectedCategory());
        fields.put("phone", GetShopFormPhone());
        fields.put("address", GetShopFormAddress());
        fields.put("formValid", "true");
        final String logo = stripFile(GetShopFormLogo());
        final String target = url.trim();
        final String auth = authorization == null ? "" : authorization.trim();

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection c = null;
                try {
                    String boundary = "----Manatest" + System.currentTimeMillis();
                    c = (HttpURLConnection) new URL(target).openConnection();
                    c.setRequestMethod("POST");
                    c.setDoOutput(true);
                    c.setConnectTimeout(20000);
                    c.setReadTimeout(60000);
                    c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    if (!auth.isEmpty()) {
                        c.setRequestProperty("Authorization", auth);
                    }
                    DataOutputStream out = new DataOutputStream(c.getOutputStream());
                    for (Map.Entry<String, String> e : fields.entrySet()) {
                        mpField(out, boundary, e.getKey(), e.getValue());
                    }
                    if (logo != null && !logo.isEmpty()) {
                        File f = new File(logo);
                        if (f.exists()) {
                            out.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
                            out.write("Content-Disposition: form-data; name=\"logo\"; filename=\"logo.jpg\"\r\n".getBytes("UTF-8"));
                            out.write("Content-Type: image/jpeg\r\n\r\n".getBytes("UTF-8"));
                            FileInputStream in = new FileInputStream(f);
                            try {
                                byte[] buf = new byte[8192];
                                int n;
                                while ((n = in.read(buf)) > 0) {
                                    out.write(buf, 0, n);
                                }
                            } finally {
                                try { in.close(); } catch (Exception ignored) { }
                            }
                            out.write("\r\n".getBytes("UTF-8"));
                        }
                    }
                    out.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
                    out.flush();
                    out.close();

                    final int code = c.getResponseCode();
                    InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
                    final String body = readAll(is);
                    final boolean ok = code >= 200 && code < 300 && responseIsSuccess(body);
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            submitting = false;
                            if (ok) {
                                setSeller(cleanUid, true);
                                OnShopCreated(code, body);
                            } else {
                                if (code == 409) {
                                    nameState = 2;
                                    showNameStatus();
                                }
                                OnShopCreateFailed(code, body);
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            submitting = false;
                            OnError("SubmitShopForm: " + e.getMessage());
                        }
                    });
                } finally {
                    if (c != null) c.disconnect();
                }
            }
        }).start();
    }

    // =========================================================================
    // STATUT VENDEUR PERMANENT
    // =========================================================================

    private boolean readSeller(String uid) {
        return uid != null && !uid.isEmpty() && prefs().getBoolean(PREF_SELLER_PREFIX + uid, false);
    }

    // Enregistre le statut ; déclenche OnSellerStatusChanged seulement s'il change
    private void setSeller(String uid, boolean value) {
        if (uid == null || uid.isEmpty()) return;
        boolean old = readSeller(uid);
        SharedPreferences.Editor ed = prefs().edit();
        if (value) {
            ed.putBoolean(PREF_SELLER_PREFIX + uid, true);
        } else {
            ed.remove(PREF_SELLER_PREFIX + uid);
        }
        ed.apply();
        if (old != value) OnSellerStatusChanged(value);
    }

    private Boolean sellerFromJson(JSONObject o) {
        String[] keys = {"seller", "isSeller", "is_seller", "hasShop", "has_shop", "shop"};
        for (int i = 0; i < keys.length; i++) {
            if (o.has(keys[i])) {
                Object v = o.opt(keys[i]);
                if (v instanceof Boolean) return (Boolean) v;
                if (v == null || v == JSONObject.NULL) return Boolean.FALSE;
                if (v instanceof Number) return Boolean.valueOf(((Number) v).intValue() != 0);
                if (v instanceof String) {
                    String s = ((String) v).trim();
                    return Boolean.valueOf("true".equalsIgnoreCase(s) || "1".equals(s)
                            || "seller".equalsIgnoreCase(s) || "vendeur".equalsIgnoreCase(s));
                }
                return Boolean.TRUE; // objet « shop » présent
            }
        }
        if (o.has("role")) {
            String r = o.optString("role", "");
            return Boolean.valueOf("seller".equalsIgnoreCase(r) || "vendeur".equalsIgnoreCase(r));
        }
        JSONObject data = o.optJSONObject("data");
        if (data != null) return sellerFromJson(data);
        return null;
    }

    // Appel réseau synchrone (à lancer hors du fil principal). null = réponse inexploitable
    private Boolean fetchSeller(String url, String auth, String uid) throws Exception {
        String enc = URLEncoder.encode(uid, "UTF-8");
        String target = url.contains("{uid}")
                ? url.replace("{uid}", enc)
                : url + (url.contains("?") ? "&" : "?") + "uid=" + enc;
        String[] r = httpGet(target, auth);
        int code = Integer.parseInt(r[0]);
        if (code >= 400) throw new Exception("serveur " + code);
        return sellerFromJson(new JSONObject(r[1].trim()));
    }

    private void applySellerResult(String uid, boolean isSeller) {
        setSeller(uid, isSeller);
        OnSellerStatusChecked(isSeller);
    }

    @SimpleFunction(description = "Retourne vrai si l'utilisateur connecté est déjà vendeur (statut gardé dans le téléphone et confirmé par le serveur).")
    public boolean IsSeller() {
        return readSeller(prefs().getString(PREF_CURRENT_UID, ""));
    }

    @SimpleFunction(description = "Retourne vrai si la page « Créer ma boutique » doit encore être affichée (faux dès que l'utilisateur est vendeur).")
    public boolean ShouldShowShopCreation() {
        return !IsSeller();
    }

    @SimpleFunction(description = "Demande une fois au serveur si l'utilisateur est vendeur (utile à la reconnexion, même après effacement des données du téléphone). GET url?uid=... (ou {uid} dans l'adresse). Réponse JSON attendue : {\"seller\": true} (ou isSeller, hasShop). Résultat dans OnSellerStatusChecked ; OnSellerStatusChanged si le statut change.")
    public void CheckSellerStatus(final String url, final String authorization, final String uid) {
        if (url == null || url.trim().isEmpty() || uid == null || uid.trim().isEmpty()) {
            OnError("CheckSellerStatus: adresse ou uid vide.");
            return;
        }
        final String cleanUid = uid.trim();
        final String auth = authorization == null ? "" : authorization.trim();
        SetShopUser(cleanUid);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Boolean r = fetchSeller(url.trim(), auth, cleanUid);
                    if (r == null) {
                        fail("CheckSellerStatus: réponse inattendue (seller / isSeller / hasShop manquant).");
                        return;
                    }
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            applySellerResult(cleanUid, r.booleanValue());
                        }
                    });
                } catch (Exception e) {
                    fail("CheckSellerStatus: " + e.getMessage());
                }
            }
        }).start();
    }

    @SimpleFunction(description = "Écoute le serveur en continu : vérifie le statut vendeur tout de suite puis toutes les intervalSeconds secondes (minimum 5) et déclenche OnSellerStatusChanged dès que l'utilisateur devient vendeur. L'écoute s'arrête d'elle-même une fois vendeur.")
    public void StartSellerStatusListener(final String url, final String authorization,
                                          final String uid, int intervalSeconds) {
        if (url == null || url.trim().isEmpty() || uid == null || uid.trim().isEmpty()) {
            OnError("StartSellerStatusListener: adresse ou uid vide.");
            return;
        }
        StopSellerStatusListener();
        final String cleanUid = uid.trim();
        final String cleanUrl = url.trim();
        final String auth = authorization == null ? "" : authorization.trim();
        final long every = Math.max(5, intervalSeconds) * 1000L;
        SetShopUser(cleanUid);
        sellerListening = true;

        sellerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean reported = false;
                while (sellerListening) {
                    try {
                        final Boolean r = fetchSeller(cleanUrl, auth, cleanUid);
                        if (r != null && sellerListening) {
                            runOnUi(new Runnable() {
                                @Override
                                public void run() {
                                    applySellerResult(cleanUid, r.booleanValue());
                                }
                            });
                            if (r.booleanValue()) {
                                sellerListening = false;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        if (!reported) {
                            reported = true;
                            fail("StartSellerStatusListener: " + e.getMessage());
                        }
                    }
                    try {
                        Thread.sleep(every);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        });
        sellerThread.setDaemon(true);
        sellerThread.start();
    }

    @SimpleFunction(description = "Arrête l'écoute du statut vendeur.")
    public void StopSellerStatusListener() {
        sellerListening = false;
        if (sellerThread != null) {
            sellerThread.interrupt();
            sellerThread = null;
        }
    }

    @SimpleFunction(description = "À appeler UNIQUEMENT après la suppression totale du compte : efface le statut vendeur de l'utilisateur courant et arrête l'écoute. La page de création peut alors réapparaître.")
    public void ClearSellerStatus() {
        StopSellerStatusListener();
        setSeller(prefs().getString(PREF_CURRENT_UID, ""), false);
    }

    @SimpleFunction(description = "Vide tous les champs, la catégorie et le logo du formulaire.")
    public void ClearShopForm() {
        shopName = "";
        shopCategory = "";
        shopPhone = "";
        shopAddress = "";
        logoPath = "";
        nameState = 0;
        nameChecking = false;
        nameRequiredShown = false;
        nameSeq++;
        if (shopFormContainer != null && !IsSeller()) renderShopForm();
    }

    // =========================================================================
    // 19. PAGE DÉTAILS PRODUIT (ex-Manatest 4/5)
    //     Utilise les outils déjà présents plus haut : police (shopFont), icônes
    //     (SetShopIconFont), utilisateur (SetShopUser / isOwner), images (loadBitmap).
    // =========================================================================

    private static final String PRODUCT_TAG = "manatest_product";
    private static final int MAX_QTY = 999999;

    private LinearLayout pHolder;
    private JSONObject pData = new JSONObject();
    private final ArrayList<String> pImageUrls = new ArrayList<String>();
    private final HashMap<String, Bitmap> pBitmaps = new HashMap<String, Bitmap>();
    private HorizontalScrollView pHsv;
    private LinearLayout pImagesRow;
    private LinearLayout pDotsRow;
    private TextView pTitleTv;
    private TextView pPriceCurTv;
    private TextView pPriceAmountTv;
    private TextView pStockText;
    private TextView pStockIconTv;
    private TextView pDeliveryText;
    private TextView pDeliveryIconTv;
    private TextView pQtyText;
    private TextView pQtyMinus;
    private TextView pQtyPlus;
    private LinearLayout pDescBlock;
    private ImageView pSellerImg;
    private int pImageToken = 0;
    private int pLogoToken = 0;
    private int pPage = 0;
    private int pQuantity = 1;
    private boolean pDetailsReceived = false;
    private String pShareUrl = "";
    private String pDefaultSellerLogo = "";
    private String pLoadedLogo = "";
    private String pIconStock = "";
    private String pIconDelivery = "";
    private String pIconMinus = "";
    private String pIconPlus = "";
    private int pTopPct = 11;
    private int pBottomPct = 12;

    // ---- événements ----

    @SimpleEvent(description = "Le cercle du vendeur a été touché. sellerUid : l'UID du vendeur du produit. isMine : vrai si c'est ton propre produit (ton UID défini avec SetShopUser). Branche ici BuildShopHome avec sellerUid pour ouvrir la boutique du vendeur.")
    public void OnSellerClick(String sellerUid, boolean isMine) {
        EventDispatcher.dispatchEvent(this, "OnSellerClick", sellerUid, isMine);
    }

    @SimpleEvent(description = "La quantité a changé avec les boutons plus / moins.")
    public void OnProductQuantityChanged(int quantity) {
        EventDispatcher.dispatchEvent(this, "OnProductQuantityChanged", quantity);
    }

    @SimpleEvent(description = "Le bouton plus a été touché alors que la quantité est déjà au maximum (le stock disponible). maxQuantity : ce maximum (0 = rupture de stock). Sert à afficher un message.")
    public void OnProductQuantityLimit(int maxQuantity) {
        EventDispatcher.dispatchEvent(this, "OnProductQuantityLimit", maxQuantity);
    }

    // ---- configuration ----

    @SimpleFunction(description = "Définit les icônes de la page produit par leur caractère Phosphor. stock : icône de la carte En stock. delivery : icône camion de la carte Livraison. minus : icône moins. plus : icône plus. Texte vide = pas d'icône (pour moins et plus, un symbole simple est utilisé). La police d'icônes se règle avec SetShopIconFont. À appeler avant BuildProductPage.")
    public void SetProductIcons(String stock, String delivery, String minus, String plus) {
        pIconStock = stock == null ? "" : stock;
        pIconDelivery = delivery == null ? "" : delivery;
        pIconMinus = minus == null ? "" : minus;
        pIconPlus = plus == null ? "" : plus;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                refreshProductIcons();
            }
        });
    }

    @SimpleFunction(description = "Réserve de l'espace en haut et en bas de la page produit, en pourcentage de la hauteur de l'écran. topPercent : place pour tes boutons retour et partage au-dessus de l'image (11 par défaut, 0 = aucune). bottomPercent : place pour ta barre Envoyer / Panier en bas (12 par défaut). À appeler avant BuildProductPage.")
    public void SetProductPageSpacing(int topPercent, int bottomPercent) {
        pTopPct = Math.max(0, Math.min(40, topPercent));
        pBottomPct = Math.max(0, Math.min(40, bottomPercent));
    }

    @SimpleFunction(description = "Adresse du produit utilisée pour le partage. Mets {uid} là où l'UID du produit doit apparaître (ex: https://monsite.com/produit/{uid}). Sans {uid} : si l'adresse finit par / l'UID est ajouté à la fin, sinon ?uid=... est ajouté.")
    public void SetProductShareUrl(String urlTemplate) {
        pShareUrl = urlTemplate == null ? "" : urlTemplate.trim();
    }

    @SimpleFunction(description = "Image du cercle vendeur quand le serveur n'envoie pas de logo (nom de fichier des Assets, ex: logo-manaplace.png, ou adresse http).")
    public void SetDefaultSellerLogo(String source) {
        pDefaultSellerLogo = source == null ? "" : source.trim();
    }

    // ---- construction ----

    @SimpleFunction(description = "Construit la page détails produit dans l'arrangement scrollable donné (images défilantes avec points, titre, prix, cartes Stock et Livraison, description, quantité, vendeur). productJson : le JSON reçu par ManaplaceUtils.OnProductCardClick. Les détails complets (description, stock, autres images...) s'ajoutent ensuite avec SetProductDetailsFromJson.")
    public void BuildProductPage(final AndroidViewComponent scrollContainer, final String productJson) {
        if (scrollContainer == null || scrollContainer.getView() == null) {
            OnError("BuildProductPage: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    buildProductPageInternal(scrollContainer, productJson);
                } catch (Exception e) {
                    OnError("BuildProductPage: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Reçoit les détails du produit et met la page à jour. detailsJson : par exemple le résultat de ManaplaceUtils.BuildProductDetailsJson (à appeler dans OnProductDetailsReceived), ou la réponse brute du serveur. Clés lues : description, stock (ou quantity), image2 à image5, images, deliveryIncluded, condition, color, size, sellerUid, sellerLogo (ou un objet seller avec uid et logo).")
    public void SetProductDetailsFromJson(final String detailsJson) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    if (detailsJson == null || detailsJson.trim().isEmpty()) return;
                    mergeProduct(new JSONObject(detailsJson));
                    pDetailsReceived = true;
                    renderProduct();
                } catch (Exception e) {
                    OnError("SetProductDetailsFromJson: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Ajoute l'état, la couleur, la taille et le vendeur du produit (si ton serveur les envoie séparément). Les champs vides sont ignorés. sellerUid : l'UID du vendeur. sellerLogo : adresse du logo du vendeur. À appeler après BuildProductPage.")
    public void SetProductExtras(final String condition, final String color, final String size,
                                 final String sellerUid, final String sellerLogo) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    putIfNotEmpty("condition", condition);
                    putIfNotEmpty("color", color);
                    putIfNotEmpty("size", size);
                    putIfNotEmpty("sellerUid", sellerUid);
                    putIfNotEmpty("sellerLogo", sellerLogo);
                    pDetailsReceived = true;
                    renderProduct();
                } catch (Exception e) {
                    OnError("SetProductExtras: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "À appeler en quittant la page produit : retire la page et libère les images.")
    public void ReleaseProductPage() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                removeProductHolder();
                pData = new JSONObject();
                pImageUrls.clear();
                pBitmaps.clear();
                pImageToken++;
                pLogoToken++;
            }
        });
    }

    // ---- quantité et lecture ----

    @SimpleFunction(description = "Retourne la quantité choisie par l'acheteur (1 au minimum, le stock au maximum ; 0 si rupture de stock).")
    public int GetProductQuantity() {
        return pQuantity;
    }

    @SimpleFunction(description = "Change la quantité affichée (elle reste entre 1 et le stock disponible).")
    public void SetProductQuantity(final int quantity) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                pQuantity = quantity;
                clampQuantity();
                updateQuantityView();
            }
        });
    }

    @SimpleFunction(description = "Retourne vrai si le produit affiché appartient à l'utilisateur connecté (UID du vendeur = SetShopUser).")
    public boolean IsProductOwner() {
        return isOwner(pSellerUid());
    }

    @SimpleFunction(description = "Retourne l'UID du produit affiché.")
    public String GetProductUid() {
        return pUid();
    }

    @SimpleFunction(description = "Retourne l'UID du vendeur du produit affiché (vide si le serveur ne l'a pas envoyé).")
    public String GetProductSellerUid() {
        return pSellerUid();
    }

    @SimpleFunction(description = "Retourne le titre du produit affiché.")
    public String GetProductTitle() {
        return pick(pData, "title", "name", "titre");
    }

    @SimpleFunction(description = "Retourne l'adresse de la première image du produit affiché.")
    public String GetProductImage() {
        ArrayList<String> urls = collectImages(pData);
        return urls.isEmpty() ? "" : urls.get(0);
    }

    @SimpleFunction(description = "Retourne le lien de partage du produit (SetProductShareUrl avec l'UID du produit). Vide si SetProductShareUrl n'a pas été appelé.")
    public String GetProductShareLink() {
        return pShareLink();
    }

    @SimpleFunction(description = "Retourne le produit complet en JSON (infos de la carte et détails réunis).")
    public String GetProductJson() {
        return pData.toString();
    }

    // ---- partage ----

    @SimpleFunction(description = "Ouvre le menu de partage Android (WhatsApp ou toute autre application) avec la photo du produit, son titre, son prix et son lien (SetProductShareUrl). Si la photo ne peut pas être préparée, le texte et le lien sont partagés seuls. À brancher sur ton bouton partager.")
    public void ShareProduct() {
        if (pData.length() == 0) {
            OnError("ShareProduct: aucun produit chargé (appelle BuildProductPage d'abord).");
            return;
        }
        final String title = pick(pData, "title", "name", "titre");
        final String uid = pUid();
        final String link = pShareLink();
        ArrayList<String> urls = collectImages(pData);
        final String firstUrl = urls.isEmpty() ? "" : urls.get(0);
        final Bitmap cached = firstUrl.isEmpty() ? null : pBitmaps.get(firstUrl);

        StringBuilder sb = new StringBuilder(title);
        String amount = pPriceAmount();
        if (!amount.isEmpty()) sb.append("\n").append(pPriceCur()).append(" ").append(amount);
        if (!link.isEmpty()) sb.append("\n").append(link);
        final String body = sb.toString();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = cached;
                if (bmp == null && !firstUrl.isEmpty()) {
                    try {
                        bmp = loadBitmap(firstUrl, 1080);
                    } catch (Exception ignored) {
                    }
                }
                final Uri uri = bmp == null ? null
                        : saveBitmapForShare(bmp, "manaplace_" + (uid.isEmpty() ? String.valueOf(System.currentTimeMillis()) : uid));
                runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        startShare(uri, title, body);
                    }
                });
            }
        }).start();
    }

    private Uri saveBitmapForShare(Bitmap bmp, String name) {
        try {
            android.content.ContentResolver cr = context.getContentResolver();
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Images.Media.DISPLAY_NAME, name + ".jpg");
                v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                v.put("relative_path", "Pictures/Manaplace");
                v.put("is_pending", 1);
                Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                if (uri == null) return null;
                OutputStream os = cr.openOutputStream(uri);
                if (os == null) return null;
                bmp.compress(Bitmap.CompressFormat.JPEG, 92, os);
                os.close();
                ContentValues done = new ContentValues();
                done.put("is_pending", 0);
                cr.update(uri, done, null, null);
                return uri;
            }
            String p = MediaStore.Images.Media.insertImage(cr, bmp, name, "");
            return p == null ? null : Uri.parse(p);
        } catch (Exception e) {
            return null;
        }
    }

    private void startShare(Uri uri, String title, String body) {
        try {
            Intent it = new Intent(Intent.ACTION_SEND);
            if (uri != null) {
                it.setType("image/*");
                it.putExtra(Intent.EXTRA_STREAM, uri);
                it.setClipData(ClipData.newRawUri("", uri));
                it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                it.setType("text/plain");
            }
            it.putExtra(Intent.EXTRA_SUBJECT, title);
            it.putExtra(Intent.EXTRA_TEXT, body);
            Intent chooser = Intent.createChooser(it, "Partager");
            if (uri != null) {
                chooser.setClipData(ClipData.newRawUri("", uri));
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            activity.startActivity(chooser);
        } catch (Exception e) {
            OnError("ShareProduct: " + e.getMessage());
        }
    }

    // ---- données ----

    private void putIfNotEmpty(String key, String value) throws Exception {
        if (value != null && !value.trim().isEmpty()) pData.put(key, value.trim());
    }

    // Fusionne un JSON produit (objet direct, ou enveloppé dans product / data) sans écraser par du vide
    private void mergeProduct(JSONObject src) throws Exception {
        JSONObject o = src;
        JSONObject nested = src.optJSONObject("product");
        if (nested != null) {
            o = nested;
        } else {
            JSONObject d = src.optJSONObject("data");
            if (d != null) o = d;
        }
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            Object v = o.opt(k);
            if (v == null || v == JSONObject.NULL) continue;
            if (v instanceof String && ((String) v).trim().isEmpty()) continue;
            pData.put(k, v);
        }
    }

    private String pUid() {
        return pick(pData, "productUid", "uid", "id");
    }

    private String pSellerUid() {
        String u = pick(pData, "sellerUid", "seller_uid", "ownerUid", "owner_uid", "shopUid", "shop_uid",
                "vendorUid", "vendeurUid", "uidVendeur", "userUid", "user_uid");
        if (u.isEmpty()) {
            JSONObject s = pData.optJSONObject("seller");
            if (s == null) s = pData.optJSONObject("shop");
            if (s != null) u = pick(s, "uid", "id", "sellerUid");
        }
        return u;
    }

    private String pSellerLogo() {
        String l = pick(pData, "sellerLogo", "seller_logo", "shopLogo", "shop_logo", "sellerImage",
                "sellerPhoto", "vendeurLogo");
        if (l.isEmpty()) {
            JSONObject s = pData.optJSONObject("seller");
            if (s == null) s = pData.optJSONObject("shop");
            if (s != null) l = pick(s, "logo", "photo", "image", "avatar", "logoUrl", "logo_url");
        }
        return l;
    }

    private String pPriceRaw() {
        return pData.has("price_display") ? pick(pData, "price_display") : pick(pData, "price");
    }

    private String pPriceCur() {
        String raw = pPriceRaw();
        String cur = pick(pData, "currency", "devise").toUpperCase(Locale.ROOT);
        if (cur.equals("USD") || raw.contains("$")) return "$";
        if (cur.equals("EUR") || raw.contains("€")) return "€";
        if (cur.isEmpty() || cur.equals("HTG")) return "G";
        return cur;
    }

    private String pPriceAmount() {
        String raw = pPriceRaw().replaceAll("^[^0-9]+|[^0-9]+$", "").trim();
        return raw.replaceAll("[.,]0{1,2}$", "");
    }

    private String pShareLink() {
        String t = pShareUrl == null ? "" : pShareUrl.trim();
        if (t.isEmpty()) return "";
        try {
            String enc = URLEncoder.encode(pUid(), "UTF-8");
            if (t.contains("{uid}")) return t.replace("{uid}", enc);
            if (t.endsWith("/")) return t + enc;
            return t + (t.contains("?") ? "&" : "?") + "uid=" + enc;
        } catch (Exception e) {
            return t;
        }
    }

    private boolean pBool(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number) return ((Number) v).intValue() != 0;
        if (v instanceof String) {
            String s = ((String) v).trim().toLowerCase(Locale.ROOT);
            return s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("oui");
        }
        return false;
    }

    // {connu ? 1 : 0, valeur}
    private int[] pStock() {
        if (pData.has("stock") && !pData.isNull("stock")) return new int[]{1, pData.optInt("stock", 0)};
        if (pData.has("quantity") && !pData.isNull("quantity")) return new int[]{1, pData.optInt("quantity", 0)};
        return new int[]{0, 0};
    }

    private int pMaxQty() {
        int[] s = pStock();
        return s[0] == 1 ? Math.max(0, s[1]) : MAX_QTY;
    }

    private void clampQuantity() {
        int max = pMaxQty();
        if (max <= 0) {
            pQuantity = 0;
        } else {
            if (pQuantity < 1) pQuantity = 1;
            if (pQuantity > max) pQuantity = max;
        }
    }

    private void addOneImage(ArrayList<String> out, String s) {
        if (s == null) return;
        s = s.trim();
        if (s.isEmpty() || "null".equals(s)) return;
        if (!out.contains(s)) out.add(s);
    }

    private void addImagesFrom(ArrayList<String> out, Object v) {
        if (v == null || v == JSONObject.NULL) return;
        try {
            if (v instanceof JSONArray) {
                JSONArray a = (JSONArray) v;
                for (int i = 0; i < a.length(); i++) {
                    Object e = a.opt(i);
                    if (e instanceof JSONObject) {
                        addOneImage(out, pick((JSONObject) e, "url", "src", "image", "link"));
                    } else if (e != null && e != JSONObject.NULL) {
                        addOneImage(out, String.valueOf(e));
                    }
                }
            } else if (v instanceof String) {
                String s = ((String) v).trim();
                if (s.startsWith("[")) addImagesFrom(out, new JSONArray(s));
                else addOneImage(out, s);
            }
        } catch (Exception ignored) {
        }
    }

    private ArrayList<String> collectImages(JSONObject o) {
        ArrayList<String> out = new ArrayList<String>();
        addImagesFrom(out, o.opt("images"));
        addImagesFrom(out, o.opt("image"));
        for (int i = 2; i <= 5; i++) addImagesFrom(out, o.opt("image" + i));
        return out;
    }

    // ---- construction de la vue ----

    private int padPx() {
        return (int) (screenW * 0.033f);
    }

    private void buildProductPageInternal(AndroidViewComponent container, String productJson) {
        ScrollView sc = findScrollView(container.getView(), 0);
        if (sc == null) {
            OnError("BuildProductPage: le conteneur doit être un arrangement vertical scrollable.");
            return;
        }
        if (sc.getChildCount() == 0 || !(sc.getChildAt(0) instanceof LinearLayout)
                || ((LinearLayout) sc.getChildAt(0)).getOrientation() != LinearLayout.VERTICAL) {
            OnError("BuildProductPage: contenu de l'arrangement scrollable inattendu.");
            return;
        }
        LinearLayout inner = (LinearLayout) sc.getChildAt(0);

        removeProductHolder();
        for (int i = inner.getChildCount() - 1; i >= 0; i--) {
            if (PRODUCT_TAG.equals(inner.getChildAt(i).getTag())) inner.removeViewAt(i);
        }

        pData = new JSONObject();
        pImageUrls.clear();
        pBitmaps.clear();
        pImageToken++;
        pLogoToken++;
        pPage = 0;
        pQuantity = 1;
        pDetailsReceived = false;
        pLoadedLogo = "";
        try {
            if (productJson != null && !productJson.trim().isEmpty()) {
                mergeProduct(new JSONObject(productJson));
            }
        } catch (Exception e) {
            OnError("BuildProductPage: productJson invalide (" + e.getMessage() + ").");
        }

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;
        int pad = padPx();
        int black = Color.BLACK;
        int green = Color.argb(0xFF, 0x85, 0xED, 0x82);

        LinearLayout holder = new LinearLayout(context);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setTag(PRODUCT_TAG);
        holder.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // place pour les boutons retour / partage (les tiens)
        View top = new View(context);
        top.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (screenH * pTopPct / 100f)));
        holder.addView(top);

        holder.addView(buildProductImages());

        // titre : taille 25, noir
        pTitleTv = text("", 25, black);
        pTitleTv.setPadding(pad, dp(18), pad, 0);
        holder.addView(pTitleTv);

        // prix : devise petite + montant plus grand
        LinearLayout priceRow = new LinearLayout(context);
        priceRow.setOrientation(LinearLayout.HORIZONTAL);
        priceRow.setPadding(pad, dp(10), pad, dp(14));
        pPriceCurTv = text("", 16, black);
        pPriceCurTv.setPadding(0, 0, dp(6), 0);
        pPriceAmountTv = text("", 30, black);
        priceRow.addView(pPriceCurTv);
        priceRow.addView(pPriceAmountTv);
        holder.addView(priceRow);

        View sep = new View(context);
        sep.setBackgroundColor(Color.parseColor("#EDEDED"));
        sep.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        holder.addView(sep);

        // cartes Stock (40% x 10%) et Livraison (50% x 10%)
        int cardH = (int) (screenH * 0.10f);
        int gap = (int) (screenW * 0.03f);
        LinearLayout statRow = new LinearLayout(context);
        statRow.setOrientation(LinearLayout.HORIZONTAL);
        statRow.setPadding(pad, dp(16), 0, 0);

        LinearLayout stockCard = new LinearLayout(context);
        stockCard.setOrientation(LinearLayout.HORIZONTAL);
        stockCard.setGravity(Gravity.CENTER);
        stockCard.setLayoutParams(new LinearLayout.LayoutParams((int) (screenW * 0.40f), cardH));
        GradientDrawable stockBg = new GradientDrawable();
        stockBg.setColor(Color.argb(0xE9, 0x1A, 0x1A, 0x1B));
        stockCard.setBackground(stockBg);
        pStockIconTv = prodIcon(pIconStock, "", 20, green);
        pStockIconTv.setPadding(0, 0, dp(8), 0);
        pStockText = text("En stock", 12, green);
        stockCard.addView(pStockIconTv);
        stockCard.addView(pStockText);
        statRow.addView(stockCard);

        LinearLayout deliveryCard = new LinearLayout(context);
        deliveryCard.setOrientation(LinearLayout.HORIZONTAL);
        deliveryCard.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams((int) (screenW * 0.50f), cardH);
        dlp.setMargins(gap, 0, 0, 0);
        deliveryCard.setLayoutParams(dlp);
        GradientDrawable delBg = new GradientDrawable();
        delBg.setColor(Color.WHITE);
        delBg.setStroke(dp(1), black);
        deliveryCard.setBackground(delBg);
        pDeliveryIconTv = prodIcon(pIconDelivery, "", 20, black);
        pDeliveryIconTv.setPadding(0, 0, dp(8), 0);
        pDeliveryText = text("Livraison inclus", 12, black);
        deliveryCard.addView(pDeliveryIconTv);
        deliveryCard.addView(pDeliveryText);
        statRow.addView(deliveryCard);
        holder.addView(statRow);

        // description (le texte du produit prend la place du mot « Description »)
        pDescBlock = new LinearLayout(context);
        pDescBlock.setOrientation(LinearLayout.VERTICAL);
        pDescBlock.setPadding(pad, dp(18), pad, 0);
        holder.addView(pDescBlock);

        // quantité : carte 40% x 10%, fond F5F5F5, texte 20
        TextView qtyTitle = text("Quantité", 15, black);
        qtyTitle.setPadding(pad, dp(22), pad, 0);
        holder.addView(qtyTitle);

        LinearLayout qtyCard = new LinearLayout(context);
        qtyCard.setOrientation(LinearLayout.HORIZONTAL);
        qtyCard.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams((int) (screenW * 0.40f), cardH);
        qlp.setMargins(pad, dp(10), 0, 0);
        qtyCard.setLayoutParams(qlp);
        GradientDrawable qBg = new GradientDrawable();
        qBg.setColor(Color.rgb(0xF5, 0xF5, 0xF5));
        qBg.setStroke(dp(1), black);
        qtyCard.setBackground(qBg);

        pQtyMinus = prodIcon(pIconMinus, "−", 22, black);
        pQtyMinus.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        pQtyMinus.setClickable(true);
        pQtyMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pQuantity > 1) {
                    pQuantity--;
                    updateQuantityView();
                    OnProductQuantityChanged(pQuantity);
                }
            }
        });
        pQtyText = text("1", 20, black);
        pQtyText.setGravity(Gravity.CENTER);
        pQtyText.setMinWidth(dp(40));
        pQtyPlus = prodIcon(pIconPlus, "+", 22, black);
        pQtyPlus.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        pQtyPlus.setClickable(true);
        pQtyPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int max = pMaxQty();
                if (pQuantity < max) {
                    pQuantity++;
                    updateQuantityView();
                    OnProductQuantityChanged(pQuantity);
                } else {
                    OnProductQuantityLimit(max);
                }
            }
        });
        qtyCard.addView(pQtyMinus);
        qtyCard.addView(pQtyText);
        qtyCard.addView(pQtyPlus);
        holder.addView(qtyCard);

        // vendeur : titre 15 + cercle avec le logo
        TextView sellerTitle = text("Vendeur", 15, black);
        sellerTitle.setPadding(pad, dp(22), pad, 0);
        holder.addView(sellerTitle);

        int sz = (int) (screenW * 0.15f);
        LinearLayout sellerWrap = new LinearLayout(context);
        sellerWrap.setOrientation(LinearLayout.HORIZONTAL);
        sellerWrap.setPadding(pad, dp(10), pad, 0);
        pSellerImg = new ImageView(context);
        pSellerImg.setLayoutParams(new LinearLayout.LayoutParams(sz, sz));
        pSellerImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(Color.parseColor("#F0F0F0"));
        pSellerImg.setBackground(circleBg);
        pSellerImg.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        pSellerImg.setClipToOutline(true);
        pSellerImg.setClickable(true);
        pSellerImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String su = pSellerUid();
                if (su.isEmpty()) {
                    OnError("[diag] uid du vendeur inconnu : le serveur doit envoyer sellerUid avec le produit (ou utilise SetProductExtras).");
                    return;
                }
                OnSellerClick(su, isOwner(su));
            }
        });
        sellerWrap.addView(pSellerImg);
        holder.addView(sellerWrap);

        // place pour ta barre Envoyer / Panier
        View bottom = new View(context);
        bottom.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (screenH * pBottomPct / 100f)));
        holder.addView(bottom);

        inner.addView(holder, 0);
        pHolder = holder;
        renderProduct();
        final ScrollView scf = sc;
        scf.post(new Runnable() {
            @Override
            public void run() {
                scf.scrollTo(0, 0);
            }
        });
    }

    private View buildProductImages() {
        int imageH = (int) (screenW * 0.605f);
        FrameLayout frame = new FrameLayout(context);
        frame.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, imageH));

        pHsv = new HorizontalScrollView(context) {
            @Override
            protected void onScrollChanged(int l, int t, int oldl, int oldt) {
                super.onScrollChanged(l, t, oldl, oldt);
                updateDotsFromScroll(l);
            }
        };
        pHsv.setHorizontalScrollBarEnabled(false);
        pHsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        pHsv.setFillViewport(false);
        final HorizontalScrollView hsv = pHsv;
        final int[] startPage = {0};
        hsv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                int w = screenW;
                int action = e.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    startPage[0] = Math.round(hsv.getScrollX() / (float) w);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    float off = hsv.getScrollX() - startPage[0] * w;
                    int page = startPage[0];
                    if (off > w * 0.15f) page++;
                    else if (off < -w * 0.15f) page--;
                    int last = Math.max(0, Math.max(1, pImageUrls.size()) - 1);
                    page = Math.max(0, Math.min(last, page));
                    hsv.smoothScrollTo(page * w, 0);
                    return true;
                }
                return false;
            }
        });
        pImagesRow = new LinearLayout(context);
        pImagesRow.setOrientation(LinearLayout.HORIZONTAL);
        pHsv.addView(pImagesRow, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, imageH));
        frame.addView(pHsv, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // points de pagination (visibles seulement s'il y a plusieurs images)
        pDotsRow = new LinearLayout(context);
        pDotsRow.setOrientation(LinearLayout.HORIZONTAL);
        pDotsRow.setGravity(Gravity.CENTER);
        pDotsRow.setPadding(dp(8), dp(5), dp(8), dp(5));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(Color.argb(0x66, 0x1A, 0x1A, 0x1B));
        pill.setCornerRadius(dp(12));
        pDotsRow.setBackground(pill);
        pDotsRow.setVisibility(View.GONE);
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dlp.bottomMargin = dp(10);
        frame.addView(pDotsRow, dlp);
        return frame;
    }

    private void updateDotsFromScroll(int x) {
        if (screenW <= 0) return;
        int count = pDotsRow == null ? 0 : pDotsRow.getChildCount();
        if (count <= 1) return;
        int page = Math.max(0, Math.min(count - 1, Math.round(x / (float) screenW)));
        if (page != pPage) {
            pPage = page;
            updateDots(page);
        }
    }

    private void updateDots(int page) {
        if (pDotsRow == null) return;
        for (int i = 0; i < pDotsRow.getChildCount(); i++) {
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(i == page ? Color.WHITE : Color.argb(0x80, 255, 255, 255));
            pDotsRow.getChildAt(i).setBackground(g);
        }
    }

    // Recrée les pages d'images si la liste a changé (les images déjà chargées sont réutilisées)
    private void syncImages() {
        if (pImagesRow == null || pDotsRow == null) return;
        ArrayList<String> urls = collectImages(pData);
        if (urls.equals(pImageUrls) && pImagesRow.getChildCount() > 0) return;
        pImageUrls.clear();
        pImageUrls.addAll(urls);
        final int token = ++pImageToken;
        int imageH = (int) (screenW * 0.605f);

        pImagesRow.removeAllViews();
        pDotsRow.removeAllViews();
        if (urls.isEmpty()) {
            View ph = new View(context);
            ph.setBackgroundColor(Color.parseColor("#F5F5F5"));
            ph.setLayoutParams(new LinearLayout.LayoutParams(screenW, imageH));
            pImagesRow.addView(ph);
        }
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            ImageView iv = new ImageView(context);
            iv.setLayoutParams(new LinearLayout.LayoutParams(screenW, imageH));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(Color.parseColor("#F5F5F5"));
            Bitmap cached = pBitmaps.get(url);
            if (cached != null) iv.setImageBitmap(cached);
            else loadProductImage(url, iv, token);
            pImagesRow.addView(iv);

            View dot = new View(context);
            LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(dp(6), dp(6));
            dp1.setMargins(dp(3), 0, dp(3), 0);
            dot.setLayoutParams(dp1);
            pDotsRow.addView(dot);
        }
        pPage = 0;
        updateDots(0);
        pDotsRow.setVisibility(urls.size() > 1 ? View.VISIBLE : View.GONE);
        if (pHsv != null) pHsv.scrollTo(0, 0);
    }

    private void loadProductImage(final String url, final ImageView iv, final int token) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bmp = loadBitmap(url, screenW);
                    if (bmp == null) {
                        fail("Image produit introuvable : " + url);
                        return;
                    }
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            pBitmaps.put(url, bmp);
                            if (token == pImageToken) iv.setImageBitmap(bmp);
                        }
                    });
                } catch (Exception e) {
                    fail("Image produit : " + e.getMessage());
                }
            }
        }).start();
    }

    private void loadSellerLogo(final String src) {
        final int token = ++pLogoToken;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bmp = loadBitmap(src, dp(200));
                    if (bmp == null) return;
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            if (token == pLogoToken && pSellerImg != null) pSellerImg.setImageBitmap(bmp);
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    // Icône Phosphor si un caractère est donné, sinon symbole simple (ou rien)
    private TextView prodIcon(String ch, String fallback, int sp, int color) {
        TextView t = new TextView(context);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        applyIcon(t, ch, fallback);
        boolean empty = (ch == null || ch.isEmpty()) && fallback.isEmpty();
        t.setVisibility(empty ? View.GONE : View.VISIBLE);
        return t;
    }

    private void refreshProductIcons() {
        if (pStockIconTv != null) {
            applyIcon(pStockIconTv, pIconStock, "");
            pStockIconTv.setVisibility(pIconStock.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (pDeliveryIconTv != null) {
            applyIcon(pDeliveryIconTv, pIconDelivery, "");
            pDeliveryIconTv.setVisibility(pIconDelivery.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (pQtyMinus != null) applyIcon(pQtyMinus, pIconMinus, "−");
        if (pQtyPlus != null) applyIcon(pQtyPlus, pIconPlus, "+");
    }

    private void setStrike(TextView t, boolean on) {
        if (t == null) return;
        int f = t.getPaintFlags();
        t.setPaintFlags(on ? (f | Paint.STRIKE_THRU_TEXT_FLAG) : (f & ~Paint.STRIKE_THRU_TEXT_FLAG));
    }

    private void updateQuantityView() {
        if (pQtyText != null) pQtyText.setText(String.valueOf(pQuantity));
    }

    private void addDescLine(String s) {
        TextView t = text(s, 13, Color.argb(0xBC, 0x01, 0x01, 0x01));
        t.setPadding(0, dp(6), 0, 0);
        pDescBlock.addView(t);
    }

    private void removeProductHolder() {
        if (pHolder != null && pHolder.getParent() instanceof ViewGroup) {
            ((ViewGroup) pHolder.getParent()).removeView(pHolder);
        }
        pHolder = null;
        pHsv = null;
        pImagesRow = null;
        pDotsRow = null;
        pTitleTv = null;
        pPriceCurTv = null;
        pPriceAmountTv = null;
        pStockText = null;
        pStockIconTv = null;
        pDeliveryText = null;
        pDeliveryIconTv = null;
        pQtyText = null;
        pQtyMinus = null;
        pQtyPlus = null;
        pDescBlock = null;
        pSellerImg = null;
    }

    // Met toute la page à jour à partir des données reçues
    private void renderProduct() {
        if (pHolder == null) return;

        pTitleTv.setText(pick(pData, "title", "name", "titre"));
        String amount = pPriceAmount();
        pPriceCurTv.setText(amount.isEmpty() ? "" : pPriceCur());
        pPriceAmountTv.setText(amount);

        syncImages();

        // stock et livraison : texte barré s'il n'y en a pas
        int[] st = pStock();
        setStrike(pStockText, st[0] == 1 && st[1] <= 0);
        boolean dKnown = pData.has("deliveryIncluded") || pData.has("delivery_included");
        boolean dOn = dKnown && pBool(pData.opt(pData.has("deliveryIncluded") ? "deliveryIncluded" : "delivery_included"));
        setStrike(pDeliveryText, dKnown && !dOn);

        // description puis état, couleur, taille
        pDescBlock.removeAllViews();
        String desc = pick(pData, "description", "desc");
        String cond = pick(pData, "condition", "etat", "état", "state");
        String color = pick(pData, "color", "couleur");
        String size = pick(pData, "size", "taille");
        boolean any = false;
        if (!desc.isEmpty()) {
            addDescLine(desc);
            any = true;
        }
        if (!cond.isEmpty()) {
            addDescLine("État : " + cond);
            any = true;
        }
        if (!color.isEmpty()) {
            addDescLine("Couleur : " + color);
            any = true;
        }
        if (!size.isEmpty()) {
            addDescLine("Taille : " + size);
            any = true;
        }
        if (!any && !pDetailsReceived) addDescLine("Description");
        pDescBlock.setVisibility(any || !pDetailsReceived ? View.VISIBLE : View.GONE);

        clampQuantity();
        updateQuantityView();

        // logo du vendeur
        String logo = pSellerLogo();
        if (logo.isEmpty()) logo = pDefaultSellerLogo;
        if (!logo.equals(pLoadedLogo)) {
            pLoadedLogo = logo;
            if (!logo.isEmpty()) loadSellerLogo(logo);
        }
    }

    // =========================================================================
    // 20. PAGES MODIFIER LA BOUTIQUE / PRIX / ÉTAT ET VARIANTES (ex-Manatest 5/5)
    //     Renommés pour éviter les conflits avec la page « Créer ma boutique » :
    //     GetShopEditName / GetShopEditPhone / GetShopEditAddress / GetShopEditLogo /
    //     GetShopEditCategory (au lieu de GetShop... / GetSelectedCategory),
    //     OpenShopEditLogoPicker (au lieu de OpenLogoPicker), CardFieldView (au lieu de FieldView).
    //     Partagés avec la page Créer ma boutique : SetShopCategories, SetShopNameCheckUrl,
    //     SetShopUser, LoadCustomFont, LoadCustomBoldFont, SetRadioButtonColor et les
    //     événements AfterCategorySelected, OnShopNameChecked, OnShopFormInvalid, OnShopLogoPicked.
    // =========================================================================

    private ViewGroup activeContainer;

    // Page 1 : modifier la boutique
    private ViewGroup editContainer;
    private String eShopName = "";
    private String eShopCategory = "";
    private String eShopPhone = "";
    private String eShopAddress = "";
    private String eLogoPath = "";
    private String logoCurrent = "";
    private boolean logoChanged = false;
    private String origName = "";
    private String origCategory = "";
    private String origPhone = "";
    private String origAddress = "";
    private CardFieldView eNameField;
    private CardFieldView ePhoneField;
    private CardFieldView eAddressField;
    private CardFieldView eCatField;
    private TextView eCatValue;
    private TextView eCatError;
    private TextView eNameStatus;
    private ImageView eLogoImage;
    private View eLogoGlyph;
    private int eLogoToken = 0;
    private int eNameSeq = 0;
    private boolean eNameChecking = false;
    private int eNameState = 0; // 0 inconnu, 1 disponible, 2 déjà utilisé, 3 erreur réseau
    private boolean eNameRequiredShown = false;
    private Runnable eNameRunnable;
    private boolean eSubmitting = false;

    // Page 2 : prix
    private ViewGroup priceContainer;
    private String priceText = "";
    private String feeText = "";
    private String priceCurrency = "HTG";
    private boolean deliveryOn = false;
    private CardFieldView priceField;
    private CardFieldView feeField;
    private TextView priceCurTv;
    private TextView deliveryTv;
    private TextView totalValueTv;
    private TextView totalCurTv;
    private TextView priceErr;
    private TextView feeErr;

    // Page 3 : état et variantes
    private ViewGroup variantContainer;
    private String condition = "";
    private String color = "";
    private String size = "";
    private final List<String> conditions = new ArrayList<String>();
    {
        conditions.add("Neuf");
        conditions.add("Bon état");
        conditions.add("Comme neuf");
        conditions.add("Assez bon état");
    }
    private CardFieldView condField;
    private CardFieldView colorField;
    private CardFieldView sizeField;
    private LinearLayout swatchRow;

    private int editLogoRequestCode;

    private String currentUserUid() {
        return prefs().getString(PREF_CURRENT_UID, "");
    }

    @SimpleEvent(description = "Le serveur a accepté la modification de la boutique. responseCode : code HTTP, response : texte renvoyé.")
    public void OnShopUpdated(int responseCode, String response) {
        EventDispatcher.dispatchEvent(this, "OnShopUpdated", responseCode, response);
    }

    @SimpleEvent(description = "Le serveur a refusé la modification de la boutique (ou erreur serveur). Le code 409 signale un nom déjà pris.")
    public void OnShopUpdateFailed(int responseCode, String response) {
        EventDispatcher.dispatchEvent(this, "OnShopUpdateFailed", responseCode, response);
    }

    @SimpleEvent(description = "La devise a changé dans la page Prix. currency : HTG ou USD.")
    public void OnCurrencyChanged(String currency) {
        EventDispatcher.dispatchEvent(this, "OnCurrencyChanged", currency);
    }

    @SimpleEvent(description = "Le choix des frais de livraison a changé. included : vrai pour Oui, faux pour Non.")
    public void OnDeliveryChoiceChanged(boolean included) {
        EventDispatcher.dispatchEvent(this, "OnDeliveryChoiceChanged", included);
    }

    @SimpleEvent(description = "La page Prix est invalide. fieldKey : price ou deliveryFee. message : le texte de l'erreur (aussi affiché en rouge à l'écran).")
    public void OnPriceFormInvalid(String fieldKey, String message) {
        EventDispatcher.dispatchEvent(this, "OnPriceFormInvalid", fieldKey, message);
    }

    @SimpleEvent(description = "Un état a été choisi dans la boîte de dialogue (Neuf, Bon état...). condition : l'état choisi.")
    public void OnConditionSelected(String condition) {
        EventDispatcher.dispatchEvent(this, "OnConditionSelected", condition);
    }

    private interface TextCb {
        void on(String value);
    }

    private interface PickCb {
        void on(String value);
    }

    // Toutes les tailles de composants sont des pourcentages de l'écran
    private int wp(float pct) {
        return Math.round(context.getResources().getDisplayMetrics().widthPixels * pct / 100f);
    }

    private int hp(float pct) {
        return Math.round(context.getResources().getDisplayMetrics().heightPixels * pct / 100f);
    }

    private int fieldH() {
        return Math.max(hp(8.5f), dp(48));
    }

    private void clearAllFocus() {
        CardFieldView[] all = {eNameField, ePhoneField, eAddressField, priceField, feeField, colorField, sizeField};
        for (int i = 0; i < all.length; i++) {
            if (all[i] != null && all[i].edit != null) all[i].edit.clearFocus();
        }
    }

    private LinearLayout cardSection() {
        LinearLayout s = new LinearLayout(context);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        s.setPadding(wp(5.5f), hp(2f), wp(5.5f), hp(2f));
        return s;
    }

    private TextView cardDesc(String s) {
        TextView t = text(s, 12, cTitle(), false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = hp(1.5f);
        p.leftMargin = wp(0.5f);
        t.setLayoutParams(p);
        return t;
    }

    // Titre de bloc : taille 15, noir
    private TextView blockTitle(String s) {
        TextView t = text(s, 15, Color.BLACK, true);
        t.setPadding(wp(0.5f), 0, 0, hp(1.5f));
        return t;
    }

    private TextView errorText() {
        TextView t = text("", 12, COLOR_ERROR, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = hp(0.8f);
        p.leftMargin = wp(0.5f);
        t.setLayoutParams(p);
        t.setVisibility(View.GONE);
        return t;
    }

    private void addSeparatorTo(ViewGroup parent) {
        View v = new View(context);
        v.setBackgroundColor(Color.parseColor("#F5F5F5"));
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, hp(1.8f)));
        parent.addView(v);
    }

    // ---- champ à libellé animé (même design que la création de boutique) ----

    private class CardFieldView {
        final FrameLayout box;
        final TextView label;
        final GradientDrawable bg;
        final int fh = fieldH();
        EditText edit;
        TextView value;
        boolean floated = false;
        boolean focused = false;
        boolean error = false;
        boolean disabled = false;

        CardFieldView(String hint) {
            box = new FrameLayout(context);
            box.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, fh));
            bg = new GradientDrawable();
            bg.setColor(Color.TRANSPARENT);
            bg.setCornerRadius(fh * 0.375f);
            box.setBackground(bg);

            label = text(hint, 15, cHint(), false);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, Math.round(fh * 0.34f),
                    Gravity.CENTER_VERTICAL | Gravity.START);
            lp.leftMargin = wp(5.5f);
            label.setLayoutParams(lp);
            label.setGravity(Gravity.CENTER_VERTICAL);
            label.setSingleLine(true);
            label.setClickable(false);
            label.setPivotX(0f);
            label.setPivotY(fh * 0.17f);
            updateStroke();
        }

        void attachLabel() {
            box.addView(label);
        }

        void updateStroke() {
            int w;
            int c;
            if (focused) {
                w = dp(2);
                c = COLOR_FOCUS;
            } else if (error) {
                w = dp(1);
                c = COLOR_ERROR;
            } else {
                w = dp(1);
                c = cStroke();
            }
            bg.setStroke(w, c);
        }

        void setFloated(boolean f, boolean animate) {
            if (animate && floated == f) return;
            floated = f;
            float ty = f ? -(fh * 0.27f) : 0f;
            float sc = f ? 0.8f : 1f;
            if (animate) {
                label.animate()
                        .translationY(ty)
                        .scaleX(sc)
                        .scaleY(sc)
                        .setDuration(160)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            } else {
                label.animate().cancel();
                label.setTranslationY(ty);
                label.setScaleX(sc);
                label.setScaleY(sc);
            }
        }

        // Carte grisée : la saisie est impossible
        void setDisabled(boolean d) {
            disabled = d;
            label.setAlpha(d ? 0.45f : 1f);
            if (edit != null) {
                edit.setEnabled(!d);
                edit.setFocusable(!d);
                edit.setFocusableInTouchMode(!d);
                if (d) edit.clearFocus();
            }
        }
    }

    private CardFieldView editField(String hint, int inputType, int maxLen, int imeAction,
                                String initial, final TextCb cb, float rightPadPct) {
        final CardFieldView f = new CardFieldView(hint);
        final EditText e = new EditText(context);
        e.setBackground(null);
        e.setTextSize(15);
        e.setTextColor(cInput());
        e.setHintTextColor(cHint());
        Typeface ef = shopFont();
        e.setTypeface(ef != null ? ef : Typeface.DEFAULT);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setImeOptions(imeAction);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLen)});
        e.setPadding(wp(5.5f), Math.round(f.fh * 0.375f), wp(rightPadPct), Math.round(f.fh * 0.09f));
        e.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        f.edit = e;
        f.box.addView(e, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        f.attachLabel();

        restoring = true;
        e.setText(initial);
        restoring = false;
        f.setFloated(initial != null && !initial.isEmpty(), false);

        e.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                f.focused = hasFocus;
                f.updateStroke();
                f.setFloated(hasFocus || e.getText().length() > 0, true);
            }
        });

        e.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable ed) {
                if (restoring) return;
                String s = ed.toString();
                if (s.length() > 0 && !f.floated) f.setFloated(true, true);
                f.error = false;
                f.updateStroke();
                if (cb != null) cb.on(s);
            }
        });

        if (imeAction == EditorInfo.IME_ACTION_DONE) {
            e.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent ev) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        hideKeyboard();
                        e.clearFocus();
                        return true;
                    }
                    return false;
                }
            });
        }
        return f;
    }

    // Carte cliquable avec flèche (menu déroulant ou boîte de dialogue)
    private CardFieldView dropField(String hint, String current) {
        CardFieldView f = new CardFieldView(hint);
        f.value = text(current == null ? "" : current, 15, cInput(), false);
        f.value.setSingleLine(true);
        f.value.setEllipsize(TextUtils.TruncateAt.END);
        f.value.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        f.value.setPadding(wp(5.5f), Math.round(f.fh * 0.375f), wp(13.3f), Math.round(f.fh * 0.09f));
        f.box.addView(f.value, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(
                wp(3.9f), wp(2.2f), Gravity.END | Gravity.CENTER_VERTICAL);
        ap.rightMargin = wp(5.5f);
        f.box.addView(new ArrowView(context), ap);

        f.attachLabel();
        f.setFloated(current != null && !current.isEmpty(), false);
        f.box.setClickable(true);
        return f;
    }

    // Petit sélecteur « HTG ▾ » ou « Non ▾ » posé à droite d'une carte
    private TextView addSelector(CardFieldView f, String initial, final Runnable onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(wp(2.2f), wp(2.2f), wp(2.2f), wp(2.2f));
        TextView t = text(initial, 15, cInput(), false);
        row.addView(t);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(wp(3.9f), wp(2.2f));
        ap.leftMargin = wp(2.2f);
        row.addView(new ArrowView(context), ap);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.CENTER_VERTICAL);
        lp.rightMargin = wp(3.3f);
        row.setClickable(true);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClick.run();
            }
        });
        f.box.addView(row, lp);
        return t;
    }

    // Menu déroulant (même style que le spinner de catégories)
    private void showListPopup(View anchor, int widthPx, int hOffset, final List<String> items,
                               final String selected, final PickCb cb, final Runnable onDismiss) {
        if (items.isEmpty()) {
            OnError("Aucun choix disponible dans le menu.");
            return;
        }
        hideKeyboard();
        clearAllFocus();

        final ListPopupWindow pop = new ListPopupWindow(context);
        pop.setAnchorView(anchor);
        pop.setModal(true);
        pop.setWidth(widthPx);
        pop.setHeight(items.size() <= 5 ? ViewGroup.LayoutParams.WRAP_CONTENT : hp(6.2f) * 5 + hp(1f));
        pop.setHorizontalOffset(hOffset);
        pop.setVerticalOffset(hp(0.5f));

        GradientDrawable pbg = new GradientDrawable();
        pbg.setColor(Color.WHITE);
        pbg.setCornerRadius(wp(4.4f));
        pbg.setStroke(dp(1), cStroke());
        pop.setBackgroundDrawable(pbg);

        pop.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return items.size();
            }

            @Override
            public Object getItem(int position) {
                return items.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv;
                if (convertView instanceof TextView) {
                    tv = (TextView) convertView;
                } else {
                    tv = text("", 15, cInput(), false);
                    tv.setGravity(Gravity.CENTER_VERTICAL);
                    tv.setPadding(wp(5.5f), 0, wp(5.5f), 0);
                    tv.setSingleLine(true);
                    tv.setEllipsize(TextUtils.TruncateAt.END);
                    tv.setLayoutParams(new AbsListView.LayoutParams(
                            AbsListView.LayoutParams.MATCH_PARENT, hp(6.2f)));
                }
                String item = items.get(position);
                tv.setText(item);
                tv.setBackgroundColor(item.equals(selected)
                        ? Color.parseColor("#F2F2F2") : Color.TRANSPARENT);
                return tv;
            }
        });

        pop.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cb.on(items.get(position));
                pop.dismiss();
            }
        });

        pop.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (onDismiss != null) onDismiss.run();
            }
        });
        pop.show();
    }

    private void rerenderAll() {
        if (editContainer != null) renderEdit();
        if (priceContainer != null) renderPrice();
        if (variantContainer != null) renderVariants();
    }

    @SimpleFunction(description = "Ouvre la galerie pour choisir le nouveau logo de la page Modifier la boutique (demande la permission si besoin). Appelée automatiquement au toucher de la tuile logo. OnShopLogoPicked se déclenche quand le logo est prêt.")
    public void OpenShopEditLogoPicker() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? "android.permission.READ_MEDIA_IMAGES"
                : "android.permission.READ_EXTERNAL_STORAGE";

        form.askPermission(permission, new PermissionResultHandler() {
            @Override
            public void HandlePermissionResponse(String permissionName, boolean granted) {
                if (!granted) {
                    OnError("Permission refusée.");
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent i = new Intent(Intent.ACTION_PICK);
                            i.setType("image/*");
                            form.startActivityForResult(i, editLogoRequestCode);
                        } catch (Exception e) {
                            OnError("OpenShopEditLogoPicker: " + e.getMessage());
                        }
                    }
                });
            }
        });
    }

    // =========================================================================
    // PAGE 1 : MODIFIER LE PROFIL DE LA BOUTIQUE
    // =========================================================================

    @SimpleFunction(description = "Construit la page « Modifier le profil de la boutique » dans l'arrangement donné (arrangement vertical dédié : son contenu est remplacé). À brancher sur OnEditShopClick. Préremplis les champs avec SetShopEditValues.")
    public void BuildShopEditForm(final AndroidViewComponent container) {
        if (container == null || container.getView() == null) {
            OnError("BuildShopEditForm: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                ViewGroup target = realLayout(container);
                if (target == null) {
                    OnError("BuildShopEditForm: conteneur invalide.");
                    return;
                }
                editContainer = target;
                renderEdit();
            }
        });
    }

    @SimpleFunction(description = "Préremplit la page avec les informations actuelles de la boutique (par exemple celles reçues par OnShopInfoLoaded). logo : adresse http(s) du logo actuel ou chemin local ; il n'est renvoyé au serveur que si l'utilisateur en choisit un nouveau. Le nom donné ici n'est pas considéré comme « déjà utilisé ».")
    public void SetShopEditValues(String name, String category, String phone, String address, String logo) {
        eShopName = name == null ? "" : name.trim();
        eShopCategory = category == null ? "" : category.trim();
        eShopPhone = phone == null ? "" : phone.trim();
        eShopAddress = address == null ? "" : address.trim();
        logoCurrent = logo == null ? "" : logo.trim();
        eLogoPath = "";
        logoChanged = false;
        origName = eShopName;
        origCategory = eShopCategory;
        origPhone = eShopPhone;
        origAddress = eShopAddress;
        eNameState = 0;
        eNameChecking = false;
        eNameRequiredShown = false;
        eNameSeq++;
        if (editContainer != null) renderEdit();
    }

    private void renderEdit() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    if (editContainer == null) return;
                    activeContainer = editContainer;
                    editContainer.removeAllViews();

                    TextView intro = text("Ces informations seront visibles par vos clients sur votre profil",
                            15, cTitle(), false);
                    intro.setPadding(wp(5.5f), hp(2f), wp(5.5f), 0);
                    editContainer.addView(intro);

                    // nom
                    LinearLayout s1 = cardSection();
                    s1.addView(blockTitle("Nom de la boutique"));
                    eNameField = editField("Nom de la boutique",
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                            MAX_NAME, EditorInfo.IME_ACTION_NEXT, eShopName, new TextCb() {
                                @Override
                                public void on(String v) {
                                    eShopName = v;
                                    eNameRequiredShown = false;
                                    scheduleEditNameCheck();
                                }
                            }, 5.5f);
                    s1.addView(eNameField.box);
                    eNameStatus = errorText();
                    s1.addView(eNameStatus);
                    showEditNameStatus();
                    editContainer.addView(s1);

                    // catégorie
                    LinearLayout s2 = cardSection();
                    eCatField = dropField("Catégorie", eShopCategory);
                    eCatField.box.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openEditCategoryPopup();
                        }
                    });
                    eCatValue = eCatField.value;
                    s2.addView(eCatField.box);
                    eCatError = errorText();
                    s2.addView(eCatError);
                    editContainer.addView(s2);

                    // coordonnées
                    LinearLayout s3 = cardSection();
                    s3.addView(blockTitle("Coordonnées de la boutique"));
                    ePhoneField = editField("Téléphone de la boutique", InputType.TYPE_CLASS_PHONE,
                            MAX_PHONE, EditorInfo.IME_ACTION_NEXT, eShopPhone, new TextCb() {
                                @Override
                                public void on(String v) {
                                    eShopPhone = v;
                                }
                            }, 5.5f);
                    s3.addView(ePhoneField.box);
                    eAddressField = editField("Adresse complète",
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                            MAX_ADDRESS, EditorInfo.IME_ACTION_DONE, eShopAddress, new TextCb() {
                                @Override
                                public void on(String v) {
                                    eShopAddress = v;
                                }
                            }, 5.5f);
                    LinearLayout.LayoutParams alp = (LinearLayout.LayoutParams) eAddressField.box.getLayoutParams();
                    alp.topMargin = hp(2.3f);
                    eAddressField.box.setLayoutParams(alp);
                    s3.addView(eAddressField.box);
                    editContainer.addView(s3);

                    addSeparatorTo(editContainer);

                    // média
                    LinearLayout s4 = cardSection();
                    s4.addView(blockTitle("Média"));
                    addLogoCard(s4);
                    editContainer.addView(s4);

                    addSeparatorTo(editContainer);
                    View pad = new View(context);
                    pad.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, hp(3f)));
                    editContainer.addView(pad);
                } catch (Exception e) {
                    OnError("renderEdit: " + e.getMessage());
                }
            }
        });
    }

    private void openEditCategoryPopup() {
        if (categories.isEmpty()) {
            OnError("Aucune catégorie : appelle SetShopCategories avant d'ouvrir le menu.");
            return;
        }
        eCatField.focused = true;
        eCatField.updateStroke();
        eCatField.setFloated(true, true);
        showListPopup(eCatField.box, eCatField.box.getWidth(), 0, categories, eShopCategory,
                new PickCb() {
                    @Override
                    public void on(String v) {
                        selectEditCategory(v, true);
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        eCatField.focused = false;
                        eCatField.updateStroke();
                        if (eShopCategory.isEmpty()) eCatField.setFloated(false, true);
                    }
                });
    }

    private void selectEditCategory(String c, boolean fire) {
        eShopCategory = c == null ? "" : c;
        if (eCatValue != null) eCatValue.setText(eShopCategory);
        if (eCatField != null) {
            eCatField.error = false;
            eCatField.updateStroke();
            eCatField.setFloated(!eShopCategory.isEmpty(), true);
        }
        if (eCatError != null) eCatError.setVisibility(View.GONE);
        if (fire) AfterCategorySelected(eShopCategory);
    }

    // ---- logo (même carte inclinée que la création) ----

    private void addLogoCard(LinearLayout sec) {
        sec.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout outer = new FrameLayout(context);
        final int outerSide = wp(44f);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(outerSide, outerSide);
        op.gravity = Gravity.CENTER_HORIZONTAL;
        op.topMargin = hp(1f);
        op.bottomMargin = hp(2.5f);
        outer.setLayoutParams(op);
        GradientDrawable obg = new GradientDrawable();
        obg.setColor(Color.WHITE);
        obg.setCornerRadius(Math.round(outerSide * 0.19f));
        outer.setBackground(obg);
        outer.setElevation((float) dp(6));
        outer.setRotation(-4f);

        final int innerSide = wp(37.2f);
        final int innerRadius = Math.round(innerSide * 0.19f);
        FrameLayout inner = new FrameLayout(context);
        GradientDrawable ibg = new GradientDrawable();
        ibg.setColor(Color.parseColor("#E8E8E8"));
        ibg.setCornerRadius(innerRadius);
        inner.setBackground(ibg);
        inner.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), innerRadius);
            }
        });
        inner.setClipToOutline(true);
        outer.addView(inner, new FrameLayout.LayoutParams(innerSide, innerSide, Gravity.CENTER));

        eLogoGlyph = new PhotoGlyph(context);
        inner.addView(eLogoGlyph, new FrameLayout.LayoutParams(wp(12.2f), wp(12.2f), Gravity.CENTER));

        eLogoImage = new ImageView(context);
        eLogoImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        eLogoImage.setVisibility(View.GONE);
        inner.addView(eLogoImage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        outer.setClickable(true);
        outer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenShopEditLogoPicker();
            }
        });

        sec.addView(outer);
        updateEditLogoView();
    }

    private void updateEditLogoView() {
        if (eLogoImage == null || eLogoGlyph == null) return;
        if (!eLogoPath.isEmpty()) {
            Bitmap b = decodeSampled(stripFile(eLogoPath), 400);
            if (b != null) {
                eLogoImage.setImageBitmap(b);
                eLogoImage.setVisibility(View.VISIBLE);
                eLogoGlyph.setVisibility(View.GONE);
                return;
            }
        }
        if (!logoCurrent.isEmpty()) {
            loadCurrentLogo();
            return;
        }
        eLogoImage.setImageDrawable(null);
        eLogoImage.setVisibility(View.GONE);
        eLogoGlyph.setVisibility(View.VISIBLE);
    }

    // Logo actuel de la boutique (adresse http(s) ou fichier local)
    private void loadCurrentLogo() {
        final String src = logoCurrent;
        final int token = ++eLogoToken;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap b = null;
                HttpURLConnection c = null;
                try {
                    if (src.startsWith("http://") || src.startsWith("https://")) {
                        c = (HttpURLConnection) new URL(src).openConnection();
                        c.setConnectTimeout(10000);
                        c.setReadTimeout(15000);
                        InputStream is = c.getInputStream();
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) > 0) {
                            bos.write(buf, 0, n);
                        }
                        is.close();
                        byte[] bytes = bos.toByteArray();
                        BitmapFactory.Options o = new BitmapFactory.Options();
                        o.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
                        int sample = 1;
                        while (o.outWidth / (sample * 2) >= 400) sample *= 2;
                        o.inJustDecodeBounds = false;
                        o.inSampleSize = sample;
                        b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
                    } else {
                        b = decodeSampled(stripFile(src), 400);
                    }
                } catch (Exception ignored) {
                } finally {
                    if (c != null) c.disconnect();
                }
                final Bitmap bmp = b;
                if (bmp == null) return;
                runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        if (token != eLogoToken || eLogoImage == null || eLogoGlyph == null) return;
                        if (!eLogoPath.isEmpty()) return;
                        eLogoImage.setImageBitmap(bmp);
                        eLogoImage.setVisibility(View.VISIBLE);
                        eLogoGlyph.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    // Appelée par resultReturned (aiguillage par code de retour)
    private void shopEditLogoResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != editLogoRequestCode) return;
        if (resultCode != Activity.RESULT_OK || data == null) return;
        final Uri uri = data.getData();
        if (uri == null) {
            OnError("Aucune image sélectionnée (URI nulle).");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String result = processLogo(uri);
                if (result == null) return;
                runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        eLogoPath = result;
                        logoChanged = true;
                        updateEditLogoView();
                        OnShopLogoPicked(result);
                    }
                });
            }
        }).start();
    }

    // ---- vérification du nom en direct ----

    private boolean nameUnchanged() {
        String n = eShopName == null ? "" : eShopName.trim();
        return !origName.isEmpty() && n.equalsIgnoreCase(origName);
    }

    private void scheduleEditNameCheck() {
        if (eNameRunnable != null) handler.removeCallbacks(eNameRunnable);
        eNameSeq++;
        eNameState = 0;
        eNameChecking = false;
        showEditNameStatus();

        final String n = eShopName == null ? "" : eShopName.trim();
        if (n.isEmpty() || nameCheckUrl.isEmpty() || nameUnchanged()) return;

        eNameChecking = true;
        final int seq = eNameSeq;
        eNameRunnable = new Runnable() {
            @Override
            public void run() {
                runEditNameCheck(n, seq);
            }
        };
        handler.postDelayed(eNameRunnable, NAME_DEBOUNCE_MS);
    }

    private String buildEditNameUrl(String name) throws Exception {
        String enc = URLEncoder.encode(name, "UTF-8");
        String uidEnc = URLEncoder.encode(currentUserUid() == null ? "" : currentUserUid(), "UTF-8");
        String u = nameCheckUrl.replace("{uid}", uidEnc);
        String r;
        if (u.contains("{name}")) {
            r = u.replace("{name}", enc);
        } else {
            r = u + (u.contains("?") ? "&" : "?") + "name=" + enc;
        }
        if (!currentUserUid().isEmpty() && !nameCheckUrl.contains("{uid}")) {
            r += (r.contains("?") ? "&" : "?") + "uid=" + uidEnc;
        }
        return r;
    }

    private void runEditNameCheck(final String name, final int seq) {
        final String auth = nameCheckAuth;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] r = httpGet(buildEditNameUrl(name), auth);
                    int code = Integer.parseInt(r[0]);
                    if (code >= 400) throw new Exception("serveur " + code);
                    Boolean avail = nameAvailableFromJson(new JSONObject(r[1].trim()));
                    if (avail == null) throw new Exception("réponse inattendue (available / exists manquant)");
                    final boolean available = avail.booleanValue();
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            if (seq != eNameSeq) return;
                            eNameChecking = false;
                            eNameState = available ? 1 : 2;
                            showEditNameStatus();
                            OnShopNameChecked(name, available);
                        }
                    });
                } catch (final Exception e) {
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            if (seq != eNameSeq) return;
                            eNameChecking = false;
                            eNameState = 3;
                            showEditNameStatus();
                            OnError("Vérification du nom: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void showEditNameStatus() {
        if (eNameStatus == null) return;
        String msg = "";
        int color = COLOR_ERROR;
        if (eNameRequiredShown) {
            msg = "Le nom de la boutique est obligatoire";
        } else if (nameUnchanged()) {
            msg = "";
        } else if (eNameState == 2) {
            msg = "Ce nom est déjà utilisé";
        } else if (eNameState == 1 && eShopName != null && !eShopName.trim().isEmpty()) {
            msg = "Ce nom est disponible";
            color = COLOR_OK;
        }
        eNameStatus.setTextColor(color);
        eNameStatus.setText(msg);
        eNameStatus.setVisibility(msg.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ---- lecture et validation (sécurité : nom et catégorie) ----

    @SimpleFunction(description = "Retourne le nom de la boutique saisi.")
    public String GetShopEditName() {
        return eShopName == null ? "" : eShopName.trim();
    }

    @SimpleFunction(description = "Retourne la catégorie choisie (vide si aucune).")
    public String GetShopEditCategory() {
        return eShopCategory == null ? "" : eShopCategory;
    }

    @SimpleFunction(description = "Retourne le téléphone de la boutique saisi.")
    public String GetShopEditPhone() {
        return eShopPhone == null ? "" : eShopPhone.trim();
    }

    @SimpleFunction(description = "Retourne l'adresse de la boutique saisie.")
    public String GetShopEditAddress() {
        return eShopAddress == null ? "" : eShopAddress.trim();
    }

    @SimpleFunction(description = "Retourne le lien file:// du NOUVEAU logo choisi (vide si le logo n'a pas changé).")
    public String GetShopEditLogo() {
        return eLogoPath == null ? "" : eLogoPath;
    }

    @SimpleFunction(description = "Retourne vrai si au moins un champ (nom, catégorie, téléphone, adresse, logo) a été modifié depuis SetShopEditValues.")
    public boolean IsShopEditChanged() {
        return !GetShopEditName().equals(origName)
                || !GetShopEditCategory().equals(origCategory)
                || !GetShopEditPhone().equals(origPhone)
                || !GetShopEditAddress().equals(origAddress)
                || logoChanged;
    }

    @SimpleFunction(description = "Retourne toute la page en JSON : name, category, phone, address, logo (nouveau logo seulement), uid, changed, formValid.")
    public String GetShopEditJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("name", GetShopEditName());
            o.put("category", GetShopEditCategory());
            o.put("phone", GetShopEditPhone());
            o.put("address", GetShopEditAddress());
            o.put("logo", GetShopEditLogo());
            o.put("uid", currentUserUid());
            o.put("changed", IsShopEditChanged());
            o.put("formValid", firstEditError() == null);
            return o.toString();
        } catch (Exception e) {
            OnError("GetShopEditJson: " + e.getMessage());
            return "{}";
        }
    }

    private String[] firstEditError() {
        if (eShopName == null || eShopName.trim().isEmpty()) {
            return new String[]{"name", "Le nom de la boutique est obligatoire"};
        }
        if (!nameUnchanged() && eNameState == 2) {
            return new String[]{"name", "Ce nom est déjà utilisé"};
        }
        if (!nameUnchanged() && eNameChecking) {
            return new String[]{"name", "Vérification du nom en cours, patiente un instant"};
        }
        if (eShopCategory == null || eShopCategory.trim().isEmpty()) {
            return new String[]{"category", "Choisis une catégorie"};
        }
        return null;
    }

    @SimpleFunction(description = "Vérifie la page. Retourne vrai si le nom (obligatoire, pas déjà utilisé) et la catégorie (obligatoire) sont valides. Sinon retourne faux, affiche l'erreur en rouge sous le champ et déclenche OnShopFormInvalid. À mettre directement dans un « si » sur ton bouton ✓.")
    public boolean ValidateShopEdit() {
        final String[] err = firstEditError();
        if (err == null) return true;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if ("name".equals(err[0])) {
                    if (eNameField != null) {
                        eNameField.error = true;
                        eNameField.updateStroke();
                    }
                    if (eShopName == null || eShopName.trim().isEmpty()) {
                        eNameRequiredShown = true;
                    }
                    showEditNameStatus();
                } else if ("category".equals(err[0])) {
                    if (eCatField != null) {
                        eCatField.error = true;
                        eCatField.updateStroke();
                    }
                    if (eCatError != null) {
                        eCatError.setText(err[1]);
                        eCatError.setVisibility(View.VISIBLE);
                    }
                }
                OnShopFormInvalid(err[0], err[1]);
            }
        });
        return false;
    }

    @SimpleFunction(description = "Retourne le premier message d'erreur de la page, ou \"\" si tout est valide (sans rien afficher).")
    public String GetShopEditError() {
        String[] err = firstEditError();
        return err == null ? "" : err[1];
    }

    // ---- envoi au serveur (multipart/form-data) ----

    @SimpleFunction(description = "Envoie les modifications de la boutique au serveur (POST multipart). Champs : action=update_shop, uid, name, category, phone, address, formValid=true, et le fichier logo seulement si un nouveau logo a été choisi. authorization : en-tête Authorization (ex : Bearer <jeton Firebase>). uid : l'UID Firebase de l'utilisateur. Le serveur doit vérifier le jeton et utiliser l'uid du jeton, jamais celui envoyé. La page est vérifiée avant l'envoi. Réponse : OnShopUpdated ou OnShopUpdateFailed (409 = nom déjà pris).")
    public void SubmitShopEdit(final String url, final String authorization, final String uid) {
        if (url == null || url.trim().isEmpty()) {
            OnError("SubmitShopEdit: adresse du serveur vide.");
            return;
        }
        if (uid == null || uid.trim().isEmpty()) {
            OnError("SubmitShopEdit: uid vide.");
            return;
        }
        if (eSubmitting) return;
        if (!ValidateShopEdit()) return;

        eSubmitting = true;
        final String cleanUid = uid.trim();
        prefs().edit().putString(PREF_CURRENT_UID, cleanUid).apply();

        final Map<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("action", "update_shop");
        fields.put("uid", cleanUid);
        fields.put("name", GetShopEditName());
        fields.put("category", GetShopEditCategory());
        fields.put("phone", GetShopEditPhone());
        fields.put("address", GetShopEditAddress());
        fields.put("formValid", "true");
        final String logo = logoChanged ? stripFile(GetShopEditLogo()) : "";
        final String target = url.trim();
        final String auth = authorization == null ? "" : authorization.trim();

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection c = null;
                try {
                    String boundary = "----Manatest" + System.currentTimeMillis();
                    c = (HttpURLConnection) new URL(target).openConnection();
                    c.setRequestMethod("POST");
                    c.setDoOutput(true);
                    c.setConnectTimeout(20000);
                    c.setReadTimeout(60000);
                    c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    if (!auth.isEmpty()) {
                        c.setRequestProperty("Authorization", auth);
                    }
                    DataOutputStream out = new DataOutputStream(c.getOutputStream());
                    for (Map.Entry<String, String> e : fields.entrySet()) {
                        mpField(out, boundary, e.getKey(), e.getValue());
                    }
                    if (logo != null && !logo.isEmpty()) {
                        File f = new File(logo);
                        if (f.exists()) {
                            out.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
                            out.write("Content-Disposition: form-data; name=\"logo\"; filename=\"logo.jpg\"\r\n".getBytes("UTF-8"));
                            out.write("Content-Type: image/jpeg\r\n\r\n".getBytes("UTF-8"));
                            FileInputStream in = new FileInputStream(f);
                            try {
                                byte[] buf = new byte[8192];
                                int n;
                                while ((n = in.read(buf)) > 0) {
                                    out.write(buf, 0, n);
                                }
                            } finally {
                                try { in.close(); } catch (Exception ignored) { }
                            }
                            out.write("\r\n".getBytes("UTF-8"));
                        }
                    }
                    out.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
                    out.flush();
                    out.close();

                    final int code = c.getResponseCode();
                    InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
                    final String body = readAll(is);
                    final boolean ok = code >= 200 && code < 300 && responseIsSuccess(body);
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            eSubmitting = false;
                            if (ok) {
                                origName = GetShopEditName();
                                origCategory = GetShopEditCategory();
                                origPhone = GetShopEditPhone();
                                origAddress = GetShopEditAddress();
                                if (logoChanged) {
                                    logoCurrent = eLogoPath;
                                    logoChanged = false;
                                }
                                eNameState = 0;
                                showEditNameStatus();
                                OnShopUpdated(code, body);
                            } else {
                                if (code == 409) {
                                    eNameState = 2;
                                    showEditNameStatus();
                                }
                                OnShopUpdateFailed(code, body);
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            eSubmitting = false;
                            OnError("SubmitShopEdit: " + e.getMessage());
                        }
                    });
                } finally {
                    if (c != null) c.disconnect();
                }
            }
        }).start();
    }

    @SimpleFunction(description = "Vide tous les champs de la page Modifier la boutique.")
    public void ClearShopEditForm() {
        eShopName = "";
        eShopCategory = "";
        eShopPhone = "";
        eShopAddress = "";
        eLogoPath = "";
        logoCurrent = "";
        logoChanged = false;
        origName = "";
        origCategory = "";
        origPhone = "";
        origAddress = "";
        eNameState = 0;
        eNameChecking = false;
        eNameRequiredShown = false;
        eNameSeq++;
        if (editContainer != null) renderEdit();
    }

    // =========================================================================
    // PAGE 2 : PRIX
    // =========================================================================

    @SimpleFunction(description = "Construit la page « Prix » dans l'arrangement donné (arrangement vertical dédié : son contenu est remplacé) : prix du produit avec menu HTG / USD, question des frais de livraison avec menu Non / Oui (carte des frais grisée tant que c'est Non), et carte Montant total toujours au même endroit. Branche-la sur OnProductFormFieldClick (id = price).")
    public void BuildPriceForm(final AndroidViewComponent container) {
        if (container == null || container.getView() == null) {
            OnError("BuildPriceForm: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                ViewGroup target = realLayout(container);
                if (target == null) {
                    OnError("BuildPriceForm: conteneur invalide.");
                    return;
                }
                priceContainer = target;
                renderPrice();
            }
        });
    }

    @SimpleFunction(description = "Préremplit la page Prix. basePrice : le prix du produit seul (sans frais). currency : HTG ou USD. deliveryChoice : Oui / Non (ou true / false). deliveryFee : les frais de livraison (ignorés si Non).")
    public void SetPriceValues(String basePrice, String currency, String deliveryChoice, String deliveryFee) {
        BigDecimal p = parseMoney(basePrice, false);
        priceText = p == null ? "" : plainAmount(p);
        priceCurrency = "USD".equalsIgnoreCase(currency == null ? "" : currency.trim())
                || "$".equals(currency == null ? "" : currency.trim()) ? "USD" : "HTG";
        deliveryOn = isYes(deliveryChoice);
        BigDecimal f = deliveryOn ? parseMoney(deliveryFee, true) : null;
        feeText = f == null ? "" : plainAmount(f);
        if (priceContainer != null) renderPrice();
    }

    @SimpleFunction(description = "Vide la page Prix (HTG, Non, aucun montant).")
    public void ClearPriceForm() {
        priceText = "";
        feeText = "";
        priceCurrency = "HTG";
        deliveryOn = false;
        if (priceContainer != null) renderPrice();
    }

    private boolean isYes(String s) {
        String t = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
        return t.equals("oui") || t.equals("true") || t.equals("yes") || t.equals("1");
    }

    private String curLabel() {
        return "USD".equals(priceCurrency) ? "$" : "HTG";
    }

    // « 5000 » ou « 5 000,5 » : 2 décimales maximum ; null si invalide (ou <= 0 sans allowZero)
    private BigDecimal parseMoney(String text, boolean allowZero) {
        if (text == null) return null;
        String t = text.trim().replace(" ", "").replace("\u00A0", "").replace(",", ".");
        if (!t.matches("\\d{1,12}(\\.\\d{1,2})?")) return null;
        BigDecimal bd = new BigDecimal(t);
        if (!allowZero && bd.signum() <= 0) return null;
        return bd;
    }

    private String plainAmount(BigDecimal bd) {
        return bd.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatAmount(BigDecimal bd) {
        String s = String.format(Locale.US, "%,.2f", bd);
        String r = s.replace(",", " ").replace(".", ",");
        if (r.endsWith(",00")) r = r.substring(0, r.length() - 3);
        return r;
    }

    private BigDecimal totalValue() {
        BigDecimal p = parseMoney(priceText, false);
        BigDecimal f = deliveryOn ? parseMoney(feeText, true) : null;
        BigDecimal t = p == null ? BigDecimal.ZERO : p;
        if (f != null) t = t.add(f);
        return t;
    }

    private void updateTotal() {
        if (totalValueTv != null) totalValueTv.setText(formatAmount(totalValue()));
        if (totalCurTv != null) totalCurTv.setText(curLabel());
    }

    private void renderPrice() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    if (priceContainer == null) return;
                    activeContainer = priceContainer;
                    priceContainer.removeAllViews();
                    int numberType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;

                    // prix du produit + menu de devise
                    LinearLayout s1 = cardSection();
                    priceField = editField("Prix du produit", numberType, 15,
                            EditorInfo.IME_ACTION_NEXT, priceText, new TextCb() {
                                @Override
                                public void on(String v) {
                                    priceText = v;
                                    if (priceErr != null) priceErr.setVisibility(View.GONE);
                                    updateTotal();
                                }
                            }, 30f);
                    priceCurTv = addSelector(priceField, curLabel(), new Runnable() {
                        @Override
                        public void run() {
                            openCurrencyPopup();
                        }
                    });
                    s1.addView(priceField.box);
                    priceErr = errorText();
                    s1.addView(priceErr);
                    priceContainer.addView(s1);

                    // frais de livraison + menu Non / Oui
                    LinearLayout s2 = cardSection();
                    s2.addView(blockTitle("Voulez-vous ajouter les frais de livraison ?"));
                    feeField = editField("Frais de livraison", numberType, 15,
                            EditorInfo.IME_ACTION_DONE, feeText, new TextCb() {
                                @Override
                                public void on(String v) {
                                    feeText = v;
                                    if (feeErr != null) feeErr.setVisibility(View.GONE);
                                    updateTotal();
                                }
                            }, 30f);
                    deliveryTv = addSelector(feeField, deliveryOn ? "Oui" : "Non", new Runnable() {
                        @Override
                        public void run() {
                            openDeliveryPopup();
                        }
                    });
                    feeField.setDisabled(!deliveryOn);
                    s2.addView(feeField.box);
                    feeErr = errorText();
                    s2.addView(feeErr);
                    priceContainer.addView(s2);

                    // montant total (toujours au même endroit)
                    LinearLayout s3 = cardSection();
                    FrameLayout card = new FrameLayout(context);
                    card.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, hp(9.2f)));
                    GradientDrawable cbg = new GradientDrawable();
                    cbg.setColor(Color.parseColor("#F5F5F5"));
                    cbg.setCornerRadius(wp(4.4f));
                    card.setBackground(cbg);

                    TextView lbl = text("Montant total", 12, cHint(), false);
                    FrameLayout.LayoutParams l1 = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP | Gravity.START);
                    l1.leftMargin = wp(5.5f);
                    l1.topMargin = hp(1.3f);
                    card.addView(lbl, l1);

                    totalValueTv = text("0", 25, cInput(), false);
                    FrameLayout.LayoutParams l2 = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.BOTTOM | Gravity.START);
                    l2.leftMargin = wp(5.5f);
                    l2.bottomMargin = hp(1f);
                    card.addView(totalValueTv, l2);

                    totalCurTv = text(curLabel(), 20, cInput(), false);
                    FrameLayout.LayoutParams l3 = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.BOTTOM | Gravity.END);
                    l3.rightMargin = wp(5.5f);
                    l3.bottomMargin = hp(1f);
                    card.addView(totalCurTv, l3);

                    s3.addView(card);
                    s3.addView(cardDesc("Seul le montant total sera affiché au client"));
                    priceContainer.addView(s3);

                    addSeparatorTo(priceContainer);
                    updateTotal();
                } catch (Exception e) {
                    OnError("renderPrice: " + e.getMessage());
                }
            }
        });
    }

    private void openCurrencyPopup() {
        final List<String> items = new ArrayList<String>();
        items.add("HTG");
        items.add("USD");
        int w = wp(39f);
        showListPopup(priceField.box, w, priceField.box.getWidth() - w - wp(3.3f), items, priceCurrency,
                new PickCb() {
                    @Override
                    public void on(String v) {
                        setCurrency(v, true);
                    }
                }, null);
    }

    private void setCurrency(String c, boolean fire) {
        priceCurrency = "USD".equals(c) ? "USD" : "HTG";
        if (priceCurTv != null) priceCurTv.setText(curLabel());
        updateTotal();
        if (fire) OnCurrencyChanged(priceCurrency);
    }

    private void openDeliveryPopup() {
        final List<String> items = new ArrayList<String>();
        items.add("Non");
        items.add("Oui");
        int w = wp(39f);
        showListPopup(feeField.box, w, feeField.box.getWidth() - w - wp(3.3f), items,
                deliveryOn ? "Oui" : "Non",
                new PickCb() {
                    @Override
                    public void on(String v) {
                        setDeliveryOn("Oui".equals(v), true);
                    }
                }, null);
    }

    private void setDeliveryOn(boolean on, boolean fire) {
        deliveryOn = on;
        if (deliveryTv != null) deliveryTv.setText(on ? "Oui" : "Non");
        if (!on) {
            feeText = "";
            if (feeField != null && feeField.edit != null) {
                restoring = true;
                feeField.edit.setText("");
                restoring = false;
                feeField.setFloated(false, true);
                feeField.error = false;
                feeField.updateStroke();
            }
            if (feeErr != null) feeErr.setVisibility(View.GONE);
        }
        if (feeField != null) feeField.setDisabled(!on);
        updateTotal();
        if (fire) OnDeliveryChoiceChanged(on);
    }

    // ---- lecture et validation du prix ----

    @SimpleFunction(description = "Retourne le prix du produit seul, à 2 décimales (ex : 5000.50), ou \"\" s'il est vide ou invalide.")
    public String GetPriceAmount() {
        BigDecimal p = parseMoney(priceText, false);
        return p == null ? "" : plainAmount(p);
    }

    @SimpleFunction(description = "Retourne la devise choisie : HTG ou USD.")
    public String GetPriceCurrency() {
        return priceCurrency;
    }

    @SimpleFunction(description = "Retourne le choix des frais de livraison : Oui ou Non.")
    public String GetDeliveryChoice() {
        return deliveryOn ? "Oui" : "Non";
    }

    @SimpleFunction(description = "Retourne vrai si le choix est Oui (livraison incluse), faux si Non. Le serveur le renvoie à la page détails : faux = texte « Livraison inclus » barré.")
    public boolean GetDeliveryIncluded() {
        return deliveryOn;
    }

    @SimpleFunction(description = "Retourne les frais de livraison à 2 décimales (0.00 si Non).")
    public String GetDeliveryFee() {
        BigDecimal f = deliveryOn ? parseMoney(feeText, true) : null;
        return plainAmount(f == null ? BigDecimal.ZERO : f);
    }

    @SimpleFunction(description = "Retourne le montant total (prix + frais si Oui) à 2 décimales. C'est le prix à envoyer au serveur et affiché au client.")
    public String GetPriceTotal() {
        return plainAmount(totalValue());
    }

    @SimpleFunction(description = "Retourne le montant total prêt à afficher : « 5 000,50 HTG » ou « $25 ».")
    public String GetPriceTotalDisplay() {
        String a = formatAmount(totalValue());
        return "USD".equals(priceCurrency) ? "$" + a : a + " HTG";
    }

    @SimpleFunction(description = "Retourne toute la page Prix en JSON : price (total), basePrice, deliveryFee, currency, deliveryIncluded, delivery (Oui / Non), formValid.")
    public String GetPriceFormJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("price", GetPriceTotal());
            o.put("basePrice", GetPriceAmount());
            o.put("deliveryFee", GetDeliveryFee());
            o.put("currency", priceCurrency);
            o.put("deliveryIncluded", deliveryOn);
            o.put("delivery", GetDeliveryChoice());
            o.put("formValid", firstPriceError() == null);
            return o.toString();
        } catch (Exception e) {
            OnError("GetPriceFormJson: " + e.getMessage());
            return "{}";
        }
    }

    private String[] firstPriceError() {
        if (parseMoney(priceText, false) == null) {
            return new String[]{"price", "Entre un prix supérieur à 0 (2 décimales maximum)"};
        }
        if (deliveryOn && parseMoney(feeText, true) == null) {
            return new String[]{"deliveryFee", "Entre les frais de livraison (0 ou plus)"};
        }
        return null;
    }

    @SimpleFunction(description = "Vérifie la page Prix. Retourne faux (et affiche l'erreur en rouge, avec OnPriceFormInvalid) si le prix est vide ou invalide, ou si Oui est choisi sans frais valides. À mettre dans un « si » sur ton bouton ✓.")
    public boolean ValidatePriceForm() {
        final String[] err = firstPriceError();
        if (err == null) return true;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if ("price".equals(err[0])) {
                    if (priceField != null) {
                        priceField.error = true;
                        priceField.updateStroke();
                    }
                    if (priceErr != null) {
                        priceErr.setText(err[1]);
                        priceErr.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (feeField != null) {
                        feeField.error = true;
                        feeField.updateStroke();
                    }
                    if (feeErr != null) {
                        feeErr.setText(err[1]);
                        feeErr.setVisibility(View.VISIBLE);
                    }
                }
                OnPriceFormInvalid(err[0], err[1]);
            }
        });
        return false;
    }

    // =========================================================================
    // PAGE 3 : ÉTAT ET VARIANTES
    // =========================================================================

    private static final Map<String, Integer> NAMED_COLORS = new HashMap<String, Integer>();

    static {
        String[][] c = {
                {"rouge", "#D50000"}, {"red", "#D50000"},
                {"bleu", "#1565C0"}, {"blue", "#1565C0"},
                {"vert", "#2E9E4F"}, {"verte", "#2E9E4F"}, {"green", "#2E9E4F"},
                {"jaune", "#FFD600"}, {"yellow", "#FFD600"},
                {"noir", "#111111"}, {"noire", "#111111"}, {"black", "#111111"},
                {"blanc", "#FFFFFF"}, {"blanche", "#FFFFFF"}, {"white", "#FFFFFF"},
                {"gris", "#9E9E9E"}, {"grise", "#9E9E9E"}, {"gray", "#9E9E9E"}, {"grey", "#9E9E9E"},
                {"rose", "#F48FB1"}, {"pink", "#F48FB1"},
                {"orange", "#FF9800"},
                {"violet", "#7B1FA2"}, {"violette", "#7B1FA2"}, {"purple", "#7B1FA2"},
                {"marron", "#6D4C41"}, {"brown", "#6D4C41"},
                {"beige", "#D9C3A0"},
                {"turquoise", "#1ABC9C"},
                {"bordeaux", "#6D0F1F"},
                {"marine", "#14213D"}, {"navy", "#14213D"},
                {"or", "#D4AF37"}, {"dore", "#D4AF37"}, {"gold", "#D4AF37"},
                {"argent", "#C0C0C0"}, {"argente", "#C0C0C0"}, {"silver", "#C0C0C0"},
                {"kaki", "#8A8B4B"},
                {"saumon", "#FA8072"},
                {"cyan", "#00BCD4"},
                {"magenta", "#D81B60"},
                {"lavande", "#B39DDB"},
                {"creme", "#FFF3D6"}
        };
        for (int i = 0; i < c.length; i++) {
            NAMED_COLORS.put(c[i][0], Color.parseColor(c[i][1]));
        }
    }

    // Reconnaît un nom de couleur (français ou anglais) ou un code #RRGGBB ; null si inconnue
    private Integer resolveColor(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return null;
        try {
            if (s.startsWith("#") && (s.length() == 7 || s.length() == 9)) {
                return Color.parseColor(s);
            }
        } catch (Exception ignored) {
        }
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        Integer base = NAMED_COLORS.get(s);
        if (base == null) {
            String[] tokens = s.split("[\\s,/\\-]+");
            for (int i = 0; i < tokens.length && base == null; i++) {
                String t = tokens[i];
                base = NAMED_COLORS.get(t);
                if (base == null && t.endsWith("s") && t.length() > 2) {
                    base = NAMED_COLORS.get(t.substring(0, t.length() - 1));
                }
            }
        }
        if (base == null) return null;
        if (s.contains("fonc")) return blendColor(base, Color.BLACK, 0.30f);
        if (s.contains("clair") || s.contains("pastel")) return blendColor(base, Color.WHITE, 0.40f);
        return base;
    }

    private int blendColor(int from, int to, float amount) {
        int r = (int) (Color.red(from) + (Color.red(to) - Color.red(from)) * amount);
        int g = (int) (Color.green(from) + (Color.green(to) - Color.green(from)) * amount);
        int b = (int) (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount);
        return Color.rgb(r, g, b);
    }

    @SimpleFunction(description = "Construit la page « État et variantes » dans l'arrangement donné (arrangement vertical dédié : son contenu est remplacé) : carte État qui ouvre une boîte de dialogue, puis Couleur (avec une pastille de la couleur reconnue) et Taille. Branche-la sur OnProductFormFieldClick (id = variants, condition, color ou size).")
    public void BuildVariantsForm(final AndroidViewComponent container) {
        if (container == null || container.getView() == null) {
            OnError("BuildVariantsForm: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                ViewGroup target = realLayout(container);
                if (target == null) {
                    OnError("BuildVariantsForm: conteneur invalide.");
                    return;
                }
                variantContainer = target;
                renderVariants();
            }
        });
    }

    @SimpleFunction(description = "Préremplit la page État et variantes. Les valeurs vides sont acceptées.")
    public void SetVariantValues(String condition, String color, String size) {
        this.condition = condition == null ? "" : condition.trim();
        this.color = color == null ? "" : color.trim();
        this.size = size == null ? "" : size.trim();
        if (variantContainer != null) renderVariants();
    }

    @SimpleFunction(description = "Remplace la liste des états de la boîte de dialogue : un tableau JSON ou des noms séparés par des virgules. Par défaut : Neuf, Bon état, Comme neuf, Assez bon état.")
    public void SetConditionOptions(String optionsList) {
        if (optionsList == null || optionsList.trim().isEmpty()) return;
        List<String> list = new ArrayList<String>();
        String t = optionsList.trim();
        try {
            if (t.startsWith("[")) {
                JSONArray a = new JSONArray(t);
                for (int i = 0; i < a.length(); i++) {
                    String s = String.valueOf(a.get(i)).trim();
                    if (!s.isEmpty()) list.add(s);
                }
            } else {
                String[] parts = t.split(",");
                for (int i = 0; i < parts.length; i++) {
                    String s = parts[i].trim();
                    if (!s.isEmpty()) list.add(s);
                }
            }
        } catch (Exception e) {
            OnError("SetConditionOptions: " + e.getMessage());
            return;
        }
        if (list.isEmpty()) return;
        conditions.clear();
        conditions.addAll(list);
    }

    @SimpleFunction(description = "Vide la page État et variantes.")
    public void ClearVariantsForm() {
        condition = "";
        color = "";
        size = "";
        if (variantContainer != null) renderVariants();
    }

    private void renderVariants() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    if (variantContainer == null) return;
                    activeContainer = variantContainer;
                    variantContainer.removeAllViews();

                    // état : carte qui ouvre la boîte de dialogue
                    LinearLayout s1 = cardSection();
                    s1.setPadding(wp(5.5f), hp(3f), wp(5.5f), hp(3f));
                    condField = dropField("État", condition);
                    condField.box.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openConditionDialog();
                        }
                    });
                    s1.addView(condField.box);
                    variantContainer.addView(s1);

                    addSeparatorTo(variantContainer);

                    // variantes
                    TextView title = text("Variantes", 15, ink(0xA6), true);
                    title.setPadding(wp(5.5f), hp(2f), wp(5.5f), 0);
                    variantContainer.addView(title);

                    LinearLayout s2 = cardSection();
                    colorField = editField("Couleur",
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                            60, EditorInfo.IME_ACTION_NEXT, color, new TextCb() {
                                @Override
                                public void on(String v) {
                                    color = v;
                                    updateSwatch();
                                }
                            }, 5.5f);
                    s2.addView(colorField.box);

                    // pastilles des couleurs reconnues (une par couleur, séparées par une virgule)
                    swatchRow = new LinearLayout(context);
                    swatchRow.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    sp.leftMargin = wp(4.4f);
                    sp.topMargin = hp(1.3f);
                    swatchRow.setLayoutParams(sp);
                    swatchRow.setVisibility(View.GONE);
                    s2.addView(swatchRow);

                    sizeField = editField("Taille",
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                            30, EditorInfo.IME_ACTION_DONE, size, new TextCb() {
                                @Override
                                public void on(String v) {
                                    size = v;
                                }
                            }, 5.5f);
                    LinearLayout.LayoutParams zlp = (LinearLayout.LayoutParams) sizeField.box.getLayoutParams();
                    zlp.topMargin = hp(2f);
                    sizeField.box.setLayoutParams(zlp);
                    s2.addView(sizeField.box);
                    variantContainer.addView(s2);

                    updateSwatch();
                } catch (Exception e) {
                    OnError("renderVariants: " + e.getMessage());
                }
            }
        });
    }

    // « Vert, Bleu , rouge » ou « rouge et noir » : une couleur par élément reconnu
    private List<Integer> resolveColors(String raw) {
        List<Integer> out = new ArrayList<Integer>();
        if (raw == null) return out;
        String[] parts = raw.toLowerCase(Locale.ROOT).split("\\s*(?:,|;|/|\\+|&|\\bet\\b|\\band\\b)\\s*");
        for (int i = 0; i < parts.length && out.size() < 8; i++) {
            String p = parts[i].trim();
            if (p.isEmpty()) continue;
            Integer c = resolveColor(p);
            if (c != null && !out.contains(c)) out.add(c);
        }
        return out;
    }

    private void updateSwatch() {
        if (swatchRow == null) return;
        swatchRow.removeAllViews();
        List<Integer> cols = resolveColors(color);
        if (cols.isEmpty()) {
            swatchRow.setVisibility(View.GONE);
            return;
        }
        int side = wp(7f);
        for (int i = 0; i < cols.size(); i++) {
            View v = new View(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(side, side);
            if (i > 0) lp.leftMargin = wp(2f);
            v.setLayoutParams(lp);
            GradientDrawable g = new GradientDrawable();
            g.setColor(cols.get(i).intValue());
            g.setStroke(dp(1), Color.BLACK);
            v.setBackground(g);
            swatchRow.addView(v);
        }
        swatchRow.setVisibility(View.VISIBLE);
    }

    private void openConditionDialog() {
        hideKeyboard();
        clearAllFocus();
        if (conditions.isEmpty()) {
            OnError("Aucun état disponible : appelle SetConditionOptions.");
            return;
        }
        try {
            final AlertDialog[] ref = new AlertDialog[1];

            LinearLayout content = new LinearLayout(context);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(wp(6f), hp(2.5f), wp(6f), hp(1.5f));
            GradientDrawable wbg = new GradientDrawable();
            wbg.setColor(Color.WHITE);
            wbg.setCornerRadius(wp(7f));
            content.setBackground(wbg);

            TextView title = text("État", 18, ink(0xE9), true);
            title.setPadding(0, 0, 0, hp(1f));
            content.addView(title);

            RadioGroup group = new RadioGroup(context);
            group.setOrientation(LinearLayout.VERTICAL);
            ColorStateList tint = ColorStateList.valueOf(radioButtonColor);
            for (int i = 0; i < conditions.size(); i++) {
                final String item = conditions.get(i);
                RadioButton rb = new RadioButton(context);
                rb.setId(View.generateViewId());
                rb.setText(item);
                rb.setTextColor(ink(0xC0));
                rb.setTextSize(15);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    rb.setButtonTintList(tint);
                }
                Typeface rf = shopFont();
                if (rf != null) rb.setTypeface(rf);
                rb.setPadding(wp(2.2f), hp(1.5f), wp(2.2f), hp(1.5f));
                rb.setChecked(item.equals(condition));
                rb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectCondition(item, true);
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                group.addView(rb);
            }
            content.addView(group);

            TextView cancel = text("Annuler", 14, ink(0xC0), true);
            cancel.setPadding(wp(3f), hp(1.5f), wp(1f), hp(1f));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            clp.gravity = Gravity.END;
            cancel.setLayoutParams(clp);
            cancel.setClickable(true);
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ref[0] != null) ref[0].dismiss();
                }
            });
            content.addView(cancel);

            AlertDialog d = new AlertDialog.Builder(activity).setView(content).create();
            ref[0] = d;
            d.show();
            try {
                d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                d.getWindow().setLayout(wp(90f), ViewGroup.LayoutParams.WRAP_CONTENT);
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            OnError("openConditionDialog: " + e.getMessage());
        }
    }

    private void selectCondition(String c, boolean fire) {
        condition = c == null ? "" : c;
        if (condField != null) {
            if (condField.value != null) condField.value.setText(condition);
            condField.setFloated(!condition.isEmpty(), true);
        }
        if (fire) OnConditionSelected(condition);
    }

    @SimpleFunction(description = "Retourne l'état choisi (Neuf, Bon état...), vide si aucun.")
    public String GetCondition() {
        return condition == null ? "" : condition;
    }

    @SimpleFunction(description = "Retourne la couleur saisie, vide si aucune.")
    public String GetColor() {
        return color == null ? "" : color.trim();
    }

    @SimpleFunction(description = "Retourne la taille saisie, vide si aucune.")
    public String GetSize() {
        return size == null ? "" : size.trim();
    }

    @SimpleFunction(description = "Retourne la page en JSON : condition, color, size. À brancher sur SetProductFormValue (clés condition, color, size) de ton formulaire produit.")
    public String GetVariantsJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("condition", GetCondition());
            o.put("color", GetColor());
            o.put("size", GetSize());
            return o.toString();
        } catch (Exception e) {
            OnError("GetVariantsJson: " + e.getMessage());
            return "{}";
        }
    }
}

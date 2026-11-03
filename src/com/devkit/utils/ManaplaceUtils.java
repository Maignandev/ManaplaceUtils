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
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
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
        description = "Extension ManaplaceUtils pour Seven : écrans Ajouter un événement (Gratuit, Premium, abonnement Google Play), billets blancs, bouton Ajouter un événement avec vérification d'internet, pages réglages et langue, chat (bulles, saisie flottante, historique, conversations, WebSocket), barre de navigation, badges, notifications, polices, icônes Phosphor, dialogue alpha, menu déroulant, photo du billet, serveur, JSON et son du scan.",
        category = ComponentCategory.EXTENSION,
        nonVisible = true
)
@SimpleObject(external = true)
@UsesPermissions(
        permissionNames =
                "android.permission.READ_EXTERNAL_STORAGE," +
                "android.permission.READ_MEDIA_IMAGES," +
                "android.permission.INTERNET," +
                "android.permission.POST_NOTIFICATIONS," +
                "android.permission.ACCESS_NETWORK_STATE"
)
public class ManaplaceUtils extends AndroidNonvisibleComponent implements ActivityResultListener {

    private final Context context;
    private final Activity activity;
    private final Form monForm;
    private final int requestCode;

    private Dialog activeAlphaDialog;
    private View activeDialogContentView;

    private Typeface customTypeface = Typeface.DEFAULT;

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
            description = "Charge une police personnalisée .ttf ou .otf (fichier des Assets, chemin complet ou dossier Companion). Sans police chargée, Seven utilise Manrope-Medium.ttf si ce fichier est dans les Assets. À appeler avant BuildEventForm."
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
            description = "Ajoute une bulle de chat avec un petit avatar rond dessiné en code (sa couleur change selon l'uid de l'expéditeur)."
    )
    public void AddChatBubble(
            final AndroidViewComponent chatContainer,
            final String messageText,
            final String timeText,
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

                            chatApplyDefaultAvatar(avatarImg, senderUid);

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
            description = "Attache la zone de saisie au-dessus du clavier de manière flottante. editTextComponent : le TextBox de saisie. maxHeightPx : hauteur maximale du champ en pixels (0 = 40 % de l'écran ; toujours limitée entre 1 ligne et 40 % de l'écran) : au-delà, le texte défile dans le champ ; la longueur est limitée à 2000 caractères. Un nouvel appel remplace l'écouteur précédent. Pour la zone de saisie dessinée en code, utilise BuildChatInputBar."
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

        EditText edit = null;
        if (editTextComponent instanceof AndroidViewComponent) {
            View v = ((AndroidViewComponent) editTextComponent).getView();
            if (v instanceof EditText) edit = (EditText) v;
        }

        chatAttachKeyboard(containerView, edit, maxHeightPx);
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

                            pickTarget = 0;
                            OnError("Permission refusée.");

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

                                            pickTarget = 0;
                                            OnError("OpenPhotoPicker: " + e.getMessage());
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
        final int pickedFor = pickTarget;
        pickTarget = 0;

        if (resultCode == Activity.RESULT_OK && data != null) {

            Uri selectedImageUri = data.getData();

            if (selectedImageUri != null) {

                if (pickedFor == 1) {
                    eventImagePicked(selectedImageUri.toString());
                    OnEventImagePicked(selectedImageUri.toString());
                } else {
                    OnPhotoPicked(selectedImageUri.toString());
                }

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
                                            ? evT("negotiating")
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
                        ? evT("negotiating")
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
    // 11b. SAISIE DE CHAT DESSINÉE EN CODE, EN-TÊTE PRODUIT ET AVATAR PAR DÉFAUT
    // =========================================================================

    private static final int CHAT_MAX_CHARS = 2000;

    private EditText chatEdit;
    private TextView chatImageIconTv;
    private TextView chatSendTv;
    private GradientDrawable chatSendBg;
    private String chatIconImage = "";
    private String chatIconSend = "";
    private boolean chatIconOpensGallery = true;
    private int chatAccent = Color.parseColor("#3949AB");
    private Typeface chatIconFont;
    private ViewTreeObserver.OnGlobalLayoutListener chatKeyboardListener;
    private View chatKeyboardRoot;
    private View chatKeyboardContainer;

    private FrameLayout chatHeaderPhotoHolder;
    private ImageView chatHeaderPhoto;
    private TextView chatHeaderTitle;
    private String chatProductUid = "";
    private String chatProductTitle = "";
    private String chatProductPhoto = "";

    // ---- événements ----

    @SimpleEvent(description = "L'icône image de la zone de saisie du chat a été touchée. La galerie s'ouvre aussi toute seule (voir SetChatImageIconOpensGallery) : récupère ensuite l'image avec OnPhotoPicked et affiche-la avec AddChatImageBubble.")
    public void OnChatImageIconClick() {
        EventDispatcher.dispatchEvent(this, "OnChatImageIconClick");
    }

    @SimpleEvent(description = "Le cercle photo ou le titre du produit, en haut du chat, a été touché. productUid : l'uid enregistré avec SetChatProduct. Ouvre ensuite le produit (RequestProductDetails puis BuildProductPage).")
    public void OnChatProductClick(String productUid) {
        EventDispatcher.dispatchEvent(this, "OnChatProductClick", productUid);
    }

    // ---- configuration ----

    @SimpleFunction(description = "Définit les icônes de la zone de saisie par leur caractère Phosphor. imageIcon : icône image (à droite du champ). sendIcon : icône du bouton d'envoi. Texte vide = symbole simple (+ et flèche). La police d'icônes est celle de SetShopIconFont (ou SetChatIconFont).")
    public void SetChatInputIcons(String imageIcon, String sendIcon) {
        chatIconImage = imageIcon == null ? "" : imageIcon;
        chatIconSend = sendIcon == null ? "" : sendIcon;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                chatApplyIcons();
            }
        });
    }

    @SimpleFunction(description = "Police d'icônes dédiée au chat (ex : Phosphor.ttf dans les Assets). Facultatif : sans elle, la police de SetShopIconFont est utilisée.")
    public void SetChatIconFont(String fontPath) {
        chatIconFont = tryLoadTypeface(fontPath);
        if (chatIconFont == null && fontPath != null && !fontPath.trim().isEmpty()) {
            OnError("SetChatIconFont: police introuvable (" + fontPath + ").");
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                chatApplyIcons();
            }
        });
    }

    @SimpleFunction(description = "Vrai (par défaut) : toucher l'icône image ouvre aussi la galerie (OpenPhotoPicker). Faux : seul l'événement OnChatImageIconClick est déclenché et tu ouvres la galerie toi-même.")
    public void SetChatImageIconOpensGallery(boolean opens) {
        chatIconOpensGallery = opens;
    }

    @SimpleFunction(description = "Couleur d'accent de la zone de saisie : icône image et bouton d'envoi rempli (bleu #3949AB par défaut).")
    public void SetChatAccentColor(int color) {
        chatAccent = color;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                chatApplyIcons();
            }
        });
    }

    // ---- zone de saisie dessinée en code ----

    @SimpleFunction(description = "Dessine la zone de saisie du chat dans l'arrangement donné (hauteur Automatique, placé en bas) : champ à 82 % de la largeur, coins 30, hauteur qui s'adapte au texte, icône image à droite et bouton d'envoi rond. maxHeightPx : hauteur maximale du champ en pixels (0 = 40 % de l'écran ; toujours limitée entre 1 ligne et 40 % de l'écran, au-delà le texte défile dans le champ). Le texte est limité à 2000 caractères. L'envoi déclenche OnChatMessageSent. Elle remonte au-dessus du clavier comme AttachFloatingInputWithDynamicHeight. Appelle LoadCustomFont avant.")
    public void BuildChatInputBar(final AndroidViewComponent container, final int maxHeightPx) {
        if (container == null || container.getView() == null) {
            OnError("BuildChatInputBar: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    buildChatInputBarInternal(container.getView(), maxHeightPx);
                } catch (Exception e) {
                    OnError("BuildChatInputBar: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Retourne le texte écrit dans la zone de saisie dessinée par BuildChatInputBar (vide si rien).")
    public String GetChatInputText() {
        if (chatEdit == null || chatEdit.getText() == null) return "";
        return chatEdit.getText().toString();
    }

    @SimpleFunction(description = "Met un texte dans la zone de saisie du chat (par exemple un message prédéfini).")
    public void SetChatInputText(final String text) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if (chatEdit == null) return;
                chatEdit.setText(text == null ? "" : text);
                chatEdit.setSelection(chatEdit.getText().length());
            }
        });
    }

    @SimpleFunction(description = "Vide la zone de saisie du chat.")
    public void ClearChatInput() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if (chatEdit != null) chatEdit.setText("");
            }
        });
    }

    @SimpleFunction(description = "À appeler en quittant le chat : retire l'écouteur du clavier et remet la zone de saisie en place.")
    public void ReleaseChatInput() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                chatDetachKeyboard();
                chatEdit = null;
                chatImageIconTv = null;
                chatSendTv = null;
                chatSendBg = null;
            }
        });
    }

    private ViewGroup chatHost(View v) {
        if (v instanceof ScrollView) {
            View c = ((ScrollView) v).getChildAt(0);
            return c instanceof ViewGroup ? (ViewGroup) c : null;
        }
        if (v instanceof HorizontalScrollView) {
            View c = ((HorizontalScrollView) v).getChildAt(0);
            return c instanceof ViewGroup ? (ViewGroup) c : null;
        }
        return v instanceof ViewGroup ? (ViewGroup) v : null;
    }

    private Typeface chatIcons() {
        return chatIconFont != null ? chatIconFont : shopIconFont;
    }

    private void buildChatInputBarInternal(View hostView, int maxHeightPx) {
        ViewGroup host = chatHost(hostView);
        if (host == null) {
            OnError("BuildChatInputBar: le conteneur doit être un arrangement.");
            return;
        }
        // l'arrangement doit s'adapter à la hauteur du champ
        ViewGroup.LayoutParams hlp = hostView.getLayoutParams();
        if (hlp != null && hlp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            hlp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            hostView.setLayoutParams(hlp);
        }
        host.removeAllViews();

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        final int sw = dm.widthPixels;
        final int sh = dm.heightPixels;
        int pillMinH = (int) (sh * 0.062f);
        int vMargin = (int) (sh * 0.012f);
        int sendSize = (int) (sw * 0.13f);
        Typeface font = customTypeface != null ? customTypeface : Typeface.DEFAULT;

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, (int) (sw * 0.02f), 0);

        // carte du champ : 82 % de la largeur, coins 30
        FrameLayout pill = new FrameLayout(context);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                (int) (sw * 0.75f), ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.leftMargin = (int) (sw * 0.02f);
        plp.topMargin = vMargin;
        plp.bottomMargin = vMargin;
        pill.setLayoutParams(plp);
        pill.setMinimumHeight(pillMinH);
        GradientDrawable pbg = new GradientDrawable();
        pbg.setColor(Color.argb(0xED, 0x1A, 0x1A, 0x1B));
        pbg.setCornerRadius(dpToPx(30));
        pbg.setStroke(0, Color.TRANSPARENT);
        pill.setBackground(pbg);

        chatEdit = new EditText(context);
        chatEdit.setBackground(null);
        chatEdit.setHint(evT("msg_hint"));
        chatEdit.setHintTextColor(Color.parseColor("#9E9E9E"));
        chatEdit.setTextColor(Color.WHITE);
        chatEdit.setTextSize(15);
        chatEdit.setTypeface(font);
        chatEdit.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        chatEdit.setSingleLine(false);
        chatEdit.setHorizontallyScrolling(false);
        chatEdit.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        chatEdit.setPadding((int) (sw * 0.045f), (int) (sh * 0.016f),
                (int) (sw * 0.16f), (int) (sh * 0.016f));
        chatEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(CHAT_MAX_CHARS)});
        pill.addView(chatEdit, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL));

        // icône image (à droite, en bas du champ)
        chatImageIconTv = new TextView(context);
        chatImageIconTv.setGravity(Gravity.CENTER);
        chatImageIconTv.setIncludeFontPadding(false);
        chatImageIconTv.setTextSize(24);
        chatImageIconTv.setClickable(true);
        chatImageIconTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnChatImageIconClick();
                if (chatIconOpensGallery) {
                    pickTarget = 0;
                    OpenPhotoPicker();
                }
            }
        });
        pill.addView(chatImageIconTv, new FrameLayout.LayoutParams(
                (int) (sw * 0.12f), pillMinH, Gravity.BOTTOM | Gravity.END));
        row.addView(pill);

        // bouton d'envoi rond : gris si vide, rempli si du texte est écrit
        chatSendTv = new TextView(context);
        chatSendBg = new GradientDrawable();
        chatSendBg.setShape(GradientDrawable.OVAL);
        chatSendTv.setBackground(chatSendBg);
        chatSendTv.setGravity(Gravity.CENTER);
        chatSendTv.setIncludeFontPadding(false);
        chatSendTv.setTextSize(22);
        chatSendTv.setClickable(true);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(sendSize, sendSize);
        slp.leftMargin = (int) (sw * 0.015f);
        slp.bottomMargin = 0;
        chatSendTv.setLayoutParams(slp);
        chatSendTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatSendClicked();
            }
        });
        row.addView(chatSendTv);

        chatEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                chatUpdateSendState();
            }
        });

        host.addView(row, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        chatApplyIcons();
        chatAttachKeyboard(hostView, chatEdit, maxHeightPx);
    }

    private void chatApplyIcons() {
        if (chatImageIconTv != null) {
            boolean has = !chatIconImage.isEmpty();
            chatImageIconTv.setTypeface(has && chatIcons() != null ? chatIcons() : Typeface.DEFAULT);
            chatImageIconTv.setText(has ? chatIconImage : "+");
            chatImageIconTv.setTextColor(chatAccent);
        }
        chatUpdateSendState();
    }

    private void chatUpdateSendState() {
        if (chatSendTv == null || chatEdit == null || chatSendBg == null) return;
        boolean active = chatEdit.getText() != null
                && chatEdit.getText().toString().trim().length() > 0;
        chatSendBg.setColor(active ? Color.WHITE : Color.TRANSPARENT);
        chatSendBg.setStroke(dp(1), active ? Color.WHITE : Color.parseColor("#9E9E9E"));
        chatSendTv.setTextColor(active ? Color.BLACK : Color.parseColor("#9E9E9E"));
        boolean has = !chatIconSend.isEmpty();
        chatSendTv.setTypeface(has && chatIcons() != null ? chatIcons() : Typeface.DEFAULT);
        chatSendTv.setText(has ? chatIconSend : "\u2191");
    }

    private void chatSendClicked() {
        if (chatEdit == null || chatEdit.getText() == null) return;
        String m = chatEdit.getText().toString().trim();
        if (m.isEmpty()) return;
        if (m.length() > CHAT_MAX_CHARS) m = m.substring(0, CHAT_MAX_CHARS);
        chatEdit.setText("");
        OnChatMessageSent(m);
    }

    // Hauteur maximale réelle du champ : entre 1 ligne et 40 % de l'écran, texte limité en longueur
    private void chatApplyMaxHeight(EditText e, int requestedPx) {
        if (e == null) return;
        int screenH = context.getResources().getDisplayMetrics().heightPixels;
        int cap = (int) (screenH * 0.40f);
        int minPx = Math.max(1, e.getLineHeight()) + e.getPaddingTop() + e.getPaddingBottom();
        int max = requestedPx <= 0 ? cap : requestedPx;
        max = Math.max(minPx, Math.min(max, cap));
        e.setMaxHeight(max);
        e.setVerticalScrollBarEnabled(true);
        InputFilter[] cur = e.getFilters();
        boolean has = false;
        for (int i = 0; i < cur.length; i++) {
            if (cur[i] instanceof InputFilter.LengthFilter) has = true;
        }
        if (!has) {
            InputFilter[] n = new InputFilter[cur.length + 1];
            System.arraycopy(cur, 0, n, 0, cur.length);
            n[cur.length] = new InputFilter.LengthFilter(CHAT_MAX_CHARS);
            e.setFilters(n);
        }
    }

    private void chatDetachKeyboard() {
        try {
            if (chatKeyboardListener != null && chatKeyboardRoot != null) {
                ViewTreeObserver vto = chatKeyboardRoot.getViewTreeObserver();
                if (vto.isAlive()) vto.removeOnGlobalLayoutListener(chatKeyboardListener);
            }
            if (chatKeyboardContainer != null) chatKeyboardContainer.setTranslationY(0);
        } catch (Exception ignored) {
        }
        chatKeyboardListener = null;
        chatKeyboardRoot = null;
        chatKeyboardContainer = null;
    }

    // Remonte le conteneur au-dessus du clavier ; un nouvel appel remplace l'écouteur précédent
    private void chatAttachKeyboard(final View containerView, EditText edit, int maxHeightPx) {
        evAutoPan();
        chatApplyMaxHeight(edit, maxHeightPx);
        chatDetachKeyboard();

        final View rootView = activity.getWindow().getDecorView().getRootView();
        chatKeyboardRoot = rootView;
        chatKeyboardContainer = containerView;
        chatKeyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                float ty = 0f;
                if (keypadHeight > screenHeight * 0.15) {
                    // Position naturelle (sans décalage) du bas de la saisie, puis
                    // on la place à 1 % de l'écran au-dessus du clavier, quel que soit
                    // le mode (fenêtre redimensionnée ou non).
                    int[] loc = new int[2];
                    containerView.getLocationOnScreen(loc);
                    float naturalBottom = loc[1] + containerView.getHeight() - containerView.getTranslationY();
                    float targetBottom = r.bottom - Math.round(screenHeight * 0.01f);
                    ty = Math.min(0f, targetBottom - naturalBottom);
                }
                if (Math.abs(containerView.getTranslationY() - ty) > 0.5f) containerView.setTranslationY(ty);
            }
        };
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(chatKeyboardListener);
    }

    // ---- en-tête du chat : cercle photo du produit + titre ----

    @SimpleFunction(description = "Dessine l'en-tête du chat dans l'arrangement donné (à côté de ta flèche retour) : cercle photo du produit (7 % de la hauteur, 17 % de la largeur, bien rond) et titre en Manrope Medium 19. Le cercle et le titre sont cliquables (OnChatProductClick). Enregistre le produit avec SetChatProduct. Appelle LoadCustomFont avant.")
    public void BuildChatHeader(final AndroidViewComponent container) {
        if (container == null || container.getView() == null) {
            OnError("BuildChatHeader: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    buildChatHeaderInternal(container.getView());
                } catch (Exception e) {
                    OnError("BuildChatHeader: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Enregistre le produit du chat : uid (renvoyé par OnChatProductClick), titre et adresse de la photo. Peut être appelé avant ou après BuildChatHeader. Utilise par exemple les productImage et productTitle reçus avec le message.")
    public void SetChatProduct(String uid, String title, String photo) {
        chatProductUid = uid == null ? "" : uid.trim();
        chatProductTitle = title == null ? "" : title.trim();
        chatProductPhoto = photo == null ? "" : photo.trim();
        runOnUi(new Runnable() {
            @Override
            public void run() {
                chatRenderHeader();
            }
        });
    }

    @SimpleFunction(description = "Retourne l'uid du produit enregistré avec SetChatProduct.")
    public String GetChatProductUid() {
        return chatProductUid;
    }

    private void buildChatHeaderInternal(View hostView) {
        ViewGroup host = chatHost(hostView);
        if (host == null) {
            OnError("BuildChatHeader: le conteneur doit être un arrangement.");
            return;
        }
        host.removeAllViews();

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int sw = dm.widthPixels;
        int sh = dm.heightPixels;
        // 7 % de la hauteur et 17 % de la largeur : on garde la plus petite mesure pour un cercle parfait
        int side = Math.min((int) (sh * 0.07f), (int) (sw * 0.17f));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chatProductUid.isEmpty()) {
                    OnError("[diag] uid du produit inconnu : appelle SetChatProduct avant.");
                    return;
                }
                OnChatProductClick(chatProductUid);
            }
        };

        chatHeaderPhotoHolder = new FrameLayout(context);
        chatHeaderPhotoHolder.setLayoutParams(new LinearLayout.LayoutParams(side, side));
        GradientDrawable hbg = new GradientDrawable();
        hbg.setShape(GradientDrawable.OVAL);
        hbg.setColor(Color.parseColor("#F7F7F7"));
        hbg.setStroke(dp(1), Color.parseColor("#E6E6E6"));
        chatHeaderPhotoHolder.setBackground(hbg);
        chatHeaderPhotoHolder.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        chatHeaderPhotoHolder.setClipToOutline(true);
        chatHeaderPhoto = new ImageView(context);
        chatHeaderPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        chatHeaderPhotoHolder.addView(chatHeaderPhoto, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        chatHeaderPhotoHolder.setClickable(true);
        chatHeaderPhotoHolder.setOnClickListener(click);
        row.addView(chatHeaderPhotoHolder);

        chatHeaderTitle = new TextView(context);
        chatHeaderTitle.setTextSize(19);
        chatHeaderTitle.setTextColor(Color.argb(0xE9, 0x1A, 0x1A, 0x1B));
        chatHeaderTitle.setTypeface(customTypeface != null ? customTypeface : Typeface.DEFAULT);
        chatHeaderTitle.setSingleLine(true);
        chatHeaderTitle.setEllipsize(TextUtils.TruncateAt.END);
        chatHeaderTitle.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = (int) (sw * 0.03f);
        chatHeaderTitle.setLayoutParams(tlp);
        chatHeaderTitle.setClickable(true);
        chatHeaderTitle.setOnClickListener(click);
        row.addView(chatHeaderTitle);

        host.addView(row, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        chatRenderHeader();
    }

    private void chatRenderHeader() {
        if (chatHeaderTitle != null) chatHeaderTitle.setText(chatProductTitle);
        if (chatHeaderPhoto != null) {
            chatHeaderPhoto.setImageDrawable(null);
            if (!chatProductPhoto.isEmpty()) loadImageAsync(chatHeaderPhoto, chatProductPhoto);
        }
    }

    // ---- avatar par défaut (image intégrée, couleur propre à chaque utilisateur) ----

    private static final String CHAT_DEFAULT_AVATAR_B64 =
            "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAUDBAQEAwUEBAQFBQUGBwwIBwcHBw8LCwkMEQ8SEhEPERETFhwXExQaFRERGCEYGh0d" +
            "Hx8fExciJCIeJBweHx7/2wBDAQUFBQcGBw4ICA4eFBEUHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4e" +
            "Hh4eHh4eHh7/wAARCACgAKADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUF" +
            "BAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVW" +
            "V1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi" +
            "4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAEC" +
            "AxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVm" +
            "Z2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq" +
            "8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDsrv8A4+H+tRVLd/8AHw/1qE9Ca/NcR/Fl6v8AM8LM/wDfa3+KX5sWorz/AI85v+ubfyrs" +
            "oPB8ckMch1CQb1DY8ocZGfWodW8IRw6VeTC/kPlwSPjyhzhSfWvIoZ3gvbxjz63XR9/Q6aOUYvnjLl6rqjxwdBXRfDb/AJHjS/8A" +
            "rq3/AKA1c6OQD7Vt+Brg2vizT7gJv2SE4zjPymv3TN8PUxOAr0KSvKUJJerTSP1LFVY0qE6ktkm38ke+Vk+Kv+Qcn/XUfyNVR4if" +
            "H/Hov/ff/wBaqmqaq19AsRgEeG3ZDZ7GvxrhbgTPcBnGHxOIo2hGV2+aLsrPonc/Ms34gy/EYKpSpzu2tNH/AJGbTJugp9Rz/wAN" +
            "fuWc/wC5z+X5o8Tgb/kd0f8At7/0lkdUL/8A4+P+Air9QT24lk37yOMdK/OcwozrUuWC1ufsHEmBr47B+yoK8rp7pd+5Qoq59jH/" +
            "AD0P5UfYx/z0P5V4n9mYn+X8UfAf6qZp/wA+/wAY/wCZTqi33j9a2vsY/wCeh/Ks1rcbj856+lcGOwVamo8y/I+m4bkuHHUlmPuc" +
            "9rdb2vf4b91uR2v/AB8xf74/nXQViwQATRneeGHb3rar1+H4OFOd+6PsqOb4TM7yw0uZLR6NfmkFFFFfQmxnXf8Ax8P9ahP3T9K0" +
            "nt43cu27J96YbSHB+909a+eq5XXnOUlbVs/NMbwnj62IqVI8tpNvfu/Q9Is/+POD/rkv8hVfXv8AkBah/wBesv8A6AasWgxawj/p" +
            "mv8AIVX17/kBah/16y/+gGvxrDq2Mj/iX5nsQTjZM+dV+6PpWr4U/wCRhs/98/8AoJrKX7o+lavhT/kYbP8A3z/6Ca/sOPxI9HOP" +
            "+RfX/wAEvyZ6KKKO1VNVuJLa3V4tuS2DkZrXG4ung6E8RU+GOrsfgOXYCrmGKhhaNuabsr7Fuo5u1Yv9rXf/AEz/AO+KRtUum6+X" +
            "/wB818RmHGmW4jDypw5ru3Tz9T9V4Z4DzTLMyp4mvy8sb7N31TXbzNeisf8AtK5/6Z/980f2lc/9M/8Avmvmv7ewvn93/BP1T6pU" +
            "Niisf+0rn/pn/wB81sWf721ikf7zLk4r1cprRzWpKnh90r66f5nh57m1DI6Ma2KvaTsra62v5AKym+8fqa2xGvvWI/32+prn4ky+" +
            "rhI03Utrfb5H5tn/ABHg85jBYa/u3vdW3t5+QsX+tT/eFahrKU7WDDqDmpvtc3+z+VeRl+Mp4eMlPqdfDWd4bLadSNe+rWyuX6Kz" +
            "Zb2dYyRsz/u1B/aVz6p/3zXZPO8NB2d/u/4J+j5Vi6eaUXWobJ2107f5m1ikIODXXaRp1jLpsEklpE7svJI5PJq1/ZWm/wDPlD+V" +
            "eJjuNcLg8TUw8qcm4ScXt0du559DPKNalGqou0kn95Ztf+PWL/rmv8hVbXv+QFqH/XrL/wCgGrigKoVQAAMAVBqaq+m3SMAVaBwQ" +
            "e42mvyijVSxMZ/3k/wATxpSUbyPnBfuj6Vq+FP8AkYbP/fP/AKCa2103T9o/0SLp6VLb2drbzLNBAkcinKso5Ff0uuMsKnf2cvw/" +
            "zPKx3FOHxOGqUYwacotdOqsdKKztf/481/66D+Rqv9puP+ez/nTJpJJV2yuXXOcGubN+LMPjsFUw8ISTkra2/wAz4PIoPLcxo4up" +
            "qoO9luZ1FXPLj/uCorhVXbtUDrX5nOhKCu2fuGVcY4XMsVHDU6ck5X3t0V+/kQUUVj6xdXEN5simZF2g4FdmU5XUzOv7Cm0na+vk" +
            "fU16yox5mjYrpdN/48IP9wV5t9uvP+fmT9K9D8PO0mh2buxZjECSe9fp3CvDlfKsROpUmmmraX7o/KPE7Fxr4Cikvt/oy8Kwn++3" +
            "+8a3RWE/32/3jWPHfwUPWX6H5Tle8vkJRU+nIsmoW0bqGVpUDA9wWFeiHQ9Hyf8AiW2//fJr8mzDNaeBlGM03fsfU4HLamMTcWlb" +
            "ueYXH+pP4VUr0DxnpenWvh6ea3s4YpAyAMo5GWrz+sKOOhjY+0grLbU/XeDcHLCYCUJO/vN/gj13Qgf7It+D93+pq7g+hrhA7gYD" +
            "sB7E0eZJ/wA9H/76NfcZj4TTxmLq4lYpLnlKVuTa7b/mPxzC8YxoUYUvY35Ulv2Xod3g+hqDUB/xL7ng/wCpf/0E1wjO+4/vH6/3" +
            "jSF3IwXc/wDAjXyS8PpQnf2+393/AO2P1aOUupTT59127/Mxh90fSlrWwPQflRgeg/KvrP7Gf8/4f8E+c/1Gl/z+/wDJf+CZNFa2" +
            "B6D8qr34AhHA+9WVfK3RpufNe3kceP4Qlg8NOv7W/Kr2t/wSjUF0M7fxqesHxYzLJbbWI4bocelcuAy95jXWGUuW/W19lc4OE6vs" +
            "s1pztf4v/SWaGD6Vg67/AMf/APwAVS8x/wC+/wD30aaSSckk/WvuMj4WlleJ9u6vNo1a1t/mfreIxvtoctrAK9J8N/8AIAsf+uIr" +
            "zYV6T4b/AOQBY/8AXEV9tQ3Z+Y+If+5Uv8X6M0BWE5G9uR1Nbp6H6V3EMcfkp+7T7o/hHpX5r4nZmsBTwzcb3cuttuU+J4ay542V" +
            "Rc1rW6d7nmWlEf2pac/8t0/9CFepnqfrTBHGOkaD/gIp1fhOa5ksfKMlG1vO5+g5bl/1KMlzXuYPj3/kWLj/AH4//QhXmdemePf+" +
            "RYuP9+P/ANCFeZ16+R/7u/X9EfonD/8Auz9X+SOyS2uHUOlvKynoQhINOFnd/wDPrP8A9+zXU6Hn+yLb/dP8zV5Sdw571+iZl4r4" +
            "rB4ythlh4tQlKN7vWza7H4fheD6NehCq6rXMk9l1R5w7oHILKCDyCaQSJ/fX86oXv/H5P/11f/0I0yL/AFi/UVpDOp1GnyLU+spc" +
            "ZVlKNL2S7bv0NSkZlVSzEKB1Jpar6l/x4y/QfzFe1XqOlSlNdE2fo8VeSRJ9og/57R/99CoL2aFogFlQnd2YVi0V8nWz2pVpuDgt" +
            "S8dlMMXh50JSa5lYu70/vL+dYPixgz22CDw3T8K0ayPEX3oPo39K7uFa7nmlNW7/APpLPl8PwbRyyosTGq249Gl10/UyqmhtbqZN" +
            "8NtNIucblQkVDXb+C/8AkCD/AK6v/Sv1PG4l4anzpXPWhHmdjkfsF9/z5XP/AH6P+FeheH0ePQ7NHVlZYgCpGCKnyfU1Mn3R9KnK" +
            "sfLE1JRcbWR8D4j0+TA0v8X6MU9D9K7qH/Up/uj+VcKeh+ld1D/qU/3R/KvzLxj/AIWE9Z/+2ny/BPx1v+3f1HMQqlmIAAySe1Rf" +
            "a7X/AJ+Yf+/gov8A/jyn/wCubfyNcQOlfH8E8E0OJKFWrVquHI0tEne6v1Paz7PamVzhGMFLmRt+OriCTw1Osc8TsXThXBP3hXm9" +
            "dDrX/IOk+q/zrnq9TNuHKfD1dYWnNzTXNdq291+h9/wJmcsyy2VaUbWm1+C/zPYdKikg06GGUbXVcEZz3NWl+8PrSUq/eH1r8vxm" +
            "KnjMRUxE/im3J22u3dng0KUaNKNOO0Ul9x5Re/8AH5P/ANdX/wDQjUaHDqT0BFSXv/H5P/11f/0I1FX6hRdoxfofnbk41OZdGaP2" +
            "iH/noPypGRr8GztAJJ5OEXOM9+p+lZ9avhH/AJGOz/3m/wDQTXdjs4rrDVHZfC+/b1PssNxljp1oRcY6tdH39Sv/AMInr3/Pkv8A" +
            "39X/ABqrqeianpsCz3tuI42baDvVufwPtXrArmPiT/yA4f8Ar4X/ANBavzrCZvWrVo05JWfr/mfe4POsRXrxpySs35/5nnlU9R02" +
            "8vzGbWISBM7vmAxn61crU0T7sv1FffcPV5UMwhOO6v8Akz6DFx5qTRyn/CO6v/z6r/38X/Gup8MWk9lpYguUCSeYxxkHg49K06K/" +
            "QsRmFTEQ5JJWPIjSUXdBTWv7SMmN5cMvBG006sLUP+P2X/eryMTnVfKIqrQSbemt/Xo0efmnDuGz6mqOJk0ou/utLy6pm0dSssH9" +
            "9/46a6uLxVoSxopvSCFA/wBU3p9K8yor4viXNKvEUaccUkuS9uXTe292+xjlfAuX5Y5OjOb5rbtdPkj0u48TaLPBJDFdlpJFKqPK" +
            "bkkYHasGuYsv+PyH/fH866evufDDBU8LhcQoN6yW/ofnHiXgaeDxNCMG9Yvf1RW1OJ57J44xliRgZx3rH/s29/54j/vsV0NFfV5v" +
            "wvhM1rqvWlJNK2lvPun3PCyDjXH5HhnhsPGLi23qm3dpLo12Onvddt7S7kt3hmZkOCVxg1CPE1pkH7NP+n+NYviH/kM3P+8P5CqF" +
            "fkOA4QyyvhKVWcXeUU3q92kz9ro5fQlTjJrdIq3Fs8lxJIGUB3ZgPqc1G1o4BO9eBmr1JJ/q2+hr6R5Vhox0W3meTW4Uy1QlLld9" +
            "erMqtXwj/wAjFZ/7zf8AoJrKHStXwj/yMVn/ALzf+gmvj8d/utT/AAv8j8xwf+8U/VfmejCuX+JP/IDh/wCvgf8AoLV1Arl/iT/y" +
            "A4f+vkf+gtX55l3+9Q9T9Syz/e6fqee1raCpZJsEcEf1rJrY8O/cn+o/rX61wpQhXzWnTns+b/0lns8Y5hXy7KKuIoO0ly20vvJI" +
            "0fKPqKYylTg1PUU33/wr9PzTLaGGoc9Na37n51wfxTmOaZh7DESTjyt6JLawysLUP+P2X/erdrC1D/j9l/3q/POIv4EfX9D9cwfx" +
            "Mr0UUV8iegTWX/H5D/10H866euYsc/bIcf8APQfzrp6/VfDz/d63qvyPxDxY/wB7w/8Ahf5oZPIIojIwJA9Krf2hF/zzk/SpdQ/4" +
            "83/D+dZFdXE2fYzL8VGlQkknFPZPqz86wWFp1qfNLuacGpPq8K6lLEkT3A3FFJIXt1P0p9Z3hn/kA2n+4f8A0I1o1wSowoP2VNWj" +
            "HRLsloj+loJKKSCnRRiWVIiSA7BSR2ycU2pbP/j8g/66r/MVhXbVKTXZhNJxaZsf8IZbA4+3z/8Aftas6X4YgsL+K7S7lkaMkhSg" +
            "AOQR/Wt89TRX4FPN8ZUi4yno/Q/P4ZZhISUow1Xr/mFc18RE36LCCcf6QP8A0Fq6Wud+IH/IHi/67j/0E0sqSeMpp9zoxWJqYajK" +
            "tSdpLVM8/wDs6/3zWpokYjjlwScsP5VSrQ0j7kn+8P5V+1cJUoRzam0v5v8A0lnxGdcQ5ljMFOjXquUXa6sujT6IvU103HOadRX6" +
            "/WoU68eSoro+RwGY4nL6vtsNLlla19NvmR+WPWqc+lRyzPIZnBY5xgVoUV5lfIMuxCUalJNLzf8Ame5DjXPIaxxD+6P+Rl/2PH/z" +
            "3f8A75FdCvgW1Kg/2jPyM/6tapV3if6tP90fyr8h8TsFQyeOGeCjyc3Nfre3Lbe/dn2PCvFGbY11fb1nK1raLz7I5SDwRaxTJKNQ" +
            "nJRgceWtaP8Awj0P/P1L/wB8ituivznAcWZxl8XHDV3FPfRfqmexmeFpZrKM8Yudx0XT8rHKeI9GitNGnnWeRipXgqMcsBXH16J4" +
            "y/5Fy5+qf+hCvO6+py7OMbm1N1sZU55J2votNH0S7s+MzTBUMHWUKEbK1yz4WRT4esyR/Af/AEI1p+Wvv+dZ3hX/AJF6y/3D/wCh" +
            "GtOv6IWBw8tXTX3HkZnxBmlPG1oQxE0lKSS5norsb5a+/wCdOjAjkV1+8pBH1FFFJ5fhWrOmvuRw/wCsebP/AJiZ/wDgT/zNP+3N" +
            "Q/56R/8AfsVFda/qUdu7rJHkDj92Ko1Bf/8AHpJ9P6187mXCuS08HVlHC001F/ZXb0Io5xj3UinWlv3ZJ/wlOsf89of+/IqrqetX" +
            "+owLBdOjIrbgFQLz/k1nUV+PwwGGpyUoU0mvI9+eNxE04ym2n5hUsE8kIIQgZ65Gaior0KFerQmqlKTjJdVuckoxmrS1RZ+3XH95" +
            "f++aw9b8Qana33lQyRBNgODGD1rTrl/E3/IT/wC2a/1r6/hnM8ZiMbyVaspKz3b8j2+GsuwlfG8lWmpKz3SfYn/4SnWP+esP/foU" +
            "f8JTrH/PWH/v0KxaK/QueXc+/wD9X8r/AOgeH/gK/wAjaPinWMH97D0/55CveYCTBET1KKf0r5rP3T9K+lLf/j2h/wCua/yFfjXi" +
            "9JuGEv8A3/8A20yq5fhcHb6vTUL72SVxl/I8NjcSxkB0iZlJGcEAmuFHinWcf66H/vyK7bV/+QVef9cH/wDQTXlo6V+f8O4WjXpT" +
            "dSCeq3R8rnuJq0ZwVOTWnRmrf6/qd9avbXEkbRvjIEYB4OetZdFFfVUaFOjHlpxSXkfNVa1Sq7zbb8zjodS1CGJYob24jjXhVVyA" +
            "KcdX1XB/4mN10/56GqdIfun6V+83Z+xSwOFlLmlSi2/Jf5Hq1qxa1hZiSTGpJPc4FOmJELkHBCnH5Uyy/wCPOD/rkv8A6CKdP/qJ" +
            "P9w/yrpru1GT8n+R/PEEvraX979TnPtd1j/j5l/76pGurllKtPIQeoLVDRX86vHYqSs6kvvf+Z/V39l4FPSjD/wFf5D/ADH/AL7f" +
            "nSeY/wDfb86bRXPzPuX/AGfhP+fUf/AV/kO8x/77fnR5j/32/Om0Ucz7h/Z+E/59R/8AAV/kO8x/77fnWBr5J1DJJPyL/Wt2sHXf" +
            "+P8A/wCACvruCm3mWv8AK/0OTG4ShSp81OCTv0SRRooor9aPKEP3T9K+lLf/AI9of+ua/wAhXzWfun6V9KW//HtD/wBc1/kK/G/F" +
            "34MJ/wBv/wDtp52YbR+ZFqYzptyD/wA8m/ka8/8AJix/q1/KvQNS/wCQdc/9cm/ka4IV4vAEIyw9a6vqvyNctoUqsJe0in6pMZ5M" +
            "X/PNfyo8mL/nmv5U+ivv/Y0/5V9x6X1LDf8APuP3I//Z";

    private Bitmap chatDefaultAvatarBase;
    private final LinkedHashMap<Integer, Bitmap> chatAvatarCache = new LinkedHashMap<Integer, Bitmap>();

    // 0 à 359 : toujours le même pour un même uid (0 = couleurs d'origine si l'uid est vide)
    private int chatAvatarHue(String uid) {
        if (uid == null || uid.trim().isEmpty()) return 0;
        int h = uid.trim().hashCode();
        h ^= (h >>> 16);
        h *= 0x45d9f3b;
        h ^= (h >>> 16);
        return (h & 0x7fffffff) % 360;
    }

    private float[] chatHueMatrix(int degrees) {
        double r = Math.toRadians(degrees);
        float cos = (float) Math.cos(r);
        float sin = (float) Math.sin(r);
        final float lr = 0.213f;
        final float lg = 0.715f;
        final float lb = 0.072f;
        return new float[]{
                lr + cos * (1 - lr) + sin * (-lr), lg + cos * (-lg) + sin * (-lg), lb + cos * (-lb) + sin * (1 - lb), 0, 0,
                lr + cos * (-lr) + sin * 0.143f, lg + cos * (1 - lg) + sin * 0.140f, lb + cos * (-lb) + sin * (-0.283f), 0, 0,
                lr + cos * (-lr) + sin * (-(1 - lr)), lg + cos * (-lg) + sin * lg, lb + cos * (1 - lb) + sin * lb, 0, 0,
                0, 0, 0, 1, 0
        };
    }

    private Bitmap chatDefaultAvatarFor(String uid) {
        if (chatDefaultAvatarBase == null) {
            byte[] bytes = Base64.decode(CHAT_DEFAULT_AVATAR_B64, Base64.DEFAULT);
            chatDefaultAvatarBase = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        Bitmap base = chatDefaultAvatarBase;
        if (base == null) return null;
        int hue = chatAvatarHue(uid);
        if (hue == 0) return base;
        Bitmap cached = chatAvatarCache.get(hue);
        if (cached != null) return cached;

        Bitmap out = Bitmap.createBitmap(base.getWidth(), base.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(chatHueMatrix(hue))));
        canvas.drawBitmap(base, 0, 0, paint);
        while (chatAvatarCache.size() >= 24) {
            Integer eldest = chatAvatarCache.keySet().iterator().next();
            chatAvatarCache.remove(eldest);
        }
        chatAvatarCache.put(hue, out);
        return out;
    }

    // Image par défaut quand l'expéditeur n'a pas de logo de boutique
    private void chatApplyDefaultAvatar(ImageView iv, String uid) {
        try {
            Bitmap b = chatDefaultAvatarFor(uid);
            if (b == null) return;
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setPadding(0, 0, 0, 0);
            iv.setImageBitmap(b);
            iv.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            iv.setClipToOutline(true);
        } catch (Exception ignored) {
        }
    }

    // =========================================================================
    // 12. BULLE PHOTO DANS LE CHAT
    // =========================================================================

    @SimpleFunction(description = "Ajoute une bulle de chat contenant une photo, avec le même style et positionnement que AddChatBubble (avatar dessiné en code).")
    public void AddChatImageBubble(
            final AndroidViewComponent chatContainer,
            final String imagePath,
            final String timeText,
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

                    chatApplyDefaultAvatar(avatarImg, senderUid);

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
            return evT("today");
        } else if (estHier) {
            return evT("yesterday");
        } else {
            boolean memeAnnee = calAujourdhui.get(java.util.Calendar.YEAR) == calMessage.get(java.util.Calendar.YEAR);
            String format = memeAnnee ? "d MMM" : "d MMM yyyy";
            return new java.text.SimpleDateFormat(format, evLocale()).format(new java.util.Date(millis));
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

                String uid = isMe ? myUid : conversationTag;
                int bColor = isMe ? bubbleColorMe : bubbleColorOther;
                int tColor = isMe ? textColorMe : textColorOther;

                if (!text.isEmpty()) {
                    AddChatBubble(chatContainer, text, time, uid, isMe, bColor, tColor);
                } else if (!image.isEmpty()) {
                    AddChatImageBubble(chatContainer, image, time, uid, isMe, bColor);
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
            statusTv.setText(evT("sent"));
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
                    dernier = "\uD83D\uDCF7 " + evT("photo");
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
                        ref.get().setText(evT("seen"));
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
                        ref.get().setText(evT("seen"));
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

    private Typeface shopIconFont;
    private int screenH;
    private ScrollView scrollView;
    private TextView titleTv;

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

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(context);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        Typeface f = shopFont();
        if (f != null) t.setTypeface(f);
        return t;
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

    private Bitmap decodeFile(String path, int maxW) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        if (o.outWidth <= 0) return null;
        o.inSampleSize = sampleFor(o.outWidth, maxW);
        o.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, o);
    }

    private int sampleFor(int width, int maxW) {
        int s = 1;
        while (width / (s * 2) >= maxW) s *= 2;
        return s;
    }

    // Vraies valeurs (séparées de l'affichage) : clé du champ -> valeur
    private final Map<String, String> values = new LinkedHashMap<String, String>();
    private int productPickRequestCode;

    private Typeface iconFont;
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

    private Typeface loadFontNamed(String fontPath, String who) {
        if (fontPath == null || fontPath.trim().isEmpty()) return null;
        Typeface t = tryLoadTypeface(fontPath.trim());
        if (t == null) {
            OnError(who + ": police introuvable (" + fontPath + ").");
        }
        return t;
    }

    @SimpleFunction(description = "Charge (facultatif) la police des grands titres et du gras (ex: Manrope-Bold.ttf). Sans elle, les titres utilisent la police normale en gras.")
    public void LoadCustomBoldFont(String fontPath) {
        customBoldFont = loadFontNamed(fontPath, "LoadCustomBoldFont");
        applyFonts();
    }

    private void applyFonts() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if (evContainer != null) evRender();
            }
        });
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

    private int valueColor(String key) {
        if (key.equals(invalidKey)) return col("#D93025");
        return has(key) ? cText() : col("#6E6E73");
    }

    private int iconColor() {
        return col("#1A1A1B");
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
    private int shopLogoRequestCode;

    // =========================================================================
    // OUTILS INTERNES
    // =========================================================================

    private int cTitle() { return ink(0xC0); }  // contour au repos

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
    private String color = "";
    private String size = "";

    private int editLogoRequestCode;

    // Toutes les tailles de composants sont des pourcentages de l'écran
    private int wp(float pct) {
        return Math.round(context.getResources().getDisplayMetrics().widthPixels * pct / 100f);
    }

    private int hp(float pct) {
        return Math.round(context.getResources().getDisplayMetrics().heightPixels * pct / 100f);
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

    // =========================================================================
    // SEVEN : ÉCRANS « AJOUTER UN ÉVÉNEMENT » (Gratuit / Premium) ET ABONNEMENT
    // Espace de 2 % entre les composants, titres 13, sous-titres 12, champs
    // 8 % x 93 % coins 14, grands titres 18. Couleurs : fiche V19 (inchangées).
    // =========================================================================
    private ViewGroup evContainer;
    private boolean evPremium = false;
    private boolean evSubscribeMode = false;
    private int pickTarget = 0; // 0 = chat / général, 1 = image du billet
    private String evImageUri = "";
    private ImageView evPreview;
    private LinearLayout evPickHint;
    private EditText evName, evTicket, evCount;
    private TextView evDate, evTime, evExpDate, evExpTime;
    private final String[] evVals = new String[7];
    private String evSubPrice = "1 $";
    private String evSubPeriod = "mois";
    private String evSubEmail = "";
    private Typeface evTitleFont, evButtonFont, evIconFont;
    private String evGlyphImage = "", evGlyphQr = "", evGlyphLock = "";
    private String evGlyphCheck = "", evGlyphPlay = "", evGlyphInfo = "";

    private int evBlack() { return Color.rgb(0, 0, 0); }
    private int evCard() { return Color.argb(0xED, 0x1A, 0x1A, 0x1B); }
    private int evGray() { return Color.rgb(0x9E, 0x9E, 0x9E); }
    private float evRadius() { return dpToPx(14); }

    private GradientDrawable evBg(int fill, int strokeDp, int strokeColor, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        g.setStroke(dp(strokeDp), strokeColor);
        return g;
    }

    private GradientDrawable evDash(int fill, int strokeColor, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        g.setStroke(dp(1), strokeColor, dp(4), dp(3));
        return g;
    }

    private View evDashLine() {
        View v = new View(context);
        v.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.LINE);
        g.setStroke(dp(1), evGray(), dp(4), dp(3));
        v.setBackground(g);
        return v;
    }

    private LinearLayout.LayoutParams evLp(float wPct, float hPct, float topPct) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                wp(wPct), hPct <= 0 ? LinearLayout.LayoutParams.WRAP_CONTENT : hp(hPct));
        lp.topMargin = hp(topPct);
        return lp;
    }

    private TextView evBig(String s, int sp) {
        TextView t = text(s, sp, Color.WHITE, true);
        if (evTitleFont != null) t.setTypeface(evTitleFont);
        return t;
    }

    private TextView evIcon(String glyph, String fallback, int sp, int color) {
        TextView t = new TextView(context);
        boolean has = glyph != null && !glyph.isEmpty();
        String g = has ? glyph : fallback;
        t.setText(g);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setGravity(Gravity.CENTER);
        if (has && evIconFont != null) t.setTypeface(evIconFont);
        if (g.isEmpty()) t.setVisibility(View.GONE);
        return t;
    }

    // Titre (13, blanc) + pastille Premium facultative
    private void evTitleRow(LinearLayout col, String title, String sub, boolean pill) {
        LinearLayout r = new LinearLayout(context);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setLayoutParams(evLp(93f, 0f, 2f));
        TextView t = text(title, 13, Color.WHITE, true);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(t);
        if (pill) {
            LinearLayout p = new LinearLayout(context);
            p.setOrientation(LinearLayout.HORIZONTAL);
            p.setGravity(Gravity.CENTER_VERTICAL);
            p.setBackground(evBg(Color.TRANSPARENT, 1, evGray(), dpToPx(30)));
            p.setPadding(dp(10), dp(4), dp(12), dp(4));
            TextView lk = evIcon(evGlyphLock, "", 14, Color.WHITE);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            llp.rightMargin = dp(5);
            lk.setLayoutParams(llp);
            p.addView(lk);
            p.addView(text("Premium", 12, Color.WHITE, true));
            r.addView(p);
        }
        col.addView(r);
        TextView s = text(sub, 12, evGray(), false);
        s.setLayoutParams(evLp(93f, 0f, 0.4f));
        col.addView(s);
    }

    private void evLock(FrameLayout box) {
        View o = new View(context);
        o.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnUpgradeClick();
            }
        });
        box.addView(o, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private EditText evEdit(String hint, int inputType, boolean locked, LinearLayout parent, String initial) {
        final FrameLayout box = new FrameLayout(context);
        final GradientDrawable bg = locked ? evDash(evCard(), evGray(), evRadius())
                : evBg(evCard(), 1, evGray(), evRadius());
        box.setBackground(bg);
        box.setLayoutParams(evLp(93f, 8f, 1.2f));
        final EditText e = new EditText(context);
        e.setBackground(null);
        e.setTextSize(13);
        e.setTextColor(Color.WHITE);
        android.text.SpannableString ss = new android.text.SpannableString(hint);
        ss.setSpan(new android.text.style.AbsoluteSizeSpan(12, true), 0, hint.length(),
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        e.setHint(ss);
        e.setHintTextColor(evGray());
        Typeface f = shopFont();
        e.setTypeface(f != null ? f : Typeface.DEFAULT);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setPadding(wp(5f), 0, wp(5f), 0);
        e.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        if (initial != null) e.setText(initial);
        box.addView(e, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        if (locked) {
            e.setEnabled(false);
            e.setFocusable(false);
            evLock(box);
        } else {
            e.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) bg.setStroke(dp(2), Color.rgb(0xEE, 0xEE, 0xEE));
                    else bg.setStroke(dp(1), evGray());
                }
            });
        }
        parent.addView(box);
        return e;
    }

    private TextView evPicker(final String hint, final boolean isTime, boolean locked,
                              LinearLayout parent, float wPct, float leftPct, String initial) {
        final FrameLayout box = new FrameLayout(context);
        box.setBackground(locked ? evDash(evCard(), evGray(), evRadius())
                : evBg(evCard(), 1, evGray(), evRadius()));
        boolean horiz = parent.getOrientation() == LinearLayout.HORIZONTAL;
        LinearLayout.LayoutParams lp = evLp(wPct, 8f, horiz ? 0f : 1.2f);
        lp.leftMargin = wp(leftPct);
        box.setLayoutParams(lp);
        final TextView t = text(hint, 12, evGray(), false);
        t.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        t.setPadding(wp(5f), 0, wp(3f), 0);
        box.addView(t, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        if (initial != null && !initial.isEmpty()) {
            t.setText(initial);
            t.setTextSize(13);
            t.setTextColor(Color.WHITE);
            t.setTag(initial);
        }
        if (locked) {
            evLock(box);
        } else {
            box.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    java.util.Calendar c = java.util.Calendar.getInstance();
                    if (isTime) {
                        new android.app.TimePickerDialog(activity,
                                new android.app.TimePickerDialog.OnTimeSetListener() {
                                    @Override
                                    public void onTimeSet(android.widget.TimePicker p, int h, int m) {
                                        evSetPicked(t, String.format(java.util.Locale.US, "%02d:%02d", h, m));
                                    }
                                }, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), true).show();
                    } else {
                        new android.app.DatePickerDialog(activity,
                                new android.app.DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(android.widget.DatePicker p, int y, int m, int d) {
                                        evSetPicked(t, String.format(java.util.Locale.US, "%02d/%02d/%04d", d, m + 1, y));
                                    }
                                }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH),
                                c.get(java.util.Calendar.DAY_OF_MONTH)).show();
                    }
                }
            });
        }
        parent.addView(box);
        return t;
    }

    private void evSetPicked(TextView t, String s) {
        t.setText(s);
        t.setTextSize(13);
        t.setTextColor(Color.WHITE);
        t.setTag(s);
    }

    // Bouton blanc, texte noir (police des boutons chargeable avec LoadEventFonts)
    private TextView evButton(String label, float hPct, float wPct, int sp, boolean bold, float topPct) {
        TextView b = text(label, sp, Color.BLACK, bold);
        if (evButtonFont != null) b.setTypeface(evButtonFont);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setBackground(evBg(Color.WHITE, 0, Color.WHITE, evRadius()));
        b.setLayoutParams(evLp(wPct, hPct, topPct));
        return b;
    }

    private String evVal(TextView t) {
        return t == null || t.getTag() == null ? "" : t.getTag().toString();
    }

    private void evSave() {
        if (evName != null) evVals[0] = evName.getText().toString();
        if (evTicket != null) evVals[1] = evTicket.getText().toString();
        if (evCount != null) evVals[2] = evCount.getText().toString();
        if (evDate != null) evVals[3] = evVal(evDate);
        if (evTime != null) evVals[4] = evVal(evTime);
        if (evExpDate != null) evVals[5] = evVal(evExpDate);
        if (evExpTime != null) evVals[6] = evVal(evExpTime);
    }

    private void evRender() {
        if (evContainer == null) return;
        evAutoPan();
        if (!evSubscribeMode) evSave();
        evContainer.removeAllViews();
        evApplyDir(evContainer);
        evContainer.setBackgroundColor(evBlack());
        ScrollView sv = new ScrollView(context);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(0, hp(2f), 0, hp(4f));
        sv.addView(col, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        evContainer.addView(sv, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (evSubscribeMode) evRenderSubscribe(col);
        else evRenderForm(col);
    }

    private void evRenderForm(LinearLayout col) {
        // Bandeau : 11 % x 93 %, bouton Premium 8 % x 40 %
        LinearLayout banner = new LinearLayout(context);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setBackground(evBg(evCard(), 1, evGray(), evRadius()));
        banner.setPadding(wp(4f), 0, wp(3f), 0);
        banner.setLayoutParams(evLp(93f, 11f, 0f));
        if (evPremium) {
            TextView ck = evIcon(evGlyphCheck, "✓", 24, Color.WHITE);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = wp(3f);
            ck.setLayoutParams(clp);
            banner.addView(ck);
        }
        LinearLayout left = new LinearLayout(context);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        left.addView(text(evPremium ? evT("premium_on") : evT("free_offer"), 13, Color.WHITE, true));
        left.addView(text(evPremium ? evT("premium_sub")
                : evT("free_sub"), 12, evGray(), false));
        banner.addView(left);
        if (!evPremium) {
            TextView up = evButton(evT("premium_btn"), 8f, 40f, 13, false, 0f);
            up.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OnUpgradeClick();
                }
            });
            banner.addView(up);
        }
        col.addView(banner);

        // Carte image du ticket : 30 % x 93 %
        final int cardH = hp(30f);
        final int pad = dp(10);
        FrameLayout img = new FrameLayout(context);
        img.setBackground(evBg(evCard(), 1, evGray(), evRadius()));
        img.setLayoutParams(evLp(93f, 30f, 2f));
        LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(pad, pad, pad, pad);
        FrameLayout dz = new FrameLayout(context);
        dz.setBackground(evDash(Color.TRANSPARENT, evGray(), evRadius()));
        dz.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 3f));
        evPreview = new ImageView(context);
        evPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        evPreview.setVisibility(View.GONE);
        evPreview.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), evRadius());
            }
        });
        evPreview.setClipToOutline(true);
        dz.addView(evPreview, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        evPickHint = new LinearLayout(context);
        evPickHint.setOrientation(LinearLayout.VERTICAL);
        evPickHint.setGravity(Gravity.CENTER);
        evPickHint.addView(evIcon(evGlyphImage, "", 30, Color.WHITE));
        TextView h1 = text(evT("add_image"), 13, Color.WHITE, false);
        TextView h2 = text(evT("png_jpg"), 12, evGray(), false);
        h1.setGravity(Gravity.CENTER);
        h2.setGravity(Gravity.CENTER);
        evPickHint.addView(h1);
        evPickHint.addView(h2);
        dz.addView(evPickHint, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        inner.addView(dz);
        View sep = evDashLine();
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(2));
        slp.topMargin = dp(8);
        slp.bottomMargin = dp(8);
        inner.addView(sep, slp);
        LinearLayout bottom = new LinearLayout(context);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(4), 0, dp(4), 0);
        bottom.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.2f));
        LinearLayout bl = new LinearLayout(context);
        bl.setOrientation(LinearLayout.VERTICAL);
        bl.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView media = text(evT("media"), 12, evGray(), true);
        media.setLetterSpacing(0.15f);
        bl.addView(media);
        bl.addView(text(evT("media_sub"), 12, evGray(), false));
        bottom.addView(bl);
        bottom.addView(evIcon(evGlyphQr, "", 34, Color.WHITE));
        inner.addView(bottom);
        img.addView(inner, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        // encoches rondes de chaque côté du pointillé
        float avail = cardH - 2 * pad - dp(2) - dp(16);
        int sepY = pad + (int) (avail * 3f / 4.2f) + dp(8) + dp(1);
        for (int k = 0; k < 2; k++) {
            View n = new View(context);
            GradientDrawable ng = new GradientDrawable();
            ng.setShape(GradientDrawable.OVAL);
            ng.setColor(evBlack());
            n.setBackground(ng);
            FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(dp(18), dp(18));
            nlp.gravity = (k == 0 ? Gravity.START : Gravity.END) | Gravity.TOP;
            nlp.topMargin = sepY - dp(9);
            if (k == 0) nlp.leftMargin = -dp(9);
            else nlp.rightMargin = -dp(9);
            img.addView(n, nlp);
        }
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTarget = 1;
                OpenPhotoPicker();
            }
        });
        col.addView(img);
        if (evImageUri != null && !evImageUri.isEmpty()) evShowImage(evImageUri);

        evTitleRow(col, evT("ev_name"), evT("ev_name_sub"), false);
        evName = evEdit(evT("ev_name_hint"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false, col, evVals[0]);

        evTitleRow(col, evT("tk_name"), evT("tk_name_sub"), !evPremium);
        evTicket = evEdit(evPremium ? evT("tk_name_hint") : evT("premium_only"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, !evPremium, col, evVals[1]);

        evTitleRow(col, evT("tk_count"), evT("tk_count_sub"), false);
        evCount = evEdit(evT("tk_count_hint"), InputType.TYPE_CLASS_NUMBER, false, col, evVals[2]);

        evTitleRow(col, evT("ev_date"), evT("ev_date_sub"), false);
        LinearLayout row1 = new LinearLayout(context);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setLayoutParams(evLp(93f, 8f, 1.2f));
        evDate = evPicker(evT("date_hint"), false, false, row1, 55f, 0f, evVals[3]);
        evTime = evPicker(evT("time_hint"), true, false, row1, 36.5f, 1.5f, evVals[4]);
        col.addView(row1);

        evTitleRow(col, evT("exp"), evT("exp_sub"), !evPremium);
        LinearLayout row2 = new LinearLayout(context);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setLayoutParams(evLp(93f, 8f, 1.2f));
        evExpDate = evPicker(evPremium ? evT("date_hint") : evT("premium_only"), false, !evPremium,
                row2, 55f, 0f, evVals[5]);
        evExpTime = evPicker(evT("time_hint"), true, !evPremium, row2, 36.5f, 1.5f, evVals[6]);
        col.addView(row2);

        TextView create = evButton(evT("create"), 7f, 93f, 16, true, 4f);
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                evCreate();
            }
        });
        col.addView(create);
        TextView cap = text(evT("create_sub"), 12, evGray(), false);
        cap.setGravity(Gravity.CENTER);
        cap.setLayoutParams(evLp(93f, 0f, 1f));
        col.addView(cap);
    }

    private void evShowImage(String uri) {
        if (evPreview == null) return;
        try {
            evPreview.setImageURI(Uri.parse(uri));
            evPreview.setVisibility(View.VISIBLE);
            if (evPickHint != null) evPickHint.setVisibility(View.GONE);
        } catch (Exception e) {
            OnError("Image du billet : " + e.getMessage());
        }
    }

    // Appelé par resultReturned quand l'image du billet est choisie
    private void eventImagePicked(final String uri) {
        evImageUri = uri;
        if (evContainer == null || evSubscribeMode) return;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                evShowImage(uri);
            }
        });
    }

    private void evCreate() {
        evSave();
        String name = evVals[0] == null ? "" : evVals[0].trim();
        String ticket = evVals[1] == null ? "" : evVals[1].trim();
        int n = 0;
        try {
            n = Integer.parseInt(evVals[2] == null ? "" : evVals[2].trim());
        } catch (Exception ignored) {
        }
        if (evImageUri == null || evImageUri.isEmpty()) {
            OnEventFormInvalid("image", evT("err_image"));
        } else if (name.isEmpty()) {
            OnEventFormInvalid("name", evT("err_name"));
        } else if (evPremium && ticket.isEmpty()) {
            OnEventFormInvalid("ticketName", evT("err_ticket"));
        } else if (n < 1) {
            OnEventFormInvalid("count", evT("err_count"));
        } else if (evVals[3] == null || evVals[3].isEmpty()) {
            OnEventFormInvalid("date", evT("err_date"));
        } else if (evVals[4] == null || evVals[4].isEmpty()) {
            OnEventFormInvalid("time", evT("err_time"));
        } else {
            OnEventCreate(GetEventFormJson());
        }
    }

    private void evRenderSubscribe(LinearLayout col) {
        TextView t1 = evBig(evT("sub_title"), 18);
        t1.setLayoutParams(evLp(93f, 0f, 0f));
        col.addView(t1);
        TextView t2 = text(evT("sub_sub"), 12, evGray(), false);
        t2.setLayoutParams(evLp(93f, 0f, 0.5f));
        col.addView(t2);
        String[] perks = {evT("perk1"), evT("perk2"), evT("perk3")};
        for (String p : perks) {
            LinearLayout pr = new LinearLayout(context);
            pr.setOrientation(LinearLayout.HORIZONTAL);
            pr.setGravity(Gravity.CENTER_VERTICAL);
            pr.setLayoutParams(evLp(93f, 0f, 1f));
            TextView ck = evIcon(evGlyphCheck, "✓", 22, Color.WHITE);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = wp(3f);
            ck.setLayoutParams(clp);
            pr.addView(ck);
            pr.addView(text(p, 13, Color.WHITE, false));
            col.addView(pr);
        }

        // Carte SEVEN PREMIUM : 11 % x 93 %
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(evBg(Color.TRANSPARENT, 1, Color.WHITE, evRadius()));
        card.setPadding(wp(5f), 0, wp(5f), 0);
        card.setLayoutParams(evLp(93f, 10f, 2f));
        LinearLayout cl = new LinearLayout(context);
        cl.setOrientation(LinearLayout.VERTICAL);
        cl.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView sp = text("SEVEN PREMIUM", 12, evGray(), true);
        sp.setLetterSpacing(0.15f);
        cl.addView(sp);
        cl.addView(text(evT("subscription"), 12, evGray(), false));
        card.addView(cl);
        LinearLayout cr = new LinearLayout(context);
        cr.setOrientation(LinearLayout.VERTICAL);
        cr.setGravity(Gravity.END);
        cr.addView(evBig(evSubPrice, 18));
        cr.addView(text(evPer(), 12, evGray(), false));
        card.addView(cr);
        col.addView(card);

        TextView pm = text(evT("pay_method"), 13, Color.WHITE, true);
        pm.setLayoutParams(evLp(93f, 0f, 2f));
        col.addView(pm);
        LinearLayout gp = new LinearLayout(context);
        gp.setOrientation(LinearLayout.HORIZONTAL);
        gp.setGravity(Gravity.CENTER);
        gp.setBackground(evBg(Color.BLACK, 2, Color.WHITE, evRadius()));
        LinearLayout.LayoutParams glp = evLp(40f, 8f, 1.2f);
        glp.gravity = Gravity.START;
        glp.leftMargin = wp(3.5f);
        gp.setLayoutParams(glp);
        TextView pl = evIcon(evGlyphPlay, "▶", 18, Color.WHITE);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        plp.rightMargin = wp(2.5f);
        pl.setLayoutParams(plp);
        gp.addView(pl);
        gp.addView(text("Google Play", 13, Color.WHITE, false));
        col.addView(gp);

        // Récapitulatif : 35 % x 93 %
        LinearLayout rec = new LinearLayout(context);
        rec.setOrientation(LinearLayout.VERTICAL);
        rec.setBackground(evBg(evCard(), 1, evGray(), evRadius()));
        rec.setPadding(wp(5f), hp(2f), wp(5f), hp(2f));
        rec.setLayoutParams(evLp(93f, 36f, 2f));
        rec.addView(text(evT("recap"), 13, Color.WHITE, true));
        evRow(rec, evT("subscription"), "Seven Premium");
        evRow(rec, evT("price"), evSubPrice);
        evRow(rec, evT("renewal"), evRenew());
        evRow(rec, evT("google_account"), evSubEmail);
        View line = evDashLine();
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(2));
        dlp.topMargin = hp(2f);
        rec.addView(line, dlp);
        LinearLayout info = new LinearLayout(context);
        info.setOrientation(LinearLayout.HORIZONTAL);
        info.setLayoutParams(evLp(93f, 0f, 2f));
        TextView ii = evIcon(evGlyphInfo, "i", 14, evGray());
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ilp.rightMargin = wp(3f);
        ilp.topMargin = dp(2);
        ii.setLayoutParams(ilp);
        info.addView(ii);
        TextView fine = text(evT("cancel_info"),
                12, evGray(), false);
        fine.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        info.addView(fine);
        rec.addView(info);
        col.addView(rec);

        TextView sub = evButton(evT("subscribe_btn").replace("%s", evSubPrice), 7f, 93f, 16, true, 5f);
        sub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnSubscribeClick();
            }
        });
        col.addView(sub);
        TextView cap = text(evT("secure_pay"), 12, evGray(), false);
        cap.setGravity(Gravity.CENTER);
        cap.setLayoutParams(evLp(93f, 0f, 1f));
        col.addView(cap);
    }

    private void evRow(LinearLayout col, String k, String v) {
        LinearLayout r = new LinearLayout(context);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) r.getLayoutParams()).topMargin = hp(1f);
        TextView a = text(k, 12, evGray(), false);
        a.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView b = text(v == null ? "" : v, 13, Color.WHITE, true);
        b.setGravity(Gravity.END);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(a);
        r.addView(b);
        col.addView(r);
    }

    private void evRebuildOnUi() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    evRender();
                } catch (Exception e) {
                    OnError("Seven : " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Construit l'écran « Ajouter un événement » (design Seven) dans l'arrangement donné. premium = faux : écran Gratuit (nom du ticket et expiration verrouillés) ; vrai : écran Premium.")
    public void BuildEventForm(final AndroidViewComponent container, final boolean premium) {
        if (container == null || container.getView() == null) {
            OnError("BuildEventForm: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup target = realLayout(container);
                    if (target == null) {
                        OnError("BuildEventForm: conteneur invalide.");
                        return;
                    }
                    evContainer = target;
                    evPremium = premium;
                    evSubscribeMode = false;
                    evRender();
                } catch (Exception e) {
                    OnError("BuildEventForm: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Passe l'écran « Ajouter un événement » en Premium (vrai) ou Gratuit (faux) sans perdre ce qui est saisi.")
    public void SetEventPremium(boolean premium) {
        evPremium = premium;
        evRebuildOnUi();
    }

    @SimpleFunction(description = "Affiche l'écran d'abonnement Google Play dans l'arrangement donné (prix ex. « 1 $ », période ex. « mois », e-mail du compte Google).")
    public void BuildSubscribeScreen(final AndroidViewComponent container, String price, String period, String email) {
        if (container == null || container.getView() == null) {
            OnError("BuildSubscribeScreen: conteneur invalide.");
            return;
        }
        evSubPrice = price == null || price.isEmpty() ? "1 $" : price;
        evSubPeriod = period == null || period.isEmpty() ? "mois" : period;
        evSubEmail = email == null ? "" : email;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup target = realLayout(container);
                    if (target == null) {
                        OnError("BuildSubscribeScreen: conteneur invalide.");
                        return;
                    }
                    evContainer = target;
                    evSubscribeMode = true;
                    evRender();
                } catch (Exception e) {
                    OnError("BuildSubscribeScreen: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Charge les polices des écrans Seven : titlesFont pour les grands titres (18), buttonsFont pour les boutons. Chemin d'un fichier .ttf/.otf des Assets ; vide = police normale (LoadCustomFont / LoadCustomBoldFont).")
    public void LoadEventFonts(String titlesFont, String buttonsFont) {
        evTitleFont = titlesFont == null || titlesFont.trim().isEmpty() ? null : tryLoadTypeface(titlesFont.trim());
        evButtonFont = buttonsFont == null || buttonsFont.trim().isEmpty() ? null : tryLoadTypeface(buttonsFont.trim());
        if (evTitleFont == null && titlesFont != null && !titlesFont.trim().isEmpty())
            OnError("LoadEventFonts: police des titres introuvable (" + titlesFont + ").");
        if (evButtonFont == null && buttonsFont != null && !buttonsFont.trim().isEmpty())
            OnError("LoadEventFonts: police des boutons introuvable (" + buttonsFont + ").");
        evRebuildOnUi();
    }

    @SimpleFunction(description = "Charge la police d'icônes (ex. Phosphor .ttf des Assets) utilisée par les écrans Seven.")
    public void SetEventIconFont(String fontPath) {
        evIconFont = fontPath == null || fontPath.trim().isEmpty() ? null : tryLoadTypeface(fontPath.trim());
        if (evIconFont == null && fontPath != null && !fontPath.trim().isEmpty())
            OnError("SetEventIconFont: police introuvable (" + fontPath + ").");
        evRebuildOnUi();
    }

    @SimpleFunction(description = "Définit les icônes (le caractère Phosphor de chaque icône) : image du billet, QR code, cadenas, coche, lecture Google Play, information.")
    public void SetEventIcons(String imageGlyph, String qrGlyph, String lockGlyph, String checkGlyph,
                              String playGlyph, String infoGlyph) {
        evGlyphImage = imageGlyph == null ? "" : imageGlyph;
        evGlyphQr = qrGlyph == null ? "" : qrGlyph;
        evGlyphLock = lockGlyph == null ? "" : lockGlyph;
        evGlyphCheck = checkGlyph == null ? "" : checkGlyph;
        evGlyphPlay = playGlyph == null ? "" : playGlyph;
        evGlyphInfo = infoGlyph == null ? "" : infoGlyph;
        evRebuildOnUi();
    }

    @SimpleFunction(description = "Retourne les valeurs saisies (image, nom, nom du ticket, nombre, date, heure, expiration, premium) au format JSON.")
    public String GetEventFormJson() {
        evSave();
        try {
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("image", evImageUri == null ? "" : evImageUri);
            o.put("name", evVals[0] == null ? "" : evVals[0].trim());
            o.put("ticketName", evVals[1] == null ? "" : evVals[1].trim());
            o.put("count", evVals[2] == null ? "" : evVals[2].trim());
            o.put("date", evVals[3] == null ? "" : evVals[3]);
            o.put("time", evVals[4] == null ? "" : evVals[4]);
            String ed = evVals[5] == null ? "" : evVals[5];
            String et = evVals[6] == null ? "" : evVals[6];
            o.put("expiry", et.isEmpty() ? ed : (ed + " " + et).trim());
            o.put("premium", evPremium);
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @SimpleFunction(description = "Vide l'écran « Ajouter un événement » (image et champs).")
    public void ClearEventForm() {
        evImageUri = "";
        for (int i = 0; i < evVals.length; i++) evVals[i] = "";
        evName = null;
        evTicket = null;
        evCount = null;
        evDate = null;
        evTime = null;
        evExpDate = null;
        evExpTime = null;
        if (!evSubscribeMode) evRebuildOnUi();
    }

    @SimpleEvent(description = "Déclenché quand l'image du billet est choisie dans l'écran « Ajouter un événement » (n'envoie rien au chat : OnPhotoPicked reste réservé au chat).")
    public void OnEventImagePicked(String imageUri) {
        EventDispatcher.dispatchEvent(this, "OnEventImagePicked", imageUri);
    }

    @SimpleEvent(description = "Déclenché quand le formulaire est valide et que l'utilisateur appuie sur « Créer l'événement ». eventJson contient les valeurs saisies.")
    public void OnEventCreate(String eventJson) {
        EventDispatcher.dispatchEvent(this, "OnEventCreate", eventJson);
    }

    @SimpleEvent(description = "Déclenché quand un champ est manquant ou invalide (field : image, name, ticketName, count, date, time).")
    public void OnEventFormInvalid(String field, String message) {
        EventDispatcher.dispatchEvent(this, "OnEventFormInvalid", field, message);
    }

    @SimpleEvent(description = "Déclenché sur « Passer à Premium » ou sur un champ verrouillé de l'offre gratuite.")
    public void OnUpgradeClick() {
        EventDispatcher.dispatchEvent(this, "OnUpgradeClick");
    }

    @SimpleEvent(description = "Déclenché sur le bouton « S'abonner » de l'écran Google Play.")
    public void OnSubscribeClick() {
        EventDispatcher.dispatchEvent(this, "OnSubscribeClick");
    }

    // =========================================================================
    // SEVEN : ACCUEIL (bouton + billets), PAGES RÉGLAGES, LANGUE, SON DU SCAN
    // =========================================================================
    private String addEvOffFont = "";
    private String addEvOffChar = "";
    private String addEvOffMsg = "";
    private int addEvOffColor = Color.WHITE;
    private String addEvOffPos = "top";
    private float addEvOffOffset = 5f;
    private int addEvOffMs = 2500;
    private boolean addEvBusy = false;

    private boolean evHasInternet() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                android.net.Network n = cm.getActiveNetwork();
                if (n == null) return false;
                android.net.NetworkCapabilities c = cm.getNetworkCapabilities(n);
                return c != null
                        && c.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && c.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private View evDashLineC(int color) {
        View v = new View(context);
        v.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.LINE);
        g.setStroke(dp(2), color, dp(6), dp(4));
        v.setBackground(g);
        return v;
    }

    @SimpleFunction(description = "Règle la notification affichée par le bouton « Ajouter un événement » quand il n'y a pas d'internet : police d'icônes, caractère de l'icône, couleur de l'icône, message, position (top ou bottom), décalage en %, durée en ms.")
    public void SetAddEventOfflineNotice(String fontPath, String unicodeChar, int iconColor, String message,
                                         String position, float offsetPercent, int durationMs) {
        addEvOffFont = fontPath == null ? "" : fontPath;
        addEvOffChar = unicodeChar == null ? "" : unicodeChar;
        addEvOffColor = iconColor;
        if (message != null && !message.isEmpty()) addEvOffMsg = message;
        if (position != null && !position.isEmpty()) addEvOffPos = position;
        addEvOffOffset = offsetPercent;
        addEvOffMs = durationMs <= 0 ? 2500 : durationMs;
    }

    @SimpleFunction(description = "Construit le bouton « Ajouter un événement » (7 % x 95 %) dans l'arrangement donné. label vide = texte traduit automatiquement. iconGlyph = caractère Phosphor (ex. circle-dashed). Au clic : cercle de progression, vérification d'internet, puis OnAddEventClick (connecté) ou notification + OnAddEventOffline (pas de connexion).")
    public void BuildAddEventButton(final AndroidViewComponent container, final String label, final String iconGlyph) {
        if (container == null || container.getView() == null) {
            OnError("BuildAddEventButton: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup target = realLayout(container);
                    if (target == null) {
                        OnError("BuildAddEventButton: conteneur invalide.");
                        return;
                    }
                    addEvTarget = target;
                    addEvLabel = label == null ? "" : label;
                    addEvGlyph = iconGlyph == null ? "" : iconGlyph;
                    evBuildAddButton();
                } catch (Exception e) {
                    OnError("BuildAddEventButton: " + e.getMessage());
                }
            }
        });
    }

    private void evBuildAddButton() {
        if (addEvTarget == null) return;
        ViewGroup target = addEvTarget;
        target.removeAllViews();
        evApplyDir(target);
        if (target instanceof LinearLayout) ((LinearLayout) target).setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout btn = new LinearLayout(context);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(evBg(evCard(), 0, evCard(), evRadius()));
        FrameLayout holder = new FrameLayout(context);
        final TextView ic = evIcon(addEvGlyph, "+", 24, Color.WHITE);
        final android.widget.ProgressBar pb = new android.widget.ProgressBar(context);
        pb.setIndeterminate(true);
        pb.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        pb.setVisibility(View.GONE);
        holder.addView(ic, new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
        holder.addView(pb, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(28), dp(28));
        hlp.setMarginEnd(wp(3f));
        btn.addView(holder, hlp);
        btn.addView(text(addEvLabel.isEmpty() ? evT("add_event") : addEvLabel, 16, Color.WHITE, true));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEvClicked(ic, pb);
            }
        });
        if (target instanceof LinearLayout) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(wp(95f), hp(7f));
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            target.addView(btn, lp);
        } else {
            target.addView(btn, new ViewGroup.LayoutParams(wp(95f), hp(7f)));
        }
    }

    private void addEvClicked(final TextView ic, final android.widget.ProgressBar pb) {
        if (addEvBusy) return;
        addEvBusy = true;
        ic.setVisibility(View.INVISIBLE);
        pb.setVisibility(View.VISIBLE);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean ok = evHasInternet();
                pb.setVisibility(View.GONE);
                ic.setVisibility(View.VISIBLE);
                addEvBusy = false;
                if (ok) {
                    OnAddEventClick();
                } else {
                    String m = addEvOffMsg.isEmpty() ? evT("offline") : addEvOffMsg;
                    ShowNotifier(addEvOffFont, addEvOffChar, addEvOffColor, m,
                            addEvOffPos, addEvOffOffset, addEvOffMs);
                    OnAddEventOffline(m);
                }
            }
        }, 600);
    }

    @SimpleEvent(description = "Déclenché quand l'utilisateur appuie sur « Ajouter un événement » et qu'internet est disponible : ouvre ici l'arrangement concerné.")
    public void OnAddEventClick() {
        EventDispatcher.dispatchEvent(this, "OnAddEventClick");
    }

    @SimpleEvent(description = "Déclenché quand l'utilisateur appuie sur « Ajouter un événement » sans connexion internet (la notification est déjà affichée).")
    public void OnAddEventOffline(String message) {
        EventDispatcher.dispatchEvent(this, "OnAddEventOffline", message);
    }

    // ---------------------------- Billets blancs -----------------------------
    private static class EvTicketViews {
        TextView count;
        ImageView barcode;
    }

    private final java.util.HashMap<String, EvTicketViews> evTickets =
            new java.util.HashMap<String, EvTicketViews>();

    @SimpleFunction(description = "Ajoute un billet blanc (37 % x 80 %) dans l'arrangement donné : image, pointillé, nom (16), date et heure (14), distribués/total (14, ex. 99/100) et un espace réservé au code-barres (voir SetTicketBarcode). À appeler une fois par événement enregistré.")
    public void AddTicketCard(final AndroidViewComponent container, final String ticketId, final String imageUri,
                              final String name, final String date, final String time,
                              final int distributed, final int total) {
        if (container == null || container.getView() == null) {
            OnError("AddTicketCard: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup target = realLayout(container);
                    if (target == null) {
                        OnError("AddTicketCard: conteneur invalide.");
                        return;
                    }
                    final int cardH = hp(37f);
                    final int pad = dp(8);
                    FrameLayout card = new FrameLayout(context);
                    card.setBackground(evBg(Color.WHITE, 0, Color.WHITE, dpToPx(16)));
                    LinearLayout inner = new LinearLayout(context);
                    inner.setOrientation(LinearLayout.VERTICAL);
                    inner.setPadding(pad, pad, pad, pad);

                    ImageView img = new ImageView(context);
                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    img.setBackgroundColor(Color.rgb(0xE8, 0xE8, 0xE8));
                    img.setOutlineProvider(new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, Outline outline) {
                            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx(8));
                        }
                    });
                    img.setClipToOutline(true);
                    inner.addView(img, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 0, 5f));
                    if (imageUri != null && !imageUri.isEmpty()) loadImageAsync(img, imageUri);

                    LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(2));
                    slp.topMargin = dp(8);
                    slp.bottomMargin = dp(8);
                    inner.addView(evDashLineC(Color.BLACK), slp);

                    LinearLayout info = new LinearLayout(context);
                    info.setOrientation(LinearLayout.HORIZONTAL);
                    info.setPadding(dp(6), 0, dp(6), 0);
                    info.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 0, 3.3f));
                    LinearLayout left = new LinearLayout(context);
                    left.setOrientation(LinearLayout.VERTICAL);
                    left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    TextView nm = text(name == null ? "" : name, 16, Color.BLACK, true);
                    nm.setSingleLine(true);
                    nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    left.addView(nm);
                    left.addView(text(date == null ? "" : date, 14, Color.BLACK, false));
                    left.addView(text(time == null ? "" : time, 14, Color.BLACK, false));
                    info.addView(left);
                    LinearLayout right = new LinearLayout(context);
                    right.setOrientation(LinearLayout.VERTICAL);
                    right.setGravity(Gravity.END);
                    ImageView bar = new ImageView(context);
                    bar.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    right.addView(bar, new LinearLayout.LayoutParams(wp(30f), hp(5f)));
                    TextView cnt = text(distributed + "/" + total, 14, Color.BLACK, true);
                    cnt.setGravity(Gravity.END);
                    right.addView(cnt);
                    info.addView(right);
                    inner.addView(info);
                    card.addView(inner, new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

                    // encoches rondes de chaque côté du pointillé
                    float avail = cardH - 2 * pad - dp(2) - dp(16);
                    int notchTop = pad + (int) (avail * 5f / 8.3f);
                    for (int k = 0; k < 2; k++) {
                        View n = new View(context);
                        GradientDrawable ng = new GradientDrawable();
                        ng.setShape(GradientDrawable.OVAL);
                        ng.setColor(evBlack());
                        n.setBackground(ng);
                        FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(dp(18), dp(18));
                        nlp.gravity = (k == 0 ? Gravity.START : Gravity.END) | Gravity.TOP;
                        nlp.topMargin = notchTop;
                        if (k == 0) nlp.leftMargin = -dp(9);
                        else nlp.rightMargin = -dp(9);
                        card.addView(n, nlp);
                    }
                    final String id = ticketId == null ? "" : ticketId;
                    card.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            OnTicketClick(id);
                        }
                    });
                    EvTicketViews tv = new EvTicketViews();
                    tv.count = cnt;
                    tv.barcode = bar;
                    evTickets.put(id, tv);

                    if (target instanceof LinearLayout) {
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(wp(80f), cardH);
                        lp.gravity = Gravity.CENTER_HORIZONTAL;
                        lp.topMargin = hp(2f);
                        target.addView(card, lp);
                    } else {
                        target.addView(card, new ViewGroup.LayoutParams(wp(80f), cardH));
                    }
                } catch (Exception e) {
                    OnError("AddTicketCard: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Affiche le code-barres (lien http, content:// ou chemin) dans l'espace réservé du billet ticketId.")
    public void SetTicketBarcode(final String ticketId, final String imageUri) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                EvTicketViews v = evTickets.get(ticketId);
                if (v != null && v.barcode != null && imageUri != null && !imageUri.isEmpty())
                    loadImageAsync(v.barcode, imageUri);
            }
        });
    }

    @SimpleFunction(description = "Met à jour le compteur « distribués/total » (ex. 99/100) du billet ticketId.")
    public void UpdateTicketCount(final String ticketId, final int distributed, final int total) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                EvTicketViews v = evTickets.get(ticketId);
                if (v != null && v.count != null) v.count.setText(distributed + "/" + total);
            }
        });
    }

    @SimpleFunction(description = "Supprime tous les billets affichés dans l'arrangement donné.")
    public void ClearTickets(final AndroidViewComponent container) {
        if (container == null || container.getView() == null) return;
        runOnUi(new Runnable() {
            @Override
            public void run() {
                ViewGroup target = realLayout(container);
                if (target != null) target.removeAllViews();
                evTickets.clear();
            }
        });
    }

    @SimpleEvent(description = "Déclenché quand l'utilisateur appuie sur un billet blanc.")
    public void OnTicketClick(String ticketId) {
        EventDispatcher.dispatchEvent(this, "OnTicketClick", ticketId);
    }

    // ------------------------ Pages réglages et langue -----------------------
    private LinearLayout evSheet(ViewGroup target, String title) {
        target.removeAllViews();
        evApplyDir(target);
        ScrollView sv = new ScrollView(context);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(wp(6f), 0, wp(6f), hp(3f));
        sv.addView(col, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        target.addView(sv, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // ligne horizontale (poignée) en haut
        View handle = new View(context);
        GradientDrawable hg = new GradientDrawable();
        hg.setColor(Color.rgb(0x6B, 0x6B, 0x6B));
        hg.setCornerRadius(dpToPx(3));
        handle.setBackground(hg);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(wp(13f), dp(4));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.topMargin = hp(1.5f);
        col.addView(handle, hlp);
        TextView t = text(title == null ? "" : title, 18, evGray(), true);
        t.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = hp(1f);
        col.addView(t, tlp);
        return col;
    }

    @SimpleFunction(description = "Construit une page de réglages (Sécurité, Notification, Profil…) avec uniquement des textes dans l'arrangement donné : ligne horizontale du haut, titre de page (18, gris), puis chaque ligne de itemsJson. itemsJson = liste JSON d'objets {\"id\":\"\",\"title\":\"\",\"description\":\"\",\"value\":\"\",\"valueColor\":\"gray\",\"icon\":\"\"} (seul title est obligatoire). Titres 18, descriptions 13 gris #9E9E9E. Un clic sur une ligne déclenche OnSettingsItemClick(id).")
    public void BuildSettingsPage(final AndroidViewComponent container, final String pageTitle, final String itemsJson) {
        if (container == null || container.getView() == null) {
            OnError("BuildSettingsPage: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup target = realLayout(container);
                    if (target == null) {
                        OnError("BuildSettingsPage: conteneur invalide.");
                        return;
                    }
                    LinearLayout col = evSheet(target, pageTitle);
                    org.json.JSONArray arr = new org.json.JSONArray(itemsJson);
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject o = arr.getJSONObject(i);
                        evSettingsRow(col, o.optString("id", ""), o.optString("title", ""),
                                o.optString("description", ""), o.optString("value", ""),
                                o.optString("valueColor", "white"), o.optString("icon", ""),
                                o.optBoolean("toggle", false));
                    }
                } catch (Exception e) {
                    OnError("BuildSettingsPage: " + e.getMessage());
                }
            }
        });
    }

    private void evSettingsRow(LinearLayout col, final String id, String title, String desc,
                               String value, String valueColor, String icon, boolean reserve) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = hp(3f);
        row.setLayoutParams(rlp);
        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        if (reserve) head.setPaddingRelative(0, 0, wp(18f), 0);
        boolean hasValue = value != null && !value.isEmpty();
        boolean iconNext = !hasValue && icon != null && !icon.isEmpty();
        TextView t = text(title, 18, Color.WHITE, true);
        t.setLayoutParams(hasValue || iconNext
                ? new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                : new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(t);
        if (hasValue) {
            TextView v = text(value, 18, "gray".equalsIgnoreCase(valueColor) ? evGray() : Color.WHITE, true);
            v.setGravity(Gravity.END);
            LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            vlp.leftMargin = wp(4f);
            head.addView(v, vlp);
        }
        if (icon != null && !icon.isEmpty()) {
            TextView ic = evIcon(icon, "", 22, Color.WHITE);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ilp.leftMargin = wp(3f);
            head.addView(ic, ilp);
        }
        row.addView(head);
        if (desc != null && !desc.isEmpty()) {
            TextView d = text(desc, 13, evGray(), false);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dlp.topMargin = hp(0.6f);
            row.addView(d, dlp);
        }
        if (id != null && !id.isEmpty()) {
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    evRowClicked(id);
                }
            });
        }
        col.addView(row);
    }

    @SimpleEvent(description = "Déclenché quand l'utilisateur appuie sur une ligne de BuildSettingsPage ayant un id.")
    public void OnSettingsItemClick(String id) {
        EventDispatcher.dispatchEvent(this, "OnSettingsItemClick", id);
    }

    // ------------------------------ Son du scan -------------------------------
    private String scanSoundOk = "";
    private String scanSoundFail = "";
    private boolean scanSoundEnabled = true;

    @SimpleFunction(description = "Définit les sons du scan : fichier du bip « billet valide » et fichier du bip « billet refusé » (Assets ou chemin).")
    public void SetScanSounds(String okSoundFile, String failSoundFile) {
        scanSoundOk = okSoundFile == null ? "" : okSoundFile.trim();
        scanSoundFail = failSoundFile == null ? "" : failSoundFile.trim();
    }

    @SimpleFunction(description = "Active ou désactive les sons de numérisation (réglage Notification).")
    public void SetScanSoundEnabled(boolean enabled) {
        scanSoundEnabled = enabled;
    }

    @SimpleFunction(description = "Joue le bip du scan : valide = vrai pour un billet accepté, faux pour un billet refusé ou déjà scanné. Ne joue rien si les sons sont désactivés.")
    public void PlayScanResult(boolean valid) {
        if (!scanSoundEnabled) return;
        String f = valid ? scanSoundOk : scanSoundFail;
        if (!f.isEmpty()) PlayCustomSound(f);
    }

    // =========================================================================
    // LANGUES (dans le code) : fr, en, es, ht, pt, ar, de, it, zh
    // Toutes les phrases des écrans Seven passent par evT("clé").
    // =========================================================================
    private static final String[] EV_LANGS = {"fr", "en", "es", "ht", "pt", "ar", "de", "it", "zh"};

    private static final String[][] EV_I18N = {
        {"premium_btn", "Passer à Premium", "Go Premium", "Pasar a Premium", "Pase Premium", "Mudar para Premium", "الترقية إلى بريميوم", "Auf Premium upgraden", "Passa a Premium", "升级到高级版"},
        {"premium_on", "Premium actif", "Premium active", "Premium activo", "Premium aktif", "Premium ativo", "بريميوم مفعّل", "Premium aktiv", "Premium attivo", "高级版已开通"},
        {"free_offer", "Offre gratuite", "Free plan", "Plan gratuito", "Ofri gratis", "Plano gratuito", "العرض المجاني", "Kostenloses Angebot", "Offerta gratuita", "免费版"},
        {"premium_sub", "Billets illimités · expiration personnalisable.", "Unlimited tickets · custom expiry.", "Entradas ilimitadas · caducidad personalizable.", "Tikè san limit · dat ekspirasyon pèsonalize.", "Ingressos ilimitados · validade personalizável.", "تذاكر غير محدودة · انتهاء صلاحية مخصص.", "Unbegrenzte Tickets · individuelles Ablaufdatum.", "Biglietti illimitati · scadenza personalizzabile.", "不限票数 · 自定义有效期。"},
        {"free_sub", "Vos billets restent 3 jours sur le serveur.", "Your tickets stay on the server for 3 days.", "Tus entradas permanecen 3 días en el servidor.", "Tikè w yo rete 3 jou sou sèvè a.", "Seus ingressos ficam 3 dias no servidor.", "تبقى تذاكرك على الخادم لمدة 3 أيام.", "Deine Tickets bleiben 3 Tage auf dem Server.", "I tuoi biglietti restano 3 giorni sul server.", "您的票券将在服务器上保留3天。"},
        {"add_image", "Ajouter l’image du billet", "Add the ticket image", "Añadir la imagen de la entrada", "Ajoute imaj tikè a", "Adicionar a imagem do ingresso", "إضافة صورة التذكرة", "Ticketbild hinzufügen", "Aggiungi l’immagine del biglietto", "添加票券图片"},
        {"png_jpg", "PNG ou JPG", "PNG or JPG", "PNG o JPG", "PNG oswa JPG", "PNG ou JPG", "PNG أو JPG", "PNG oder JPG", "PNG o JPG", "PNG 或 JPG"},
        {"media", "MÉDIA", "MEDIA", "MEDIOS", "MEDYA", "MÍDIA", "الوسائط", "MEDIEN", "MEDIA", "媒体"},
        {"media_sub", "Ajoutez le visuel officiel ou l’affiche de votre événement.", "Add the official visual or poster of your event.", "Añade el visual oficial o el cartel de tu evento.", "Ajoute vizyèl ofisyèl la oswa afich evènman w lan.", "Adicione o visual oficial ou o cartaz do seu evento.", "أضف الصورة الرسمية أو ملصق فعاليتك.", "Füge das offizielle Bild oder Plakat deiner Veranstaltung hinzu.", "Aggiungi l’immagine ufficiale o la locandina del tuo evento.", "添加活动的官方图片或海报。"},
        {"ev_name", "Nom de l’événement", "Event name", "Nombre del evento", "Non evènman an", "Nome do evento", "اسم الفعالية", "Name der Veranstaltung", "Nome dell’evento", "活动名称"},
        {"ev_name_sub", "Donnez un titre clair et accrocheur à votre événement.", "Give your event a clear, catchy title.", "Dale a tu evento un título claro y atractivo.", "Bay evènman w lan yon tit klè ki atire moun.", "Dê ao seu evento um título claro e atraente.", "اختر عنواناً واضحاً وجذاباً لفعاليتك.", "Gib deiner Veranstaltung einen klaren, ansprechenden Titel.", "Dai al tuo evento un titolo chiaro e accattivante.", "为您的活动起一个清晰、吸引人的标题。"},
        {"ev_name_hint", "Ex. Soirée Kompa Night", "E.g. Kompa Night Party", "Ej. Noche de Kompa", "Egz. Swaré Kompa Night", "Ex. Noite de Kompa", "مثال: ليلة كومبا", "z. B. Kompa-Nacht", "Es. Serata Kompa Night", "例如：Kompa 之夜"},
        {"tk_name", "Nom du ticket", "Ticket name", "Nombre de la entrada", "Non tikè a", "Nome do ingresso", "اسم التذكرة", "Ticketname", "Nome del biglietto", "票券名称"},
        {"tk_name_sub", "Nommez votre type de billet.", "Name your ticket type.", "Nombra tu tipo de entrada.", "Bay tip tikè w la yon non.", "Dê um nome ao seu tipo de ingresso.", "سمِّ نوع تذكرتك.", "Benenne deinen Tickettyp.", "Dai un nome al tuo tipo di biglietto.", "为您的票种命名。"},
        {"premium_only", "Disponible avec Premium", "Available with Premium", "Disponible con Premium", "Disponib ak Premium", "Disponível com Premium", "متاح مع بريميوم", "Mit Premium verfügbar", "Disponibile con Premium", "高级版可用"},
        {"tk_name_hint", "Ex. VIP, Standard", "E.g. VIP, Standard", "Ej. VIP, Estándar", "Egz. VIP, Estanda", "Ex. VIP, Padrão", "مثال: VIP، عادي", "z. B. VIP, Standard", "Es. VIP, Standard", "例如：VIP、普通"},
        {"tk_count", "Nombre de tickets", "Number of tickets", "Número de entradas", "Kantite tikè", "Número de ingressos", "عدد التذاكر", "Anzahl der Tickets", "Numero di biglietti", "票券数量"},
        {"tk_count_sub", "Ajoutez le nombre de tickets que vous souhaitez enregistrer.", "Enter the number of tickets you want to register.", "Añade el número de entradas que quieres registrar.", "Antre kantite tikè ou vle anrejistre yo.", "Informe o número de ingressos que deseja registrar.", "أدخل عدد التذاكر التي تريد تسجيلها.", "Gib die Anzahl der Tickets ein, die du registrieren möchtest.", "Inserisci il numero di biglietti che vuoi registrare.", "输入您要登记的票券数量。"},
        {"tk_count_hint", "Ex. 100", "E.g. 100", "Ej. 100", "Egz. 100", "Ex. 100", "مثال: 100", "z. B. 100", "Es. 100", "例如：100"},
        {"ev_date", "Date de l’événement", "Event date", "Fecha del evento", "Dat evènman an", "Data do evento", "تاريخ الفعالية", "Datum der Veranstaltung", "Data dell’evento", "活动日期"},
        {"ev_date_sub", "Indiquez le jour et l’heure du début de l’événement.", "Enter the day and start time of the event.", "Indica el día y la hora de inicio del evento.", "Make jou ak lè evènman an kòmanse.", "Informe o dia e a hora de início do evento.", "حدد يوم وساعة بدء الفعالية.", "Gib Tag und Startzeit der Veranstaltung an.", "Indica il giorno e l’ora di inizio dell’evento.", "请填写活动的日期和开始时间。"},
        {"date_hint", "JJ/MM/AAAA", "DD/MM/YYYY", "DD/MM/AAAA", "JJ/MM/AAAA", "DD/MM/AAAA", "يوم/شهر/سنة", "TT/MM/JJJJ", "GG/MM/AAAA", "日/月/年"},
        {"time_hint", "HH:MM", "HH:MM", "HH:MM", "HH:MM", "HH:MM", "ساعة:دقيقة", "HH:MM", "HH:MM", "时:分"},
        {"exp", "Expiration du billet", "Ticket expiry", "Caducidad de la entrada", "Ekspirasyon tikè a", "Validade do ingresso", "انتهاء صلاحية التذكرة", "Ablauf des Tickets", "Scadenza del biglietto", "票券有效期"},
        {"exp_sub", "Passé cette date, les billets ne seront plus valides pour le scan.", "After this date, tickets will no longer be valid for scanning.", "Pasada esta fecha, las entradas ya no serán válidas para el escaneo.", "Apre dat sa a, tikè yo p ap valab ankò pou eskane.", "Após esta data, os ingressos não serão mais válidos para leitura.", "بعد هذا التاريخ لن تكون التذاكر صالحة للمسح.", "Nach diesem Datum sind die Tickets für den Scan nicht mehr gültig.", "Dopo questa data i biglietti non saranno più validi per la scansione.", "过了此日期，票券将无法再扫描验证。"},
        {"create", "Créer l’événement", "Create event", "Crear evento", "Kreye evènman an", "Criar evento", "إنشاء الفعالية", "Veranstaltung erstellen", "Crea evento", "创建活动"},
        {"create_sub", "Un QR code dynamique et unique est attribué à chaque billet.", "A unique, dynamic QR code is assigned to each ticket.", "Se asigna un código QR dinámico y único a cada entrada.", "Chak tikè resevwa yon kòd QR inik ki chanje.", "Um código QR dinâmico e único é atribuído a cada ingresso.", "يُخصَّص رمز QR ديناميكي وفريد لكل تذكرة.", "Jedem Ticket wird ein einzigartiger, dynamischer QR-Code zugewiesen.", "A ogni biglietto viene assegnato un codice QR dinamico e unico.", "每张票券都会分配唯一的动态二维码。"},
        {"err_image", "Ajoutez l’image du billet.", "Add the ticket image.", "Añade la imagen de la entrada.", "Ajoute imaj tikè a.", "Adicione a imagem do ingresso.", "أضف صورة التذكرة.", "Füge das Ticketbild hinzu.", "Aggiungi l’immagine del biglietto.", "请添加票券图片。"},
        {"err_name", "Indiquez le nom de l’événement.", "Enter the event name.", "Indica el nombre del evento.", "Make non evènman an.", "Informe o nome do evento.", "أدخل اسم الفعالية.", "Gib den Namen der Veranstaltung ein.", "Inserisci il nome dell’evento.", "请输入活动名称。"},
        {"err_ticket", "Indiquez le nom du ticket.", "Enter the ticket name.", "Indica el nombre de la entrada.", "Make non tikè a.", "Informe o nome do ingresso.", "أدخل اسم التذكرة.", "Gib den Ticketnamen ein.", "Inserisci il nome del biglietto.", "请输入票券名称。"},
        {"err_count", "Indiquez le nombre de tickets.", "Enter the number of tickets.", "Indica el número de entradas.", "Make kantite tikè yo.", "Informe o número de ingressos.", "أدخل عدد التذاكر.", "Gib die Anzahl der Tickets ein.", "Inserisci il numero di biglietti.", "请输入票券数量。"},
        {"err_date", "Choisissez la date de l’événement.", "Choose the event date.", "Elige la fecha del evento.", "Chwazi dat evènman an.", "Escolha a data do evento.", "اختر تاريخ الفعالية.", "Wähle das Datum der Veranstaltung.", "Scegli la data dell’evento.", "请选择活动日期。"},
        {"err_time", "Choisissez l’heure de l’événement.", "Choose the event time.", "Elige la hora del evento.", "Chwazi lè evènman an.", "Escolha a hora do evento.", "اختر وقت الفعالية.", "Wähle die Uhrzeit der Veranstaltung.", "Scegli l’ora dell’evento.", "请选择活动时间。"},
        {"sub_title", "Des billets sans limite", "Unlimited tickets", "Entradas sin límite", "Tikè san limit", "Ingressos sem limite", "تذاكر بلا حدود", "Tickets ohne Limit", "Biglietti senza limiti", "不限量票券"},
        {"sub_sub", "Débloquez toutes les fonctions de Seven.", "Unlock all Seven features.", "Desbloquea todas las funciones de Seven.", "Debloke tout fonksyon Seven yo.", "Desbloqueie todos os recursos do Seven.", "افتح جميع ميزات Seven.", "Schalte alle Funktionen von Seven frei.", "Sblocca tutte le funzioni di Seven.", "解锁 Seven 的全部功能。"},
        {"perk1", "Billets illimités", "Unlimited tickets", "Entradas ilimitadas", "Tikè san limit", "Ingressos ilimitados", "تذاكر غير محدودة", "Unbegrenzte Tickets", "Biglietti illimitati", "不限票数"},
        {"perk2", "Expiration du billet personnalisable", "Custom ticket expiry", "Caducidad de la entrada personalizable", "Ekspirasyon tikè pèsonalize", "Validade do ingresso personalizável", "انتهاء صلاحية مخصص للتذكرة", "Individuelles Ablaufdatum", "Scadenza del biglietto personalizzabile", "自定义票券有效期"},
        {"perk3", "Nom du ticket personnalisable", "Custom ticket name", "Nombre de entrada personalizable", "Non tikè pèsonalize", "Nome do ingresso personalizável", "اسم مخصص للتذكرة", "Individueller Ticketname", "Nome del biglietto personalizzabile", "自定义票券名称"},
        {"subscription", "Abonnement", "Subscription", "Suscripción", "Abònman", "Assinatura", "الاشتراك", "Abonnement", "Abbonamento", "订阅"},
        {"per_month", "par mois", "per month", "al mes", "pa mwa", "por mês", "شهرياً", "pro Monat", "al mese", "每月"},
        {"pay_method", "Moyen de paiement", "Payment method", "Método de pago", "Metòd peman", "Forma de pagamento", "طريقة الدفع", "Zahlungsmethode", "Metodo di pagamento", "支付方式"},
        {"recap", "Récapitulatif", "Summary", "Resumen", "Rezime", "Resumo", "الملخص", "Zusammenfassung", "Riepilogo", "摘要"},
        {"price", "Prix", "Price", "Precio", "Pri", "Preço", "السعر", "Preis", "Prezzo", "价格"},
        {"renewal", "Renouvellement", "Renewal", "Renovación", "Renouvèlman", "Renovação", "التجديد", "Verlängerung", "Rinnovo", "续订"},
        {"renew_monthly", "Tous les mois", "Every month", "Cada mes", "Chak mwa", "Todo mês", "كل شهر", "Jeden Monat", "Ogni mese", "每月"},
        {"google_account", "Compte Google", "Google account", "Cuenta de Google", "Kont Google", "Conta Google", "حساب Google", "Google-Konto", "Account Google", "Google 账号"},
        {"cancel_info", "Annulable à tout moment dans Google Play. Le paiement est géré par votre compte Google.", "Cancel anytime in Google Play. Payment is handled by your Google account.", "Cancela cuando quieras en Google Play. El pago lo gestiona tu cuenta de Google.", "Ou ka anile nenpòt lè nan Google Play. Kont Google ou jere peman an.", "Cancele a qualquer momento no Google Play. O pagamento é gerido pela sua conta Google.", "يمكن الإلغاء في أي وقت من Google Play. تتم إدارة الدفع عبر حساب Google الخاص بك.", "Jederzeit in Google Play kündbar. Die Zahlung wird über dein Google-Konto abgewickelt.", "Annullabile in qualsiasi momento su Google Play. Il pagamento è gestito dal tuo account Google.", "可随时在 Google Play 取消。付款由您的 Google 账号处理。"},
        {"subscribe_btn", "S’abonner · %s avec Google Play", "Subscribe · %s with Google Play", "Suscribirse · %s con Google Play", "Abòne · %s ak Google Play", "Assinar · %s com Google Play", "اشترك · %s عبر Google Play", "Abonnieren · %s mit Google Play", "Abbonati · %s con Google Play", "订阅 · %s（通过 Google Play）"},
        {"secure_pay", "Paiement sécurisé via Google Play.", "Secure payment via Google Play.", "Pago seguro con Google Play.", "Peman an sekirite via Google Play.", "Pagamento seguro via Google Play.", "دفع آمن عبر Google Play.", "Sichere Zahlung über Google Play.", "Pagamento sicuro tramite Google Play.", "通过 Google Play 安全支付。"},
        {"add_event", "Ajouter un événement", "Add an event", "Añadir un evento", "Ajoute yon evènman", "Adicionar um evento", "إضافة فعالية", "Veranstaltung hinzufügen", "Aggiungi un evento", "添加活动"},
        {"offline", "Pas de connexion internet.", "No internet connection.", "Sin conexión a internet.", "Pa gen koneksyon entènèt.", "Sem conexão com a internet.", "لا يوجد اتصال بالإنترنت.", "Keine Internetverbindung.", "Nessuna connessione internet.", "没有网络连接。"},
        {"security", "Sécurité", "Security", "Seguridad", "Sekirite", "Segurança", "الأمان", "Sicherheit", "Sicurezza", "安全"},
        {"app_lock", "Verrouillage de l’application", "App lock", "Bloqueo de la aplicación", "Vèrouyaj aplikasyon an", "Bloqueio do aplicativo", "قفل التطبيق", "App-Sperre", "Blocco dell’app", "应用锁"},
        {"app_lock_sub", "Exiger votre mot de passe ou l’empreinte digitale pour ouvrir l’application.", "Require your password or fingerprint to open the app.", "Exigir tu contraseña o huella digital para abrir la aplicación.", "Mande modpas ou oswa anprent dijital ou pou louvri aplikasyon an.", "Exigir sua senha ou impressão digital para abrir o aplicativo.", "طلب كلمة المرور أو بصمة الإصبع لفتح التطبيق.", "Passwort oder Fingerabdruck zum Öffnen der App verlangen.", "Richiedi la password o l’impronta digitale per aprire l’app.", "打开应用时需要输入密码或指纹。"},
        {"two_fa", "Authentification à deux facteurs", "Two-factor authentication", "Autenticación en dos pasos", "Otantifikasyon de faktè", "Autenticação em duas etapas", "المصادقة الثنائية", "Zwei-Faktor-Authentifizierung", "Autenticazione a due fattori", "双重验证"},
        {"two_fa_sub", "Ajouter une étape de sécurité pour protéger l’accès à vos billetteries.", "Add a security step to protect access to your ticketing.", "Añade un paso de seguridad para proteger el acceso a tus entradas.", "Ajoute yon etap sekirite pou pwoteje aksè ak biyetri w yo.", "Adicione uma etapa de segurança para proteger o acesso às suas bilheterias.", "أضف خطوة أمان لحماية الوصول إلى تذاكرك.", "Füge einen Sicherheitsschritt hinzu, um den Zugriff auf dein Ticketing zu schützen.", "Aggiungi un passaggio di sicurezza per proteggere l’accesso alla tua biglietteria.", "增加一道安全验证，保护您的票务账户。"},
        {"notification", "Notification", "Notifications", "Notificaciones", "Notifikasyon", "Notificações", "الإشعارات", "Benachrichtigungen", "Notifiche", "通知"},
        {"scan_sounds", "Sons de numérisation", "Scan sounds", "Sonidos de escaneo", "Son eskanè", "Sons de leitura", "أصوات المسح", "Scan-Töne", "Suoni di scansione", "扫描提示音"},
        {"scan_sounds_sub", "Émettre un signal sonore à chaque scan de billet réussi.", "Play a sound each time a ticket is scanned successfully.", "Emitir un sonido en cada escaneo de entrada correcto.", "Fè yon son chak fwa yon tikè eskane avèk siksè.", "Emitir um som a cada leitura de ingresso bem-sucedida.", "إصدار صوت عند كل مسح ناجح للتذكرة.", "Bei jedem erfolgreichen Ticket-Scan einen Ton abspielen.", "Emetti un suono a ogni scansione riuscita del biglietto.", "每次成功扫描票券时播放提示音。"},
        {"profile", "Profil", "Profile", "Perfil", "Pwofil", "Perfil", "الملف الشخصي", "Profil", "Profilo", "个人资料"},
        {"personal", "Informations personnelles", "Personal information", "Información personal", "Enfòmasyon pèsonèl", "Informações pessoais", "المعلومات الشخصية", "Persönliche Daten", "Informazioni personali", "个人信息"},
        {"email", "E-mail", "Email", "Correo electrónico", "Imèl", "E-mail", "البريد الإلكتروني", "E-Mail", "E-mail", "电子邮箱"},
        {"identifier", "Identifiant", "ID", "Identificador", "Idantifyan", "Identificador", "المعرّف", "Kennung", "Identificativo", "标识符"},
        {"password", "Mot de passe", "Password", "Contraseña", "Modpas", "Senha", "كلمة المرور", "Passwort", "Password", "密码"},
        {"change_pw", "modifier le mot de passe", "change password", "cambiar la contraseña", "chanje modpas la", "alterar a senha", "تغيير كلمة المرور", "Passwort ändern", "modifica la password", "修改密码"},
        {"phone", "Numéro de téléphone", "Phone number", "Número de teléfono", "Nimewo telefòn", "Número de telefone", "رقم الهاتف", "Telefonnummer", "Numero di telefono", "电话号码"},
        {"add_phone", "Ajouter un numéro de téléphone", "Add a phone number", "Añadir un número de teléfono", "Ajoute yon nimewo telefòn", "Adicionar um número de telefone", "إضافة رقم هاتف", "Telefonnummer hinzufügen", "Aggiungi un numero di telefono", "添加电话号码"},
        {"delete_acc", "Supprimer le compte", "Delete account", "Eliminar la cuenta", "Siprime kont lan", "Excluir a conta", "حذف الحساب", "Konto löschen", "Elimina l’account", "删除账号"},
        {"delete_warn", "Attention : cette action est irréversible. En supprimant votre compte, vous perdrez définitivement l’accès à votre abonnement Seven Premium, à l’historique de vos événements ainsi qu’à la validation de vos billets en cours.", "Warning: this action is irreversible. By deleting your account, you will permanently lose access to your Seven Premium subscription, your event history and the validation of your current tickets.", "Atención: esta acción es irreversible. Al eliminar tu cuenta, perderás definitivamente el acceso a tu suscripción Seven Premium, al historial de tus eventos y a la validación de tus entradas en curso.", "Atansyon : aksyon sa a pa ka defèt. Si w siprime kont ou, w ap pèdi pou tout tan aksè ak abònman Seven Premium ou, istwa evènman w yo ak validasyon tikè ou yo ki an kou.", "Atenção: esta ação é irreversível. Ao excluir sua conta, você perderá definitivamente o acesso à sua assinatura Seven Premium, ao histórico dos seus eventos e à validação dos seus ingressos em andamento.", "تنبيه: هذا الإجراء لا رجعة فيه. بحذف حسابك ستفقد نهائياً إمكانية الوصول إلى اشتراك Seven Premium وسجل فعالياتك والتحقق من تذاكرك الحالية.", "Achtung: Diese Aktion ist unwiderruflich. Wenn du dein Konto löschst, verlierst du dauerhaft den Zugriff auf dein Seven-Premium-Abonnement, deinen Veranstaltungsverlauf und die Validierung deiner aktuellen Tickets.", "Attenzione: questa azione è irreversibile. Eliminando il tuo account perderai definitivamente l’accesso al tuo abbonamento Seven Premium, alla cronologia dei tuoi eventi e alla convalida dei biglietti in corso.", "注意：此操作不可撤销。删除账号后，您将永久失去 Seven Premium 订阅、活动历史记录以及当前票券验证的访问权限。"},
        {"logout", "Déconnexion", "Log out", "Cerrar sesión", "Dekonekte", "Sair", "تسجيل الخروج", "Abmelden", "Disconnetti", "退出登录"},
        {"language", "Langue", "Language", "Idioma", "Lang", "Idioma", "اللغة", "Sprache", "Lingua", "语言"},
        {"lang_fr", "Français", "French", "Francés", "Fransè", "Francês", "الفرنسية", "Französisch", "Francese", "法语"},
        {"lang_en", "Anglais", "English", "Inglés", "Angle", "Inglês", "الإنجليزية", "Englisch", "Inglese", "英语"},
        {"lang_es", "Espagnol", "Spanish", "Español", "Espayòl", "Espanhol", "الإسبانية", "Spanisch", "Spagnolo", "西班牙语"},
        {"lang_ht", "Créole Haïtien", "Haitian Creole", "Criollo haitiano", "Kreyòl ayisyen", "Crioulo haitiano", "الكريولية الهايتية", "Haitianisches Kreolisch", "Creolo haitiano", "海地克里奥尔语"},
        {"lang_pt", "Portugais", "Portuguese", "Portugués", "Pòtigè", "Português", "البرتغالية", "Portugiesisch", "Portoghese", "葡萄牙语"},
        {"lang_ar", "Arabe", "Arabic", "Árabe", "Arab", "Árabe", "العربية", "Arabisch", "Arabo", "阿拉伯语"},
        {"lang_de", "Allemand", "German", "Alemán", "Alman", "Alemão", "الألمانية", "Deutsch", "Tedesco", "德语"},
        {"lang_it", "Italien", "Italian", "Italiano", "Italyen", "Italiano", "الإيطالية", "Italienisch", "Italiano", "意大利语"},
        {"lang_zh", "Chinois", "Chinese", "Chino", "Chinwa", "Chinês", "الصينية", "Chinesisch", "Cinese", "中文"},
        {"settings", "Paramètres", "Settings", "Ajustes", "Paramèt", "Configurações", "الإعدادات", "Einstellungen", "Impostazioni", "设置"},
        {"support", "Contacter Assistance", "Contact support", "Contactar con soporte", "Kontakte asistans", "Contactar suporte", "الاتصال بالدعم", "Support kontaktieren", "Contatta l’assistenza", "联系客服"},
        {"terms", "Conditions d’utilisation", "Terms of use", "Condiciones de uso", "Kondisyon itilizasyon", "Termos de uso", "شروط الاستخدام", "Nutzungsbedingungen", "Condizioni d’uso", "使用条款"},
        {"privacy", "Politique de confidentialité", "Privacy policy", "Política de privacidad", "Règleman konfidansyalite", "Política de privacidade", "سياسة الخصوصية", "Datenschutzerklärung", "Informativa sulla privacy", "隐私政策"},
        {"msg_title", "Message", "Message", "Mensaje", "Mesaj", "Mensagem", "الرسالة", "Nachricht", "Messaggio", "消息"},
        {"msg_delete_warn", "Si vous supprimez ce message de votre historique de chat, l’assistant ne pourra plus le voir dans cette conversation.", "If you delete this message from your chat history, the assistant will no longer be able to see it in this conversation.", "Si eliminas este mensaje de tu historial de chat, el asistente ya no podrá verlo en esta conversación.", "Si w siprime mesaj sa a nan istwa chat ou, asistan an p ap ka wè l ankò nan konvèsasyon sa a.", "Se você excluir esta mensagem do seu histórico de chat, o assistente não poderá mais vê-la nesta conversa.", "إذا حذفت هذه الرسالة من سجل الدردشة، فلن يتمكن المساعد من رؤيتها في هذه المحادثة.", "Wenn du diese Nachricht aus deinem Chatverlauf löschst, kann der Assistent sie in dieser Unterhaltung nicht mehr sehen.", "Se elimini questo messaggio dalla cronologia della chat, l’assistente non potrà più vederlo in questa conversazione.", "如果您从聊天记录中删除此消息，助手将无法在此对话中再看到它。"},
        {"delete", "Supprimer", "Delete", "Eliminar", "Siprime", "Excluir", "حذف", "Löschen", "Elimina", "删除"},
        {"cancel", "Annuler", "Cancel", "Cancelar", "Anile", "Cancelar", "إلغاء", "Abbrechen", "Annulla", "取消"},
        {"today", "Aujourd’hui", "Today", "Hoy", "Jodi a", "Hoje", "اليوم", "Heute", "Oggi", "今天"},
        {"yesterday", "Hier", "Yesterday", "Ayer", "Yè", "Ontem", "أمس", "Gestern", "Ieri", "昨天"},
        {"sent", "Envoyé", "Sent", "Enviado", "Voye", "Enviado", "تم الإرسال", "Gesendet", "Inviato", "已发送"},
        {"seen", "Vu", "Seen", "Visto", "Wè", "Visto", "تمت المشاهدة", "Gesehen", "Visto", "已读"},
        {"msg_hint", "Message", "Message", "Mensaje", "Mesaj", "Mensagem", "رسالة", "Nachricht", "Messaggio", "消息"},
        {"negotiating", "Négociation en cours", "Negotiation in progress", "Negociación en curso", "Negosyasyon ap fèt", "Negociação em andamento", "التفاوض جارٍ", "Verhandlung läuft", "Trattativa in corso", "洽谈中"},
        {"photo", "Photo", "Photo", "Foto", "Foto", "Foto", "صورة", "Foto", "Foto", "照片"}
    };

    private java.util.HashMap<String, String[]> evDict;
    private String evLang = null;
    private ViewGroup evPageTarget;
    private String evPageKind = "";
    private String evProfEmail = "";
    private String evProfId = "";
    private String evGlyphChevron = "";
    private String evGlyphCopy = "";
    private ViewGroup addEvTarget;
    private String addEvLabel = "";
    private String addEvGlyph = "";

    private int evLangIndex(String c) {
        for (int i = 0; i < EV_LANGS.length; i++) if (EV_LANGS[i].equals(c)) return i;
        return -1;
    }

    private String evCurrentLang() {
        if (evLang == null) {
            try {
                evLang = context.getSharedPreferences("SevenUtils", Context.MODE_PRIVATE)
                        .getString("seven_lang", "fr");
            } catch (Exception e) {
                evLang = "fr";
            }
            if (evLangIndex(evLang) < 0) evLang = "fr";
        }
        return evLang;
    }

    private String evT(String key) {
        if (evDict == null) {
            evDict = new java.util.HashMap<String, String[]>();
            for (String[] r : EV_I18N) evDict.put(r[0], r);
        }
        String[] cu = evCustom.get(key);
        if (cu != null) {
            int li = evLangIndex(evCurrentLang());
            String v = li >= 0 ? cu[li] : null;
            if (v == null || v.isEmpty()) v = cu[0];
            return v == null || v.isEmpty() ? key : v;
        }
        String[] r = evDict.get(key);
        if (r == null) return key;
        int i = evLangIndex(evCurrentLang());
        String s = (i >= 0 && i + 1 < r.length) ? r[i + 1] : null;
        return s == null || s.isEmpty() ? r[1] : s;
    }

    private void evApplyDir(ViewGroup g) {
        if (g != null) g.setLayoutDirection("ar".equals(evCurrentLang())
                ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
    }

    private String evPer() {
        return "mois".equalsIgnoreCase(evSubPeriod) ? evT("per_month") : "par " + evSubPeriod;
    }

    private String evRenew() {
        return "mois".equalsIgnoreCase(evSubPeriod) ? evT("renew_monthly") : "Tous les " + evSubPeriod;
    }

    private void evRefreshAll() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    if (evContainer != null) evRender();
                    if (evPageTarget != null) evRenderPage();
                    if (addEvTarget != null) evBuildAddButton();
                    if (evMenuTarget != null) evRenderMenu();
                    if (evDelTarget != null) evRenderDelete();
                    if (chatEdit != null) chatEdit.setHint(evT("msg_hint"));
                } catch (Exception e) {
                    OnError("Langue : " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Change la langue de toute l'appli : fr, en, es, ht, pt, ar, de, it ou zh. Elle est mémorisée et les écrans Seven affichés (formulaire, abonnement, bouton, pages) sont redessinés dans la nouvelle langue. L'arabe passe en lecture de droite à gauche.")
    public void SetAppLanguage(String code) {
        String c = code == null ? "" : code.trim().toLowerCase(java.util.Locale.ROOT);
        if (evLangIndex(c) < 0) {
            OnError("SetAppLanguage: langue inconnue (" + code + ").");
            return;
        }
        evLang = c;
        try {
            context.getSharedPreferences("SevenUtils", Context.MODE_PRIVATE)
                    .edit().putString("seven_lang", c).apply();
        } catch (Exception ignored) {
        }
        evRefreshAll();
        OnLanguageChanged(c);
    }

    @SimpleFunction(description = "Retourne le code de la langue actuelle (fr, en, es, ht, pt, ar, de, it, zh). « fr » au premier lancement.")
    public String GetAppLanguage() {
        return evCurrentLang();
    }

    @SimpleFunction(description = "Retourne le texte traduit d'une clé du dictionnaire (ex. offline, err_name, logout) dans la langue actuelle, pour traduire aussi tes propres messages.")
    public String GetTranslation(String key) {
        return evT(key == null ? "" : key);
    }

    @SimpleEvent(description = "Déclenché après un changement de langue (code = fr, en, es, ht, pt, ar, de, it ou zh).")
    public void OnLanguageChanged(String code) {
        EventDispatcher.dispatchEvent(this, "OnLanguageChanged", code);
    }

    @SimpleEvent(description = "Déclenché quand l'utilisateur choisit une langue dans BuildLanguageList (la langue est déjà appliquée).")
    public void OnLanguageSelected(String code) {
        EventDispatcher.dispatchEvent(this, "OnLanguageSelected", code);
    }

    @SimpleFunction(description = "Définit les icônes Phosphor des pages Profil : chevron (flèche vers la droite) et copier. Utilise la police de SetEventIconFont.")
    public void SetProfileIcons(String chevronGlyph, String copyGlyph) {
        evGlyphChevron = chevronGlyph == null ? "" : chevronGlyph;
        evGlyphCopy = copyGlyph == null ? "" : copyGlyph;
        evRefreshAll();
    }

    private void evBuildPage(final AndroidViewComponent container, final String kind) {
        if (container == null || container.getView() == null) {
            OnError("Page " + kind + ": conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup target = realLayout(container);
                    if (target == null) {
                        OnError("Page " + kind + ": conteneur invalide.");
                        return;
                    }
                    evPageTarget = target;
                    evPageKind = kind;
                    evRenderPage();
                } catch (Exception e) {
                    OnError("Page " + kind + ": " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Affiche la page Sécurité (textes traduits, ligne horizontale du haut) dans l'arrangement donné. Lignes : app_lock (interrupteur à droite : place le tien) et two_fa. Un clic sur une ligne déclenche OnSettingsItemClick(id).")
    public void BuildSecurityPage(AndroidViewComponent container) {
        evBuildPage(container, "security");
    }

    @SimpleFunction(description = "Affiche la page Notification (textes traduits) dans l'arrangement donné. Ligne : scan_sounds (interrupteur à droite : place le tien).")
    public void BuildNotificationPage(AndroidViewComponent container) {
        evBuildPage(container, "notification");
    }

    @SimpleFunction(description = "Affiche la page Profil (textes traduits) avec l'e-mail et l'identifiant donnés. Lignes : email, copy_id (copie l'identifiant), password, phone, delete et le bouton logout : chacune déclenche OnSettingsItemClick(id).")
    public void BuildProfilePage(AndroidViewComponent container, String email, String userId) {
        evProfEmail = email == null ? "" : email;
        evProfId = userId == null ? "" : userId;
        evBuildPage(container, "profile");
    }

    @SimpleFunction(description = "Affiche la liste des langues (Français, Anglais, Espagnol, Créole Haïtien, Portugais, Arabe, Allemand, Italien, Chinois) : la langue actuelle est en blanc avec une coche, les autres en gris. Un clic change la langue de toute l'appli.")
    public void BuildLanguageList(AndroidViewComponent container) {
        evBuildPage(container, "language");
    }

    private void evRowClicked(String id) {
        if ("copy_id".equals(id)) {
            try {
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("id", evProfId));
            } catch (Exception ignored) {
            }
        }
        OnSettingsItemClick(id);
    }

    private void evRenderPage() {
        if (evPageTarget == null) return;
        if ("language".equals(evPageKind)) {
            evRenderLanguage();
            return;
        }
        if ("security".equals(evPageKind)) {
            LinearLayout col = evSheet(evPageTarget, evT("security"));
            evSettingsRow(col, "app_lock", evT("app_lock"), evT("app_lock_sub"), "", "white", "", true);
            evSettingsRow(col, "two_fa", evT("two_fa"), evT("two_fa_sub"), "", "white", "", false);
        } else if ("notification".equals(evPageKind)) {
            LinearLayout col = evSheet(evPageTarget, evT("notification"));
            evSettingsRow(col, "scan_sounds", evT("scan_sounds"), evT("scan_sounds_sub"), "", "white", "", true);
        } else if ("profile".equals(evPageKind)) {
            LinearLayout col = evSheet(evPageTarget, evT("profile"));
            TextView ph = text(evT("personal"), 18, Color.WHITE, true);
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            plp.topMargin = hp(3f);
            col.addView(ph, plp);
            evSettingsRow(col, "email", evT("email"), "", evProfEmail, "white", "", false);
            evSettingsRow(col, "copy_id", evT("identifier"), "", evProfId, "white",
                    evGlyphCopy, false);
            evSettingsRow(col, "password", evT("password"), "", evT("change_pw"), "gray",
                    evGlyphChevron.isEmpty() ? "›" : evGlyphChevron, false);
            evSettingsRow(col, "phone", evT("phone"), "", evT("add_phone"), "gray",
                    evGlyphChevron.isEmpty() ? "›" : evGlyphChevron, false);
            evSettingsRow(col, "delete", evT("delete_acc"), evT("delete_warn"), "", "white",
                    evGlyphChevron.isEmpty() ? "›" : evGlyphChevron, false);
            TextView lo = text(evT("logout"), 18, Color.WHITE, true);
            lo.setGravity(Gravity.CENTER);
            lo.setBackground(evBg(Color.rgb(0x2C, 0x2C, 0x2E), 0, Color.TRANSPARENT, dpToPx(24)));
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, hp(7f));
            llp.topMargin = hp(4f);
            lo.setLayoutParams(llp);
            lo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OnSettingsItemClick("logout");
                }
            });
            col.addView(lo);
        }
    }

    private void evRenderLanguage() {
        if (evPageTarget == null) return;
        LinearLayout col = evSheet(evPageTarget, evT("language"));
        String current = evCurrentLang();
        for (int i = 0; i < EV_LANGS.length; i++) {
            final String code = EV_LANGS[i];
            boolean sel = code.equals(current);
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, hp(6f));
            rlp.topMargin = hp(1f);
            row.setLayoutParams(rlp);
            TextView t = text(evT("lang_" + code), 16, sel ? Color.WHITE : evGray(), true);
            t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(t);
            if (sel) row.addView(evIcon(evGlyphCheck, "✓", 22, Color.WHITE));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SetAppLanguage(code);
                    OnLanguageSelected(code);
                }
            });
            col.addView(row);
        }
    }

    // =========================================================================
    // CLAVIER, TRADUCTIONS PERSONNALISÉES, MENU PARAMÈTRES, DIALOGUE SUPPRESSION
    // =========================================================================
    private boolean evKbExplicit = false;
    private final java.util.HashMap<String, String[]> evCustom = new java.util.HashMap<String, String[]>();
    private ViewGroup evMenuTarget;
    private ViewGroup evDelTarget;

    private void evSetSoftInput(final int mode) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    activity.getWindow().setSoftInputMode(mode);
                } catch (Exception e) {
                    OnError("SetKeyboardBehavior: " + e.getMessage());
                }
            }
        });
    }

    private void evAutoPan() {
        if (!evKbExplicit)
            evSetSoftInput(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    private java.util.Locale evLocale() {
        String c = evCurrentLang();
        if ("ht".equals(c)) return new java.util.Locale("fr", "HT");
        return new java.util.Locale(c);
    }

    @SimpleFunction(description = "Choisit comment la page réagit à l'ouverture du clavier : \"pan\" (la page n'est plus écrasée, elle se décale pour montrer le champ ; par défaut dès qu'un écran Seven ou la saisie du chat est affiché), \"nothing\" (rien ne bouge) ou \"resize\" (la page est rétrécie, comportement d'origine).")
    public void SetKeyboardBehavior(String mode) {
        String m = mode == null ? "" : mode.trim().toLowerCase(java.util.Locale.ROOT);
        evKbExplicit = true;
        if (m.equals("pan")) evSetSoftInput(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        else if (m.equals("nothing")) evSetSoftInput(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        else if (m.equals("resize")) evSetSoftInput(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        else {
            evKbExplicit = false;
            OnError("SetKeyboardBehavior: mode inconnu (pan, nothing ou resize).");
        }
    }

    @SimpleFunction(description = "Ajoute tes propres textes au dictionnaire. json = {\"scanner\":{\"fr\":\"Scanner\",\"en\":\"Scan\",\"es\":\"Escanear\"}} (codes fr, en, es, ht, pt, ar, de, it, zh). Ensuite GetTranslation(\"scanner\") suit la langue choisie. Une clé déjà existante est remplacée.")
    public void AddTranslations(String json) {
        try {
            org.json.JSONObject root = new org.json.JSONObject(json);
            java.util.Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                org.json.JSONObject langs = root.getJSONObject(k);
                String[] arr = new String[EV_LANGS.length];
                for (int i = 0; i < EV_LANGS.length; i++) arr[i] = langs.optString(EV_LANGS[i], "");
                evCustom.put(k, arr);
            }
            evRefreshAll();
        } catch (Exception e) {
            OnError("AddTranslations: " + e.getMessage());
        }
    }

    // ----------------------------- Menu Paramètres ----------------------------
    @SimpleFunction(description = "Construit le menu Paramètres dans l'arrangement donné : Profil, Langue, Sécurité, Notification, Contacter Assistance, Conditions d'utilisation, Politique de confidentialité (Manrope gras 20, traduits). Chaque ligne a son événement : OnProfileClick, OnLanguageClick, OnSecurityClick, OnNotificationClick, OnSupportClick, OnTermsClick, OnPrivacyClick. Le titre de la page (clé settings) se pose avec GetTranslation.")
    public void BuildSettingsMenu(final AndroidViewComponent container) {
        if (container == null || container.getView() == null) {
            OnError("BuildSettingsMenu: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup target = realLayout(container);
                    if (target == null) {
                        OnError("BuildSettingsMenu: conteneur invalide.");
                        return;
                    }
                    evMenuTarget = target;
                    evRenderMenu();
                } catch (Exception e) {
                    OnError("BuildSettingsMenu: " + e.getMessage());
                }
            }
        });
    }

    private void evRenderMenu() {
        if (evMenuTarget == null) return;
        evMenuTarget.removeAllViews();
        evApplyDir(evMenuTarget);
        ScrollView sv = new ScrollView(context);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(wp(6f), hp(1f), wp(6f), hp(3f));
        sv.addView(col, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        evMenuTarget.addView(sv, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        String[] ids = {"profile", "language", "security", "notification", "support", "terms", "privacy"};
        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];
            TextView t = text(evT(id), 20, Color.WHITE, true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = i == 0 ? 0 : hp(3f);
            t.setLayoutParams(lp);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    evMenuClick(id);
                }
            });
            col.addView(t);
        }
    }

    private void evMenuClick(String id) {
        if ("profile".equals(id)) OnProfileClick();
        else if ("language".equals(id)) OnLanguageClick();
        else if ("security".equals(id)) OnSecurityClick();
        else if ("notification".equals(id)) OnNotificationClick();
        else if ("support".equals(id)) OnSupportClick();
        else if ("terms".equals(id)) OnTermsClick();
        else if ("privacy".equals(id)) OnPrivacyClick();
    }

    @SimpleEvent(description = "Déclenché sur la ligne Profil du menu Paramètres.")
    public void OnProfileClick() {
        EventDispatcher.dispatchEvent(this, "OnProfileClick");
    }

    @SimpleEvent(description = "Déclenché sur la ligne Langue du menu Paramètres.")
    public void OnLanguageClick() {
        EventDispatcher.dispatchEvent(this, "OnLanguageClick");
    }

    @SimpleEvent(description = "Déclenché sur la ligne Sécurité du menu Paramètres.")
    public void OnSecurityClick() {
        EventDispatcher.dispatchEvent(this, "OnSecurityClick");
    }

    @SimpleEvent(description = "Déclenché sur la ligne Notification du menu Paramètres.")
    public void OnNotificationClick() {
        EventDispatcher.dispatchEvent(this, "OnNotificationClick");
    }

    @SimpleEvent(description = "Déclenché sur la ligne Contacter Assistance du menu Paramètres.")
    public void OnSupportClick() {
        EventDispatcher.dispatchEvent(this, "OnSupportClick");
    }

    @SimpleEvent(description = "Déclenché sur la ligne Conditions d'utilisation du menu Paramètres.")
    public void OnTermsClick() {
        EventDispatcher.dispatchEvent(this, "OnTermsClick");
    }

    @SimpleEvent(description = "Déclenché sur la ligne Politique de confidentialité du menu Paramètres.")
    public void OnPrivacyClick() {
        EventDispatcher.dispatchEvent(this, "OnPrivacyClick");
    }

    // ------------------------ Dialogue de suppression -------------------------
    @SimpleFunction(description = "Construit le contenu du dialogue « Message » (suppression d'un message du chat) dans l'arrangement donné : ligne horizontale, titre 18, texte gris 15, boutons Supprimer (rouge) et Annuler (blanc) en 14, traduits. Supprimer déclenche OnDeleteMessageConfirm, Annuler déclenche OnDeleteMessageCancel : tu fermes ton dialogue ou tu supprimes le message dans ces événements.")
    public void BuildDeleteMessageDialog(final AndroidViewComponent container) {
        if (container == null || container.getView() == null) {
            OnError("BuildDeleteMessageDialog: conteneur invalide.");
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup target = realLayout(container);
                    if (target == null) {
                        OnError("BuildDeleteMessageDialog: conteneur invalide.");
                        return;
                    }
                    evDelTarget = target;
                    evRenderDelete();
                } catch (Exception e) {
                    OnError("BuildDeleteMessageDialog: " + e.getMessage());
                }
            }
        });
    }

    private void evRenderDelete() {
        if (evDelTarget == null) return;
        LinearLayout col = evSheet(evDelTarget, evT("msg_title"));
        TextView body = text(evT("msg_delete_warn"), 15, evGray(), false);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.topMargin = hp(2.5f);
        col.addView(body, blp);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = hp(8f);
        TextView del = text(evT("delete"), 14, Color.rgb(0xE5, 0x39, 0x35), true);
        del.setGravity(Gravity.START);
        del.setPadding(0, dp(10), 0, dp(10));
        del.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnDeleteMessageConfirm();
            }
        });
        TextView cancel = text(evT("cancel"), 14, Color.WHITE, true);
        cancel.setGravity(Gravity.END);
        cancel.setPadding(0, dp(10), 0, dp(10));
        cancel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnDeleteMessageCancel();
            }
        });
        row.addView(del);
        row.addView(cancel);
        col.addView(row, rlp);
    }

    @SimpleEvent(description = "Déclenché sur « Supprimer » du dialogue de suppression : supprime le message et ferme ton dialogue.")
    public void OnDeleteMessageConfirm() {
        EventDispatcher.dispatchEvent(this, "OnDeleteMessageConfirm");
    }

    @SimpleEvent(description = "Déclenché sur « Annuler » du dialogue de suppression : ferme ton dialogue sans supprimer.")
    public void OnDeleteMessageCancel() {
        EventDispatcher.dispatchEvent(this, "OnDeleteMessageCancel");
    }}
